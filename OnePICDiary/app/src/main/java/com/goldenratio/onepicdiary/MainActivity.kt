package com.goldenratio.onepicdiary

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.goldenratio.onepic.JpegViewModel
import com.goldenratio.onepic.JpegViewModelFactory
import com.goldenratio.onepic.PictureModule.MCContainer
import com.goldenratio.onepicdiary.databinding.ActivityMainBinding
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var jpegViewModelFactory: JpegViewModelFactory
    lateinit var jpegViewModels: JpegViewModel

    private val RECORD_AUDIO_PERMISSION_REQUEST_CODE = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /* ViewModel 생성 및 설정 */
        jpegViewModelFactory = JpegViewModelFactory(this)
        jpegViewModels = ViewModelProvider(this, jpegViewModelFactory)[JpegViewModel::class.java]


        val MCContainer = MCContainer(this)
        jpegViewModels.setContainer(MCContainer)


        checkRecordAudioPermission()
    }

    // RECORD_AUDIO 권한을 체크하고 요청하는 함수
    private fun checkRecordAudioPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // 권한이 없는 경우 권한을 요청
            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO_PERMISSION_REQUEST_CODE
            )
        } else {

        }
    }

    private fun checkPermissions() {
//        val permission = android.Manifest.permission.WRITE_EXTERNAL_STORAGE
//        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
//            Log.d("save_test", "권한 요청")
//            ActivityCompat.requestPermissions(this,
//                arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
//        } else {
//            // 권한이 이미 허용되었을 경우
//            // 필요한 처리를 진행하세요
//        }
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

            } else {
                ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == 1) {
//            if (resultCode == Activity.RESULT_OK) {
//                // 사용자가 승인한 경우 삭제 진행
//                Log.d("save_test", "사용자 허가를 받고 다시 진행")
//                jpegViewModels.jpegMCContainer.value?.saveResolver!!.deleteImage(jpegViewModels.currentUri!!, jpegViewModels.currentFileName)
//            } else {
//                // 사용자가 거부한 경우 또는 오류가 발생한 경우
//                // 예외 처리
//            }
//        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == REQUEST_EXTERNAL_STORAGE_PERMISSION) {
//            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                Log.d("save_test", "권한 요청 허용")
//                // 권한이 허용된 경우
//                // 필요한 처리를 진행하세요
//            } else {
//                Log.d("save_test", "권한 요청 거부")
//                // 권한이 거부된 경우
//                // 필요한 처리를 진행하세요
//            }
//        }
    }
}