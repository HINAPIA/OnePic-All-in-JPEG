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
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.util.TypedValue
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


    /* text, audio, magic 선택 여부 */
    private var isAudioBtnClicked = false
    private var isMagicBtnClicked = false

    companion object {
        var currentFilePath:String = ""
        var isFinished: MutableLiveData<Boolean> = MutableLiveData(false)
        var isEditStoraged:Boolean = false
        var isAudioPlaying = MutableLiveData<Boolean>()
        var audioTopMargin = MutableLiveData<Int>()
        var audioEndMargin = MutableLiveData<Int>()
    }

    private lateinit var context: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context = requireContext()
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

        if (isEditStoraged && currentFilePath != "" && currentFilePath != null) { // 편집창에서 저장하고 넘어왔을 때

            mainViewPagerAdapter.setUriList(jpegViewModel.imageUriLiveData.value!!)

            var path = currentFilePath

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) { // 13 버전 보다 낮을 경우 -> uri 를 filePath 로 변경
                path = getFilePathFromUri(requireContext(),Uri.parse(currentFilePath)).toString()
            }

            binding.viewPager2.setCurrentItem(jpegViewModel.getFilePathIdx(path)!!,false)

            isEditStoraged = false
        }


        setCurrentOtherImage()
        binding.scrollView.visibility = View.VISIBLE


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

        binding.viewPager2.setOnClickListener {

            if (binding.savedTextView.text != null && binding.savedTextView.text != ""){

                if (binding.savedTextView.visibility == View.VISIBLE){
                    binding.savedTextView.visibility = View.INVISIBLE
                }
                else {
                    binding.savedTextView.visibility = View.VISIBLE
                }
            }
        }

        binding.savedTextView.setOnClickListener {
            if (binding.savedTextView.text != null && binding.savedTextView.text != ""){
                if (it.visibility == View.VISIBLE){
                    it.visibility = View.INVISIBLE
                }
                else {
                    it.visibility = View.VISIBLE
                }
            }
        }


        /* Audio 버튼 UI - 있으면 표시, 없으면 GONE */
        if (container.audioContent.audio != null && container.audioContent.audio!!.size != 0) {
            binding.audioBtn.visibility = View.VISIBLE
            // margin 계산

        }
        else {
            binding.audioBtn.visibility = View.GONE
        }

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
                CoroutineScope(Dispatchers.Default).launch {
//                    setMagicPicture()
                }

                Log.d("[ViewerFragment] 바뀐 position : ", ""+position)
                mainViewPagerAdapter.notifyDataSetChanged()


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

        /** Button 이벤트 리스너 - editBtn, backBtn, audioBtn*/

        binding.audioBtn.setOnClickListener{

            // TODO: 이미 존재는하지만 hidden처리 되어있는 view의 속성을 변경
            //어떤 방법을 사용하던 어쨌든 이미지 크기 계산해서 width 조절 -> 이미지마다 위에 뜰 수 있도록!

            if (!isAudioBtnClicked) { // 클릭 안되어 있던 상태
                /* layout 변경 */
                binding.audioBtn.setImageResource(R.drawable.sound4)
                isAudioBtnClicked = true

                // 오디오 재생
                jpegViewModel.jpegMCContainer.value!!.audioPlay()
                isAudioPlaying.value = true
            }

            //TODO: FrameLayout에 동적으로 추가된 View 삭제 or FrameLayout에 view는 박아놓고 hidden 처리로 수행
            else { // 클릭 되어 있던 상태

                /* layout 변경 */
                binding.audioBtn.setImageResource(R.drawable.audio)
                isAudioBtnClicked = false

                jpegViewModel.jpegMCContainer.value!!.audioStop()
            }
        }

        isAudioPlaying.observe(requireActivity()){ value ->
            if (value == false){
                binding.audioBtn.performClick()
            }
        }


        audioTopMargin.observe(requireActivity()){ value ->

            val layoutParams = binding.audioBtn.layoutParams as ViewGroup.MarginLayoutParams
            val leftMarginInDp = 0 // 왼쪽 마진(dp)
            val topMarginInDp =  pxToDp(value.toFloat()).toInt()// 위쪽 마진(dp)
            val rightMarginInDp = pxToDp(20f).toInt() // 오른쪽 마진(dp)
            val bottomMarginInDp = 0 // 아래쪽 마진(dp)

            layoutParams.setMargins(leftMarginInDp, topMarginInDp, rightMarginInDp, bottomMarginInDp)
            binding.audioBtn.layoutParams = layoutParams

        }

        audioEndMargin.observe(requireActivity()) {value ->

            val layoutParams = binding.audioBtn.layoutParams as ViewGroup.MarginLayoutParams
            val leftMarginInDp = 0 // 왼쪽 마진(dp)
            val topMarginInDp =  pxToDp(20f).toInt()// 위쪽 마진(dp)
            val rightMarginInDp = pxToDp(value.toFloat()).toInt() // 오른쪽 마진(dp)
            val bottomMarginInDp = 0 // 아래쪽 마진(dp)

            layoutParams.setMargins(leftMarginInDp, topMarginInDp, rightMarginInDp, bottomMarginInDp)
            binding.audioBtn.layoutParams = layoutParams
        }

//        setMagicPicture()

        binding.editBtn.setOnClickListener{
            findNavController().navigate(R.id.action_viewerFragment_to_editFragment)
        }

        binding.backBtn.setOnClickListener{
            Glide.get(requireContext()).clearMemory()
            val bundle = Bundle()
            bundle.putInt("currentPosition",binding.viewPager2.currentItem)
            findNavController().navigate(R.id.action_viewerFragment_to_basicViewerFragment,bundle)
        }
    }

    fun scrollAnimation(){
        binding.scrollView.visibility = View.VISIBLE

        val startPosition = - binding.scrollView.width - 10
        val endPosition = 1.0F//binding.scrollView.x //binding.pullRightView.x

        val animation = TranslateAnimation(startPosition.toFloat(), endPosition,0f, 0f)
        animation.duration = 600
        binding.scrollView.startAnimation(animation)

    }

    fun setMagicPicture() {
        val imageContent = jpegViewModel.jpegMCContainer.value?.imageContent!!
        imageContent.resetMainBitmap()
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

    fun pxToDp(px: Float): Float {

        val resources = context.resources
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_PX,
            px,
            resources.displayMetrics
        )
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
}