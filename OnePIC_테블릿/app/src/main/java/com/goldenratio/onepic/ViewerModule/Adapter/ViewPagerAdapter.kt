package com.goldenratio.onepic.ViewerModule.Adapter


import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.goldenratio.onepic.ImageToolModule
import com.goldenratio.onepic.PictureModule.Contents.Picture
import com.goldenratio.onepic.PictureModule.ImageContent
import com.goldenratio.onepic.R
import com.goldenratio.onepic.ViewerModule.Fragment.ViewerFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ViewPagerAdapter (val context: Context) : RecyclerView.Adapter<ViewPagerAdapter.PagerViewHolder>() {

    lateinit var viewHolder: PagerViewHolder // Viewholder
    lateinit var galleryMainimage:List<String>// gallery에 있는 이미지 리스트

    private var externalImage: ByteArray? = null // ScrollView로 부터 선택된 embedded image
    private var externalImageBitmap:Bitmap? = null

    private var checkMagicPicturePlay = false // magic picture 재생 or stop
    private var isMainChanged = false

    /* Magic picture 변수 */
    var boundingBox: ArrayList<ArrayList<Int>> = arrayListOf()
    val handler = Handler()
    var magicPlaySpeed: Long = 100

    private lateinit var imageContent : ImageContent
    private lateinit var imageToolModule: ImageToolModule

    private var changeFaceStartX = 0
    private var changeFaceStartY = 0

    private var pictureList: ArrayList<Picture> = arrayListOf()
    private var bitmapList: ArrayList<Bitmap> = arrayListOf()

    private lateinit var mainPicture: Picture
    private lateinit var mainBitmap: Bitmap

    private var overlayImg: ArrayList<Bitmap> = arrayListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : PagerViewHolder {
        viewHolder = PagerViewHolder(parent)
        return viewHolder
    }

    override fun onBindViewHolder(holder: PagerViewHolder, position: Int) {

        if (checkMagicPicturePlay){ //magic picture 재생
            holder.magicPictureRun(overlayImg)
            checkMagicPicturePlay = false
        }
        else if (externalImage != null){ // 숨겨진 이미지가 선택 되었을 때 (스와이프 X, scrollView 아이템 중 하나 선택)
            holder.bindEmbeddedImage(externalImage!!)
            externalImage = null // 초기화
        }
        else if (externalImageBitmap != null) {
            holder.bindBitmapImage(externalImageBitmap!!)
            externalImageBitmap = null // 초기화
        }
        else { // 사용자가 스와이프로 화면 넘길 때
            holder.bind(galleryMainimage[position]) // binding
        }
    }

    override fun getItemCount(): Int = galleryMainimage.size

    /** 숨겨진 사진 중, 선택된 것 -> Main View에서 보이도록 설정 */
    fun setImageContent(imageContent: ImageContent){
        this.imageContent = imageContent
        imageToolModule = ImageToolModule()
        // main Picture의 byteArray를 bitmap 제작
        mainPicture = imageContent.mainPicture
        val newMainBitmap = imageContent.getMainBitmap()
        if(newMainBitmap != null) {
            mainBitmap = newMainBitmap
        }
    }

    fun setExternalImage(byteArray: ByteArray){
        externalImage = byteArray
        notifyDataSetChanged()
    }

    fun setExternalImageBitmap(bitmap:Bitmap){
        externalImageBitmap = bitmap
        notifyDataSetChanged()
    }

    fun setUriList(uriList: List<String>){
        galleryMainimage = uriList
    }

    fun setCheckMagicPicturePlay(value:Boolean, isFinished: MutableLiveData<Boolean>){

        if(!value) {
            handler.removeCallbacksAndMessages(null)
            checkMagicPicturePlay = false
            //viewHolder.magicPictureStop()

            //viewHolder.externalImageView.visibility = View.INVISIBLE
        }
        else {
            CoroutineScope(Dispatchers.Default).launch {
                Log.d("view magic","size: ${overlayImg.size}")
                if (overlayImg.size <= 0) {
                    overlayImg = magicPictureProcessing()
                }
                //magicPictureRun(overlayImg)
                CoroutineScope(Dispatchers.Main).launch {
                    checkMagicPicturePlay = true
                    notifyDataSetChanged()
                }
                withContext(Dispatchers.Main) {
                    isFinished.value = true
                }
            }
        }
    }

/*--------------------------------------------------------------------------------------------------------------*/

    /** FilePath String 을 Uri로 변환 */
    @SuppressLint("Range")
    fun getUriFromPath(filePath: String): Uri { // filePath String to Uri
        val cursor = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            null, "_data = '$filePath'", null, null)
        var uri: Uri
        if(cursor!=null) {
            cursor!!.moveToNext()
            val id = cursor.getInt(cursor.getColumnIndex("_id"))
            uri = ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                id.toLong()
            )
            cursor.close()
        }
        else {
            return Uri.parse("Invalid path")
        }
        Log.d("여기 니가 원하는거 : ", ""+uri)
        return uri
    }


    /* ---------------------------------------------------------- Magic Picture ----------------------------------------------- */
    /** ViewHolder 정의 = Main Image UI */

    fun resetMagicPictureList() {
        overlayImg.clear()
        bitmapList.clear()
        boundingBox.clear()
        Log.d("view magic","reset after: ${overlayImg.size}")
    }

    private suspend fun magicPictureProcessing(): ArrayList<Bitmap>  =
        suspendCoroutine { result ->
//             val overlayImg: ArrayList<Bitmap> = arrayListOf()
            // rewind 가능한 연속 사진 속성의 picture list 얻음
            pictureList = imageContent.pictureList
            if (bitmapList.size == 0) {
                val newBitmapList = imageContent.getBitmapList()
                if(newBitmapList!=null) {
                    bitmapList = newBitmapList
                }
            }

            var basicIndex = 0
            var checkEmbedded = false
            for (i in 0 until pictureList.size) {
                if (pictureList[i].embeddedData?.size != null) {
                    if (pictureList[i].embeddedData?.size!! > 0) {
                        checkEmbedded = true
                        break
                    }
                    basicIndex++
                }
            }
//             Log.d("checkEmbedded", "!!!!!!!! $basicIndex")
//             Log.d("!!!!!","!!!!!!pictureList ${pictureList[basicIndex].embeddedData}")
            if (checkEmbedded) {
                changeFaceStartX = (pictureList[basicIndex].embeddedData?.get(4) ?: Int) as Int
                changeFaceStartY = (pictureList[basicIndex].embeddedData?.get(5) ?: Int) as Int

                val checkFinish = BooleanArray(pictureList.size - basicIndex)
                for (i in basicIndex until pictureList.size) {
                    checkFinish[i - basicIndex] = false
                    pictureList[i].embeddedData?.let { boundingBox.add(it) }
                }

                for (i in basicIndex until pictureList.size) {
                    CoroutineScope(Dispatchers.Default).launch {
                        createOverlayImg(overlayImg, boundingBox[i - basicIndex], i)
                        checkFinish[i - basicIndex] = true
                    }
                }

                while (!checkFinish.all { it }) {
                    // Wait for all tasks to finish
                }
            }
            result.resume(overlayImg)
        }


    private fun createOverlayImg(ovelapBitmap: ArrayList<Bitmap> , rect: ArrayList<Int>, index: Int) {

        // 감지된 모든 boundingBox 출력
        println("=======================================================")

        // bitmap를 자르기
        if(rect.size >= 4 && bitmapList.size > index) {
            val cropImage = imageToolModule.cropBitmap(
                bitmapList[index],
                Rect(rect[0], rect[1], rect[2], rect[3])
            )

            val newImage = imageToolModule.circleCropBitmap(cropImage)
            ovelapBitmap.add(
                imageToolModule.overlayBitmap(mainBitmap, newImage, changeFaceStartX, changeFaceStartY)
            )
        }
    }

    private fun setBitmapPicture() {

        val checkFinish = BooleanArray(pictureList.size)
        for (i in 0 until pictureList.size) {
            checkFinish[i] = false
            bitmapList.add(mainBitmap)
        }
        for (i in 0 until pictureList.size) {
            CoroutineScope(Dispatchers.Default).launch {
                val bitmap = imageToolModule.byteArrayToBitmap(imageContent.getJpegBytes(pictureList[i]))
                bitmapList.add(i, bitmap)
                checkFinish[i] = true
            }
        }
        while (!checkFinish.all { it }) {
            // Wait for all tasks to finish
        }
    }

    inner class PagerViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder
        (LayoutInflater.from(parent.context).inflate(R.layout.main_image_list_item, parent, false)){

        private val imageView: ImageView = itemView.findViewById(R.id.imageView) // Main Gallery 이미지 보여주는 view
        val externalImageView:ImageView = itemView.findViewById(R.id.externalImageView) // ScrollView로 부터 선택된 embedded image 보여주는 view

        /** Uri 로 imageView에 띄우기 */
        fun bind(image:String) { // Main 이미지 보여주기
            imageView.visibility = View.VISIBLE
            externalImageView.visibility = View.GONE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                Glide.with(context)
                    .load(image)
                    .into(imageView)
            }
            else {
                Glide.with(context)
                    .load(getUriFromPath(image))
                    .into(imageView)
            }
        }

        /** ByteArray 로 imageView에 띄우기  */
        fun bindEmbeddedImage(embeddedImage: ByteArray){ // ScrollView로 부터 선택된 embedded image 보여 주기
            externalImageView.visibility = View.VISIBLE
            imageView.visibility = View.INVISIBLE
            Glide.with(context)
                .load(embeddedImage)
                .into(externalImageView)
        }

        fun bindBitmapImage(embeddedImage: Bitmap){ // ScrollView로 부터 선택된 embedded image 보여 주기
            externalImageView.visibility = View.VISIBLE
            imageView.visibility = View.INVISIBLE
            externalImageView.setImageBitmap(embeddedImage)
        }


        fun magicPictureRun(ovelapBitmap: ArrayList<Bitmap>) {
            externalImageView.visibility = View.VISIBLE
            CoroutineScope(Dispatchers.Main).launch {
                var currentImageIndex = 0
                var increaseIndex = 1

                val runnable = object : Runnable {
                    override fun run() {
                        if (ovelapBitmap.size > 0) {
                            externalImageView.setImageBitmap(ovelapBitmap[currentImageIndex])
                            //currentImageIndex++

                            currentImageIndex += increaseIndex

                            if (currentImageIndex >= ovelapBitmap.size - 1) {
                                //currentImageIndex = 0
                                increaseIndex = -1
                            } else if (currentImageIndex <= 0) {
                                increaseIndex = 1
                            }
                            handler.postDelayed(this, magicPlaySpeed)
                        }
                        else {
                            Log.d("overlay bitmap","size == 0")
                        }
                    }
                }
                handler.postDelayed(runnable, magicPlaySpeed)
            }
        }

    }
}