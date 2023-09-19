package com.goldenratio.onepic.EditModule.Fragment

import android.annotation.SuppressLint
import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.core.graphics.toRectF
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import com.goldenratio.onepic.EditModule.BlurBitmapUtil
import com.goldenratio.onepic.ImageToolModule
import com.goldenratio.onepic.JpegViewModel
import com.goldenratio.onepic.AllinJPEGModule.Contents.ContentAttribute
import com.goldenratio.onepic.AllinJPEGModule.Contents.Picture
import com.goldenratio.onepic.AllinJPEGModule.ImageContent
import com.goldenratio.onepic.R
import com.goldenratio.onepic.databinding.FragmentFocusChangeBinding
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class FocusChangeFragment : Fragment() {

    // focus가 어디에 잡혔는지 표시되어있는 bitmap
    private var focusCheckingBitmap: Bitmap? = null
    private var resultBitmap: Bitmap?  = null
    private var index: Int? = null
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

    private lateinit var imageToolModule: ImageToolModule

    private lateinit var checkFinish: BooleanArray

    private var infoLevel = MutableLiveData<InfoLevel>()

    val max = 5

    // 객체별 boundingBox Resize된 ArrayList를 저장하는 ArrayList
    val boundingBoxResizeList = arrayListOf<ArrayList<Int>>()
    protected var selectObjRect: Rect? = null

    private val maxBlurRadius: Float = 24f
    private var curBlurRadius = 10.0f

    enum class InfoLevel {
        BeforeMainSelect,
        AfterMainSelect
    }

    private enum class LoadingText {
        Save
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

        imageContent = jpegViewModel.jpegAiContainer.value?.imageContent!!
        imageToolModule = ImageToolModule()

        checkFinish = BooleanArray(pictureList.size)


        while(!imageContent.checkPictureList) {}

        mainPicture = imageContent.mainPicture
        pictureList = imageContent.pictureList

        imageToolModule.showView(binding.blurSettingLinearLayout, false)

        for(i in 0 until pictureList.size){
            if(pictureList[i].contentAttribute == ContentAttribute.object_focus) {
                boundingBoxList.add(pictureList[i].embeddedData!!)
            }
        }

        CoroutineScope(Dispatchers.Default).launch {
            val bitmap = imageContent.getBitmapList()
            if(bitmap!=null)
                bitmapList = bitmap
        }

        CoroutineScope(Dispatchers.Default).launch {
            val allBitmapList = imageContent.getBitmapList()

            val index = jpegViewModel.getMainSubImageIndex()
            val newMainBitmap = allBitmapList?.get(index)
            if (newMainBitmap != null) {
                selectBitmap = newMainBitmap
            }
            originalSelectBitmap = selectBitmap
        }

        CoroutineScope(Dispatchers.IO).launch {
            // blending 가능한 연속 사진 속성의 picture list 얻음
            Log.d("focus", "newBitmapList call before")
            val newBitmapList = imageContent.getBitmapList(ContentAttribute.edited)
            Log.d("focus", "newBitmapList $newBitmapList")
            if (newBitmapList != null) {
                // faceDetection 하고 결과가 표시된 사진을 받아 imaveView에 띄우기
                setMainImageBoundingBox()
            }
        }


        Log.d("error 잡기", "focusEdit picureList size ${pictureList.size}")

        infoLevel.value = InfoLevel.BeforeMainSelect
        infoLevel.observe(viewLifecycleOwner){ _ ->
            infoTextView()
        }

        setClickEvent()

        return binding.root
    }

    /**
     * 이벤트 처리를 설정한다.
     */
    @SuppressLint("ClickableViewAccessibility")
    fun setClickEvent(){
        // 이미지 뷰 클릭 시
        binding.focusMainView.setOnTouchListener { _, event ->
            if (event!!.action == MotionEvent.ACTION_UP) {
                // click 좌표를 bitmap에 해당하는 좌표로 변환
                val touchPoint = ImageToolModule().getBitmapClickPoint(
                    PointF(event.x, event.y),
                    binding.focusMainView
                )
                println("------- click point:$touchPoint")

                if (touchPoint != null) {

                    CoroutineScope(Dispatchers.Default).launch {
                        // Click 좌표가 포함된 Bounding Box 얻음
                        index = getIndex(touchPoint)

                        Log.d("focus test","getIndex out : $index")

                        if(index == null) return@launch
                        else {
                            val newBitmapList = imageContent.getBitmapList(ContentAttribute.edited)

                            if (newBitmapList != null) {

                                bitmapList = newBitmapList
                                selectBitmap = bitmapList[index!!]

                                // bitmap 자르기
                                val selectBoundingBox = boundingBoxResizeList[index!!]
                                selectObjRect = Rect(selectBoundingBox[0], selectBoundingBox[1], selectBoundingBox[2], selectBoundingBox[3])

                                val cropBitmap = bitmapCropRect(selectBitmap, selectObjRect!!)
                                val blurSelectBitmap = BlurBitmapUtil.blur(requireContext(), selectBitmap, curBlurRadius)
                                resultBitmap = mergeBitmaps(blurSelectBitmap, cropBitmap, selectObjRect!!.left, selectObjRect!!.top)

                                withContext(Dispatchers.Main) {
                                    binding.focusMainView.setImageBitmap(resultBitmap)
                                }

                                imageToolModule.showView(binding.blurSettingLinearLayout, true)
                                setSeekBar()
                            }
                        }
                    }
                }
            }
            return@setOnTouchListener true
        }

        // compare 버튼 클릭시
        binding.imageCompareBtn.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    binding.focusMainView.setImageBitmap(focusCheckingBitmap)
                    return@setOnTouchListener true
                }
                MotionEvent.ACTION_UP -> {
                    binding.focusMainView.setImageBitmap(resultBitmap)
                    return@setOnTouchListener true
                }
                else -> return@setOnTouchListener false
            }
        }

        binding.focusSaveBtn.setOnClickListener {
//            imageContent.resetBitmap()
            showProgressBar(true, LoadingText.Save)
            CoroutineScope(Dispatchers.Default).launch {
                Log.d("error 잡기", "main으로 지정된 객체 삭제 완료")

                if(index != null && resultBitmap != null) {

                    val allBytes = imageToolModule.bitmapToByteArray(
                        resultBitmap!!,
                        imageContent.getJpegBytes(pictureList[index!!])
                    )
                    val app1Segment = imageContent.extractAPP1(allBytes)
                    val frame =async {
                        imageContent.extractSOI(allBytes)
                    }
                    val picture = Picture(ContentAttribute.edited, app1Segment, frame.await())
                    imageContent.pictureList.add(picture)

                    picture.waitForByteArrayInitialized()

                    jpegViewModel.setPictureByteList(imageContent.getJpegBytes(imageContent.pictureList[index!!]), pictureList.size-1)

                    jpegViewModel.selectedSubImage = imageContent.pictureList[pictureList.size-1]
                }

//                }

                withContext(Dispatchers.Main) {
                    Log.d("error 잡기", "바로 편집에서 navigate호출 전")
                    imageContent.checkMainChanged = true
                    findNavController().navigate(R.id.action_focusChangeFragment_to_Fragment)
                }
                showProgressBar(false, null)
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
            binding.infoText.text = "초점이 잡힌 객체를 선택하여 심도를 조절할 수 있습니다."
            imageToolModule.showView(binding.infoDialogLayout, true)
        }

        // info 삭제
        binding.dialogCloseBtn.setOnClickListener {
            imageToolModule.showView(binding.infoDialogLayout, false)
        }
    }

    /**
     * 하나의 이미지에 블러 처리된 이미지 그리고, 잘라진 이미지 그려진 이미지를 반환한다.
     *
     * @param blurredBitmap 블러 처리된 이미지
     * @param cropBitmap 잘라진 이미지
     * @param x 그릴 x 값
     * @param y 그릴 y 값
     * @return 요청사항이 모두 그려진 이미지 반환
     */
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
     * 터치된 좌표가 객체 감지 결과 중 객체 위치 정보(bounding Box) 속에 포함되는지 알아낸 후,
     * 터치 좌표에 초점이 맞춰진 이미지 index를 반환한다.
     *
     * @param touchPoint 터치된 좌표 정된
     * @return 해당 객체에 초점이 맞춰진 이미지 index 반환
     */
    private suspend fun getIndex(touchPoint: Point): Int? = suspendCoroutine { box ->
        var index: Int?

        val checkFinish = BooleanArray(bitmapList.size)
        for (i in 0 until bitmapList.size) {
            checkFinish[i] = false
        }

        CoroutineScope(Dispatchers.IO).launch {
            if (bitmapList.size == 0) {
                return@launch
            }

            index = getClickPointBoundingBox(touchPoint)

            Log.v("focus test", "index : $index")

            if (index == null) {
                checkFinish.fill(true) // 배열의 모든 요소를 true로 설정
            } else {

                for (i in 0 until bitmapList.size)
                    checkFinish[i] = true
            }
            while (!checkFinish.all { it }) { }

            box.resume(index)
        }
    }

    /**
     * 선택한 사진 속 클릭된 좌표에 있는 객체 위치 정보(boundingBox)에 초점이 맞춰진 이미지 index를 반환한다.
     *
     * @param point 터치된 좌표 정보
     * @return 해당 객체에 초점이 맞춰진 이미지 index 반환
     */
    private suspend fun getClickPointBoundingBox(point: Point): Int? =
        suspendCoroutine { bitmapResult ->

            var checkResume = false

            for(i in 0 until boundingBoxResizeList.size) {
                val obj = makeRect(boundingBoxResizeList[i])

                if(imageToolModule.checkPointInRect(point, obj)) {
                    checkResume = true
                    bitmapResult.resume(i)
                    break
                }
                if(!checkResume && i == boundingBoxResizeList.size) bitmapResult.resume(null)
            }
        }

    /**
     * 선택된 이미지의 App3 메타데이터를 통해 감지된 객체를 표시한 후 화면에 출력한다.
     */
    private fun setMainImageBoundingBox() {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                for (i in 0 until boundingBoxList.size) {
                    val arraylist = arrayListOf<Int>()

                    val scale = selectBitmap.width.toFloat() / boundingBoxList[i][0].toFloat()

                    val left = (boundingBoxList[i][1].toFloat() * scale).toInt()
                    val top = (boundingBoxList[i][2].toFloat() * scale).toInt()
                    val right = (boundingBoxList[i][3].toFloat() * scale).toInt()
                    val bottom = (boundingBoxList[i][4].toFloat() * scale).toInt()

                    arraylist.add(left)
                    arraylist.add(top)
                    arraylist.add(right)
                    arraylist.add(bottom)

                    boundingBoxResizeList.add(arraylist)
                }

                focusCheckingBitmap =
                    imageToolModule.drawFocusResult(selectBitmap, boundingBoxResizeList,
                        requireContext().resources.getColor(R.color.focus), requireContext().resources.getColor(R.color.focus_30))

                // imageView 변환
                withContext(Dispatchers.Main) {
                    binding.focusMainView.setImageBitmap(focusCheckingBitmap)
                }
            } catch (e: IllegalStateException) {
                println(e.message)
            }
        }
    }

    /**
     * [Rect]를 제작하여 반환한다.
     *
     * @param arraylist [Rect] 제작할 요소들
     * @return 제작된 [Rect] 반환
     */
    private fun makeRect(arraylist : ArrayList<Int>) : Rect {
        val rect = Rect()
        rect.left = arraylist[0]
        rect.top = arraylist[1]
        rect.right = arraylist[2]
        rect.bottom = arraylist[3]

        return rect
    }

    /**
     * 블러처리를 조절할 수 있는 SeekBar를 설정한다.
     */
    private fun setSeekBar(){
        imageToolModule.showView(binding.seekBar, true)

        binding.seekBar.max = maxBlurRadius.toInt()  // 0 ~ 24 ( 1 ~ 25 )
        binding.seekBar.progress = curBlurRadius.toInt() + 1 // 현재 10f면 11f로 설정

        imageToolModule.showView(binding.apertureInfoTextView, false)

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // SeekBar의 진행 상태에 따라 블러 강도 조절
                if(fromUser) {
                    curBlurRadius = seekBar!!.progress.toFloat() + 1 + seekBar!!.progress.toFloat()/maxBlurRadius

                    if (curBlurRadius < 1.4f)
                        curBlurRadius = 1.4f
                    if(curBlurRadius > 25.0f)
                        curBlurRadius = 25.0f

                    imageToolModule.showView(binding.apertureInfoTextView, true)
                    binding.apertureInfoTextView.text = String.format("f  %.1f", curBlurRadius)
                }
            }

            // 슬라이더를 터치하여 조작을 시작할 때
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            // 슬라이더 조작을 멈추고 손을 뗄 때
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                imageToolModule.showView(binding.apertureInfoTextView, false)

                // 블러를 다시 적용하여 이미지 업데이트
                applyBlur()

            }
        })
    }

    /**
     * 블러를 적용하여 이미지를 업데이트한다.
     */
    private fun applyBlur() {
        val cropBitmap = bitmapCropRect(selectBitmap, selectObjRect!!)
        val blurSelectBitmap = BlurBitmapUtil.blur(requireContext(), selectBitmap, curBlurRadius)
        resultBitmap = mergeBitmaps(blurSelectBitmap, cropBitmap, selectObjRect!!.left, selectObjRect!!.top)

        binding.focusMainView.setImageBitmap(resultBitmap)
    }

    /**
     * 도움말을 알맞게 띄운다.
     */
    private fun infoTextView() {
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

    /**
     * 로딩바를 설정한다.
     *
     * @param boolean 로딩바 보여줄지 여부
     * @param loadingText 로딩과 함께 보여질 텍스트
     */
    private fun showProgressBar(boolean: Boolean, loadingText: LoadingText?){
        setEnable(boolean)

        CoroutineScope(Dispatchers.Main).launch {
            binding.loadingText.text = when (loadingText) {
                LoadingText.Save -> {
                    "편집을 저장 중.."
                }
                else -> {
                    ""
                }
            }
        }
        imageToolModule.showView(binding.progressBar, boolean)
    }

    /**
     * 버튼들의 터치 가능 여부를 조정한다.
     *
     * @param boolean 터치 가능 여부
     */
    private fun setEnable(boolean: Boolean) {
        CoroutineScope(Dispatchers.Main).launch {
            binding.focusCloseBtn.isEnabled = boolean
            binding.focusSaveBtn.isEnabled = boolean

            binding.focusInfoBtn.isEnabled = boolean
            binding.dialogCloseBtn.isEnabled = boolean
            binding.imageCompareBtn.isEnabled = boolean
        }
    }
}