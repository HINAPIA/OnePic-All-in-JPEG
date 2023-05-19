package com.goldenratio.onepicdiary.DiaryModule

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.goldenratio.onepicdiary.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ViewPagerAdapter (val context: Context) : RecyclerView.Adapter<ViewPagerAdapter.PagerViewHolder>() {

    lateinit var viewHolder: PagerViewHolder // Viewholder

    private var imageList: ArrayList<ByteArray> = arrayListOf()


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : PagerViewHolder {
        viewHolder = PagerViewHolder(parent)
        return viewHolder
    }

    override fun onBindViewHolder(holder: PagerViewHolder, position: Int) {
        holder.bind(imageList[position])
    }

    override fun getItemCount(): Int = imageList.size

    fun setImageList(uriList: ArrayList<ByteArray>){
        imageList = uriList
    }

    inner class PagerViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder
        (LayoutInflater.from(parent.context).inflate(R.layout.main_image_view, parent, false)){

        private val imageView: ImageView = itemView.findViewById(R.id.imageView) // Main Gallery 이미지 보여주는 view

        /** Uri 로 imageView에 띄우기 */
        fun bind(image:ByteArray) { // Main 이미지 보여주기
            CoroutineScope(Dispatchers.Main).launch {
                Glide.with(imageView)
                    .load(image)
                    .into(imageView)
            }
        }

    }
}