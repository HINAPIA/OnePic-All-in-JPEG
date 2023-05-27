package com.goldenratio.onepic.EditModule.Fragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ClipData
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.get
import androidx.core.view.setPadding
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.goldenratio.onepic.EditModule.MagicPictureModule
import com.goldenratio.onepic.EditModule.RewindModule
import com.goldenratio.onepic.EditModule.ShakeLevelModule
import com.goldenratio.onepic.ImageToolModule
import com.goldenratio.onepic.JpegViewModel
import com.goldenratio.onepic.LoadModule.LoadResolver
import com.goldenratio.onepic.PictureModule.AudioContent
import com.goldenratio.onepic.PictureModule.Contents.ContentAttribute
import com.goldenratio.onepic.PictureModule.Contents.Picture
import com.goldenratio.onepic.PictureModule.ImageContent
import com.goldenratio.onepic.PictureModule.MCContainer
import com.goldenratio.onepic.PictureModule.TextContent
import com.goldenratio.onepic.R
import com.goldenratio.onepic.ViewerModule.Fragment.ViewerFragment
import com.goldenratio.onepic.ViewerModule.ViewerEditorActivity
import com.goldenratio.onepic.databinding.FragmentEditBinding
import kotlinx.coroutines.*
import java.io.IOException
import java.io.InputStream
import java.lang.String
import kotlin.Boolean
import kotlin.ByteArray
import kotlin.Double
import kotlin.Exception
import kotlin.IllegalStateException
import kotlin.Int
import kotlin.Long
import kotlin.Unit
import kotlin.getValue
import kotlin.let


class EditFragment : Fragment(R.layout.fragment_edit) {

    private lateinit var binding: FragmentEditBinding
    private lateinit var activity: ViewerEditorActivity
    private val jpegViewModel by activityViewModels<JpegViewModel>()

    private lateinit var imageToolModule: ImageToolModule
    private lateinit var magicPictureModule: MagicPictureModule
    private lateinit var loadResolver: LoadResolver

    private lateinit var imageContent: ImageContent
    private lateinit var textContent: TextContent
    private lateinit var audioContent: AudioContent

    private var imageTool = ImageToolModule()

    private var isFinished = MutableLiveData<Boolean>()

    private var mainSubView: View? = null
    private var mainTextView: TextView? = null

    private var pictureList = arrayListOf<Picture>()
    //    private var pictureByteList = mutableListOf<ByteArray>()
    private lateinit var mainPicture: Picture

    private var isSaving = false
    private var isAudioPlay = false
    private var isTextView = false
    private var isPictureChanged : MutableLiveData<Boolean> = MutableLiveData(false)

    private var isAudio = false
    private var isText = false
    private var isDeleting = false
    private var isMagicPlay: Boolean = false

    private var bestImageIndex: Int? = null

    private val PICK_IMAGE_REQUEST = 1

    private var overlayBitmap = arrayListOf<Bitmap>()

    val handler = Handler()

    var magicPlaySpeed: Long = 100


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

        imageToolModule.showView(binding.progressBar, true)

