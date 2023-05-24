package com.goldenratio.onepic.EditModule.Fragment

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.goldenratio.onepic.ImageToolModule
import com.goldenratio.onepic.JpegViewModel
import com.goldenratio.onepic.PictureModule.Contents.ActivityType
import com.goldenratio.onepic.PictureModule.Contents.ContentAttribute
import com.goldenratio.onepic.PictureModule.Contents.Picture
import com.goldenratio.onepic.PictureModule.ImageContent
import com.goldenratio.onepic.R
import com.goldenratio.onepic.databinding.FragmentFocusChangeBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class FocusChangeFragment : Fragment() {


    private lateinit var binding: FragmentFocusChangeBinding
    lateinit var fragment: Fragment
    private val jpegViewModel by activityViewModels<JpegViewModel>()
    private lateinit var imageContent : ImageContent

    private var pictureList = arrayListOf<Picture>()
    private var bitmapList = arrayListOf<Bitmap>()
    private lateinit var mainPicture : Picture

    private lateinit var imageToolModule: ImageToolModule
    private var mainIndex = 0

    private lateinit var mainSubView: View

    private lateinit var checkFinish: BooleanArray

    private var infoLevel = MutableLiveData<InfoLevel>()

    val max = 5

    enum class InfoLevel {
        BeforeMainSelect,
        AfterMainSelect
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 상태바 색상 변경
        val window: Window = activity?.window
            ?: throw IllegalStateException("Fragment is not attached to an activity")
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.setStatusBarColor(ContextCompat.getColor(requireContext(), android.R.color.black))

        // 뷰 바인딩 설정
        binding = FragmentFocusChangeBinding.inflate(inflater, container, false)

        imageContent = jpegViewModel.jpegMCContainer.value?.imageContent!!
        imageToolModule = ImageToolModule()

        checkFinish = BooleanArray(pictureList.size)


            while(!imageContent.checkPictureList) {}

            mainPicture = imageContent.mainPicture
            pictureList = imageContent.pictureList

        CoroutineScope(Dispatchers.Default).launch {
            val bitmap = imageContent.getBitmapList()
            if(bitmap!=null)
                bitmapList = bitmap
        }

        CoroutineScope(Dispatchers.Main).launch {
            // 파일을 parsing해서 PictureContainer로 바꾸는 함수 호출
            // 메인 이미지 설정
            withContext(Dispatchers.Main) {
                Glide.with(binding.focusMainView)
                    .load(imageContent.getJpegBytes(mainPicture))
                    .into(binding.focusMainView)
            }
        }

        binding.focusSaveBtn.setOnClickListener {
            imageContent.resetBitmap()
            imageToolModule.showView(binding.progressBar , true)
            CoroutineScope(Dispatchers.Default).launch {
                // 1. main으로 지정된 picture를 picturelist에서 삭제
                var result = imageContent.removePicture(mainPicture)
                Log.d("error 잡기", "메인 바꾸고 save : ${result}")
                if (result) {
                    Log.d("error 잡기", "main으로 지정된 객체 삭제 완료")

                    // 2. main 사진을 첫번 째로 삽입
                    imageContent.insertPicture(0, mainPicture)
                    imageContent.mainPicture = mainPicture
                }


                if(imageContent.activityType == ActivityType.Camera) {
                    withContext(Dispatchers.Main){
                        Log.d("error 잡기", "바로 편집에서 save() 호출 전")
                        jpegViewModel.jpegMCContainer.value?.save()
                        Log.d("error 잡기", "바로 편집에서 save() 호출후")
                        imageContent.checkMainChangeAttribute = true
                        Thread.sleep(2000)
                        imageToolModule.showView(binding.progressBar , false)
                        findNavController().navigate(R.id.action_focusChangeFragment_to_Fragment)
                    }
                }
                else{
                    withContext(Dispatchers.Main){
                        Log.d("error 잡기", "바로 편집에서 navigate호출 전")
                        imageContent.checkMainChangeAttribute = true
                        findNavController().navigate(R.id.action_focusChangeFragment_to_Fragment)
                    }
                    imageToolModule.showView(binding.progressBar , false)

                }

            }
        }
        binding.focusCloseBtn.setOnClickListener {
            CoroutineScope(Dispatchers.Default).launch {
                withContext(Dispatchers.Main) {
                    findNavController().navigate(R.id.action_focusChangeFragment_to_Fragment)
                }
            }
        }

//        Thread.sleep(3000)
        Log.d("error 잡기", "focusEdit picureList size ${pictureList.size}")
        if(imageContent.activityType == ActivityType.Viewer) {
            infoLevel.value = InfoLevel.BeforeMainSelect
            infoLevel.observe(viewLifecycleOwner){ _ ->
                infoTextView()
            }
        }
        else {
            infoLevel.value = InfoLevel.AfterMainSelect
            infoTextView()
        }

        // info 확인
        binding.focusInfoBtn.setOnClickListener {
            imageToolModule.showView(binding.infoDialogLayout, true)
        }

        // info 삭제
        binding.dialogCloseBtn.setOnClickListener {
            imageToolModule.showView(binding.infoDialogLayout, false)
        }

        CoroutineScope(Dispatchers.Default).launch {
            if (imageContent.checkAttribute(ContentAttribute.distance_focus)) {

                setSeekBar()
            }
        }


        return binding.root
    }

    fun infoTextView() {
        Log.d("infoTextView","infoTextView call")
        when (infoLevel.value) {
            InfoLevel.BeforeMainSelect -> {
                binding.infoText.text = "아래 사진을 선택해\n메인 이미지를 변경할 수 있습니다."
            }
            InfoLevel.AfterMainSelect -> {
                binding.infoText.text = "choice Best버튼을 클릭해\n메인 이미지를 추천 받을 수 있습니다."
            }
            else -> {}
        }
    }

    fun setSeekBar(){
        while(!imageContent.checkPictureList) {}
        Log.d("seekBar","#####")
        imageToolModule.showView(binding.seekBar, true)

        binding.seekBar.max = pictureList.size - 1

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // SeekBar의 값이 변경될 때 호출되는 메서드입니다.
                // progress 변수는 현재 SeekBar의 값입니다.
                // fromUser 변수는 사용자에 의해 변경된 값인지 여부를 나타냅니다.
                if (fromUser) {
                    val index = progress % pictureList.size
                    mainPicture = pictureList[index]

                    // 글라이드로만 seekbar 사진 변화 하면 좀 끊겨 보이길래
                    if (bitmapList.size > index) {
                        // 만들어 졌으면 비트맵으로 띄웠어
                        CoroutineScope(Dispatchers.Main).launch {
                            binding.focusMainView.setImageBitmap(bitmapList[index])
                        }
                    } else {
                        // 비트맵은 따로 만들고 있고 해당 index의 비트맵이 안만들어졌음명 글라이드로
                        CoroutineScope(Dispatchers.Main).launch {
                            Log.d("error 잡기", "$progress 번째 이미지 띄우기")
                            Glide.with(binding.focusMainView)
                                .load(imageContent.getJpegBytes(pictureList[index]))
                                .into(binding.focusMainView)
                        }
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
}