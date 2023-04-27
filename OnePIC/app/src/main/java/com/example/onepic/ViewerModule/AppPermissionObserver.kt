package com.example.onepic.ViewerModule

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings

import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.example.onepic.CameraModule.CameraEditorActivity


class AppPermissionObserver(private val activity: ViewerEditorActivity) : LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            if (Environment.isExternalStorageManager()) {
                ViewerEditorActivity.PERMMISION_CHECK = false
                // 권한이 이미 허용됨
                getAllPhotos() // 갤러리에서 photo 가져오기

            } else {
                if(!ViewerEditorActivity.PERMMISION_CHECK){
                    ViewerEditorActivity.PERMMISION_CHECK = true
                    // 권한 요청
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.addCategory("android.intent.category.DEFAULT")
                    intent.data = Uri.parse("package:" + activity.applicationContext.packageName)
                    activity.startActivityForResult(intent, 2296)
                }
                else {
                    ViewerEditorActivity.PERMMISION_CHECK = false // 초기화
                    navigateToCameraActivity()
                }
            }
        } else {
            // Android 10 이하 버전에서는 WRITE_EXTERNAL_STORAGE 권한만 요청하면 됨
            if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
                // 권한이 이미 허용됨
                getAllPhotos() // 갤러리 이미지 uri 가져오기

            } else {
                ActivityCompat.requestPermissions(activity,
                    arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
            }
        }
    }

    private fun getAllPhotos(){ // 이미지는 glider 써서 불러와야함
        val cursor = activity.contentResolver.query(
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
            activity.jpegViewModels.updateImageUriData(uriList)

        }
    }

    fun navigateToCameraActivity(){ // camera Activity로 이동
        val intent = Intent(activity.getApplicationContext(), CameraEditorActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags (Intent.FLAG_ACTIVITY_NO_ANIMATION);
        activity.startActivity(intent)
    }

}








