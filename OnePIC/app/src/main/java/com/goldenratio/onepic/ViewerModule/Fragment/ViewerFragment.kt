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
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.bumptech.glide.Glide
import com.goldenratio.onepic.ImageToolModule
import com.goldenratio.onepic.JpegViewModel
import com.goldenratio.onepic.PictureModule.Contents.ContentAttribute
import com.goldenratio.onepic.R
import com.goldenratio.onepic.ViewerModule.Adapter.ViewPagerAdapter

import com.goldenratio.onepic.databinding.FragmentViewerBinding
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

@SuppressLint("LongLogTag")
class ViewerFragment : Fragment() {

    private lateinit var binding: FragmentViewerBinding//FragmentViewerBinding
    private lateinit var mainViewPagerAdapter: ViewPagerAdapter
    private val jpegViewModel by activityViewModels<JpegViewModel>()

    private lateinit var context: Context
    private lateinit var callback: OnBackPressedCallback // back 버튼 처리 콜백
    private var currentPosition:Int? = null // galㅣery fragmentd 에서 넘어올 때
    private var bitmapList = arrayListOf<Bitmap>()

    /* audio, magic 클릭 여부 */
    private var isAudioBtnClicked = false
    private var isMagicBtnClicked = false

    /* 스크롤바 클릭 아이템 */
    private var previousClickedItem:ImageView? = null

    companion object {

        var currentFilePath:String = "" // 현재 파일 path(or uri)

        /* 사진 및 오디오 상태 표시 */
        var isFinished: MutableLiveData<Boolean> = MutableLiveData(false) // 매직픽쳐 관련 작업 수행 완료
        var isEditStoraged:Boolean = false // 편집된 사진인지 여부 - 텍스트, 오디오 scrollView update
        var isAudioPlaying = MutableLiveData<Boolean>() // 오디오 재생중 표시

        /* 동적 margin 설정 변수 */
        var audioTopMargin = MutableLiveData<Int>() // 오디오 버튼 top margin
        var audioEndMargin = MutableLiveData<Int>() // 오디오 버튼 end margin
        var seekBarMargin = MutableLiveData<Int>() // seek bar margin
        var isFocusingChange = false
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = object : OnBackPressedCallback(true) { // 백버튼 처리 콜백
            override fun handleOnBackPressed() {
                backPressed()
            }
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

        if (isEditStoraged && currentFilePath != "" && currentFilePath != null) { // 편집창에서 저장하고 넘어왔을 때

            isEditStoraged = false // 초기화
            mainViewPagerAdapter.setUriList(jpegViewModel.imageUriLiveData.value!!) // ViewPager Update

            /* 편집 후, 바로 편집된 이미지로 넘어감 */
            var path = currentFilePath
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) { // 13 버전 보다 낮을 경우 -> uri 를 filePath 로 변경
                path = getFilePathFromUri(requireContext(),Uri.parse(currentFilePath)).toString()
            }

            binding.viewPager2.setCurrentItem(jpegViewModel.getFilePathIdx(path)!!,false)
            jpegViewModel.setCurrentImageUri(binding.viewPager2.currentItem)
        }

        setCurrentOtherImage() // 스크롤뷰 이미지 채우기


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

    override fun onDetach() {
        super.onDetach()
        callback.remove()
    }


    override fun onStop() {
        super.onStop()
        var currentPosition: Int = binding.viewPager2.currentItem // 현재 파일 path or uri 저장해두기
        currentFilePath = mainViewPagerAdapter.galleryMainimage[currentPosition]
    }

    /** ViewPager Adapter 및 swipe callback 설정 & Button 이벤트 처리 */
    @RequiresApi(Build.VERSION_CODES.M)
    fun init() {

        var container = jpegViewModel.jpegMCContainer.value!!

        binding.viewPager2.setOnClickListener {

            if (container.textContent.textCount != 0){ // text가 존재할 때
                if (binding.savedTextView.visibility == View.VISIBLE){
                    binding.savedTextView.visibility = View.INVISIBLE
                }
                else {
                    binding.savedTextView.visibility = View.VISIBLE
                }
            }
        }

        binding.savedTextView.setOnClickListener { // Text view 클릭했을 때
            if (binding.savedTextView.text != null && binding.savedTextView.text != ""){
                if (it.visibility == View.VISIBLE){
                    it.visibility = View.INVISIBLE
                }
                else {
                    it.visibility = View.VISIBLE
                }
            }
        }

        binding.allInJpegTextView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() { // all in jpeg 로고 텍스트뷰의 로딩이 완료된 후에 호출될 작업 - 마진 조절
                val width = binding.allInJpegTextView.width
                val textViewlayoutParams = binding.allInJpegTextView.layoutParams as ViewGroup.MarginLayoutParams
                val leftMarginInDp = 0
                val topMarginInDp =  spToDp(context,11f).toInt()
                val rightMarginInDp = - pxToDp((width/2 - spToDp(context,10f)).toFloat()).toInt() //왼쪽 마진(dp) //
                val bottomMarginInDp = 0 // 아래쪽 마진(dp)

                textViewlayoutParams.setMargins(leftMarginInDp, topMarginInDp, rightMarginInDp, bottomMarginInDp)
                binding.allInJpegTextView.layoutParams = textViewlayoutParams

                // 작업을 수행한 후 리스너를 제거할 수도 있습니다.
                binding.allInJpegTextView.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })

        /* Audio 버튼 UI - 있으면 표시, 없으면 GONE */
//        if (container.audioContent.audio != null && container.audioContent.audio!!.size != 0) {
//            binding.audioBtn.visibility = View.VISIBLE
//        }
//        else {
//            binding.audioBtn.visibility = View.GONE
//        }

        /*  Text 있을 경우 - 표시 */
        if (container.textContent.textCount != 0){

            binding.savedTextView.visibility = View.VISIBLE

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
            else {
                binding.savedTextView.setText("")
            }
        }
        else {
            binding.savedTextView.visibility = View.INVISIBLE
        }

        mainViewPagerAdapter = ViewPagerAdapter(requireContext())
        mainViewPagerAdapter.setUriList(jpegViewModel.imageUriLiveData.value!!)
        binding.viewPager2.setUserInputEnabled(false);

        binding.viewPager2.adapter = mainViewPagerAdapter
        binding.viewPager2.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            @RequiresApi(Build.VERSION_CODES.Q)
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                Log.d("[ViewerFragment] 바뀐 position : ", ""+position)
                mainViewPagerAdapter.notifyDataSetChanged()

                // 오디오 버튼 초기화
                if( isAudioBtnClicked ) { // 클릭 되어 있던 상태
                    binding.audioBtn.background = ColorDrawable(Color.TRANSPARENT)
                    isAudioBtnClicked = false
                }

                // 재생 중인 오디오 stop
                //jpegViewModel.jpegMCContainer.value!!.audioResolver.audioStop()

                // 매직 버튼 초기화
                if( isMagicBtnClicked ) { // 클릭 되어 있던 상태
                    binding.magicBtn.background = ColorDrawable(Color.TRANSPARENT)
                    isMagicBtnClicked = false
                    mainViewPagerAdapter.setCheckMagicPicturePlay(false, isFinished)
                }
            }
        })

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

        /** Button 이벤트 리스너 - editBtn, backBtn, audioBtn*/

