package com.goldenratio.onepic.ViewerModule.Fragment

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.bumptech.glide.Glide
import com.goldenratio.onepic.ImageToolModule
import com.goldenratio.onepic.JpegViewModel
import com.goldenratio.onepic.PictureModule.Contents.ContentAttribute
import com.goldenratio.onepic.PictureModule.Contents.Picture
import com.goldenratio.onepic.R
import com.goldenratio.onepic.ViewerModule.Adapter.ViewPagerAdapter
import com.goldenratio.onepic.databinding.FragmentViewerBinding
import kotlinx.coroutines.*

@SuppressLint("LongLogTag")
class ViewerFragment : Fragment() {

    /* layout 관련 변수 */
    private lateinit var context: Context
    private lateinit var binding: FragmentViewerBinding
    private lateinit var mainViewPagerAdapter: ViewPagerAdapter
    private val jpegViewModel by activityViewModels<JpegViewModel>()
    private var scrollAudioView: ImageView? = null
    private var scrollTextView: ImageView? = null
    private var imageTool = ImageToolModule()

    /* audio, magic, text 버튼 클릭 여부 */
    private var isAudioBtnClicked = false
    private var isMagicBtnClicked = false
    private var isTextBtnClicked = false

    private var currentPosition:Int? = null // AnalyzeFragment 에서 넘어올 때 받는 번들 값
    var pictureList : ArrayList<Picture> = arrayListOf()
    private var bitmapList = arrayListOf<Bitmap>() // seek bar 속도 개선위한 비트맵(스크롤뷰)
    private var firstImageView: ImageView? = null // 스크롤바 첫번째 이미지
    private var previousClickedItem:ImageView? = null //스크롤바에서 클릭한 아이템
    private lateinit var callback: OnBackPressedCallback // back 버튼 처리 콜백

