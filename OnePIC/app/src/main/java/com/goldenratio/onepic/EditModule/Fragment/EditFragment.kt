package com.goldenratio.onepic.EditModule.Fragment

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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
import java.io.File
import java.io.IOException

class EditFragment : Fragment(R.layout.fragment_edit) {
    private lateinit var binding: FragmentEditBinding
    private lateinit var activity: ViewerEditorActivity
    lateinit var fragment: Fragment
    private val jpegViewModel by activityViewModels<JpegViewModel>()
    private lateinit var imageContent : ImageContent
    private var imageTool = ImageToolModule()

    private var checkFocus = true
    private var checkRewind = true
    private var checkMagic = true
    private var checkAdd = true

    private var isSaved = false
    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = context as ViewerEditorActivity
    }
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

        // 뷰 바인딩 설정
        binding = FragmentEditBinding.inflate(inflater, container, false)
        imageContent = jpegViewModel.jpegMCContainer.value?.imageContent!!
        imageContent.resetMainBitmap()
        imageContent.activityType = ActivityType.Viewer

        while (!imageContent.checkPictureList) {}

        if(imageContent.checkMainChangeAttribute || imageContent.checkRewindAttribute ||
            imageContent.checkMagicAttribute || imageContent.checkAddAttribute) {
            imageTool.showView(binding.saveBtn, true)
        }

        CoroutineScope(Dispatchers.Main).launch {
            // 파일을 parsing해서 PictureContainer로 바꾸는 함수 호출
            // 메인 이미지 설정

            withContext(Dispatchers.Main) {
                Glide.with(binding.mainImageView)
                    .load(imageContent.getJpegBytes(imageContent.mainPicture))
                    .into(binding.mainImageView)
            }
        }

        if(imageContent.checkAttribute(ContentAttribute.burst)){
//            binding.focusBtn.setTextColor(requireContext().resources.getColor(R.color.do_not_click_color))
//            binding.focusBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.focus_deactivation_icon_resize, 0, 0)
//            checkFocus = false
        }
        else {
            binding.magicBtn.setTextColor(requireContext().resources.getColor(R.color.do_not_click_color))
            binding.magicBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.magic_deactivation_icon_resize, 0, 0)
            checkMagic = false

            if(!imageContent.checkAttribute(ContentAttribute.focus)) {
                binding.rewindBtn.setTextColor(requireContext().resources.getColor(R.color.do_not_click_color))
                binding.rewindBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.rewind_deactivation_icon_resize, 0, 0)
                checkRewind = false

