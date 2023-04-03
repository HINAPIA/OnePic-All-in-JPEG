package com.example.onepic.ViewerModule

import android.annotation.SuppressLint
import android.content.ContentUris
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.bumptech.glide.Glide
import com.example.camerax.LoadModule.LoadResolver
import com.example.camerax.PictureModule.Contents.ContentAttribute
import com.example.camerax.PictureModule.Picture
import com.example.onepic.JpegViewModel
import com.example.onepic.R
import com.example.onepic.databinding.FragmentViewerBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

    // 후에 JpegViewer가 할 일
    private lateinit var mainPicture: Picture

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

        /** TODO: 1) ViewModel의 image uri list -> drawable list
        2) ViewPager로 메인 스크롤뷰 채우기(main 이미지)
        3) jpegContainer 만들기 - main uri로
        4) jpegContainer 분석해서 main 및 내부 이미지들로 하단 스크롤뷰 채우기
         */

        val adapter = ViewPagerAdapter(requireContext(),jpegViewModel.imageUriLiveData.value!!)
        Log.d("adapter item count = ",""+adapter.itemCount)
        binding.viewPager2.adapter = adapter
        binding.viewPager2.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            @RequiresApi(Build.VERSION_CODES.Q)
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                Log.d("[ViewerFragment] 바뀐 position : ", ""+position)
                setCurrentMCContainer(position)
            }
        })

        isViewChanged.observe(requireActivity()){ value ->
            Log.d("[ViewerFragment] jpegMCContainer가 바뀜!","")
            if (value == true){
                setCurrentOtherImage()
                isViewChanged.value = false
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun setCurrentMCContainer(position:Int){
        Log.d("[ViewerFragment] 바뀐 position : ", ""+position)
        val sourcePhotoUri = getUriFromPath(jpegViewModel.imageUriLiveData.value!!.get(position))
        val iStream: InputStream? = requireContext().contentResolver.openInputStream(sourcePhotoUri!!)
        var sourceByteArray = getBytes(iStream!!)
        loadResolver.createMCContainer(jpegViewModel.jpegMCContainer.value!!,sourceByteArray)
        isViewChanged.value = true
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