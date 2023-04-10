package com.example.onepic.EditModule.Fragment

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import com.bumptech.glide.Glide
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.onepic.LoadModule.LoadResolver
import com.example.onepic.PictureModule.ImageContent
import com.example.onepic.PictureModule.MCContainer
import com.example.onepic.*
import com.example.onepic.PictureModule.Contents.ContentAttribute
import com.example.onepic.databinding.FragmentEditBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EditFragment : Fragment(R.layout.fragment_edit) {
    // EditFragment에서 사용할 변수 선언
    //private lateinit var exPictureContainer: ExPictureContainer
    private lateinit var binding: FragmentEditBinding

    var activity : MainActivity = MainActivity()
    //private var MCContainer : MCContainer = MCContainer(activity)
    //private var loadResolver : LoadResolver = LoadResolver()
    private val jpegViewModel by activityViewModels<JpegViewModel>()
    private lateinit var imageConent : ImageContent

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        bundle: Bundle?
    ): View {
        // 뷰 바인딩 설정
        binding = FragmentEditBinding.inflate(inflater, container, false)
        imageConent = jpegViewModel.jpegMCContainer.value?.imageContent!!

        // 파일을 parsing해서 PictureContainer로 바꾸는 함수 호출
        // 메인 이미지 설정
        val mainPicture = imageConent.mainPicture
        val mainByteArray = imageConent.getJpegBytes(mainPicture)
        //mainBitmap =  imageToolModule.byteArrayToBitmap(imageConent.getJpegBytes(imageConent.mainPicture))
        val mainBitmap = ImageToolModule().byteArrayToBitmap(imageConent.getJpegBytes(imageConent.mainPicture))
//        val mainPicture = MCContainer.getMainPicture()
//        val mainImageBytes = MCContainer.imageContent.getJpegBytes(mainPicture)
//        val mainBitmap = ImageToolModule().byteArrayToBitmap(mainImageBytes)
       // binding.mainImageView.setImageBitmap(mainBitmap)
        CoroutineScope(Dispatchers.Main).launch{
            Glide.with(binding.mainImageView)
                .load(mainByteArray)
                .into(binding.mainImageView)
        }

        binding.saveBtn.setOnClickListener{
            // 바뀐 비트맵을 Main(맨 앞)으로 하는 새로운 Jpeg? 저장
            imageConent.insertPicture(0, mainPicture)
            jpegViewModel.jpegMCContainer.value?.save()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // "Rewind" 버튼 클릭 이벤트 리스너 등록
        binding.rewindBtn.setOnClickListener {

            // 일반 사진이면 안 넘어가도록
            if(!(imageConent.mainPicture.contentAttribute == ContentAttribute.basic)){
                // RewindFragment로 이동
                findNavController().navigate(R.id.action_editFragment_to_rewindFragment)
            }

        }
        binding.backBtn.setOnClickListener {
            findNavController().navigate(R.id.action_editFragment_to_viewerFragment)

        }
    }
}
