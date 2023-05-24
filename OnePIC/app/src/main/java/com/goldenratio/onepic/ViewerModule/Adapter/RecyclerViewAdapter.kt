package com.goldenratio.onepic.ViewerModule.Adapter

import android.os.Build
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.goldenratio.onepic.R
import com.goldenratio.onepic.ViewerModule.Fragment.BasicViewerFragment

class RecyclerViewAdapter(var imageList: List<String>, var layoutManager: LinearLayoutManager) :
    RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>() {


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

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val imageUrl = imageList[position]

        Glide.with(holder.itemView)
            .load(imageUrl)
            .into(holder.imageView)

        holder.imageView.setOnClickListener{
            BasicViewerFragment.currentPosition = position
            BasicViewerFragment.isClickedRecyclerViewImage.value = true
        }
//
//        holder.imageView.background = null
//        holder.imageView.setPadding(0,0,0,0)
//
//
//        holder.imageView.isFocusable = true // 포커스를 받을 수 있도록 설정
//        holder.imageView.isFocusableInTouchMode = true // 터치 모드에서 포커스를 받을 수 있도록 설정
//
//        holder.imageView.onFocusChangeListener = View.OnFocusChangeListener { view, hasFocus ->
//            if (hasFocus) {
//                // 포커스를 얻었을 때의 동작 처리
//                holder.imageView.setBackgroundResource(R.drawable.chosen_image_border)
//                holder.imageView.setPadding(6,6,6,6)
//
//            } else {
//                holder.imageView.background = null
//                holder.imageView.setPadding(0,0,0,0)
//            }
//        }
//
//        holder.imageView.setOnTouchListener { _, event ->
//            when (event.action) {
//                MotionEvent.ACTION_UP -> {
//                    holder.imageView.performClick() // 클릭 이벤트 강제로 발생
//                }
//            }
//            false
//        }

    }

    override fun getItemCount(): Int {
        return imageList.size
    }

}