package com.goldenratio.onepic.ViewerModule.Adapter

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.goldenratio.onepic.ImageToolModule
import com.goldenratio.onepic.R

class RecyclerViewGridAdapter(private val context: Context, val currentPosition: MutableLiveData<Int>) :
    RecyclerView.Adapter<RecyclerViewGridAdapter.ViewHolder>() {

    private lateinit var items: List<String>

    private var imageTool = ImageToolModule()
    private var selectedImageView:ImageView? = null
    private var selectedItemPosition: Int = -1

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.gridImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_grid_image, parent, false)
        val display = context.resources.displayMetrics
        val layoutParams = FrameLayout.LayoutParams(display.widthPixels / 4, display.widthPixels / 4)
        view.findViewById<ImageView>(R.id.gridImageView).layoutParams = layoutParams
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int) {

        val isSelected = position == selectedItemPosition
        if (isSelected) {
            // 선택된 아이템의 뷰 설정 (예: 테두리 표시)
            holder.imageView.setBackgroundResource(R.drawable.chosen_gallery_border)
            holder.imageView.setPadding(6, 6, 6, 6)
        } else {
            // 선택되지 않은 아이템의 뷰 설정 (예: 테두리 제거)
            holder.imageView.background = null
            holder.imageView.setPadding(2, 2, 2, 2)
        }

        Glide.with(context).load(items[position]).into(holder.imageView)

        //val display = context.getResources().getDisplayMetrics()
        //holder.imageView.setPadding(2,2,2,2)
//        holder.imageView.scaleType = ImageView.ScaleType.CENTER_CROP

//        holder.imageView.layoutParams = LinearLayout.LayoutParams(display.widthPixels/4,display.widthPixels/4)
        holder.imageView.setOnClickListener{

            val previousSelectedItemPosition = selectedItemPosition
            selectedItemPosition = position
            notifyItemChanged(previousSelectedItemPosition,false)
            notifyItemChanged(position,false)

            currentPosition.value = position
//            if (selectedImageView != null) {
//                selectedImageView!!.background = null
//                selectedImageView!!.setPadding(2,2,2,2)
//            }
//            selectedImageView = holder.imageView
//            // 테두리 두께와 색상 설정
//            holder.imageView.setBackgroundResource(R.drawable.chosen_gallery_border)
//            holder.imageView.setPadding(6,6,6,6)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Glide.with(context).load(items[position]).into(holder.imageView)
        } else {
            Glide.with(context).load(imageTool.getUriFromPath(context,items[position])).into(holder.imageView)
        }

    }

    fun performClickOnItem(position:Int) {
        if (items.isNotEmpty()) {
            // 첫 번째 아이템을 클릭하여 선택 처리
            selectedItemPosition = position
            notifyDataSetChanged()

        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun setItem(item:List<String>) {
        items = item
        notifyDataSetChanged()
    }
}