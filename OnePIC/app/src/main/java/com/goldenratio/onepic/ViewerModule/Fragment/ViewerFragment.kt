package com.goldenratio.onepic.ViewerModule.Fragment

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
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
import com.bumptech.glide.Glide
import com.goldenratio.onepic.ImageToolModule
import com.goldenratio.onepic.JpegViewModel
import com.goldenratio.onepic.AllinJPEGModule.Content.ContentAttribute
import com.goldenratio.onepic.AllinJPEGModule.Content.Picture
import com.goldenratio.onepic.AllinJPEGModule.AiContainer
import com.goldenratio.onepic.EditModule.MagicPictureModule
import com.goldenratio.onepic.R
import com.goldenratio.onepic.databinding.FragmentViewerBinding
import kotlinx.coroutines.*

@SuppressLint("LongLogTag")
class ViewerFragment : Fragment() {

    /* layout 관련 변수 */
    private lateinit var context: Context
    private lateinit var binding: FragmentViewerBinding
    private val jpegViewModel by activityViewModels<JpegViewModel>()
    private var scrollAudioView: ImageView? = null
    private var scrollTextView: ImageView? = null
    private var imageTool = ImageToolModule()

    /* audio, magic, text 버튼 클릭 여부 */
    private var isAudioBtnClicked = false
    private var isMagicBtnClicked = false
    private var isTextBtnClicked = false

    var currentPosition:Int? = null // AnalyzeFragment 에서 넘어올 때 받는 번들 값
    var pictureList : ArrayList<Picture> = arrayListOf()
    private var bitmapList = arrayListOf<Bitmap>() // seek bar 속도 개선위한 비트맵(스크롤뷰)
    private var firstImageView: ImageView? = null // 스크롤바 첫번째 이미지
    private var previousClickedItem:ImageView? = null //스크롤바에서 클릭한 아이템
    private lateinit var callback: OnBackPressedCallback // back 버튼 처리 콜백

    private var isEdited:Boolean = false // edit된 사진인지 여부
    private var isDeleted:Boolean = false // 삭제된 사진인지 여부
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
        setViewerBasicUI()
        setHeaderBarEventListeners()

        /* GalleryFragment에서 넘어왔을 때 (선택된 이미지가 있음) */
        if(currentPosition != null){
            setMainImage()
            //currentPosition = null
        }

        /* 편집창에서 저장하고 넘어왔을 때 */
        if (isEditStoraged && currentFilePath != "" && currentFilePath != null) {
            isEdited = true // edit된 사진 체크
            //mainViewPagerAdapter.setUriList(jpegViewModel.imageUriLiveData.value!!) // ViewPager Update

            /* 편집 후, 바로 편집된 이미지로 넘어감 */
            var path = currentFilePath
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) { // 13 버전 보다 낮을 경우 -> uri 를 filePath 로 변경
                path = imageTool.getFilePathFromUri(requireContext(),Uri.parse(currentFilePath)).toString()
            }
            currentPosition = jpegViewModel.getFilePathIdx(path)

