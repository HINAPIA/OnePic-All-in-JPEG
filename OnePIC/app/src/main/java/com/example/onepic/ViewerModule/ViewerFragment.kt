package com.example.onepic.ViewerModule

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.ContentUris
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.bumptech.glide.Glide
import com.example.onepic.LoadModule.LoadResolver
import com.example.onepic.JpegViewModel
import com.example.onepic.R
import com.example.onepic.databinding.FragmentViewerBinding
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

@SuppressLint("LongLogTag")
class ViewerFragment : Fragment() {

    private lateinit var binding: FragmentViewerBinding
    private val jpegViewModel by activityViewModels<JpegViewModel>()
    private var loadResolver : LoadResolver = LoadResolver()
    private lateinit var mainViewPagerAdapter:ViewPagerAdapter
    private var isContainerChanged = MutableLiveData<Boolean>()
    private var currentPosition:Int? = null

    /* text, audio, magic 선택 여부 */
    private var isTxtBtnClicked = false
    private var isAudioBtnClicked = false
    private var isMagicBtnClicked = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentViewerBinding.inflate(inflater, container, false)
        Log.d("[ViewerFragment] onCreateView: ","fragment 전환됨")
        currentPosition = arguments?.getInt("currentPosition")
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init()

        if(currentPosition != null){ // GalleryFragment에서 넘어왔을 때 (선택된 이미지가 있음)
            binding.viewPager2.setCurrentItem(currentPosition!!,false)
            currentPosition = null
        }

