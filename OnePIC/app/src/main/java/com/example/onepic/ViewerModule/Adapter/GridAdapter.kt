package com.example.onepic.ViewerModule.Adapter

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater

import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.onepic.R

class GridAdapter(val fragment: Fragment, val context: Context): BaseAdapter() {

    private lateinit var items: List<String>

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
            imageView.layoutParams = LinearLayout.LayoutParams(display.widthPixels/3,display.widthPixels/3)
            imageView.setOnClickListener{
                val bundle = Bundle()
                bundle.putInt("currentPosition",p)
                fragment.findNavController().navigate(R.id.action_galleryFragment_to_viewerFragment,bundle)
            }

            Glide.with(context).load(getUriFromPath(items[p])).into(imageView)
            return imageView
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