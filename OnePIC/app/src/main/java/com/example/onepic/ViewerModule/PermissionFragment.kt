package com.example.onepic.ViewerModule


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.impl.utils.ContextUtil.getApplicationContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.navigation.findNavController
import com.example.onepic.CameraModule.CameraEditorActivity
import com.example.onepic.JpegViewModel
import com.example.onepic.R
import com.example.onepic.ViewerModule.ViewerEditorActivity.Companion.LAUNCH_ACTIVITY
import com.example.onepic.databinding.FragmentGalleryBinding
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
            if (LAUNCH_ACTIVITY){
                LAUNCH_ACTIVITY = false
                val navController = requireActivity().findNavController(R.id.framelayout)
                navController.navigate(R.id.galleryFragment) // Gallery Fragment 로 전환
            }
        }
    }
}