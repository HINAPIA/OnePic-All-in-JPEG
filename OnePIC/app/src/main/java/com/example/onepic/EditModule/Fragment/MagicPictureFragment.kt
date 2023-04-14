package com.example.onepic.EditModule.Fragment

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.navigation.fragment.findNavController
import com.example.onepic.EditModule.ArrowMoveClickListener
import com.example.onepic.EditModule.RewindModule
import com.example.onepic.ImageToolModule
import com.example.onepic.PictureModule.Contents.ContentAttribute
import com.example.onepic.PictureModule.Contents.Picture
import com.example.onepic.R
import com.example.onepic.databinding.FragmentMagicPictureBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MagicPictureFragment : RewindFragment() {
    private lateinit var binding: FragmentMagicPictureBinding

    var boundingBox: ArrayList<ArrayList<Int>> = arrayListOf()

    var checkMagicPicturePlay = false
    val handler = Handler()

    var magicPlaySpeed: Long = 100

    val ovelapBitmap: ArrayList<Bitmap> = arrayListOf()


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 뷰 바인딩 설정
        binding = FragmentMagicPictureBinding.inflate(inflater, container, false)

        imageContent = jpegViewModel.jpegMCContainer.value?.imageContent!!

        imageToolModule = ImageToolModule()
        rewindModule = RewindModule()

        // main Picture의 byteArray를 bitmap 제작
        mainPicture = imageContent.mainPicture

        mainBitmap = ImageToolModule().byteArrayToBitmap(imageContent.getJpegBytes(mainPicture))

        // faceDetection하고 결과가 표시된 사진을 받아 imaveView에 띄우기
        setMainImageBoundingBox()

        // magic 가능한 연속 사진 속성의 picture list 얻음
        pictureList = jpegViewModel.jpegMCContainer.value!!.getPictureList(ContentAttribute.focus)

        // save btn 클릭 시
        binding.magicSaveBtn.setOnClickListener {
            CoroutineScope(Dispatchers.Default).launch {
                val allBytes = imageToolModule.bitmapToByteArray(mainBitmap, imageContent.getJpegBytes(mainPicture))

                imageContent.mainPicture = Picture(ContentAttribute.edited, imageContent.extractSOI(allBytes) )
                imageContent.mainPicture.waitForByteArrayInitialized()

                // EmbeddedData 추가
                val indices = intArrayOf(5,6,7,8) // 추출할 배열의 인덱스

                if(boundingBox.size > 0) {
                    val mainBoundingBox: ArrayList<Int> =
                        boundingBox[0].filterIndexed { index, _ -> index in indices } as ArrayList<Int>

                    mainBoundingBox.add(changeFaceStartX)
                    mainBoundingBox.add(changeFaceStartY)

                    pictureList[boundingBox[0][0]].insertEmbeddedData(mainBoundingBox)

                    for (i in 1 until boundingBox.size) {
                        pictureList[boundingBox[i][0]].insertEmbeddedData(
                            boundingBox[i].filterIndexed { index, _ -> index in indices } as ArrayList<Int>)
                    }
                }

                withContext(Dispatchers.Main){
                    findNavController().navigate(R.id.action_magicPictureFragment_to_editFragment)
                }
            }
        }

        // close btn 클릭 시
        binding.magicCloseBtn.setOnClickListener {
            findNavController().navigate(R.id.action_magicPictureFragment_to_editFragment)
        }

        // magicPlayBtn 클릭했을 때: magic pricture 실행 (움직이게 하기)
        binding.magicPlayBtn.setOnClickListener {
            if(!checkMagicPicturePlay) {
                magicPictureRun(cropBitmapList)
                binding.magicPlayBtn.setImageResource(R.drawable.magic_picture_pause_icon)
                checkMagicPicturePlay = true
            }
            else {
                handler.removeCallbacksAndMessages(null)
                binding.magicPlayBtn.setImageResource(R.drawable.magic_picture_play_icon)
                checkMagicPicturePlay = false
            }
        }

        // 이미지 뷰 클릭 시
        binding.magicMainView.setOnTouchListener { view, event ->
            if (event!!.action == MotionEvent.ACTION_DOWN) {
                // click 좌표를 bitmap에 해당하는 좌표로 변환
                val touchPoint = ImageToolModule().getBitmapClickPoint(
                    PointF(event.x, event.y),
                    view as ImageView
                )
                println("------- click point:" + touchPoint)

                CoroutineScope(Dispatchers.Default).launch {
                    // Click 좌표가 포함된 Bounding Box 얻음
                    boundingBox = getBoundingBox(touchPoint)

                    // Bounding Box로 이미지를 Crop한 후 보여줌
                    withContext(Dispatchers.Main) {
                        cropImgAndView(boundingBox)
                    }
                }
            }
            return@setOnTouchListener true
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
        CoroutineScope(Dispatchers.Default).launch {
            val faceResultBitmap = rewindModule.runFaceDetection(mainBitmap)

            // imageView 변환
            withContext(Dispatchers.Main) {
                binding.magicMainView.setImageBitmap(faceResultBitmap)
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
        binding.magicCandidateLayout.removeAllViews()
        cropBitmapList.clear()

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
                Rect(rect[1], rect[2], rect[3], rect[4])
            )

            val ovelapImage = imageToolModule.cropBitmap(
                bitmapList[rect[0]],
                Rect(rect[5], rect[6], rect[7], rect[8])
            )
            // 크롭이미지 배열에 값 추가
            cropBitmapList.add(ovelapImage)

            // 넣고자 하는 layout 불러오기
            val candidateLayout = layoutInflater.inflate(R.layout.candidate_image_array, null)

            // 위 불러온 layout에서 변경을 할 view가져오기
            val cropImageView: ImageView =
                candidateLayout.findViewById(R.id.cropImageView)

            // 자른 사진 이미지뷰에 붙이기
            cropImageView.setImageBitmap(cropImage)

            // main activity에 만들어둔 scrollbar 속 layout의 아이디를 통해 해당 layout에 넣기
            binding.magicCandidateLayout.addView(candidateLayout)
        }
    }

    private fun magicPictureRun(cropBitmapList: ArrayList<Bitmap>) {
        ovelapBitmap.clear()
        CoroutineScope(Dispatchers.Main).launch {
            for (i in 0 until cropBitmapList.size) {
                newImage = imageToolModule.circleCropBitmap(cropBitmapList[i])
                ovelapBitmap.add(
                    imageToolModule.overlayBitmap(
                        mainBitmap,
                        newImage!!,
                        changeFaceStartX,
                        changeFaceStartY
                    )
                )
            }
            var currentImageIndex = 0
            var increaseIndex = 1

            val runnable = object : Runnable {
                override fun run() {
                    if (ovelapBitmap.size > 0) {
                        binding.magicMainView.setImageBitmap(ovelapBitmap[currentImageIndex])
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
        if(checkMagicPicturePlay) {
            handler.removeCallbacksAndMessages(null)
            binding.magicPlayBtn.setImageResource(R.drawable.magic_picture_play_icon)
            checkMagicPicturePlay = false
        }

        if (newImage != null) {

            changeFaceStartX += moveX
            changeFaceStartY += moveY

            ovelapBitmap[0] = imageToolModule.overlayBitmap(
                mainBitmap,
                newImage!!,
                changeFaceStartX,
                changeFaceStartY
            )

            binding.magicMainView.setImageBitmap(ovelapBitmap[0])
        }
    }
}