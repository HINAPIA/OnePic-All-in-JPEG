package com.goldenratio.onepic.ViewerModule.Fragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.goldenratio.onepic.JpegViewModel
import com.goldenratio.onepic.R
import com.goldenratio.onepic.ViewerModule.Adapter.BasicViewerAdapter
import com.goldenratio.onepic.ViewerModule.Adapter.RecyclerViewAdapter
import com.goldenratio.onepic.databinding.FragmentBasicViewerBinding


@SuppressLint("LongLogTag")
@RequiresApi(Build.VERSION_CODES.M)
class BasicViewerFragment : Fragment() {

    private lateinit var binding: FragmentBasicViewerBinding
    private val jpegViewModel by activityViewModels<JpegViewModel>()
    private lateinit var mainViewPagerAdapter: BasicViewerAdapter

    private var currentPosition:Int? = null // gallery fragment 에서 넘어올 때
    private var centerItemPosition:Int? = null // recyclerView의 중앙 item idx
    companion object {
        var currentFilePath:String = ""
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

                mainViewPagerAdapter.notifyDataSetChanged()

                // 리사이클러뷰를 중앙에 오도록 해당 인덱스로 스크롤
                binding.recyclerView.scrollToPosition(position)
            }
        })

        val recyclerView = binding.recyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = RecyclerViewAdapter(jpegViewModel.imageUriLiveData.value!!)


        // 리사이클러뷰를 가운데에 배치하고 시작과 끝에 여분의 공간을 추가합니다.
        val screenWidth = resources.displayMetrics.widthPixels
        val itemWidth = screenWidth / 2 // 화면 너비의 절반으로 아이템 크기 설정


        val paddingStart = (screenWidth - itemWidth) / 2 + 170//tab 에서 되는 코드 -> dpToPx(170f,requireContext())
        recyclerView.setPaddingRelative(paddingStart, 0, paddingStart, 0)
        recyclerView.clipToPadding = false


        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                centerItemPosition = (firstVisibleItemPosition + lastVisibleItemPosition) / 2

                binding.viewPager2.setCurrentItem(centerItemPosition!!, false)

                super.onScrolled(recyclerView, dx, dy)
            }
        })


        /** Button 이벤트 리스너 - editBtn, backBtn, textBtn*/
        binding.analyzeBtn.setOnClickListener{
            binding.viewPager2.setUserInputEnabled(false);
            it.visibility = View.INVISIBLE
            val bundle = Bundle()
            bundle.putInt("currentPosition",centerItemPosition!!)
            findNavController().navigate(R.id.action_basicViewerFragment_to_analyzeFragment,bundle)
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

    private fun dpToPx(dp: Float, context: Context): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }

}