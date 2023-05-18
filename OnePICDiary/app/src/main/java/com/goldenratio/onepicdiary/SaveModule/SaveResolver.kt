package com.goldenratio.onepic.SaveModule

import android.annotation.SuppressLint
import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startIntentSenderForResult
import com.goldenratio.onepic.PictureModule.MCContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.*


class SaveResolver(_mainActivity: Activity, _MC_Container: MCContainer) {
    private var MCContainer : MCContainer
    private var mainActivity : Activity
    init{
        MCContainer = _MC_Container
        mainActivity = _mainActivity
    }


    fun getContentIdFromUri(uri: Uri): Long {
        val id = uri.lastPathSegment
        return id?.toLongOrNull() ?: -1
    }
    fun overwriteSave(fileName : String): String{
        var savedFile : String = ""
        Log.d("burst", "overwirte save()")
      //  CoroutineScope(Dispatchers.IO).launch {
            var resultByteArray = MCContainerToBytes()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                //Q 버전 이상일 경우. (안드로이드 10, API 29 이상일 경우)
                savedFile = saveImageOnAboveAndroidQ(resultByteArray, fileName)
                Log.d("Save Resolver", "save")
            } else {
                // Q 버전 이하일 경우. 저장소 권한을 얻어온다.
                val writePermission = mainActivity?.let {
                    ActivityCompat.checkSelfPermission(
                        it,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                }
                if (writePermission == PackageManager.PERMISSION_GRANTED) {

                    savedFile = saveImageOnUnderAndroidQ(resultByteArray)

                    // Toast.makeText(context, "이미지 저장이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    val requestExternalStorageCode = 1

                    val permissionStorage = arrayOf(
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )

                    ActivityCompat.requestPermissions(
                        mainActivity as Activity,
                        permissionStorage,
                        requestExternalStorageCode
                    )
                }
            }
        return savedFile
      //  }

    }

    fun save() : String{
        var savedFile : String = ""
//        CoroutineScope(Dispatchers.IO).launch {
            var resultByteArray = MCContainerToBytes()
            val fileName = System.currentTimeMillis().toString() + ".jpg" // 파일이름 현재시간.jpg

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                //Q 버전 이상일 경우. (안드로이드 10, API 29 이상일 경우)
                savedFile = saveImageOnAboveAndroidQ(resultByteArray, fileName)
                Log.d("Save Resolver", "save")
            } else {
                // Q 버전 이하일 경우. 저장소 권한을 얻어온다.
                val writePermission = mainActivity?.let {
                    ActivityCompat.checkSelfPermission(
                        it,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                }
                if (writePermission == PackageManager.PERMISSION_GRANTED) {

                    savedFile = saveImageOnUnderAndroidQ(resultByteArray)

                    // Toast.makeText(context, "이미지 저장이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    val requestExternalStorageCode = 1

                    val permissionStorage = arrayOf(
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )

                    ActivityCompat.requestPermissions(
                        mainActivity as Activity,
                        permissionStorage,
                        requestExternalStorageCode
                    )
                }
            }
        return savedFile

    }

    fun byteArrayToBitmap(byteArray: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }


    fun registerImageToMediaStore(imageUri: Uri, fileName: String) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg") // 이미지 타입에 맞게 설정
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val collectionUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val itemUri = mainActivity.contentResolver.insert(collectionUri, values)

        if (itemUri != null) {
            mainActivity.contentResolver.openOutputStream(itemUri)?.use { outputStream ->
                mainActivity.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            mainActivity.contentResolver.update(itemUri, values, null, null)
        }
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    fun deleteImage(imageUri: Uri,fileName : String) {

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
                } catch (e: SecurityException) {
                    // 사용자 요청 메시지를 보냄
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && e is RecoverableSecurityException) {
                        val intentSender = e.userAction.actionIntent.intentSender
                        mainActivity.startIntentSenderForResult(intentSender, 1, null, 0, 0, 0, null)
                    } else {
                        // 예외 처리
                    }
                }
            } else {
                // 이미지가 존재하지 않는 경우
                Log.d("save_test", "이미지가 존재 하지 않음")
            }
        }

    }


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
             // 같은 파일이 이미 존재 하는 경우 덮어쓰기 모드로
             //&& cursor.moveToFirst()