//        binding.audioBtn.setOnClickListener{
//
//            // TODO: 이미 존재는하지만 hidden처리 되어있는 view의 속성을 변경
//            //어떤 방법을 사용하던 어쨌든 이미지 크기 계산해서 width 조절 -> 이미지마다 위에 뜰 수 있도록!
//
//            if (!isAudioBtnClicked) { // 클릭 안되어 있던 상태
//                /* layout 변경 */
//                binding.audioBtn.setImageResource(R.drawable.sound4)
//                isAudioBtnClicked = true
//
//                // 오디오 재생
//                jpegViewModel.jpegMCContainer.value!!.audioPlay()
//                isAudioPlaying.value = true
//            }
//
//            //TODO: FrameLayout에 동적으로 추가된 View 삭제 or FrameLayout에 view는 박아놓고 hidden 처리로 수행
//            else { // 클릭 되어 있던 상태
//
//                /* layout 변경 */
//                binding.audioBtn.setImageResource(R.drawable.audio)
//                isAudioBtnClicked = false
//
//                jpegViewModel.jpegMCContainer.value!!.audioStop()
//            }
//        }


//        audioTopMargin.observe(requireActivity()){ value ->
//
//            val layoutParams = binding.audioBtn.layoutParams as ViewGroup.MarginLayoutParams
//            val leftMarginInDp = 0 // 왼쪽 마진(dp)
//            val topMarginInDp =  pxToDp(value.toFloat()).toInt()// 위쪽 마진(dp)
//            val rightMarginInDp = pxToDp(20f).toInt() // 오른쪽 마진(dp)
//            val bottomMarginInDp = 0 // 아래쪽 마진(dp)
//
//            layoutParams.setMargins(leftMarginInDp, topMarginInDp, rightMarginInDp, bottomMarginInDp)
//            binding.audioBtn.layoutParams = layoutParams
//
//        }

