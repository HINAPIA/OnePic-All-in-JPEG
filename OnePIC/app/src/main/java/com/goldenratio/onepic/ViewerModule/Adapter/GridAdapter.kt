package com.goldenratio.onepic.ViewerModule.Adapter

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.goldenratio.onepic.R

class GridAdapter(val fragment: Fragment, val context: Context, val currentPosition:MutableLiveData<Int>): BaseAdapter() {

    private lateinit var items: List<String>
    private var selectedImageView:ImageView? = null

    fun setItems(uriArr:List<String>){
        items = uriArr
        notifyDataSetChanged()
    }

    override fun getCount(): Int {
        return items.size
    }

    override fun getItem(position: Int): Any {
        return items[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }


    override fun getView(p: Int, convertView: View?, parent: ViewGroup?): View {
        Log.d("gallery fragment", "갤러리가 비지 않아따")
        val imageView = ImageView(context)
        val display = context.getResources().getDisplayMetrics()
        imageView.setPadding(2,2,2,2)
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        imageView.layoutParams = LinearLayout.LayoutParams(display.widthPixels/4,display.widthPixels/4)
        imageView.setOnClickListener{
            currentPosition.value = p
            if (selectedImageView != null) {
                selectedImageView!!.background = null
                selectedImageView!!.setPadding(2,2,2,2)
            }
            selectedImageView = imageView
            // 테두리 두께와 색상 설정
           imageView.setBackgroundResource(R.drawable.chosen_gallery_border)
           imageView.setPadding(6,6,6,6)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Glide.with(context).load(items[p]).into(imageView)
        } else {
            Glide.with(context).load(getUriFromPath(items[p])).into(imageView)
        }

        return imageView
    }

    /** FilePath String 을 Uri로 변환 */
    @SuppressLint("Range")
    fun getUriFromPath(filePath: String): Uri { // filePath String to Uri

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            val cursor = context.contentResolver.query(
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
                null,
                MediaStore.Images.Media.DATA + " = ?",
                arrayOf(filePath),
                null
            )
            var uri: Uri
            if (cursor != null && cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
                    id
                )
                cursor.close()
            } else {
                return Uri.parse("Invalid path")
            }
            return uri
        }
        else {

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

    class GridViewHolder(view: View) {
        val imageView: ImageView = view.findViewById(R.id.gridImageView)
    }
}