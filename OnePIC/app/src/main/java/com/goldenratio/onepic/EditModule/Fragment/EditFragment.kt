package com.goldenratio.onepic.EditModule.Fragment

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.goldenratio.onepic.EditModule.RewindModule
import com.goldenratio.onepic.EditModule.ShakeLevelModule
import com.goldenratio.onepic.ImageToolModule
import com.goldenratio.onepic.JpegViewModel
import com.goldenratio.onepic.PictureModule.AudioContent
import com.goldenratio.onepic.PictureModule.Contents.ContentAttribute
import com.goldenratio.onepic.PictureModule.Contents.Picture
import com.goldenratio.onepic.PictureModule.ImageContent
import com.goldenratio.onepic.PictureModule.TextContent
import com.goldenratio.onepic.R
import com.goldenratio.onepic.ViewerModule.Fragment.ViewerFragment
import com.goldenratio.onepic.ViewerModule.ViewerEditorActivity
import com.goldenratio.onepic.databinding.FragmentEditBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class EditFragment : Fragment(R.layout.fragment_edit) {

    private lateinit var binding: FragmentEditBinding
    private lateinit var activity: ViewerEditorActivity
    private val jpegViewModel by activityViewModels<JpegViewModel>()

    private lateinit var imageToolModule: ImageToolModule

    private lateinit var imageContent: ImageContent
    private lateinit var textContent: TextContent
    private lateinit var audioContent: AudioContent

    private var imageTool = ImageToolModule()

    private var isFinished = MutableLiveData<Boolean>()

    private lateinit var mainSubView: View
    private var mainTextView: TextView? = null

    private var pictureList = arrayListOf<Picture>()
    private var pictureByteList = mutableListOf<ByteArray>()
    private lateinit var mainPicture: Picture

    private var isSaving = false
    private var isAudioPlay = false
    private var isTextView = false

    private var bestImageIndex : Int? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = context as ViewerEditorActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        bundle: Bundle?
    ): View {
        // 상태바 색상 변경
        val window: Window = activity.window
            ?: throw IllegalStateException("Fragment is not attached to an activity")
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.setStatusBarColor(ContextCompat.getColor(requireContext(), android.R.color.black))

        // isFinished가 ture가 되면 viewerFragment로 전환
        isFinished.observe(requireActivity()) { value ->
            if (value == true) {
                CoroutineScope(Dispatchers.Main).launch {
                    findNavController().navigate(R.id.action_editFragment_to_viewerFragment)
                }
                isFinished.value = false // 초기화
            }
        }

        // 뷰 바인딩 설정
        binding = FragmentEditBinding.inflate(inflater, container, false)

        // imageToolModule 설정
        imageToolModule = ImageToolModule()

        // picture 리스트 만들어질때까지
        while (!imageContent.checkPictureList) { }

        // Content 설정
        imageContent = jpegViewModel.jpegMCContainer.value?.imageContent!!
        imageContent.setMainBitmap(null)
        textContent = jpegViewModel.jpegMCContainer.value!!.textContent
        audioContent = jpegViewModel.jpegMCContainer.value!!.audioContent

        // 만약 편집을 했다면 저장 버튼이 나타나게 설정
        if (imageContent.checkMainChangeAttribute || imageContent.checkRewindAttribute ||
            imageContent.checkMagicAttribute || imageContent.checkAddAttribute
        ) {
            imageToolModule.showView(binding.saveBtn, true)
        }

        // picture 설정
        mainPicture = imageContent.mainPicture
        pictureList = imageContent.pictureList
        pictureByteList = jpegViewModel.getPictureByteArrList()

        // 메인 이미지 설정
        CoroutineScope(Dispatchers.Main).launch {
            Glide.with(binding.mainImageView)
                .load(pictureByteList[jpegViewModel.selectPictureIndex])
                .into(binding.mainImageView)
        }

        // distance focus일 경우 seekbar 보이게
        if (imageContent.checkAttribute(ContentAttribute.distance_focus)) {
            binding.seekBar.visibility = View.VISIBLE
        }

        // jpeg container의 텍스트 설정
        var imageCntText = "담긴 사진 ${imageContent.pictureList.size}장 "
        if (textContent.textList.size > 0) {
            imageCntText += "+ 텍스트"
        }
        if (audioContent.audio != null) {
            imageCntText += "+ 오디오"
        }
        binding.imageCntTextView.text = imageCntText

        // 컨테이너 이미지 설정 (오디오 텍스트 이미지도 포함)
        setSubImage()

        /* TODO: ViewrFragment to EditorFragment - currentImageFilePath와 selectedImageFilePath 확인 */
        // ViewerFragment에서 스크롤뷰 이미지 중 아무것도 선택하지 않은 상태에서 edit 누르면 picturelist의 맨 앞 객체(메인)이 선택된 것으로 했음
        Log.d("currentImageUri: ", "" + jpegViewModel.currentImageUri)
        Log.d("selectedImageFilePath: ", "" + jpegViewModel.selectedSubImage)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Rewind 버튼 클릭 이벤트 리스너 등록
        binding.rewindBtn.setOnClickListener {
            // RewindFragment로 이동
            findNavController().navigate(R.id.action_editFragment_to_rewindFragment)
        }
        // 움직이는 Magic 버튼 클릭 이벤트 리스너 등록
        binding.magicBtn.setOnClickListener {
            // MagicPictureFragment로 이동
            findNavController().navigate(R.id.action_editFragment_to_magicPictureFragment)
        }

        // 얼굴 추천 버튼 클릭 이벤트 리스너 등록
        binding.bestMainBtn.setOnClickListener {
            viewBestImage()
        }

//        // audio ADD
//        binding.audioAddBtn.setOnClickListener {
//            // 일반 사진이면 안 넘어가도록
//            if (checkAdd) {
//                // MagicPictureFragment로 이동
//                findNavController().navigate(R.id.action_editFragment_to_audioAddFragment)
//            }
//        }
//
//        // text ADD
//        binding.textAddBtn.setOnClickListener {
//            findNavController().navigate(R.id.action_editFragment_to_addFragment)
//        }


        // 메인 변경
        binding.mainChangeBtn.setOnClickListener {
            // 기존 메인에 관한 설정 제거
            mainTextView?.visibility = View.INVISIBLE

            // 선택한 picture Index 알아내기
            val selectPictureIndex = jpegViewModel.selectPictureIndex
            jpegViewModel.mainPictureIndex = selectPictureIndex

            // 해당 뷰를 index를 통해 알아내 mainMark 표시 띄우기
            val newMainSubView = binding.linear.getChildAt(selectPictureIndex)
                .findViewById<TextView>(R.id.mainMark)
            newMainSubView.visibility = View.VISIBLE

            // 메인에 관한 설정
            mainTextView = newMainSubView
            mainPicture = pictureList[selectPictureIndex]

            // 1. 메인으로 하고자하는 picture를 기존의 pictureList에서 제거
            val result = imageContent.removePicture(mainPicture)
            if (result) {
                // 2. main 사진을 첫번 째로 삽입
                imageContent.insertPicture(0, mainPicture)
                imageContent.mainPicture = mainPicture

                // 메인 변경 유무 flag true로 변경
                imageContent.checkMainChangeAttribute = true

                // 저장 버튼 표시 | 메인 변경 버튼 없애기
                imageToolModule.showView(binding.saveBtn, true)
                imageToolModule.showView(binding.mainChangeBtn, false)
            }
        }

        // 취소 버튼 (viewer로 이동)
        binding.backBtn.setOnClickListener {
            // 변경된 편집이 있을 경우 확인 창 띄우기
            if (imageContent.checkMainChangeAttribute || imageContent.checkRewindAttribute ||
                imageContent.checkMagicAttribute || imageContent.checkAddAttribute
            ) {
                val oDialog: AlertDialog.Builder = AlertDialog.Builder(
                    activity,
                    android.R.style.Theme_DeviceDefault_Light_Dialog
                )

                oDialog.setMessage("편집한 내용을 저장하지 않고 나가시겠습니까?")
                    .setPositiveButton(
                        "취소"
                    ) { _, _ -> }
                    .setNeutralButton("확인") { _, _ ->
                        imageContent.resetBitmap()
                        setButtonDeactivation()

                        val pre = jpegViewModel.preEditMainPicture
                        if (pre != null) {
                            imageContent.mainPicture = pre
                            jpegViewModel.preEditMainPicture = null
                        }
                        findNavController().navigate(R.id.action_editFragment_to_viewerFragment)
                    }.show()
            } else {
                findNavController().navigate(R.id.action_editFragment_to_viewerFragment)
            }
        }

        // 저장 버튼 (viewer로 이동)
        binding.saveBtn.setOnClickListener {
            // 저장 중인지 확인하는 flag가 false일 경우만 저장 단계 실행 --> 두번 실행될 경우 오류를 예외처리하기 위해
            if (!isSaving) {
                imageTool.showView(binding.progressBar, true)

                isSaving = true

                ViewerFragment.isEditStoraged = true

                // 덮어쓰기
                val currentFilePath = jpegViewModel.currentImageUri

                var fileName = ""
                // 파일 이름 얻어내기

                // 13버전 이상일 경우는 uri를 받아 옴 --> 전처리 거쳐서 파일 이름 얻어내기
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    fileName = jpegViewModel.getFileNameFromUri(currentFilePath!!.toUri())
                }
                // 13버전 이하일 경우는 file path를 받아 옴
                else {
                    fileName = currentFilePath!!.substring(currentFilePath.lastIndexOf("/") + 1);
                }

                val result = jpegViewModel.jpegMCContainer.value?.overwiteSave(fileName)
                // 우리 앱의 사진이 아닐 때
                if (result == "another") {
                    singleSave()
                    CoroutineScope(Dispatchers.Default).launch {
                        Thread.sleep(1000)
                        setButtonDeactivation()
                        setCurrentPictureByteArrList()
                    }
                } else {
                    CoroutineScope(Dispatchers.Default).launch {
                        setButtonDeactivation()
                        Thread.sleep(2000)
                        setCurrentPictureByteArrList()
                    }
                }
                imageContent.setCheckAttribute()
            }
        }
    }


    fun setButtonDeactivation() {
        imageContent.checkAddAttribute = false
        imageContent.checkRewindAttribute = false
        imageContent.checkMagicAttribute = false
        imageContent.checkMainChangeAttribute = false
        jpegViewModel.mainPictureIndex = 0
    }

    fun setCurrentPictureByteArrList() {
        var pictureList = jpegViewModel.jpegMCContainer.value?.getPictureList()

        if (pictureList != null) {

            CoroutineScope(Dispatchers.IO).launch {
                val pictureByteArrayList = mutableListOf<ByteArray>()
                for (picture in pictureList) {
                    val pictureByteArr =
                        jpegViewModel.jpegMCContainer.value?.imageContent?.getJpegBytes(picture)
                    pictureByteArrayList.add(pictureByteArr!!)
                } // end of for..
                jpegViewModel.setpictureByteArrList(pictureByteArrayList)
                CoroutineScope(Dispatchers.Main).launch {
                    isFinished.value = true
                }
            }
        }
    }

    fun remainOriginalPictureSave() {
        val oDialog: AlertDialog.Builder = AlertDialog.Builder(
            activity,
            android.R.style.Theme_DeviceDefault_Light_Dialog
        )
        imageTool.showView(binding.progressBar, false)
        oDialog.setMessage("편집된 이미지만 저장하시겠습니까? 원본 이미지는 사라지지 않습니다.")
            .setPositiveButton(
                "모두 저장",
                DialogInterface.OnClickListener { dialog, which ->
                    imageTool.showView(binding.progressBar, true)
                    if (!imageContent.checkMagicAttribute || !imageContent.checkRewindAttribute) {
                        val mainPicture = imageContent.mainPicture
                        // 바뀐 비트맵을 Main(맨 앞)으로 하는 새로운 Jpeg 저장
                        imageContent.insertPicture(0, mainPicture)
                    }
                    jpegViewModel.jpegMCContainer.value?.save()
                    CoroutineScope(Dispatchers.Default).launch {
                        setButtonDeactivation()
                        Thread.sleep(2000)
                        withContext(Dispatchers.Main) {
//                        imageTool.showView(binding.progressBar , false)
                            findNavController().navigate(R.id.action_editFragment_to_viewerFragment)
                        }
                    }
                })
            .setNeutralButton("예",
                DialogInterface.OnClickListener { dialog, which ->
                    try {
                        singleSave()
                    } catch (e: IOException) {
                        Toast.makeText(activity, "저장에 실패 했습니다.", Toast.LENGTH_SHORT)
                            .show()
                    }
                })
            .show()
    }

    fun singleSave() {
        try {
            imageTool.showView(binding.progressBar, true)
            val newImageContent =
                jpegViewModel.jpegMCContainer.value?.imageContent!!
            val singlePictureList: ArrayList<Picture> =
                ArrayList<Picture>(1)
            singlePictureList.add(newImageContent.mainPicture)
            newImageContent.setContent(singlePictureList)

            var savedFilePath = jpegViewModel.jpegMCContainer.value?.save()
            //ViewerFragment.currentFilePath = savedFilePath.toString()

        } catch (e: IOException) {
            Toast.makeText(activity, "저장에 실패 했습니다.", Toast.LENGTH_SHORT)
                .show()
        }

    }

    /**
     * setSubImage()
     *      : 아래 서브 이미지를 설정
     */
    fun setSubImage() {

        var audioSize = 0
        if(audioContent.audio != null) {
            audioSize = 1
        }

        for (i in 0 until pictureList.size + textContent.textList.size + audioSize) {
            // 넣고자 하는 layout 불러오기
            val subLayout =
                layoutInflater.inflate(R.layout.scroll_item_layout_edit, null)

            // 위 불러온 layout에서 변경을 할 view가져오기
            val imageView: ImageView =
                subLayout.findViewById(R.id.scrollImageView)

            if (i < pictureList.size) {
                // 이미지뷰에 붙이기
                CoroutineScope(Dispatchers.Main).launch {
                    Log.d("error 잡기", "$i 번째 이미지 띄우기")
                    Glide.with(imageView)
                        .load(pictureByteList[i])
                        .into(imageView)
                }
            } else if (i == pictureList.size && textContent.textList.size > 0) {
                imageView.setImageResource(R.drawable.container_text_icon)
            } else {
                imageView.setImageResource(R.drawable.container_audio_icon)
            }

            Log.d("editFragment", "selectPictureIndex: " + jpegViewModel.selectPictureIndex)
            if (i == jpegViewModel.selectPictureIndex) {
                CoroutineScope(Dispatchers.Main).launch {
                    imageView.setBackgroundResource(R.drawable.chosen_image_border)
                    imageView.setPadding(6, 6, 6, 6)
                    mainSubView = imageView

                    if (i != jpegViewModel.mainPictureIndex) {
                        imageToolModule.showView(binding.mainChangeBtn, true)
                    }

                    Glide.with(binding.mainImageView)
                        .load(pictureByteList[i])
                        .into(binding.mainImageView)


                    setMoveScrollView(subLayout, i)
                }
            }

            if (i == jpegViewModel.mainPictureIndex) {
//                imageToolModule.showView(subLayout.findViewById(R.id.checkMainIcon), true)

                CoroutineScope(Dispatchers.Main).launch {
                    mainTextView = subLayout.findViewById<TextView>(R.id.mainMark)
                    if (mainTextView != null)
                        imageToolModule.showView(mainTextView!!, true)
                }
            }

            subLayout.setOnClickListener {
                // 이미지인 경우
                if (i < pictureList.size) {

                    mainSubView.background = null
                    mainSubView.setPadding(0, 0, 0, 0)

                    CoroutineScope(Dispatchers.Main).launch {
                        // 메인 이미지 설정
                        Glide.with(binding.mainImageView)
                            .load(pictureByteList[i])
                            .into(binding.mainImageView)
                    }
                    if (jpegViewModel.mainPictureIndex != i) {
                        imageToolModule.showView(binding.mainChangeBtn, true)
                    } else {
                        imageToolModule.showView(binding.mainChangeBtn, false)
                    }
                    jpegViewModel.selectPictureIndex = i

                    imageView.setBackgroundResource(R.drawable.chosen_image_border)
                    imageView.setPadding(6, 6, 6, 6)

                    mainSubView = imageView
                }
                // text인 경우
                else if (i == pictureList.size && textContent.textList.size > 0) {
                    if(!isTextView) {
                        imageView.setBackgroundResource(R.drawable.chosen_image_border)
                        imageView.setPadding(6, 6, 6, 6)
                        isTextView = true
                    }
                    else {
                        mainSubView.background = null
                        mainSubView.setPadding(0, 0, 0, 0)
                        isTextView = false
                    }
                }
                // audio인 경우
                else {
                    if(!isAudioPlay) {
                        imageView.setBackgroundResource(R.drawable.chosen_image_border)
                        imageView.setPadding(6, 6, 6, 6)
                        isAudioPlay = true
                    }
                    else {
                        imageView.background = null
                        imageView.setPadding(0, 0, 0, 0)
                        isAudioPlay = false
                    }
                }
            }

            CoroutineScope(Dispatchers.Main).launch {
                // main activity에 만들어둔 scrollbar 속 layout의 아이디를 통해 해당 layout에 넣기
                binding.linear.addView(subLayout)
            }
        }
    }

    fun viewBestImage() {
        imageToolModule.showView(binding.progressBar, true)

        val bitmapList = imageContent.getBitmapList()
        if (bitmapList != null) {
            val rewindModule = RewindModule()
            CoroutineScope(Dispatchers.IO).launch {

                if (bestImageIndex == null) {

                    rewindModule.allFaceDetection(bitmapList)
                    val faceDetectionResult = rewindModule.choiceBestImage(bitmapList)
                    Log.d("anaylsis", "end faceDetection")

                    val shakeDetectionResult =
                        ShakeLevelModule().shakeLevelDetection(bitmapList)

                    val analysisResults = arrayListOf<Double>()

                    var preBestImageIndex = 0

                    for (i in 0 until bitmapList.size) {

                        Log.d("anaylsis", "[$i] =  faceDetectio ${faceDetectionResult[i]} ")
                        Log.d("anaylsis", "[$i] =  shake ${shakeDetectionResult[i]}")

                        analysisResults.add(faceDetectionResult[i] + shakeDetectionResult[i])
                        if (analysisResults[preBestImageIndex] < analysisResults[i] ||
                            analysisResults[preBestImageIndex] == analysisResults[i] && faceDetectionResult[preBestImageIndex] < faceDetectionResult[i]
                        ) {
                            preBestImageIndex = i
                        }
                    }

                    Log.d("anaylsis", "=== ${analysisResults[preBestImageIndex]}")
                    println("bestImageIndex = $preBestImageIndex")

                    bestImageIndex = preBestImageIndex
                }
                withContext(Dispatchers.Main) {
                    mainSubView.background = null
                    mainSubView.setPadding(0, 0, 0, 0)

                    Glide.with(binding.mainImageView)
                        .load(pictureByteList[bestImageIndex!!])
                        .into(binding.mainImageView)

                    mainSubView = binding.linear.getChildAt(bestImageIndex!!)
                        .findViewById<ImageView>(R.id.scrollImageView)
                    mainSubView.setBackgroundResource(R.drawable.chosen_image_border)
                    mainSubView.setPadding(6, 6, 6, 6)

                    jpegViewModel.selectPictureIndex = bestImageIndex!!
                    if(bestImageIndex != jpegViewModel.mainPictureIndex) {
                        imageToolModule.showView(binding.mainChangeBtn, true)
                    }
                    else {
                        imageToolModule.showView(binding.mainChangeBtn, false)
                    }

                    imageToolModule.showView(binding.progressBar, false)
                    setMoveScrollView(mainSubView ,bestImageIndex!!)

                    Log.d("mainChange", "bestImage null")
                }
            }
        }
    }

    /**
     * setMoveScrollView(subLayout: View, index: Int)
     *      : 해당 뷰를 스크롤의 가운데로 가게 해주는 함수
     */
    fun setMoveScrollView(subLayout: View, index: Int) {
        val viewTreeObserver = subLayout.viewTreeObserver

        viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                // 동적으로 추가된 뷰가 다 그려진 후에 실행되어야 할 코드 작성
                val x = subLayout.width * (index-2)

                CoroutineScope(Dispatchers.Main).launch {
                    binding.scrollView.scrollTo(x, 0)
                }
                subLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
    }
}
