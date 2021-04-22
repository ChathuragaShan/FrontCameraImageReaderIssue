package com.chathuranga.shan.imagereaderissue.framgment

import android.os.Bundle
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.chathuranga.shan.imagereaderissue.CameraViewModel
import com.chathuranga.shan.imagereaderissue.R
import com.chathuranga.shan.imagereaderissue.databinding.FragmentBlankFacingCameraResultBinding
import com.chathuranga.shan.imagereaderissue.utilities.waitForLayout
import com.squareup.picasso.Picasso
import java.io.File


class BlankFacingCameraResultFragment : CameraBaseFragment(R.layout.fragment_blank_facing_camera_result) {

    private lateinit var binding: FragmentBlankFacingCameraResultBinding

    private val viewModel: CameraViewModel by activityViewModels()

    private val navigationController: NavController by lazy {
        Navigation.findNavController(requireView())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentBlankFacingCameraResultBinding.bind(view)

        initialization()
        onClickContinueButton()
    }

    private fun initialization(){

        val width: Int = viewModel.adjustedViewWidth.value!!
        val height: Int = viewModel.adjustedViewHeight.value!!

        val layoutParams =
                binding.documentPreviewImageView.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.width = width
        layoutParams.height = height
        binding.documentPreviewImageView.layoutParams = layoutParams

        binding.documentPreviewImageView.waitForLayout {

            val file = File(viewModel.documentFrontImageFilePath)

            Picasso.get()
                    .load(file)
                    .resize(width,height)
                    .into(binding.documentPreviewImageView)
        }

    }

    private fun onClickContinueButton(){
        binding.continueButton.setOnClickListener {
            navigationController.navigate(R.id.to_front_camera)
        }
    }



}