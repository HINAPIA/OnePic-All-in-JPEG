package com.goldenratio.onepicdiary.Fragment

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.goldenratio.onepic.JpegViewModel
import com.goldenratio.onepic.PictureModule.Contents.ContentAttribute
import com.goldenratio.onepic.PictureModule.ImageContent
import com.goldenratio.onepic.PictureModule.TextContent
import com.goldenratio.onepicdiary.DiaryModule.LayoutToolModule
import com.goldenratio.onepicdiary.DiaryModule.ViewPagerAdapter
import com.goldenratio.onepicdiary.MagicPictureModule.MagicPictureModule
import com.goldenratio.onepicdiary.R
import com.goldenratio.onepicdiary.databinding.FragmentViewDiaryBinding
import kotlinx.coroutines.*
import java.util.*
import kotlin.collections.ArrayList

class ViewDiaryFragment : Fragment() {

    private lateinit var binding: FragmentViewDiaryBinding
    private val jpegViewModel by activityViewModels<JpegViewModel>()
    private lateinit var imageContent : ImageContent

    private lateinit var textContent: TextContent
    private lateinit var layoutToolModule: LayoutToolModule

    private var month = MutableLiveData<Int>()
    private var day =  MutableLiveData<Int>()

    private lateinit var viewPagerAdapter:ViewPagerAdapter

    private lateinit var magicPictureModule: MagicPictureModule

    val handler = Handler()

    var magicPlaySpeed: Long = 100

    private var isMagicPlay = false
    private var isMagicSetting = false
    private var isViewUnder = false

    private var overlayBitmap = arrayListOf<Bitmap>()

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

            binding.progressBar.visibility = View.VISIBLE

            imageContent = jpegViewModel.jpegMCContainer.value!!.imageContent
            textContent = jpegViewModel.jpegMCContainer.value!!.textContent

            while (!imageContent.checkPictureList) {
                delay(300)
            }

            setDiary()

            withContext(Dispatchers.Main) {

                month.value = textContent.getMonth() + 1
                day.value = textContent.getDay()
                Log.d("Cell Text", "create : ${month.value!!} || ${day.value!!}")

                layoutToolModule.setSubImage(layoutInflater, binding.monthLayout, 12, month.value!!, null, ::month)

                setDayView()
                month.observe(viewLifecycleOwner) { _ ->
                    dateChange()
                    setDayView()
                }
                day.observe(viewLifecycleOwner) { _ ->
                    dateChange()
                }
            }
        }

