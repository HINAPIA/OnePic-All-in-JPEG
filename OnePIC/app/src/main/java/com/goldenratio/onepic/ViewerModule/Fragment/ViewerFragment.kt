package com.goldenratio.onepic.ViewerModule.Fragment

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.ShapeDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.bumptech.glide.Glide
import com.goldenratio.onepic.ImageToolModule
import com.goldenratio.onepic.JpegViewModel
import com.goldenratio.onepic.LoadModule.LoadResolver
import com.goldenratio.onepic.R
import com.goldenratio.onepic.ViewerModule.Adapter.ViewPagerAdapter
import com.goldenratio.onepic.databinding.FragmentViewerBinding
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

@SuppressLint("LongLogTag")
class ViewerFragment : Fragment() {

    private lateinit var binding: FragmentViewerBinding
    private val jpegViewModel by activityViewModels<JpegViewModel>()
    private var loadResolver : LoadResolver = LoadResolver()
    private lateinit var mainViewPagerAdapter: ViewPagerAdapter

    private var isContainerChanged = MutableLiveData<Boolean>()
    private var currentPosition:Int? = null // galery fragmentd 에서 넘어올 때
    private var isMainChanged:Boolean? = null
    private var isInitFocus:Boolean = true

    /* text, audio, magic 선택 여부 */
    private var isTxtBtnClicked = false
    private var isAudioBtnClicked = false
    private var isMagicBtnClicked = false

    companion object {
        var currentFilePath:String = ""
        var isFinished: MutableLiveData<Boolean> = MutableLiveData(false)
        var audioTopMargin = MutableLiveData<Int>()
        var audioEndMargin = MutableLiveData<Int>()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 상태바 색상 변경
        val window: Window = activity?.window
            ?: throw IllegalStateException("Fragment is not attached to an activity")
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.setStatusBarColor(ContextCompat.getColor(requireContext(), android.R.color.white))

        binding = FragmentViewerBinding.inflate(inflater, container, false)

        currentPosition = arguments?.getInt("currentPosition") // 갤러리 프래그먼트에서 넘어왔을 때

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

        if (currentFilePath != "" && currentFilePath != null) { // 편집창에서 저장하고 넘어왔을 때

            mainViewPagerAdapter.setUriList(jpegViewModel.imageUriLiveData.value!!)

            Log.d("songsong currentFIlePath: ", currentFilePath)
            mainViewPagerAdapter.viewHolder.bind(currentFilePath)

            //setCurrentItem(jpegViewModel.getFilePathIdx(currentFilePath)!!,false)

        }


        setCurrentOtherImage()
        binding.scrollView.visibility = View.VISIBLE

        //scrollAnimation()


        // gallery에 들어있는 사진이 변경되었을 때, 화면 다시 reload
        jpegViewModel.imageUriLiveData.observe(viewLifecycleOwner){

            mainViewPagerAdapter.setUriList(jpegViewModel.imageUriLiveData.value!!) // 새로운 데이터로 업데이트
            mainViewPagerAdapter.notifyDataSetChanged() // 데이터 변경 알림

            var position = jpegViewModel.getFilePathIdx(currentFilePath) // 기존에 보고 있던 화면 인덱스

            if (position != null){ // 사진이 갤러리에 존재하면
                binding.viewPager2.setCurrentItem(position,false) // 기존에 보고 있던 화면 유지
            }
            else {
                //TODO: 보고 있는 사진이 삭제된 경우

            }
        }
    }

    override fun onStop() {
        super.onStop()
        var currentPosition: Int = binding.viewPager2.currentItem
        currentFilePath = mainViewPagerAdapter.galleryMainimage[currentPosition]
    }

