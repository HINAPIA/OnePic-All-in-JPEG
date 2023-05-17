package com.goldenratio.onepicdiary.Fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.goldenratio.onepic.JpegViewModel
import com.goldenratio.onepic.PictureModule.ImageContent
import com.goldenratio.onepicdiary.R
import com.goldenratio.onepicdiary.databinding.FragmentViewDiaryBinding
import kotlinx.coroutines.*

class ViewDiaryFragment : Fragment() {

    private lateinit var binding: FragmentViewDiaryBinding
    private val jpegViewModel by activityViewModels<JpegViewModel>()
    private lateinit var imageContent : ImageContent

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment

        binding = FragmentViewDiaryBinding.inflate(inflater, container, false)

        imageContent = jpegViewModel.jpegMCContainer.value!!.imageContent

        CoroutineScope(Dispatchers.Default).launch {
            while (!imageContent.checkPictureList) {
                delay(300)
            }

            // 파일을 parsing해서 PictureContainer로 바꾸는 함수 호출
            // 메인 이미지 설정
            withContext(Dispatchers.Main) {
                Glide.with(binding.viewerMainView)
                    .load(imageContent.getJpegBytes(imageContent.mainPicture))
                    .into(binding.viewerMainView)
            }
        }

        binding.okBtn.setOnClickListener {
            jpegViewModel.currentUri = null
            findNavController().navigate(R.id.action_viewDiaryFragment_to_calendarFragment)
        }

        return binding.root
    }


}