package com.goldenratio.onepic.SaveModule

import android.annotation.SuppressLint
import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import com.goldenratio.onepic.JpegViewModel
import kotlinx.coroutines.*
import java.io.*
import java.nio.ByteBuffer


class SaveResolver(_mainActivity: Activity) {
    private var mainActivity: Activity

    init {
        mainActivity = _mainActivity
    }

    /**
     * TODO 촬영 후 파일 저장
     *
     * @param isSaved
     * @param resultByteArray 저장할 데이터
     */
     fun save(resultByteArray : ByteArray, fileName: String?) : String{
        val finalFileName = fileName ?: System.currentTimeMillis().toString() + ".jpg" // 파일 이름 현재 시간.jpg
        var savedUri = ""
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //saveJPEG(resultByteArray, isSaved)
            //Q 버전 이상일 경우. (안드로이드 10, API 29 이상일 경우)
            savedUri = saveImageOnAboveAndroidQ(resultByteArray, finalFileName)

        }
        return savedUri
    }

//
//    /**
//     * TODO 편집 후 파일 저장
//     *
//     * @param fileName
//     * @param isBurst
//     */
//     fun overwriteSave(fileName: String, resultByteArray : ByteArray) {
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            //Q 버전 이상일 경우. (안드로이드 10, API 29 이상일 경우)
//             saveImageOnAboveAndroidQ(resultByteArray, fileName, null)
//        } else {
//            saveJPEG(resultByteArray, null)
//        }
//    }

//
//    fun byteArrayToBitmap(byteArray: ByteArray): Bitmap {
//        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
//    }

    //Android Q (Android 10, API 29 이상에서는 이 메서드를 통해서 이미지를 저장한다.)
    @RequiresApi(Build.VERSION_CODES.Q)
    fun saveImageOnAboveAndroidQ(byteArray: ByteArray, fileName : String) : String {

        val indexOfDot = fileName.lastIndexOf(".")
        val fileNameWithoutExtension = if (indexOfDot > 0) fileName.substring(0, indexOfDot) else fileName
        Log.d("save_test", "저장 함수 파일 이름 : ${fileNameWithoutExtension}")

        println(fileNameWithoutExtension)
        var uri : Uri
        Log.d("Picture Module", "이미지 저장 함수 :saveImageOnAboveAndroidQ 111")
        // 기존 파일이 존재하는지 확인합니다.
        val selection = "${MediaStore.Images.Media.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(fileNameWithoutExtension)
        val cursor = mainActivity.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            null,
            selection,
            selectionArgs,
            null
        )
        Log.d("savedFilePath", "delete File Path = $fileNameWithoutExtension")

        val values = ContentValues()
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/ImageSave")
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        Log.d("saveResolver", "새 파일에 저장")
        uri= mainActivity.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!

        try {
            val outputStream: OutputStream? = uri?.let {
                mainActivity.getContentResolver().openOutputStream(
                    it
                )
            }
            if (outputStream != null) {
                outputStream.write(byteArray)
                outputStream.close()
            }

        } catch(e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return uri.toString()
    }
