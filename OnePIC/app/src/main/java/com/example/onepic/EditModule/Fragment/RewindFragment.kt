package com.example.onepic.EditModule.Fragment

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.core.view.isEmpty
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.onepic.*
import com.example.onepic.EditModule.ArrowMoveClickListener
import com.example.onepic.EditModule.RewindModule
import com.example.onepic.PictureModule.Contents.ActivityType
import com.example.onepic.PictureModule.Contents.ContentAttribute
import com.example.onepic.PictureModule.Contents.Picture
import com.example.onepic.PictureModule.ImageContent
import com.example.onepic.databinding.FragmentRewindBinding
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

open class RewindFragment : Fragment(R.layout.fragment_rewind) {

    private lateinit var binding: FragmentRewindBinding

    protected lateinit var imageToolModule: ImageToolModule
    protected lateinit var rewindModule: RewindModule

    protected lateinit var mainPicture: Picture
    private lateinit var originalMainBitmap: Bitmap
    protected lateinit var mainBitmap: Bitmap

    private var preMainBitmap: Bitmap? = null
    protected var newImage: Bitmap? = null

    protected var changeFaceStartX = 0
    protected var changeFaceStartY = 0

    protected var bitmapList: ArrayList<Bitmap> = arrayListOf()

    protected val cropBitmapList: ArrayList<Bitmap> = arrayListOf()

