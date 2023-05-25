package com.goldenratio.onepic.EditModule.Fragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.goldenratio.onepic.*
import com.goldenratio.onepic.PictureModule.Contents.ActivityType
import com.goldenratio.onepic.PictureModule.Contents.ContentAttribute
import com.goldenratio.onepic.PictureModule.Contents.Picture
import com.goldenratio.onepic.PictureModule.ImageContent
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
    lateinit var fragment: Fragment
    private val jpegViewModel by activityViewModels<JpegViewModel>()
    private lateinit var imageContent : ImageContent
    private var imageTool = ImageToolModule()

    private var checkFocus = true
    private var checkChange = true
    private var checkRewind = true
    private var checkMagic = true
    private var checkAdd = true

    private var isSaved = false
    private var isFinished = MutableLiveData<Boolean>()

    private lateinit var mainSubView: View
    private var mainTextView: TextView? = null

    private var pictureList = arrayListOf<Picture>()
    private var bitmapList = arrayListOf<Bitmap>()
    private lateinit var mainPicture : Picture

    private lateinit var imageToolModule: ImageToolModule

    private var mainPictureIndex = 0
    private var currentMainViewIndex = 0


    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = context as ViewerEditorActivity
    }
    @SuppressLint("UseCompatLoadingForDrawables")
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        bundle: Bundle?
    ): View {
        // 상태바 색상 변경
        val window: Window = activity.window
            ?: throw IllegalStateException("Fragment is not attached to an activity")
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.setStatusBarColor(ContextCompat.getColor(requireContext(), android.R.color.black))

        isFinished.observe(requireActivity()){value ->
            if (value == true){
                CoroutineScope(Dispatchers.Main).launch {
                    findNavController().navigate(R.id.action_editFragment_to_viewerFragment)
                }
                isFinished.value = false // 초기화
            }
        }

        // 뷰 바인딩 설정
        binding = FragmentEditBinding.inflate(inflater, container, false)
        imageContent = jpegViewModel.jpegMCContainer.value?.imageContent!!
        imageContent.setMainBitmap(null)

        imageToolModule = ImageToolModule()

        while (!imageContent.checkPictureList) {}
        mainPicture = imageContent.mainPicture
        pictureList = imageContent.pictureList


        // 메인 이미지 설정
            CoroutineScope(Dispatchers.Main).launch {
                Glide.with(binding.mainImageView)
                    .load(imageContent.getJpegBytes(imageContent.mainPicture))
                    .into(binding.mainImageView)
            }

        if(imageContent.checkAttribute(ContentAttribute.distance_focus)) {
            binding.seekBar.visibility = View.VISIBLE
        }
        val textContent = jpegViewModel.jpegMCContainer.value!!.textContent
        val audioContent = jpegViewModel.jpegMCContainer.value!!.audioContent

        var imageCntText = "담긴 사진 ${imageContent.pictureList.size}장 "

        if(textContent.textList.size > 0) {
            imageCntText += "+ 텍스트"
        }
        if(audioContent.audio != null) {
            imageCntText += "+ 오디오"
        }

        setSubImage()

        binding.imageCntTextView.text =  imageCntText

//        if(imageContent.checkMainChangeAttribute || imageContent.checkRewindAttribute ||
//            imageContent.checkMagicAttribute || imageContent.checkAddAttribute) {
//            imageTool.showView(binding.saveBtn, true)
//        }
//        else {
//            jpegViewModel.preEditMainPicture = imageContent.mainPicture
//        }
//        if(imageContent.checkAttribute(ContentAttribute.burst)){
////            binding.focusBtn.setTextColor(requireContext().resources.getColor(R.color.do_not_click_color))
////            binding.focusBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.focus_deactivation_icon_resize, 0, 0)
//            checkFocus = false
//            if(imageContent.checkAttribute(ContentAttribute.magic)) {
//                binding.magicBtn.setTextColor(requireContext().resources.getColor(R.color.do_not_click_color))
//                binding.magicBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.magic_deactivation_icon_resize, 0, 0)
//                checkMagic = false
//            }
//        }
//        else {
//            // focus 변경 아이콘 텍스트로 수정
//            checkChange = false
//            binding.changeBtn.text = getString(R.string.focus)
//            val drawable = resources.getDrawable(R.drawable.focus_icon_resize)
//            drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
//            binding.changeBtn.setCompoundDrawables(null, drawable, null, null)
//
//
//            binding.magicBtn.setTextColor(requireContext().resources.getColor(R.color.do_not_click_color))
//            binding.magicBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.magic_deactivation_icon_resize, 0, 0)
//            checkMagic = false
//
//            if(!imageContent.checkAttribute(ContentAttribute.object_focus)) {
//                binding.rewindBtn.setTextColor(requireContext().resources.getColor(R.color.do_not_click_color))
//                binding.rewindBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.rewind_deactivation_icon_resize, 0, 0)
//                checkRewind = false
//
////                binding.focusBtn.setTextColor(requireContext().resources.getColor(R.color.do_not_click_color))
////                binding.focusBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.focus_deactivation_icon_resize, 0, 0)
////                checkFocus = false
//            }
//        }

        CoroutineScope(Dispatchers.Default).launch{
             imageContent.getMainBitmap()
        }
        CoroutineScope(Dispatchers.Default).launch{
            imageContent.getBitmapList()
        }

        /* TODO: ViewrFragment to EditorFragment - currentImageFilePath와 selectedImageFilePath 확인 */
        // ViewerFragment에서 스크롤뷰 이미지 중 아무것도 선택하지 않은 상태에서 edit 누르면 picturelist의 맨 앞 객체(메인)이 선택된 것으로 했음
        Log.d("currentImageUri: ",""+jpegViewModel.currentImageUri)
        Log.d("selectedImageFilePath: ",""+jpegViewModel.selectedSubImage)

        return binding.root
    }

    @SuppressLint("SuspiciousIndentation")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // "Rewind" 버튼 클릭 이벤트 리스너 등록
        binding.rewindBtn.setOnClickListener {
            // focus 가능한지 확인
            if (checkRewind) {
                imageContent.activityType = ActivityType.Viewer
                // RewindFragment로 이동
                findNavController().navigate(R.id.action_editFragment_to_rewindFragment)
            }
        }
        // Magic
        binding.magicBtn.setOnClickListener {
            // magic 가능한지 확인
            if (checkMagic) {
                // MagicPictureFragment로 이동
                findNavController().navigate(R.id.action_editFragment_to_magicPictureFragment)
            }
        }

        // ADD
        binding.addBtn.setOnClickListener {
            // 일반 사진이면 안 넘어가도록
            if (checkAdd) {
                // MagicPictureFragment로 이동
                findNavController().navigate(R.id.action_editFragment_to_addFragment)
            }
        }

        // 메인 변경
        binding.mainChangeBtn.setOnClickListener {
            mainTextView?.visibility = View.INVISIBLE
            val newMainSubView = binding.linear.getChildAt(currentMainViewIndex)
                .findViewById<TextView>(R.id.mainMark)
            newMainSubView.visibility = View.VISIBLE

            mainTextView = newMainSubView
            mainPicture = pictureList[currentMainViewIndex]

            val result = imageContent.removePicture(mainPicture)
            Log.d("error 잡기", "메인 바꾸고 save : ${result}")
            if (result) {
                Log.d("error 잡기", "main으로 지정된 객체 삭제 완료")

                // 2. main 사진을 첫번 째로 삽입
                imageContent.insertPicture(0, mainPicture)
                imageContent.mainPicture = mainPicture

                imageContent.checkMainChangeAttribute = true

                imageToolModule.showView(binding.saveBtn, true)
                imageToolModule.showView(binding.mainChangeBtn, false)
            }
        }


        // Viewer
        binding.backBtn.setOnClickListener {
            if (imageContent.checkMainChangeAttribute || imageContent.checkRewindAttribute ||
                imageContent.checkMagicAttribute || imageContent.checkAddAttribute
            ) {
                val oDialog: AlertDialog.Builder = AlertDialog.Builder(
                    activity,
                    android.R.style.Theme_DeviceDefault_Light_Dialog
                )

                oDialog.setMessage("편집한 내용을 저장하지 않고 나가시겠습니까?")
                    .setPositiveButton(
                        "취소",
                        DialogInterface.OnClickListener { _, _ -> })
                    .setNeutralButton("확인",
                        DialogInterface.OnClickListener { _, _ ->
                            imageContent.resetBitmap()
                            setButtonDeactivation()

                            val pre = jpegViewModel.preEditMainPicture
                            if (pre != null) {
                                imageContent.mainPicture = pre
                                jpegViewModel.preEditMainPicture = null
                            }

                            findNavController().navigate(R.id.action_editFragment_to_viewerFragment)
                        }).show()
            } else {
                findNavController().navigate(R.id.action_editFragment_to_viewerFragment)
            }
        }

        // Save
        binding.saveBtn.setOnClickListener {
            ViewerFragment.isEditStoraged = true
            imageTool.showView(binding.progressBar2, true)

            imageContent = jpegViewModel.jpegMCContainer.value?.imageContent!!

            // 덮어쓰기
            val currentFilePath = jpegViewModel.currentImageUri

            var fileName: String = ""
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                fileName = jpegViewModel.getFileNameFromUri(currentFilePath!!.toUri())
            } else {
                fileName = currentFilePath!!.substring(currentFilePath.lastIndexOf("/") + 1);
            }
            //val fileName = currentFilePath!!.substring(currentFilePath.lastIndexOf("/") + 1);
            var result = jpegViewModel.jpegMCContainer.value?.overwiteSave(fileName)
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

    fun setButtonDeactivation() {
        imageContent.checkAddAttribute = false
        imageContent.checkRewindAttribute = false
        imageContent.checkMagicAttribute = false
        imageContent.checkMainChangeAttribute = false
    }

    fun setCurrentPictureByteArrList(){

        var pictureList = jpegViewModel.jpegMCContainer.value?.getPictureList()

        if (pictureList != null) {

            CoroutineScope(Dispatchers.IO).launch {
                val pictureByteArrayList = mutableListOf<ByteArray>()
                for (picture in pictureList){
                    val pictureByteArr = jpegViewModel.jpegMCContainer.value?.imageContent?.getJpegBytes(picture)
                    pictureByteArrayList.add(pictureByteArr!!)
                } // end of for..
                jpegViewModel.setpictureByteArrList(pictureByteArrayList)
                CoroutineScope(Dispatchers.Main).launch {
                    isFinished.value = true
                }
            }
        }
    }

    fun remainOriginalPictureSave(){
        val oDialog: AlertDialog.Builder = AlertDialog.Builder(
            activity,
            android.R.style.Theme_DeviceDefault_Light_Dialog
        )
        imageTool.showView(binding.progressBar2, false)
        oDialog.setMessage("편집된 이미지만 저장하시겠습니까? 원본 이미지는 사라지지 않습니다.")
            .setPositiveButton(
                "모두 저장",
                DialogInterface.OnClickListener { dialog, which ->
                    imageTool.showView(binding.progressBar2, true)
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
//                        imageTool.showView(binding.progressBar2 , false)
                            findNavController().navigate(R.id.action_editFragment_to_viewerFragment)
                        }
                    }
                })
            .setNeutralButton("예",
                DialogInterface.OnClickListener { dialog, which ->
                    try {
                        singleSave()
                    }catch (e: IOException) {
                        Toast.makeText(activity, "저장에 실패 했습니다.", Toast.LENGTH_SHORT)
                            .show()
                    }
//                        imageTool.showView(binding.progressBar2, true)
//                        val newImageContent =
//                            jpegViewModel.jpegMCContainer.value?.imageContent!!
//                        val singlePictureList: ArrayList<Picture> =
//                            ArrayList<Picture>(1)
//                        singlePictureList.add(newImageContent.mainPicture)
//                        newImageContent.setContent(singlePictureList)
//
//                        var savedFilePath = jpegViewModel.jpegMCContainer.value?.save()
//                        //ViewerFragment.currentFilePath = savedFilePath.toString()
//
//                    } catch (e: IOException) {
//                        Toast.makeText(activity, "저장에 실패 했습니다.", Toast.LENGTH_SHORT)
//                            .show()
//                    }
//                    CoroutineScope(Dispatchers.Default).launch {
//                        setButtonDeactivation()
//                        Thread.sleep(1000)
//                        withContext(Dispatchers.Main) {
//                            findNavController().navigate(R.id.action_editFragment_to_viewerFragment)
//                        }
//                    }
                    })
            .show()
    }

    fun singleSave(){
        try {
            imageTool.showView(binding.progressBar2, true)
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

    @SuppressLint("ResourceAsColor")
    fun setSubImage() {

        for (i in 0 until pictureList.size) {
            // 넣고자 하는 layout 불러오기
            val subLayout =
                layoutInflater.inflate(R.layout.scroll_item_layout_edit, null)

            // 위 불러온 layout에서 변경을 할 view가져오기
            val imageView: ImageView =
                subLayout.findViewById(R.id.scrollImageView)

            // 이미지뷰에 붙이기
            CoroutineScope(Dispatchers.Main).launch {
                Log.d("error 잡기", "$i 번째 이미지 띄우기")
                Glide.with(imageView)
                    .load(imageContent.getJpegBytes(pictureList[i]))
                    .into(imageView)
            }

            if (i == mainPictureIndex) {
//                imageToolModule.showView(subLayout.findViewById(R.id.checkMainIcon), true)
                mainSubView = imageView

                CoroutineScope(Dispatchers.Main).launch {
                    mainSubView.setBackgroundResource(R.drawable.chosen_image_border)
                    mainSubView.setPadding(6,6,6,6)

                    mainTextView = subLayout.findViewById<TextView>(R.id.mainMark)
                    if(mainTextView != null)
                        imageToolModule.showView(mainTextView!!, true)
                }
            }

            subLayout.setOnClickListener {
                    mainSubView.background = null
                    mainSubView.setPadding(0, 0, 0, 0)

                mainPicture = pictureList[i]
//                imageToolModule.showView(mainSubView, false)
                CoroutineScope(Dispatchers.Main).launch {
                    // 메인 이미지 설정
                    Glide.with(binding.mainImageView)
                        .load(imageContent.getJpegBytes(mainPicture))
                        .into(binding.mainImageView)
                    imageView.setBackgroundResource(R.drawable.chosen_image_border)
                    imageView.setPadding(6,6,6,6)

                    if(mainPictureIndex != i) {
                        imageToolModule.showView(binding.mainChangeBtn, true)
                    }
                    else {
                        imageToolModule.showView(binding.mainChangeBtn, false)
                    }
                }
                currentMainViewIndex = i
                mainSubView = imageView
            }

            CoroutineScope(Dispatchers.Main).launch {
                // main activity에 만들어둔 scrollbar 속 layout의 아이디를 통해 해당 layout에 넣기
                binding.linear.addView(subLayout)
            }
        }
    }

}
