package com.example.onepic.Edit.Fragment

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.onepic.Edit.RewindModule
import com.example.onepic.ExPictureContainer
import com.example.onepic.ImageToolModule
import com.example.onepic.Picture
import com.example.onepic.R
import com.example.onepic.databinding.FragmentRewindBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class rewindFragment : Fragment(R.layout.fragment_rewind) {
    private lateinit var binding: FragmentRewindBinding

    private lateinit var exPictureContainer: ExPictureContainer
    private lateinit var imageToolModule: ImageToolModule
    private lateinit var rewindModule: RewindModule

    private lateinit var mainPicture: Picture
    private lateinit var mainBitmap: Bitmap

    private var changeFaceStartX = 0
    private var changeFaceStartY = 0

    private var pictureList: ArrayList<Picture> = arrayListOf()
    private val bitmapList: ArrayList<Bitmap> = arrayListOf()

    private val cropBitmapList: ArrayList<Bitmap> = arrayListOf()


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        bundle: Bundle?
    ): View {
        // 뷰 바인딩 설정
        binding = FragmentRewindBinding.inflate(inflater, container, false)

        /** ExPictureContainer 설정 **/
        if(arguments != null)
        exPictureContainer =
            requireArguments().getSerializable("exPictureContainer") as ExPictureContainer // Bundle에서 객체를 받아옴
        else
            exPictureContainer = ExPictureContainer(inflater.context)

        imageToolModule = ImageToolModule()
        rewindModule = RewindModule()

        // main Picture의 byteArray를 bitmap 제작
        mainPicture = exPictureContainer.getMainPicture()
        mainBitmap = imageToolModule.byteArrayToBitmap(mainPicture.byteArray)

        // rewind 가능한 연속 사진 속성의 picture list 얻음
        pictureList = exPictureContainer.getPictureList(1, "BurstShots")

        // save btn 클릭 시
        binding.rewindSaveBtn.setOnClickListener {
            mainPicture.byteArray = imageToolModule.bitmapToByteArray(mainBitmap)
            exPictureContainer.setMainPicture(0, mainPicture)

            val bundle = Bundle()
            bundle.putSerializable("exPictureContainer", exPictureContainer) // 객체를 Bundle에 저장
            findNavController().navigate(R.id.action_rewindFragment_to_editFragment, bundle)
        }

        // close btn 클릭 시
        binding.rewindCloseBtn.setOnClickListener {
            val bundle = Bundle()
            bundle.putSerializable("exPictureContainer", exPictureContainer) // 객체를 Bundle에 저장
            findNavController().navigate(R.id.action_rewindFragment_to_editFragment, bundle)
        }

        // 이미지 뷰 클릭 시
        binding.rewindMainView.setOnTouchListener { view, event ->
            if (event!!.action == MotionEvent.ACTION_DOWN) {
                // click 좌표를 bitmap에 해당하는 좌표로 변환
                val touchPoint = ImageToolModule().getBitmapClickPoint(
                    PointF(event.x, event.y),
                    view as ImageView
                )
                println("------- click point:" + touchPoint)

                CoroutineScope(Dispatchers.Default).launch {
                    try {
                        // Click 좌표가 포함된 Bounding Box 얻음
                        val boundingBox = getBoundingBox(touchPoint)

                        // Bounding Box로 이미지를 Crop한 후 보여줌
                        withContext(Dispatchers.Main) {
                            cropImgAndView(boundingBox)
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
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
    fun setMainImageBoundingBox() {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val faceResultBitmap = rewindModule.runFaceDetection(mainBitmap)

                // imageView 변환
                withContext(Dispatchers.Main) {
                    binding.rewindMainView.setImageBitmap(faceResultBitmap)
                }
            } catch (e: Exception) { // 예외 처리를 수행
                e.printStackTrace()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // faceDetection하고 결과가 표시된 사진을 받아 imaveView에 띄우기
        setMainImageBoundingBox()
    }

    /**
     * setBitmapPicture()
     *      - Picture의 ArrayList를 모두 Bitmap으로 전환해서 저장
     */
    private fun setBitmapPicture() {
        for(i in 0 until pictureList.size) {
            bitmapList.add(imageToolModule.byteArrayToBitmap(pictureList[i].byteArray))
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

        CoroutineScope(Dispatchers.Default).launch {
            try {
                if (bitmapList.size == 0) {
                    setBitmapPicture()
                }

                for (i in 0 until pictureList.size) {

                    // clickPoint와 사진을 비교하여 클릭된 좌표에 감지된 얼굴이 있는지 확인 후 해당 얼굴 boundingBox 받기
                    val rect =
                        rewindModule.getClickPointBoundingBox(bitmapList[i], touchPoint)

                    // 포인트에 해당되는 얼굴이 없을 때
                    if(rect == null) {
                        // 메인 사진의 boundingBox에 인지된 얼굴이 없을 때
                        if(i==0) {
                            // faceDetection하고 결과가 표시된 사진을 받아 imaveView에 띄우기
                            setMainImageBoundingBox()
                            break
                        }
                        continue
                    }

                    // 메인 사진일 경우 나중에 다른 사진을 겹칠 위치 지정
                    if(i == 0){
                        changeFaceStartX = rect[4]
                        changeFaceStartY = rect[5]
                    }

                    val arrayBounding = listOf(
                        i,
                        rect[0], rect[1], rect[2], rect[3],
                        rect[4], rect[5], rect[6], rect[7]
                    )
                    boundingBox.add(arrayBounding)
                }

                box.resume(boundingBox)

            } catch (e: Exception) {
                e.printStackTrace()
            }
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

        if (bitmapList.size == 0) {
            setBitmapPicture()
        }

        for (i in 0 until boundingBox.size) {
            println(i.toString() + " || " + boundingBox[i])

            // bounding rect 알아내기
            val rect = boundingBox[i]

            // bitmap를 자르기
            val cropImage = imageToolModule.cropBitmap(
                bitmapList[rect[0]].copy(Bitmap.Config.ARGB_8888, true),
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
                var newImage = imageToolModule.cropBitmap(
                    bitmapList[rect[0]].copy(Bitmap.Config.ARGB_8888, true),
                    Rect(rect[5], rect[6], rect[7], rect[8])
                )
                newImage = imageToolModule.circleCropBitmap(newImage)
                mainBitmap = imageToolModule.overlayBitmap(mainBitmap, newImage, changeFaceStartX, changeFaceStartY)
                binding.rewindMainView.setImageBitmap(mainBitmap)
            }

            // main activity에 만들어둔 scrollbar 속 layout의 아이디를 통해 해당 layout에 넣기
            binding.candidateLayout.addView(candidateLayout)
        }
    }

}