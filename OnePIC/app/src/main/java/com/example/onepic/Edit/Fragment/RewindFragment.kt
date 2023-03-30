package com.example.onepic.Edit.Fragment

import android.graphics.Bitmap
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.onepic.Edit.RewindModule
import com.example.onepic.ExPictureContainer
import com.example.onepic.ImageToolModule
import com.example.onepic.Picture
import com.example.onepic.R
import com.example.onepic.databinding.FragmentRewindBinding
import com.google.mlkit.vision.common.InputImage

class rewindFragment : Fragment(R.layout.fragment_rewind) {

    private lateinit var binding: FragmentRewindBinding
    private lateinit var exPictureContainer: ExPictureContainer
    private lateinit var imageToolModule: ImageToolModule
    private lateinit var rewindModule: RewindModule

    private lateinit var mainPicture: Picture
    private lateinit var mainBitmap: Bitmap

    private var pictureList: ArrayList<Picture> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 뷰 바인딩 설정
        binding = FragmentRewindBinding.inflate(inflater, container, false)

        /** ExPictureContainer 설정 **/
        exPictureContainer = ExPictureContainer(inflater.context)
        imageToolModule = ImageToolModule()
        rewindModule = RewindModule()

        // main Picture의 byteArray를 bitmap 제작
        mainPicture = exPictureContainer.getMainPicture()
        mainBitmap = imageToolModule.byteArrayToBitmap(mainPicture.byteArray)

        // faceDetection하고 결과가 표시된 사진을 받아 imaveView에 띄우기
        //val faceResultBitmap = rewindModule.getDrawFaceBoxBitmap(mainBitmap)
        val faceResultBitmap = rewindModule.getDrawFaceBoxBitmap(mainBitmap)

        binding.mainImageView.setImageBitmap(faceResultBitmap)

        // rewind 가능한 연속 사진 속성의 picture list 얻음
        pictureList = exPictureContainer.getPictureList(1, "BurstShots")

        return binding.root
    }


}