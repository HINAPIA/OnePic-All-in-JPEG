package com.example.onepic.ViewerModule

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
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
import com.example.onepic.LoadModule.LoadResolver
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
    private lateinit var mainViewPagerAdapter:ViewPagerAdapter
    private var isContainerChanged = MutableLiveData<Boolean>()
    private var isTxtBtnClicked = false
    private var currentPosition:Int? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentViewerBinding.inflate(inflater, container, false)
        Log.d("[ViewerFragment] onCreateView: ","fragment 전환됨")
        currentPosition = arguments?.getInt("currentPosition")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
        if(currentPosition != null){ // GalleryFragment에서 넘어왔을 때 (선택된 이미지가 있음)
            binding.viewPager2.setCurrentItem(currentPosition!!,false)
            currentPosition = null
        }
    }

    /** ViewPager Adapter 및 swipe callback 설정 & Button 이벤트 처리 */
    fun init() {

        mainViewPagerAdapter = ViewPagerAdapter(requireContext(),jpegViewModel.imageUriLiveData.value!!)
        Log.d("adapter item count = ",""+mainViewPagerAdapter.itemCount)
        binding.viewPager2.adapter = mainViewPagerAdapter
        binding.viewPager2.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            @RequiresApi(Build.VERSION_CODES.Q)
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                Log.d("[ViewerFragment] 바뀐 position : ", ""+position)
                mainViewPagerAdapter.notifyDataSetChanged()
                // 텍스트 버튼 초기화
                if( isTxtBtnClicked ) { // 클릭 되어 있던 상태
                    binding.textBtn.setBackgroundResource(R.drawable.round_button)
                    isTxtBtnClicked = false
                    binding.textLinear.visibility = View.INVISIBLE
                    //TODO: 1) mainPictureDrawable도 초기화, 2) FrameLayout에 추가 되었었던 View hidden 처리
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
                it.setBackgroundResource(R.drawable.round_button_clicked)
                isTxtBtnClicked = true
                textLinear.visibility = View.VISIBLE

            }

            //TODO: FrameLayout에 동적으로 추가된 View 삭제 or FrameLayout에 view는 박아놓고 hidden 처리로 수행
            else { // 클릭 되어 있던 상태
                /* layout 변경 */
                it.setBackgroundResource(R.drawable.round_button)
                isTxtBtnClicked = false
                textLinear.visibility = View.INVISIBLE
            }
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
            val iStream: InputStream? = requireContext().contentResolver.openInputStream(sourcePhotoUri!!)
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

}