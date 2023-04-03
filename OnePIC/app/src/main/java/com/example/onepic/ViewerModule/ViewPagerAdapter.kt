package com.example.onepic.ViewerModule


import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.example.onepic.R

import kotlinx.coroutines.NonDisposableHandle.parent
import java.io.File
import java.security.AccessController.getContext

class ViewPagerAdapter (val context: Context, uriList: List<String>) : RecyclerView.Adapter<ViewPagerAdapter.PagerViewHolder>() {

    var images = uriList // gallery에 있는 이미지 리스트

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = PagerViewHolder(parent)

    override fun onBindViewHolder(holder: PagerViewHolder, position: Int) {
        holder.bind(images[position]) // binding
    }

    override fun getItemCount(): Int = images.size

    /* View Holder 정의 */
    inner class PagerViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder
        (LayoutInflater.from(parent.context).inflate(R.layout.main_image_list_item, parent, false)){
        private val imageView: ImageView = itemView.findViewById(R.id.imageView)

        fun bind(image:String) {
            Glide.with(context).load(getUriFromPath(image)).into(imageView)
        }

        @SuppressLint("Range")
        fun getUriFromPath(filePath: String): Uri { // filePath String to Uri
            val cursor = context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                null, "_data = '$filePath'", null, null)
            var uri:Uri
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
                return Uri.parse("dd")
            }
            return uri
        }
    }

}