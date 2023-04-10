package com.example.onepic.EditModule.Fragment

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.onepic.*
import com.example.onepic.EditModule.RewindModule
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
    protected lateinit var mainBitmap: Bitmap
    protected lateinit var preMainBitmap: Bitmap

    var activity : MainActivity = MainActivity()

    protected var changeFaceStartX = 0
    protected var changeFaceStartY = 0

    protected var pictureList: ArrayList<Picture> = arrayListOf()
    protected val bitmapList: ArrayList<Bitmap> = arrayListOf()

    protected val cropBitmapList: ArrayList<Bitmap> = arrayListOf()

    protected val jpegViewModel by activityViewModels<JpegViewModel>()
    protected lateinit var imageContent : ImageContent
    lateinit var fragment :Fragment

    var newImage: Bitmap? = null

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        bundle: Bundle?
    ): View {
        // 뷰 바인딩 설정
        binding = FragmentRewindBinding.inflate(inflater, container, false)

        imageContent = jpegViewModel.jpegMCContainer.value?.imageContent!!
        fragment = this

        imageToolModule = ImageToolModule()
        rewindModule = RewindModule()

        // main Picture의 byteArray를 bitmap 제작
        mainPicture = imageContent.mainPicture
        //mainBitmap = ImageToolModule().byteArrayToBitmap(imageContent.getJpegBytes(mainPicture))
        CoroutineScope(Dispatchers.Main).launch {
            mainBitmap = withContext(Dispatchers.IO) {
                Glide.with(fragment)
                    .asBitmap()
                    .load(imageContent.getJpegBytes(mainPicture))
                    .submit()
                    .get()
            }

            withContext(Dispatchers.Main) {
                // faceDetection하고 결과가 표시된 사진을 받아 imaveView에 띄우기
                setMainImageBoundingBox()
            }
        }
        // rewind 가능한 연속 사진 속성의 picture list 얻음
        pictureList = imageContent.pictureList

        // save btn 클릭 시
        binding.rewindSaveBtn.setOnClickListener {

            CoroutineScope(Dispatchers.Default).launch {
                val allBytes = imageToolModule.bitmapToByteArray(mainBitmap, imageContent.getJpegBytes(mainPicture))

                imageContent.mainPicture = Picture(ContentAttribute.edited,imageContent.extractSOI(allBytes) )
                imageContent.mainPicture.waitForByteArrayInitialized()

                withContext(Dispatchers.Main){
                    findNavController().navigate(R.id.action_rewindFragment_to_editFragment)
                }
            }
        }

        // close btn 클릭 시
        binding.rewindCloseBtn.setOnClickListener {
            findNavController().navigate(R.id.action_rewindFragment_to_editFragment)
        }

        binding.autoRewindBtn.setOnClickListener {
            if (bitmapList.size == 0) {
                setBitmapPicture()
            }
            CoroutineScope(Dispatchers.Default).launch {
                mainBitmap = rewindModule.autoBestFaceChange( bitmapList)

                withContext(Dispatchers.Main) {
                    binding.rewindMainView.setImageBitmap(mainBitmap)
                }
            }
        }

        // 이미지 뷰 클릭 시
        binding.rewindMainView.setOnTouchListener { view, event ->
            if (event!!.action == MotionEvent.ACTION_DOWN) {
                // click 좌표를 bitmap에 해당하는 좌표로 변환
                val touchPoint = ImageToolModule().getBitmapClickPoint(
                    PointF(event.x, event.y),
                    view as ImageView
                )
                println("------- click point:$touchPoint")

                CoroutineScope(Dispatchers.Default).launch {
                    // Click 좌표가 포함된 Bounding Box 얻음
                    val boundingBox = getBoundingBox(touchPoint)

                    // Bounding Box로 이미지를 Crop한 후 보여줌
                    withContext(Dispatchers.Main) {
                        cropImgAndView(boundingBox)
                    }


                }
            }
            return@setOnTouchListener true
        }

        binding.topArrowBtn.setOnTouchListener { view, motionEvent ->
            if (motionEvent!!.action == MotionEvent.ACTION_DOWN) {
                moveCropFace(0, -2)
                return@setOnTouchListener true
            }
            else {
                return@setOnTouchListener false
            }
        }
        binding.bottomArrowBtn.setOnTouchListener { view, motionEvent ->
            if (motionEvent!!.action == MotionEvent.ACTION_DOWN) {
                moveCropFace(0, 2)
            }
            return@setOnTouchListener true
        }
        binding.leftArrowBtn.setOnTouchListener { view, motionEvent ->
            if (motionEvent!!.action == MotionEvent.ACTION_DOWN) {
                moveCropFace(-2, 0)
            }
            return@setOnTouchListener true
        }
        binding.rightArrowBtn.setOnTouchListener { view, motionEvent ->
            if (motionEvent!!.action == MotionEvent.ACTION_DOWN) {
                moveCropFace(2, 0)
            }
            return@setOnTouchListener true
        }

        return binding.root
    }

    /**
     * setMainImageBoundingBox()
     *      - mainImage를 faceDetection 실행 후,
     *        감지된 얼굴의 사각형 표시된 사진으로 imageView 변환
     */
    open fun setMainImageBoundingBox() {
        CoroutineScope(Dispatchers.Default).launch {
            val faceResultBitmap = rewindModule.runFaceDetection(mainBitmap)

            // imageView 변환
            withContext(Dispatchers.Main) {
                binding.rewindMainView.setImageBitmap(faceResultBitmap)

            }
        }
    }

    /**
     * setBitmapPicture()
     *      - Picture의 ArrayList를 모두 Bitmap으로 전환해서 저장
     */
    protected fun setBitmapPicture() {
        val checkFinish = BooleanArray(pictureList.size)
        for(i in 0 until pictureList.size){
            checkFinish[i] = false
        }
        for(i in 0 until pictureList.size) {
            CoroutineScope(Dispatchers.Default).launch {
                //bitmapList.add(imageToolModule.byteArrayToBitmap(pictureList[i].byteArray))
                ///bitmapList.add(imageToolModule.byteArrayToBitmap((imageContent.getJpegBytes(pictureList[i]))))
                bitmapList.add(withContext(Dispatchers.IO) {
                    Glide.with(fragment)
                        .asBitmap()
                        .load(imageContent.getJpegBytes(pictureList[i]))
                        .submit()
                        .get()
                })

                checkFinish[i] = true
            }
        }
        while(!checkFinish.all { it }) {

        }
    }

    /**
     * getBoundingBox(touchPoint: Point): ArrayList<List<Int>>
     *     - click된 포인트를 알려주면,
     *       해당 포인트가 객체 감지 결과 bounding Box 속에 존재하는지 찾아서
     *       만약 포인트를 포함하는 boundingBox를 찾으면 모아 return
     */
    suspend fun getBoundingBox(touchPoint: Point): ArrayList<List<Int>> = suspendCoroutine { box ->
        val boundingBox: ArrayList<List<Int>> = arrayListOf()

        val checkFinish = BooleanArray(pictureList.size)
        for (i in 0 until pictureList.size) {
            checkFinish[i] = false
        }

        CoroutineScope(Dispatchers.Default).launch {
            if (bitmapList.size == 0) {
                setBitmapPicture()
            }

            val basicRect =
                rewindModule.getClickPointBoundingBox(bitmapList[0], touchPoint)
            if (basicRect == null) {
                // 메인 사진의 boundingBox에 인지된 얼굴이 없을 때
                // faceDetection하고 결과가 표시된 사진을 받아 imaveView에 띄우기
                setMainImageBoundingBox()
                checkFinish.fill(true) // 배열의 모든 요소를 true로 설정
            } else {
                // 메인 사진일 경우 나중에 다른 사진을 겹칠 위치 지정
                changeFaceStartX = basicRect[4]
                changeFaceStartY = basicRect[5]

                val arrayBounding = listOf(
                    0,
                    basicRect[0], basicRect[1], basicRect[2], basicRect[3],
                    basicRect[4], basicRect[5], basicRect[6], basicRect[7]
                )
                boundingBox.add(arrayBounding)
                checkFinish[0] = true
                for (i in 1 until pictureList.size) {

                    CoroutineScope(Dispatchers.Default).launch {
                        // clickPoint와 사진을 비교하여 클릭된 좌표에 감지된 얼굴이 있는지 확인 후 해당 얼굴 boundingBox 받기
                        val rect =
                            rewindModule.getClickPointBoundingBox(bitmapList[i], touchPoint)

                        if (rect != null) {
                            val arrayBounding = listOf(
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
            box.resume(boundingBox)
        }
    }


    /**
     *  cropImgAndView(boundingBox: ArrayList<List<Int>>)
     *         - 이미지를 자르고 화면에 띄어줌
     */
    private fun cropImgAndView(boundingBox: ArrayList<List<Int>>) {
        // 감지된 모든 boundingBox 출력
        println("=======================================================")
        binding.candidateLayout.removeAllViews()
        cropBitmapList.removeAll(cropBitmapList)

        if (bitmapList.size == 0) {
            setBitmapPicture()
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
                preMainBitmap = mainBitmap.copy(Bitmap.Config.ARGB_8888, true)
                mainBitmap = imageToolModule.overlayBitmap(mainBitmap, newImage!!, changeFaceStartX, changeFaceStartY)

                binding.rewindMainView.setImageBitmap(mainBitmap)
            }

            // main activity에 만들어둔 scrollbar 속 layout의 아이디를 통해 해당 layout에 넣기
            binding.candidateLayout.addView(candidateLayout)
        }
    }

    private fun moveCropFace(moveX:Int, moveY:Int) {
        if (newImage != null) {

            changeFaceStartX += moveX
            changeFaceStartY += moveY

            mainBitmap = imageToolModule.overlayBitmap(
                preMainBitmap,
                newImage!!,
                changeFaceStartX,
                changeFaceStartY
            )

            binding.rewindMainView.setImageBitmap(mainBitmap)
        }
    }
}