package com.goldenratio.onepic.EditModule.Fragment

import android.annotation.SuppressLint
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.toRectF
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.goldenratio.onepic.AllinJPEGModule.Content.ContentAttribute
import com.goldenratio.onepic.AllinJPEGModule.Content.Picture
import com.goldenratio.onepic.R
import com.goldenratio.onepic.databinding.FragmentMagicPictureBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class MagicPictureFragment : FaceBlendingFragment() {

    private lateinit var binding: FragmentMagicPictureBinding

    var boundingBox: ArrayList<ArrayList<Int>> = arrayListOf()

    var checkMagicPicturePlay = false
    val handler = Handler()

    var magicPlaySpeed: Long = 100

    val ovelapBitmap: ArrayList<Bitmap> = arrayListOf()

    var pictureList: ArrayList<Picture> = arrayListOf()

    private var infoLevel = MutableLiveData(InfoLevel.EditFaceSelect)

    private var touchEvent: PointF = PointF(0f,0f)

    private enum class InfoLevel {
        EditFaceSelect,
        MagicStart,
        ArrowCheck,
    }

    private enum class LoadingText {
        FaceDetection,
        MagicCreate,
        Save,
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // 뷰 바인딩 설정
        binding = FragmentMagicPictureBinding.inflate(inflater, container, false)
        showProgressBar(true, LoadingText.FaceDetection)

        settingMagicPictureFragment()

        setClickEvent()

        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.post {
            val joystickView = binding.joystick
            joystickView.setOnMoveListener({ angle, strength ->

                Log.d("joystick", "angle : $angle" + "strength : $strength")
                moveCropFace(angle, strength) }, 200)
        }
    }

    /**
     *  변수를 초기화한다.
     */
    private fun settingMagicPictureFragment() {
        imageContent = jpegViewModel.jpegAiContainer.value?.imageContent!!

        while (!imageContent.checkPictureList) {}

        // magic 가능한 연속 사진 속성의 picture list 얻음
        pictureList = jpegViewModel.jpegAiContainer.value!!.getPictureList(ContentAttribute.burst)

        // main Picture의 byteArray를 bitmap 제작
        selectPicture = imageContent.pictureList[jpegViewModel.getSelectedSubImageIndex()]

        // 메인 이미지 임시 설정
        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.Main) {
                Glide.with(binding.mainView)
                    .load(imageContent.getJpegBytes(selectPicture))
                    .into(binding.mainView)
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            // blending 가능한 연속 사진 속성의 picture list 얻음
            bitmapList = imageContent.getBitmapList(ContentAttribute.edited)
            if(bitmapList.size <= jpegViewModel.getSelectedSubImageIndex()) {
                imageContent.resetBitmap()
                imageContent.setBitmapList()
                bitmapList = imageContent.getBitmapList()
            }
            selectBitmap = bitmapList[jpegViewModel.getSelectedSubImageIndex()]

            faceDetectionModule.allFaceDetection(bitmapList)

            withContext(Dispatchers.Main) {
                // faceDetection 하고 결과가 표시된 사진을 받아 imaveView에 띄우기
                setMainImageBoundingBox()
            }
        }
    }

    /**
     * 이벤트 처리를 설정한다.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setClickEvent() {
        // save btn 클릭 시
        binding.magicSaveBtn.setOnClickListener {
            saveNewImage()
        }

        // close btn 클릭 시
        binding.magicCloseBtn.setOnClickListener {
            findNavController().navigate(R.id.action_magicPictureFragment_to_editFragment)
        }

        // 이미지 뷰 클릭 시
        binding.mainView.setOnTouchListener { _, event ->
            handler.removeCallbacksAndMessages(null)
            binding.magicPlayBtn.setImageResource(R.drawable.edit_magic_icon)
            checkMagicPicturePlay = false

            CoroutineScope(Dispatchers.Main).launch {
                binding.magicPlayBtn.visibility = View.GONE
                binding.bottomLayout.visibility = View.INVISIBLE
            }

            if (event!!.action == MotionEvent.ACTION_UP) {
//                imageToolModule.showView(binding.progressBar, true)

                // click 좌표를 bitmap에 해당하는 좌표로 변환
                val touchPoint = imageToolModule.getBitmapClickPoint(
                    PointF(event.x, event.y),
                    binding.mainView
                )

                touchEvent = PointF(event.x, event.y)

                Log.d("magic", "event touchEvent: $touchPoint")
                Log.d("magic", "bitmap touchPoint: $touchPoint")

                if (touchPoint != null) {
                    showProgressBar(true, LoadingText.MagicCreate)
                    CoroutineScope(Dispatchers.IO).launch {
                        // Click 좌표가 포함된 Bounding Box 얻음
                        while (!faceDetectionModule.getCheckFaceDetection()) {
                        }
                        Log.d("magic", "getCheckFaceDetection")

                        boundingBox = getBoundingBox(touchPoint)
                        Log.d("magic", "end getCheckFaceDetection")

                        if (boundingBox.size > 0) {
                            // Bounding Box로 이미지를 Crop한 후 보여줌
                            withContext(Dispatchers.Main) {
                                cropImgAndView(boundingBox)
                            }
                        }
                    }
                } else {
                    showProgressBar(false, null)
                }
            }
            return@setOnTouchListener true
        }

        // magicPlayBtn 클릭했을 때: magic pricture 실행 (움직이게 하기)
        binding.magicPlayBtn.setOnClickListener {
            if (!checkMagicPicturePlay) {
                infoLevel.value = InfoLevel.ArrowCheck

                // 이미지뷰의 레이아웃 파라미터를 가져옵니다.
                val layoutParams = binding.glitterView.layoutParams as ConstraintLayout.LayoutParams

                val width = selectFaceRect?.width() ?: 50
                val height = selectFaceRect?.height() ?: 50

                // 새로운 위치를 설정합니다.
                layoutParams.leftMargin = touchEvent.x.toInt() - (width / 2)  // 왼쪽 여백 설정
                layoutParams.topMargin = touchEvent.y.toInt() - (height / 2)   // 위쪽 여백 설정

                layoutParams.width = width
                layoutParams.height = height

                // 이미지뷰의 레이아웃 파라미터를 적용합니다.
                binding.glitterView.layoutParams = layoutParams
                imageToolModule.showView(binding.glitterView, true)

                Glide.with(binding.glitterView)
                    .load(R.raw.magic_twinkle)
                    .skipMemoryCache(true) // 메모리 캐시 비우기
                    .diskCacheStrategy(DiskCacheStrategy.NONE) // 디스크 캐시 비우기
                    .into(binding.glitterView)

                binding.glitterView.postDelayed({
                    imageToolModule.showView(binding.glitterView, false)
                    binding.glitterView.setImageDrawable(null)
                    magicPictureRun(cropBitmapList)
                    Glide.get(requireContext()).clearMemory()
                }, 1000)

                binding.magicPlayBtn.setImageResource(R.drawable.edit_magic_ing_icon)
                checkMagicPicturePlay = true
            } else {
                handler.removeCallbacksAndMessages(null)
                binding.magicPlayBtn.setImageResource(R.drawable.edit_magic_icon)
                checkMagicPicturePlay = false
            }
        }

        // info 확인
        binding.magicInfoBtn.setOnClickListener {
            imageToolModule.showView(binding.infoDialogLayout, true)
        }

        // info 삭제
        binding.dialogCloseBtn.setOnClickListener {
            imageToolModule.showView(binding.infoDialogLayout, false)
        }
    }

    /**
     * 이미지를 저장한다.
     */
    private fun saveNewImage() {
        CoroutineScope(Dispatchers.IO).launch {
            showProgressBar(true, LoadingText.Save)

            for (i in 0 until pictureList.size) {
                Log.d("magicPicture Check", " before ===== index= $i : ${pictureList[i].contentAttribute}")
                imageContent.pictureList[i].contentAttribute = ContentAttribute.burst
                Log.d("magicPicture Check", " after ===== index= $i : ${pictureList[i].contentAttribute}")
                imageContent.pictureList[i].embeddedData?.clear()
                imageContent.pictureList[i].embeddedSize = 0
            }

            // EmbeddedData 추가
            val indices = intArrayOf(5, 6, 7, 8) // 추출할 배열의 인덱스

            if (boundingBox.size > 0) {
                val mainBoundingBox: ArrayList<Int> =
                    boundingBox[0].filterIndexed { index, _ -> index in indices } as ArrayList<Int>

                mainBoundingBox.add(changeFaceStartX)
                mainBoundingBox.add(changeFaceStartY)

                if (boundingBox.size > 0 && boundingBox[0].size > 0 && pictureList.size > boundingBox[0][0]) {
                    pictureList[boundingBox[0][0]].insertEmbeddedData(mainBoundingBox)
                }
                for (i in 0 until pictureList.size) {
                    if (pictureList[i].contentAttribute == ContentAttribute.magic) {
                        pictureList[i].contentAttribute = ContentAttribute.burst
                    }
                }
                jpegViewModel.selectedSubImage?.contentAttribute = ContentAttribute.magic

                for (i in 1 until boundingBox.size) {
                    val addBoundingBox: ArrayList<Int> =
                        boundingBox[i].filterIndexed { index, _ -> index in indices } as ArrayList<Int>

                    addBoundingBox.add(changeFaceStartX)
                    addBoundingBox.add(changeFaceStartY)

                    if (pictureList.size > boundingBox[i][0]) {
                        pictureList[boundingBox[i][0]].insertEmbeddedData(addBoundingBox)
                    }
                }
            }

            imageContent.checkMagicCreated = true
            withContext(Dispatchers.Main) {
                findNavController().navigate(R.id.action_magicPictureFragment_to_editFragment)
            }
//            showProgressBar(false, null)
        }
    }

    /**
     * 선택된 이미지로 얼굴 감지 모델 실행 후, 감지된 얼굴이 표시한 후 화면에 출력한다.
     */
    override fun setMainImageBoundingBox() {
        CoroutineScope(Dispatchers.Main).launch {
            infoLevel.observe(viewLifecycleOwner) {
                infoTextView()
            }
        }

        if (checkMagicPicturePlay) {
            handler.removeCallbacksAndMessages(null)
            CoroutineScope(Dispatchers.Main).launch {
                binding.magicPlayBtn.setImageResource(R.drawable.magic_picture_play_icon)
            }
            checkMagicPicturePlay = false
        }


        CoroutineScope(Dispatchers.Default).launch {
            Log.d("magic", "!!!!!!!!!!!!!!!!!!! setMainImageBoundingBox")
            val faceResult = faceDetectionModule.getFaces(jpegViewModel.getSelectedSubImageIndex())

            Log.d("magic", "!!!!!!!!!!!!!!!!!!! end runFaceDetection")

            if (faceResult.size == 0) {
                withContext(Dispatchers.Main) {
                    try {
                        Toast.makeText(requireContext(), "사진에 얼굴이 존재하지 않습니다.", Toast.LENGTH_SHORT)
                            .show()
                        showProgressBar(false, null)
                    } catch (e: IllegalStateException) {
                        println(e.message)
                    }
                }
            } else {
                try {
                    val resultBitmap = imageToolModule.drawDetectionResult(selectBitmap, faceResult, requireContext().resources.getColor(R.color.white))

                    Log.d("magic", "!!!!!!!!!!!!!!!!!!! end drawDetectionResult")

                    // imageView 변환
                    withContext(Dispatchers.Main) {
                        binding.mainView.setImageBitmap(resultBitmap)
                    }
                } catch (e: IllegalStateException) {
                    // 예외가 발생한 경우 처리할 코드
                    e.printStackTrace() // 예외 정보 출력
                }
                showProgressBar(false, null)
            }
        }
    }

    /**
     * 자를 위치정보에 맞게 이미지를 자르고 화면에 띄어준다.
     *
     * @param boundingBox 자를 위치 정보
     */
    private fun cropImgAndView(boundingBox: ArrayList<ArrayList<Int>>) {
        // 감지된 모든 boundingBox 출력
        println("=======================================================")
        imageToolModule.showView(binding.arrowBar, true)
        changeSelectedView(selectBitmap)

        cropBitmapList.clear()

        if (bitmapList.size == 0) {
            showProgressBar(false, null)
            return
        }

        for (i in 0 until boundingBox.size) {
            println(i.toString() + " || " + boundingBox[i])

            // bounding rect 알아내기
            val rect = boundingBox[i]

            // bitmap를 자르기
            val cropImage = imageToolModule.cropBitmap(
                bitmapList[rect[0]], Rect(rect[1], rect[2], rect[3], rect[4])
            )

            Log.d("magicPictue", "rect[0] = ${rect[0]}")

            try {
                // 넣고자 하는 layout 불러오기
                val candidateLayout = layoutInflater.inflate(R.layout.candidate_image_array, null)

                // 위 불러온 layout에서 변경을 할 view가져오기
                val cropImageView: ImageView =
                    candidateLayout.findViewById(R.id.cropImageView)

                // 자른 사진 이미지뷰에 붙이기
                cropImageView.setImageBitmap(cropImage)

//                cropImageView.setOnClickListener {
                newImage = imageToolModule.cropBitmap(
                    bitmapList[rect[0]],
                    //bitmapList[rect[0]].copy(Bitmap.Config.ARGB_8888, true),
                    Rect(rect[5], rect[6], rect[7], rect[8])
                )
                cropBitmapList.add(newImage!!)

                // main activity에 만들어둔 scrollbar 속 layout의 아이디를 통해 해당 layout에 넣기
//                binding.magicCandidateLayout.addView(candidateLayout)
            } catch (e: IllegalStateException) {
                println(e.message)
            }
        }

        newImage = imageToolModule.circleCropBitmap(cropBitmapList[0])
        ovelapBitmap.add(
            imageToolModule.overlayBitmap(
                selectBitmap, newImage!!, changeFaceStartX, changeFaceStartY
            )
        )

//        imageToolModule.showView(binding.progressBar , false)
        showProgressBar(false, null)
        infoLevel.value = InfoLevel.MagicStart
    }

    /**
     * 잘라진 이미지를 가지고 움직이는 매직픽처를 재생한다.
     *
     * @param cropBitmapList 잘라진 이미지
     */
    private fun magicPictureRun(cropBitmapList: ArrayList<Bitmap>) {
        ovelapBitmap.clear()
        CoroutineScope(Dispatchers.Main).launch {
            for (i in 0 until cropBitmapList.size) {
                newImage = imageToolModule.circleCropBitmap(cropBitmapList[i])
                ovelapBitmap.add(
                    imageToolModule.overlayBitmap(
                        selectBitmap, newImage!!, changeFaceStartX, changeFaceStartY
                    )
                )
            }
            var currentImageIndex = 0
            var increaseIndex = 1

            val runnable = object : Runnable {
                override fun run() {
                    if (ovelapBitmap.size > 0) {
                        binding.mainView.setImageBitmap(ovelapBitmap[currentImageIndex])
                        //currentImageIndex++

                        currentImageIndex += increaseIndex

                        if (currentImageIndex >= ovelapBitmap.size - 1) {
                            //currentImageIndex = 0
                            increaseIndex = -1
                        } else if (currentImageIndex <= 0) {
                            increaseIndex = 1
                        }
                        handler.postDelayed(this, magicPlaySpeed)
                    }
                }
            }
            handler.postDelayed(runnable, magicPlaySpeed)
        }
    }


    /**
     * 잘라진 이미지를 방향과 강도에 따라 이동한다.
     *
     * @param angle 이동할 방향
     * @param strength 강도
     */
    private fun moveCropFace(angle: Int, strength: Int) {
        if(infoLevel.value != InfoLevel.EditFaceSelect) {
            imageToolModule.showView(binding.infoDialogLayout, false)
            infoLevel.value = InfoLevel.EditFaceSelect
        }

        if(checkMagicPicturePlay) {
            handler.removeCallbacksAndMessages(null)
            binding.magicPlayBtn.setImageResource(R.drawable.magic_picture_play_icon)
            imageToolModule.showView(binding.magicPlayBtn, true)
            checkMagicPicturePlay = false
        }

        if (newImage != null) {

            val newStrength = strength / 10

            val angleRadians = angle * (PI / 180.0)

            // x 좌표 계산
            val moveX = (newStrength * cos(angleRadians)).toInt()
            // y 좌표 계산
            val moveY = - (newStrength * sin(angleRadians)).toInt()

            changeFaceStartX += moveX
            changeFaceStartY += moveY

            ovelapBitmap[0] = imageToolModule.overlayBitmap(
                selectBitmap, newImage!!, changeFaceStartX, changeFaceStartY
            )

            binding.mainView.setImageBitmap(ovelapBitmap[0])
        }
    }

    /**
     * 도움말을 알맞게 띄운다.
     */
    override fun infoTextView() {
        Log.d("infoTextView","infoTextView call")
        when (infoLevel.value) {
            InfoLevel.EditFaceSelect -> {
                binding.infoText.text = "움직이길 원하는 얼굴을 누릅니다."
            }
            InfoLevel.MagicStart -> {
                binding.infoText.text = "아래 버튼을 통해\n매직 사진을 제작합니다."
            }
            InfoLevel.ArrowCheck -> {
                binding.infoText.text = "얼굴의 위치는\n조정 바를 통해 수정할 수 있습니다."
            }
            else -> {}
        }
    }

    /**
     * 로딩바를 설정한다.
     *
     * @param boolean 로딩바 보여줄지 여부
     * @param loadingText 로딩과 함께 보여질 텍스트
     */
    private fun showProgressBar(boolean: Boolean, loadingText: LoadingText?){
        setEnable(!boolean)

        if(boolean && isInfoViewed) {
            imageToolModule.showView(binding.infoDialogLayout, false)
        }
        else if (isInfoViewed) {
            imageToolModule.showView(binding.infoDialogLayout, true)
        }

        CoroutineScope(Dispatchers.Main).launch {
            binding.loadingText.text = when (loadingText) {
                LoadingText.FaceDetection -> {
                    "사진 분석 중.."
                }
                LoadingText.Save -> {
                    "편집 저장 중.."
                }
                LoadingText.MagicCreate -> {
                    "매직 사진 제작 중.."
                }
                else -> {
                    ""
                }
            }
        }

        imageToolModule.showView(binding.progressBar, boolean)
        imageToolModule.showView(binding.loadingText, boolean)
    }

    /**
     * 선택한 이미지임을 표시한다.
     *
     * @param bitmap 선택한 이미지
     */
    override fun changeSelectedView(bitmap: Bitmap) {
        if (selectFaceRect != null) {
            CoroutineScope(Dispatchers.IO).launch {
                val faceResult = faceDetectionModule.getFaces(jpegViewModel.getSelectedSubImageIndex())
                var resultBitmap = imageToolModule.drawDetectionResult(
                    selectBitmap,
                    faceResult,
                    requireContext().resources.getColor(R.color.white)
                )
                resultBitmap = imageToolModule.drawDetectionResult(
                    resultBitmap,
                    selectFaceRect!!.toRectF(),
                    requireContext().resources.getColor(R.color.select_face)
                )
                binding.mainView.setImageBitmap(resultBitmap)

                CoroutineScope(Dispatchers.Main).launch {
                    binding.magicPlayBtn.visibility = View.VISIBLE
                    binding.bottomLayout.visibility = View.VISIBLE
                }
            }
        }
    }

    /**
     * 버튼들의 터치 가능 여부를 조정한다.
     *
     * @param boolean 터치 가능 여부
     */
    private fun setEnable(boolean: Boolean) {
        CoroutineScope(Dispatchers.Main).launch {
            binding.magicCloseBtn.isEnabled = boolean
            binding.magicSaveBtn.isEnabled = boolean

            binding.magicInfoBtn.isEnabled = boolean
            binding.dialogCloseBtn.isEnabled = boolean
            binding.magicPlayBtn.isEnabled = boolean
//            binding.circleArrowBtn.isEnabled = boolean
        }
    }
}