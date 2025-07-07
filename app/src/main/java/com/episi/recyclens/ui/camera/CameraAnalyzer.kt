package com.episi.recyclens.ui.camera

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.episi.recyclens.domain.camera.Detector
import androidx.core.graphics.createBitmap

class CameraAnalyzer(
    private val isFrontCamera: Boolean,
    private val detector: Detector
) : ImageAnalysis.Analyzer {

    override fun analyze(imageProxy: ImageProxy) {
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees

        val bitmapBuffer = createBitmap(imageProxy.width, imageProxy.height)

        imageProxy.use { proxy ->
            bitmapBuffer.copyPixelsFromBuffer(proxy.planes[0].buffer)
        }

        val matrix = Matrix().apply {
            postRotate(rotationDegrees.toFloat())
            if (isFrontCamera) {
                postScale(-1f, 1f, bitmapBuffer.width / 2f, bitmapBuffer.height / 2f)
            }
        }

        val rotatedBitmap = Bitmap.createBitmap(
            bitmapBuffer, 0, 0,
            bitmapBuffer.width, bitmapBuffer.height,
            matrix, true
        )

        detector.detect(rotatedBitmap)

        imageProxy.close()
    }
}