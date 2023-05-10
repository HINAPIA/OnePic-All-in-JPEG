package com.goldenratio.onepic.ViewerModule.Fragment

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
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.goldenratio.onepic.JpegViewModel
import com.goldenratio.onepic.LoadModule.LoadResolver
import com.goldenratio.onepic.R
import com.goldenratio.onepic.ViewerModule.Adapter.BasicViewerAdapter
import com.goldenratio.onepic.ViewerModule.Adapter.RecyclerViewAdapter
import com.goldenratio.onepic.databinding.FragmentBasicViewerBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream


@SuppressLint("LongLogTag")
@RequiresApi(Build.VERSION_CODES.M)
class BasicViewerFragment : Fragment() {

    private lateinit var binding: FragmentBasicViewerBinding
    private val jpegViewModel by activityViewModels<JpegViewModel>()
    private var loadResolver : LoadResolver = LoadResolver()
    private lateinit var mainViewPagerAdapter: BasicViewerAdapter

    private var isContainerChanged = MutableLiveData<Boolean>()
    private var currentPosition:Int? = null // gallery fragment 에서 넘어올 때

    companion object {
        var currentFilePath:String = ""
        var isFinished: MutableLiveData<Boolean> = MutableLiveData(false)
        var isPictureClicked: MutableLiveData<Boolean> = MutableLiveData(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBasicViewerBinding.inflate(inflater, container, false)
        currentPosition = arguments?.getInt("currentPosition") // 갤러리 프래그먼트에서 넘어왔을 때
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewerFragment.currentFilePath = ""

        init()

        if(currentPosition != null){ // GalleryFragment에서 넘어왔을 때 (선택된 이미지가 있음)
            binding.viewPager2.setCurrentItem(currentPosition!!,false)

            val targetIndex = currentPosition!! // 이동하고자 하는 인덱스
            binding.recyclerView.layoutManager?.scrollToPosition(targetIndex)
            binding.recyclerView.postDelayed({
                val targetView = binding.recyclerView.layoutManager?.findViewByPosition(targetIndex)
                targetView?.requestFocus()
            }, 200) // 포커스 설정을 지연시키기 위해 postDelayed 사용 (필요에 따라 딜레이 시간 조정)

            currentPosition = null
        }

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
        currentFilePath = mainViewPagerAdapter.galleryMainimages[currentPosition]
    }

    /** ViewPager Adapter 및 swipe callback 설정 & Button 이벤트 처리 */
    @SuppressLint("NewApi")
    fun init() {

        mainViewPagerAdapter = BasicViewerAdapter(requireContext())
        mainViewPagerAdapter.setUriList(jpegViewModel.imageUriLiveData.value!!)

        binding.viewPager2.adapter = mainViewPagerAdapter
        binding.viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            @RequiresApi(Build.VERSION_CODES.Q)
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                isPictureClicked.value = true // 초기화
                Log.d("song 넘어가기전(Viewer로) position : ", ""+position)
                mainViewPagerAdapter.notifyDataSetChanged()
            }
        })

        val recyclerView = binding.recyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = RecyclerViewAdapter(jpegViewModel.imageUriLiveData.value!!)



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
                CoroutineScope(Dispatchers.Default).launch {
                    Thread.sleep(1500)
                    CoroutineScope(Dispatchers.Main).launch{
                        val bundle = Bundle()
                        bundle.putInt("currentPosition",mainViewPagerAdapter.getCurrentItemPosition())
                        Log.d("song 넘어가는 position : ", ""+mainViewPagerAdapter.getCurrentItemPosition())
                        findNavController().navigate(R.id.action_basicViewerFragment_to_viewerFragment,bundle)
                        isFinished.value = false
                    }
                }
            }
        }

        /** Button 이벤트 리스너 - editBtn, backBtn, textBtn*/
        binding.analyzeBtn.setOnClickListener{
            binding.viewPager2.setUserInputEnabled(false);
            it.visibility = View.INVISIBLE
            binding.analyzeLinear.visibility = View.VISIBLE
            Glide.with(this).load(R.raw.loading).into(binding.loadingImageView)
            /*Jpeg Container, Viewer의 ScrollView Picture ByteArr 생성 */
            setCurrentMCContainer(mainViewPagerAdapter.getCurrentItemPosition())
        }

        binding.backBtn.setOnClickListener{
            Glide.get(requireContext()).clearMemory()
            findNavController().navigate(R.id.action_basicViewerFragment_to_galleryFragment)
        }

        isPictureClicked.observe(requireActivity()){
            if (isPictureClicked.value == true){
                binding.analyzeBtn.visibility = View.VISIBLE
            }
            else {
                binding.analyzeBtn.visibility = View.INVISIBLE
            }
        }
    }

    /** MCContainer 변경 */
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

    /** 숨겨진 이미지들 imageView로 ScrollView에 추가 */
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