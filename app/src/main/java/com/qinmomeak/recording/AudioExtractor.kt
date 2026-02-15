package com.qinmomeak.recording

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaMuxer
import android.media.MediaFormat
import android.net.Uri
import java.io.File
import java.nio.ByteBuffer

class AudioExtractor(private val context: Context) {

    fun extractAudioFromVideo(videoUri: Uri): File? {
        val extractor = MediaExtractor()
        return try {
            val pfd = context.contentResolver.openFileDescriptor(videoUri, "r") ?: return null
            extractor.setDataSource(pfd.fileDescriptor)

            var audioTrackIndex = -1
            var mime: String? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val trackMime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                if (trackMime.startsWith("audio/")) {
                    audioTrackIndex = i
                    mime = trackMime
                    break
                }
            }

            if (audioTrackIndex == -1 || mime == null) return null

            extractor.selectTrack(audioTrackIndex)
            val format = extractor.getTrackFormat(audioTrackIndex)

            // MediaMuxer writes an MP4 container. Use .m4a extension for AAC tracks.
            val ext = when {
                mime.contains("mpeg") || mime.contains("mp3") -> "mp3"
                else -> "m4a"
            }
            val outputFile = File(context.cacheDir, "extract_${System.currentTimeMillis()}.$ext")
            val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerTrack = muxer.addTrack(format)
            muxer.start()

            val maxInputSize = if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
            } else {
                256 * 1024
            }
            val buffer = ByteBuffer.allocate(maxInputSize)
            val info = android.media.MediaCodec.BufferInfo()

            while (true) {
                info.offset = 0
                info.size = extractor.readSampleData(buffer, 0)
                if (info.size < 0) break
                info.presentationTimeUs = extractor.sampleTime
                info.flags = extractor.sampleFlags
                muxer.writeSampleData(muxerTrack, buffer, info)
                extractor.advance()
            }

            muxer.stop()
            muxer.release()
            outputFile
        } catch (_: Exception) {
            null
        } finally {
            extractor.release()
        }
    }

    fun resampleTo16kIfNeeded(input: File): File {
        return input
    }
}

