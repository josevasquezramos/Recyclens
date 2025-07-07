package com.episi.recyclens.domain.camera

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.BufferedReader
import java.io.InputStreamReader
import androidx.core.graphics.scale

class Detector(
    private val context: Context,
    private val modelPath: String,
    private val labelPath: String,
    private val listener: DetectorListener
) {
    @Volatile
    private var isClosed = false

    private var interpreter: Interpreter? = null
    private var labels = mutableListOf<String>()

    private var tensorWidth = 0
    private var tensorHeight = 0
    private var numChannel = 0
    private var numElements = 0

    private val imageProcessor = ImageProcessor.Builder()
        .add(NormalizeOp(INPUT_MEAN, INPUT_STD))
        .add(CastOp(INPUT_TYPE))
        .build()

    fun setup() {
        val model = FileUtil.loadMappedFile(context, modelPath)
        val options = Interpreter.Options().apply {
            numThreads = 4
        }
        interpreter = Interpreter(model, options)

        interpreter?.getInputTensor(0)?.shape()?.let {
            tensorWidth = it[1]
            tensorHeight = it[2]
        }

        interpreter?.getOutputTensor(0)?.shape()?.let {
            numChannel = it[1]
            numElements = it[2]
        }

        context.assets.open(labelPath).use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).useLines { lines ->
                lines.forEach { labels.add(it) }
            }
        }
    }

    fun clear() {
        synchronized(this) {
            isClosed = true
            interpreter?.close()
            interpreter = null
        }
    }

    fun detect(frame: Bitmap) {
        val localInterpreter: Interpreter
        synchronized(this) {
            if (isClosed || interpreter == null) return
            localInterpreter = interpreter!!
        }

        try {
            var inferenceTime = SystemClock.uptimeMillis()

            val resized = frame.scale(tensorWidth, tensorHeight, false)
            val tensorImage = TensorImage(DataType.FLOAT32).apply { load(resized) }
            val imageBuffer = imageProcessor.process(tensorImage).buffer

            val output = TensorBuffer.createFixedSize(
                intArrayOf(1, numChannel, numElements),
                OUTPUT_TYPE
            )

            localInterpreter.run(imageBuffer, output.buffer)

            val boxes = bestBoxes(output.floatArray)
            inferenceTime = SystemClock.uptimeMillis() - inferenceTime

            if (boxes == null) {
                listener.onEmptyDetect()
            } else {
                listener.onDetect(boxes, inferenceTime)
            }

        } catch (e: Exception) {
            if (!isClosed) {
                e.printStackTrace()
            }
        }
    }

    private fun bestBoxes(array: FloatArray): List<BoundingBox>? {
        val boxes = mutableListOf<BoundingBox>()

        for (i in 0 until numElements) {
            var maxConf = -1f
            var classIdx = -1
            for (j in 4 until numChannel) {
                val idx = i + numElements * j
                if (array[idx] > maxConf) {
                    maxConf = array[idx]
                    classIdx = j - 4
                }
            }

            if (maxConf > CONF_THRESHOLD) {
                val cx = array[i]
                val cy = array[i + numElements]
                val w = array[i + numElements * 2]
                val h = array[i + numElements * 3]

                val x1 = cx - w / 2f
                val y1 = cy - h / 2f
                val x2 = cx + w / 2f
                val y2 = cy + h / 2f

                if (x1 < 0f || y1 < 0f || x2 > 1f || y2 > 1f) continue

                boxes.add(
                    BoundingBox(
                        x1, y1, x2, y2, cx, cy, w, h,
                        maxConf, classIdx, labels[classIdx]
                    )
                )
            }
        }

        return if (boxes.isEmpty()) null else applyNMS(boxes)
    }

    private fun applyNMS(boxes: List<BoundingBox>): MutableList<BoundingBox> {
        val sorted = boxes.sortedByDescending { it.cnf }.toMutableList()
        val selected = mutableListOf<BoundingBox>()

        while (sorted.isNotEmpty()) {
            val top = sorted.removeAt(0)
            selected.add(top)

            val iterator = sorted.iterator()
            while (iterator.hasNext()) {
                val next = iterator.next()
                if (iou(top, next) >= IOU_THRESHOLD) iterator.remove()
            }
        }

        return selected
    }

    private fun iou(a: BoundingBox, b: BoundingBox): Float {
        val x1 = maxOf(a.x1, b.x1)
        val y1 = maxOf(a.y1, b.y1)
        val x2 = minOf(a.x2, b.x2)
        val y2 = minOf(a.y2, b.y2)

        val interArea = maxOf(0f, x2 - x1) * maxOf(0f, y2 - y1)
        val unionArea = a.w * a.h + b.w * b.h - interArea

        return interArea / unionArea
    }

    companion object {
        private const val INPUT_MEAN = 0f
        private const val INPUT_STD = 255f
        private val INPUT_TYPE = DataType.FLOAT32
        private val OUTPUT_TYPE = DataType.FLOAT32
        private const val CONF_THRESHOLD = 0.3f
        private const val IOU_THRESHOLD = 0.5f
    }
}