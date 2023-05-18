package com.goldenratio.onepic.ViewerModule.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.goldenratio.onepic.R

class RecyclerViewAdapter(var imageList: List<String>) :
    RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>() {

//    fun setRecyclerViewItem(list:List<String>){
//        imageList = list
//    }

    fun updateData(newData: List<String>) {
        imageList = newData
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recyclerview_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val imageUrl = imageList[position]
        Glide.with(holder.itemView)
            .load(imageUrl)
            .into(holder.imageView)
    }

    override fun getItemCount(): Int {
        return imageList.size
    }

}