package com.example.onepic.EditModule.Fragment

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
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

    private var isAdd : Boolean = false

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
            binding.focusBtn.setTextColor(requireContext().resources.getColor(R.color.do_not_click_color))
        }
        else {
            binding.magicBtn.setTextColor(requireContext().resources.getColor(R.color.do_not_click_color))
            if(!imageContent.checkAttribute(ContentAttribute.focus)) {
                binding.rewindBtn.setTextColor(requireContext().resources.getColor(R.color.do_not_click_color))
                binding.focusBtn.setTextColor(requireContext().resources.getColor(R.color.do_not_click_color))
            }

        }

        CoroutineScope(Dispatchers.Default).launch{
             imageContent.getMainBitmap()
        }
        CoroutineScope(Dispatchers.Default).launch{
            imageContent.getBitmapList()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // "Rewind" 버튼 클릭 이벤트 리스너 등록
        binding.rewindBtn.setOnClickListener {
            // 일반 사진이면 안 넘어가도록
            if(!(imageContent.mainPicture.contentAttribute == ContentAttribute.basic)){
                // RewindFragment로 이동
                findNavController().navigate(R.id.action_editFragment_to_rewindFragment)
            }
        }
        // Masic
        binding.magicBtn.setOnClickListener {
            // 일반 사진이면 안 넘어가도록
            if(!(imageContent.mainPicture.contentAttribute == ContentAttribute.basic)) {
                // MagicPictureFragment로 이동
                findNavController().navigate(R.id.action_editFragment_to_magicPictureFragment)
            }
        }
        // ADD
        binding.addBtn.setOnClickListener {
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
        // Save
        binding.saveBtn.setOnClickListener{
            val oDialog: AlertDialog.Builder = AlertDialog.Builder(
                activity,
                android.R.style.Theme_DeviceDefault_Light_Dialog
            )

            oDialog.setMessage(" 편집된 이미지만 저장하시겠습니까? 원본 이미지는 사라지지 않습니다.")
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
                            var imageContent = jpegViewModel.jpegMCContainer.value?.imageContent
                            var singlePictureList : ArrayList<Picture> =  ArrayList<Picture>(1)
                            singlePictureList.add(imageContent!!.mainPicture)
                            imageContent.setContent(singlePictureList)
                            jpegViewModel.jpegMCContainer.value?.save()
                        }catch (e :IOException){
                            Toast.makeText(activity,"저장에 실패 했습니다." , Toast.LENGTH_SHORT)
                        }
                        findNavController().navigate(R.id.action_editFragment_to_viewerFragment)


                    })
                .show()


        }

    }
}
