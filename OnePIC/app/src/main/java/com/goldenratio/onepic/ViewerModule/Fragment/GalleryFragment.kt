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
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.goldenratio.onepic.CameraModule.CameraEditorActivity
import com.goldenratio.onepic.JpegViewModel
import com.goldenratio.onepic.R
import com.goldenratio.onepic.ViewerModule.Adapter.GridAdapter
import com.goldenratio.onepic.ViewerModule.ViewerEditorActivity
import com.goldenratio.onepic.databinding.FragmentGalleryBinding


class GalleryFragment : Fragment() {

    private lateinit var callback: OnBackPressedCallback

    private lateinit var binding: FragmentGalleryBinding
    private val jpegViewModel by activityViewModels<JpegViewModel>()
    private lateinit var gridAdapter: GridAdapter


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

        gridAdapter = GridAdapter(this, requireContext())
        gridAdapter.setItems(jpegViewModel.imageUriLiveData.value!!)


        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("OnViewCreated",": gallery fragment")
        binding.gridView.numColumns = 3 // 갤러리 이미지 3개씩 보이기
        binding.gridView.adapter = gridAdapter

        ViewerFragment.currentFilePath = ""
        BasicViewerFragment.currentFilePath =""

        jpegViewModel.imageUriLiveData.observe(viewLifecycleOwner){

            if (jpegViewModel.imageUriLiveData.value!!.size == 0){ // 갤러리에 이미지가 아무것도 없을 때
                binding.emptyLinearLayout.visibility = View.VISIBLE
                binding.gridView.visibility = View.GONE
            }
            else {
                binding.emptyLinearLayout.visibility = View.GONE
                binding.gridView.visibility = View.VISIBLE
            }

            gridAdapter.setItems(jpegViewModel.imageUriLiveData.value!!)
        }

        binding.backBtn.setOnClickListener{ //Camera Activity로 이동
          backPressed()
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