package com.chathuranga.shan.imagereaderissue.framgment

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.PixelFormat
import android.graphics.RectF
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.chathuranga.shan.imagereaderissue.CameraViewModel
import com.chathuranga.shan.imagereaderissue.R
import com.chathuranga.shan.imagereaderissue.databinding.FragmentBackFacingCameraBinding
import com.chathuranga.shan.imagereaderissue.utilities.*
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BackFacingCameraFragment : CameraBaseFragment(R.layout.fragment_back_facing_camera),
    ObjectInBoundCheck {

    private lateinit var binding: FragmentBackFacingCameraBinding
    private lateinit var objectedDetectionOptions: ObjectDetectorOptions
    private lateinit var objectDetector: ObjectDetector
    private lateinit var safeArea : RectF

    private var previewSize: Size? = null
    private var captureSize: Size? = null
    private var analyzeImageSize: Size? = null
    private var isImageProcessing = false
    private var adjustedCameraPreviewWidth = 0
    private var adjustedCameraPreviewHeight = 0

    private val viewModel: CameraViewModel by activityViewModels()

    private val navigationController: NavController by lazy {
        Navigation.findNavController(requireView())
    }

    private val overlaySurfaceHolder = ObjectDetectionOverlaySurfaceHolder()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        binding = FragmentBackFacingCameraBinding.bind(view)

        initialization()
        onClickCameraFabButton()
    }

    private fun initialization() {

        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                binding.cameraSurfaceView.visibility = View.VISIBLE
                setupCameraIdWithView()
            } else {
                // Permission request was denied.
                Snackbar.make(
                    binding.root,
                    R.string.camera_permission_denied, Snackbar.LENGTH_SHORT
                )
                    .setAction(R.string.ok) {
                        navigationController.navigateUp()
                    }.show()

            }
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            setupCameraIdWithView()
        } else {
            binding.cameraSurfaceView.visibility = View.GONE
            requestCameraPermission()
        }

        ORIENTATIONS.append(Surface.ROTATION_0, 90)
        ORIENTATIONS.append(Surface.ROTATION_90, 0)
        ORIENTATIONS.append(Surface.ROTATION_180, 270)
        ORIENTATIONS.append(Surface.ROTATION_270, 180)

        objectedDetectionOptions = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .build()

        objectDetector = ObjectDetection.getClient(objectedDetectionOptions)

        binding.cameraOverlayView.setZOrderMediaOverlay(true)
        val cameraOverlaySurfaceHolder = binding.cameraOverlayView.holder
        cameraOverlaySurfaceHolder.addCallback(overlaySurfaceHolder)
        cameraOverlaySurfaceHolder.setFormat(PixelFormat.TRANSLUCENT)
    }

    /**
     * This function is responsible for selecting appropriate camera id for the user case passing
     * it into [selectSuitableCameraID] to start initialize camera view
     */
    private fun setupCameraIdWithView() {
        cameraId = selectSuitableCameraID(CameraCharacteristics.LENS_FACING_BACK)
        if (cameraId != null) {
            characteristics = cameraManager.getCameraCharacteristics(cameraId!!)

            // Used to rotate the output media to match device orientation
            relativeOrientation = OrientationLiveData(requireContext(), characteristics).apply {
                observe(viewLifecycleOwner, { orientation ->
                    Log.d(TAG,
                        "Orientation changed: $orientation"
                    )
                })
            }

            settingUpCameraView(cameraId!!)

        } else {
            Snackbar.make(
                binding.root, R.string.camera_id_find_error,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.ok) {
                    navigationController.navigateUp()
                }.show()
        }
    }

    /**
     * This function is responsible for setting camera preview into Surface view with the correct
     * preview size match the phone screen
     */
    private fun settingUpCameraView(selectedCameraId: String) {

        binding.cameraSurfaceView.holder.addCallback(object : SurfaceHolder.Callback {

            override fun surfaceChanged(
                holder: SurfaceHolder, format: Int,
                width: Int, height: Int
            ) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                if (isCameraInitialized) {
                    camera.close()
                }
            }

            override fun surfaceCreated(holder: SurfaceHolder) {

                // Selects appropriate preview size and configures view finder
                previewSize = getPreviewOutputSize(
                    binding.cameraSurfaceView.display,
                    characteristics,
                    SurfaceHolder::class.java
                )

                if (previewSize != null) {

                    Log.d(TAG,
                        "View finder size: " +
                                "${binding.cameraSurfaceView.width} x " +
                                "${binding.cameraSurfaceView.height}"
                    )

                    Log.d(TAG, "Selected preview size: $previewSize")

                    binding.cameraSurfaceView.setAspectRatio(
                        previewSize!!.width,
                        previewSize!!.height
                    )

                    val cutoutTop = binding.cameraCutoutView.top + DisplayUtils.pxFromDp(
                        requireContext(),
                        16f
                    )
                    val cutoutLeft = binding.cameraCutoutView.left + DisplayUtils.pxFromDp(
                        requireContext(),
                        16f
                    )
                    val cutoutRight =
                        cutoutLeft + binding.cameraCutoutView.measuredWidth - DisplayUtils.pxFromDp(
                            requireContext(),
                            32f
                        )
                    val cutoutBottom =
                        cutoutTop + binding.cameraCutoutView.measuredHeight - DisplayUtils.pxFromDp(
                            requireContext(),
                            32f
                        )

                    safeArea = RectF()
                    safeArea.top = cutoutTop
                    safeArea.left = cutoutLeft
                    safeArea.right = cutoutRight
                    safeArea.bottom = cutoutBottom

                    overlaySurfaceHolder.previewSize = previewSize!!
                    overlaySurfaceHolder.safeAreaBound = safeArea

                    // To ensure that size is set, initialize camera in the view's thread
                    view?.post { configureCamera(selectedCameraId) }
                }
            }

        })

    }

    /** This function is responsible for creating camera session, defining target to receive
     * camera frames and setting up with appropriate parameters
     */
    private fun configureCamera(selectedCameraId: String) {

        lifecycleScope.launch(Dispatchers.Main) {

            camera = openCamera(cameraManager, selectedCameraId, cameraHandler)

            val previewFraction = DisplayUtils
                .asFraction(previewSize!!.width.toLong(), previewSize!!.height.toLong())

            val previewSizeMatchingCaptureSize = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
                    .getOutputSizes(ImageFormat.JPEG)
                    .filter { DisplayUtils
                            .asFraction(it.width.toLong(),it.height.toLong()) == previewFraction }
                    .filter { it.width.toLong() == previewSize!!.width.toLong() &&
                            it.height.toLong() == previewSize!!.height.toLong()}
                    .sortedBy { it.height * it.width}
                    .reversed()

            // Select capture size which matches the preview size which or select the highest
            // capture size camera support which has the same aspect ratio as the preview size
            captureSize = if(previewSizeMatchingCaptureSize.isNotEmpty()){
                previewSizeMatchingCaptureSize.first()
            }else{
                characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
                        .getOutputSizes(ImageFormat.JPEG)
                        .filter { DisplayUtils.asFraction(it.width.toLong(),it.height.toLong()) == previewFraction }
                        .sortedBy { it.height * it.width}
                        .reversed()
                        .first()
            }

            analyzeImageSize = characteristics
                .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
                .getOutputSizes(ImageFormat.YUV_420_888)
                .filter { DisplayUtils.asFraction(it.width.toLong(), it.height.toLong()) == previewFraction }
                .sortedBy { it.height * it.width}
                .first()

            if (captureSize != null) {

                captureImageReader = ImageReader.newInstance(
                    captureSize!!.width, captureSize!!.height, ImageFormat.JPEG, IMAGE_BUFFER_SIZE
                )

                analyzeImageReader = ImageReader.newInstance(
                    analyzeImageSize!!.width,
                    analyzeImageSize!!.height,
                    ImageFormat.YUV_420_888,
                    IMAGE_BUFFER_SIZE
                )
                Log.d(TAG, "Selected capture size: $captureSize")
                Log.d(TAG, "Selected image analyze size: $analyzeImageSize")


                val targets = listOf(
                    binding.cameraSurfaceView.holder.surface,
                    captureImageReader.surface,
                    analyzeImageReader.surface
                )

                session = createCaptureSession(camera, targets, cameraHandler)

                val captureBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                captureBuilder.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )
                captureBuilder.addTarget(binding.cameraSurfaceView.holder.surface)
                captureBuilder.addTarget(analyzeImageReader.surface)

                session.setRepeatingRequest(captureBuilder.build(), null, cameraHandler)

                adjustedCameraPreviewWidth= binding.cameraSurfaceView.adjustedWidth
                adjustedCameraPreviewHeight = binding.cameraSurfaceView.adjustedHeight

                overlaySurfaceHolder.analyzedSize = analyzeImageSize!!
                overlaySurfaceHolder.surfaceTop = binding.cameraSurfaceView.top
                overlaySurfaceHolder.surfaceWidth = adjustedCameraPreviewWidth
                overlaySurfaceHolder.surfaceHeight = adjustedCameraPreviewHeight
                overlaySurfaceHolder.objectInBoundCheck = this@BackFacingCameraFragment

                readImageBuffer()
            }
        }
    }

    private fun readImageBuffer() {

        val rotationCompensation = relativeOrientation.value ?: defaultOrientation()

        analyzeImageReader.setOnImageAvailableListener({ reader ->

            if (!isImageProcessing) {

                val processingImage = reader.acquireLatestImage()
                isImageProcessing = true

                Log.d(tag, "Image available in queue: ${processingImage?.timestamp}")

                val inputImage = InputImage.fromMediaImage(processingImage!!, rotationCompensation)

                objectDetector.process(inputImage)
                    .addOnSuccessListener { detectedObjects ->

                        isImageProcessing = false

                        for (detectedObject in detectedObjects) {
                            val boundingBox = detectedObject.boundingBox
                            val trackingId = detectedObject.trackingId
                            Log.d(TAG, "bounding box top ${boundingBox.top}")
                            Log.d(TAG, "bounding box left ${boundingBox.left}")
                            Log.d(TAG, "bounding box bottom ${boundingBox.bottom}")
                            Log.d(TAG, "bounding box right ${boundingBox.right}")
                            Log.d(TAG, "track id $trackingId")

                            overlaySurfaceHolder.repositionBound(boundingBox)
                            binding.cameraOverlayView.invalidate()
                        }

                    }
                    .addOnFailureListener { e ->
                        print("tracking exception $e")
                        isImageProcessing = false
                    }
                    .addOnCompleteListener {
                        processingImage.close()
                    }

            } else {
                Log.d(tag, "Frame Skipped")
            }

        }, analyzeImageReaderHandler)

    }

    override fun isObjectInbound(objectCoordinates: RectF) {

        if(objectCoordinates.top > safeArea.top &&
            objectCoordinates.bottom < safeArea.bottom &&
            objectCoordinates.left > safeArea.left &&
            objectCoordinates.right < safeArea.right){

            Log.d(TAG, "object is in safe area")
        }else{
            Log.d(TAG, "object is out of safe area")
        }
    }


    private fun onClickCameraFabButton() {
        binding.cameraFabButton.setOnClickListener {

            it.isEnabled = false

            lifecycleScope.launch(Dispatchers.IO) {

                takePhoto().use { result ->

                    Log.d(TAG, "Result received: $result")

                    // Save the result to disk
                    val output = saveResult(result)
                    Log.d(TAG, "Image saved: ${output.absolutePath}")

                    viewModel.documentFrontImageFilePath = output.absolutePath
                    viewModel.adjustedViewWidth.postValue(adjustedCameraPreviewWidth)
                    viewModel.adjustedViewHeight.postValue(adjustedCameraPreviewHeight)

                    // If the result is a JPEG file, update EXIF metadata with orientation info
                    if (output.extension == "jpg") {

                        val exif = ExifInterface(output.absolutePath)
                        exif.setAttribute(
                            ExifInterface.TAG_ORIENTATION, result.orientation.toString()
                        )
                        exif.saveAttributes()
                        Log.d(TAG, "EXIF metadata saved: ${output.absolutePath}")
                    }
                }

                it.post {
                    it.isEnabled = true
                    navigationController.navigate(R.id.to_back_facing_camera_result)
                }
            }
        }
    }

    /**
     * Request camera permission if it is not given
     */
    private fun requestCameraPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            Snackbar.make(binding.root, R.string.camera_access_required, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok) {
                    requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                }.show()
        } else {
            // You can directly ask for the permission.
            Snackbar.make(
                binding.root,
                R.string.camera_permission_not_available,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.ok) {
                    requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                }.show()
        }
    }

}