        // gallery에 들어있는 사진이 변경되었을 때, 화면 다시 reload
        jpegViewModel.imageUriLiveData.observe(viewLifecycleOwner){
            mainViewPagerAdapter.setUriList(jpegViewModel.imageUriLiveData.value!!)
            // 기존 위치 curr Position을 가지고 있어야 하나..?
            mainViewPagerAdapter.notifyDataSetChanged()
        }
    }

    /** ViewPager Adapter 및 swipe callback 설정 & Button 이벤트 처리 */
    @RequiresApi(Build.VERSION_CODES.M)
    fun init() {

        mainViewPagerAdapter = ViewPagerAdapter(requireContext())
        mainViewPagerAdapter.setUriList(jpegViewModel.imageUriLiveData.value!!)

        binding.viewPager2.adapter = mainViewPagerAdapter
        binding.viewPager2.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            @RequiresApi(Build.VERSION_CODES.Q)
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                Log.d("[ViewerFragment] 바뀐 position : ", ""+position)
                mainViewPagerAdapter.notifyDataSetChanged()
                // 텍스트 버튼 초기화
                if( isTxtBtnClicked ) { // 클릭 되어 있던 상태
                    binding.textBtn.background = ColorDrawable(Color.TRANSPARENT)
                    isTxtBtnClicked = false
                    binding.textLinear.visibility = View.INVISIBLE
                    //TODO: 1) mainPictureDrawable도 초기화, 2) FrameLayout에 추가 되었었던 View hidden 처리
                }
                // 오디오 버튼 초기화
                if( isAudioBtnClicked ) { // 클릭 되어 있던 상태
                    binding.audioBtn.background = ColorDrawable(Color.TRANSPARENT)
                    isAudioBtnClicked = false
                }

                // 재생 중인 오디오 stop
                jpegViewModel.jpegMCContainer.value!!.audioResolver.audioStop()

                // 매직 버튼 초기화
                if( isMagicBtnClicked ) { // 클릭 되어 있던 상태
                    binding.magicBtn.background = ColorDrawable(Color.TRANSPARENT)
                    isMagicBtnClicked = false
                    mainViewPagerAdapter.setCheckMagicPicturePlay(false)

                }
                setCurrentMCContainer(position)
            }
        })

        // MCContainer가 변경되었을 때(Page가 넘어갔을 때) 처리
        isContainerChanged.observe(requireActivity()){ value ->
            if (value == true){
                setCurrentOtherImage()
                isContainerChanged.value = false
            }
        }

        /** Button 이벤트 리스너 - editBtn, backBtn, textBtn*/

        //TODO: Text가 여러 개 이므로, 어떻게 layout 구성할지 생각
        binding.textBtn.setOnClickListener{

            val textLinear = binding.textLinear

            // TODO: 이미 존재는하지만 hidden처리 되어있는 view의 속성을 변경
            //어떤 방법을 사용하던 어쨌든 이미지 크기 계산해서 width 조절 -> 이미지마다 위에 뜰 수 있도록!

            if (!isTxtBtnClicked) { // 클릭 안되어 있던 상태
                /* layout 변경 */
                it.setBackgroundResource(R.drawable.round_button)
                isTxtBtnClicked = true
                textLinear.visibility = View.VISIBLE

            }

            //TODO: FrameLayout에 동적으로 추가된 View 삭제 or FrameLayout에 view는 박아놓고 hidden 처리로 수행
            else { // 클릭 되어 있던 상태
                /* layout 변경 */
                it.background = ColorDrawable(Color.TRANSPARENT)
                isTxtBtnClicked = false
                textLinear.visibility = View.INVISIBLE
            }
        }

        binding.audioBtn.setOnClickListener{

            // TODO: 이미 존재는하지만 hidden처리 되어있는 view의 속성을 변경
            //어떤 방법을 사용하던 어쨌든 이미지 크기 계산해서 width 조절 -> 이미지마다 위에 뜰 수 있도록!

            if (!isAudioBtnClicked) { // 클릭 안되어 있던 상태
                /* layout 변경 */
                it.setBackgroundResource(R.drawable.round_button)
                isAudioBtnClicked = true

                // 오디오 재생
                jpegViewModel.jpegMCContainer.value!!.audioPlay()
            }

            //TODO: FrameLayout에 동적으로 추가된 View 삭제 or FrameLayout에 view는 박아놓고 hidden 처리로 수행
            else { // 클릭 되어 있던 상태
                /* layout 변경 */
                it.background = ColorDrawable(Color.TRANSPARENT)
                isAudioBtnClicked = false
            }
        }

        binding.magicBtn.setOnClickListener{

            // TODO: 이미 존재는하지만 hidden처리 되어있는 view의 속성을 변경
            //어떤 방법을 사용하던 어쨌든 이미지 크기 계산해서 width 조절 -> 이미지마다 위에 뜰 수 있도록!

            if (!isMagicBtnClicked) { // 클릭 안되어 있던 상태
                /* layout 변경 */
                it.setBackgroundResource(R.drawable.round_button)
                isMagicBtnClicked = true
                CoroutineScope(Dispatchers.Default).launch {
                    mainViewPagerAdapter.setImageContent(jpegViewModel.jpegMCContainer.value?.imageContent!!)
                    mainViewPagerAdapter.setCheckMagicPicturePlay(true)
                }
            }

            //TODO: FrameLayout에 동적으로 추가된 View 삭제 or FrameLayout에 view는 박아놓고 hidden 처리로 수행
            else { // 클릭 되어 있던 상태
                /* layout 변경 */
                it.background = ColorDrawable(Color.TRANSPARENT)
                isMagicBtnClicked = false
                mainViewPagerAdapter.setCheckMagicPicturePlay(false)
            }
        }

        binding.pullRightView.setOnClickListener {
            binding.scrollView.visibility = View.VISIBLE

//            val startPosition =  binding.filmCaseImageView.width - binding.scrollView.width
//            val endPoisition = binding.filmCaseImageView.width
//            val animator = ObjectAnimator.ofInt(binding.scrollView, "scrollX", startPosition, endPoisition)
//            animator.duration = 2000 // 애니메이션 지속 시간
//            animator.start()
//            it.visibility = View.INVISIBLE

            // 스크롤뷰가 왼쪽에서 오른쪽으로 스르륵 나오도록 애니메이션 효과를 적용합니다.
           // val animation = TranslateAnimation(-binding.scrollView.width.toFloat(), 0f, 0f, 0f)
            val startPosition =  binding.pullRightView.x - binding.scrollView.width
            val endPosition = binding.pullRightView.x


            val animation = TranslateAnimation(startPosition, endPosition,0f, 0f)
            animation.duration = 300
            it.visibility = View.INVISIBLE
            binding.scrollView.startAnimation(animation)

        }


        binding.editBtn.setOnClickListener{
            findNavController().navigate(R.id.action_viewerFragment_to_editFragment)
        }

        binding.backBtn.setOnClickListener{
            Glide.get(requireContext()).clearMemory()
            findNavController().navigate(R.id.action_viewerFragment_to_galleryFragment)
        }
    }


    /** MCContainer 변경 */
    @RequiresApi(Build.VERSION_CODES.Q)
     fun setCurrentMCContainer(position:Int){
        CoroutineScope(Dispatchers.IO).launch {
            Log.d("[ViewerFragment] 바뀐 position : ", ""+position)
            val sourcePhotoUri = jpegViewModel.imageUriLiveData.value!!.get(position)
            val iStream: InputStream? = requireContext().contentResolver.openInputStream(getUriFromPath(sourcePhotoUri))
            var sourceByteArray = getBytes(iStream!!)
            var jop = async {
                loadResolver.createMCContainer(jpegViewModel.jpegMCContainer.value!!,sourceByteArray, isContainerChanged) }
            jop.await()

        }
    }

    /** 숨겨진 이미지들 imageView로 ScrollView에 추가 */
    fun setCurrentOtherImage(){

        var pictureList = jpegViewModel.jpegMCContainer.value?.getPictureList()

        if (pictureList != null) {
            binding.linear.removeAllViews()
            Log.d("picture list size: ",""+pictureList.size)

            CoroutineScope(Dispatchers.IO).launch {
                for (picture in pictureList){
                    val pictureByteArr = jpegViewModel.jpegMCContainer.value?.imageContent?.getJpegBytes(picture)
                    // 넣고자 하는 layout 불러오기
                    val scollItemLayout = layoutInflater.inflate(R.layout.scroll_item_layout, null)

                    // 위 불러온 layout에서 변경을 할 view가져오기
                    val scrollImageView: ImageView = scollItemLayout.findViewById(R.id.scrollImageView)

                    CoroutineScope(Dispatchers.Main).launch {
                        // 이미지 바인딩
                        Glide.with(scrollImageView)
                            .load(pictureByteArr)
                            .into(scrollImageView)

                        scrollImageView.setOnClickListener{ // scrollview 이미지를 main으로 띄우기
                            mainViewPagerAdapter.setExternalImage(pictureByteArr!!)
                        }

                        binding.linear.addView(scollItemLayout)

                    }
                } // end of for..
            }
        }
    }

    @Throws(IOException::class)
    fun getBytes(inputStream: InputStream): ByteArray {
        val byteBuffer = ByteArrayOutputStream()
        val bufferSize = 1024
        val buffer = ByteArray(bufferSize)
        var len = 0
        while (inputStream.read(buffer).also { len = it } != -1) {
            byteBuffer.write(buffer, 0, len)
        }
        byteBuffer.close()
        inputStream.close()
        return byteBuffer.toByteArray()
    }

    @SuppressLint("Range")
    fun getUriFromPath(filePath: String): Uri { // filePath String to Uri
        val cursor = requireContext(). contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            null, "_data = '$filePath'", null, null)
        var uri: Uri
        if(cursor!=null) {
            cursor!!.moveToNext()
            val id = cursor.getInt(cursor.getColumnIndex("_id"))
            uri = ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                id.toLong()
            )
            cursor.close()
        }
        else {
            return Uri.parse("Invalid path")
        }
        return uri
    }

}