package com.goldenratio.onepic.EditModule.Fragment

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.toRectF
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.goldenratio.onepic.EditModule.ArrowMoveClickListener
import com.goldenratio.onepic.EditModule.FaceDetectionModule
import com.goldenratio.onepic.ImageToolModule
import com.goldenratio.onepic.PictureModule.Contents.ContentAttribute
import com.goldenratio.onepic.PictureModule.Contents.Picture
import com.goldenratio.onepic.R
import com.goldenratio.onepic.databinding.FragmentMagicPictureBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MagicPictureFragment : FaceBlendingFragment() {

    private lateinit var binding: FragmentMagicPictureBinding

    var boundingBox: ArrayList<ArrayList<Int>> = arrayListOf()

    var checkMagicPicturePlay = false
    val handler = Handler()

    var magicPlaySpeed: Long = 100

    val ovelapBitmap: ArrayList<Bitmap> = arrayListOf()

    var pictureList: ArrayList<Picture> = arrayListOf()

    private lateinit var context: Context

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
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 상태바 색상 변경
        val window: Window = activity?.window
            ?: throw IllegalStateException("Fragment is not attached to an activity")
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(requireContext(), android.R.color.black)

        context = requireContext()

        // 뷰 바인딩 설정
        binding = FragmentMagicPictureBinding.inflate(inflater, container, false)

        imageContent = jpegViewModel.jpegMCContainer.value?.imageContent!!

        imageToolModule = ImageToolModule()
        faceDetectionModule = FaceDetectionModule()

        while (!imageContent.checkPictureList) {}

        // magic 가능한 연속 사진 속성의 picture list 얻음
        pictureList =
            jpegViewModel.jpegMCContainer.value!!.getPictureList(ContentAttribute.burst)

//        imageToolModule.showView(binding.progressBar, true)
        showProgressBar(true, LoadingText.FaceDetection)

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
            // rewind 가능한 연속 사진 속성의 picture list 얻음
            val newBitmapList = imageContent.getBitmapList(ContentAttribute.edited)

            if (newBitmapList != null) {
                val newSelectBitmap = newBitmapList[jpegViewModel.getSelectedSubImageIndex()]
                if (newSelectBitmap != null) {
                    selectBitmap = newSelectBitmap
                }
                bitmapList = newBitmapList

                faceDetectionModule.allFaceDetection(bitmapList)

                // faceDetection 하고 결과가 표시된 사진을 받아 imaveView에 띄우기
                setMainImageBoundingBox()
            }
        }
        // save btn 클릭 시
        binding.magicSaveBtn.setOnClickListener {

            CoroutineScope(Dispatchers.IO).launch {
//                imageToolModule.showView(binding.progressBar , true)
                showProgressBar(true, LoadingText.Save)
//                val allBytes = imageToolModule.bitmapToByteArray(selectBitmap, imageContent.getJpegBytes(selectPicture))

                for(i in 0 until pictureList.size) {
                    pictureList[i].embeddedData?.clear()
                    pictureList[i].embeddedSize = 0
                }

                // EmbeddedData 추가
                val indices = intArrayOf(5, 6, 7, 8) // 추출할 배열의 인덱스

                if (boundingBox.size > 0) {
                    val mainBoundingBox: ArrayList<Int> =
                        boundingBox[0].filterIndexed { index, _ -> index in indices } as ArrayList<Int>

                    mainBoundingBox.add(changeFaceStartX)
                    mainBoundingBox.add(changeFaceStartY)

                    if(boundingBox.size > 0 && boundingBox[0].size > 0 && pictureList.size > boundingBox[0][0]) {
                        pictureList[boundingBox[0][0]].insertEmbeddedData(mainBoundingBox)
                    }
                    for(i in 0 until pictureList.size) {
                        if(pictureList[i].contentAttribute == ContentAttribute.magic) {
                            pictureList[i].contentAttribute = ContentAttribute.burst
                        }
                    }
                    jpegViewModel.selectedSubImage?.contentAttribute = ContentAttribute.magic

                    for (i in 1 until boundingBox.size) {
//                        pictureList[boundingBox[i][0]].insertEmbeddedData(
//                            boundingBox[i].filterIndexed { index, _ -> index in indices } as ArrayList<Int>)

                        val addBoundingBox: ArrayList<Int> =
                            boundingBox[i].filterIndexed { index, _ -> index in indices } as ArrayList<Int>

                        addBoundingBox.add(changeFaceStartX)
                        addBoundingBox.add(changeFaceStartY)

                        if(pictureList.size > boundingBox[i][0]) {
                            pictureList[boundingBox[i][0]].insertEmbeddedData(addBoundingBox)
                        }
                    }
                }

                imageContent.checkMagicCreated = true
                withContext(Dispatchers.Main) {
                    try {
                        findNavController().navigate(R.id.action_magicPictureFragment_to_editFragment)
                    } catch (e: IllegalStateException) {
                        println(e.message)
                    }
                }

//                imageToolModule.showView(binding.progressBar, false)
                showProgressBar(false, null)
            }
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
//                    imageToolModule.showView(binding.progressBar, false)
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

                val glide = Glide.with(binding.glitterView)
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

        infoLevel.observe(viewLifecycleOwner){ _ ->
            infoTextView()
        }

        // info 확인
        binding.magicInfoBtn.setOnClickListener {
            imageToolModule.showView(binding.infoDialogLayout, true)
        }

        // info 삭제
        binding.dialogCloseBtn.setOnClickListener {
            imageToolModule.showView(binding.infoDialogLayout, false)
        }

        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.post {
            binding.circleArrowBtn.setOnTouchListener(ArrowMoveClickListener(::moveCropFace, binding.maxArrowBtn, binding.circleArrowBtn))
        }
    }

    override fun setMainImageBoundingBox() {

        if (checkMagicPicturePlay) {
            handler.removeCallbacksAndMessages(null)
            CoroutineScope(Dispatchers.Main).launch {
                binding.magicPlayBtn.setImageResource(R.drawable.magic_picture_play_icon)
            }
            checkMagicPicturePlay = false
        }


        CoroutineScope(Dispatchers.Default).launch {
            Log.d("magic", "!!!!!!!!!!!!!!!!!!! setMainImageBoundingBox")
            val faceResult = faceDetectionModule.runFaceDetection(0)

            Log.d("magic", "!!!!!!!!!!!!!!!!!!! end runFaceDetection")

            if (faceResult.size == 0) {
                withContext(Dispatchers.Main) {
                    try {
                        Toast.makeText(requireContext(), "사진에 얼굴이 존재하지 않습니다.", Toast.LENGTH_SHORT)
                            .show()
//                        imageToolModule.showView(binding.progressBar, false)
                        showProgressBar(false, null)
                    } catch (e: IllegalStateException) {
                        println(e.message)
                    }
                }
            } else {
                try {
                    var resultBitmap = imageToolModule.drawDetectionResult(selectBitmap, faceResult, requireContext().resources.getColor(R.color.white))

//                    faceResult.forEach {
//                        resultBitmap = drawMagicIcon(resultBitmap,  it.boundingBox.toRectF(), requireContext().resources.getColor(R.color.white))
//                    }

                    Log.d("magic", "!!!!!!!!!!!!!!!!!!! end drawDetectionResult")

                    // imageView 변환
                    withContext(Dispatchers.Main) {
                        binding.mainView.setImageBitmap(resultBitmap)
                    }
                } catch (e: IllegalStateException) {
                    // 예외가 발생한 경우 처리할 코드
                    e.printStackTrace() // 예외 정보 출력
                }
//                imageToolModule.showView(binding.progressBar, false)
                showProgressBar(false, null)
            }
        }
    }

    /**
     *  cropImgAndView(boundingBox: ArrayList<List<Int>>)
     *         - 이미지를 자르고 화면에 띄어줌
     */
    private fun cropImgAndView(boundingBox: ArrayList<ArrayList<Int>>) {
        // 감지된 모든 boundingBox 출력
        println("=======================================================")
//        binding.magicCandidateLayout.removeAllViews()
        imageToolModule.showView(binding.arrowBar, true)
        changeMainView(selectBitmap)

        cropBitmapList.clear()

        if (bitmapList.size == 0) {
//            imageToolModule.showView(binding.progressBar , false)
            showProgressBar(false, null)
            return
        }

//        imageToolModule.showView(binding.bottomLayout, true)
//        imageToolModule.showView(binding.magicPlayBtn, true)

        for (i in 0 until boundingBox.size) {
            println(i.toString() + " || " + boundingBox[i])

            // bounding rect 알아내기
            val rect = boundingBox[i]

            // bitmap를 자르기
            val cropImage = imageToolModule.cropBitmap(
                bitmapList[rect[0]],
                //bitmapList[rect[0]].copy(Bitmap.Config.ARGB_8888, true),
                Rect(rect[1], rect[2], rect[3], rect[4])
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

    private fun moveCropFace(moveX:Int, moveY:Int) {
        if(infoLevel.value != InfoLevel.EditFaceSelect) {
            imageToolModule.showView(binding.infoDialogLayout, false)
            infoLevel.value = InfoLevel.EditFaceSelect
        }

        if(checkMagicPicturePlay) {
            handler.removeCallbacksAndMessages(null)
            binding.magicPlayBtn.setImageResource(R.drawable.magic_picture_play_icon)
            checkMagicPicturePlay = false
        }

        if (newImage != null) {

            changeFaceStartX += moveX
            changeFaceStartY += moveY

            ovelapBitmap[0] = imageToolModule.overlayBitmap(
                selectBitmap, newImage!!, changeFaceStartX, changeFaceStartY
            )

            binding.mainView.setImageBitmap(ovelapBitmap[0])
        }
    }

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

    private fun showProgressBar(boolean: Boolean, loadingText: LoadingText?){
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



    override fun changeMainView(bitmap: Bitmap) {
        if(selectFaceRect != null) {
            CoroutineScope(Dispatchers.IO).launch {
                val faceResult = faceDetectionModule.runFaceDetection(0)
                var resultBitmap = imageToolModule.drawDetectionResult(selectBitmap, faceResult, requireContext().resources.getColor(R.color.white))
                resultBitmap = imageToolModule.drawDetectionResult(resultBitmap, selectFaceRect!!.toRectF(), requireContext().resources.getColor(R.color.select_face))
                binding.mainView.setImageBitmap(resultBitmap)

                    CoroutineScope(Dispatchers.Main).launch {
                        binding.magicPlayBtn.visibility = View.VISIBLE
                        binding.bottomLayout.visibility = View.VISIBLE
                    }
                }
        }
    }
}