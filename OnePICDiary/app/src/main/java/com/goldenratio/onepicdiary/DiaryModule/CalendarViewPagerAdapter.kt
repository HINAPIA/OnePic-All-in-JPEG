package com.goldenratio.onepicdiary.DiaryModule

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.goldenratio.onepicdiary.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CalendarViewPagerAdapter (val context: Context) : RecyclerView.Adapter<CalendarViewPagerAdapter.PagerViewHolder>() {

    lateinit var viewHolder: PagerViewHolder // Viewholder

    private var monthList: ArrayList<Int> = arrayListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagerViewHolder {
        viewHolder = PagerViewHolder(parent)
        return viewHolder
    }

    override fun onBindViewHolder(holder: PagerViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = 500

    inner class PagerViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder
        (LayoutInflater.from(parent.context).inflate(R.layout.empty, parent, false)) {

        val layout: ConstraintLayout = itemView.findViewById(R.id.layout)

        @SuppressLint("ClickableViewAccessibility")
        fun bind(int: Int) {
            layout.setOnTouchListener { _, _ ->
                // 터치 이벤트를 무시하고 처리하지 않음
                Log.d("Click Touch", "bind")
                false
            }
        }
    }
}