    protected val jpegViewModel by activityViewModels<JpegViewModel>()
    protected lateinit var imageContent : ImageContent
    lateinit var fragment :Fragment

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        bundle: Bundle?
    ): View {
        // 뷰 바인딩 설정
        binding = FragmentRewindBinding.inflate(inflater, container, false)

        imageContent = jpegViewModel.jpegMCContainer.value?.imageContent!!

        imageToolModule = ImageToolModule()
        rewindModule = RewindModule()

        imageToolModule.showView(binding.progressBar, true)

        // main Picture의 byteArray를 bitmap 제작
        mainPicture = imageContent.mainPicture

        // 메인 이미지 임시 설정
        CoroutineScope(Dispatchers.Default).launch {
            withContext(Dispatchers.Main) {
                Glide.with(binding.rewindMainView)
                    .load(imageContent.getJpegBytes(imageContent.mainPicture))
                    .into(binding.rewindMainView)
            }
        }
        CoroutineScope(Dispatchers.Default).launch {
            mainBitmap = imageContent.getMainBitmap()
            originalMainBitmap = mainBitmap
            if(imageContent.activityType == ActivityType.Viewer) {
                // faceDetection 하고 결과가 표시된 사진을 받아 imaveView에 띄우기
                setMainImageBoundingBox()
            }
        }
        CoroutineScope(Dispatchers.Default).launch {
            // rewind 가능한 연속 사진 속성의 picture list 얻음
            var startTime = System.currentTimeMillis()
            val newBitmapList = imageContent.getBitmapList(ContentAttribute.edited)
            if (newBitmapList != null) {
                bitmapList = newBitmapList
                //rewindModule.allFaceDetection(bitmapList)


                var endTime = System.currentTimeMillis()
                var elapsedTime = endTime - startTime
                Log.d("ElapsedTime", "get Bitmap Time: $elapsedTime ms")
                //CoroutineScope(Dispatchers.Default).launch {
                startTime = System.currentTimeMillis()

                rewindModule.allFaceDetection(bitmapList)

                endTime = System.currentTimeMillis()
                elapsedTime = endTime - startTime
                Log.d("ElapsedTime", "get face result Time: $elapsedTime ms")
                //}

                if (imageContent.activityType == ActivityType.Camera) {
                    startTime = System.currentTimeMillis()

                    mainBitmap = rewindModule.autoBestFaceChange(bitmapList)

                    endTime = System.currentTimeMillis()
                    elapsedTime = endTime - startTime
                    Log.d("ElapsedTime", "autorewind Time: $elapsedTime ms")

                    setMainImageBoundingBox()

                    imageToolModule.showView(binding.progressBar, false)

                }
            }
        }
        // save btn 클릭 시
        binding.rewindSaveBtn.setOnClickListener {

            CoroutineScope(Dispatchers.Default).launch {
                imageToolModule.showView(binding.progressBar , true)

                if(preMainBitmap != null) {
                    mainBitmap = preMainBitmap!!
                    newImage = null
                }

                Log.d("ImageTool", "11111")
                val allBytes =  imageToolModule.bitmapToByteArray(mainBitmap, imageContent.getJpegBytes(mainPicture))
                Log.d("ImageTool", "222222")

                imageContent.mainPicture = Picture(ContentAttribute.edited,imageContent.extractSOI(allBytes))
                imageContent.mainPicture.waitForByteArrayInitialized()

                if(imageContent.activityType == ActivityType.Camera) {
                    val mainPicture = imageContent.mainPicture
                    // 바뀐 비트맵을 Main(맨 앞)으로 하는 새로운 Jpeg 저장
                    //imageContent.insertPicture(0, mainPicture)
                    jpegViewModel.jpegMCContainer.value?.save()
                    //Thread.sleep(10000)

                }
                withContext(Dispatchers.Main){
                    //jpegViewModel.jpegMCContainer.value?.save()
                    findNavController().navigate(R.id.action_fregemnt_to_editFragment)
                }
                imageToolModule.showView(binding.progressBar , false)
            }
        }

        // close btn 클릭 시
        binding.rewindCloseBtn.setOnClickListener {
            findNavController().navigate(R.id.action_fregemnt_to_editFragment)
        }

        binding.autoRewindBtn.setOnClickListener {
            imageToolModule.showView(binding.progressBar, true)
            imageToolModule.showView(binding.arrowBar, false)
            binding.candidateLayout.removeAllViews()

            if (bitmapList.size != 0) {
                CoroutineScope(Dispatchers.Default).launch {
                    rewindModule.allFaceDetection(bitmapList)
                    mainBitmap = rewindModule.autoBestFaceChange(bitmapList)

                    withContext(Dispatchers.Main) {
                        binding.rewindMainView.setImageBitmap(mainBitmap)
                    }
                    newImage = null
                    imageToolModule.showView(binding.progressBar, false)
                }
            }
        }

        // 이미지 뷰 클릭 시
        binding.rewindMainView.setOnTouchListener { view, event ->
            if (event!!.action == MotionEvent.ACTION_UP) {
                imageToolModule.showView(binding.progressBar, true)

                if (preMainBitmap != null) {
                    mainBitmap = preMainBitmap!!
                    newImage = null
                }
                // click 좌표를 bitmap에 해당하는 좌표로 변환
                val touchPoint = ImageToolModule().getBitmapClickPoint(PointF(event.x, event.y), binding.rewindMainView)
                println("------- click point:$touchPoint")

                if (touchPoint != null) {

                    CoroutineScope(Dispatchers.Default).launch {
                        // Click 좌표가 포함된 Bounding Box 얻음
                        val boundingBox = getBoundingBox(touchPoint)

                        if (boundingBox.size > 0) {
                            // Bounding Box로 이미지를 Crop한 후 보여줌
                            withContext(Dispatchers.Main) {
                                cropImgAndView(boundingBox)
                            }
                        }
                    }
                }
                else {
                    imageToolModule.showView(binding.progressBar, false)

                }
            }
            return@setOnTouchListener true
        }

        // reset 버튼 클릭시
        binding.imageResetBtn.setOnClickListener {
            binding.rewindMainView.setImageBitmap(originalMainBitmap)
            mainBitmap = originalMainBitmap
            newImage = null
            // faceDetection 하고 결과가 표시된 사진을 받아 imageView에 띄우기
            setMainImageBoundingBox()
        }

        // compare 버튼 클릭시
        binding.imageCompareBtn.setOnTouchListener { view, event ->

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    binding.rewindMainView.setImageBitmap(originalMainBitmap)
                    return@setOnTouchListener true
                }
                MotionEvent.ACTION_UP -> {
                    if(newImage != null)
                        binding.rewindMainView.setImageBitmap(preMainBitmap)
                    else
                        binding.rewindMainView.setImageBitmap(mainBitmap)
                    return@setOnTouchListener true
                }
                else -> return@setOnTouchListener false
            }
        }

        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.post {
            val arrowListener = ArrowMoveClickListener(::moveCropFace, binding.maxArrowBtn, binding.circleArrowBtn)
            binding.circleArrowBtn.setOnTouchListener(arrowListener)
        }
    }



    /**
     * setMainImageBoundingBox()
     *      - mainImage를 faceDetection 실행 후,
     *        감지된 얼굴의 사각형 표시된 사진으로 imageView 변환
     */
    open fun setMainImageBoundingBox() {

        //showView(binding.faceListView, false)
        imageToolModule.showView(binding.arrowBar, false)
        if(!binding.candidateLayout.isEmpty()) {
            CoroutineScope(Dispatchers.Main).launch {
                withContext(Dispatchers.Main) {
                    binding.candidateLayout.removeAllViews()
                }
            }
        }

        CoroutineScope(Dispatchers.Default).launch {

            val faceResult = rewindModule.runFaceDetection(0)

            Log.d("checkPictureList", "!!!!!!!!!!!!!!!!!!! setMainImageBoundingBox")

            val resultBitmap = imageToolModule.drawDetectionResult(requireContext(), mainBitmap, faceResult)

            // imageView 변환
            withContext(Dispatchers.Main) {
                binding.rewindMainView.setImageBitmap(resultBitmap)
            }
            imageToolModule.showView(binding.progressBar , false)
        }
    }

    /**
     * getBoundingBox(touchPoint: Point): ArrayList<List<Int>>
     *     - click된 포인트를 알려주면,
     *       해당 포인트가 객체 감지 결과 bounding Box 속에 존재하는지 찾아서
     *       만약 포인트를 포함하는 boundingBox를 찾으면 모아 return
     */
    suspend fun getBoundingBox(touchPoint: Point): ArrayList<ArrayList<Int>> = suspendCoroutine { box ->
        val boundingBox: ArrayList<ArrayList<Int>> = arrayListOf()

        val checkFinish = BooleanArray(bitmapList.size)
        for (i in 0 until bitmapList.size) {
            checkFinish[i] = false
        }

        CoroutineScope(Dispatchers.Default).launch {

            if (bitmapList.size == 0) {
                imageToolModule.showView(binding.progressBar, false)
                return@launch
            }

            var startTime = System.currentTimeMillis()
            val basicRect =
                rewindModule.getClickPointBoundingBox(bitmapList[0], 0, touchPoint)

            var endTime = System.currentTimeMillis()
            var elapsedTime = endTime - startTime
            Log.d("ElapsedTime", "[1] basicRect Time: $elapsedTime ms")

            if (basicRect == null) {
                startTime = System.currentTimeMillis()

                // 메인 사진의 boundingBox에 인지된 얼굴이 없을 때
                // faceDetection하고 결과가 표시된 사진을 받아 imaveView에 띄우기
                setMainImageBoundingBox()

                checkFinish.fill(true) // 배열의 모든 요소를 true로 설정

                endTime = System.currentTimeMillis()
                elapsedTime = endTime - startTime
                Log.d("ElapsedTime", "[2] noFace Time: $elapsedTime ms")

            } else {

                // 메인 사진일 경우 나중에 다른 사진을 겹칠 위치 지정
                changeFaceStartX = basicRect[4]
                changeFaceStartY = basicRect[5]

                val arrayBounding = arrayListOf(
                    0,
                    basicRect[0], basicRect[1], basicRect[2], basicRect[3],
                    basicRect[4], basicRect[5], basicRect[6], basicRect[7]
                )
                boundingBox.add(arrayBounding)
                checkFinish[0] = true
                startTime = System.currentTimeMillis()
                for (i in 1 until bitmapList.size) {
                    CoroutineScope(Dispatchers.Default).launch {

                        // clickPoint와 사진을 비교하여 클릭된 좌표에 감지된 얼굴이 있는지 확인 후 해당 얼굴 boundingBox 받기
                        val rect =
                            rewindModule.getClickPointBoundingBox(bitmapList[i], i, touchPoint)

                        if (rect != null) {
                            val arrayBounding = arrayListOf(
                                i,
                                rect[0], rect[1], rect[2], rect[3],
                                rect[4], rect[5], rect[6], rect[7]
                            )
                            boundingBox.add(arrayBounding)
                            checkFinish[i] = true
                        }
                    }
                }
            }
            while (!checkFinish.all { it }) { }
            endTime = System.currentTimeMillis()
            elapsedTime = endTime - startTime
            Log.d("ElapsedTime", "[2] for Time: $elapsedTime ms")

            box.resume(boundingBox)
        }
    }


    /**
     *  cropImgAndView(boundingBox: ArrayList<List<Int>>)
     *         - 이미지를 자르고 화면에 띄어줌
     */
    private fun cropImgAndView(boundingBox: ArrayList<ArrayList<Int>>) {
        // 감지된 모든 boundingBox 출력
        println("=======================================================")
        binding.candidateLayout.removeAllViews()
        cropBitmapList.removeAll(cropBitmapList)

        if (bitmapList.size == 0) {
            imageToolModule.showView(binding.progressBar , false)
            return
        }

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
            // 크롭이미지 배열에 값 추가
            cropBitmapList.add(cropImage)

            // 넣고자 하는 layout 불러오기
            val candidateLayout = layoutInflater.inflate(R.layout.candidate_image_array, null)

            // 위 불러온 layout에서 변경을 할 view가져오기
            val cropImageView: ImageView =
                candidateLayout.findViewById(R.id.cropImageView)

            // 자른 사진 이미지뷰에 붙이기
            cropImageView.setImageBitmap(cropImage)

            // crop 된 후보 이미지 클릭시 해당 이미지로 얼굴 변환 (rewind)
            cropImageView.setOnClickListener{
                newImage = imageToolModule.cropBitmap(
                    bitmapList[rect[0]],
                    //bitmapList[rect[0]].copy(Bitmap.Config.ARGB_8888, true),
                    Rect(rect[5], rect[6], rect[7], rect[8])
                )
                newImage = imageToolModule.circleCropBitmap(newImage!!)
                preMainBitmap = imageToolModule.overlayBitmap(mainBitmap, newImage!!, changeFaceStartX, changeFaceStartY)

                binding.rewindMainView.setImageBitmap(preMainBitmap)
                imageToolModule.showView(binding.arrowBar, true)
            }

            // main activity에 만들어둔 scrollbar 속 layout의 아이디를 통해 해당 layout에 넣기
            binding.candidateLayout.addView(candidateLayout)
        }
        imageToolModule.showView(binding.progressBar , false)
        imageToolModule.showView(binding.arrowBar, false)
    }

    private fun moveCropFace(moveX:Int, moveY:Int) {
        if (newImage != null) {

            changeFaceStartX += moveX
            changeFaceStartY += moveY

            println("!!!!!!!!!! change point (${changeFaceStartX}, ${changeFaceStartY})")
            if(changeFaceStartX < 0)
                changeFaceStartX = 0
            else if(changeFaceStartX > mainBitmap.width- newImage!!.width)
                changeFaceStartX = mainBitmap.width - newImage!!.width
            if(changeFaceStartY < 0)
                changeFaceStartY = 0
            else if(changeFaceStartY > mainBitmap.height - newImage!!.height)
                changeFaceStartY = mainBitmap.height - newImage!!.height

            println("==== change point (${changeFaceStartX}, ${changeFaceStartY})")
            preMainBitmap = imageToolModule.overlayBitmap(
                mainBitmap,
                newImage!!,
                changeFaceStartX,
                changeFaceStartY
            )

            binding.rewindMainView.setImageBitmap(preMainBitmap)
        }
    }
}
