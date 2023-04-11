package com.example.onepic.ViewerModule


import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.onepic.R

import java.security.AccessController.getContext

class ViewPagerAdapter (val context: Context, uriList: List<String>) : RecyclerView.Adapter<ViewPagerAdapter.PagerViewHolder>() {

    lateinit var viewHolder: PagerViewHolder
    var galleryMainimages = uriList // gallery에 있는 이미지 리스트
    private var externalImage: ByteArray? = null // ScrollView로 부터 선택된 embedded image

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : PagerViewHolder {
        viewHolder = PagerViewHolder(parent)
        return viewHolder
    }

    override fun onBindViewHolder(holder: PagerViewHolder, position: Int) {
        if (externalImage != null){ // 숨겨진 이미지가 선택 되었을 때 (스와이프 X, scrollView 아이템 중 하나 선택)
            holder.bindEmbeddedImage(externalImage!!)
            externalImage = null // 초기화
        }
        else { // 사용자가 스와이프로 화면 넘길 때
            holder.bind(galleryMainimages[position]) // binding
        }
    }

    override fun getItemCount(): Int = galleryMainimages.size

    /** 숨겨진 사진 중, 선택된 것 -> Main View에서 보이도록 설정 */
    fun setExternalImage(byteArray: ByteArray){
        externalImage = byteArray
        notifyDataSetChanged()
    }

    /** ViewHolder 정의 = Main Image UI */
    inner class PagerViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder
        (LayoutInflater.from(parent.context).inflate(R.layout.main_image_list_item, parent, false)){
        // TODO: 조금 더 깔끔한 방법으로 바꾸기 (ImageView 하나만으로 구현 - cache 처리 필요)
        private val imageView: ImageView = itemView.findViewById(R.id.imageView) // Main Gallery 이미지 보여주는 view
        val externalImageView:ImageView = itemView.findViewById(R.id.externalImageView) // ScrollView로 부터 선택된 embedded image 보여주는 view

        /** Uri 로 imageView에 띄우기 */
        fun bind(image:String) { // Main 이미지 보여주기
            imageView.visibility = View.VISIBLE
            externalImageView.visibility = View.GONE
            Glide.with(context).load(getUriFromPath(image)).into(imageView)
        }

        /** ByteArray 로 imageView에 띄우기  */
        fun bindEmbeddedImage(embeddedImage: ByteArray){ // ScrollView로 부터 선택된 embedded image 보여 주기
            externalImageView.visibility = View.VISIBLE
            imageView.visibility = View.INVISIBLE
            Glide.with(context)
                .load(embeddedImage)
                .into(externalImageView)
        }

    }


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

}