//        audioEndMargin.observe(requireActivity()) {value ->
//
//            val layoutParams = binding.audioBtn.layoutParams as ViewGroup.MarginLayoutParams
//            val leftMarginInDp = 0 // 왼쪽 마진(dp)
//            val topMarginInDp =  pxToDp(20f).toInt()// 위쪽 마진(dp)
//            val rightMarginInDp = pxToDp(value.toFloat()).toInt() // 오른쪽 마진(dp)
//            val bottomMarginInDp = 0 // 아래쪽 마진(dp)
//
//            layoutParams.setMargins(leftMarginInDp, topMarginInDp, rightMarginInDp, bottomMarginInDp)
//            binding.audioBtn.layoutParams = layoutParams
//        }

        binding.editBtn.setOnClickListener{
            findNavController().navigate(R.id.action_viewerFragment_to_editFragment)
        }

        binding.backBtn.setOnClickListener{
            backPressed()
        }
    }

    fun setMagicPicture() {
        val imageContent = jpegViewModel.jpegMCContainer.value?.imageContent!!
        imageContent.setMainBitmap(null)
        mainViewPagerAdapter.resetMagicPictureList()

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
                }
            }
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
//        }
    }


    /** 숨겨진 이미지들 imageView로 ScrollView에 추가 */
    @RequiresApi(Build.VERSION_CODES.M)
    fun setCurrentOtherImage(){

        var pictureList = jpegViewModel.jpegMCContainer.value?.getPictureList()
        binding.imageCntTextView.text = "담긴 사진 ${jpegViewModel.getPictureByteArrList().size}장"


        // bitmap list (seek bar 속도 개선)
        val imageContent = jpegViewModel.jpegMCContainer.value?.imageContent!!
        CoroutineScope(Dispatchers.Default).launch {
            while(!imageContent.checkPictureList) {}

            val bitmap = imageContent.getBitmapList()
            if(bitmap!=null)
                bitmapList = bitmap
        }

        if (pictureList != null) {

            if (jpegViewModel.getPictureByteArrList().size != 1 ) {

                /* seekBar 처리 - 스크롤뷰 아이템 개수에 따라, progress와 max 지정 */
                if (binding.seekBar.visibility == View.VISIBLE) {
                    binding.seekBar.progress = 0
                    binding.seekBar.max = jpegViewModel.getPictureByteArrList().size - 1
                    binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) { // 시크바의 값이 변경될 때 호출되는 메서드
                            jpegViewModel.setselectedSubImage(pictureList[progress % binding.seekBar.max])
                            if (bitmapList.size >= progress + 1) { // bitmap으로 사진 띄우기
                                mainViewPagerAdapter.setExternalImageBitmap(bitmapList[progress])
                            } else {
                                // 비트맵은 따로 만들고 있고 해당 index의 비트맵이 안만들어졌으면 글라이드로
                                CoroutineScope(Dispatchers.Main).launch {
                                    mainViewPagerAdapter.setExternalImage(jpegViewModel.getPictureByteArrList()[progress % binding.seekBar.max])
                                }
                            }
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar) {
                            // 사용자가 시크바를 터치하여 움직이기 시작할 때 호출되는 메서드입니다.
                        }

                        override fun onStopTrackingTouch(seekBar: SeekBar) {
                            // 사용자가 시크바 터치를 멈추었을 때 호출되는 메서드입니다.
                        }
                    })
                }
            }

            binding.linear.removeAllViews() // 뷰 초기화

            CoroutineScope(Dispatchers.IO).launch {

                val pictureByteArrList = jpegViewModel.getPictureByteArrList()
                var firstImageView:ImageView? = null
                for(i in 0..pictureList.size-1){
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
                        if (i == 0){
                            mainMark.visibility = View.VISIBLE
                            firstImageView = scrollImageView
                        }

                        CoroutineScope(Dispatchers.Main).launch {
                            // 이미지 바인딩
                            Glide.with(scrollImageView)
                                .load(pictureByteArr)
                                .into(scrollImageView)

                            scrollImageView.setOnClickListener { // scrollview 이미지를 main으로 띄우기


                                Log.d("click i : ",""+i)
                                if (previousClickedItem != scrollImageView) {
                                    if (previousClickedItem != null){ //초기 설정값이 아닐때
                                        // 기존 아이템 UI 없애기
                                        previousClickedItem!!.background = null
                                        previousClickedItem!!.setPadding(0,0,0,0)
                                    }

                                    previousClickedItem = scrollImageView
                                    scrollImageView.setBackgroundResource(R.drawable.chosen_image_border)
                                    scrollImageView.setPadding(6,6,6,6)
                                    if (binding.seekBar.visibility == View.VISIBLE) {
                                        binding.seekBar.progress = i
                                    }

                                    CoroutineScope(Dispatchers.Main).launch{
                                        mainViewPagerAdapter.setExternalImage(pictureByteArr!!)
                                    }
                                    jpegViewModel.setselectedSubImage(picture)
                                }
                            }
                            binding.linear.addView(scollItemLayout)
                            firstImageView!!.performClick()
                        }
                    } catch (e: IllegalStateException) {
                        println(e.message)
                    }
                } // end of for

                var container = jpegViewModel.jpegMCContainer.value!!

                // 오디오 있는 경우
                if (container.audioContent.audio != null && container.audioContent.audio!!.size != 0){
                        CoroutineScope(Dispatchers.Main).launch {
                        var currText = binding.imageCntTextView.text
                        binding.imageCntTextView.text = "${currText} + 오디오"
                        val scollItemLayout =
                            layoutInflater.inflate(R.layout.scroll_item_audio, null)
                        // 위 불러온 layout에서 변경을 할 view가져오기
                        val scrollAudioView: ImageView =
                            scollItemLayout.findViewById(R.id.scrollItemAudioView)
                        scrollAudioView.setOnClickListener { // scrollview 이미지를 main으로 띄우기
                            if (scrollAudioView.background == null){
                                // TODO: 음악 재생
                                Log.d("song music: ","음악 재생")
                                scrollAudioView.setBackgroundResource(R.drawable.chosen_scroll_menu_border)
                                if(isAudioPlaying.value != true){
                                    jpegViewModel.jpegMCContainer.value!!.audioPlay()
                                    isAudioPlaying.value = true
                                }
                            }
                            else {
                                // TODO: 음악 멈춤
                                Log.d("song music: ","음악 멈춤")
                                scrollAudioView.background = null
                                jpegViewModel.jpegMCContainer.value!!.audioStop()
                                isAudioPlaying.value = false
                            }
                        }

                        isAudioPlaying.observe(requireActivity()){ value ->
                            if (value == false){
                                scrollAudioView.background = null
                            }
                        }
                        binding.linear.addView(scollItemLayout,binding.linear.childCount)
                    }
                }
                // 텍스트 있는 경우
                if (container.textContent.textCount != 0){
                    CoroutineScope(Dispatchers.Main).launch {
                        var currText = binding.imageCntTextView.text
                        binding.imageCntTextView.text = "${currText} + 텍스트"
                    }
                }

            }
        }
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

    fun pxToDp(px: Float): Float {

        val resources = context.resources
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_PX,
            px,
            resources.displayMetrics
        )
    }

    fun spToDp(context: Context, sp: Float): Float {
        val scale = context.resources.displayMetrics.density
        return sp * scale
    }

    fun getFilePathFromUri(context: Context, uri: Uri): String? {
        var filePath: String? = null

        // "content" scheme일 경우
        if (uri.scheme == "content") {
            // API 레벨이 KitKat(19) 이상인 경우
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(context, uri)) {
                // External Storage Document Provider
                if (isExternalStorageDocument(uri)) {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":")
                    val type = split[0]

                    if ("primary".equals(type, ignoreCase = true)) {
                        filePath = "${context.getExternalFilesDir(null)}/${split[1]}"
                    }
                }
                // Downloads Document Provider
                else if (isDownloadsDocument(uri)) {
                    val id = DocumentsContract.getDocumentId(uri)
                    val contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"),
                        id.toLong()
                    )
                    filePath = getDataColumn(context, contentUri, null, null)
                }
                // Media Provider
                else if (isMediaDocument(uri)) {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":")
                    val type = split[0]

                    var contentUri: Uri? = null
                    when (type) {
                        "image" -> contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        "video" -> contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        "audio" -> contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    }

                    val selection = "_id=?"
                    val selectionArgs = arrayOf(split[1])

                    filePath = getDataColumn(context, contentUri, selection, selectionArgs)
                }
            }
            // API 레벨이 KitKat(19) 미만인 경우 또는 Document Uri가 아닌 경우
            else {
                filePath = getDataColumn(context, uri, null, null)
            }
        }
        // "file" scheme일 경우
        else if (uri.scheme == "file") {
            filePath = uri.path
        }

        return filePath
    }

    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    private fun getDataColumn(context: Context, uri: Uri?, selection: String?, selectionArgs: Array<String>?): String? {
        var path: String? = null
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = context.contentResolver.query(uri!!, projection, selection, selectionArgs, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                path = it.getString(columnIndex)
            }
        }
        return path
    }

    fun backPressed(){
        Glide.get(requireContext()).clearMemory()
        val bundle = Bundle()
        bundle.putInt("currentPosition",binding.viewPager2.currentItem)
        findNavController().navigate(R.id.action_viewerFragment_to_basicViewerFragment,bundle)
    }
}