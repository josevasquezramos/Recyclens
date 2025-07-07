package com.episi.recyclens.ui.camera

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.episi.recyclens.domain.camera.BoundingBox

class CameraViewModel : ViewModel() {

    private val _inferenceTime = MutableLiveData<String>()
    val inferenceTime: LiveData<String> = _inferenceTime

    private val _boundingBoxes = MutableLiveData<List<BoundingBox>>()
    val boundingBoxes: LiveData<List<BoundingBox>> = _boundingBoxes

    fun updateResults(boxes: List<BoundingBox>, time: Long) {
        _boundingBoxes.postValue(boxes)
        _inferenceTime.postValue("${time}ms")
    }

    fun clearResults() {
        _boundingBoxes.postValue(emptyList())
        _inferenceTime.postValue("")
    }
}