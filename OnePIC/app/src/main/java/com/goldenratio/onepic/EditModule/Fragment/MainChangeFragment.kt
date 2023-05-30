package com.goldenratio.onepic.EditModule.Fragment

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.goldenratio.onepic.EditModule.RewindModule
import com.goldenratio.onepic.EditModule.ShakeLevelModule
import com.goldenratio.onepic.ImageToolModule
import com.goldenratio.onepic.JpegViewModel
import com.goldenratio.onepic.PictureModule.Contents.Picture
import com.goldenratio.onepic.PictureModule.ImageContent
import com.goldenratio.onepic.R
import com.goldenratio.onepic.databinding.FragmentMainChangeBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainChangeFragment : Fragment() {


    private var bestImage: TextView? = null
    private lateinit var binding: FragmentMainChangeBinding
    lateinit var fragment: Fragment
    private val jpegViewModel by activityViewModels<JpegViewModel>()
    private lateinit var imageContent : ImageContent

    private var pictureList = arrayListOf<Picture>()
    private var bitmapList = arrayListOf<Bitmap>()
    private lateinit var mainPicture : Picture

    private lateinit var imageToolModule: ImageToolModule
    private var mainIndex = 0

    private lateinit var mainSubView: View

    private lateinit var checkFinish: BooleanArray

    private var infoLevel = MutableLiveData<InfoLevel>()

    enum class InfoLevel {
        BeforeMainSelect,
        AfterMainSelect
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 상태바 색상 변경
        val window: Window = activity?.window
            ?: throw IllegalStateException("Fragment is not attached to an activity")
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.setStatusBarColor(ContextCompat.getColor(requireContext(), android.R.color.black))

        // 뷰 바인딩 설정
        binding = FragmentMainChangeBinding.inflate(inflater, container, false)

        imageContent = jpegViewModel.jpegMCContainer.value?.imageContent!!
        imageToolModule = ImageToolModule()

            while(!imageContent.checkPictureList) {}

            mainPicture = imageContent.mainPicture
            pictureList = imageContent.pictureList

            checkFinish = BooleanArray(pictureList.size)

        CoroutineScope(Dispatchers.Default).launch {
            val bitmap = imageContent.getBitmapList()
            if (bitmap != null)
                bitmapList = bitmap
        }

        CoroutineScope(Dispatchers.Main).launch {
            // 파일을 parsing해서 PictureContainer로 바꾸는 함수 호출
            // 메인 이미지 설정
            withContext(Dispatchers.Main) {
                Glide.with(binding.changeMainView)
                    .load(imageContent.getJpegBytes(mainPicture))
                    .into(binding.changeMainView)
            }
        }

        binding.changeSaveBtn.setOnClickListener {
            imageContent.resetBitmap()

            imageToolModule.showView(binding.progressBar , true)
            CoroutineScope(Dispatchers.Default).launch {
                // 1. main으로 지정된 picture를 picturelist에서 삭제
                var result = imageContent.removePicture(mainPicture)
                Log.d("error 잡기", "메인 바꾸고 save : ${result}")
                if (result) {
                    Log.d("error 잡기", "main으로 지정된 객체 삭제 완료")
                    // 2. main 사진을 첫번 째로 삽입
                    imageContent.insertPicture(0, mainPicture)
                    imageContent.mainPicture = mainPicture
                }

//                if (imageContent.activityType == ActivityType.Camera) {
                    withContext(Dispatchers.Main) {
                        Log.d("error 잡기", "바로 편집에서 save() 호출 전")
                        jpegViewModel.jpegMCContainer.value?.save()
                        Log.d("error 잡기", "바로 편집에서 save() 호출후")
                        imageContent.checkMainChanged = true
                        Thread.sleep(2000)
                        imageToolModule.showView(binding.progressBar, false)
                        findNavController().navigate(R.id.action_burstModeEditFragment_to_Fragment)
                    }
//                } else {
//                    withContext(Dispatchers.Main) {
//                        Log.d("error 잡기", "바로 편집에서 navigate호출 전")
//                        imageContent.checkMainChanged = true
//                        findNavController().navigate(R.id.action_burstModeEditFragment_to_Fragment)
//                    }
//                    imageToolModule.showView(binding.progressBar, false)
//
//                }
            }
        }
        binding.changeCloseBtn.setOnClickListener {
            CoroutineScope(Dispatchers.Default).launch {
                withContext(Dispatchers.Main) {
                    findNavController().navigate(R.id.action_burstModeEditFragment_to_Fragment)
                }
            }
        }
        binding.choiceMainBtn.setOnClickListener {
            imageToolModule.showView(binding.choiceMainBtn, false)
            imageToolModule.showView(binding.infoDialogLayout, false)
            imageToolModule.showView(binding.progressBar, true)
            viewBestImage()
        }

//        Thread.sleep(3000)
        Log.d("error 잡기", "BurstEdit picureList size ${pictureList.size}")
//        if(imageContent.activity Type == ActivityType.Viewer) {
            infoLevel.value = InfoLevel.AfterMainSelect
            setSubImage()
            infoLevel.observe(viewLifecycleOwner){ _ ->
                infoTextView()
            }
//        }
//        else {
//            infoLevel.value = InfoLevel.AfterMainSelect
//            infoTextView()
//        }

        // info 확인
        binding.changeInfoBtn.setOnClickListener {
            imageToolModule.showView(binding.infoDialogLayout, true)
        }

        // info 삭제
        binding.dialogCloseBtn.setOnClickListener {
            imageToolModule.showView(binding.infoDialogLayout, false)
        }

        return binding.root
    }

    fun setSubImage() {
//        CoroutineScope(Dispatchers.Default).launch{
//            val checkFinish = BooleanArray(pictureList.size)
//            for (i in 0 until pictureList.size) {
//                checkFinish[i] = false
//            }

        for (i in 0 until pictureList.size) {
//                CoroutineScope(Dispatchers.Main).launch {
            // 넣고자 하는 layout 불러오기
            val subLayout =
                layoutInflater.inflate(R.layout.sub_image_array, null)

            // 위 불러온 layout에서 변경을 할 view가져오기
            val cropImageView: ImageView =
                subLayout.findViewById(R.id.cropImageView)

            // 이미지뷰에 붙이기
            CoroutineScope(Dispatchers.Main).launch {
                Log.d("error 잡기", "$i 번째 이미지 띄우기")
                Glide.with(cropImageView)
                    .load(imageContent.getJpegBytes(pictureList[i]))
                    .into(cropImageView)
            }

            if (mainIndex == i) {
//                imageToolModule.showView(subLayout.findViewById(R.id.checkMainIcon), true)
                mainSubView = cropImageView
                CoroutineScope(Dispatchers.Main).launch {
                    mainSubView.setBackgroundResource(R.drawable.chosen_image_border)
                }
            }

            cropImageView.setOnClickListener {
                mainSubView.background = null
                CoroutineScope(Dispatchers.Main).launch {
                    mainSubView.background = null
                }

                mainPicture = pictureList[i]
//                imageToolModule.showView(mainSubView, false)
                CoroutineScope(Dispatchers.Main).launch {
//                    infoLevel.value = InfoLevel.AfterMainSelect
                    imageToolModule.showView(binding.infoDialogLayout, false)
                    
                    // 메인 이미지 설정
                    Glide.with(binding.changeMainView)
                        .load(imageContent.getJpegBytes(mainPicture))
                        .into(binding.changeMainView)
                    cropImageView.setBackgroundResource(R.drawable.chosen_image_border)
                }
//                imageToolModule.showView(subLayout.findViewById(R.id.checkMainIcon), true)

                mainSubView = cropImageView
            }

            CoroutineScope(Dispatchers.Main).launch {
                // main activity에 만들어둔 scrollbar 속 layout의 아이디를 통해 해당 layout에 넣기
                binding.candidateLayout.addView(subLayout)
            }
//                    checkFinish[i] = true

//                }
        }
//            while (!checkFinish.all { it }) { }
//            imageToolModule.showView(binding.progressBar, false)
//        }
    }
    fun viewBestImage() {
        val bitmapList = imageContent.getBitmapList()
        if (bitmapList != null) {
            val rewindModule = RewindModule()
            CoroutineScope(Dispatchers.IO).launch {

                rewindModule.allFaceDetection(bitmapList)
                val faceDetectionResult = rewindModule.choiceBestImage(bitmapList)
                Log.d("anaylsis", "end faceDetection")

                val shakeDetectionResult = ShakeLevelModule().shakeLevelDetection(bitmapList)

                val analysisResults = arrayListOf<Double>()

                var bestImageIndex = 0
                if (checkFinish.all { it }) {
                    checkFinish = BooleanArray(bitmapList.size)
                }
                for (i in 0 until bitmapList.size) {
                    if (!checkFinish[i]) {
                        bestImageIndex = i
                        break
                    }
                }

                for (i in 0 until bitmapList.size) {

                    Log.d("anaylsis", "[$i] = ${checkFinish[i]} | ")
                    Log.d("anaylsis", "[$i] =  faceDetectio ${faceDetectionResult[i]} ")
                    Log.d("anaylsis", "[$i] =  shake ${shakeDetectionResult[i]}")


                    analysisResults.add(faceDetectionResult[i] + shakeDetectionResult[i])
                    if (!checkFinish[i] && analysisResults[bestImageIndex] < analysisResults[i]) {
                        bestImageIndex = i
                        checkFinish[i] = true
                    }
                }

                Log.d("anaylsis", "=== ${analysisResults[bestImageIndex]}")
                checkFinish[bestImageIndex] = true
                println("bestImageIndex = $bestImageIndex")

                mainPicture = pictureList[bestImageIndex]

                withContext(Dispatchers.Main) {
                    // main activity에 만들어둔 scrollbar 속 layout의 아이디를 통해 해당 layout에 넣기
                    Glide.with(binding.changeMainView)
                        .load(imageContent.getJpegBytes(mainPicture))
                        .into(binding.changeMainView)

                    bestImage = binding.candidateLayout.getChildAt(bestImageIndex)
                        .findViewById(R.id.checkMainIcon)

                    mainSubView.background = null
                    mainSubView = binding.candidateLayout.getChildAt(bestImageIndex)
                        .findViewById<TextView>(R.id.cropImageView)
                    mainSubView.setBackgroundResource(R.drawable.chosen_image_border)

                    if (bestImage != null) {
                        imageToolModule.showView(bestImage!!, true)
                        Log.d("mainChange", "bestImage not null")
                    }
                    Log.d("mainChange", "bestImage null")
                    imageToolModule.showView(binding.progressBar, false)
//                    imageToolModule.showView(binding.choiceMainBtn, true)
                    infoLevel.value = InfoLevel.BeforeMainSelect
                }
            }
        }
    }

    fun infoTextView() {
        Log.d("infoTextView","infoTextView call")
        when (infoLevel.value) {
            InfoLevel.BeforeMainSelect -> {
                binding.infoText.text = "아래 사진을 선택해\n대표 사진을 변경할 수 있습니다."
            }
            InfoLevel.AfterMainSelect -> {
                binding.infoText.text = "대표사진 추천 버튼을 클릭해\n대표 사진을 추천 받을 수 있습니다."
            }
            else -> {}
        }
    }
}