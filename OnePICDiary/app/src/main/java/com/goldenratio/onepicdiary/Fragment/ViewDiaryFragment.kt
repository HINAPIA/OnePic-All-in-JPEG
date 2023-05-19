package com.goldenratio.onepicdiary.Fragment

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.goldenratio.onepic.JpegViewModel
import com.goldenratio.onepic.PictureModule.ImageContent
import com.goldenratio.onepic.PictureModule.TextContent
import com.goldenratio.onepicdiary.DiaryModule.LayoutToolModule
import com.goldenratio.onepicdiary.DiaryModule.ViewPagerAdapter
import com.goldenratio.onepicdiary.R
import com.goldenratio.onepicdiary.databinding.FragmentViewDiaryBinding
import kotlinx.coroutines.*

class ViewDiaryFragment : Fragment() {

    private lateinit var binding: FragmentViewDiaryBinding
    private val jpegViewModel by activityViewModels<JpegViewModel>()
    private lateinit var imageContent : ImageContent

    private lateinit var textContent: TextContent
    private lateinit var layoutToolModule: LayoutToolModule

    private var month = MutableLiveData<Int>()
    private var day =  MutableLiveData<Int>()

    private lateinit var viewPagerAdapter:ViewPagerAdapter

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment

        binding = FragmentViewDiaryBinding.inflate(inflater, container, false)

        layoutToolModule = LayoutToolModule()

        CoroutineScope(Dispatchers.Default).launch {
            while (!imageContent.checkPictureList) {
                delay(300)
            }

            imageContent = jpegViewModel.jpegMCContainer.value!!.imageContent
            textContent = jpegViewModel.jpegMCContainer.value!!.textContent

            Log.d("Cell Text", "@@@@@@-> ${textContent.getMonth()} || ${textContent.getDay()}")
//            binding.dateText.text = "2023년 ${textContent.getMonth()+1}월 ${textContent.getDay()}일"

        }

        binding.okBtn.setOnClickListener {
            findNavController().navigate(R.id.action_viewDiaryFragment_to_calendarFragment)
        }

        binding.deleteBtn.setOnClickListener {
            val key = "2023/${textContent.getMonth()}/${textContent.getDay()}"
            val editor: SharedPreferences.Editor = jpegViewModel.preferences.edit()
            editor.remove(key) // 삭제할 값의 키를 지정합니다.
            editor.apply()
            findNavController().navigate(R.id.action_viewDiaryFragment_to_calendarFragment)
        }
        return binding.root
    }


    fun setDiary() {
        CoroutineScope(Dispatchers.Main).launch {
            val pictureList = imageContent.pictureList
            val byteArrayList = arrayListOf<ByteArray>()
            for (i in 0 until pictureList.size) {
                byteArrayList.add(imageContent.getJpegBytes(pictureList[i]))
            }

            viewPagerAdapter = ViewPagerAdapter(requireContext())
            viewPagerAdapter.setImageList(byteArrayList)
            binding.viewPager.adapter = viewPagerAdapter

            binding.titleTextValue.text = textContent.getTitle()
            binding.contentTextValue.text = textContent.getContent()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun dateChange() {
        val cellList = jpegViewModel.diaryCellArrayList
        var isCell = false

        for (i in 0 until cellList.size) {
            val cell = cellList[i]

            if (cell.month == month.value && cell.day == day.value) {
                isCell = true
                jpegViewModel.jpegMCContainer.value!!.init()
                jpegViewModel.setCurrentMCContainer(cell.currentUri)

                CoroutineScope(Dispatchers.Default).launch {
                    while (!imageContent.checkPictureList) {
                        delay(300)
                    }
                    setDiary()
                }
                break
            }
        }
        if(!isCell) {
            binding.titleTextValue.text = ""
            binding.contentTextValue.text = ""
        }
    }
}