    /** ViewPager Adapter 및 swipe callback 설정 & Button 이벤트 처리 */
    @RequiresApi(Build.VERSION_CODES.M)
    fun init() {

        var container = jpegViewModel.jpegMCContainer.value!!

        /* Audio 버튼 UI - 있으면 표시, 없으면 GONE */
        if (container.audioContent.audio != null && container.audioContent.audio!!.size != 0) {
            binding.audioBtn.visibility = View.VISIBLE
            // margin 계산

        }
        else {
            binding.audioBtn.visibility = View.GONE
        }


        mainViewPagerAdapter = ViewPagerAdapter(requireContext())
        mainViewPagerAdapter.setUriList(jpegViewModel.imageUriLiveData.value!!)
        binding.viewPager2.setUserInputEnabled(false);

        binding.viewPager2.adapter = mainViewPagerAdapter
        binding.viewPager2.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            @RequiresApi(Build.VERSION_CODES.Q)
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                CoroutineScope(Dispatchers.Default).launch {
//                    setMagicPicture()
                }

                Log.d("[ViewerFragment] 바뀐 position : ", ""+position)
                mainViewPagerAdapter.notifyDataSetChanged()

                // 필름 스크롤뷰 초기화
                //binding.pullRightView.visibility = View.VISIBLE
                binding.scrollView.visibility = View.INVISIBLE//GONE


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
                    mainViewPagerAdapter.setCheckMagicPicturePlay(false, isFinished)

                }
                // setCurrentMCContainer(position)
            }
        })

        // MCContainer가 변경되었을 때(Page가 넘어갔을 때) 처리
        isContainerChanged.observe(requireActivity()){ value ->
            if (value == true){
                //setCurrentOtherImage()
                setMagicPicture()
                isFinished.value = true
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
                // 블러처리할 배경 색상값
                val blurColor = Color.parseColor("#80000000")

                // 배경 색상값을 포함한 ShapeDrawable 생성
                val shapeDrawable = ShapeDrawable()
                shapeDrawable.paint.color = blurColor

                // ColorFilter를 적용하여 블러처리 효과 적용
                val blurMaskFilter = BlurMaskFilter(10f, BlurMaskFilter.Blur.NORMAL)
                shapeDrawable.paint.maskFilter = blurMaskFilter
                shapeDrawable.alpha = 150

                // TextView에 배경 설정
                binding.savedTextView.background = shapeDrawable


                /* 텍스트 메시지 띄우기 */
                var textList = jpegViewModel.jpegMCContainer.value!!.textContent.textList
                if(textList != null && textList.size !=0){
                    binding.savedTextView.setText(textList.get(0).data)
                } else{
                    binding.savedTextView.setText("")
                }

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
                binding.audioBtn.setImageResource(R.drawable.mute2)
                isAudioBtnClicked = true
            }

            //TODO: FrameLayout에 동적으로 추가된 View 삭제 or FrameLayout에 view는 박아놓고 hidden 처리로 수행
            else { // 클릭 되어 있던 상태
                /* layout 변경 */
                binding.audioBtn.setImageResource(R.drawable.sound4)
                isAudioBtnClicked = false

                // 오디오 재생
                jpegViewModel.jpegMCContainer.value!!.audioPlay()
            }
        }



        audioTopMargin.observe(requireActivity()){ value ->

            val layoutParams = binding.audioBtn.layoutParams as ViewGroup.MarginLayoutParams
            val leftMarginInDp = 0 // 왼쪽 마진(dp)
            val topMarginInDp =  value// 위쪽 마진(dp)
            val rightMarginInDp = 20 // 오른쪽 마진(dp)
            val bottomMarginInDp = 0 // 아래쪽 마진(dp)

            layoutParams.setMargins(leftMarginInDp, topMarginInDp, rightMarginInDp, bottomMarginInDp)
            binding.audioBtn.layoutParams = layoutParams

        }

        audioEndMargin.observe(requireActivity()) {value ->

            val layoutParams = binding.audioBtn.layoutParams as ViewGroup.MarginLayoutParams
            val leftMarginInDp = 0 // 왼쪽 마진(dp)
            val topMarginInDp =  20// 위쪽 마진(dp)
            val rightMarginInDp = value // 오른쪽 마진(dp)
            val bottomMarginInDp = 0 // 아래쪽 마진(dp)

            layoutParams.setMargins(leftMarginInDp, topMarginInDp, rightMarginInDp, bottomMarginInDp)
            binding.audioBtn.layoutParams = layoutParams
        }


//        binding.pullRightView.setOnClickListener {
//
//            setCurrentOtherImage()
//
//            scrollAnimation()
//
//
////            binding.scrollView.visibility = View.VISIBLE
////
////            // 스크롤뷰가 왼쪽에서 오른쪽으로 스르륵 나오도록 애니메이션 효과를 적용
////            val startPosition =  binding.pullRightView.x - binding.scrollView.width - 10
////            val endPosition = 1.0F//binding.scrollView.x //binding.pullRightView.x
////
////            val animation = TranslateAnimation(startPosition, endPosition,0f, 0f)
////            animation.duration = 600
////            it.visibility = View.INVISIBLE
////            binding.scrollView.startAnimation(animation)
//
////            // 애니메이션 중 위치가 endPosition에 도달하면 애니메이션을 즉시 종료
////            binding.scrollView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
////                override fun onPreDraw(): Boolean {
////                    if (binding.scrollView.x == endPosition) {
////                        binding.scrollView.clearAnimation()
////                        binding.scrollView.viewTreeObserver.removeOnPreDrawListener(this)
////                        return false
////                    }
////                    return true
////                }
////            })
//        }

