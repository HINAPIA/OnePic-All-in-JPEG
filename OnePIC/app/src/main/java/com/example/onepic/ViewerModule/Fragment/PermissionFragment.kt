package com.example.onepic.ViewerModule.Fragment


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import com.example.onepic.JpegViewModel
import com.example.onepic.R
import com.example.onepic.ViewerModule.ViewerEditorActivity.Companion.LAUNCH_ACTIVITY
import com.example.onepic.databinding.FragmentPermissionBinding


class PermissionFragment : Fragment(){ // 권한 허용을 받는 동안 머무는 Fragment

    private lateinit var binding: FragmentPermissionBinding
    private val jpegViewModel by activityViewModels<JpegViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPermissionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        jpegViewModel.imageUriLiveData.observe(requireActivity()){
            if (LAUNCH_ACTIVITY){ // 처음 Activity가 launch 되었을 때만 실행
                LAUNCH_ACTIVITY = false
                val navController = requireActivity().findNavController(R.id.framelayout)
                navController.navigate(R.id.galleryFragment) // Gallery Fragment 로 전환
            }
        }
    }
}