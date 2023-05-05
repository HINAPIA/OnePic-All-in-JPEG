package com.goldenratio.onepic.ViewerModule.Fragment


import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import com.goldenratio.onepic.CameraModule.CameraEditorActivity
import com.goldenratio.onepic.JpegViewModel
import com.goldenratio.onepic.R
import com.goldenratio.onepic.ViewerModule.ViewerEditorActivity.Companion.LAUNCH_ACTIVITY
import com.goldenratio.onepic.databinding.FragmentPermissionBinding


class PermissionFragment : Fragment(){ // 권한 허용을 받는 동안 머무는 Fragment

    private lateinit var binding: FragmentPermissionBinding
    private val jpegViewModel by activityViewModels<JpegViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPermissionBinding.inflate(inflater, container, false)
        jpegViewModel.imageUriLiveData.observe(requireActivity()){
            if (LAUNCH_ACTIVITY){ // 처음 Activity가 launch 되었을 때만 실행
                LAUNCH_ACTIVITY = false
                val navController = requireActivity().findNavController(R.id.framelayout)
                navController.navigate(R.id.galleryFragment) // Gallery Fragment 로 전환
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(!LAUNCH_ACTIVITY) { // 런치 된 직후가 아니라면, 백버튼으로 해당 프래그먼트에 다시 돌아왔음을 의미
            val intent = Intent(activity, CameraEditorActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags (Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent)
        }
    }
}