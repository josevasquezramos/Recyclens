package com.episi.recyclens.domain.camera

interface DetectorListener {
    fun onEmptyDetect()
    fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long)
}