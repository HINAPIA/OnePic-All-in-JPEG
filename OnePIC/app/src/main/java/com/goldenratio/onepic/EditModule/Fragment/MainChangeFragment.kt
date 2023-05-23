package com.goldenratio.onepic.EditModule.Fragment

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.goldenratio.onepic.EditModule.RewindModule
import com.goldenratio.onepic.EditModule.ShakeLevelModule
import com.goldenratio.onepic.ImageToolModule
import com.goldenratio.onepic.JpegViewModel
import com.goldenratio.onepic.PictureModule.Contents.ActivityType
import com.goldenratio.onepic.PictureModule.Contents.ContentAttribute
import com.goldenratio.onepic.PictureModule.Contents.Picture
import com.goldenratio.onepic.PictureModule.ImageContent
import com.goldenratio.onepic.R
import com.goldenratio.onepic.databinding.FragmentMainChangeBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainChangeFragment : Fragment() {


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

    val max = 5
    val min = 0.1.toFloat()
    val step = 0.1.toFloat()

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

        mainPicture = imageContent.mainPicture
        pictureList = imageContent.pictureList

        checkFinish = BooleanArray(pictureList.size)

        CoroutineScope(Dispatchers.Default).launch {
            while(!imageContent.checkPictureList) {}
            val bitmap = imageContent.getBitmapList()
            if(bitmap!=null)
                bitmapList = bitmap
        }

        CoroutineScope(Dispatchers.Main).launch {
            // 파일을 parsing해서 PictureContainer로 바꾸는 함수 호출
            // 메인 이미지 설정
            withContext(Dispatchers.Main) {
                Glide.with(binding.burstMainView)
                    .load(imageContent.getJpegBytes(mainPicture))
                    .into(binding.burstMainView)
            }
        }

        binding.burstSaveBtn.setOnClickListener {
            imageContent.resetMainBitmap()
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


                if(imageContent.activityType == ActivityType.Camera) {
                    withContext(Dispatchers.Main){
                        Log.d("error 잡기", "바로 편집에서 save() 호출 전")
                        jpegViewModel.jpegMCContainer.value?.save()
                        Log.d("error 잡기", "바로 편집에서 save() 호출후")
                        imageContent.checkMainChangeAttribute = true
                        Thread.sleep(2000)
                        imageToolModule.showView(binding.progressBar , false)
                        findNavController().navigate(R.id.action_burstModeEditFragment_to_Fragment)
                    }
                }
                else{
                    withContext(Dispatchers.Main){
                        Log.d("error 잡기", "바로 편집에서 navigate호출 전")
                        imageContent.checkMainChangeAttribute = true
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
        binding.choiseMainBtn.setOnClickListener {
            viewBestImage()
        }

//        Thread.sleep(3000)
        Log.d("error 잡기", "BurstEdit picureList size ${pictureList.size}")
        if(imageContent.activityType == ActivityType.Viewer) {
            infoLevel.value = InfoLevel.BeforeMainSelect
            setSubImage()
            infoLevel.observe(viewLifecycleOwner){ _ ->
                infoTextView()
            }
        }
        else {
            infoLevel.value = InfoLevel.AfterMainSelect
            infoTextView()
        }

        // info 확인
        binding.burstInfoBtn.setOnClickListener {
            imageToolModule.showView(binding.infoDialogLayout, true)
        }

        // info 삭제
        binding.dialogCloseBtn.setOnClickListener {
            imageToolModule.showView(binding.infoDialogLayout, false)
        }

        CoroutineScope(Dispatchers.Default).launch {
            if (imageContent.activityType == ActivityType.Camera) {
                if(imageContent.checkAttribute(ContentAttribute.burst)) {
                    imageToolModule.showView(binding.progressBar, true)
                    imageToolModule.showView(binding.choiseMainBtn, true)
                    viewBestImage()
                }
                else {
                    if (imageContent.checkAttribute(ContentAttribute.distance_focus)) {
                        setSeekBar()
                    }
                }
            } else {
                imageToolModule.showView(binding.choiseMainBtn, true)
                if(!imageContent.checkAttribute(ContentAttribute.object_focus)) {
                    setSeekBar()
                }
            }
        }

        Log.d("seekBar", "${!(imageContent.activityType == ActivityType.Camera && imageContent.checkAttribute(ContentAttribute.burst))
                } && ${!imageContent.checkAttribute(ContentAttribute.object_focus)}")
        Log.d("seekBar", "~~~~ ${!(imageContent.activityType == ActivityType.Camera && imageContent.checkAttribute(ContentAttribute.burst))
                && !imageContent.checkAttribute(ContentAttribute.object_focus)}")



        return binding.root
    }

    fun setSubImage() {
        CoroutineScope(Dispatchers.Default).launch{
//            val checkFinish = BooleanArray(pictureList.size)
//            for (i in 0 until pictureList.size) {
//                checkFinish[i] = false
//            }

            for(i in 0 until pictureList.size) {
//                CoroutineScope(Dispatchers.Main).launch {
                    // 넣고자 하는 layout 불러오기
                    val subLayout =
                        layoutInflater.inflate(R.layout.sub_image_array, null)

                    // 위 불러온 layout에서 변경을 할 view가져오기
                    val cropImageView: ImageView =
                        subLayout.findViewById(R.id.cropImageView)

                    // 이미지뷰에 붙이기
                    withContext(Dispatchers.Main) {
                        Log.d("error 잡기", "$i 번째 이미지 띄우기")
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
                            infoLevel.value = InfoLevel.AfterMainSelect
                            // 메인 이미지 설정
                                Glide.with(binding.burstMainView)
                                    .load(imageContent.getJpegBytes(mainPicture))
                                    .into(binding.burstMainView)
                        }
                        imageToolModule.showView(subLayout.findViewById(R.id.checkMainIcon), true)
                        mainSubView = subLayout.findViewById(R.id.checkMainIcon)
                        //binding.burstMainView.setImageBitmap(mainBitmap)
                    }

                    withContext(Dispatchers.Main) {
                        // main activity에 만들어둔 scrollbar 속 layout의 아이디를 통해 해당 layout에 넣기
                        binding.candidateLayout.addView(subLayout)
                    }
//                    checkFinish[i] = true

//                }
            }
//            while (!checkFinish.all { it }) { }
//            imageToolModule.showView(binding.progressBar, false)
        }
    }
    fun viewBestImage() {
        imageToolModule.showView(binding.progressBar, true)
        val bitmapList = imageContent.getBitmapList()
        if (bitmapList != null) {

            val rewindModule = RewindModule()
            CoroutineScope(Dispatchers.IO).launch {

                rewindModule.allFaceDetection(bitmapList)
                val faceDetectionResult = rewindModule.choiseBestImage(bitmapList)
                Log.d("anaylsis", "end faceDetection")

                val shakeDetectionResult = ShakeLevelModule().shakeLevelDetection(bitmapList)

                val analysisResults = arrayListOf<Double>()

                var bestImageIndex = 0
                if (checkFinish.all { it }) {
                    checkFinish = BooleanArray(pictureList.size)
                }
                for(i in 0 until bitmapList.size) {
                    if(!checkFinish[i]) {
                        bestImageIndex = i
                        break
                    }
                }

                for(i in 0 until bitmapList.size) {
                    Log.d("anaylsis", "[$i] = ${checkFinish[i]} |  faceDetectio ${faceDetectionResult[i]} | shake ${shakeDetectionResult[i]}")
                    analysisResults.add(faceDetectionResult[i] + shakeDetectionResult[i])
                    if(!checkFinish[i] && analysisResults[bestImageIndex] < analysisResults[i]){
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
                    Glide.with(binding.burstMainView)
                        .load(imageContent.getJpegBytes(mainPicture))
                        .into(binding.burstMainView)

                    if(imageContent.activityType == ActivityType.Viewer) {
                        imageToolModule.showView(mainSubView, false)

                        mainSubView = binding.candidateLayout.getChildAt(bestImageIndex)
                            .findViewById(R.id.checkMainIcon)
                        imageToolModule.showView(mainSubView, true)
                    }
                    imageToolModule.showView(binding.progressBar, false)
                }

            }
        }
    }

    fun infoTextView() {
        Log.d("infoTextView","infoTextView call")
        when (infoLevel.value) {
            InfoLevel.BeforeMainSelect -> {
                binding.infoText.text = "아래 사진을 선택해\n메인 이미지를 변경할 수 있습니다."
            }
            InfoLevel.AfterMainSelect -> {
                binding.infoText.text = "Choise Best버튼을 클릭해\n메인 이미지를 추천 받을 수 있습니다."
            }
            else -> {}
        }
    }

    fun setSeekBar(){
        while(!imageContent.checkPictureList) {}
        Log.d("seekBar","#####")
        imageToolModule.showView(binding.seekBar, true)

        binding.seekBar.max = pictureList.size - 1
        binding.seekBar.progressDrawable =
            resources.getDrawable(R.drawable.custom_seekbar_progress, requireContext().theme)
        binding.seekBar.thumb =
            resources.getDrawable(R.drawable.custom_seekbar_thumb, requireContext().theme)
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // SeekBar의 값이 변경될 때 호출되는 메서드입니다.
                // progress 변수는 현재 SeekBar의 값입니다.
                // fromUser 변수는 사용자에 의해 변경된 값인지 여부를 나타냅니다.
                if (fromUser) {
                    val index = progress % pictureList.size
                    mainPicture = pictureList[index]


                    if (binding.candidateLayout.size > index) {
                        val view = binding.candidateLayout[index]
                        imageToolModule.showView(mainSubView, false)
                        mainSubView = view.findViewById<ImageView>(R.id.checkMainIcon)
                        imageToolModule.showView(mainSubView, true)
                    }

                    // 글라이드로만 seekbar 사진 변화 하면 좀 끊겨 보이길래
                    if (bitmapList.size > index) {
                        // 만들어 졌으면 비트맵으로 띄웠어
                        CoroutineScope(Dispatchers.Main).launch {
                            binding.burstMainView.setImageBitmap(bitmapList[index])
                        }
                    } else {
                        // 비트맵은 따로 만들고 있고 해당 index의 비트맵이 안만들어졌음명 글라이드로
                        CoroutineScope(Dispatchers.Main).launch {
                            Log.d("error 잡기", "$progress 번째 이미지 띄우기")
                            Glide.with(binding.burstMainView)
                                .load(imageContent.getJpegBytes(pictureList[index]))
                                .into(binding.burstMainView)
                        }
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // 사용자가 SeekBar를 터치하여 드래그를 시작할 때 호출되는 메서드입니다.
                // 필요한 작업을 수행하면 됩니다.
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // 사용자가 SeekBar에서 터치를 멈추었을 때 호출되는 메서드입니다.
                // 필요한 작업을 수행하면 됩니다.
            }
        })
    }


}