//        setMagicPicture()

        binding.editBtn.setOnClickListener{
            findNavController().navigate(R.id.action_viewerFragment_to_editFragment)
        }

        binding.backBtn.setOnClickListener{
            Glide.get(requireContext()).clearMemory()
            findNavController().navigate(R.id.action_viewerFragment_to_basicViewerFragment)
        }
    }

    fun scrollAnimation(){
        binding.scrollView.visibility = View.VISIBLE

        val startPosition = - binding.scrollView.width - 10
        val endPosition = 1.0F//binding.scrollView.x //binding.pullRightView.x

        val animation = TranslateAnimation(startPosition.toFloat(), endPosition,0f, 0f)
        animation.duration = 600
        binding.scrollView.startAnimation(animation)






        // 스크롤뷰가 왼쪽에서 오른쪽으로 스르륵 나오도록 애니메이션 효과를 적용
//        val startPosition =  binding.pullRightView.x - binding.scrollView.width - 10
//        val endPosition = 1.0F//binding.scrollView.x //binding.pullRightView.x
//
//        val animation = TranslateAnimation(startPosition, endPosition,0f, 0f)
//        animation.duration = 600
//        binding.pullRightView.visibility = View.INVISIBLE
//        binding.scrollView.startAnimation(animation)
    }

    fun setMagicPicture() {
        val imageContent = jpegViewModel.jpegMCContainer.value?.imageContent!!
        imageContent.resetMainBitmap()
        mainViewPagerAdapter.resetMagicPictureList()

//        while(!imageContent.checkPictureList){}

//        if(!imageContent.checkAttribute(ContentAttribute.magic)){
//            Log.d("magic 유무", "NO!!!!!!!!!!!")
//            ImageToolModule().showView(binding.magicBtn, false)
//        }
//        else {
        ImageToolModule().showView(binding.magicBtn, true)
        Log.d("magic 유무", "YES!!!!!!!!!!!")
        binding.magicBtn.setOnClickListener {

            // TODO: 이미 존재는하지만 hidden처리 되어있는 view의 속성을 변경
            //어떤 방법을 사용하던 어쨌든 이미지 크기 계산해서 width 조절 -> 이미지마다 위에 뜰 수 있도록!

            if (!isMagicBtnClicked) { // 클릭 안되어 있던 상태

                ImageToolModule().showView(binding.progressBar, true)

                /* layout 변경 */
                it.setBackgroundResource(R.drawable.round_button)
                isMagicBtnClicked = true
                CoroutineScope(Dispatchers.Default).launch {
                    mainViewPagerAdapter.setImageContent(jpegViewModel.jpegMCContainer.value?.imageContent!!)
                    mainViewPagerAdapter.setCheckMagicPicturePlay(true, isFinished)
                }
            }

            //TODO: FrameLayout에 동적으로 추가된 View 삭제 or FrameLayout에 view는 박아놓고 hidden 처리로 수행
            else { // 클릭 되어 있던 상태
                /* layout 변경 */
                it.background = ColorDrawable(Color.TRANSPARENT)
                isMagicBtnClicked = false
                mainViewPagerAdapter.setCheckMagicPicturePlay(false, isFinished)
            }
        }
        try {
            isFinished.observe(requireActivity()) { value ->
                if (value == true) {
                    ImageToolModule().showView(binding.progressBar, false)
                    isContainerChanged.value = false
                }
            }
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
//        }
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
            jpegViewModel.setCurrentImageFilePath(position) // edit 위한 처리
        }
    }

    /** 숨겨진 이미지들 imageView로 ScrollView에 추가 */
    fun setCurrentOtherImage(){

        var pictureList = jpegViewModel.jpegMCContainer.value?.getPictureList()

        binding.imageCntTextView.text = "담긴 사진 ${jpegViewModel.getPictureByteArrList().size}장"

        if (pictureList != null) {
            binding.linear.removeAllViews()
            Log.d("picture list size: ",""+pictureList.size)

            CoroutineScope(Dispatchers.IO).launch {

                val pictureByteArrList = jpegViewModel.getPictureByteArrList()
                for(i in 0..pictureList.size-1){

                    val picture = pictureList[i]
                    val pictureByteArr = pictureByteArrList[i]//jpegViewModel.jpegMCContainer.value?.imageContent?.getJpegBytes(picture)

                    // 넣고자 하는 layout 불러오기
                    try {
                        val scollItemLayout =
                            layoutInflater.inflate(R.layout.scroll_item_layout, null)

                        // 위 불러온 layout에서 변경을 할 view가져오기
                        val scrollImageView: ImageView =
                            scollItemLayout.findViewById(R.id.scrollImageView)

                        CoroutineScope(Dispatchers.Main).launch {
                            // 이미지 바인딩
                            Glide.with(scrollImageView)
                                .load(pictureByteArr)
                                .into(scrollImageView)


                            scrollImageView.isFocusable = true // 포커스를 받을 수 있도록 설정
                            scrollImageView.isFocusableInTouchMode = true // 터치 모드에서 포커스를 받을 수 있도록 설정

                            scrollImageView.onFocusChangeListener = View.OnFocusChangeListener { view, hasFocus ->
                                if (hasFocus) {
                                    // 포커스를 얻었을 때의 동작 처리
                                    scrollImageView.setBackgroundResource(R.drawable.chosen_image_border)
                                    scrollImageView.setPadding(6,6,6,6)
//                                    mainViewPagerAdapter.setExternalImage(pictureByteArr!!)
//                                    jpegViewModel.setselectedSubImage(picture)
                                } else {
                                    // 포커스를 잃었을 때의 동작 처리
                                    scrollImageView.background = null
                                    scrollImageView.setPadding(0,0,0,0)

                                }
                            }

                            scrollImageView.setOnClickListener { // scrollview 이미지를 main으로 띄우기
                                mainViewPagerAdapter.setExternalImage(pictureByteArr!!)
                                jpegViewModel.setselectedSubImage(picture)
                            }

                            scrollImageView.setOnTouchListener { _, event ->
                                when (event.action) {
                                    MotionEvent.ACTION_UP -> {
                                        scrollImageView.performClick() // 클릭 이벤트 강제로 발생
                                    }
                                }
                                false
                            }

                            binding.linear.addView(scollItemLayout)
                        }
                    } catch (e: IllegalStateException) {
                        println(e.message)
                    }
                }
                jpegViewModel.setselectedSubImage(pictureList[0]) // 초기 선택된 이미지는 Main으로 고정
            }
        }
    }


    // old version
