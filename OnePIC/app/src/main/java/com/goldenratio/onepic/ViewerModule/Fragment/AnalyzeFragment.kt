package com.goldenratio.onepic.ViewerModule.Fragment

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.goldenratio.onepic.JpegViewModel
import com.goldenratio.onepic.LoadModule.LoadResolver
import com.goldenratio.onepic.AllinJPEGModule.Contents.ContentAttribute
import com.goldenratio.onepic.R
import com.goldenratio.onepic.databinding.FragmentAnalyzeBinding
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.lang.Runnable


@SuppressLint("LongLogTag")
@RequiresApi(Build.VERSION_CODES.Q)
class AnalyzeFragment : Fragment() {

    private lateinit var callback: OnBackPressedCallback

    private lateinit var binding: FragmentAnalyzeBinding
    private val jpegViewModel by activityViewModels<JpegViewModel>()

    private var loadResolver : LoadResolver = LoadResolver()
    private var currentPosition:Int? = null // gallery fragment 에서 넘어올 때

    private lateinit var progressBar: ProgressBar
    private var progress = 5
    private val handler = Handler()

    private val runnable = object : Runnable {
        override fun run() {
            // 프로그레스 증가
            progress+=2

            if (progress == 7) {
                //binding.resultLinear.visibility = View.VISIBLE
                binding.analyzeDataTextView.visibility = View.VISIBLE
            }

            // 이미지
            if (progress == 23) {

                var curr = binding.analyzeDataTextView.text
                var container = jpegViewModel.jpegAiContainer.value!!

                var imageCount = container.imageContent.pictureCount
                CoroutineScope(Dispatchers.Main).launch{
                    binding.analyzeDataTextView.text = "사진 ${imageCount}장 발견!\n"
                }

            }

            if (progress == 31) {
                if (jpegViewModel.jpegAiContainer.value!!.imageContent.checkAttribute(ContentAttribute.magic)) {
                    var curr = binding.analyzeDataTextView.text
                    var container = jpegViewModel.jpegAiContainer.value!!


                    CoroutineScope(Dispatchers.Main).launch {
                        binding.analyzeDataTextView.text = "${curr}매직 사진 발견!\n"
                    }
                }
            }


            // 오디오
            if (progress == 41) {
                var curr = binding.analyzeDataTextView.text
                var container = jpegViewModel.jpegAiContainer.value!!

                if (container.audioContent.audio != null){

                    CoroutineScope(Dispatchers.Main).launch{
                        binding.analyzeDataTextView.text = "${curr}오디오 발견!\n"
                    }
                }
            }

            // 텍스트
            if (progress == 69) {
                var curr = binding.analyzeDataTextView.text
                var container = jpegViewModel.jpegAiContainer.value!!

                if (container.textContent.textCount != 0){
                    CoroutineScope(Dispatchers.Main).launch{
                        binding.analyzeDataTextView.text = "${curr}텍스트 발견!\n"
                    }
                }
            }


            // 프로그레스바 업데이트
           binding.progressBar.progress = progress
            if (progress > 100) {

                binding.stateTextView.text = "분석 완료"

                val desiredWidthInDp = 20 // 원하는 dp 값
                val desiredHeightInDp = 20 // 원하는 dp 값

                val density = resources.displayMetrics.density
                val desiredWidthInPixels = (desiredWidthInDp * density).toInt()
                val desiredHeightInPixels = (desiredHeightInDp * density).toInt()

                //Glide.with(binding.loadingImageView).load(R.raw.confirm).into(binding.loadingImageView)
                Glide.with(binding.loadingImageView)
                    .asGif()
                    .load(R.raw.confirm)
                    .listener(object : RequestListener<GifDrawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<GifDrawable>?,
                            isFirstResource: Boolean
                        ): Boolean {
                           return false
                        }

                        override fun onResourceReady(
                            resource: GifDrawable?,
                            model: Any?,
                            target: Target<GifDrawable>?,
                            dataSource: DataSource?,
                            isFirstResource: Boolean
                        ): Boolean {
                            resource?.setLoopCount(1)
                            return false
                        }

                    })
                    .into(binding.loadingImageView)


                val params = binding.loadingImageView.layoutParams as LinearLayout.LayoutParams
                params.gravity = Gravity.CENTER // 또는 다른 원하는 Gravity 값 설정
                params.width = desiredWidthInPixels
                params.height = desiredHeightInPixels
                params.setMargins(0, 0, 0, 0) // 모든 마진을 0으로 설정
                binding.loadingImageView.layoutParams = params

                binding.magnifyingView.visibility = View.GONE

                setAnalyzeResultText()

                return
            }
            // 일정 시간 간격으로 Runnable을 반복 실행
            handler.postDelayed(this, 50)
        }
    }

    companion object {
        var currentFilePath:String = ""
        private var isContainerChanged = MutableLiveData<Boolean>()
        var isFinished: MutableLiveData<Boolean> = MutableLiveData(false)
        var isPictureClicked: MutableLiveData<Boolean> = MutableLiveData(true)
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                backPressed()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAnalyzeBinding.inflate(inflater, container, false)
        currentPosition = arguments?.getInt("currentPosition") // 갤러리 프래그먼트에서 넘어왔을 때

        Glide.with(this).load(R.raw.dots).into(binding.loadingImageView)
        Glide.with(this).load(R.raw.magnifying).into(binding.magnifyingView)

        val uri = jpegViewModel.imageUriLiveData.value!![currentPosition!!]
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            Glide.with(requireContext()).load(uri).into(binding.imageView2)
        }
        else {
            Glide.with(requireContext()).load(getUriFromPath(uri)).into(binding.imageView2)
        }

        progressBar = binding.progressBar

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setCurrentMCContainer(currentPosition!!)



        // MCContainer가 변경되었을 때(Page가 넘어갔을 때) 처리
        isContainerChanged.observe(requireActivity()){ value ->
            if (value == true){
                setCurrentPictureByteArrList()
                //setMagicPicture()
                isContainerChanged.value = false
            }
        }

        isFinished.observe(viewLifecycleOwner){value ->
            if (value == true){
                handler.post(runnable)

                isFinished.value = false

//                CoroutineScope(Dispatchers.Default).launch {
//                    Thread.sleep(1500)
//                    CoroutineScope(Dispatchers.Main).launch{
//                        val bundle = Bundle()
//                        bundle.putInt("currentPosition",currentPosition!!)
//
//                        findNavController().navigate(R.id.action_basicViewerFragment_to_analyzeFragment,bundle,null,extras)
//                        BasicViewerFragment.isFinished.value = false
//                    }
//                }
            }
        }


        // 전환 시에 애니메이션 적용