    private var isEdited:Boolean = false // edit된 사진인지 여부
    companion object {

        var currentFilePath:String = "" // 현재 파일 path(or uri)

        /* 사진 및 오디오 상태 표시 */
        var isFinished: MutableLiveData<Boolean> = MutableLiveData(false) // 매직픽쳐 관련 작업 수행 완료
        var isEditStoraged:Boolean = false // 편집된 사진인지 여부 - 텍스트, 오디오 scrollView update
        var isAudioPlaying = MutableLiveData<Boolean>() // 오디오 재생중 표시

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = object : OnBackPressedCallback(true) { // 백버튼 처리 콜백
            override fun handleOnBackPressed() { backPressed() }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context = requireContext()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentViewerBinding.inflate(inflater, container, false)

        // 상태바 색상 변경
        val window: Window = activity?.window
            ?: throw IllegalStateException("Fragment is not attached to an activity")
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.setStatusBarColor(ContextCompat.getColor(requireContext(), android.R.color.white))

        currentPosition = arguments?.getInt("currentPosition") // Analyze Fragment에서 넘어왔을 때

        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /* 기본 UI & 헤더 버튼 이벤트 리스너 & viewPager 설정 */
        setMainViewPager()
        setViewerBasicUI()
        setHeaderBarEventListeners()


        /* GalleryFragment에서 넘어왔을 때 (선택된 이미지가 있음) */
        if(currentPosition != null){
            binding.viewPager2.setCurrentItem(currentPosition!!,false) // ViewPager 해당 위치로 이동
            currentPosition = null
        }

        /* 편집창에서 저장하고 넘어왔을 때 */
        if (isEditStoraged && currentFilePath != "" && currentFilePath != null) {
            isEdited = true // edit된 사진 체크
            mainViewPagerAdapter.setUriList(jpegViewModel.imageUriLiveData.value!!) // ViewPager Update

            /* 편집 후, 바로 편집된 이미지로 넘어감 */
            var path = currentFilePath
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) { // 13 버전 보다 낮을 경우 -> uri 를 filePath 로 변경
                path = imageTool.getFilePathFromUri(requireContext(),Uri.parse(currentFilePath)).toString()
            }

            binding.viewPager2.setCurrentItem(jpegViewModel.getFilePathIdx(path)!!,false)
            jpegViewModel.setCurrentImageUri(binding.viewPager2.currentItem)
            isEditStoraged = false // 초기화
        }

        setCurrentOtherImage() // 스크롤뷰 이미지 채우기

        // Gallery 변경이 있을 경우, 화면 다시 reload
        jpegViewModel.isGalleryUpdateFinished.observe(viewLifecycleOwner){ value ->
            if (value){ // 갤러리 업데이트가 되었다면
                mainViewPagerAdapter.setUriList(jpegViewModel.imageUriLiveData.value!!) // 새로운 데이터로 업데이트
                mainViewPagerAdapter.notifyDataSetChanged() // 데이터 변경 알림

                var position = jpegViewModel.getFilePathIdx(currentFilePath) // 기존에 보고 있던 화면 인덱스
                if (position != null){ //TODO: 보고 있었던 사진이 아직 존재하는 경우
                    binding.viewPager2.setCurrentItem(position,false) // 기존에 보고 있던 화면 유지
                }
                else if (currentFilePath != "" && !isEdited){ //TODO: 보고 있는 사진이 삭제된 경우 - 예외 처리
                    binding.imageNotFoundLinearLayout.visibility = View.VISIBLE
                    binding.entireLinearLayout.visibility = View.GONE
                    binding.editBtn.visibility = View.GONE
                    Glide.with(binding.deletedPhotoImageView)
                        .load(R.drawable.image_not_found)
                        .into(binding.deletedPhotoImageView)
                }
                else { //TODO: 보고 있었던 사진이 수정된 경우
                    isEdited = false // 초기화
                }
                jpegViewModel.isGalleryUpdateFinished.value = false
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        callback.remove()
    }

    override fun onStop() {
        super.onStop()
        var currentPosition: Int = binding.viewPager2.currentItem // 현재 파일 path or uri 저장해두기
        currentFilePath = mainViewPagerAdapter.galleryMainimage[currentPosition]
    }

    /** ViewPager Adapter 및 swipe callback 설정 */
    @RequiresApi(Build.VERSION_CODES.M)
    fun setMainViewPager() {

        mainViewPagerAdapter = ViewPagerAdapter(requireContext())
        mainViewPagerAdapter.setUriList(jpegViewModel.imageUriLiveData.value!!)
        binding.viewPager2.setUserInputEnabled(false);

        binding.viewPager2.adapter = mainViewPagerAdapter
        binding.viewPager2.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            @RequiresApi(Build.VERSION_CODES.Q)
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.viewPager2.post {
                    mainViewPagerAdapter.notifyDataSetChanged()
                }

                // 오디오 버튼 초기화
                if( isAudioBtnClicked && scrollAudioView != null) { // 클릭 되어 있던 상태
                    scrollAudioView!!.performClick()
                }

                // 매직 버튼 초기화
                if( isMagicBtnClicked ) { // 클릭 되어 있던 상태
                    binding.magicBtn.background = ColorDrawable(Color.TRANSPARENT)
                    isMagicBtnClicked = false
                    mainViewPagerAdapter.setCheckMagicPicturePlay(false, isFinished)
                }
            }
        })
    }


    /** 기본 UI 설정:
     * all in jpeg 로고 설정
     * seek bar & magic btn visibility 설정
     * savedText 데이터 & UI 설정 */
    fun setViewerBasicUI() {

        var container = jpegViewModel.jpegMCContainer.value!!

        // All in JPEG 로고 설정(일반 jpeg vs all in jpeg)
        binding.allInJpegTextView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() { // all in jpeg 로고 텍스트뷰의 로딩이 완료된 후에 호출될 작업 - 마진 조절
                val width = binding.allInJpegTextView.width
                val textViewlayoutParams = binding.allInJpegTextView.layoutParams as ViewGroup.MarginLayoutParams
                val leftMarginInDp = 0
                val topMarginInDp =  imageTool.spToDp(context,11f).toInt() //spToDp(context,11f)
                var rightMarginInDp = - imageTool.pxToDp(context,(width/2 - imageTool.spToDp(context,10f)).toFloat()).toInt() //왼쪽 마진(dp) //
                rightMarginInDp += imageTool.pxToDp(context,10f).toInt()
                val bottomMarginInDp = 0 // 아래쪽 마진(dp)

                textViewlayoutParams.setMargins(leftMarginInDp, topMarginInDp, rightMarginInDp, bottomMarginInDp)
                binding.allInJpegTextView.layoutParams = textViewlayoutParams

                // 작업을 수행한 후 리스너를 제거할 수도 있습니다.
                binding.allInJpegTextView.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })

        //  Text 있을 경우
        if (container.textContent.textCount != 0){
            var textList = jpegViewModel.jpegMCContainer.value!!.textContent.textList

            if(textList != null && textList.size !=0){
                val text = textList.get(0).data
                val shadowColor = Color.parseColor("#CAC6C6")// 그림자 색상
                val shadowDx = 5f // 그림자의 X 방향 오프셋
                val shadowDy = 0f // 그림자의 Y 방향 오프셋
                val shadowRadius = 3f // 그림자의 반경
                binding.savedTextView.setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor)
                binding.savedTextView.setText(text)
            }
        }

        // 매직 픽처인 경우 - 버튼 보이기
        if (jpegViewModel.jpegMCContainer.value!!.imageContent.checkAttribute(ContentAttribute.magic)) {
            setMagicPicture()
        }
        else {
            binding.magicBtnlinearLayout.visibility = View.GONE
        }

        // 거리별 초점 사진인 경우 - seekBar 보이기
        if (jpegViewModel.jpegMCContainer.value!!.imageContent.checkAttribute(ContentAttribute.distance_focus)){
            binding.seekBarLinearLayout.visibility = View.VISIBLE
        }
        else {
            binding.seekBarLinearLayout.visibility = View.GONE
        }

    }

    /** Header Btn 이벤트 리스너 설정: edit & back btn 리스너 설정 */
    fun setHeaderBarEventListeners(){
        binding.editBtn.setOnClickListener{

            CoroutineScope(Dispatchers.Main).launch {
                // 오디오 버튼 초기화
                if( isAudioBtnClicked && scrollAudioView != null ) { // 클릭 되어 있던 상태
                    scrollAudioView!!.performClick()
                }

                // 매직 버튼 초기화
                if( isMagicBtnClicked ) { // 클릭 되어 있던 상태
                    binding.magicBtn.background = ColorDrawable(Color.TRANSPARENT)
                    isMagicBtnClicked = false
                    mainViewPagerAdapter.setCheckMagicPicturePlay(false, isFinished)
                }

                findNavController().navigate(R.id.action_viewerFragment_to_editFragment)
            }

        }
        binding.backBtn.setOnClickListener{
            CoroutineScope(Dispatchers.Main).launch {
                backPressed()
            }
        }
    }

    fun setMagicPicture() {

        val imageContent = jpegViewModel.jpegMCContainer.value?.imageContent!!
        imageContent.setMainBitmap(null)
        mainViewPagerAdapter.resetMagicPictureList()

        imageTool.showView(binding.magicBtn, true)
        Log.d("magic 유무", "YES!!!!!!!!!!!")
        binding.magicBtn.setOnClickListener {

            // TODO: 이미 존재는하지만 hidden처리 되어있는 view의 속성을 변경
            //어떤 방법을 사용하던 어쨌든 이미지 크기 계산해서 width 조절 -> 이미지마다 위에 뜰 수 있도록!

            if (!isMagicBtnClicked) { // 클릭 안되어 있던 상태

                imageTool.showView(binding.progressBar, true)

                CoroutineScope(Dispatchers.Main).launch {
                    /* layout 변경 */
                    binding.magicBtn.setImageResource(R.drawable.edit_magic_ing_icon)
                    isMagicBtnClicked = true
                    mainViewPagerAdapter.setImageContent(jpegViewModel.jpegMCContainer.value?.imageContent!!)
                    mainViewPagerAdapter.setCheckMagicPicturePlay(true, isFinished)
                }

            }
            //TODO: FrameLayout에 동적으로 추가된 View 삭제 or FrameLayout에 view는 박아놓고 hidden 처리로 수행
            else { // 클릭 되어 있던 상태
                /* layout 변경 */
                binding.magicBtn.setImageResource(R.drawable.edit_magic_icon)
                isMagicBtnClicked = false
                mainViewPagerAdapter.setCheckMagicPicturePlay(false, isFinished)
            }
        }
        try {
            isFinished.observe(requireActivity()) { value ->
                if (value == true) {
                    imageTool.showView(binding.progressBar, false)
                }
            }
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
//        }
    }


    /** 숨겨진 이미지들 imageView로 ScrollView에 추가 */
    @SuppressLint("MissingInflatedId")
    @RequiresApi(Build.VERSION_CODES.M)
    fun setCurrentOtherImage() {

        pictureList = jpegViewModel.jpegMCContainer.value?.getPictureList()!!
        binding.imageCntTextView.text = "담긴 사진 ${jpegViewModel.getPictureByteArrList().size}장"


        // bitmap list (seek bar 속도 개선)
        val imageContent = jpegViewModel.jpegMCContainer.value?.imageContent!!
        CoroutineScope(Dispatchers.Default).launch {
            while (!imageContent.checkPictureList) {
            }

            val bitmap = imageContent.getBitmapList()
            if (bitmap != null)
                bitmapList = bitmap
        }

        if (pictureList != null) {

            if (jpegViewModel.getPictureByteArrList().size != 1) {
                setSeekBar(pictureList)
            }
            binding.linear.removeAllViews() // 뷰 초기화

            CoroutineScope(Dispatchers.IO).launch {

                val pictureByteArrList = jpegViewModel.getPictureByteArrList()
                for (i in 0..pictureList.size - 1) {
                    val picture = pictureList[i]
                    val pictureByteArr = pictureByteArrList[i]

                    // 넣고자 하는 layout 불러오기
                    try {
                        val scollItemLayout =
                            layoutInflater.inflate(R.layout.scroll_item_layout, null)

                        // 위 불러온 layout에서 변경을 할 view가져오기
                        val scrollImageView: ImageView =
                            scollItemLayout.findViewById(R.id.scrollImageView)

                        var mainMark: TextView = scollItemLayout.findViewById(R.id.mainMark)
                        if (i == 0) {
                            mainMark.visibility = View.VISIBLE
                            firstImageView = scrollImageView
                        }

                        CoroutineScope(Dispatchers.Main).launch {
                            // 이미지 바인딩
                            Glide.with(scrollImageView)
                                .load(pictureByteArr)
                                .into(scrollImageView)

                            scrollImageView.setOnClickListener { // scrollview 이미지를 main으로 띄우기
                                if (previousClickedItem != scrollImageView) {
                                    if (binding.seekBar.visibility == View.VISIBLE) {
                                        binding.seekBar.progress = i
                                    }
                                    CoroutineScope(Dispatchers.Main).launch {
                                        mainViewPagerAdapter.setExternalImage(pictureByteArr!!)
                                    }
                                    jpegViewModel.setselectedSubImage(picture)
                                    changeImageView(i,scrollImageView)
                                }
                            }
                            binding.linear.addView(scollItemLayout)
                            firstImageView!!.performClick()
                        }
                    } catch (e: IllegalStateException) {
                        println(e.message)
                    }
                } // end of for..

                var container = jpegViewModel.jpegMCContainer.value!!

                // 오디오 있는 경우
                if (container.audioContent.audio != null) {
                    CoroutineScope(Dispatchers.Main).launch {
                        var currText = binding.imageCntTextView.text
                        binding.imageCntTextView.text = "${currText} + 오디오"
                        val scollItemLayout =
                            layoutInflater.inflate(R.layout.scroll_item_menu, null)
                        // 위 불러온 layout에서 변경을 할 view가져오기
                        scrollAudioView = scollItemLayout.findViewById(R.id.scrollItemMenuView)
                        scrollAudioView!!.setImageResource(R.drawable.audio_item)
                        scrollAudioView!!.setOnClickListener { // scrollview 이미지를 main으로 띄우기
                            if (!isAudioBtnClicked) { // 클릭 안되어있던 상태
                                // TODO: 음악 재생
                                isAudioBtnClicked = true
                                if (isAudioPlaying.value != true) {
                                    jpegViewModel.jpegMCContainer.value!!.audioPlay()
                                    isAudioPlaying.value = true
                                }
                            } else {
                                // TODO: 음악 멈춤
                                isAudioBtnClicked = false
                                jpegViewModel.jpegMCContainer.value!!.audioStop()
                                isAudioPlaying.value = false
                            }
                        }

                        isAudioPlaying.observe(viewLifecycleOwner) { value ->
                            if (value == false) {
                                val paddingInDp = resources.getDimensionPixelSize(R.dimen.audio_item_padding)
                                scrollAudioView!!.setBackgroundResource(R.drawable.scroll_menu_btn)
                                scrollAudioView!!.setImageResource(R.drawable.audio_item)
                                scrollAudioView!!.setPadding(paddingInDp, paddingInDp, paddingInDp, paddingInDp)
                                isAudioBtnClicked = false

                            } else {
                                Log.d("song music: ", "음악 재생")
                                scrollAudioView!!.setBackgroundResource(R.drawable.scroll_menu_btn_color)
                                scrollAudioView!!.setPadding(0, 0, 0, 0)
                                Glide.with(scrollAudioView!!)
                                    .load(R.raw.giphy)
                                    .into(scrollAudioView!!)
                            }
                        }
                        binding.linear.addView(scollItemLayout, binding.linear.childCount)
                    }
                }
                // 텍스트 있는 경우
                if (container.textContent.textCount != 0) {

                    CoroutineScope(Dispatchers.Main).launch {
                        var currText = binding.imageCntTextView.text
                        binding.imageCntTextView.text = "${currText} + 텍스트"

                        val scollItemLayout =
                            layoutInflater.inflate(R.layout.scroll_item_menu, null)
                        // 위 불러온 layout에서 변경을 할 view가져오기
                        scrollTextView = scollItemLayout.findViewById(R.id.scrollItemMenuView)
                        scrollTextView!!.setImageResource(R.drawable.text_item)
                        val paddingInDp = resources.getDimensionPixelSize(R.dimen.text_item_padding)
                        scrollTextView!!.setPadding(paddingInDp,paddingInDp,paddingInDp,paddingInDp)
                        scrollTextView!!.setOnClickListener { // scrollview 이미지를 main으로 띄우기
                            if (!isTextBtnClicked) { // 클릭 안되어있던 상태
                                scrollTextView!!.setBackgroundResource(R.drawable.scroll_menu_btn_color)
                                scrollTextView!!.setImageResource(R.drawable.text_item_color)
                                binding.savedTextView.visibility = View.VISIBLE
                                isTextBtnClicked = true
                            } else {
                                scrollTextView!!.setBackgroundResource(R.drawable.scroll_menu_btn)
                                scrollTextView!!.setImageResource(R.drawable.text_item)
                                binding.savedTextView.visibility = View.INVISIBLE
                                isTextBtnClicked = false
                            }
                        }

                        binding.linear.addView(scollItemLayout, binding.linear.childCount)
                    }
                }

            }

        }

    }


    /** Seek Bar 설정 */
    fun setSeekBar(pictureList:ArrayList<Picture>){

        /* seekBar 처리 - 스크롤뷰 아이템 개수에 따라, progress와 max 지정 */
        if (binding.seekBar.visibility == View.VISIBLE) {
            binding.seekBar.progress = 0
            binding.seekBar.max = jpegViewModel.getPictureByteArrList().size - 1
            binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) { // 시크바의 값이 변경될 때 호출되는 메서드
                    jpegViewModel.setselectedSubImage(pictureList[progress])
                    if (bitmapList.size >= progress + 1) { // bitmap으로 사진 띄우기
                        mainViewPagerAdapter.setExternalImageBitmap(bitmapList[progress])
                    } else {
                        // 비트맵은 따로 만들고 있고 해당 index의 비트맵이 안만들어졌으면 글라이드로
                        CoroutineScope(Dispatchers.Main).launch {
                            mainViewPagerAdapter.setExternalImage(jpegViewModel.getPictureByteArrList()[progress])
                        }
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar) { }
                override fun onStopTrackingTouch(seekBar: SeekBar) { }
            })
        }
    }


    fun changeImageView(index: Int, imageView: ImageView) {

        previousClickedItem?.background = null
        previousClickedItem?.setPadding(0)
        previousClickedItem = imageView
        imageView.setBackgroundResource(R.drawable.chosen_image_border)
        imageView.setPadding(6)

        if(pictureList[index].contentAttribute == ContentAttribute.magic) {
            imageTool.showView(binding.magicBtnlinearLayout, true)
        }
        else {
            if( isMagicBtnClicked ) { // 클릭 되어 있던 상태
                binding.magicBtn.performClick()
            }
            imageTool.showView(binding.magicBtnlinearLayout, false)
        }
    }

    /** back btn 눌렀을 때 처리 */
    fun backPressed(){
        Glide.get(requireContext()).clearMemory()  // Glide 메모리 비우기
        if( isAudioBtnClicked && scrollAudioView != null ) { // 오디오 버튼 초기화
            scrollAudioView!!.performClick()
        }
        if( isMagicBtnClicked ) { // 매직 버튼 초기화
            binding.magicBtn.performClick()
        }
        val bundle = Bundle()
        bundle.putInt("currentPosition",binding.viewPager2.currentItem)
        findNavController().navigate(R.id.action_viewerFragment_to_basicViewerFragment,bundle)
    }

}