//                binding.focusBtn.setTextColor(requireContext().resources.getColor(R.color.do_not_click_color))
//                binding.focusBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.focus_deactivation_icon_resize, 0, 0)
//                checkFocus = false
            }
        }

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // focus - > mainChange
        binding.focusBtn.setOnClickListener {
            if(checkFocus) {
                imageContent.activityType = ActivityType.Viewer
                findNavController().navigate(R.id.action_editFragment_to_burstModeEditFragment)
            }
        }
        // "Rewind" 버튼 클릭 이벤트 리스너 등록
        binding.rewindBtn.setOnClickListener {
            // focus 가능한지 확인
            if(checkRewind){
                imageContent.activityType = ActivityType.Viewer
                // RewindFragment로 이동
                findNavController().navigate(R.id.action_editFragment_to_rewindFragment)
            }
        }
        // Magic
        binding.magicBtn.setOnClickListener {
            // magic 가능한지 확인
            if(checkMagic) {
                // MagicPictureFragment로 이동
                findNavController().navigate(R.id.action_editFragment_to_magicPictureFragment)
            }
        }

        // ADD
        binding.addBtn.setOnClickListener {
            // 일반 사진이면 안 넘어가도록
            if(checkAdd) {
                // MagicPictureFragment로 이동
                findNavController().navigate(R.id.action_editFragment_to_addFragment)
            }
        }

        // Viewer
        binding.backBtn.setOnClickListener {
            if(imageContent.checkMainChangeAttribute || imageContent.checkRewindAttribute ||
                imageContent.checkMagicAttribute || imageContent.checkAddAttribute) {
                val oDialog: AlertDialog.Builder = AlertDialog.Builder(
                    activity,
                    android.R.style.Theme_DeviceDefault_Light_Dialog
                )

                oDialog.setMessage("편집한 내용을 저장하지 않고 나가시겠습니까?")
                    .setPositiveButton(
                        "취소",
                        DialogInterface.OnClickListener { dialog, which -> })
                    .setNeutralButton("확인",
                        DialogInterface.OnClickListener { dialog, which ->
                            findNavController().navigate(R.id.action_editFragment_to_viewerFragment)
                        }).show()
            }
            else {
                findNavController().navigate(R.id.action_editFragment_to_viewerFragment)
            }
        }

        // Save
        binding.saveBtn.setOnClickListener{

              ViewerFragment.isEditStoraged = true
//            if(!imageContent.checkMainChangeAttribute && !imageContent.checkRewindAttribute &&
//                !imageContent.checkMagicAttribute && !imageContent.checkAddAttribute) {
//                findNavController().navigate(R.id.action_editFragment_to_viewerFragment)
//            }
//            else {
                imageTool.showView(binding.progressBar2, true)

                imageContent = jpegViewModel.jpegMCContainer.value?.imageContent!!
                val oDialog: AlertDialog.Builder = AlertDialog.Builder(
                    activity,
                    android.R.style.Theme_DeviceDefault_Light_Dialog
                )


            // ADD 혹은 Main Chage만 했을 경우
                if (imageContent.checkMainChangeAttribute && !imageContent.checkRewindAttribute &&
                    !imageContent.checkMagicAttribute && !imageContent.checkAddAttribute
                    || !imageContent.checkMainChangeAttribute && !imageContent.checkRewindAttribute &&
                    !imageContent.checkMagicAttribute && imageContent.checkAddAttribute)

//                if (imageContent.checkMainChangeAttribute || imageContent.checkAddAttribute &&
//                    !imageContent.checkRewindAttribute && !imageContent.checkMagicAttribute
//
//                 )
                {
                    // 편집 중 mina만 변경했을 경우 해당 파일 덮어쓰기
                    val currentFilePath = jpegViewModel.currentImageUri
                    var fileName : String = ""
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
                        fileName = jpegViewModel.getFileNameFromUri(currentFilePath!!.toUri())
                    }else{
                        fileName = currentFilePath!!.substring(currentFilePath.lastIndexOf("/") + 1);
                    }
                    //val fileName = currentFilePath!!.substring(currentFilePath.lastIndexOf("/") + 1);
                    var result = jpegViewModel.jpegMCContainer.value?.overwiteSave(fileName)
                    // 우리 앱의 사진이 아닐 때
                    if(result == "another"){
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
                                        imageTool.showView(binding.progressBar2, true)
                                        val newImageContent =
                                            jpegViewModel.jpegMCContainer.value?.imageContent!!
                                        val singlePictureList: ArrayList<Picture> =
                                            ArrayList<Picture>(1)
                                        singlePictureList.add(newImageContent.mainPicture)
                                        newImageContent.setContent(singlePictureList)

                                        jpegViewModel.jpegMCContainer.value?.save()
                                        //ViewerFragment.currentFilePath = savedFilePath.toString()

                                    } catch (e: IOException) {
                                        Toast.makeText(activity, "저장에 실패 했습니다.", Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                    CoroutineScope(Dispatchers.Default).launch {
                                        setButtonDeactivation()
                                        Thread.sleep(1000)
                                        withContext(Dispatchers.Main) {
                                            findNavController().navigate(R.id.action_editFragment_to_viewerFragment)
                                        }
                                    }
                                })
                            .show()

                    }
                    else{
                        CoroutineScope(Dispatchers.Default).launch {
                            setButtonDeactivation()
                            Thread.sleep(2000)
                            withContext(Dispatchers.Main) {
                                findNavController().navigate(R.id.action_editFragment_to_viewerFragment)
                            }
                        }
                    }
                    // 매직픽쳐 편집을 한 경우
                } else if (imageContent.checkMagicAttribute) {
                    imageContent.pictureList[0].contentAttribute = ContentAttribute.magic
                    // magic으로 변경했을 경우 모든 파일 저장
                    var savedFilePath = jpegViewModel.jpegMCContainer.value?.save()
                    //ViewerFragment.currentFilePath = savedFilePath.toString()
                    CoroutineScope(Dispatchers.Default).launch {
                        setButtonDeactivation()
                        Thread.sleep(2000)
                        withContext(Dispatchers.Main) {
//                        imageTool.showView(binding.progressBar2 , false)
                            findNavController().navigate(R.id.action_editFragment_to_viewerFragment)
                        }
                }
                    //
                } else {
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
                                CoroutineScope(Dispatchers.Default).launch {
                                    setButtonDeactivation()
                                    Thread.sleep(1000)
                                    withContext(Dispatchers.Main) {
                                        findNavController().navigate(R.id.action_editFragment_to_viewerFragment)
                                    }
                                }
                            })
                        .show()
          }
            imageContent.setCheckAttribute()
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
                    CoroutineScope(Dispatchers.Default).launch {
                        setButtonDeactivation()
                        Thread.sleep(1000)
                        withContext(Dispatchers.Main) {
                            findNavController().navigate(R.id.action_editFragment_to_viewerFragment)
                        }
                    }
                })
            .show()
    }
    fun setButtonDeactivation() {
        checkFocus = false
        checkRewind = false
        checkMagic = false
        checkAdd = false
    }
}
