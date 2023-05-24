package com.goldenratio.onepic.ViewerModule.Fragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.goldenratio.onepic.JpegViewModel
import com.goldenratio.onepic.R
import com.goldenratio.onepic.ViewerModule.Adapter.BasicViewerAdapter
import com.goldenratio.onepic.ViewerModule.Adapter.RecyclerViewAdapter
import com.goldenratio.onepic.ViewerModule.ViewerEditorActivity
import com.goldenratio.onepic.databinding.FragmentBasicViewerBinding


@SuppressLint("LongLogTag")
@RequiresApi(Build.VERSION_CODES.M)
class BasicViewerFragment : Fragment() {

    private lateinit var callback: OnBackPressedCallback

    private lateinit var binding: FragmentBasicViewerBinding
    private val jpegViewModel by activityViewModels<JpegViewModel>()
    private lateinit var mainViewPagerAdapter: BasicViewerAdapter
    private lateinit var recyclerViewAdapter:RecyclerViewAdapter


    var centerItemPosition:Int? = null // recyclerView의 중앙 item idx


    // 순환적으로 스크롤 동작하는것 제어 하는 변수
    var isUserScrolling = false

    companion object {
        var currentFilePath:String = ""
        var isPictureClicked: MutableLiveData<Boolean> = MutableLiveData(true)
        var isClickedRecyclerViewImage:MutableLiveData<Boolean> = MutableLiveData(false)
        var currentPosition:Int? = null // gallery fragment 에서 넘어올 때
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
        binding = FragmentBasicViewerBinding.inflate(inflater, container, false)
        currentPosition = arguments?.getInt("currentPosition") // 갤러리 프래그먼트에서 넘어왔을 때

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewerFragment.currentFilePath = ""

        init()

        isClickedRecyclerViewImage.observe(requireActivity()){ value ->
            if (value){
                binding.viewPager2.setCurrentItem(currentPosition!!, false)
                binding.recyclerView.scrollToPosition(currentPosition!!)
                isClickedRecyclerViewImage.value = false
                currentPosition = null
            }
        }

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

            recyclerViewAdapter.updateData(jpegViewModel.imageUriLiveData.value!!)//setRecyclerViewItem(jpegViewModel.imageUriLiveData.value!!)


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

    override fun onDetach() {
        super.onDetach()
        callback.remove()
    }

    /** ViewPager Adapter 및 swipe callback 설정 & Button 이벤트 처리 */
    @SuppressLint("NewApi")
    fun init() {

        mainViewPagerAdapter = BasicViewerAdapter(requireContext())
        mainViewPagerAdapter.setUriList(jpegViewModel.imageUriLiveData.value!!)

        binding.viewPager2.adapter = mainViewPagerAdapter
        binding.viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {

            override fun onPageScrollStateChanged(state: Int) {
                if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                    isUserScrolling = true
                } else if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    isUserScrolling = false
                }
                super.onPageScrollStateChanged(state)
            }

            @RequiresApi(Build.VERSION_CODES.Q)
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                isPictureClicked.value = true // 초기화

                // UI 스레드에서 notifyDataSetChanged() 호출
                Handler(Looper.getMainLooper()).post {
                    mainViewPagerAdapter.notifyDataSetChanged()
                }


                if (isUserScrolling){
                    // 리사이클러뷰를 중앙에 오도록 해당 인덱스로 스크롤
                    binding.recyclerView.scrollToPosition(position)
                }

                isUserScrolling = false // 초기화

            }

        })


        val recyclerView = binding.recyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        recyclerViewAdapter = RecyclerViewAdapter(jpegViewModel.imageUriLiveData.value!!,
            recyclerView.layoutManager as LinearLayoutManager
        )
        recyclerView.adapter = recyclerViewAdapter//RecyclerViewAdapter(jpegViewModel.imageUriLiveData.value!!)


        // 리사이클러뷰를 가운데에 배치하고 시작과 끝에 여분의 공간을 추가합니다.
        val screenWidth = resources.displayMetrics.widthPixels
        val itemWidth = screenWidth / 2 // 화면 너비의 절반으로 아이템 크기 설정


        val paddingStart = pxToDp(requireContext(),((screenWidth - itemWidth) / 2 + 170).toFloat()).toInt()//tab 에서 되는 코드 -> dpToPx(170f,requireContext())
        recyclerView.setPaddingRelative(paddingStart, 0, paddingStart, 0)
        recyclerView.clipToPadding = false


        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            var previousCenterItemPosition: Int = -1

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    isUserScrolling = true
                } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    isUserScrolling = false
//                    val viewHolder = recyclerView.findViewHolderForAdapterPosition(centerItemPosition!!)
//                    if (viewHolder != null && viewHolder is RecyclerViewAdapter.ViewHolder) {
//                        val imageView = viewHolder.imageView
//                        if ( viewHolder.adapterPosition == centerItemPosition) {
//                            // TODO: 포커스 주는 코드
//                            imageView.requestFocus()
//                        }
//                    }
                }
                super.onScrollStateChanged(recyclerView, newState)
            }


            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                centerItemPosition = (firstVisibleItemPosition + lastVisibleItemPosition) / 2

                if (centerItemPosition != previousCenterItemPosition) {
                    // 중앙 아이템이 변경되었을 때의 처리
                    val previousViewHolder =
                        recyclerView.findViewHolderForAdapterPosition(previousCenterItemPosition)
                    if (previousViewHolder != null && previousViewHolder is RecyclerViewAdapter.ViewHolder) {
                        val previousImageView = previousViewHolder.imageView
                        // 중앙 아이템이 아닌 경우의 처리
                        previousImageView.background = null
                        previousImageView.setPadding(0, 0, 0, 0)
                    }

                    val currentViewHolder = recyclerView.findViewHolderForAdapterPosition(centerItemPosition!!)
                    if (currentViewHolder != null && currentViewHolder is RecyclerViewAdapter.ViewHolder) {
                        val currentImageView = currentViewHolder.imageView
                        // 중앙 아이템에 대한 처리
                        currentImageView.setBackgroundResource(R.drawable.chosen_image_border)
                        currentImageView.setPadding(6, 6, 6, 6)
                    }

                    previousCenterItemPosition = centerItemPosition!!
                }


                if (isUserScrolling){
                    binding.viewPager2.setCurrentItem(centerItemPosition!!, false)
                }

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
            backPressed()
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

    fun pxToDp(context: Context, px: Float): Float {
        val resources = context.resources
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_PX,
            px,
            resources.displayMetrics
        )
    }

    fun backPressed(){
        findNavController().navigate(R.id.galleryFragment,null, NavOptions.Builder()
            .setPopUpTo(R.id.basicViewerFragment, true)
            .build())
    }
}