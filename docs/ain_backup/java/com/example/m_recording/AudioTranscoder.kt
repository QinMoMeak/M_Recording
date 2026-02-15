 if ($args[0].Groups[1].Value -eq '.data') { 'package com.qinmomeak.recording.data' } else { 'package com.qinmomeak.recording' } 

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

class AudioTranscoder(private val context: Context) {

    fun transcodeToWav(uri: Uri): File? {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        return try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
            extractor.setDataSource(pfd.fileDescriptor)
            var audioTrackIndex = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    format = f
                    break
                }
            }
            if (audioTrackIndex == -1 || format == null) return null
            extractor.selectTrack(audioTrackIndex)

            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val pcmBuffer = decodeToPcm(extractor, codec) ?: return null
            val monoPcm = toMono(pcmBuffer, channelCount)
            val resampled = if (sampleRate != 16000) {
                resample16k(monoPcm, sampleRate, 16000)
            } else {
                monoPcm
            }
            writeWav(resampled, 16000, 1)
        } catch (_: Exception) {
            null
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            extractor.release()
        }
    }

    private fun decodeToPcm(extractor: MediaExtractor, codec: MediaCodec): ByteArray? {
        val inputBuffers = codec.inputBuffers
        val outputBuffers = codec.outputBuffers
        val info = MediaCodec.BufferInfo()
        val output = ByteArrayOutputStream()
        var isEOS = false
        while (true) {
            if (!isEOS) {
                val inIndex = codec.dequeueInputBuffer(10000)
                if (inIndex >= 0) {
                    val buffer = inputBuffers[inIndex]
                    val size = extractor.readSampleData(buffer, 0)
                    if (size < 0) {
                        codec.queueInputBuffer(inIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        isEOS = true
                    } else {
                        val timeUs = extractor.sampleTime
                        codec.queueInputBuffer(inIndex, 0, size, timeUs, 0)
                        extractor.advance()
                    }
                }
            }
            val outIndex = codec.dequeueOutputBuffer(info, 10000)
            if (outIndex >= 0) {
                val outBuffer = outputBuffers[outIndex]
                val chunk = ByteArray(info.size)
                outBuffer.position(info.offset)
                outBuffer.limit(info.offset + info.size)
                outBuffer.get(chunk)
                output.write(chunk)
                codec.releaseOutputBuffer(outIndex, false)
                if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) break
            }
            if (outIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // no-op
            }
            if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // no-op
            }
        }
        return output.toByteArray()
    }

    private fun toMono(pcm: ByteArray, channels: Int): ByteArray {
        if (channels <= 1) return pcm
        val samples = pcm.size / 2 / channels
        val mono = ByteArray(samples * 2)
        var inIndex = 0
        var outIndex = 0
        repeat(samples) {
            var sum = 0
            repeat(channels) {
                val lo = pcm[inIndex++].toInt() and 0xFF
                val hi = pcm[inIndex++].toInt()
                val sample = (hi shl 8) or lo
                sum += sample
            }
            val avg = (sum / channels).toShort()
            mono[outIndex++] = (avg.toInt() and 0xFF).toByte()
            mono[outIndex++] = (avg.toInt() shr 8).toByte()
        }
        return mono
    }

    private fun resample16k(pcm: ByteArray, srcRate: Int, dstRate: Int): ByteArray {
        if (srcRate == dstRate) return pcm
        val srcSamples = pcm.size / 2
        val ratio = dstRate.toDouble() / srcRate.toDouble()
        val dstSamples = (srcSamples * ratio).roundToInt()
        val out = ByteArray(dstSamples * 2)
        for (i in 0 until dstSamples) {
            val srcPos = i / ratio
            val idx = srcPos.toInt()
            val frac = srcPos - idx
            val s1 = sampleAt(pcm, idx)
            val s2 = sampleAt(pcm, minOf(idx + 1, srcSamples - 1))
            val interp = (s1 + (s2 - s1) * frac).roundToInt().toShort()
            out[i * 2] = (interp.toInt() and 0xFF).toByte()
            out[i * 2 + 1] = (interp.toInt() shr 8).toByte()
        }
        return out
    }

    private fun sampleAt(pcm: ByteArray, index: Int): Int {
        val i = index * 2
        val lo = pcm[i].toInt() and 0xFF
        val hi = pcm[i + 1].toInt()
        return (hi shl 8) or lo
    }

    private fun writeWav(pcm: ByteArray, sampleRate: Int, channels: Int): File? {
        val outFile = File(context.cacheDir, "transcoded_${System.currentTimeMillis()}.wav")
        val byteRate = sampleRate * channels * 16 / 8
        val totalDataLen = pcm.size + 36
        val header = ByteArray(44)
        fun writeInt(offset: Int, value: Int) {
            header[offset] = (value and 0xFF).toByte()
            header[offset + 1] = (value shr 8 and 0xFF).toByte()
            header[offset + 2] = (value shr 16 and 0xFF).toByte()
            header[offset + 3] = (value shr 24 and 0xFF).toByte()
        }
        fun writeShort(offset: Int, value: Short) {
            header[offset] = (value.toInt() and 0xFF).toByte()
            header[offset + 1] = (value.toInt() shr 8 and 0xFF).toByte()
        }
        // RIFF
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        writeInt(4, totalDataLen)
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        // fmt
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        writeInt(16, 16)
        writeShort(20, 1)
        writeShort(22, channels.toShort())
        writeInt(24, sampleRate)
        writeInt(28, byteRate)
        writeShort(32, (channels * 16 / 8).toShort())
        writeShort(34, 16)
        // data
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        writeInt(40, pcm.size)
        return try {
            FileOutputStream(outFile).use { out ->
                out.write(header)
                out.write(pcm)
            }
            outFile
        } catch (_: Exception) {
            null
        }
    }
}


