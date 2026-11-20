package com.aistudio.aerofast.trmst.util

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import androidx.compose.ui.graphics.Color
import com.aistudio.aerofast.trmst.data.HeadingAnimation
import com.aistudio.aerofast.trmst.data.HeadingPlan
import com.aistudio.aerofast.trmst.ui.screens.drawHeadingFrame
import java.io.File

/** Pure colour maths, kept apart so it can be tested on the JVM (BT.601, limited range). */
object Yuv {
    fun y(r: Int, g: Int, b: Int) = (((66 * r + 129 * g + 25 * b + 128) shr 8) + 16).coerceIn(0, 255)
    fun u(r: Int, g: Int, b: Int) = (((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128).coerceIn(0, 255)
    fun v(r: Int, g: Int, b: Int) = (((112 * r - 94 * g - 18 * b + 128) shr 8) + 128).coerceIn(0, 255)
}

/**
 * Writes the heading animation to an H.264 MP4 on the phone, offline, with the platform encoder. Frames are drawn
 * with the same renderer as the in-app player and fed to the encoder as YUV with exact timestamps.
 */
object HeadingVideoEncoder {
    const val WIDTH = 720
    const val HEIGHT = 960
    const val FPS = 24

    fun frameCount(plan: HeadingPlan) = (HeadingAnimation.durationSec(plan) * FPS).toInt()

    fun encode(plan: HeadingPlan, partLabel: String, out: File, onProgress: (Float) -> Unit = {}): File {
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, WIDTH, HEIGHT).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            setInteger(MediaFormat.KEY_BIT_RATE, 3_000_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, FPS)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()
        val muxer = MediaMuxer(out.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var track = -1
        var muxing = false
        val info = MediaCodec.BufferInfo()
        val pixels = IntArray(WIDTH * HEIGHT)
        val frames = frameCount(plan)

        fun drain(endOfStream: Boolean) {
            while (true) {
                val idx = codec.dequeueOutputBuffer(info, 10_000)
                when {
                    idx == MediaCodec.INFO_TRY_AGAIN_LATER -> if (!endOfStream) return
                    idx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> { track = muxer.addTrack(codec.outputFormat); muxer.start(); muxing = true }
                    idx >= 0 -> {
                        val buf = codec.getOutputBuffer(idx)
                        if (buf != null && info.size > 0 && muxing && (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                            buf.position(info.offset); buf.limit(info.offset + info.size)
                            muxer.writeSampleData(track, buf, info)
                        }
                        codec.releaseOutputBuffer(idx, false)
                        if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) return
                    }
                }
            }
        }

        try {
            var frame = 0
            while (frame <= frames) {
                val inIdx = codec.dequeueInputBuffer(10_000)
                if (inIdx >= 0) {
                    val pts = frame * 1_000_000L / FPS
                    if (frame == frames) {
                        codec.queueInputBuffer(inIdx, 0, 0, pts, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    } else {
                        val bmp = DrawingExporter.renderBitmap(WIDTH, HEIGHT, Color.Black) {
                            drawHeadingFrame(plan, HeadingAnimation.frameAt(plan, frame / FPS.toDouble()), 30f, partLabel)
                        }
                        bmp.getPixels(pixels, 0, WIDTH, 0, 0, WIDTH, HEIGHT)
                        bmp.recycle()
                        val image = codec.getInputImage(inIdx) ?: error("encoder gave no input image")
                        val (yP, uP, vP) = Triple(image.planes[0], image.planes[1], image.planes[2])
                        val yb = yP.buffer; val ub = uP.buffer; val vb = vP.buffer
                        for (row in 0 until HEIGHT) {
                            for (col in 0 until WIDTH) {
                                val p = pixels[row * WIDTH + col]
                                val r = (p shr 16) and 0xFF; val g = (p shr 8) and 0xFF; val b = p and 0xFF
                                yb.put(row * yP.rowStride + col * yP.pixelStride, Yuv.y(r, g, b).toByte())
                                if (row % 2 == 0 && col % 2 == 0) {
                                    ub.put((row / 2) * uP.rowStride + (col / 2) * uP.pixelStride, Yuv.u(r, g, b).toByte())
                                    vb.put((row / 2) * vP.rowStride + (col / 2) * vP.pixelStride, Yuv.v(r, g, b).toByte())
                                }
                            }
                        }
                        codec.queueInputBuffer(inIdx, 0, WIDTH * HEIGHT * 3 / 2, pts, 0)
                    }
                    frame++
                    onProgress(frame / (frames + 1f))
                }
                drain(endOfStream = false)
            }
            drain(endOfStream = true)
        } finally {
            runCatching { codec.stop() }; codec.release()
            if (muxing) runCatching { muxer.stop() }
            muxer.release()
        }
        return out
    }
}
