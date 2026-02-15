package com.qinmomeak.recording

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class NativeAudioProcessor(private val context: Context) {

    interface ProgressCallback {
        fun onProgress(progress: Int, message: String)
    }

    suspend fun splitAudio(
        sourcePath: String,
        segmentDurationMs: Long = DEFAULT_SEGMENT_DURATION_MS,
        callback: ProgressCallback? = null,
        shouldStop: (() -> Boolean)? = null
    ): List<File> = withContext(Dispatchers.IO) {
        ensureNotStopped(shouldStop)
        val source = File(sourcePath)
        if (!source.exists()) return@withContext emptyList()

        val meta = readMeta(source.absolutePath) ?: return@withContext emptyList()
        val totalBytes = source.length().coerceAtLeast(1L)
        val desiredSegmentMs = computeSegmentDurationMs(
            durationMs = meta.durationMs,
            totalBytes = totalBytes,
            userLimitMs = segmentDurationMs
        )

        if (meta.durationMs <= MAX_SEGMENT_DURATION_MS && totalBytes <= MAX_SILICONFLOW_BYTES) {
            callback?.onProgress(100, "音频无需分片")
            return@withContext listOf(source)
        }

        val segmentCount = max(1, ceil(meta.durationMs.toDouble() / desiredSegmentMs.toDouble()).toInt())
        callback?.onProgress(5, "切片参数: 目标时长 ${desiredSegmentMs / 1000}s, 预计 ${segmentCount} 段")

        val rawSegments = if (isMp3(meta.mime, source.name)) {
            splitMp3ByStream(source, segmentCount, callback, shouldStop)
        } else {
            splitByMuxer(source.absolutePath, meta, desiredSegmentMs, callback, shouldStop)
        }

        if (rawSegments.isEmpty()) return@withContext emptyList()

        val fixed = enforceLimits(rawSegments, callback, shouldStop)
        callback?.onProgress(100, "分片完成，共 ${fixed.size} 段")
        fixed
    }

    private data class AudioMeta(
        val trackIndex: Int,
        val trackFormat: MediaFormat,
        val durationMs: Long,
        val mime: String
    )

    private fun computeSegmentDurationMs(durationMs: Long, totalBytes: Long, userLimitMs: Long): Long {
        val maxDurationBySize = ((TARGET_SEGMENT_BYTES.toDouble() / totalBytes.toDouble()) * durationMs.toDouble()).toLong()
        return min(
            min(userLimitMs, maxDurationBySize.coerceAtLeast(MIN_SEGMENT_DURATION_MS)),
            MAX_SEGMENT_DURATION_MS
        ).coerceAtLeast(MIN_SEGMENT_DURATION_MS)
    }

    private fun splitByMuxer(
        sourcePath: String,
        meta: AudioMeta,
        eachDurationMs: Long,
        callback: ProgressCallback?,
        shouldStop: (() -> Boolean)?
    ): List<File> {
        val count = max(1, ceil(meta.durationMs.toDouble() / eachDurationMs.toDouble()).toInt())
        val output = mutableListOf<File>()
        for (i in 0 until count) {
            ensureNotStopped(shouldStop)
            val startMs = i * eachDurationMs
            if (startMs >= meta.durationMs) break
            val endMs = min(meta.durationMs, (i + 1) * eachDurationMs)
            callback?.onProgress((10 + (i * 60 / count)).coerceIn(10, 75), "AAC/M4A切片 ${i + 1}/$count")
            val seg = muxSegment(sourcePath, meta.trackIndex, meta.trackFormat, meta.mime, startMs, endMs, i)
            if (seg != null && seg.exists() && seg.length() > 0L) {
                output += seg
            }
        }
        return output
    }

    private fun splitMp3ByStream(
        source: File,
        segmentCount: Int,
        callback: ProgressCallback?,
        shouldStop: (() -> Boolean)?
    ): List<File> {
        val output = mutableListOf<File>()
        val total = source.length().coerceAtLeast(1L)
        val approxPerSegment = ceil(total.toDouble() / segmentCount.toDouble()).toLong()
        val boundaries = mutableListOf<Long>()
        boundaries += 0L

        var cursor = 0L
        for (i in 1 until segmentCount) {
            ensureNotStopped(shouldStop)
            val target = min(total - 1, cursor + approxPerSegment)
            val sync = findMp3SyncNear(source, target)
            if (sync <= cursor || sync >= total) break
            boundaries += sync
            cursor = sync
            callback?.onProgress((10 + (i * 60 / segmentCount)).coerceIn(10, 75), "MP3切片定位 ${i}/$segmentCount")
        }
        boundaries += total

        for (i in 0 until boundaries.size - 1) {
            ensureNotStopped(shouldStop)
            val start = boundaries[i]
            val end = boundaries[i + 1]
            if (end <= start) continue
            val out = File(context.cacheDir, "sf_seg_${System.currentTimeMillis()}_${i + 1}.mp3")
            copyRange(source, out, start, end)
            if (out.exists() && out.length() > 0L) {
                output += out
            }
        }
        return output
    }

    private fun enforceLimits(
        segments: List<File>,
        callback: ProgressCallback?,
        shouldStop: (() -> Boolean)?
    ): List<File> {
        var current = segments.toMutableList()
        var pass = 0
        while (pass < 3) {
            pass++
            var changed = false
            val next = mutableListOf<File>()
            for ((i, seg) in current.withIndex()) {
                ensureNotStopped(shouldStop)
                val sizeOk = seg.length() <= MAX_SILICONFLOW_BYTES
                val meta = readMeta(seg.absolutePath)
                val durationOk = meta?.durationMs?.let { it <= MAX_SEGMENT_DURATION_MS } ?: true
                if (sizeOk && durationOk) {
                    next += seg
                    continue
                }

                val dur = meta?.durationMs ?: (MAX_SEGMENT_DURATION_MS + 1)
                val byDur = ceil(dur.toDouble() / MAX_SEGMENT_DURATION_MS.toDouble()).toInt()
                val bySize = ceil(seg.length().toDouble() / TARGET_SEGMENT_BYTES.toDouble()).toInt()
                val subCount = max(2, max(byDur, bySize))
                val children = if (isMp3(meta?.mime.orEmpty(), seg.name)) {
                    splitMp3ByStream(seg, subCount, null, shouldStop)
                } else {
                    val subMeta = meta ?: readMeta(seg.absolutePath)
                    if (subMeta == null) {
                        emptyList()
                    } else {
                        splitByMuxer(
                            seg.absolutePath,
                            subMeta,
                            max(MIN_SEGMENT_DURATION_MS, dur / subCount),
                            null,
                            shouldStop
                        )
                    }
                }
                if (children.isNotEmpty()) {
                    changed = true
                    next += children
                    runCatching { seg.delete() }
                } else {
                    next += seg
                }
                callback?.onProgress((80 + ((i + 1) * 15 / max(1, current.size))).coerceIn(80, 95), "超限校正 ${i + 1}/${current.size}")
            }
            current = next
            if (!changed) break
        }
        return current
    }

    private fun findMp3SyncNear(file: File, target: Long): Long {
        val length = file.length()
        if (length <= 4) return target
        val start = max(0L, target - MP3_SEARCH_BACK)
        val end = min(length - 2, target + MP3_SEARCH_FORWARD)

        RandomAccessFile(file, "r").use { raf ->
            var pos = start
            var fallback = -1L
            while (pos < end) {
                raf.seek(pos)
                val b1 = raf.read()
                val b2 = raf.read()
                if (b1 < 0 || b2 < 0) break
                if (isMp3FrameSync(b1, b2)) {
                    if (pos >= target) return pos
                    if (fallback < 0) fallback = pos
                }
                pos++
            }
            if (fallback >= 0) return fallback
        }
        return target
    }

    private fun isMp3FrameSync(b1: Int, b2: Int): Boolean {
        if (b1 != 0xFF) return false
        if ((b2 and 0xE0) != 0xE0) return false
        val layerBits = (b2 shr 1) and 0x03
        return layerBits != 0
    }

    private fun copyRange(source: File, dest: File, start: Long, end: Long) {
        RandomAccessFile(source, "r").use { input ->
            RandomAccessFile(dest, "rw").use { output ->
                input.seek(start)
                output.setLength(0)
                val buf = ByteArray(256 * 1024)
                var remain = end - start
                while (remain > 0) {
                    val read = input.read(buf, 0, min(buf.size.toLong(), remain).toInt())
                    if (read <= 0) break
                    output.write(buf, 0, read)
                    remain -= read
                }
            }
        }
    }

    private fun readMeta(sourcePath: String): AudioMeta? {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(sourcePath)
            val audioTrack = findAudioTrack(extractor)
            if (audioTrack < 0) return null
            val fmt = extractor.getTrackFormat(audioTrack)
            val durUs = if (fmt.containsKey(MediaFormat.KEY_DURATION)) fmt.getLong(MediaFormat.KEY_DURATION) else 0L
            val mime = fmt.getString(MediaFormat.KEY_MIME).orEmpty()
            AudioMeta(
                trackIndex = audioTrack,
                trackFormat = fmt,
                durationMs = max(1L, durUs / 1000L),
                mime = mime
            )
        } catch (_: Exception) {
            null
        } finally {
            runCatching { extractor.release() }
        }
    }

    private fun muxSegment(
        sourcePath: String,
        audioTrackIndex: Int,
        sourceFormat: MediaFormat,
        mime: String,
        startMs: Long,
        endMs: Long,
        segmentIndex: Int
    ): File? {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        return try {
            extractor.setDataSource(sourcePath)
            extractor.selectTrack(audioTrackIndex)

            val out = File(context.cacheDir, "sf_seg_${System.currentTimeMillis()}_${segmentIndex + 1}.m4a")
            muxer = MediaMuxer(out.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val outTrack = muxer.addTrack(buildMuxerFormat(sourceFormat, mime))
            muxer.start()

            val startUs = startMs * 1000L
            val endUs = endMs * 1000L
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            val buffer = ByteBuffer.allocate(512 * 1024)
            val info = MediaCodec.BufferInfo()

            while (true) {
                val sampleTime = extractor.sampleTime
                if (sampleTime < 0 || sampleTime >= endUs) break

                info.offset = 0
                info.size = extractor.readSampleData(buffer, 0)
                if (info.size <= 0) break
                info.presentationTimeUs = sampleTime
                info.flags = extractor.sampleFlags
                muxer.writeSampleData(outTrack, buffer, info)
                extractor.advance()
            }
            out
        } catch (_: Exception) {
            null
        } finally {
            runCatching { muxer?.stop() }
            runCatching { muxer?.release() }
            runCatching { extractor.release() }
        }
    }

    private fun buildMuxerFormat(source: MediaFormat, mime: String): MediaFormat {
        val sampleRate = source.getIntegerOrNull(MediaFormat.KEY_SAMPLE_RATE) ?: 44100
        val channels = source.getIntegerOrNull(MediaFormat.KEY_CHANNEL_COUNT) ?: 2
        val out = MediaFormat.createAudioFormat(mime, sampleRate, channels)
        source.getIntegerOrNull(MediaFormat.KEY_BIT_RATE)?.let { out.setInteger(MediaFormat.KEY_BIT_RATE, it) }
        source.getByteBufferOrNull("csd-0")?.let { out.setByteBuffer("csd-0", it.duplicate()) }
        source.getByteBufferOrNull("csd-1")?.let { out.setByteBuffer("csd-1", it.duplicate()) }
        return out
    }

    private fun findAudioTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val fmt = extractor.getTrackFormat(i)
            val mime = fmt.getString(MediaFormat.KEY_MIME).orEmpty()
            if (mime.startsWith("audio/")) return i
        }
        return -1
    }

    private fun MediaFormat.getIntegerOrNull(key: String): Int? = runCatching { getInteger(key) }.getOrNull()
    private fun MediaFormat.getByteBufferOrNull(key: String): ByteBuffer? = runCatching { getByteBuffer(key) }.getOrNull()

    private fun isMp3(mime: String, fileName: String): Boolean {
        return mime.equals(MediaFormat.MIMETYPE_AUDIO_MPEG, ignoreCase = true) || fileName.lowercase().endsWith(".mp3")
    }

    private fun ensureNotStopped(shouldStop: (() -> Boolean)?) {
        if (shouldStop?.invoke() == true) {
            throw java.util.concurrent.CancellationException("Task stopped")
        }
    }

    companion object {
        const val DEFAULT_SEGMENT_DURATION_MS = 50 * 60 * 1000L
        const val MAX_SILICONFLOW_BYTES = 50L * 1024L * 1024L
        const val MAX_SEGMENT_DURATION_MS = 59 * 60 * 1000L

        private const val TARGET_SEGMENT_BYTES = 45L * 1024L * 1024L
        private const val MIN_SEGMENT_DURATION_MS = 30 * 1000L
        private const val MP3_SEARCH_BACK = 128L * 1024L
        private const val MP3_SEARCH_FORWARD = 512L * 1024L
    }
}
