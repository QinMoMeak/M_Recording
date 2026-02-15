 if ($args[0].Groups[1].Value -eq '.data') { 'package com.qinmomeak.recording.data' } else { 'package com.qinmomeak.recording' } 

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaMuxer
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
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(android.media.MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    break
                }
            }

            if (audioTrackIndex == -1) return null

            extractor.selectTrack(audioTrackIndex)
            val format = extractor.getTrackFormat(audioTrackIndex)
            val outputFile = File(context.cacheDir, "extract_${System.currentTimeMillis()}.m4a")
            val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerTrack = muxer.addTrack(format)
            muxer.start()

            val maxInputSize = if (format.containsKey(android.media.MediaFormat.KEY_MAX_INPUT_SIZE)) {
                format.getInteger(android.media.MediaFormat.KEY_MAX_INPUT_SIZE)
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

    // 棰勭暀閲嶉噰鏍峰叆鍙? 鐩爣 16kHz/16bit/mono
    fun resampleTo16kIfNeeded(input: File): File {
        return input
    }
}


