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
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import com.goldenratio.onepic.JpegViewModel
import com.goldenratio.onepic.AllinJPEGModule.Contents.Picture
import com.goldenratio.onepic.AllinJPEGModule.AiContainer
import com.goldenratio.onepic.ViewerModule.Fragment.ViewerFragment
import kotlinx.coroutines.*
import java.io.*
import java.nio.ByteBuffer


class SaveResolver(_mainActivity: Activity, _Ai_Container: AiContainer) {
    private var AiContainer: AiContainer
    private var mainActivity: Activity

    init {
        AiContainer = _Ai_Container
        mainActivity = _mainActivity
    }

    /**
     * TODO 촬영 후 파일 저장
     *
     * @param isSaved
     * @param isBurst 연속 촬영 플래그
     * @return
     */
    suspend fun save(isSaved: MutableLiveData<Uri>, isBurst : Boolean): String {

        var savedFile: String = ""
        val resultByteArray = withContext(Dispatchers.Default) {
            AiContainerToBytes(isBurst)
        }

        //val fileName = System.currentTimeMillis().toString() + ".jpg" // 파일이름 현재시간.jpg
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //Q 버전 이상일 경우. (안드로이드 10, API 29 이상일 경우)
            saveJPEG(resultByteArray, isSaved)
        } else {
            // Q 버전 미만일 경우. (안드로이드 10, API 29 미만일 경우)
            lowSDKVersionSave(resultByteArray, isSaved)
        }
        return savedFile
    }


    /**
     * TODO 편집 후 파일 저장
     *
     * @param fileName
     * @param isBurst
     */
    suspend fun overwriteSave(fileName: String, isBurst : Boolean) {
        var savedFile: String = ""
        var resultByteArray =  withContext(Dispatchers.Default) {
            AiContainerToBytes(isBurst)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //Q 버전 이상일 경우. (안드로이드 10, API 29 이상일 경우)
             saveImageOnAboveAndroidQ(resultByteArray, fileName, null)
        } else {
            lowSDKVersionSave(resultByteArray, null)
        }
    }


    fun byteArrayToBitmap(byteArray: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }

    //Android Q (Android 10, API 29 이상에서는 이 메서드를 통해서 이미지를 저장한다.)
    @SuppressLint("Range", "Recycle")
    @RequiresApi(Build.VERSION_CODES.Q)
    fun saveImageOnAboveAndroidQ(
        byteArray: ByteArray,
        fileName: String,
        isSaved: MutableLiveData<Uri>?
    ) {
        var result: Boolean = true
        var uri: Uri

        /* 새로운 파일 저장 */
        val values = ContentValues()
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/ImageSave")
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")

        uri = mainActivity.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        )!!

        ViewerFragment.currentFilePath = uri.toString()

        val outputStream: OutputStream? = uri?.let {
            mainActivity.contentResolver.openOutputStream(it)
        }

        if (outputStream != null) {
            outputStream.write(byteArray)
            outputStream.flush()
            Thread.sleep(100) // 약간의 딜레이
            outputStream.close()
        }

        isSaved?.value = uri

    }



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
                    JpegViewModel.isUserInentFinish = true

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
                JpegViewModel.isUserInentFinish = true

            }
        }
        //JpegViewModel.isUserInentFinish = true
    }


    private fun saveImageOnUnderAndroidQ(byteArray: ByteArray): String {
        val fileName = System.currentTimeMillis().toString() + ".jpg"
        val externalStorage = Environment.getExternalStorageDirectory().absolutePath
        val path = "$externalStorage/DCIM/imageSave"
        val dir = File(path)
        val fileItem = File("$dir/$fileName")
        if (dir.exists().not()) {
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

            mainActivity.sendBroadcast(
                Intent(
                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                    Uri.fromFile(fileItem)
                )
            )
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

    suspend fun singleImageSave(picture: Picture) {
        val byteBuffer = ByteArrayOutputStream()
        var newJpegMetaData : ByteArray? = null
        var jpegMetaData = AiContainer.imageContent.jpegMetaData
        newJpegMetaData = jpegMetaData

        // 메타 데이터 변경
        if(AiContainer.isBurst){
            newJpegMetaData = jpegMetaData
            Log.d("version3", "단일 사진 저장할 때 메타데이터 변경 안하고 저장")
        }else{
            newJpegMetaData = AiContainer.imageContent.changeAPP1MetaData(picture._app1Segment!!)
            Log.d("version3", "단일 사진 저장할 때 메타데이터 변경 하고 저장")
        }
//        if(picture._app1Segment == null || picture._app1Segment!!.size <= 0)
//            newJpegMetaData = jpegMetaData
//        else{
//            if(jpegMetaData != null){
//                newJpegMetaData = AiContainer.imageContent.chageMetaData(picture._app1Segment!!)
//            }
//        }

        byteBuffer.write(newJpegMetaData, 0, newJpegMetaData.size)
        byteBuffer.write(picture._pictureByteArray)
        byteBuffer.write(0xff)
        byteBuffer.write(0xd9)

        val singleJpegBytes = byteBuffer.toByteArray()
       // AiContainer.exploreMarkers(singleJpegBytes)
        val fileName = System.currentTimeMillis().toString() + ".jpg" // 파일이름 현재시간.jpg

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //Q 버전 이상일 경우. (안드로이드 10, API 29 이상일 경우)
            saveImageOnAboveAndroidQ(singleJpegBytes, fileName, null)
        } else {
            // Q 버전 이하일 경우. 저장소 권한을 얻어온다.
            lowSDKVersionSave(singleJpegBytes, null)
        }
    }

    fun findInsertionApp3LocationAndLength(jpegMetaData : ByteArray) : Pair<Int, Int>{
        // APP3 삽입 위치 찾기 ( APP0, APP1, APP2 중 가장 늦게 나온 마커 뒤에)
        var pos = 2
        var findMarker : Boolean = false
        var lastAppMarkerOffset = 0
        var lastAppMarkerDataLength = 0
        val SOFMarkerPosList = AiContainer.imageContent.getSOFMarkerPosList(jpegMetaData)

        while (pos < jpegMetaData.size -1) {
            // APP0
            if (jpegMetaData[pos] == 0xFF.toByte() && jpegMetaData[pos + 1] == 0xE0.toByte()){
                lastAppMarkerOffset = pos; findMarker = true
                Log.d("save_test", "APP0 find :  ${lastAppMarkerOffset}")
            }
            else if (jpegMetaData[pos] == 0xFF.toByte() && jpegMetaData[pos + 1] == 0xE1.toByte()){
                lastAppMarkerOffset = pos; findMarker = true
                Log.d("save_test", "APP1 find :  ${lastAppMarkerOffset}")
            }
            else if (jpegMetaData[pos] == 0xFF.toByte() && jpegMetaData[pos + 1] == 0xE2.toByte()){
                lastAppMarkerOffset = pos; findMarker = true
                Log.d("save_test", "APP2 find :  ${lastAppMarkerOffset}")
            }
            pos++
        }

        if(findMarker){
            lastAppMarkerDataLength = ((jpegMetaData[lastAppMarkerOffset + 2].toInt() and 0xFF) shl 8) or
                    ((jpegMetaData[lastAppMarkerOffset + 3].toInt() and 0xFF) shl 0)

            // APPn 데이터 크기가 없을 때
            if(lastAppMarkerOffset + 2 + lastAppMarkerDataLength > jpegMetaData.size || lastAppMarkerDataLength == 0){
                // 마커의 크기만 지정
                lastAppMarkerDataLength = -2
            }
        } else{
            lastAppMarkerDataLength = 0
        }
        return Pair(lastAppMarkerOffset, lastAppMarkerDataLength)
    }

    fun getApp3ExtensionByteData() : ByteArray{
        // APP3 info 클래스 데이터 초기화
        AiContainer.settingHeaderInfo()
        return AiContainer.convertHeaderToBinaryData()
    }

    suspend fun AiContainerToBytes(isBurstMode : Boolean): ByteArray = coroutineScope {
        // 순서는 이미지 > 텍스트 > 오디오
        val byteBuffer = ByteArrayOutputStream()
        //Jpeg Meta
        var jpegMetaData = AiContainer.imageContent.jpegMetaData

        // 일반 JPEG으로 저장
        if (!AiContainer.isAllinJPEG) {
            Log.d("save_test", "1. 표준 JPEG으로 저장")
            byteBuffer.write(jpegMetaData, 0, jpegMetaData.size)
            var picture = AiContainer.imageContent.getPictureAtIndex(0)
            byteBuffer.write(picture!!._pictureByteArray)
            byteBuffer.write(0xff)
            byteBuffer.write(0xd9)

            // ALL in JPEG format으로 저장
        } else {
            Log.d("save_test", "2. all in jpeg으로 저장")
            // APP3 삽입 위치 찾기 ( APP0, APP1, APP2 중 가장 늦게 나온 마커 뒤에)
            val (lastAppMarkerOffset, lastAppMarkerDataLength) = findInsertionApp3LocationAndLength(jpegMetaData)


            // APPn(0,1,2) 마커를 찾음
            if(lastAppMarkerOffset != 0){
                // 마지막 APPn(0,1,2) 세그먼트 데이터까지 write
                byteBuffer.write(jpegMetaData, 0, lastAppMarkerOffset + lastAppMarkerDataLength + 2)
                // APP3 extension data 생성 후 write
                val App3ExtensionData = getApp3ExtensionByteData()
                byteBuffer.write(getApp3ExtensionByteData())
                //나머지 메타 데이터 쓰기
                byteBuffer.write(
                    jpegMetaData,
                    lastAppMarkerOffset + 2 + lastAppMarkerDataLength,
                    jpegMetaData.size - (lastAppMarkerOffset + 2 +lastAppMarkerDataLength)
                )
                Log.d("save_test", "작성한 APP3 크기 : ${App3ExtensionData.size}")
                Log.d("save_test", "나머지 메타 데이터 크기 : ${jpegMetaData.size - (lastAppMarkerOffset + lastAppMarkerDataLength + 4)}")
                Log.d("save_test", "총 작성한 메타 데이터 크기 : ${byteBuffer.size()}")
            }

            else{
                // APPn(0,1,2) 마커가 없음
                byteBuffer.write(jpegMetaData, 0, 2)
                // APP3 데이터 생성
                // APP3 extension data 생성 후 write
                byteBuffer.write(getApp3ExtensionByteData())
                //SOI 제외한 메타 데이터 write
                byteBuffer.write(
                    jpegMetaData,
                    2,
                    jpegMetaData.size - (2)
                )
            }

            // Imgaes Data write
            for (i in 0..AiContainer.imageContent.pictureCount - 1) {
                var picture = AiContainer.imageContent.getPictureAtIndex(i)
                //byteBuffer.write(/* b = */ picture!!._pictureByteArray)
                if (i == 0) {
                    byteBuffer.write(/* b = */ picture!!._pictureByteArray)
                    //EOI 작성
                    byteBuffer.write(0xff)
                    byteBuffer.write(0xd9)

                } else{
                    // XOI 마커
                    byteBuffer.write(0xff)
                    byteBuffer.write(0x10)
                    // 연속 모드가 아니면 APP1을 저장
                    if(!isBurstMode){
                        if(picture!!._app1Segment != null){
                            byteBuffer.write(picture!!._app1Segment)
                        }
                    }
                    byteBuffer.write(/* b = */ picture!!._pictureByteArray)
                }
            }

            // Text Data write
            for (i in 0..AiContainer.textContent.textList.size - 1) {
                var text = AiContainer.textContent.getTextAtIndex(i)

                // XOT 마커
                byteBuffer.write(0xff)
                byteBuffer.write(0x20)

                for (i in 0 until text!!.data.length) {
                    val charValue = text!!.data[i].toInt()

                    // 2개의 바이트로 쪼개기
                    var tempByteBuffer : ByteBuffer = ByteBuffer.allocate(2)
                    tempByteBuffer.put((charValue shr 8 and 0xFF).toByte())
                    tempByteBuffer.put((charValue and 0xFF).toByte())

                    byteBuffer.write(tempByteBuffer.array())
                }
            }
            // Audio Write
            if (AiContainer.audioContent.audio != null) {
                var audio = AiContainer.audioContent.audio
                // XOI 마커
                byteBuffer.write(0xff)
                byteBuffer.write(0x30)
                byteBuffer.write(/* b = */ audio!!._audioByteArray)
            }
        }
        return@coroutineScope byteBuffer.toByteArray()
    }


    fun saveJPEG(byteArray: ByteArray, isSaved: MutableLiveData<Uri>) {
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
                    isSaved.value = uri
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun lowSDKVersionSave(resultByteArray: ByteArray, isSaved: MutableLiveData<Uri>?){
        // Q 버전 이하일 경우. 저장소 권한을 얻어온다.
        val writePermission = mainActivity?.let {
            ActivityCompat.checkSelfPermission(
                it,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
        if (writePermission == PackageManager.PERMISSION_GRANTED) {
            saveImageOnUnderAndroidQ(resultByteArray)

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
}