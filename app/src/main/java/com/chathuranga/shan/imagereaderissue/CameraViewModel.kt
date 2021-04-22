package com.chathuranga.shan.imagereaderissue

import android.graphics.RectF
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CameraViewModel: ViewModel() {

    var isDocumentValid = false
    var documentFrontImageFilePath = ""
    var selfieImageFilePath = ""

    var previewSizeToCaptureSizeScaleFactor  = 0.0f
    var safeAreaRect = RectF(0f,0f,0f,0f)

    val adjustedViewWidth = MutableLiveData<Int>()
    val adjustedViewHeight = MutableLiveData<Int>()
}