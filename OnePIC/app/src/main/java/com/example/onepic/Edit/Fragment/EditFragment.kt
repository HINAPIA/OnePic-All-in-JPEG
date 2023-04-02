package com.example.onepic.Edit.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.onepic.ExPictureContainer
import com.example.onepic.ImageToolModule
import com.example.onepic.R
import com.example.onepic.databinding.FragmentEditBinding

class EditFragment : Fragment(R.layout.fragment_edit) {
    // EditFragment에서 사용할 변수 선언
    private lateinit var exPictureContainer: ExPictureContainer
    private lateinit var binding: FragmentEditBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        bundle: Bundle?
    ): View {
        // 뷰 바인딩 설정
        binding = FragmentEditBinding.inflate(inflater, container, false)

        // Bundle에서 객체를 받아옴
        if(arguments != null)
            exPictureContainer =
                requireArguments().getSerializable("exPictureContainer") as ExPictureContainer
        else
            exPictureContainer = ExPictureContainer(inflater.context)

        // 메인 이미지 설정
        val mainImage = exPictureContainer.getMainPicture()
        val mainBitmap = ImageToolModule().byteArrayToBitmap(mainImage.byteArray)
        binding.mainImageView.setImageBitmap(mainBitmap)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // "Rewind" 버튼 클릭 이벤트 리스너 등록
        binding.rewindBtn.setOnClickListener {
            // 객체를 Bundle에 저장하여 RewindFragment로 전달
            val bundle = Bundle()
            bundle.putSerializable("exPictureContainer", exPictureContainer)

            // RewindFragment로 이동
            findNavController().navigate(R.id.action_editFragment_to_rewindFragment, bundle)
        }
    }
}
