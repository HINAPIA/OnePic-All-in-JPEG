package com.goldenratio.onepic.EditModule.Fragment

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toRectF
import androidx.core.view.drawToBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.goldenratio.onepic.EditModule.BlurBitmapUtil
import com.goldenratio.onepic.EditModule.ObjectExtractModule
import com.goldenratio.onepic.ImageToolModule
import com.goldenratio.onepic.JpegViewModel
import com.goldenratio.onepic.PictureModule.Contents.ContentAttribute
import com.goldenratio.onepic.PictureModule.Contents.Picture
import com.goldenratio.onepic.PictureModule.ImageContent
import com.goldenratio.onepic.R
import com.goldenratio.onepic.databinding.FragmentFocusChangeBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class FocusChangeFragment : Fragment() {


    private lateinit var binding: FragmentFocusChangeBinding
    lateinit var fragment: Fragment
    private val jpegViewModel by activityViewModels<JpegViewModel>()
    private lateinit var imageContent : ImageContent

    private var pictureList = arrayListOf<Picture>()
    private var bitmapList = arrayListOf<Bitmap>()
    private lateinit var mainPicture : Picture
    private var boundingBoxList = arrayListOf<ArrayList<Int>>()

    private lateinit var originalSelectBitmap: Bitmap
    protected lateinit var selectBitmap: Bitmap
    private var PreSelectBitmap: Bitmap? = null
    protected var newImage: Bitmap? = null

    private lateinit var imageToolModule: ImageToolModule
    private var mainIndex = 0

    private lateinit var mainSubView: View

    private lateinit var checkFinish: BooleanArray

    private var infoLevel = MutableLiveData<InfoLevel>()

    val max = 5

    // 객체별 boundingBox Resize된 ArrayList를 저장하는 ArrayList
    val boundingBoxResizeList = arrayListOf<ArrayList<Int>>()
    protected var selectObjRect: Rect? = null

    private lateinit var objectExtractModule: ObjectExtractModule

    private val maxBlurRadius: Float = 21f
    private var curBlurRadius = 10f

    enum class InfoLevel {
        BeforeMainSelect,
        AfterMainSelect
    }

    private enum class LoadingText {
        Save,
        Change,
        Focus
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
        binding = FragmentFocusChangeBinding.inflate(inflater, container, false)

        imageContent = jpegViewModel.jpegMCContainer.value?.imageContent!!
        imageToolModule = ImageToolModule()
        objectExtractModule = ObjectExtractModule()

        checkFinish = BooleanArray(pictureList.size)


        while(!imageContent.checkPictureList) {}

        mainPicture = imageContent.mainPicture
        pictureList = imageContent.pictureList

        for(i in 0 until pictureList.size){
            boundingBoxList.add(pictureList[i].embeddedData!!)
        }

        CoroutineScope(Dispatchers.Default).launch {
            val bitmap = imageContent.getBitmapList()
            if(bitmap!=null)
                bitmapList = bitmap
        }

// TODO : 이 코드 왜 필요한지 물어보기

//        CoroutineScope(Dispatchers.Main).launch {
//            // 파일을 parsing해서 PictureContainer로 바꾸는 함수 호출
//            // 메인 이미지 설정
//            withContext(Dispatchers.Main) {
//                Glide.with(binding.focusMainView)
//                    .load(imageContent.getJpegBytes(mainPicture))
//                    .into(binding.focusMainView)
//            }
//        }

        CoroutineScope(Dispatchers.Default).launch {
            val allBitmapList = imageContent.getBitmapList()
//            val index = jpegViewModel.getSelectedSubImageIndex()
            val index = jpegViewModel.getMainSubImageIndex()
            val newMainBitmap = allBitmapList?.get(index)
            if (newMainBitmap != null) {
                selectBitmap = newMainBitmap
            }
            originalSelectBitmap = selectBitmap
        }

        CoroutineScope(Dispatchers.IO).launch {
            // rewind 가능한 연속 사진 속성의 picture list 얻음
            Log.d("focus", "newBitmapList call before")
            val newBitmapList = imageContent.getBitmapList(ContentAttribute.edited)
            Log.d("focus", "newBitmapList $newBitmapList")
            if (newBitmapList != null) {
//                bitmapList = newBitmapList
//
//                rewindModule.allFaceDetection(bitmapList)
//
//                selectBitmap = rewindModule.autoBestFaceChange(bitmapList)

                // faceDetection 하고 결과가 표시된 사진을 받아 imaveView에 띄우기
                setMainImageBoundingBox()
            }
        }

//        Thread.sleep(3000)
        Log.d("error 잡기", "focusEdit picureList size ${pictureList.size}")
//        if(imageContent.activityType == ActivityType.Viewer) {
            infoLevel.value = InfoLevel.BeforeMainSelect
            infoLevel.observe(viewLifecycleOwner){ _ ->
                infoTextView()
            }
//        }
//        else {
//            infoLevel.value = InfoLevel.AfterMainSelect
//            infoTextView()
//        }

        CoroutineScope(Dispatchers.Default).launch {
            if (imageContent.checkAttribute(ContentAttribute.object_focus)) {
                setSeekBar()
            }
        }

        setClickEvent()

        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setClickEvent(){

        // 이미지 뷰 클릭 시
        binding.focusMainView.setOnTouchListener { _, event ->
            if (event!!.action == MotionEvent.ACTION_UP) {
//                if (!isSelected) {
                    showProgressBar(true, LoadingText.Change)

//                    if (PreSelectBitmap != null) {
//                        selectBitmap = PreSelectBitmap!!
//                        newImage = null
//                    }
                    // click 좌표를 bitmap에 해당하는 좌표로 변환
                    val touchPoint = ImageToolModule().getBitmapClickPoint(
                        PointF(event.x, event.y),
                        binding.focusMainView
                    )
                    println("------- click point:$touchPoint")

                    if (touchPoint != null) {

                        CoroutineScope(Dispatchers.Default).launch {
                            // Click 좌표가 포함된 Bounding Box 얻음
//                            while (!rewindModule.getCheckFaceDetection()) {
//                            }
                            val index = getIndex(touchPoint)

                            Log.d("focus test","getIndex out : $index")

                            if(index == null) return@launch
                            else {
                                val newBitmapList = imageContent.getBitmapList(ContentAttribute.edited)
                                Log.d("faceRewind", "newBitmapList $newBitmapList")
                                if (newBitmapList != null) {
                                    bitmapList = newBitmapList
                                    selectBitmap = bitmapList?.get(index)!!

                                    // bitmap 자르기
                                    val selectBoundingBox = boundingBoxResizeList[index]
                                    selectObjRect = Rect(selectBoundingBox[0], selectBoundingBox[1], selectBoundingBox[2], selectBoundingBox[3])

                                    val cropBitmap = bitmapCropRect(selectBitmap, selectObjRect!!)
                                    val blurSelectBitmap = BlurBitmapUtil.blur(requireContext(), selectBitmap, curBlurRadius)
                                    val resultBitmap = mergeBitmaps(blurSelectBitmap, cropBitmap, selectObjRect!!.left, selectObjRect!!.top)

                                    withContext(Dispatchers.Main) {
                                        binding.focusMainView.setImageBitmap(resultBitmap)
                                    }
                                }
                            }
//                            if (boundingBox.size > 0) {
//                                // Bounding Box로 이미지를 Crop한 후 보여줌
//                                withContext(Dispatchers.Main) {
//                                    cropImgAndView(boundingBox)
//                                }
//                            }
                        }
                    } else {
//                    imageToolModule.showView(binding.progressBar, false)
                        showProgressBar(false, null)
                    }
//                }
            }
            return@setOnTouchListener true
        }

        binding.focusSaveBtn.setOnClickListener {
            imageContent.resetBitmap()
            imageToolModule.showView(binding.progressBar, true)
            CoroutineScope(Dispatchers.Default).launch {
                var result = imageContent.removePicture(mainPicture)
                Log.d("error 잡기", "메인 바꾸고 save : ${result}")
                if (result) {
                    Log.d("error 잡기", "main으로 지정된 객체 삭제 완료")

                    // 2. main 사진을 첫번 째로 삽입
                    imageContent.insertPicture(0, mainPicture)
                    imageContent.mainPicture = mainPicture
                }

                withContext(Dispatchers.Main) {
                    Log.d("error 잡기", "바로 편집에서 navigate호출 전")
                    imageContent.checkMainChanged = true
                    findNavController().navigate(R.id.action_focusChangeFragment_to_Fragment)
                }
                imageToolModule.showView(binding.progressBar, false)

            }
        }

        binding.focusCloseBtn.setOnClickListener {
            CoroutineScope(Dispatchers.Default).launch {
                withContext(Dispatchers.Main) {
                    findNavController().navigate(R.id.action_focusChangeFragment_to_Fragment)
                }
            }
        }

        // info 확인
        binding.focusInfoBtn.setOnClickListener {
            imageToolModule.showView(binding.infoDialogLayout, true)
        }

        // info 삭제
        binding.dialogCloseBtn.setOnClickListener {
            imageToolModule.showView(binding.infoDialogLayout, false)
        }
    }

    fun mergeBitmaps(blurredBitmap: Bitmap, cropBitmap: Bitmap, x: Int, y: Int): Bitmap {
        val resultBitmap = Bitmap.createBitmap(blurredBitmap.width, blurredBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)

        // 블러 처리된 이미지 그리기
        canvas.drawBitmap(blurredBitmap, 0f, 0f, null)

        // crop된 이미지 그리기
        val matrix = Matrix()
        matrix.postTranslate(x.toFloat(), y.toFloat())
        canvas.drawBitmap(cropBitmap, matrix, null)

        return resultBitmap
    }

    fun bitmapCropRect(bitmap: Bitmap, boundingBox: Rect): Bitmap {
        val left = boundingBox.left
        val top = boundingBox.top
        val right = boundingBox.right
        val bottom = boundingBox.bottom

        val width = right - left
        val height = bottom - top

        val croppedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(croppedBitmap)
        val srcRect = Rect(left, top, right, bottom)
        val dstRect = Rect(0, 0, width, height)
        canvas.drawBitmap(bitmap, srcRect, dstRect, null)

        return croppedBitmap
    }

    /**
     * getIndex(touchPoint: Point): ArrayList<List<Int>>
     *     - click된 포인트를 알려주면,
     *       해당 포인트가 객체 감지 결과 bounding Box 속에 존재하는지 찾아서
     *       만약 포인트를 포함하는 boundingBox를 찾으면 모아 return
     */
    suspend fun getIndex(touchPoint: Point): Int? = suspendCoroutine { box ->
//        val boundingBox: ArrayList<ArrayList<Int>> = arrayListOf()
        var index: Int? = null

        val checkFinish = BooleanArray(bitmapList.size)
        for (i in 0 until bitmapList.size) {
            checkFinish[i] = false
        }

        CoroutineScope(Dispatchers.IO).launch {
            if (bitmapList.size == 0) {
                showProgressBar(false, null)
                return@launch
            }

            index = getClickPointBoundingBox(touchPoint)

            Log.v("focus test", "index : $index")

            if (index == null) {
                withContext(Dispatchers.Main) {
                    try {
                        Toast.makeText(
                            requireContext(),
                            "해당 좌표에 객체가 존재 하지 않습니다.",
                            Toast.LENGTH_LONG
                        )
                            .show()
                    } catch (e: IllegalStateException) {
                        println(e.message)
                    }
                }
                // 메인 사진의 boundingBox에 인지된 얼굴이 없을 때
                // faceDetection하고 결과가 표시된 사진을 받아 imaveView에 띄우기
//                setMainImageBoundingBox()

                checkFinish.fill(true) // 배열의 모든 요소를 true로 설정
            } else {

                for (i in 0 until bitmapList.size)
                    checkFinish[i] = true

//                mainPicture = pictureList[index]
//
//                // 글라이드로만 seekbar 사진 변화 하면 좀 끊겨 보이길래
//                if (bitmapList.size > index) {
//                    // 만들어 졌으면 비트맵으로 띄웠어
//                    CoroutineScope(Dispatchers.Main).launch {
//                        binding.focusMainView.setImageBitmap(bitmapList[index])
//                    }
//                } else {
//                    // 비트맵은 따로 만들고 있고 해당 index의 비트맵이 안만들어졌음명 글라이드로
//                    CoroutineScope(Dispatchers.Main).launch {
//                        Glide.with(binding.focusMainView)
//                            .load(imageContent.getJpegBytes(pictureList[index]))
//                            .into(binding.focusMainView)
//                    }
//                }
//                setMainImageBoundingBox()

            }
            while (!checkFinish.all { it }) {
            }
            box.resume(index)
        }
    }

    open fun changeMainView(bitmap: Bitmap) {
//        if(selectFaceRect != null) {
        val newBitmap = imageToolModule.drawFocusResult(bitmap, selectObjRect!!.toRectF(),
            requireContext().resources.getColor(R.color.focus), requireContext().resources.getColor(R.color.focus_30))
        binding.focusMainView.setImageBitmap(newBitmap)
//        }
    }

    suspend fun getClickPointBoundingBox(point: Point): Int? =
        suspendCoroutine { bitmapResult ->

            var checkResume = false

            for(i in 0 until boundingBoxResizeList.size) {
                val obj = makeRect(boundingBoxResizeList[i])

                if(imageToolModule.checkPointInRect(point, obj)) {
                    checkResume = true
                    bitmapResult.resume(i)
//                    val boundingBox = listOf<Int>( obj.left, obj.top, obj.right, obj.bottom )
//                    bitmapResult.resume(boundingBox)
                    break
                }
                if(!checkResume && i == boundingBoxResizeList.size) bitmapResult.resume(null)
            }
        }

    /**
     * setMainImageBoundingBox()
     *      - mainImage를 focus가 사각형으로 표시된 사진으로 imageView 변환
     */
    open fun setMainImageBoundingBox() {

//        if(infoLevel.value != RewindFragment.InfoLevel.EditFaceSelect) {
//            CoroutineScope(Dispatchers.Main).launch {
//                infoLevel.value = RewindFragment.InfoLevel.EditFaceSelect
//                isInfoViewed = false
//
//                imageToolModule.showView(binding.infoDialogLayout, false)
//            }
//        }

//        //showView(binding.faceListView, false)
//        imageToolModule.showView(binding.arrowBar, false)
//        if (!binding.candidateLayout.isEmpty()) {
//            CoroutineScope(Dispatchers.Main).launch {
//                withContext(Dispatchers.Main) {
//                    binding.candidateLayout.removeAllViews()
//                }
//            }
//        }

        CoroutineScope(Dispatchers.Default).launch {
//            Log.d("checkPictureList", "!!!!!!!!!!!!!!!!!!! setMainImageBoundingBox")
//            val faceResult = rewindModule.runFaceDetection(0)
////            val faceResult = rewindModule.runMainFaceDetection(selectBitmap)
//            Log.d("checkPictureList", "!!!!!!!!!!!!!!!!!!! end runFaceDetection")
//
//            if (faceResult.size == 0) {
//                withContext(Dispatchers.Main) {
//                    try {
//                        Toast.makeText(requireContext(), "사진에 얼굴이 존재하지 않습니다.", Toast.LENGTH_SHORT)
//                            .show()
////                        imageToolModule.showView(binding.progressBar, false)
//                        showProgressBar(false, null)
//                        imageToolModule.showView(binding.rewindMenuLayout, true)
//                    } catch (e: IllegalStateException) {
//                        println(e.message)
//                    }
//                }
//            } else {
                try {
                    for (i in 0 until boundingBoxList.size) {
                        val arraylist = arrayListOf<Int>()
                        val scale = selectBitmap.width/480F

                        val left = (boundingBoxList[i][0].toFloat() * scale).toInt()
                        val top = (boundingBoxList[i][1].toFloat() * scale).toInt()
                        val right = (boundingBoxList[i][2].toFloat() * scale).toInt()
                        val bottom = (boundingBoxList[i][3].toFloat() * scale).toInt()

                        arraylist.add(left)
                        arraylist.add(top)
                        arraylist.add(right)
                        arraylist.add(bottom)

                        boundingBoxResizeList.add(arraylist)
                    }

                    val resultBitmap =
                        imageToolModule.drawFocusResult(selectBitmap, boundingBoxResizeList,
                            requireContext().resources.getColor(R.color.focus), requireContext().resources.getColor(R.color.focus_30))

                    // imageView 변환
                    withContext(Dispatchers.Main) {
                        binding.focusMainView.setImageBitmap(resultBitmap)
                    }
                } catch (e: IllegalStateException) {
                    println(e.message)
                }
//            }
            imageToolModule.showView(binding.progressBar, false)
            showProgressBar(false, null)
        }
    }

    private fun showProgressBar(boolean: Boolean, loadingText: LoadingText?){
//        if(boolean && isInfoViewed) {
//            imageToolModule.showView(binding.infoDialogLayout, false)
//        }
//        else if (isInfoViewed) {
//            imageToolModule.showView(binding.infoDialogLayout, true)
//        }
//
//        CoroutineScope(Dispatchers.Main).launch {
//            binding.loadingText.text = when (loadingText) {
////                LoadingText.FaceDetection -> {
////                    "자동 Face Blending 중"
////                }
//                LoadingText.Save -> {
//                    "편집을 저장 중.."
//                }
//                LoadingText.AutoRewind -> {
//                    "최적의 Blending 사진 제작 중.."
//                }
//                else -> {
//                    ""
//                }
//            }
//        }
//
//        imageToolModule.showView(binding.progressBar, boolean)
//        imageToolModule.showView(binding.loadingText, boolean)
    }

    fun makeRect(arraylist : ArrayList<Int>) : Rect {
        val rect = Rect()
        rect.left = arraylist[0]
        rect.top = arraylist[1]
        rect.right = arraylist[2]
        rect.bottom = arraylist[3]

        return rect
    }

    fun infoTextView() {
        Log.d("infoTextView","infoTextView call")
        when (infoLevel.value) {
            InfoLevel.BeforeMainSelect -> {
                binding.infoText.text = "아래 사진을 선택해\n메인 이미지를 변경할 수 있습니다."
            }
            InfoLevel.AfterMainSelect -> {
                binding.infoText.text = "choice Best버튼을 클릭해\n메인 이미지를 추천 받을 수 있습니다."
            }
            else -> {}
        }
    }

    fun setSeekBar(){
//        while(!imageContent.checkPictureList) {}

        imageToolModule.showView(binding.seekBar, true)

        binding.seekBar.max = maxBlurRadius.toInt()  // 0 ~ 21 ( 1 ~ 22 )
        binding.seekBar.progress = curBlurRadius.toInt() + 1 // 현재 10f면 11f로 설정

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // SeekBar의 값이 변경될 때 호출되는 메서드입니다.
                // progress 변수는 현재 SeekBar의 값입니다.
                // fromUser 변수는 사용자에 의해 변경된 값인지 여부를 나타냅니다.
                if (fromUser) {
                    // SeekBar의 진행 상태에 따라 블러 강도 조절
                    curBlurRadius = progress.toFloat() + 1
                    // 블러를 다시 적용하여 이미지 업데이트
                    applyBlur()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    // 블러를 적용하여 이미지 업데이트
    private fun applyBlur() {
        val cropBitmap = bitmapCropRect(selectBitmap, selectObjRect!!)
        val blurSelectBitmap = BlurBitmapUtil.blur(requireContext(), selectBitmap, curBlurRadius)
        val resultBitmap = mergeBitmaps(blurSelectBitmap, cropBitmap, selectObjRect!!.left, selectObjRect!!.top)

        binding.focusMainView.setImageBitmap(resultBitmap)
    }
}