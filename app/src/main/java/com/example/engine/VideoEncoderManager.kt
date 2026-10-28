package com.example.engine

import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

object VideoEncoderManager {

    private const val TAG = "VideoEncoderManager"
    private const val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC // H.264
    private const val TIMEOUT_USEC = 10_000L

    /**
     * Encodes a sequence of Bitmaps into a genuine MP4 (H.264) video file using Android MediaCodec & MediaMuxer.
     * Verifies output integrity using MediaMetadataRetriever.
     */
    suspend fun encodeFramesToMp4(
        frames: List<Bitmap>,
        outputFile: File,
        width: Int,
        height: Int,
        fps: Int = 12,
        bitRate: Int = 2_500_000,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Result<File> = withContext(Dispatchers.Default) {
        if (frames.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Frame sequence is empty."))
        }

        // Even dimensions required for H.264
        val alignedWidth = if (width % 2 == 0) width else width - 1
        val alignedHeight = if (height % 2 == 0) height else height - 1

        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var trackIndex = -1
        var muxerStarted = false

        try {
            val format = MediaFormat.createVideoFormat(MIME_TYPE, alignedWidth, alignedHeight).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar)
                setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1) // 1 second keyframe interval
            }

            encoder = MediaCodec.createEncoderByType(MIME_TYPE)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            if (outputFile.exists()) {
                outputFile.delete()
            }
            outputFile.parentFile?.mkdirs()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val bufferInfo = MediaCodec.BufferInfo()
            val frameDurationUs = 1_000_000L / fps
            val yuvBuffer = ByteArray(alignedWidth * alignedHeight * 3 / 2)

            for (i in frames.indices) {
                onProgress(i + 1, frames.size)

                val scaledBitmap = if (frames[i].width == alignedWidth && frames[i].height == alignedHeight) {
                    frames[i]
                } else {
                    Bitmap.createScaledBitmap(frames[i], alignedWidth, alignedHeight, true)
                }

                convertBitmapToYuv420SemiPlanar(scaledBitmap, yuvBuffer, alignedWidth, alignedHeight)
                if (scaledBitmap != frames[i]) {
                    scaledBitmap.recycle()
                }

                val pts = i * frameDurationUs
                val isEos = (i == frames.size - 1)

                // Feed input buffer
                var inputFed = false
                while (!inputFed) {
                    val inputBufferIndex = encoder.dequeueInputBuffer(TIMEOUT_USEC)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = encoder.getInputBuffer(inputBufferIndex)
                        inputBuffer?.clear()
                        inputBuffer?.put(yuvBuffer)
                        val flags = if (isEos) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                        encoder.queueInputBuffer(inputBufferIndex, 0, yuvBuffer.size, pts, flags)
                        inputFed = true
                    }
                }

                // Drain output buffer
                drainEncoder(encoder, muxer, bufferInfo, isEos, trackIndex) { newTrackIndex, isStarted ->
                    trackIndex = newTrackIndex
                    muxerStarted = isStarted
                }
            }

            // Final drain loop to ensure all EOS frames are flushed
            var eosDone = false
            var drainAttempts = 0
            while (!eosDone && drainAttempts < 30) {
                drainAttempts++
                val outIndex = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC)
                if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    break
                } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (!muxerStarted) {
                        trackIndex = muxer.addTrack(encoder.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                } else if (outIndex >= 0) {
                    val encodedData = encoder.getOutputBuffer(outIndex)
                    if (encodedData != null && bufferInfo.size > 0 && muxerStarted) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                    }
                    encoder.releaseOutputBuffer(outIndex, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        eosDone = true
                    }
                }
            }

            encoder.stop()
            encoder.release()
            encoder = null

            if (muxerStarted) {
                muxer.stop()
            }
            muxer.release()
            muxer = null

            // Validate that the output file exists, is non-empty, and has a readable video track
            val validationResult = validateGeneratedMp4(outputFile)
            if (!validationResult.isSuccess) {
                return@withContext Result.failure(Exception("MP4 generated but failed integrity validation: ${validationResult.exceptionOrNull()?.message}"))
            }

            Result.success(outputFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error encoding frames to MP4", e)
            try {
                encoder?.stop()
                encoder?.release()
            } catch (_: Exception) {}
            try {
                muxer?.release()
            } catch (_: Exception) {}
            Result.failure(e)
        }
    }

    private inline fun drainEncoder(
        encoder: MediaCodec,
        muxer: MediaMuxer,
        bufferInfo: MediaCodec.BufferInfo,
        isEos: Boolean,
        currentTrackIndex: Int,
        onMuxerStateChanged: (Int, Boolean) -> Unit
    ) {
        var track = currentTrackIndex
        var muxerStarted = (track >= 0)

        while (true) {
            val outIndex = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC)
            if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                break
            } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (!muxerStarted) {
                    val newFormat = encoder.outputFormat
                    track = muxer.addTrack(newFormat)
                    muxer.start()
                    muxerStarted = true
                    onMuxerStateChanged(track, muxerStarted)
                }
            } else if (outIndex >= 0) {
                val encodedData = encoder.getOutputBuffer(outIndex)
                if (encodedData != null && bufferInfo.size > 0 && muxerStarted) {
                    encodedData.position(bufferInfo.offset)
                    encodedData.limit(bufferInfo.offset + bufferInfo.size)
                    muxer.writeSampleData(track, encodedData, bufferInfo)
                }
                encoder.releaseOutputBuffer(outIndex, false)
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break
                }
            }
        }
    }

    /**
     * Converts an Android Bitmap (ARGB_8888) to NV12 (YUV420SemiPlanar).
     */
    private fun convertBitmapToYuv420SemiPlanar(bitmap: Bitmap, outputYuv: ByteArray, width: Int, height: Int) {
        val frameSize = width * height
        val argb = IntArray(frameSize)
        bitmap.getPixels(argb, 0, width, 0, 0, width, height)

        var yIndex = 0
        var uvIndex = frameSize

        for (j in 0 until height) {
            for (i in 0 until width) {
                val color = argb[j * width + i]
                val r = (color shr 16) and 0xff
                val g = (color shr 8) and 0xff
                val b = color and 0xff

                // ITU-R BT.601 conversion formulas
                val y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                outputYuv[yIndex++] = y.coerceIn(0, 255).toByte()

                if (j % 2 == 0 && i % 2 == 0) {
                    val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                    val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128

                    // NV12 format: U then V
                    outputYuv[uvIndex++] = u.coerceIn(0, 255).toByte()
                    outputYuv[uvIndex++] = v.coerceIn(0, 255).toByte()
                }
            }
        }
    }

    /**
     * Verifies that the produced MP4 file contains a valid playable video track.
     */
    fun validateGeneratedMp4(file: File): Result<Boolean> {
        if (!file.exists() || file.length() < 1024) {
            return Result.failure(IllegalStateException("File does not exist or is too small (${file.length()} bytes)."))
        }

        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)

            if (hasVideo == "yes" && durationStr != null && widthStr != null && heightStr != null) {
                Result.success(true)
            } else {
                Result.failure(IllegalStateException("Media metadata validation missing video track or dimensions."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }
}
