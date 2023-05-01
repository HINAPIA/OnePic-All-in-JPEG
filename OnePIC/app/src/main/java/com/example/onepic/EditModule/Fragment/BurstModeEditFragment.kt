package com.example.onepic.EditModule.Fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.onepic.ImageToolModule
import com.example.onepic.JpegViewModel
import com.example.onepic.PictureModule.Contents.ActivityType
import com.example.onepic.PictureModule.Contents.Picture
import com.example.onepic.PictureModule.ImageContent
import com.example.onepic.R
import com.example.onepic.databinding.FragmentBurstModeEditBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BurstModeEditFragment : Fragment() {


    private lateinit var binding: FragmentBurstModeEditBinding
    lateinit var fragment: Fragment
    private val jpegViewModel by activityViewModels<JpegViewModel>()
    private lateinit var imageContent : ImageContent

    private var pictureList = arrayListOf<Picture>()
    private lateinit var mainPicture : Picture

    private lateinit var imageToolModule: ImageToolModule
    private var mainIndex = 0

    private lateinit var mainSubView: View

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 뷰 바인딩 설정
        binding = FragmentBurstModeEditBinding.inflate(inflater, container, false)

        imageContent = jpegViewModel.jpegMCContainer.value?.imageContent!!
        imageToolModule = ImageToolModule()

        imageToolModule.showView(binding.progressBar,true)

        mainPicture = imageContent.mainPicture
        pictureList = imageContent.pictureList

        CoroutineScope(Dispatchers.Default).launch {
            // 파일을 parsing해서 PictureContainer로 바꾸는 함수 호출
            // 메인 이미지 설정
            withContext(Dispatchers.Main) {
                Glide.with(binding.burstMainView)
                    .load(imageContent.getJpegBytes(mainPicture))
                    .into(binding.burstMainView)
            }
        }


        CoroutineScope(Dispatchers.Default).launch{
            val checkFinish = BooleanArray(pictureList.size)
            for (i in 0 until pictureList.size) {
                checkFinish[i] = false
            }

            for(i in 0 until pictureList.size) {
                CoroutineScope(Dispatchers.Main).launch {
                    // 넣고자 하는 layout 불러오기
                    val subLayout =
                        layoutInflater.inflate(R.layout.sub_image_array, null)

                    // 위 불러온 layout에서 변경을 할 view가져오기
                    val cropImageView: ImageView =
                        subLayout.findViewById(R.id.cropImageView)

                    // 자른 사진 이미지뷰에 붙이기
                    //cropImageView.setImageBitmap(bitmapList[i])
                    withContext(Dispatchers.Main) {
                        Glide.with(cropImageView)
                            .load(imageContent.getJpegBytes(pictureList[i]))
                            .into(cropImageView)
                    }

                    if (mainIndex == i) {
                        imageToolModule.showView(subLayout.findViewById(R.id.checkMainIcon), true)
                        mainSubView = subLayout.findViewById(R.id.checkMainIcon)
                    }

                    cropImageView.setOnClickListener {
                        mainPicture = pictureList[i]
                        imageToolModule.showView(mainSubView, false)
                        CoroutineScope(Dispatchers.Main).launch {
                            // 메인 이미지 설정
                            withContext(Dispatchers.Main) {
                                Glide.with(binding.burstMainView)
                                    .load(imageContent.getJpegBytes(mainPicture))
                                    .into(binding.burstMainView)
                            }
                        }
                        imageToolModule.showView(subLayout.findViewById(R.id.checkMainIcon), true)
                        mainSubView = subLayout.findViewById(R.id.checkMainIcon)
                        //binding.burstMainView.setImageBitmap(mainBitmap)
                    }

                    withContext(Dispatchers.Main) {
                        // main activity에 만들어둔 scrollbar 속 layout의 아이디를 통해 해당 layout에 넣기
                        binding.candidateLayout.addView(subLayout)
                    }
                    checkFinish[i] = true

                }
            }
            while (!checkFinish.all { it }) { }
            imageToolModule.showView(binding.progressBar, false)
        }
        binding.burstSaveBtn.setOnClickListener {
            CoroutineScope(Dispatchers.Default).launch {
                imageToolModule.showView(binding.progressBar , true)

                // TODO: main 변경 후 save
                // 1. main으로 지정된 picture를 picturelist에서 삭제
                var result = imageContent.removePicture(mainPicture)
                if(result){
                    Log.d("burst", "main으로 지정된 객체 삭제 완료")
                    // 2. main 사진을 첫번 째로 삽입
                    imageContent.insertPicture(0, mainPicture)
                    imageContent.mainPicture = mainPicture
                }

                if(imageContent.activityType == ActivityType.Camera) {
                    withContext(Dispatchers.Main){
                        Log.d("burst", "바로 편집에서 save() 호출 전")
                        jpegViewModel.jpegMCContainer.value?.save()
                        findNavController().navigate(R.id.action_burstModeEditFragment_to_Fragment)
                    }
                }
                else{
                    withContext(Dispatchers.Main){
                        Log.d("burst", "바로 편집에서navigate호출 전")
                        findNavController().navigate(R.id.action_burstModeEditFragment_to_Fragment)
                    }
                    imageToolModule.showView(binding.progressBar , false)
                }

            }
        }
        binding.burstCloseBtn.setOnClickListener {
            CoroutineScope(Dispatchers.Default).launch {
                withContext(Dispatchers.Main) {
                    findNavController().navigate(R.id.action_burstModeEditFragment_to_Fragment)
                }
            }
        }
        return binding.root
    }

}