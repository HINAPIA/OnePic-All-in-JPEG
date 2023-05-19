package com.goldenratio.onepic.ViewerModule.Fragment

import android.annotation.SuppressLint
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.transition.TransitionInflater
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout

import android.widget.ProgressBar
import androidx.annotation.RequiresApi

import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.goldenratio.onepic.JpegViewModel
import com.goldenratio.onepic.LoadModule.LoadResolver
import com.goldenratio.onepic.R
import com.goldenratio.onepic.databinding.FragmentAnalyzeBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream


@SuppressLint("LongLogTag")
@RequiresApi(Build.VERSION_CODES.Q)
class AnalyzeFragment : Fragment() {

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
                var container = jpegViewModel.jpegMCContainer.value!!

                var imageCount = container.imageContent.pictureCount
                CoroutineScope(Dispatchers.Main).launch{
                    binding.analyzeDataTextView.text = "사진 ${imageCount}장 발견!\n"
                }

            }


            // 오디오
            if (progress == 41) {
                var curr = binding.analyzeDataTextView.text
                var container = jpegViewModel.jpegMCContainer.value!!

                if (container.audioContent.audio != null && container.audioContent.audio!!.size != 0){

                    CoroutineScope(Dispatchers.Main).launch{
                        binding.analyzeDataTextView.text = "${curr}오디오 발견!\n"
                    }
                }
            }

            // 텍스트
            if (progress == 69) {
                var curr = binding.analyzeDataTextView.text
                var container = jpegViewModel.jpegMCContainer.value!!

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
        sharedElementEnterTransition = TransitionInflater.from(context).inflateTransition(android.R.transition.move)
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
            var jop = async {
                loadResolver.createMCContainer(jpegViewModel.jpegMCContainer.value!!,sourceByteArray, isContainerChanged) }
            jop.await()
            jpegViewModel.setCurrentImageFilePath(position) // edit 위한 처리
        }
    }

    fun setCurrentPictureByteArrList(){

        var pictureList = jpegViewModel.jpegMCContainer.value?.getPictureList()

        if (pictureList != null) {

            CoroutineScope(Dispatchers.IO).launch {
                val pictureByteArrayList = mutableListOf<ByteArray>()
                for (picture in pictureList){
                    val pictureByteArr = jpegViewModel.jpegMCContainer.value?.imageContent?.getJpegBytes(picture)
                    pictureByteArrayList.add(pictureByteArr!!)
                } // end of for..
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
                findNavController().navigate(R.id.action_analyzeFragment_to_viewerFragment,bundle)
                isFinished.value = false
            }

        }
       // binding.progressBar.visibility = View.GONE

//        binding.resultLinear.visibility = View.VISIBLE
//        binding.analyzeDataTextView.visibility = View.VISIBLE
//
//        var curr = binding.analyzeDataTextView.text
//        var container = jpegViewModel.jpegMCContainer.value!!
//
//        CoroutineScope(Dispatchers.Default).launch {
//            Thread.sleep(1500)
//            var imageCount = container.imageContent.pictureCount
//            CoroutineScope(Dispatchers.Main).launch{
//                binding.analyzeDataTextView.text = "담긴 사진 ${imageCount}장 발견!"
//            }
//        }




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

}