//        sharedElementEnterTransition = TransitionInflater.from(context).inflateTransition(android.R.transition.move)
    }

    override fun onDetach() {
        super.onDetach()
        callback.remove()
    }

    @SuppressLint("Range")
    fun getUriFromPath(filePath: String): Uri { // filePath String to Uri

        val cursor = requireContext().contentResolver.query(
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

    @RequiresApi(Build.VERSION_CODES.Q)
    fun setCurrentMCContainer(position:Int){
        CoroutineScope(Dispatchers.IO).launch {
            Log.d("[ViewerFragment] 바뀐 position : ", ""+position)
            val sourcePhotoUri = jpegViewModel.imageUriLiveData.value!!.get(position)

            var uri:Uri
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                uri = Uri.parse(sourcePhotoUri)
            }
            else {
                uri = getUriFromPath(sourcePhotoUri)
            }

            val iStream: InputStream? = requireContext().contentResolver.openInputStream(uri)
            var sourceByteArray = getBytes(iStream!!)
            System.gc()

            val imageContent = jpegViewModel.jpegAiContainer.value!!.imageContent
            imageContent.checkPictureList = false

            var jop = async {
                loadResolver.createAiContainer(jpegViewModel.jpegAiContainer.value!!,sourceByteArray) }
            jop.await()

            while(!imageContent.checkPictureList) { }
            CoroutineScope(Dispatchers.Default).launch {
                imageContent.setBitmapList()
            }

            setCurrentPictureByteArrList()

            jpegViewModel.setCurrentImageUri(position) // edit 위한 처리
        }
    }

    fun setCurrentPictureByteArrList(){

        var pictureList = jpegViewModel.jpegAiContainer.value?.getPictureList()

        if (pictureList != null) {
            CoroutineScope(Dispatchers.IO).launch {
                val pictureByteArrayList = mutableListOf<ByteArray>()
                for (picture in pictureList){
                    val pictureByteArr = jpegViewModel.jpegAiContainer.value?.imageContent?.getJpegBytes(picture)
                    pictureByteArrayList.add(pictureByteArr!!)
                } // end of for..

//                    val pictureByteArr = jpegViewModel.jpegMCContainer.value?.imageContent?.getJpegBytes(pictureList[0])
//                    pictureByteArrayList.add(pictureByteArr!!)

                jpegViewModel.setpictureByteArrList(pictureByteArrayList)
                CoroutineScope(Dispatchers.Main).launch{
                    isFinished.value = true
                }
            }
        }
    }

    fun setAnalyzeResultText () {

        CoroutineScope(Dispatchers.Default).launch{
            Thread.sleep(1500)

            CoroutineScope(Dispatchers.Main).launch{
                val bundle = Bundle()
                bundle.putInt("currentPosition",currentPosition!!)
//                findNavController().popBackStack(R.id.analyzeFragment,false)
//                findNavController().navigate(R.id.viewerFragment,bundle)

                findNavController().navigate(R.id.viewerFragment, bundle, NavOptions.Builder()
                    .setPopUpTo(R.id.analyzeFragment, true)
                    .build())


                 //findNavController().navigate(R.id.action_analyzeFragment_to_viewerFragment,bundle)
                //isFinished.value = false
            }

        }
    }


    @Throws(IOException::class)
    fun getBytes(inputStream: InputStream): ByteArray {
        val byteBuffer = ByteArrayOutputStream()
        val bufferSize = 1024
        val buffer = ByteArray(bufferSize)
        var len = 0

//        val activityManager = requireContext().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
//        val memoryInfo = ActivityManager.MemoryInfo()
//        activityManager.getMemoryInfo(memoryInfo)
//
//        val availableMemory = memoryInfo.availMem
//        Log.d("analyzeByteArray","Available memory: $availableMemory bytes")

        while (inputStream.read(buffer, 0, bufferSize).also { len = it } != -1) {
            byteBuffer.write(buffer, 0, len)
        }
        byteBuffer.flush()
        byteBuffer.close()
        inputStream.close()

        val currentSize = byteBuffer.size()
        Log.d("analyzeByteArray","Current byteBuffer size: $currentSize bytes")
        return byteBuffer.toByteArray()
    }

    fun backPressed(){

        jpegViewModel.jpegAiContainer.value!!.imageContent.resetBitmap()

        jpegViewModel.jpegAiContainer.value!!.init()
        handler.removeCallbacksAndMessages(null)
        val bundle = Bundle()
        bundle.putInt("currentPosition",currentPosition!!)
        findNavController().navigate(R.id.action_analyzeFragment_to_galleryFragment,bundle)
    }

}