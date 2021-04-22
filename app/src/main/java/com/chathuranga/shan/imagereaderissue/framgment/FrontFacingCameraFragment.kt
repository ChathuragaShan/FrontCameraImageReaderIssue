package com.chathuranga.shan.imagereaderissue.framgment

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.PixelFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.media.ImageReader
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.*
import androidx.fragment.app.Fragment
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.chathuranga.shan.imagereaderissue.CameraViewModel
import com.chathuranga.shan.imagereaderissue.R
import com.chathuranga.shan.imagereaderissue.databinding.FragmentBlankFacingCameraResultBinding
import com.chathuranga.shan.imagereaderissue.databinding.FragmentFrontFacingCameraBinding
import com.chathuranga.shan.imagereaderissue.utilities.*
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FrontFacingCameraFragment : CameraBaseFragment(R.layout.fragment_front_facing_camera) {

    private lateinit var binding: FragmentFrontFacingCameraBinding

    private val viewModel: CameraViewModel by activityViewModels()

    private val navigationController: NavController by lazy {
        Navigation.findNavController(requireView())
    }

    private var previewSize: Size? = null
    private var captureSize: Size? = null
    private var analyzeImageSize: Size? = null
    private var adjustedCameraPreviewWidth = 0
    private var adjustedCameraPreviewHeight = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        binding = FragmentFrontFacingCameraBinding.bind(view)

        initialization()
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

    }

    /**
     * This function is responsible for selecting appropriate camera id for the user case passing
     * it into [selectSuitableCameraID] to start initialize camera view
     */
    private fun setupCameraIdWithView() {

        cameraId = selectSuitableCameraID(CameraCharacteristics.LENS_FACING_FRONT)
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
                    width: Int, height: Int) {
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
                        SurfaceHolder::class.java)

                if (previewSize != null) {

                    Log.d(TAG,
                            "View finder size: " +
                                    "${binding.cameraSurfaceView.width} x " +
                                    "${binding.cameraSurfaceView.height}"
                    )

                    Log.d(TAG, "Selected preview size: $previewSize")

                    val circleRadius = resources.getDimension(R.dimen.circle_cut_out_radius)

                    binding.cameraSurfaceView.setAspectRatio(previewSize!!.width, previewSize!!.height)

                    // To ensure that size is set, initialize camera in the view's thread
                    view?.post { configureCamera(selectedCameraId) }

                }
            }

        })
    }

    /**
     * This function is responsible for creating camera session, defining target to receive
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
                // Initialize an image reader which will be used to capture still photos
                characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
                        .getOutputSizes(ImageFormat.JPEG)
                        .filter { DisplayUtils.asFraction(it.width.toLong(),it.height.toLong()) == previewFraction }
                        .sortedBy { it.height * it.width}
                        .reversed()
                        .first()
            }

            val cameraZoom = CameraZoom(characteristics)

            // Smallest Image size for fast processing
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
                        analyzeImageReader.surface)

                session = createCaptureSession(camera, targets, cameraHandler)

                adjustedCameraPreviewWidth= binding.cameraSurfaceView.adjustedWidth
                adjustedCameraPreviewHeight = binding.cameraSurfaceView.adjustedHeight

                val captureBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                captureBuilder.addTarget(binding.cameraSurfaceView.holder.surface)
                captureBuilder.addTarget(analyzeImageReader.surface)
                cameraZoom.setZoom(captureBuilder, CameraZoom.DEFAULT_ZOOM_FACTOR)

                session.setRepeatingRequest(captureBuilder.build(), null, cameraHandler)
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

    private fun onClickCameraFabButton() {
        binding.cameraFabButton.setOnClickListener {

            it.isEnabled = false

            lifecycleScope.launch(Dispatchers.IO) {

                takePhoto().use { result ->

                    Log.d(TAG, "Result received: $result")

                    // Save the result to disk
                    val output = saveResult(result)
                    Log.d(TAG, "Image saved: ${output.absolutePath}")

                    val adjustedWidth = binding.cameraSurfaceView.adjustedWidth
                    val adjustedHeight = binding.cameraSurfaceView.adjustedHeight

                    viewModel.selfieImageFilePath = output.absolutePath
                    viewModel.adjustedViewWidth.postValue(adjustedWidth)
                    viewModel.adjustedViewHeight.postValue(adjustedHeight)

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
                    navigationController.navigate(R.id.to_front_facing_result)
                }
            }
        }
    }
}