//        binding.okBtn.setOnClickListener {
//            findNavController().navigate(R.id.action_viewDiaryFragment_to_calendarFragment)
//        }

        binding.deleteBtn.setOnClickListener {
            val key = "2023/${month.value!!-1}/${day.value!!}"
            val editor: SharedPreferences.Editor = jpegViewModel.preferences.edit()
            Log.d("Cell Text", key)
            editor.remove(key) // 삭제할 값의 키를 지정합니다.
            editor.apply()
            findNavController().navigate(R.id.action_viewDiaryFragment_to_calendarFragment)
        }


        return binding.root
    }

    fun setDayView() {
        val cellList = jpegViewModel.diaryCellArrayList
        val dayList = arrayListOf<Int>()

        val calendar = Calendar.getInstance()
        calendar.set(2023, month.value!! -1, 1)

        jpegViewModel.daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (i in 0 until cellList.size) {
            val cell = cellList[i]
            if (cell.month == month.value!! - 1) {
                dayList.add(cell.day)
            }
        }
        CoroutineScope(Dispatchers.Main).launch {
            binding.dayLayout.removeAllViews()
        }
        layoutToolModule.setSubImage(layoutInflater, binding.dayLayout, jpegViewModel.daysInMonth, day.value!!, dayList, ::day)

    }


    fun setDiary() {
        val pictureList = imageContent.pictureList
        val byteArrayList = arrayListOf<ByteArray>()
        for (i in 0 until pictureList.size) {
            try {
                byteArrayList.add(imageContent.getJpegBytes(pictureList[i]))
            } catch (e: IndexOutOfBoundsException) {
                e.printStackTrace()
            }
        }

        CoroutineScope(Dispatchers.Main).launch {
            viewPagerAdapter = ViewPagerAdapter(requireContext())
            viewPagerAdapter.setImageList(byteArrayList)
            binding.viewPager.adapter = viewPagerAdapter
        }

        binding.viewUnberBtn.setOnClickListener {
            isViewUnder = if(isViewUnder) {
                viewOnImageLayout()
                binding.viewUnberBtn.setImageResource(R.drawable.underview_unview)
                false
            } else {
                viewUnderLayout()
                binding.viewUnberBtn.setImageResource(R.drawable.underview_view)
                true
            }
        }

        viewOnImageLayout()

        setMagic()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun dateChange() {
        for(i in 0 until overlayBitmap.size) {
            overlayBitmap[i].recycle()
        }

        val cellList = jpegViewModel.diaryCellArrayList
        var isCell = false

        Log.d("Cell Text", "current Day 22: ${month.value!! -1} | ${day.value}")
        for (i in 0 until cellList.size) {
            val cell = cellList[i]

            Log.d("Cell Text", "dateChange: ${cell.month} | ${cell.day}")
            if (cell.month == month.value!! - 1 && cell.day == day.value) {
                isCell = true
                binding.addBtn.visibility = View.GONE
                jpegViewModel.jpegMCContainer.value!!.init()
                jpegViewModel.setCurrentMCContainer(cell.currentUri)

                while (!imageContent.checkPictureList) {

                }
                setDiary()
                break
            }
        }
        Log.d("Cell Text","isCell : $isCell")
        if(!isCell) {
            binding.viewPager.adapter = ViewPagerAdapter(requireContext())

            binding.UnderImageLayout.visibility = View.GONE
            binding.OnImageLayout.visibility = View.INVISIBLE
            binding.magicBtn.visibility = View.GONE

            binding.addBtn.visibility = View.VISIBLE
            binding.addBtn.setOnClickListener {
                jpegViewModel.selectMonth = month.value!! - 1
                jpegViewModel.selectDay = day.value!!

                findNavController().navigate(R.id.action_viewDiaryFragment_to_addDiaryFragment)
            }
        }
    }

    private fun magicPictureRun(ovelapBitmap: ArrayList<Bitmap>) {
        CoroutineScope(Dispatchers.Main).launch {

            while(!isMagicSetting) {

            }
            binding.progressBar.visibility = View.GONE

            var currentImageIndex = 0
            var increaseIndex = 1

            val runnable = object : java.lang.Runnable {
                override fun run() {
                    if (ovelapBitmap.size > 0) {
                        binding.mainView.setImageBitmap(ovelapBitmap[currentImageIndex])
                        //currentImageIndex++

                        currentImageIndex += increaseIndex

                        if (currentImageIndex >= ovelapBitmap.size - 1) {
                            //currentImageIndex = 0
                            increaseIndex = -1
                        } else if (currentImageIndex <= 0) {
                            increaseIndex = 1
                        }
                        handler.postDelayed(this, magicPlaySpeed)
                    }
                }
            }
            handler.postDelayed(runnable, magicPlaySpeed)
        }
    }

    fun viewUnderLayout() {
        CoroutineScope(Dispatchers.Main).launch {
            binding.OnImageLayout.visibility = View.GONE
            binding.UnderImageLayout.visibility = View.VISIBLE

            binding.titleTextValue.text = textContent.getTitle()
            binding.contentTextValue.text = textContent.getContent()
        }
    }

    fun viewOnImageLayout() {
        CoroutineScope(Dispatchers.Main).launch {
            binding.OnImageLayout.visibility = View.VISIBLE
            binding.UnderImageLayout.visibility = View.GONE

            binding.titleTextValueOnImage.text = textContent.getTitle()
            binding.contentTextValueOnImage.text = textContent.getContent()

            binding.progressBar.visibility = View.GONE
        }
    }

    fun setMagic() {
        CoroutineScope(Dispatchers.IO).launch {
            if (imageContent.checkAttribute(ContentAttribute.magic)) {

                withContext(Dispatchers.Main) {
                    binding.magicBtn.visibility = View.VISIBLE
                }
                magicPictureModule = MagicPictureModule(imageContent)

                withContext(Dispatchers.Default) {
                    overlayBitmap = magicPictureModule.magicPictureProcessing()
                    isMagicSetting = true
                }

                binding.magicBtn.setOnClickListener {
                    if (!isMagicPlay) {
                        CoroutineScope(Dispatchers.Main).launch {
                            binding.magicBtn.setImageResource(R.drawable.parse)

                            binding.progressBar.visibility = View.VISIBLE
                            binding.viewPager.visibility = View.GONE
                            binding.mainView.visibility = View.VISIBLE
                        }
                        isMagicPlay = true

                        magicPictureRun(overlayBitmap)
                    } else {
                        handler.removeCallbacksAndMessages(null)

                        CoroutineScope(Dispatchers.Main).launch {
                            binding.magicBtn.setImageResource(R.drawable.play)

                            binding.progressBar.visibility = View.GONE
                            binding.viewPager.visibility = View.VISIBLE
                            binding.mainView.visibility = View.GONE
                        }
                        isMagicPlay = false
                    }
                }
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    binding.magicBtn.visibility = View.INVISIBLE
                }
            }
        }
    }
}
