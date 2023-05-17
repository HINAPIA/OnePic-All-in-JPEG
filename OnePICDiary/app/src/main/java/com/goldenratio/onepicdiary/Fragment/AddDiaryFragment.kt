package com.goldenratio.onepicdiary.Fragment

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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

            if(imageUri != null) {
                val cell = DiaryCellData(imageUri!!, year, month, day)
                cell.titleText = binding.titleTextField.toString()
                cell.contentText = binding.contentTextField.toString()

                val textList: ArrayList<String> = arrayListOf()
                textList.add(cell.toString())
                jpegViewModel.jpegMCContainer.value!!.setTextConent(ContentAttribute.basic, textList)

//                jpegViewModel.jpegMCContainer.value?.save()

                jpegViewModel.diaryCellArrayList.add(cell)
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

                jpegViewModel.setCurrentMCContainer()
            }
        }
    }
}