            isEditStoraged = false // 초기화
        }

        setCurrentOtherImage() // 스크롤뷰 이미지 채우기

        // Gallery 변경이 있을 경우, 화면 다시 reload
        jpegViewModel.isGalleryUpdateFinished.observe(viewLifecycleOwner){ value ->
            if (value){ // 갤러리 업데이트가 되었다면

                var position = jpegViewModel.getFilePathIdx(currentFilePath) // 기존에 보고 있던 화면 인덱스
                if (position != null){ //TODO: 보고 있었던 사진이 아직 존재하는 경우

                }
                else if (currentFilePath != "" && !isEdited){ //TODO: 보고 있는 사진이 삭제된 경우 - 예외 처리
                    binding.imageNotFoundLinearLayout.visibility = View.VISIBLE
                    binding.entireLinearLayout.visibility = View.GONE
                    binding.editBtn.visibility = View.GONE
                    Glide.with(binding.deletedPhotoImageView)
                        .load(R.drawable.image_not_found)
                        .into(binding.deletedPhotoImageView)
                    isDeleted = true
                    currentPosition = 0//null
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

    /**
     * All-in JPEG 대표 이미지 출력
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun setMainImage() {
        var value = jpegViewModel.imageUriLiveData.value!!.get(currentPosition!!)
        currentFilePath = value
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            Glide.with(this)
                .load(value)
                .into(binding.mainView)
        }
        else {
            Glide.with(this)
                .load(imageTool.getUriFromPath(this.requireContext(),value))
                .into(binding.mainView)
        }
    }


    /** 기본 UI 설정:
     * all in jpeg 로고 설정
     * seek bar & magic btn visibility 설정
     * savedText 데이터 & UI 설정 */
    fun setViewerBasicUI() {
        var container = jpegViewModel.jpegAiContainer.value!!
        val isAiJPEG = isAllInJPEG(container)

        // All in JPEG 로고 설정(일반 jpeg vs all in jpeg)
        binding.allInJpegTextView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() { // all in jpeg 로고 텍스트뷰의 로딩이 완료된 후에 호출될 작업 - 마진 조절
                val textViewlayoutParams = binding.allInJpegTextView.layoutParams as ViewGroup.MarginLayoutParams
                var marginEndInDp = 0
                var marginTopInDp = 0
                if (isAiJPEG) {
                    binding.allInJpegTextView.text = "All in JPEG"
                    marginEndInDp = resources.getDimensionPixelSize(R.dimen.info_marker_end_margin)
                    marginTopInDp = resources.getDimensionPixelSize(R.dimen.info_marker_top_margin)
                    textViewlayoutParams.setMargins(0, marginTopInDp, marginEndInDp, 0)

                    binding.allInJpegTextView.layoutParams = textViewlayoutParams

                }
                else {
                    binding.containerImageView.visibility = View.GONE
                    binding.corner.visibility = View.GONE
                    binding.allInJpegTextView.visibility = View.GONE
                }
                binding.allInJpegTextView.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })

        binding.infoMarkerTextView.apply { // info marker text 변경
            val currText = this.text
            if (isAiJPEG) {
                this.text = "All in JPEG ${currText}"
            }
            else {
                this.text = "JPEG ${currText}"
            }
        }

        if (container.textContent.textCount != 0){//  Text 있을 경우
            var textList = jpegViewModel.jpegAiContainer.value!!.textContent.textList

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

        if (container.imageContent.checkAttribute(ContentAttribute.magic)) {// 매직 픽처인 경우
            setMagicPicture()
        }
        else {
            binding.magicBtnlinearLayout.visibility = View.GONE
        }

        // 거리별 초점 사진인 경우 - seekBar 보이기
        if (container.imageContent.checkAttribute(ContentAttribute.distance_focus)){
            binding.seekBarLinearLayout.visibility = View.VISIBLE
        }
        else {
            binding.seekBarLinearLayout.visibility = View.GONE
        }

    }

    /**
     * Header Btn 이벤트 리스너 설정: edit & back btn 리스너 설정
     */
    fun setHeaderBarEventListeners(){
        binding.editBtn.setOnClickListener{
            CoroutineScope(Dispatchers.Main).launch {
                // 오디오 버튼 초기화
                if( isAudioBtnClicked && scrollAudioView != null ) { // 클릭 되어 있던 상태
                    scrollAudioView!!.performClick()
                }

                // 매직 버튼 초기화
                if( isMagicBtnClicked ) { // 클릭 되어 있던 상태
                    binding.magicBtn.performClick()
                    isMagicBtnClicked = false
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

    /**
     * All-in JPEG 인지 일반 JPEG인 지 판단
     *
     * @param container All-in JPEG 컨테이너
     * @return All-in JPEG 인지 여부
     */
    fun isAllInJPEG(container: AiContainer):Boolean{
        if (jpegViewModel.getPictureByteArrList().size != 1 || container.textContent.textCount != 0 || container.audioContent.audio != null){
            return true
        }
        return false
    }

    fun setMagicPicture() {
        val imageContent = jpegViewModel.jpegAiContainer.value?.imageContent!!
        imageContent.setMainBitmap(null)

        imageTool.showView(binding.magicBtn, true)
        binding.magicBtn.setOnClickListener {
            playMagicPicture()
        }
        try {
            isFinished.observe(requireActivity()) { value ->
                if (value == true) {
                    imageTool.showView(binding.progressBar, false)
                    isFinished.value = false
                }
            }
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }


    private var isMagicPlay: Boolean = false
    private var overlayBitmap = arrayListOf<Bitmap>()
    private var magicPictureModule: MagicPictureModule? = null
    val handler = Handler()
    var magicPlaySpeed: Long = 150

    /**
     * 매직픽처 재생
     */
    private fun playMagicPicture() {
        if (!isMagicPlay) {
            imageTool.showView(binding.progressBar, true)
            CoroutineScope(Dispatchers.Main).launch {
                binding.magicBtn.setImageResource(R.drawable.edit_magic_ing_icon)
            }

            isMagicPlay = true

            CoroutineScope(Dispatchers.Default).launch {
                if (overlayBitmap.isEmpty() ) {
                    while(magicPictureModule == null ) {}
                    overlayBitmap = magicPictureModule!!.magicPictureProcessing()
                }
                Log.d("magic", "magicPictureProcessing end ${overlayBitmap.size}")
                imageTool.showView(binding.progressBar, false)
                Log.d("magic", "magicPucture run ${overlayBitmap.size}")
                magicPictureRun(overlayBitmap)
            }
        }
        else {
            handler.removeCallbacksAndMessages(null)
            CoroutineScope(Dispatchers.Main).launch {
                binding.magicBtn.setImageResource(R.drawable.edit_magic_icon)
                imageTool.showView(binding.progressBar, false)
            }
            isMagicPlay = false
        }
    }

    /**
     * 매직픽처를 재생한다.
     *
     * @param overlapBitmap 재생에 필요한 [Bitmap]
     */
    private fun magicPictureRun(overlapBitmap: java.util.ArrayList<Bitmap>) {
        CoroutineScope(Dispatchers.Default).launch {

            var currentImageIndex = 0
            var increaseIndex = 1

            val runnable = object : java.lang.Runnable {
                override fun run() {
                    if (overlapBitmap.size > 0) {
                        binding.magicView.setImageBitmap(overlapBitmap[currentImageIndex])
                        currentImageIndex += increaseIndex

                        if (currentImageIndex >= overlapBitmap.size - 1) {
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



    /**
     * All-in JPEG 내부의 이미지들 추출 및 레이아웃 설정
     * - imageView로 ScrollView에 추가
     */
    @SuppressLint("MissingInflatedId")
    @RequiresApi(Build.VERSION_CODES.M)
    fun setCurrentOtherImage() {
        pictureList = jpegViewModel.jpegAiContainer.value?.getPictureList()!!
        binding.imageCntTextView.text = "담긴 사진 ${jpegViewModel.getPictureByteArrList().size}장"

        // bitmap list (seek bar 속도 개선)
        val imageContent = jpegViewModel.jpegAiContainer.value?.imageContent!!
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

                    // 넣고자 하는 layout 불러오기
                    try {
                        val pictureByteArr = pictureByteArrList[i]
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
                                        Glide.with(context)
                                            .load(pictureByteArr!!)
                                            .into(binding.mainView)
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
                    } catch (e : IndexOutOfBoundsException){
                        println(e.message)
                    }
                } // end of for..

                var container = jpegViewModel.jpegAiContainer.value!!

                // 오디오 있는 경우
                if (container.audioContent.audio != null) {
                    setAudioContent()
                }
                // 텍스트 있는 경우
                if (container.textContent.textCount != 0) {
                    setTextContent()
                }
            }
        }
    }


    /**
     * All-in JPEG 내부의 오디오 추출 및 레이아웃
     * - 재생, 멈춤 처리
     * - 사용자 클릭 상태에대한 레이아웃 변경
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun setAudioContent(){
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
                        jpegViewModel.jpegAiContainer.value!!.audioPlay()
                        isAudioPlaying.value = true
                    }
                } else {
                    // TODO: 음악 멈춤
                    isAudioBtnClicked = false
                    jpegViewModel.jpegAiContainer.value!!.audioStop()
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

    /**
     * All-in JPEG 속 텍스트 추출 및 레이아웃 설정
     */
    fun setTextContent(){
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

    /**
     * Seek Bar 설정
     *
     * @param pictureList seek bar 움직임에 따라 보여줄 이미지 리스트
     */
    fun setSeekBar(pictureList:ArrayList<Picture>){

        /* seekBar 처리 - 스크롤뷰 아이템 개수에 따라, progress와 max 지정 */
        if (binding.seekBar.visibility == View.VISIBLE) {
            binding.seekBar.progress = 0
            binding.seekBar.max = jpegViewModel.getPictureByteArrList().size - 1
            binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) { // 시크바의 값이 변경될 때 호출되는 메서드
                    jpegViewModel.setselectedSubImage(pictureList[progress])
                    if (bitmapList.size >= progress + 1) { // bitmap으로 사진 띄우기
                        binding.mainView.setImageBitmap(bitmapList[progress])
                    } else {
                        // 비트맵은 따로 만들고 있고 해당 index의 비트맵이 안만들어졌으면 글라이드로
                        CoroutineScope(Dispatchers.Main).launch {
                            Glide.with(context)
                                .load(jpegViewModel.getPictureByteArrList()[progress])
                                .into(binding.mainView)
                        }
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar) { }
                override fun onStopTrackingTouch(seekBar: SeekBar) { }
            })
        }
    }

    /**
     * 클릭된 이미지 레이아웃 처리
     *
     * @param index 선택된 이미지의 인덱스
     * @param imageView 레이아웃 변경할 이미지뷰
     */
    fun changeImageView(index: Int, imageView: ImageView) {

        previousClickedItem?.background = null
        previousClickedItem?.setPadding(0)
        previousClickedItem = imageView
        imageView.setBackgroundResource(R.drawable.chosen_image_border)
        imageView.setPadding(6)

        if(pictureList[index].contentAttribute == ContentAttribute.magic) { // 매직픽처 사진일 때
            val imageContent = jpegViewModel.jpegAiContainer.value!!.imageContent
            binding.magicView.visibility = View.VISIBLE

            Glide.with(binding.magicView)
                .load(imageContent.getJpegBytes(pictureList[index]))
                .into(binding.magicView)

            imageTool.showView(binding.magicBtnlinearLayout, true)

            CoroutineScope(Dispatchers.IO).launch {
                magicPictureModule =MagicPictureModule(imageContent, pictureList[index])
            }
        }
        else {
            binding.magicView.visibility = View.GONE
            binding.magicView.setImageBitmap(null)
            if( isMagicBtnClicked ) { // 클릭 되어 있던 상태
                binding.magicBtn.performClick()
            }
            imageTool.showView(binding.magicBtnlinearLayout, false)
        }
    }

    /**
     * Back btn 눌렀을 때 처리
     * - 오디오 & 매직픽처 재생 초기화
     * - 메모리 할당 해제
     */
    fun backPressed() {
        // 제거 및 할당 해제
        CoroutineScope(Dispatchers.Main).launch {
            binding.linear.removeAllViews()
        }

        jpegViewModel.jpegAiContainer.value!!.imageContent.resetBitmap()

        jpegViewModel.clearPictureByteArrList()
        Glide.get(context).clearMemory()
        GlobalScope.launch(Dispatchers.IO) {// Glide 디스크 캐시 해제
            Glide.get(context).clearDiskCache()
        }

        if (isAudioBtnClicked && scrollAudioView != null) { // 오디오 버튼 초기화
            scrollAudioView!!.performClick()
        }
        if (isMagicBtnClicked) { // 매직 버튼 초기화
            binding.magicBtn.performClick()
        }

        if (!isDeleted && currentPosition == null) { // 삭제된 사진이 아닌 경우
            currentPosition = jpegViewModel.getFilePathIdx(currentFilePath)
        }

        val bundle = Bundle()
        bundle.putInt("currentPosition", currentPosition!!)
        findNavController().navigate(R.id.action_viewerFragment_to_galleryFragment, bundle)

        parentFragmentManager.beginTransaction()
            .remove(this)
            .commit()
    }
}