//    //Android Q (Android 10, API 29 이상에서는 이 메서드를 통해서 이미지를 저장한다.)
//    @SuppressLint("Range", "Recycle")
//    @RequiresApi(Build.VERSION_CODES.Q)
//    fun saveImageOnAboveAndroidQ (
//        byteArray: ByteArray,
//        fileName: String
//    ): String {
//        var uri: Uri
//
//        /* 새로운 파일 저장 */
//        val values = ContentValues()
//        values.put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/ImageSave")
//        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
//        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
//
//        uri = mainActivity.contentResolver.insert(
//            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
//            values
//        )!!
//
//       // ViewerFragment.currentFilePath = uri.toString()
//
//        val outputStream: OutputStream? = uri?.let {
//            mainActivity.contentResolver.openOutputStream(it)
//        }
//
//        if (outputStream != null) {
//            outputStream.write(byteArray)
//            outputStream.flush()
//            Thread.sleep(100) // 약간의 딜레이
//            outputStream.close()
//        }
//
//       return uri.toString()
//
//    }



    @RequiresApi(Build.VERSION_CODES.Q)
    fun deleteImage(fileName: String) {
        // 이미지를 조회하기 위한 쿼리
        val selection = "${MediaStore.Images.Media.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(fileName)
        val contentResolver = mainActivity.contentResolver
        val queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        // 이미지 조회
        val cursor = contentResolver.query(queryUri, null, selection, selectionArgs, null)
        cursor?.use { cursor ->
            if (cursor.moveToFirst()) {
                try {
                    // 이미지가 존재하는 경우
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val imageUri = ContentUris.withAppendedId(queryUri, cursor.getLong(idColumn))

                    // 이미지 삭제
                    val deletedRows = contentResolver.delete(imageUri, null, null)
                    if (deletedRows > 0) {
                        Log.d("save_test", "이미지 삭제 성공")
                    } else {
                        Log.d("save_test", "이미지 삭제 실패")
                    }
                   // JpegViewModel.isUserInentFinish = true

                } catch (e: SecurityException) {
                    // 사용자 요청 메시지를 보냄
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && e is RecoverableSecurityException) {
                        val intentSender = e.userAction.actionIntent.intentSender
                        mainActivity.startIntentSenderForResult(
                            intentSender,
                            1,
                            null,
                            0,
                            0,
                            0,
                            null
                        )
                        Log.d("save_test", "삭제 중 예외 처리 (우리 앱 사진이 아님)")
                    } else {
                        // 예외 처리
                        Log.d("save_test", "삭제 중 예외 처리 (우리 앱 사진이 아님) && 버전 13이하")
                    }

                }

            } else {
                // 이미지가 존재하지 않는 경우
                Log.d("save_test", "이미지가 존재 하지 않음")
                //JpegViewModel.isUserInentFinish = true

            }
        }
    }


    fun saveJPEG(byteArray: ByteArray, isSaved: MutableLiveData<Uri>?) {
        val fileName = System.currentTimeMillis().toString() + ".jpg" // 현재 시간
        val externalStorage = Environment.getExternalStorageDirectory().absolutePath
        val path = "$externalStorage/DCIM/imageSave"
        val dir = File(path)
        if (dir.exists().not()) {
            dir.mkdirs() // 폴더 없을경우 폴더 생성
        }
        try {
            val fos = FileOutputStream("$dir/$fileName")
            fos.write(byteArray) // ByteArray의 이미지 데이터를 파일에 쓰기
            fos.close()

            // 미디어 스캐닝을 통해 갤러리에 이미지를 등록
            MediaScannerConnection.scanFile(mainActivity, arrayOf("$dir/$fileName"), null) { _, uri ->
                // 미디어 스캐닝이 끝났을 때의 동작을 여기에 작성
                CoroutineScope(Dispatchers.Main).launch {
                    isSaved?.value = uri
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

//
//     fun singleImageSave(picture: Picture) {
//        val byteBuffer = ByteArrayOutputStream()
//        var newJpegMetaData : ByteArray? = null
//        var jpegMetaData = AiContainer.imageContent.jpegHeader
//        newJpegMetaData = jpegMetaData
//
//        // 메타 데이터 변경
//        if(AiContainer.isBurst){
//            newJpegMetaData = jpegMetaData
//            Log.d("version3", "단일 사진 저장할 때 메타데이터 변경 안하고 저장")
//        }else{
//            newJpegMetaData = AiContainer.imageContent.changeAPP1MetaData(picture._app1Segment!!)
//            Log.d("version3", "단일 사진 저장할 때 메타데이터 변경 하고 저장")
//        }
//
//        byteBuffer.write(newJpegMetaData, 0, newJpegMetaData.size)
//        byteBuffer.write(picture._pictureByteArray)
//        byteBuffer.write(0xff)
//        byteBuffer.write(0xd9)
//
//        val singleJpegBytes = byteBuffer.toByteArray()
//       // AiContainer.exploreMarkers(singleJpegBytes)
//        val fileName = System.currentTimeMillis().toString() + ".jpg" // 파일이름 현재시간.jpg
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            //Q 버전 이상일 경우. (안드로이드 10, API 29 이상일 경우)
//            saveImageOnAboveAndroidQ(singleJpegBytes, fileName, null)
//        } else {
//            // Q 버전 이하일 경우. 저장소 권한을 얻어온다.
//            lowSDKVersionSave(singleJpegBytes, null)
//        }
//    }







//    fun lowSDKVersionSave(resultByteArray: ByteArray, isSaved: MutableLiveData<Uri>?){
//        // Q 버전 이하일 경우. 저장소 권한을 얻어온다.
//        val writePermission = mainActivity?.let {
//            ActivityCompat.checkSelfPermission(
//                it,
//                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
//            )
//        }
//        if (writePermission == PackageManager.PERMISSION_GRANTED) {
//            saveImageOnUnderAndroidQ(resultByteArray)
//
//        } else {
//            val requestExternalStorageCode = 1
//            val permissionStorage = arrayOf(
//                android.Manifest.permission.READ_EXTERNAL_STORAGE,
//                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
//            )
//
//            ActivityCompat.requestPermissions(
//                mainActivity as Activity,
//                permissionStorage,
//                requestExternalStorageCode
//            )
//        }
//    }
}