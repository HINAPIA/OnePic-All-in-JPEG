package com.goldenratio.onepicdiary.Fragment

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.goldenratio.onepic.JpegViewModel
import com.goldenratio.onepic.PictureModule.Contents.ContentAttribute
import com.goldenratio.onepicdiary.DiaryModule.DiaryCellData
import com.goldenratio.onepicdiary.DiaryModule.LayoutToolModule
import com.goldenratio.onepicdiary.R
import com.goldenratio.onepicdiary.databinding.FragmentAddDiaryBinding
import kotlinx.coroutines.*
import java.util.*


class AddDiaryFragment : Fragment() {

    private var imageUri: Uri? = null
    private lateinit var binding: FragmentAddDiaryBinding
    private val jpegViewModel by activityViewModels<JpegViewModel>()
    private lateinit var layoutToolModule : LayoutToolModule

    private var month = MutableLiveData<Int>()
    private var day =  MutableLiveData<Int>()

    private val PICK_IMAGE_REQUEST = 1

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentAddDiaryBinding.inflate(inflater, container, false)

        binding.contentTextField.setHorizontallyScrolling(false)
        binding.contentTextField.maxLines = Int.MAX_VALUE

        // 취소 버튼 클릭 시
        binding.cancleBtn.setOnClickListener {
            findNavController().navigate(com.goldenratio.onepicdiary.R.id.action_addDiaryFragment_to_calendarFragment)
        }

       val calendar = Calendar.getInstance()

        month.value = calendar.get(Calendar.MONTH) + 1
        day.value = calendar.get(Calendar.DATE)

        layoutToolModule = LayoutToolModule()

        layoutToolModule.setSubDate(layoutInflater, 12, binding.monthLayout, month.value!!, ::month)
        layoutToolModule.setSubDate(layoutInflater, jpegViewModel.daysInMonth, binding.dayLayout, day.value!!, ::day)

        // 저장 버튼 클릭 시
        binding.saveBtn.setOnClickListener {
            val year = 2023
            val month = Integer.parseInt((month.value!!).toString())
            val day = Integer.parseInt((day.value!!).toString())

            if(imageUri == null) {
                // TODO: 이미지 안 넣으면 추가 못함 코드 작성
            }
            else {
                val cell = DiaryCellData(imageUri!!, year, month, day)
                cell.titleText = binding.titleTextField.text.toString()
                cell.contentText = binding.contentTextField.text.toString()

                val textList: ArrayList<String> = arrayListOf()
                textList.add(cell.toString())
                Log.d("Cell Text", "AddDiary -- $cell")

                jpegViewModel.jpegMCContainer.value!!.setTextConent(
                    ContentAttribute.basic,
                    textList
                )

                // CoroutineScope(Dispatchers.Default).launch {
                val fileName = jpegViewModel.getFileNameFromUri(imageUri!!)
                jpegViewModel.currentFileName = fileName

                // 기존 파일 삭제
                jpegViewModel.jpegMCContainer.value?.saveResolver!!.deleteImage(
                    imageUri!!,
                    fileName
                )

                var savedFilePath = jpegViewModel.jpegMCContainer.value?.save()
                //var savedFilePath = jpegViewModel.jpegMCContainer.value?.overwiteSave(fileName)
                val imageUri = Uri.parse(savedFilePath)

                cell.currentUri = imageUri
                jpegViewModel.currentUri = imageUri

                val editor: SharedPreferences.Editor = jpegViewModel.preferences.edit()
                editor.putString("$year/$month/$day", savedFilePath)
                editor.apply()
                findNavController().navigate(com.goldenratio.onepicdiary.R.id.action_addDiaryFragment_to_calendarFragment)
            }
        }
        binding.mainView.setOnClickListener {
            openGallery()
        }

        return binding.root
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        jpegViewModel.jpegMCContainer.value?.init()

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.data
            // 선택한 이미지 처리 로직을 여기에 추가
            if (imageUri != null) {
                CoroutineScope(Dispatchers.Main).launch {
                    Glide.with(binding.mainView)
                        .load(imageUri)
                        .into(binding.mainView)
                }

                jpegViewModel.jpegMCContainer.value!!.init()
                jpegViewModel.setCurrentMCContainer(imageUri!!)

                val imageContent = jpegViewModel.jpegMCContainer.value!!.imageContent
                while (!imageContent.checkPictureList) {
                    Thread.sleep(200)
                }
            }
        }
    }
}