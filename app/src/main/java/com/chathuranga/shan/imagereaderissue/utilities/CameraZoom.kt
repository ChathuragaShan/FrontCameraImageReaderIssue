package com.chathuranga.shan.imagereaderissue.utilities

import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import androidx.core.math.MathUtils

/**
 * This class is responsible for
 */
class CameraZoom(characteristics: CameraCharacteristics) {

    var hasSupport: Boolean
    private var maxZoom: Float
    private val mCropRegion = Rect()
    private val mSensorSize: Rect? =
        characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)

    companion object {
        const val DEFAULT_ZOOM_FACTOR = 1.0f
    }

    init {
        if (mSensorSize == null) {
            maxZoom = DEFAULT_ZOOM_FACTOR
            hasSupport = false
        }
        val value = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)
        maxZoom = if (value == null || value < DEFAULT_ZOOM_FACTOR) DEFAULT_ZOOM_FACTOR else value
        hasSupport = maxZoom.compareTo(DEFAULT_ZOOM_FACTOR) > 0
    }

    fun setZoom(builder: CaptureRequest.Builder, zoom: Float) {
        if (!hasSupport) {
            return
        }
        val newZoom = MathUtils.clamp(zoom, DEFAULT_ZOOM_FACTOR, maxZoom)
        val centerX = mSensorSize!!.width() / 2
        val centerY = mSensorSize.height() / 2
        val deltaX = (0.5f * mSensorSize.width() / newZoom).toInt()
        val deltaY = (0.5f * mSensorSize.height() / newZoom).toInt()
        mCropRegion[centerX - deltaX, centerY - deltaY, centerX + deltaX] = centerY + deltaY
        builder.set(CaptureRequest.SCALER_CROP_REGION, mCropRegion)
    }
}