        CoroutineScope(Dispatchers.Default).launch {
            loadResolver = LoadResolver()

            // Content 설정
            imageContent = jpegViewModel.jpegMCContainer.value?.imageContent!!
            magicPictureModule = MagicPictureModule(imageContent)
//        imageContent.setMainBitmap(null)
            textContent = jpegViewModel.jpegMCContainer.value!!.textContent
            audioContent = jpegViewModel.jpegMCContainer.value!!.audioContent


            // picture 리스트 만들어질때까지
            while (!imageContent.checkPictureList) { }

            // picture 설정
            mainPicture = imageContent.mainPicture
            pictureList = imageContent.pictureList

            // 만약 편집을 했다면 저장 버튼이 나타나게 설정
            if (imageContent.checkMainChangeAttribute || imageContent.checkRewindAttribute ||
                imageContent.checkMagicAttribute || imageContent.checkAddAttribute
            ) {
                imageToolModule.showView(binding.saveBtn, true)
            }
            setViewDetailMenu()

            // 메인 이미지 설정
            CoroutineScope(Dispatchers.Main).launch {
                val index = jpegViewModel.getSelectedSubImageIndex()
                if (pictureList[index].contentAttribute == ContentAttribute.magic) {
                    imageToolModule.showView(binding.magicPlayBtn, true)
                    imageToolModule.showView(binding.progressBar, true)
                    setMagicPlay()
//            Toast.makeText(requireContext(), "매직 사진입니다", Toast.LENGTH_LONG).show()
                }
                Glide.with(binding.mainImageView)
                    .load(imageContent.getJpegBytes(pictureList[index]))
                    .into(binding.mainImageView)
            }

            // distance focus일 경우 seekbar 보이게
            if (imageContent.checkAttribute(ContentAttribute.distance_focus)) {
                binding.seekBar.visibility = View.VISIBLE
            }

            setContainerTextSetting()

            // 컨테이너 이미지 설정 (오디오 텍스트 이미지도 포함)
            withContext(Dispatchers.Main) {
                setContainer()
            }

            /* TODO: ViewrFragment to EditorFragment - currentImageFilePath와 selectedImageFilePath 확인 */
            // ViewerFragment에서 스크롤뷰 이미지 중 아무것도 선택하지 않은 상태에서 edit 누르면 picturelist의 맨 앞 객체(메인)이 선택된 것으로 했음
            Log.d("currentImageUri: ", "" + jpegViewModel.currentImageUri)
            Log.d("selectedImageFilePath: ", "" + jpegViewModel.selectedSubImage)


            imageToolModule.showView(binding.progressBar, false)
        }

        isPictureChanged.observe(viewLifecycleOwner) {
            if (isPictureChanged.value == true) {
                CoroutineScope(Dispatchers.IO).launch {
                    bestImageIndex = null
                    overlayBitmap.clear()
                    withContext(Dispatchers.Main) {
                        isPictureChanged.value = false
                    }
                }
            }
        }

