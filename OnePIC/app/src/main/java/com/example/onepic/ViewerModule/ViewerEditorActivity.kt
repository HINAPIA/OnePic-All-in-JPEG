package com.example.onepic.ViewerModule

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.example.onepic.CameraModule.CameraEditorActivity

import com.example.onepic.LoadModule.LoadResolver
import com.example.onepic.PictureModule.MCContainer

import com.example.onepic.JpegViewModel
import com.example.onepic.JpegViewModelFactory
import com.example.onepic.R
import com.example.onepic.databinding.ActivityViewerEditorBinding


@SuppressLint("LongLogTag")
class ViewerEditorActivity : AppCompatActivity() {

    private lateinit var binding : ActivityViewerEditorBinding
    private lateinit var jpegViewModelFactory: JpegViewModelFactory
    lateinit var jpegViewModels: JpegViewModel
    private lateinit var appPermissionObserver: AppPermissionObserver // permission check observer
    companion object {
        var PERMMISION_CHECK = false // Permission check 를 한 적이 있는지
        var LAUNCH_ACTIVITY = true // Activity가 Launch 되었는가(Lanch된 직후인가)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewerEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        LAUNCH_ACTIVITY = true

        /* ViewModel 생성 및 설정 */
        jpegViewModelFactory = JpegViewModelFactory(this)
        jpegViewModels = ViewModelProvider(this, jpegViewModelFactory).get(JpegViewModel::class.java)

        var MCContainer : MCContainer = MCContainer(this)
        jpegViewModels.setContainer(MCContainer)

        /* 권한 요청 - Register the AppPermissionObserver */
        appPermissionObserver = AppPermissionObserver(this)
        lifecycle.addObserver(appPermissionObserver)

    }


    override fun onRequestPermissionsResult( // 권한 요청 처리
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // TODO: 권한이 허용되었을 때의 처리
                    getAllPhotos() // 갤러리 이미지 uri 가져오기

                } else {
                    // TODO: 권한이 거부되었을 때의 처리
                    Log.d("[ViewerEditorActivity] 권한 요청 결과: ", "거부됨")
                    val intent = Intent(getApplicationContext(), CameraEditorActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags (Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    startActivity(intent)
                }
                return
            }
        }
    }

    private fun getAllPhotos(){ // 사진 경로(File Path) 가져오기
        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            null,
            null,
            null,
            MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC")

        val uriList = mutableListOf<String>()

        if(cursor!=null){
            while(cursor.moveToNext()){
                // 사진 경로 FilePath 가져오기
                val uri = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
                uriList.add(uri)
            }
            cursor.close()
            jpegViewModels.updateImageUriData(uriList)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove the AppPermissionObserver
        lifecycle.removeObserver(appPermissionObserver)
    }

}