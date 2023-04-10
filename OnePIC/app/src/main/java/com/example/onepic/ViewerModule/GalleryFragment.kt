package com.example.onepic.ViewerModule


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.onepic.JpegViewModel
import com.example.onepic.databinding.FragmentGalleryBinding


class GalleryFragment : Fragment() {

    private lateinit var binding: FragmentGalleryBinding
    private val jpegViewModel by activityViewModels<JpegViewModel>()
    private lateinit var gridAdapter:GridAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGalleryBinding.inflate(inflater, container, false)
        gridAdapter = GridAdapter(this, requireContext(),jpegViewModel.imageUriLiveData.value!!)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.gridView.numColumns = 3
        binding.gridView.adapter = gridAdapter
    }


}