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

import com.example.camerax.LoadModule.LoadResolver
import com.example.camerax.PictureModule.MCContainer

import com.example.onepic.JpegViewModel
import com.example.onepic.R
import com.example.onepic.databinding.ActivityViewerEditorBinding


@SuppressLint("LongLogTag")
class ViewerEditorActivity : AppCompatActivity() {

    private lateinit var binding : ActivityViewerEditorBinding
    private var loadResolver : LoadResolver = LoadResolver()
    private val viewerFragment = ViewerFragment()
    private val jpegViewModels: JpegViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewerEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // 권한 요청
        requestStoragePermission()
//        // 외부저장소 읽기 권한 부여 확인
//        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
//            // 권한 요청
//            ActivityCompat.requestPermissions(this@ViewerEditorActivity,
//                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),1)
//        } else {
//            // 권한이 이미 허용됨
//            Log.d("[ViewerEditorActivity] 외부저장소 읽기 권한: ","confirm")
//            getAllPhotosURI() // 갤러리 이미지 uri 가져오기
//        }

        var MCContainer : MCContainer = MCContainer(this)
        jpegViewModels.setContainer(MCContainer)




        jpegViewModels.imageUriLiveData.observe(this){
            var size = jpegViewModels.imageUriLiveData.value?.size
            Log.d("[ViewerEditorActivity] imageUriLIst size : ",""+size)
            val uriList = mutableListOf<Uri>()
            if (size != 0){

                for (uri in jpegViewModels.imageUriLiveData.value!!){
                    Log.d("uri string : ",""+uri)
                }

                supportFragmentManager  // fragment 전환
                    .beginTransaction()
                    .replace(R.id.framelayout,viewerFragment)
                    .addToBackStack(null)
                    .commit()
            }
            else {
                // TODO: 갤러리에 사진이 아무것도 없을 때 -> Empty Fragment 만들기
                Log.d("[ViewerEditorActivity]","갤러리에 사진이 아무것도 없을 때 처리해야함")
            }
        }
    }
    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                // 권한이 이미 허용됨
                getAllPhotosURI() // 갤러리 이미지 uri 가져오기
            } else {
                // 권한 요청
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data = Uri.parse("package:" + applicationContext.packageName)
                startActivityForResult(intent, 2296)
            }
        } else {
            // Android 10 이하 버전에서는 WRITE_EXTERNAL_STORAGE 권한만 요청하면 됨
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
                // 권한이 이미 허용됨
                getAllPhotosURI() // 갤러리 이미지 uri 가져오기
            } else {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
            }
        }
    }
    private fun getAllPhotosURI(){ // 이미지는 glider 써서 불러와야함
        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            null,
            null,
            null,
            MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC")
        val uriList = mutableListOf<String>()
        if(cursor!=null){
            var count = 0
            while(cursor.moveToNext()){
                count ++
                if(count == 5) break
                // 사진 경로 Uri 가져오기
                val uri = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
                //uriList.add(getUriFromPath(uri))
                uriList.add(uri)
            }
            cursor.close()
            jpegViewModels.updateImageUriData(uriList)

        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 권한이 허용되었을 때의 처리
                    Log.d("[ViewerEditorActivity] 권한 요청 결과: ", "허용됨")
                    getAllPhotosURI() // 갤러리 이미지 uri 가져오기
                } else {
                    // 권한이 거부되었을 때의 처리
                    Log.d("[ViewerEditorActivity] 권한 요청 결과: ", "거부됨")
                }
                return
            }
        }
    }
}