//    fun setCurrentOtherImage(){
//
//        var pictureList = jpegViewModel.jpegMCContainer.value?.getPictureList()
//
//        if (pictureList != null) {
//            binding.linear.removeAllViews()
//            Log.d("picture list size: ",""+pictureList.size)
//
//            CoroutineScope(Dispatchers.IO).launch {
//                for (picture in pictureList){
//                    val pictureByteArr = jpegViewModel.jpegMCContainer.value?.imageContent?.getJpegBytes(picture)
//                    // 넣고자 하는 layout 불러오기
//                   try {
//                       val scollItemLayout =
//                           layoutInflater.inflate(R.layout.scroll_item_layout, null)
//
//                       // 위 불러온 layout에서 변경을 할 view가져오기
//                       val scrollImageView: ImageView =
//                           scollItemLayout.findViewById(R.id.scrollImageView)
//
//                       CoroutineScope(Dispatchers.Main).launch {
//                           // 이미지 바인딩
//                           Glide.with(scrollImageView)
//                               .load(pictureByteArr)
//                               .into(scrollImageView)
//
//                           scrollImageView.setOnClickListener { // scrollview 이미지를 main으로 띄우기
//                               mainViewPagerAdapter.setExternalImage(pictureByteArr!!)
//                               jpegViewModel.setselectedSubImage(picture)
//                           }
//                           binding.linear.addView(scollItemLayout)
//                       }
//                   } catch (e: IllegalStateException) {
//                       println(e.message)
//                   }
//                } // end of for..
//                jpegViewModel.setselectedSubImage(pictureList[0]) // 초기 선택된 이미지는 Main으로 고정
//                Log.d("초기 선택된 이미지: ",""+pictureList[0])
//            }
//        }
//    }

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