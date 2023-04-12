package com.example.onepic.EditModule.Fragment

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.Rect
import android.opengl.Visibility
import android.os.Bundle
import android.os.Handler
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.onepic.EditModule.RewindModule
import com.example.onepic.ImageToolModule
import com.example.onepic.JpegViewModel
import com.example.onepic.PictureModule.Contents.ContentAttribute
import com.example.onepic.PictureModule.Contents.Picture
import com.example.onepic.PictureModule.ImageContent
import com.example.onepic.R
import com.example.onepic.databinding.FragmentPlayBinding
import com.example.onepic.databinding.FragmentRewindBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PlayFragment : Fragment() {

    private lateinit var binding: FragmentPlayBinding

    var boundingBox: ArrayList<ArrayList<Int>> = arrayListOf()

    var checkMagicPicturePlay = false
    val handler = Handler()

    var magicPlaySpeed: Long = 100

    private val jpegViewModel by activityViewModels<JpegViewModel>()
    private lateinit var imageContent : ImageContent

    private lateinit var imageToolModule: ImageToolModule

    private var changeFaceStartX = 0
    private var changeFaceStartY = 0

    private var pictureList: ArrayList<Picture> = arrayListOf()
    private val bitmapList: ArrayList<Bitmap> = arrayListOf()

    private lateinit var mainPicture: Picture
    private lateinit var mainBitmap: Bitmap

    private var overlayImg: ArrayList<Bitmap> = arrayListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 뷰 바인딩 설정
        binding = FragmentPlayBinding.inflate(inflater, container, false)
        imageContent = jpegViewModel.jpegMCContainer.value?.imageContent!!
        imageToolModule = ImageToolModule()

        // main Picture의 byteArray를 bitmap 제작
        mainPicture = imageContent.mainPicture
        mainBitmap = imageToolModule.byteArrayToBitmap(imageContent.getJpegBytes(mainPicture))

        binding.magicMainView.setImageBitmap(mainBitmap)

        // magicPlayBtn 클릭했을 때: magic pricture 실행 (움직이게 하기)
        binding.magicPlayBtn.setOnClickListener {
            if(!checkMagicPicturePlay) {
                binding.magicPlayBtn.setImageResource(R.drawable.magic_picture_pause_icon)
                CoroutineScope(Dispatchers.Default).launch {
                    if (overlayImg.size <= 0) {
                        withContext(Dispatchers.Main) {
                            binding.magicIng.visibility = View.VISIBLE
                        }
                        overlayImg = magicPictureProcessing()
                        withContext(Dispatchers.Main) {
                            binding.magicIng.visibility = View.INVISIBLE
                        }
                    }
                    magicPictureRun(overlayImg)
                    checkMagicPicturePlay = true
                }
            }
            else {
                binding.magicPlayBtn.setImageResource(R.drawable.magic_picture_play_icon)
                handler.removeCallbacksAndMessages(null)
                checkMagicPicturePlay = false
            }
        }

        // save btn 클릭 시
        binding.magicSaveBtn.setOnClickListener {
            findNavController().navigate(R.id.action_playFragment_to_editFragment)
        }

        // close btn 클릭 시
        binding.magicCloseBtn.setOnClickListener {
            findNavController().navigate(R.id.action_playFragment_to_editFragment)
        }

        return binding.root
    }

    private suspend fun magicPictureProcessing(): ArrayList<Bitmap>  =
        suspendCoroutine { result ->
            val overlayImg: ArrayList<Bitmap> = arrayListOf()
            // rewind 가능한 연속 사진 속성의 picture list 얻음
            pictureList = imageContent.pictureList
            if (bitmapList.size == 0) {
                setBitmapPicture()
            }

            var basicIndex = 0
            var checkEmbedded = false
            for (i in 0 until pictureList.size) {
                if (pictureList[basicIndex].embeddedData?.size!! > 0) {
                    checkEmbedded = true
                    break
                }
                basicIndex++
            }

            if (checkEmbedded) {
                changeFaceStartX = (pictureList[basicIndex].embeddedData?.get(4) ?: Int) as Int
                changeFaceStartY = (pictureList[basicIndex].embeddedData?.get(5) ?: Int) as Int

                for (i in basicIndex until bitmapList.size) {
                    pictureList[i].embeddedData?.let { boundingBox.add(it) }
                    createOverlayImg(overlayImg, boundingBox[i - basicIndex], i)
                }
            }
            result.resume(overlayImg)
        }

    private fun createOverlayImg(ovelapBitmap: ArrayList<Bitmap> , rect: ArrayList<Int>, index: Int) {

        // 감지된 모든 boundingBox 출력
        println("=======================================================")

        // bitmap를 자르기
        val cropImage = imageToolModule.cropBitmap(
            bitmapList[index],
            Rect(rect[0], rect[1], rect[2], rect[3])
        )

        val newImage = imageToolModule.circleCropBitmap(cropImage)
        ovelapBitmap.add(
            imageToolModule.overlayBitmap(
                mainBitmap,
                newImage,
                changeFaceStartX,
                changeFaceStartY
            )
        )
    }

    private fun magicPictureRun(ovelapBitmap: ArrayList<Bitmap>) {

        CoroutineScope(Dispatchers.Main).launch {
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


    /**
     * setBitmapPicture()
     *      - Picture의 ArrayList를 모두 Bitmap으로 전환해서 저장
     */
    private fun setBitmapPicture() {
        val checkFinish = BooleanArray(pictureList.size)
        for(i in 0 until pictureList.size){
            checkFinish[i] = false
        }
        for(i in 0 until pictureList.size) {
            bitmapList.add(imageToolModule.byteArrayToBitmap((imageContent.getJpegBytes(pictureList[i]))))
            checkFinish[i] = true
        }
    }

}