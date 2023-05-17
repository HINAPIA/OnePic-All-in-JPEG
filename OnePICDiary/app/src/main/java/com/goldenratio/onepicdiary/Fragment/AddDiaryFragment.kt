package com.goldenratio.onepicdiary.Fragment

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.goldenratio.onepic.JpegViewModel
import com.goldenratio.onepicdiary.R
import com.goldenratio.onepicdiary.databinding.FragmentAddDiaryBinding
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

        calendar = Calendar.getInstance()
        openGallery()

        // 취소 버튼 클릭 시
        binding.cancleBtn.setOnClickListener {
            findNavController().navigate(R.id.action_addDiaryFragment_to_calendarFragment)
        }
        // 저장 버튼 클릭 시
        binding.saveBtn.setOnClickListener {
            jpegViewModel.year = Integer.parseInt(binding.yearTextDate.text.toString())
            jpegViewModel.month = Integer.parseInt(binding.monthTextDate.text.toString())
            jpegViewModel.day = Integer.parseInt(binding.dayTextDate.text.toString())
            jpegViewModel.currentUri = imageUri

            findNavController().navigate(R.id.action_addDiaryFragment_to_calendarFragment)
        }

        return binding.root
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.data
                    // 선택한 이미지 처리 로직을 여기에 추가
            if (imageUri != null) {
//                adapter.addDiaryImage(imageUri, currentYear, currentMonth, 15)
                binding.mainView.setImageURI(imageUri)
            }
        }
    }
}