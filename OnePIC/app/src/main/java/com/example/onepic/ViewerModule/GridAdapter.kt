package com.example.onepic.ViewerModule

import android.content.Context
import android.net.Uri
import android.os.Bundle

import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.onepic.R

class GridAdapter(val fragment: Fragment, val context: Context, uriArr:List<Uri>): BaseAdapter() {

    private var items: List<Uri>

    init {
        this.items = uriArr
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

        Glide.with(context).load(items[p]).into(imageView)
        return imageView
    }

}