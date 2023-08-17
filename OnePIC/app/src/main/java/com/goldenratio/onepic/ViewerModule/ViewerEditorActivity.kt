package com.goldenratio.onepic.ViewerModule

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.goldenratio.onepic.CameraModule.CameraEditorActivity
import com.goldenratio.onepic.JpegViewModel
import com.goldenratio.onepic.JpegViewModelFactory
import com.goldenratio.onepic.PictureModule.MCContainer
import com.goldenratio.onepic.databinding.ActivityViewerEditorBinding


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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                1
            )
        } else {
            // Android 10 이하 버전에서는 WRITE_EXTERNAL_STORAGE 권한만 요청하면 됨
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
                // 권한이 이미 허용됨
                getAllPhotos() // 갤러리 이미지 uri 가져오기

            } else {
                ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
            }
        }

        /* 권한 요청 - Register the AppPermissionObserver */
        //appPermissionObserver = AppPermissionObserver(this)
        //lifecycle.addObserver(appPermissionObserver)

    }

    // 저장 버튼을 눌렀을 때 외부 사진일 때 사용자 요청 다이얼로그를 띄우고 결과를 받음
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                // 사용자가 승인한 경우 삭제 진행
                Log.d("save_test", "사용자 허가를 받고 삭제 다시 진행")
                jpegViewModels.jpegMCContainer.value?.saveResolver!!.deleteImage( jpegViewModels.currentFileName!!)
                JpegViewModel.isUserInentFinish = true
            } else {
                // 사용자가 거부한 경우 또는 오류가 발생한 경우
                Log.d("save_test", "사용자가 거부. 삭제 안함")
                JpegViewModel.isUserInentFinish = true
            }
        }
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){

            val cursor = contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Images.Media._ID),
                null,
                null,
                "${MediaStore.Images.ImageColumns.DATE_TAKEN} DESC"
            )

            val uriList = mutableListOf<String>()

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                    val uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    ).toString()
                    uriList.add(uri)
                }
                cursor.close()
                jpegViewModels.updateImageUriData(uriList)
            }
        }
        else {
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

    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove the AppPermissionObserver
        //lifecycle.removeObserver(appPermissionObserver)
    }

}