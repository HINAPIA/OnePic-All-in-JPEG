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
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.goldenratio.onepic.JpegViewModel
import com.goldenratio.onepic.PictureModule.Contents.ContentAttribute
import com.goldenratio.onepicdiary.DiaryCellData
import com.goldenratio.onepicdiary.R
import com.goldenratio.onepicdiary.databinding.FragmentAddDiaryBinding
import kotlinx.coroutines.*
import java.io.File
import java.util.*


class AddDiaryFragment : Fragment() {

    private var imageUri: Uri? = null
    private lateinit var binding: FragmentAddDiaryBinding
    private val jpegViewModel by activityViewModels<JpegViewModel>()

    private val PICK_IMAGE_REQUEST = 1

    private lateinit var calendar: Calendar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentAddDiaryBinding.inflate(inflater, container, false)

        // 취소 버튼 클릭 시
        binding.cancleBtn.setOnClickListener {
            findNavController().navigate(R.id.action_addDiaryFragment_to_calendarFragment)
        }
        // 저장 버튼 클릭 시
        binding.saveBtn.setOnClickListener {
            val year = binding.datePicker.year
            val month = binding.datePicker.month
            val day = binding.datePicker.dayOfMonth

                val cell = DiaryCellData(imageUri!!, year, month+1, day)
                cell.titleText = binding.titleTextField.text.toString()
                cell.contentText = binding.contentTextField.text.toString()

                val textList: ArrayList<String> = arrayListOf()
                textList.add(cell.toString())
                Log.d( "Cell Text ----- ", cell.toString())
                jpegViewModel.jpegMCContainer.value!!.setTextConent(
                    ContentAttribute.basic,
                    textList
                )
                Log.d("Cell Text"," == > "+ textList.toString())

                CoroutineScope(Dispatchers.Default).launch {

                    val fileName = jpegViewModel.getFileNameFromUri(imageUri!!)
//                    val fileName =
//                        currentFilePath!!.substring(currentFilePath.lastIndexOf("/") -1)
                    var savedFilePath = jpegViewModel.jpegMCContainer.value?.overwiteSave(fileName)
                    //ViewerFragment.currentFilePath = savedFilePath.toString()
                    Log.d("savedFilePath", "savedFilePath : $savedFilePath")
                    Log.d("savedFilePath", "currentFilePath : $fileName")

                    Log.d("savedFilePath", "file Path ------- ${imageUri!!.path}")

                    val imageUri = Uri.parse(savedFilePath)

                    cell.currentUri = imageUri
                    jpegViewModel.currentUri = imageUri

                    jpegViewModel.diaryCellArrayList.add(cell)

                    val editor: SharedPreferences.Editor = jpegViewModel.preferences.edit()
                    editor.putString("$year/$month/$day", savedFilePath)
                    editor.apply()
                }

                findNavController().navigate(R.id.action_addDiaryFragment_to_calendarFragment)
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
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.data
                    // 선택한 이미지 처리 로직을 여기에 추가
            if (imageUri != null) {
//                adapter.addDiaryImage(imageUri, currentYear, currentMonth, 15)
//                binding.mainView.setImageURI(imageUri)
                CoroutineScope(Dispatchers.Main).launch {
                    Glide.with(binding.mainView)
                        .load(imageUri)
                        .into(binding.mainView)
                }
                jpegViewModel.currentUri = imageUri
                jpegViewModel.setCurrentMCContainer()

                while (!jpegViewModel.jpegMCContainer.value!!.imageContent.checkPictureList) {

                }
                // text 입력 UI에 기존의 텍스트 메시지 띄우기
                val textList = jpegViewModel.jpegMCContainer.value!!.textContent.textList
                if(textList.size !=0){
                    val title = textList[0].data.split("<title>")
                    val finalTitle = title[1].split("</title>")

                    val contentText = textList[0].data.split("<contentText>")
                    val finalContentText  = contentText[1].split("</contentText>")

                    CoroutineScope(Dispatchers.Main).launch {
                        binding.titleTextField.setText(finalTitle[0])
                        binding.contentTextField.setText(finalContentText[0])
                    }
                }
            }
        }
    }
}