//            if (cursor != null && cursor.moveToFirst()) {
//                Log.d("saveResolver", "덮어 쓰기")
//                // 기존 파일이 존재하는 경우 해당 파일의 Uri를 반환합니다.
//                val tempUri = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media._ID))
//                cursor.close()
//                uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, tempUri)
//
//                // Delete the existing file
//                val contentResolver = mainActivity.contentResolver
//                contentResolver.delete(uri, null, null)
//            }


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
       // }'
        return uri.toString()
    }

    private fun saveImageOnUnderAndroidQ(byteArray: ByteArray) :String {
        val fileName = System.currentTimeMillis().toString() + ".jpg"
        val externalStorage = Environment.getExternalStorageDirectory().absolutePath
        val path = "$externalStorage/DCIM/imageSave"
        val dir = File(path)
        val fileItem = File("$dir/$fileName")
        if(dir.exists().not()) {
            dir.mkdirs() // 폴더 없을경우 폴더 생성
        }

        try {

            fileItem.createNewFile()
            //0KB 파일 생성.

            val fos = FileOutputStream(fileItem) // 파일 아웃풋 스트림
            fos.write(byteArray)
           // bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            //파일 아웃풋 스트림 객체를 통해서 Bitmap 압축.

            fos.close() // 파일 아웃풋 스트림 객체 close

            mainActivity.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(fileItem)))
            // 브로드캐스트 수신자에게 파일 미디어 스캔 액션 요청. 그리고 데이터로 추가된 파일에 Uri를 넘겨준다.

        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return fileItem.toString()
    }

    fun MCContainerToBytes() : ByteArray{
        // 순서는 이미지 > 텍스트 > 오디오
        val byteBuffer = ByteArrayOutputStream()
        //Jpeg Meta
        var jpegMetaData = MCContainer.imageContent.jpegMetaData
        //if(firstPicture == null) throw NullPointerException("empty first Picture")
        /* APP1뒤에 APP3를 쓰는 작업*/
        // APP1 세그먼트의 시작 위치를 찾음
        var pos = 2
        var exifDataLength = 0
        while (pos < jpegMetaData.size - 1) {
            // APP1 존재
            if (jpegMetaData[pos] == 0xFF.toByte() && jpegMetaData[pos + 1] == 0xE1.toByte()) {
                exifDataLength = ((jpegMetaData[pos+2].toInt() and 0xFF) shl 8) or
                        ((jpegMetaData[pos+3].toInt() and 0xFF) shl 0)
                //SOI + APP1(EXIF) 쓰기
                byteBuffer.write(jpegMetaData,0,4 + exifDataLength)
                break
            }
            pos++
        }
        // APP1 미존재
        if (pos == jpegMetaData.size - 1) {
            // SOI 쓰기
            byteBuffer.write(jpegMetaData,0,2)
            //Log.d("test_test", "APP1 세그먼트를 찾지 못함")
        }

        //헤더 쓰기
        //App3 Extension 데이터 생성
        MCContainer.settingHeaderInfo()
        var APP3ExtensionByteArray = MCContainer.convertHeaderToBinaryData()
        byteBuffer.write(APP3ExtensionByteArray)

        //나머지 첫번째 사진의 데이터 쓰기
        byteBuffer.write(jpegMetaData,4 + exifDataLength,jpegMetaData.size-(4 + exifDataLength))
        // byteBuffer.write(jpegMetaData)

        // Imgaes write
        for(i in 0.. MCContainer.imageContent.pictureCount -1){
            var picture = MCContainer.imageContent.getPictureAtIndex(i)
            byteBuffer.write(/* b = */ picture!!._pictureByteArray)
            if(i == 0){
                //EOI 작성
                byteBuffer.write(0xff)
                byteBuffer.write(0xd9)

            }
        }

        // Audio Write
        if(MCContainer.audioContent.audio!= null){
            var audio = MCContainer.audioContent.audio
            byteBuffer.write(/* b = */ audio!!._audioByteArray)
        }
        return byteBuffer.toByteArray()
    }

}