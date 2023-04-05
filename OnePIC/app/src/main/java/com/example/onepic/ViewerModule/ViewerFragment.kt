package com.example.onepic.ViewerModule

import android.annotation.SuppressLint
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.bumptech.glide.Glide
import com.example.camerax.LoadModule.LoadResolver
import com.example.camerax.PictureModule.Picture
import com.example.onepic.EditModule.Fragment.EditFragment
import com.example.onepic.JpegViewModel
import com.example.onepic.R
import com.example.onepic.databinding.FragmentViewerBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

@SuppressLint("LongLogTag")
class ViewerFragment : Fragment() {

    private lateinit var binding: FragmentViewerBinding
    private val jpegViewModel by activityViewModels<JpegViewModel>()
    private var loadResolver : LoadResolver = LoadResolver()
    private var isViewChanged = MutableLiveData<Boolean>()
    private var isTxtBtnClicked = false
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentViewerBinding.inflate(inflater, container, false)
        Log.d("[ViewerFragment] onCreateView: ","fragment 전환됨")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("[ViewerFragment] onViewCreated: ","")
        Log.d("jpegContainer = ", jpegViewModel.jpegMCContainer.value?.toString()+"")

        var size = jpegViewModel.imageUriLiveData.value!!.size // activity viewmodel 데이터가 제대로 들어왔는지 확인
        Log.d("[ViewerFragment] imageUriLIst size : ",""+size)

        init()
    }


    fun init() {

        val adapter = ViewPagerAdapter(requireContext(),jpegViewModel.imageUriLiveData.value!!)
        Log.d("adapter item count = ",""+adapter.itemCount)
        binding.viewPager2.adapter = adapter
        binding.viewPager2.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            @RequiresApi(Build.VERSION_CODES.Q)
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                Log.d("[ViewerFragment] 바뀐 position : ", ""+position)

                // 텍스트 버튼 초기화
                if( isTxtBtnClicked ) { // 클릭 되어 있던 상태
                    binding.textBtn.setBackgroundResource(R.drawable.round_button)
                    isTxtBtnClicked = false
                }

                setCurrentMCContainer(position)
            }
        })

        isViewChanged.observe(requireActivity()){ value ->
            Log.d("[ViewerFragment] jpegMCContainer가 바뀜!","")
            Log.d("test_test", "jpegMCContainer가 바뀜!")
            if (value == true){
                setCurrentOtherImage()
                isViewChanged.value = false
            }
        }

        binding.textBtn.setOnClickListener{

            if (!isTxtBtnClicked) { // 클릭 안되어 있던 상태
                it.setBackgroundResource(R.drawable.round_button_clicked)
                isTxtBtnClicked = true
            }
            else { // 클릭 되어 있던 상태
                it.setBackgroundResource(R.drawable.round_button)
                isTxtBtnClicked = false
            }
        }

        binding.editBtn.setOnClickListener{
            findNavController().navigate(R.id.action_viewerFragment_to_editFragment)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
     fun setCurrentMCContainer(position:Int){
        CoroutineScope(Dispatchers.IO).launch {
            Log.d("[ViewerFragment] 바뀐 position : ", ""+position)
            val sourcePhotoUri = getUriFromPath(jpegViewModel.imageUriLiveData.value!!.get(position))
            val iStream: InputStream? = requireContext().contentResolver.openInputStream(sourcePhotoUri!!)
            var sourceByteArray = getBytes(iStream!!)
            var jop = async {
                loadResolver.createMCContainer(jpegViewModel.jpegMCContainer.value!!,sourceByteArray, isViewChanged) }
            jop.await()
            // Picture 완전히 생성될 때까지 기다리기
            Log.d("test_test", "=========================")
            Log.d("test_test", "프래그먼트 변화 하기 전 picutureList size : ${jpegViewModel.jpegMCContainer.value!!.imageContent.pictureCount}")

        }

    }

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
                        Glide.with(scrollImageView)
                            .load(pictureByteArr)
                            .into(scrollImageView)
                        binding.linear.addView(scollItemLayout)
                    }

                }
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
        return byteBuffer.toByteArray()
    }

    @SuppressLint("Range")
    fun getUriFromPath(filePath: String): Uri { // filePath String to Uri
        val cursor = requireContext().contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            null, "_data = '$filePath'", null, null)
        var uri:Uri
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
            return Uri.parse("dd")
        }
        return uri
    }

}