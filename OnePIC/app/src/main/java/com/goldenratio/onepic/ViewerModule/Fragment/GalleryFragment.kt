package com.goldenratio.onepic.ViewerModule.Fragment


import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.goldenratio.onepic.CameraModule.CameraEditorActivity
import com.goldenratio.onepic.ImageToolModule
import com.goldenratio.onepic.JpegViewModel
import com.goldenratio.onepic.R
import com.goldenratio.onepic.ViewerModule.Adapter.RecyclerViewGridAdapter
import com.goldenratio.onepic.databinding.FragmentGalleryBinding


class GalleryFragment : Fragment() {

    private lateinit var callback: OnBackPressedCallback

    private lateinit var binding: FragmentGalleryBinding
    private val jpegViewModel by activityViewModels<JpegViewModel>()
    private var imageTool = ImageToolModule()
    private lateinit var recyclerViewAdapter:RecyclerViewGridAdapter
    private var currentPosition = MutableLiveData(0) // 매직픽쳐 관련 작업 수행 완료

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

        binding = FragmentGalleryBinding.inflate(inflater, container, false)

        val curr = arguments?.getInt("currentPosition")
        if (curr != null) {
            currentPosition.value = curr
        }

        currentPosition.observe(viewLifecycleOwner){ position ->
            if (position != null){
                val value = jpegViewModel.imageUriLiveData.value!!.get(position)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                    Glide.with(this)
                        .load(value)
                        .into(binding.selectedImageView)
                }
                else {
                    Glide.with(this)
                        .load(imageTool.getUriFromPath(this.requireContext(),value))
                        .into(binding.selectedImageView)
                }
            }
        }

        val recyclerView: RecyclerView = binding.recyclerView
        val layoutManager = GridLayoutManager(this.requireContext(), 4) // 4열의 그리드 레이아웃
        recyclerView.layoutManager = layoutManager

        recyclerViewAdapter = RecyclerViewGridAdapter(this.requireContext(), currentPosition)
        recyclerViewAdapter.setItem(jpegViewModel.imageUriLiveData.value!!)
        recyclerView.adapter = recyclerViewAdapter

        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("OnViewCreated",": gallery fragment")

        ViewerFragment.currentFilePath = ""

        jpegViewModel.imageUriLiveData.observe(viewLifecycleOwner){

            if (jpegViewModel.imageUriLiveData.value!!.size == 0){ // 갤러리에 이미지가 아무것도 없을 때
                binding.emptyLinearLayout.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
            }
            else {
                binding.emptyLinearLayout.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
            }
            recyclerViewAdapter.setItem(jpegViewModel.imageUriLiveData.value!!)
            currentPosition.value = currentPosition.value // 삭제된 사진에 대해서도 메인 이미지 바뀔수 있도록
        }

        binding.backBtn.setOnClickListener{ //Camera Activity로 이동
            backPressed()
        }

        binding.selectedImageView.setOnClickListener{
            if (binding.analyzeBtn.visibility == View.VISIBLE){
                binding.analyzeBtn.visibility = View.INVISIBLE
            }
            else {
                binding.analyzeBtn.visibility = View.VISIBLE
            }
        }

        binding.analyzeBtn.setOnClickListener{
            val bundle = Bundle()
            bundle.putInt("currentPosition",currentPosition.value!!)
            this.findNavController().navigate(R.id.action_galleryFragment_to_analyzeFragment,bundle)
        }

        binding.recyclerView.post {
            recyclerViewAdapter.performClickOnItem(currentPosition.value!!)
        }

    }

    override fun onDetach() {
        super.onDetach()
        callback.remove()
    }

    fun backPressed(){
        val intent = Intent(activity, CameraEditorActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags (Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent)
    }
}