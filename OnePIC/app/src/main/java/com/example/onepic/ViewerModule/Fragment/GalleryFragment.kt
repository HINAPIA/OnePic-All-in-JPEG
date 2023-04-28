package com.example.onepic.ViewerModule.Fragment


import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.onepic.CameraModule.CameraEditorActivity
import com.example.onepic.JpegViewModel
import com.example.onepic.ViewerModule.Adapter.GridAdapter
import com.example.onepic.databinding.FragmentGalleryBinding


class GalleryFragment : Fragment() {

    private lateinit var binding: FragmentGalleryBinding
    private val jpegViewModel by activityViewModels<JpegViewModel>()
    private lateinit var gridAdapter: GridAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentGalleryBinding.inflate(inflater, container, false)
        gridAdapter = GridAdapter(this, requireContext())
        gridAdapter.setItems(jpegViewModel.imageUriLiveData.value!!)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("OnViewCreated",": gallery fragment")
        binding.gridView.numColumns = 3 // 갤러리 이미지 3개씩 보이기
        binding.gridView.adapter = gridAdapter

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
            val intent = Intent(activity, CameraEditorActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags (Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent)
        }

    }
}