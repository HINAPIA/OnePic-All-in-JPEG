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
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.goldenratio.onepic.JpegViewModel
import com.goldenratio.onepic.PictureModule.ImageContent
import com.goldenratio.onepic.PictureModule.TextContent
import com.goldenratio.onepicdiary.DiaryModule.LayoutToolModule
import com.goldenratio.onepicdiary.R
import com.goldenratio.onepicdiary.databinding.FragmentViewDiaryBinding
import kotlinx.coroutines.*

class ViewDiaryFragment : Fragment() {

    private lateinit var binding: FragmentViewDiaryBinding
    private val jpegViewModel by activityViewModels<JpegViewModel>()
    private lateinit var imageContent : ImageContent
    private lateinit var textContent : TextContent
    private lateinit var layoutToolModule: LayoutToolModule

    private var month = MutableLiveData<Int>()
    private var day =  MutableLiveData<Int>()

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

            imageContent = jpegViewModel.jpegMCContainer.value!!.imageContent
            textContent = jpegViewModel.jpegMCContainer.value!!.textContent

            while (!imageContent.checkPictureList) {
                delay(300)
            }

            Log.d("Cell Text", "@@@@@@-> ${textContent.getMonth()} || ${textContent.getDay()}")

            setDiary()

            withContext(Dispatchers.Main) {

                month.value = textContent.getMonth()
                day.value = textContent.getDay()

                layoutToolModule.setSubDate(
                    layoutInflater,
                    12,
                    binding.monthLayout,
                    month.value!!,
                    ::month
                )
                layoutToolModule.setSubDate(
                    layoutInflater,
                    jpegViewModel.daysInMonth,
                    binding.dayLayout,
                    day.value!!,
                    ::day
                )

                month.observe(viewLifecycleOwner) { value ->
                    dateChange()
                }
                day.observe(viewLifecycleOwner) { value ->
                    dateChange()
                }
            }
        }

        binding.okBtn.setOnClickListener {
            findNavController().navigate(R.id.action_viewDiaryFragment_to_calendarFragment)
        }

        binding.deleteBtn.setOnClickListener {
            val key = "2023/${textContent.getMonth() + 1}/${textContent.getDay()}"
            val editor: SharedPreferences.Editor = jpegViewModel.preferences.edit()
            editor.remove(key) // 삭제할 값의 키를 지정합니다.
            editor.apply()
            findNavController().navigate(R.id.action_viewDiaryFragment_to_calendarFragment)
        }

        return binding.root
    }

    @SuppressLint("MissingInflatedId")
    fun setSubImage(view: LinearLayout) {

        var currentLayout: ImageView? = null

        CoroutineScope(Dispatchers.Default).launch{
            while(!imageContent.checkPictureList) {
                delay(300)
            }

            val pictureList = imageContent.pictureList

            for(i in 1 until  pictureList.size) {
                val subLayout =
                    layoutInflater.inflate(R.layout.sub_image, null)

                val imageView = subLayout.findViewById<ImageView>(R.id.imageView)

                withContext(Dispatchers.Main) {
                    Glide.with(imageView)
                        .load(imageContent.getJpegBytes(pictureList[i]))
                        .into(imageView)
                }

                subLayout.setOnClickListener {
                    //
                }

                withContext(Dispatchers.Main) {
                    // main activity에 만들어둔 scrollbar 속 layout의 아이디를 통해 해당 layout에 넣기
                    view.addView(subLayout)
                }
            }
        }
    }

    fun setDiary() {
        CoroutineScope(Dispatchers.Main).launch {
            Glide.with(binding.viewerMainView)
                .load(imageContent.getJpegBytes(imageContent.mainPicture))
                .into(binding.viewerMainView)

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
            binding.viewerMainView.setImageDrawable(null)
        }
    }
}