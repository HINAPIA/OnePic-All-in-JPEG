package com.goldenratio.onepicdiary.Fragment

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.goldenratio.onepic.JpegViewModel
import com.goldenratio.onepicdiary.DiaryModule.LayoutToolModule
import com.goldenratio.onepicdiary.R
import com.goldenratio.onepicdiary.databinding.FragmentAddDiaryBinding
import com.goldenratio.onepicdiary.databinding.FragmentAudioAddBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AudioAddFragment : Fragment() {
    private var imageUri: Uri? = null
    private lateinit var binding: FragmentAudioAddBinding
    private val jpegViewModel by activityViewModels<JpegViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreate(savedInstanceState)
        binding = FragmentAudioAddBinding.inflate(inflater, container, false)

        val currentUri = jpegViewModel.currentUri
        Log.d("audio_test",currentUri.toString())
        // 선택한 이미지 처리 로직을 여기에 추가
        if (currentUri != null) {
            CoroutineScope(Dispatchers.Main).launch {
                Glide.with(binding.mainView)
                    .load(currentUri)
                    .into(binding.mainView)
            }
        }

        binding.back.setOnClickListener {
            findNavController().navigate(R.id.action_audioAddFragment_to_addDiaryFragment)
        }
        return binding.root
    }
}