        return binding.root
    }

    private fun setViewDetailMenu() {
        if(pictureList.size <= 1) {
            CoroutineScope(Dispatchers.Main).launch {
                binding.bestMainBtn.visibility = View.GONE
                binding.rewindBtn.visibility = View.GONE
                binding.magicBtn.visibility = View.GONE
            }
        } else {
            CoroutineScope(Dispatchers.Main).launch {
                binding.bestMainBtn.visibility = View.VISIBLE
                binding.rewindBtn.visibility = View.VISIBLE
                binding.magicBtn.visibility = View.VISIBLE
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
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
//            imageToolModule.showView(binding.progressBar, true)
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
            val selectPictureIndex = pictureList.indexOf(jpegViewModel.selectedSubImage)
            jpegViewModel.mainSubImage = jpegViewModel.selectedSubImage

            Log.d("main Change", "selectPictureIndex: $selectPictureIndex")

            // 해당 뷰를 index를 통해 알아내 mainMark 표시 띄우기
            val newMainSubView = binding.linear.getChildAt(selectPictureIndex)
                .findViewById<TextView>(R.id.mainMark)
            newMainSubView.visibility = View.VISIBLE

            // 메인에 관한 설정
            mainTextView = newMainSubView
            mainPicture = pictureList[selectPictureIndex]

            // 메인 변경 유무 flag true로 변경
            imageContent.checkMainChangeAttribute = true

            // 저장 버튼 표시 | 메인 변경 버튼 없애기
            imageToolModule.showView(binding.saveBtn, true)
            imageToolModule.showView(binding.mainChangeBtn, false)
        }

        // 컨텐츠 삭제
        binding.contentDeleteBtn.setOnClickListener {
            val size = binding.linear.size
            if (!isDeleting) {
                CoroutineScope(Dispatchers.Main).launch {
                    binding.contentDeleteBtn.setImageResource(R.drawable.container_deleting_icon)
                }
                if (pictureList.size > 1) {
                    for (i in 0 until size - 3) {
                        val view = binding.linear.getChildAt(i)
                        imageToolModule.showView(
                            view.findViewById<ImageView>(R.id.deleteIcon),
                            true
                        )
                    }
                }
                for (i in size-3 until size - 1 ) {
                    val view = binding.linear.getChildAt(i)
                    imageToolModule.showView(
                        view.findViewById<ImageView>(R.id.deleteIcon),
                        true
                    )
                }
                if (textContent.textList.size == 0) {
                    val view = binding.linear.getChildAt(binding.linear.size - 3)
                    imageToolModule.showView(view, false)
                    Log.d("delete edit", "text add")
                }
                if (audioContent.audio == null) {
                    val view = binding.linear.getChildAt(binding.linear.size - 2)
                    imageToolModule.showView(view, false)
                    Log.d("delete edit", "audio add")
                }

                val view = binding.linear.getChildAt(binding.linear.size - 1)
                imageToolModule.showView(view, false)

                isDeleting = true
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    binding.contentDeleteBtn.setImageResource(R.drawable.container_delete_icon)
                }
                for (i in 0 until size - 1) {
                    val view = binding.linear.getChildAt(i)
                    imageToolModule.showView(
                        view.findViewById<ImageView>(R.id.deleteIcon),
                        false
                    )
                }
                if (textContent.textList.isEmpty()) {
                    val view = binding.linear.getChildAt(binding.linear.size - 3)
                    imageToolModule.showView(view, true)
                }
                if (audioContent.audio == null) {
                    val view = binding.linear.getChildAt(binding.linear.size - 2)
                    imageToolModule.showView(view, true)
                }

                val view = binding.linear.getChildAt(binding.linear.size - 1)
                imageToolModule.showView(view, true)
                isDeleting = false
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
                        setButtonDeactivation()

                        val uri:Uri
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                            uri = Uri.parse(jpegViewModel.currentImageUri)
                        }
                        else {
                            uri = imageToolModule.getUriFromPath(requireContext(), jpegViewModel.currentImageUri!!)
                        }

                        val iStream: InputStream? = requireContext().contentResolver.openInputStream(uri)
                        val sourceByteArray = imageToolModule.getBytes(iStream!!)

                        val isContainerChanged = MutableLiveData<Boolean>()
                        CoroutineScope(Dispatchers.Main).launch {
                            imageContent.checkPictureList = false
                            val job = async {
                                loadResolver.createMCContainer(jpegViewModel.jpegMCContainer.value!!,sourceByteArray) }
                            job.await()

                            while(!imageContent.checkPictureList) {}
                            CoroutineScope(Dispatchers.Default).launch {
                                imageContent.setBitmapList()
                            }
                            findNavController().navigate(R.id.action_editFragment_to_viewerFragment)
                        }

                    }.show()
            } else {
                findNavController().navigate(R.id.action_editFragment_to_viewerFragment)
            }
        }

        // 저장 버튼 (viewer로 이동)
        binding.saveBtn.setOnClickListener {
            ViewerFragment.isEditStoraged = true
            // 저장 중인지 확인하는 flag가 false일 경우만 저장 단계 실행 --> 두번 실행될 경우 오류를 예외처리하기 위해
            if (!isSaving) {
                imageTool.showView(binding.progressBar, true)

                isSaving = true

                // 1. 메인으로 하고자하는 picture를 기존의 pictureList에서 제거
                val removeResult = imageContent.removePicture(mainPicture)
                if (removeResult) {
                    // 2. main 사진을 첫번 째로 삽입
                    imageContent.insertPicture(0, mainPicture)
                    imageContent.mainPicture = mainPicture
                }


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

                CoroutineScope(Dispatchers.IO).launch {
                    jpegViewModel.currentFileName = fileName
                    // 기존 파일 삭제
                    jpegViewModel.jpegMCContainer.value?.saveResolver?.deleteImage(fileName)
                    var i =0
                    while (JpegViewModel.isUserInentFinish == false){
                        Log.d("save_test", "${i++}")
                        delay(500)
                    }
                    JpegViewModel.isUserInentFinish = false
                    jpegViewModel.jpegMCContainer.value?.overwiteSave(fileName)
                    Thread.sleep(2000)
                    Log.d("save_test", "뷰어로 넘어가기")
                    setButtonDeactivation()
                    setCurrentPictureByteArrList()
                }

                // 우리 앱의 사진이 아닐 때
//                if (result == "another") {
//
//                    // 권한을 요청하고 다시 save
//                    singleSave()
//                    CoroutineScope(Dispatchers.Default).launch {
//                        Thread.sleep(1000)
//                        setButtonDeactivation()
//                        setCurrentPictureByteArrList()
//                    }
//                } else {
//                    CoroutineScope(Dispatchers.Default).launch {
//                        setButtonDeactivation()
//                        Thread.sleep(2000)
//                        setCurrentPictureByteArrList()
//                    }
//                }
                imageContent.setCheckAttribute()
            }
        }
    }


    fun setButtonDeactivation() {
        imageContent.checkAddAttribute = false
        imageContent.checkRewindAttribute = false
        imageContent.checkMagicAttribute = false
        imageContent.checkMainChangeAttribute = false
        jpegViewModel.mainSubImage = null
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
     * setContainer()
     *      : 아래 서브 이미지를 설정
     */
    @SuppressLint("MissingInflatedId")
    fun setContainer() {
        for (i in 0 until pictureList.size) {
            val view = setSubImage(pictureList[i])
            binding.linear.addView(view)
        }

        if (textContent.textList.size > 0) {
            val view = setContainerSubItem(R.drawable.edit_text_icon, clickedFunc = ::ShowingText, deleteFunc = ::DeleteText)
            binding.linear.addView(view)
        } else {
            val view = setContainerSubItem(R.drawable.edit_text_add_icon, clickedFunc = ::AddText, deleteFunc = ::DeleteText)
            binding.linear.addView(view)
        }

        if (audioContent.audio != null) {
            val view = setContainerSubItem(R.drawable.edit_audio_icon, clickedFunc = ::ShowingAudio, deleteFunc = ::DeleteAudio)
            binding.linear.addView(view)
        } else {
            val view = setContainerSubItem(R.drawable.edit_audio_add_icon, clickedFunc = ::AddAudio, deleteFunc = ::DeleteAudio)
            binding.linear.addView(view)
        }

        val view = setContainerSubItem(R.drawable.edit_image_add_icon, clickedFunc = ::AddImage, deleteFunc = fun() {})
        binding.linear.addView(view)
    }

    fun viewBestImage() {
        imageToolModule.showView(binding.magicPlayBtn, true)
        val bitmapList = imageContent.getBitmapList()
        if (bitmapList != null) {
            val rewindModule = RewindModule()
            CoroutineScope(Dispatchers.IO).launch {

                if (bestImageIndex == null ) {
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
                    Glide.with(binding.mainImageView)
                        .load(imageContent.getJpegBytes(pictureList[bestImageIndex!!]))
                        .into(binding.mainImageView)

                    changeViewImage(bestImageIndex!!, binding.linear.getChildAt(bestImageIndex!!)
                        .findViewById<ImageView>(R.id.scrollImageView))
//                    mainSubView =
//                    mainSubView.setBackgroundResource(R.drawable.chosen_image_border)
//                     mainSubView.setPadding(6)
//
////                    jpegViewModel.selectPictureIndex = bestImageIndex!!
//                    jpegViewModel.selectedSubImage = pictureList[bestImageIndex!!]
//
//                    if (bestImageIndex != jpegViewModel.getMainSubImageIndex()) {
//                        imageToolModule.showView(binding.mainChangeBtn, true)
//                    } else {
//                        imageToolModule.showView(binding.mainChangeBtn, false)
//                    }
//
                    imageToolModule.showView(binding.progressBar, false)
//                    setMoveScrollView(mainSubView, bestImageIndex!!)

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
                val x = subLayout.width * (index - 2)

                CoroutineScope(Dispatchers.Main).launch {
                    binding.scrollView.scrollTo(x, 0)
                }
                subLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
    }

    fun changeViewImage(index: Int, imageView: ImageView) {
            mainSubView?.background = null
            mainSubView?.setPadding(0)

        if(pictureList[index].contentAttribute == ContentAttribute.magic) {
            imageToolModule.showView(binding.magicPlayBtn, true)
        }
        else {
            imageToolModule.showView(binding.magicPlayBtn, false)
        }

        CoroutineScope(Dispatchers.Main).launch {
            // 메인 이미지 설정
            Glide.with(binding.mainImageView)
                .load(imageContent.getJpegBytes(pictureList[index]))
                .into(binding.mainImageView)
        }
        if (jpegViewModel.getMainSubImageIndex() != index) {
            imageToolModule.showView(binding.mainChangeBtn, true)
        } else {
            imageToolModule.showView(binding.mainChangeBtn, false)
        }
//        jpegViewModel.selectPictureIndex = index

        imageView.setBackgroundResource(R.drawable.chosen_image_border)
        imageView.setPadding(6)

        mainSubView = imageView
    }

    private fun openGallery() {
//        val intent = Intent(Intent.ACTION_PICK)
//        intent.type = "image/*"
//        startActivityForResult(intent, PICK_IMAGE_REQUEST)

        val intent = Intent(Intent.ACTION_PICK)
        intent.type = MediaStore.Images.Media.CONTENT_TYPE
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val uriList = arrayListOf<Uri>()

        if (data == null) {   // 어떤 이미지도 선택하지 않은 경우
            Toast.makeText(requireContext(), "이미지를 선택하지 않았습니다.", Toast.LENGTH_SHORT).show()
        } else {   // 이미지를 하나라도 선택한 경우
            if (data.clipData == null) {     // 이미지를 하나만 선택한 경우
                Log.e("single choice: ", String.valueOf(data.data))
                val imageUri: Uri? = data.data
                if (imageUri != null) {
                    uriList.add(imageUri)
                }

            } else {      // 이미지를 여러장 선택한 경우
                val clipData: ClipData? = data.clipData
                Log.e("clipData", String.valueOf(clipData?.itemCount))
                if (clipData != null) {
                    if (clipData.itemCount > 10) {   // 선택한 이미지가 11장 이상인 경우
                        Toast.makeText(requireContext(), "사진은 10장까지 선택 가능합니다.", Toast.LENGTH_LONG)
                            .show()
                    } else {   // 선택한 이미지가 1장 이상 10장 이하인 경우
                        Log.e("image불러오기", "multiple choice")

                        for (i in 0 until clipData.itemCount) {
                            val imageUri: Uri =
                                clipData.getItemAt(i).uri // 선택한 이미지들의 uri를 가져온다.
                            try {
                                uriList.add(imageUri) //uri를 list에 담는다.
                            } catch (e: Exception) {
                                Log.e("image불러오기", "File select error", e)
                            }
                        }
                    }
                }
            }
            if(uriList.size > 0) {
                for(i in 0 until uriList.size) {
                    val iStream: InputStream? = requireContext().contentResolver.openInputStream(uriList[i])
                    val sourceByteArray = imageToolModule.getBytes(iStream!!)
                    val isContainerChanged = MutableLiveData<Boolean>()

                    CoroutineScope(Dispatchers.Main).launch {
                        val jpegMCContainer = MCContainer(requireActivity())
                        loadResolver.createMCContainer(jpegMCContainer, sourceByteArray)
                        while (!jpegMCContainer.imageContent.checkPictureList) {
                        }

                        val newPictureList = jpegMCContainer.imageContent.pictureList
//                        pictureList.addAll(newPictureList)

//                        pictureByteList.add(sourceByteArray)
                        for (j in 0 until newPictureList.size) {
//                            newImageByteArrayList.add(
//                                jpegMCContainer.imageContent.getJpegBytes(
//                                    newPictureList[j]
//                                )
//                            )
                            pictureList.add(newPictureList[j])
                            val subLayout = setSubImage(pictureList[pictureList.size - 1])
                            binding.linear.addView(subLayout, pictureList.size - 1)
                            val imageView = subLayout?.findViewById<ImageView>(R.id.scrollImageView)

                            CoroutineScope(Dispatchers.Default).launch {
                                val bitmapList = imageContent.getBitmapList()
                                imageContent.addBitmapList(
                                    imageToolModule.byteArrayToBitmap(
                                        jpegMCContainer.imageContent.getJpegBytes(newPictureList[j])
                                    )
                                )
                            }

                            if (j == newPictureList.size - 1) {
                                CoroutineScope(Dispatchers.Main).launch {
                                    Glide.with(binding.mainImageView)
                                        .load(imageContent.getJpegBytes(pictureList[pictureList.size - 1]))
                                        .into(binding.mainImageView)

                                    mainSubView?.background = null
                                    mainSubView?.setPadding(0)

                                    mainSubView = imageView!!

                                    mainSubView?.setBackgroundResource(R.drawable.chosen_image_border)
                                    mainSubView?.setPadding(6)

                                    if(mainSubView != null)
                                        setMoveScrollView(mainSubView!!, pictureList.size)
                                }
                            }
                            imageContent.pictureList = pictureList
                            imageContent.checkMainChangeAttribute = true
                            imageToolModule.showView(binding.saveBtn, true)
                        }
                        setViewDetailMenu()
                    }


                    //TODO: 유진아 여기 --> uriList: 방금 불러온 이미지들의 uri / newImageByteArrayList: 지금까지 불러온 이미지들의 bytearray
                }
            }
        }
    }

    fun setSubImage(picture: Picture): View? {
        // 넣고자 하는 layout 불러오기
        val subLayout =
            layoutInflater.inflate(R.layout.scroll_item_layout_edit, null)

        // 위 불러온 layout에서 변경을 할 view가져오기
        val imageView: ImageView =
            subLayout.findViewById(R.id.scrollImageView)

        // 이미지뷰에 붙이기
        CoroutineScope(Dispatchers.Main).launch {
            Glide.with(imageView)
                .load(imageContent.getJpegBytes(picture))
                .into(imageView)
        }

        if (picture == jpegViewModel.selectedSubImage) {
            CoroutineScope(Dispatchers.Main).launch {
                imageView.setBackgroundResource(R.drawable.chosen_image_border)
                imageView.setPadding(6)
                mainSubView = imageView

                if (picture != jpegViewModel.mainSubImage) {
                    imageToolModule.showView(binding.mainChangeBtn, true)
                }

                Glide.with(binding.mainImageView)
                    .load(imageContent.getJpegBytes(picture))
                    .into(binding.mainImageView)


                setMoveScrollView(subLayout, pictureList.indexOf(picture))
            }
        }

        if (picture == jpegViewModel.mainSubImage ||
            jpegViewModel.mainSubImage == null && pictureList.indexOf(picture) == 0) {
//                imageToolModule.showView(subLayout.findViewById(R.id.checkMainIcon), true)

            jpegViewModel.mainSubImage = picture

            CoroutineScope(Dispatchers.Main).launch {
                mainTextView = subLayout.findViewById<TextView>(R.id.mainMark)
                if (mainTextView != null)
                    imageToolModule.showView(mainTextView!!, true)
            }
        }

        subLayout.setOnClickListener {

            handler.removeCallbacksAndMessages(null)
            // 이미지인 경우
            jpegViewModel.selectedSubImage = picture
            val index = pictureList.indexOf(picture)
            Log.d("main Change", "onClickListener : $index" )
            changeViewImage(index, imageView)
        }

        // 삭제
        subLayout.findViewById<ImageView>(R.id.deleteIcon).setOnClickListener {
            // 이미지인 경우
//            changeViewImage(index, imageView)

            CoroutineScope(Dispatchers.Main).launch {
                // 메인 이미지 설정
                Glide.with(binding.mainImageView)
                    .load(imageContent.getJpegBytes(picture))
                    .into(binding.mainImageView)
            }

            val oDialog: AlertDialog.Builder = AlertDialog.Builder(
                activity,
                android.R.style.Theme_DeviceDefault_Light_Dialog
            )

            oDialog.setMessage("삭제 하시겠습니까?")
                .setPositiveButton(
                    "아니요"
                ) { _, _ ->
                    CoroutineScope(Dispatchers.Main).launch {
                        // 메인 이미지 설정
                        Glide.with(binding.mainImageView)
                            .load(imageContent.getJpegBytes(jpegViewModel.selectedSubImage!!))
                            .into(binding.mainImageView)
                    }
                }
                .setNeutralButton("네") { _, _ ->
                    CoroutineScope(Dispatchers.Main).launch {
                        isPictureChanged.value = true
                    }
                    binding.linear.removeView(subLayout)
                    imageContent.removePicture(picture)

                    if (pictureList.size == 1) {
                        imageToolModule.showView(binding.linear[0].findViewById<ImageView>(R.id.deleteIcon), false)
                        jpegViewModel.mainSubImage = pictureList[0]

                        setViewDetailMenu()
                    }
                    setContainerTextSetting()

                    CoroutineScope(Dispatchers.Main).launch {
                        // 메인 이미지 설정
                        Glide.with(binding.mainImageView)
                            .load(imageContent.getJpegBytes(jpegViewModel.selectedSubImage!!))
                            .into(binding.mainImageView)


                        imageContent.checkMainChangeAttribute = true
                        imageToolModule.showView(binding.saveBtn, true)
                    }

                    if (jpegViewModel.mainSubImage == picture) {
                        jpegViewModel.mainSubImage = pictureList[0]

                        CoroutineScope(Dispatchers.Main).launch {
                            mainTextView = binding.linear.getChildAt(0)
                                .findViewById<TextView>(R.id.mainMark)
                            if (mainTextView != null)
                                imageToolModule.showView(mainTextView!!, true)
                        }
                        mainPicture = pictureList[jpegViewModel.getSelectedSubImageIndex()]
                    }
                }.show()
        }

//        CoroutineScope(Dispatchers.Main).launch {
//            // main activity에 만들어둔 scrollbar 속 layout의 아이디를 통해 해당 layout에 넣기
//            val index = pictureList.indexOf(picture)
//            if (binding.linear.size - 3 == index) {
//                binding.linear.addView(subLayout, binding.linear.size - 3)
//                Log.d("main Change","sublayout index add : $index")
//            } else {
//                binding.linear.addView(subLayout)
//                Log.d("main Change","sublayout add : $index")
//            }
//
//        }
        return subLayout
    }

    fun setContainerSubItem(drawable_image: Int, clickedFunc: (imageView: ImageView) -> Unit, deleteFunc: () -> Unit): View? {
        // 넣고자 하는 layout 불러오기
        val subLayout =
            layoutInflater.inflate(R.layout.scroll_item_layout_edit, null)

        // 위 불러온 layout에서 변경을 할 view가져오기
        val imageView: ImageView =
            subLayout.findViewById(R.id.scrollImageView)

        imageView.setImageResource(drawable_image)

        subLayout.setOnClickListener {
            clickedFunc(imageView)
        }

        // 삭제
        subLayout.findViewById<ImageView>(R.id.deleteIcon).setOnClickListener {

            val oDialog: AlertDialog.Builder = AlertDialog.Builder(
                activity,
                android.R.style.Theme_DeviceDefault_Light_Dialog
            )

            oDialog.setMessage("삭제 하시겠습니까?")
                .setPositiveButton(
                    "아니요"
                ) { _, _ -> }
                .setNeutralButton("네") { _, _ ->
                    binding.linear.removeView(subLayout)
                    setContainerTextSetting()
                    deleteFunc()
                }.show()
        }

        return subLayout
    }

    //
    fun ShowingText(imageView: ImageView) {
        handler.removeCallbacksAndMessages(null)
//        changeRound(imageView)
        isTextView = if(isTextView) {
            imageView.setImageResource(R.drawable.edit_text_icon)
            false
        } else {
            imageView.setImageResource(R.drawable.edit_text_click_icon)
            true
        }
    }

    fun AddText(imageView: ImageView) {
        handler.removeCallbacksAndMessages(null)
//        changeRound(imageView)
    }

    fun DeleteText() {
        textContent.textList.clear()
        imageContent.checkMainChangeAttribute = true
        imageToolModule.showView(binding.saveBtn, true)
        setContainerTextSetting()
        val view = setContainerSubItem(R.drawable.edit_text_add_icon, clickedFunc = ::AddText, deleteFunc = ::DeleteText)
        binding.linear.addView(view, binding.linear.size - 2)
    }

    fun ShowingAudio(imageView: ImageView) {
        handler.removeCallbacksAndMessages(null)
//        changeRound(imageView)
        isAudioPlay = if(isAudioPlay) {
            imageView.setImageResource(R.drawable.edit_audio_icon)
            false
        } else {
            imageView.setImageResource(R.drawable.edit_audio_click_icon)
            true
        }
    }

    fun AddAudio(imageView: ImageView) {
        handler.removeCallbacksAndMessages(null)
//        changeRound(imageView)
    }

    fun DeleteAudio() {
        audioContent.audio = null
        setContainerTextSetting()
        imageContent.checkMainChangeAttribute = true
        imageToolModule.showView(binding.saveBtn, true)
        val view = setContainerSubItem(R.drawable.edit_audio_add_icon, clickedFunc = ::AddAudio, deleteFunc = ::DeleteAudio)
        binding.linear.addView(view, binding.linear.size - 1)
    }

    fun AddImage(imageView: ImageView) {
        handler.removeCallbacksAndMessages(null)
        openGallery()
    }

    fun changeRound(imageView: ImageView) {
        if (imageView.background == null) {
            imageView.setBackgroundResource(R.drawable.chosen_image_border)
            imageView.setPadding(6)
        } else {
            imageView.background = null
            imageView.setPadding(0)
        }
    }

    fun setContainerTextSetting() {
        // jpeg container의 텍스트 설정
        var imageCntText = "담긴 사진 ${imageContent.pictureList.size}장 "
        if (textContent.textList.size > 0) {
            imageCntText += "+ 텍스트"
        }
        if (audioContent.audio != null) {
            imageCntText += "+ 오디오"
        }
        CoroutineScope(Dispatchers.Main).launch {
            binding.imageCntTextView.text = imageCntText
        }
    }

    fun setMagicPlay() {
        CoroutineScope(Dispatchers.IO).launch {
            isMagicPlay = false
            magicPictureModule = MagicPictureModule(imageContent)

            binding.magicPlayBtn.setOnClickListener {
                if (!isMagicPlay) {
                    CoroutineScope(Dispatchers.Main).launch {
                        binding.magicPlayBtn.setImageResource(R.drawable.edit_magic_ing_icon)
                        imageToolModule.showView(binding.magicPlayBtn, true)

//                        if (overlayBitmap.isEmpty()) {
                            Glide.with(binding.mainImageView)
                                .load(jpegViewModel.selectedSubImage?.let { picture ->
                                    imageContent.getJpegBytes(
                                        picture
                                    )
                                })
                                .into(binding.mainImageView)
//                        }
                    }
                    isMagicPlay = true

                    CoroutineScope(Dispatchers.Default).launch {
                        if (overlayBitmap.isEmpty() ) {
                            overlayBitmap = magicPictureModule.magicPictureProcessing()
                        }

                        Log.d("magic", "magicPictureProcessing end ${overlayBitmap.size}")
                        withContext(Dispatchers.Main) {
                            binding.progressBar.visibility = View.GONE
                        }
                        Log.d("magic", "magicPucture run ${overlayBitmap.size}")
                        magicPictureRun(overlayBitmap)
                    }
                } else {
                    handler.removeCallbacksAndMessages(null)

                    CoroutineScope(Dispatchers.Main).launch {
                        binding.magicPlayBtn.setImageResource(R.drawable.edit_magic_icon)

                        binding.progressBar.visibility = View.GONE
                    }
                    isMagicPlay = false
                }
            }
        }
    }
    private fun magicPictureRun(ovelapBitmap: ArrayList<Bitmap>) {
        CoroutineScope(Dispatchers.Default).launch {

            var currentImageIndex = 0
            var increaseIndex = 1

            val runnable = object : java.lang.Runnable {
                override fun run() {
                    if (ovelapBitmap.size > 0) {
                        binding.mainImageView.setImageBitmap(ovelapBitmap[currentImageIndex])
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
}

