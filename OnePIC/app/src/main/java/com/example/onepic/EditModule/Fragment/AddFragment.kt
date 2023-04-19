package com.example.onepic.EditModule.Fragment

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.Toast
import androidx.camera.core.impl.utils.ContextUtil.getApplicationContext
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModel
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.onepic.AudioModule.AudioResolver
import com.example.onepic.ImageToolModule
import com.example.onepic.JpegViewModel
import com.example.onepic.PictureModule.Contents.ContentAttribute
import com.example.onepic.PictureModule.Contents.Picture
import com.example.onepic.PictureModule.Contents.Text
import com.example.onepic.PictureModule.ImageContent
import com.example.onepic.R
import com.example.onepic.ViewerModule.ViewerEditorActivity
import com.example.onepic.databinding.FragmentAddBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList


class AddFragment : Fragment() {

    private lateinit var activity: ViewerEditorActivity
    private lateinit var binding: FragmentAddBinding
    private lateinit var imageToolModule : ImageToolModule
    private lateinit var mainPicture : Picture

    protected val jpegViewModel by activityViewModels<JpegViewModel>()
    protected lateinit var imageContent : ImageContent

    // audio
    var isAudioOn : Boolean = false
    var isRecording : Boolean = false
    private lateinit var audioResolver :AudioResolver
    private lateinit var timerTask: TimerTask

    // text
    var isTextOn : Boolean = false
    var textList : ArrayList<String> = arrayListOf()


    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = context as ViewerEditorActivity
        audioResolver = AudioResolver(activity)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentAddBinding.inflate(inflater, container, false)

        imageContent = jpegViewModel.jpegMCContainer.value?.imageContent!!
        imageToolModule = ImageToolModule()

        //binding.contentLayout.visibility = View.GONE
        textInit()
        // main Picture의 byteArray를 bitmap 제작
        mainPicture = imageContent.mainPicture
        var mainBitmap = ImageToolModule().byteArrayToBitmap(imageContent.getJpegBytes(mainPicture))

        // imageView 변환
        binding.addMainView.setImageBitmap(mainBitmap)


        // save btn 클릭 시
        binding.addSaveBtn.setOnClickListener {
            if(isRecording){
                audioSave()
            }
            //TODO(텍스트 메시지가 몇개 저장되어 있는지 보이도록)
            //jpegViewModel.jpegMCContainer.value!!.setTextConent(ContentAttribute.basic, textList)
            findNavController().navigate(R.id.action_addFragment_to_editFragment)
        }

        // close btn 클릭 시
        binding.addCloseBtn.setOnClickListener {
            findNavController().navigate(R.id.action_addFragment_to_editFragment)
        }

        // text btn 클릭 시
        binding.textAddBtn.setOnClickListener {
            if(!isTextOn){
                if(isAudioOn){
                    isAudioOn = false
                    binding.contentLayout.visibility = View.GONE
                }
                isTextOn = true
                //binding.recordingImageView.setImageDrawable(resources.getDrawable(R.drawable.record))
                binding.textContentLayout.visibility = View.VISIBLE

            }else{
                textInit()
                binding.textContentLayout.visibility = View.GONE
            }

        }

        // text의 확인 클릭 시
        binding.checkButton.setOnClickListener {
            var textMessage : String = binding.editText.text.toString()
            if(textMessage != ""){
                var text : Text = Text(textMessage, ContentAttribute.basic)
                jpegViewModel.jpegMCContainer.value!!.textContent.insertText(text)
                binding.editText.setText("")
            }
        }

        // audio btn 클릭 시
        binding.audioAddBtn.setOnClickListener {
            if(!isAudioOn){
                if(isTextOn){
                    textInit()
                    binding.textContentLayout.visibility = View.GONE
                }
                isAudioOn = true
                binding.recordingImageView.setImageDrawable(resources.getDrawable(R.drawable.record))
                binding.contentLayout.visibility = View.VISIBLE

            }else{
                isAudioOn = false
                binding.contentLayout.visibility = View.GONE
            }

        }

        binding.recordingImageView.setOnClickListener {
            if(isRecording) {
                /* 녹음 중단 */
                // UI
                binding.recordingImageView.setImageDrawable(resources.getDrawable(R.drawable.record))
                timerUIStop()
                // 녹음 중단 후 저장
                audioSave()
            }
            else{
                /* 녹음 시작 */
                // UI
                Glide.with(this).load(R.raw.giphy).into(binding.recordingImageView);
                timerUIStart()

                // 녹음 시작
                audioResolver.startRecording("edit_record")
                isRecording = true
            }
        }
        return binding.root
    }

    fun textInit(){
        isTextOn = false
        textList.clear()
        CoroutineScope(Dispatchers.Main).launch{
            binding.editText.setText("")
        }

    }
    fun audioSave(){
        // 녹음 중단, 저장
        var savedFile = audioResolver.stopRecording()
        isRecording = false
        //MC Container에 추가
        var auioBytes = audioResolver.getByteArrayInFile(savedFile!!)
        jpegViewModel.jpegMCContainer.value!!.setAudioContent(auioBytes, ContentAttribute.basic)

    }
    fun timerUIStart(){
        if(!isRecording){
            timerTask = object : TimerTask() {
                var cnt = 0
                override fun run() {
                    CoroutineScope(Dispatchers.Main).launch {

                        var string : String = String.format("%02d:%02d", cnt/60, cnt)
                        binding.RecordingTextView.setText(string)
                        cnt++

                        if(cnt > 30){
                            timerUIStop()
                        }
                    }
                }
            }
            val timer = Timer()
            timer.schedule(timerTask, 0, 1000)
        }
    }

    fun timerUIStop(){
        if(isRecording){
            timerTask.cancel()
            CoroutineScope(Dispatchers.Main).launch {
                binding.RecordingTextView.setText("")
                Toast.makeText(activity, "녹음이 완료 되었습니다", Toast.LENGTH_SHORT).show();
            }
        }
    }
}