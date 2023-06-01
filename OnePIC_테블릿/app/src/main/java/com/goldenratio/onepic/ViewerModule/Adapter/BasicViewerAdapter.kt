package com.goldenratio.onepic.ViewerModule.Adapter

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.goldenratio.onepic.ImageToolModule
import com.goldenratio.onepic.PictureModule.Contents.Picture
import com.goldenratio.onepic.PictureModule.ImageContent
import com.goldenratio.onepic.R
import com.goldenratio.onepic.ViewerModule.Fragment.BasicViewerFragment

class BasicViewerAdapter (val context: Context) : RecyclerView.Adapter<BasicViewerAdapter.PagerViewHolder>() {

    lateinit var viewHolder: PagerViewHolder // Viewholder
    lateinit var galleryMainimages:List<String>// gallery에 있는 이미지 리스트
    private var externalImage: ByteArray? = null // ScrollView로 부터 선택된 embedded image
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

    private var currentItemPosition:Int = 0

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : PagerViewHolder {
        viewHolder = PagerViewHolder(parent)
        viewHolder.init()
        return viewHolder
    }

    override fun onBindViewHolder(holder: PagerViewHolder, @SuppressLint("RecyclerView") position: Int) {
        holder.bind(galleryMainimages[position]) // binding
        currentItemPosition = position
    }

    override fun getItemCount(): Int = galleryMainimages.size

    fun getCurrentItemPosition():Int = currentItemPosition

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

    fun setUriList(uriList: List<String>){
        galleryMainimages = uriList
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
        return uri

    }

    /* ---------------------------------------------------------- Magic Picture ----------------------------------------------- */


    inner class PagerViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder
        (LayoutInflater.from(parent.context).inflate(R.layout.main_image_list_item, parent, false)){
        // TODO: 조금 더 깔끔한 방법으로 바꾸기 (ImageView 하나만으로 구현 - cache 처리 필요)
        val imageView: ImageView = itemView.findViewById(R.id.imageView) // Main Gallery 이미지 보여주는 view

        @RequiresApi(Build.VERSION_CODES.M)
        fun init(){
            imageView.setOnClickListener{
                if (BasicViewerFragment.isPictureClicked.value == true){
                    BasicViewerFragment.isPictureClicked.value = false
                }
                else {
                    BasicViewerFragment.isPictureClicked.value = true
                }
            }
        }

        /** Uri 로 imageView에 띄우기 */
        fun bind(image:String) { // Main 이미지 보여주기
            imageView.visibility = View.VISIBLE
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                Glide.with(context).load(image).into(imageView)
            }
            else {
                Glide.with(context).load(getUriFromPath(image)).into(imageView)
            }
        }

    }

}