package com.example.onepic.EditModule.Fragment

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.onepic.*
import com.example.onepic.PictureModule.Contents.ActivityType
import com.example.onepic.PictureModule.Contents.ContentAttribute
import com.example.onepic.PictureModule.Contents.Picture
import com.example.onepic.PictureModule.ImageContent
import com.example.onepic.ViewerModule.ViewerEditorActivity
import com.example.onepic.databinding.FragmentEditBinding
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

    companion object{
        private var isAdd : Boolean = false
        private var isOnylMainChange : Boolean? = null
    }


    private var checkFocus = true
    private var checkRewind = true
    private var checkMagic = true

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = context as ViewerEditorActivity
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        bundle: Bundle?
    ): View {
        // 뷰 바인딩 설정
        binding = FragmentEditBinding.inflate(inflater, container, false)

        imageContent = jpegViewModel.jpegMCContainer.value?.imageContent!!
        imageContent.activityType = ActivityType.Viewer

        CoroutineScope(Dispatchers.Default).launch {
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
        Log.d("currentImageFilePath: ",""+jpegViewModel.currentImageFilePath)
        Log.d("selectedImageFilePath: ",""+jpegViewModel.selectedSubImage)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // focus - > mainChage
        binding.focusBtn.setOnClickListener {
            isAdd = false
            if(isOnylMainChange == null)
                isOnylMainChange = true

            imageContent.activityType = ActivityType.Viewer
            findNavController().navigate(R.id.action_editFragment_to_burstModeEditFragment)
        }
        // "Rewind" 버튼 클릭 이벤트 리스너 등록
        binding.rewindBtn.setOnClickListener {
            isOnylMainChange = false
            isAdd = false
            // focus 가능한지 확인
            if(checkRewind){
                imageContent.activityType = ActivityType.Viewer
                // RewindFragment로 이동
                findNavController().navigate(R.id.action_editFragment_to_rewindFragment)
            }
        }
        // Magic
        binding.magicBtn.setOnClickListener {
            isOnylMainChange = false
            isAdd = false
            // magic 가능한지 확인
            if(checkMagic) {
                // MagicPictureFragment로 이동
                findNavController().navigate(R.id.action_editFragment_to_magicPictureFragment)
            }
        }
        // ADD
        binding.addBtn.setOnClickListener {
            isOnylMainChange = false
            // 일반 사진이면 안 넘어가도록
            isAdd = true
            // MagicPictureFragment로 이동
            findNavController().navigate(R.id.action_editFragment_to_addFragment)

           // if(!(imageContent.mainPicture.contentAttribute == ContentAttribute.basic))

            //findNavController().navigate(R.id.action_editFragment_to_addFragment)
        }

        // Viewer
        binding.backBtn.setOnClickListener {
            findNavController().navigate(R.id.action_editFragment_to_viewerFragment)

        }
        Log.d("burst", isOnylMainChange.toString())
        // Save
        binding.saveBtn.setOnClickListener{

            imageContent = jpegViewModel.jpegMCContainer.value?.imageContent!!

            Log.d("burst","edit창에서 save click")
            val oDialog: AlertDialog.Builder = AlertDialog.Builder(
                activity,
                android.R.style.Theme_DeviceDefault_Light_Dialog
            )
            if(imageContent.checkMainChangeAttribute && !imageContent.checkRewindAttribute &&
                    !imageContent.checkMagicAttribute && !imageContent.checkAddAttribute) {
                // 편집 중 mina만 변경했을 경우 해당 파일 덮어쓰기
                val currentFilePath = jpegViewModel.currentImageFilePath
                val fileName = currentFilePath!!.substring(currentFilePath.lastIndexOf("/") + 1);
                jpegViewModel.currentImageFilePath
                jpegViewModel.jpegMCContainer.value?.overwiteSave(fileName)
                findNavController().navigate(R.id.action_editFragment_to_viewerFragment)

            }
            else if(imageContent.checkMagicAttribute) {
                // magic으로 변경했을 경우 모든 파일 저장
                if(!isAdd){
                    val mainPicture = imageContent.mainPicture
                    // 바뀐 비트맵을 Main(맨 앞)으로 하는 새로운 Jpeg 저장
                    imageContent.insertPicture(0, mainPicture)
                } else isAdd = false
                jpegViewModel.jpegMCContainer.value?.save()
                findNavController().navigate(R.id.action_editFragment_to_viewerFragment)
            }
            else{
                oDialog.setMessage("편집된 이미지만 저장하시겠습니까? 원본 이미지는 사라지지 않습니다.")
                    .setPositiveButton("모두 저장", DialogInterface.OnClickListener { dialog, which ->
                        if(!isAdd){
                            val mainPicture = imageContent.mainPicture
                            // 바뀐 비트맵을 Main(맨 앞)으로 하는 새로운 Jpeg 저장
                            imageContent.insertPicture(0, mainPicture)
                        } else isAdd = false
                        jpegViewModel.jpegMCContainer.value?.save()
                        findNavController().navigate(R.id.action_editFragment_to_viewerFragment)
                    })
                    .setNeutralButton("예",
                        DialogInterface.OnClickListener { dialog, which ->
                            try{
                                val newImageContent = jpegViewModel.jpegMCContainer.value?.imageContent!!
                                val singlePictureList : ArrayList<Picture> =  ArrayList<Picture>(1)
                                singlePictureList.add(newImageContent.mainPicture)
                                newImageContent.setContent(singlePictureList)
                                jpegViewModel.jpegMCContainer.value?.save()
                            }catch (e :IOException){
                                Toast.makeText(activity,"저장에 실패 했습니다." , Toast.LENGTH_SHORT).show()
                            }
                            findNavController().navigate(R.id.action_editFragment_to_viewerFragment)
                        })
                    .show()
            }
            imageContent.setCheckAttribute()
        }
    }

    override fun onResume() {
        super.onResume()
        isAdd = false
        //isOnylMainChange = null
    }
}
