package com.goldenratio.onepicdiary.Fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.goldenratio.onepic.JpegViewModel
import com.goldenratio.onepic.PictureModule.ImageContent
import com.goldenratio.onepicdiary.DiaryModule.LayoutToolModule
import com.goldenratio.onepicdiary.R
import com.goldenratio.onepicdiary.databinding.FragmentViewDiaryBinding
import kotlinx.coroutines.*

class ViewDiaryFragment : Fragment() {

    private lateinit var binding: FragmentViewDiaryBinding
    private val jpegViewModel by activityViewModels<JpegViewModel>()
    private lateinit var imageContent : ImageContent
    private lateinit var layoutToolModule: LayoutToolModule

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment

        binding = FragmentViewDiaryBinding.inflate(inflater, container, false)

        layoutToolModule = LayoutToolModule()

        imageContent = jpegViewModel.jpegMCContainer.value!!.imageContent
        val textContent = jpegViewModel.jpegMCContainer.value!!.textContent

        CoroutineScope(Dispatchers.Default).launch {
            while (!imageContent.checkPictureList) {
                delay(300)
            }

            Log.d("Cell Text", "@@@@@@-> ${textContent.getMonth()} || ${textContent.getDay()}")
            binding.dateText.text = "2023년 ${textContent.getMonth()+1}월 ${textContent.getDay()}일"

            // 파일을 parsing해서 PictureContainer로 바꾸는 함수 호출
            // 메인 이미지 설정
            withContext(Dispatchers.Main) {
                Glide.with(binding.viewerMainView)
                    .load(imageContent.getJpegBytes(imageContent.mainPicture))
                    .into(binding.viewerMainView)
            }
            // text 입력 UI에 기존의 텍스트 메시지 띄우기
            withContext(Dispatchers.Main) {
                binding.titleTextValue.text = textContent.getTitle()
                binding.contentTextValue.text = textContent.getContent()
            }
        }

        binding.okBtn.setOnClickListener {
            findNavController().navigate(R.id.action_viewDiaryFragment_to_calendarFragment)
        }

        return binding.root
    }


}