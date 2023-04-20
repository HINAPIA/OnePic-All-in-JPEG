package com.example.onepic.EditModule.Fragment

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.onepic.*
import com.example.onepic.PictureModule.Contents.ContentAttribute
import com.example.onepic.PictureModule.ImageContent
import com.example.onepic.databinding.FragmentEditBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditFragment : Fragment(R.layout.fragment_edit) {
    private lateinit var binding: FragmentEditBinding

    var activity : MainActivity = MainActivity()
    lateinit var fragment: Fragment
    private val jpegViewModel by activityViewModels<JpegViewModel>()
    private lateinit var imageContent : ImageContent

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        bundle: Bundle?
    ): View {
        // 뷰 바인딩 설정
        binding = FragmentEditBinding.inflate(inflater, container, false)

        imageContent = jpegViewModel.jpegMCContainer.value?.imageContent!!

        CoroutineScope(Dispatchers.Default).launch {
            // 파일을 parsing해서 PictureContainer로 바꾸는 함수 호출
            // 메인 이미지 설정
            withContext(Dispatchers.Main) {
                Glide.with(binding.mainImageView)
                    .load(imageContent.getJpegBytes(imageContent.mainPicture))
                    .into(binding.mainImageView)
            }
        }
        CoroutineScope(Dispatchers.Default).launch{
             imageContent.getMainBitmap()
        }
        CoroutineScope(Dispatchers.Default).launch{
            imageContent.getBitmapList()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // "Rewind" 버튼 클릭 이벤트 리스너 등록
        binding.rewindBtn.setOnClickListener {
            // 일반 사진이면 안 넘어가도록
            if(!(imageContent.mainPicture.contentAttribute == ContentAttribute.basic)){
                // RewindFragment로 이동
                findNavController().navigate(R.id.action_editFragment_to_rewindFragment)
            }
        }

        binding.magicBtn.setOnClickListener {
            // 일반 사진이면 안 넘어가도록
            if(!(imageContent.mainPicture.contentAttribute == ContentAttribute.basic)) {
                // MagicPictureFragment로 이동
                findNavController().navigate(R.id.action_editFragment_to_magicPictureFragment)
            }
        }

        binding.addBtn.setOnClickListener {
            // 일반 사진이면 안 넘어가도록
            if(!(imageContent.mainPicture.contentAttribute == ContentAttribute.basic)) {
                // MagicPictureFragment로 이동
                findNavController().navigate(R.id.action_editFragment_to_playFragment)
            }

            //findNavController().navigate(R.id.action_editFragment_to_addFragment)
        }

        binding.backBtn.setOnClickListener {
            findNavController().navigate(R.id.action_editFragment_to_viewerFragment)

        }
        binding.saveBtn.setOnClickListener{
            val mainPicture = imageContent.mainPicture
            // 바뀐 비트맵을 Main(맨 앞)으로 하는 새로운 Jpeg? 저장
            //Log.d("test_test", "save 버튼 클릭")
            imageContent.insertPicture(0, mainPicture)
            jpegViewModel.jpegMCContainer.value?.save()
            findNavController().navigate(R.id.action_editFragment_to_viewerFragment)
        }

    }
}
