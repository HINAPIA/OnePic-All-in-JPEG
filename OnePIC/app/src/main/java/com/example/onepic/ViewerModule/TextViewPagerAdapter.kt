package com.example.onepic.ViewerModule

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.onepic.R

class TextViewPagerAdapter(val context: Context, textList: ArrayList<String>) : RecyclerView.Adapter<TextViewPagerAdapter.TextPagerViewHolder>() {

    var texts = textList

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = TextPagerViewHolder(parent)

    override fun onBindViewHolder(holder: TextPagerViewHolder, position: Int) {
        holder.bind(texts[position]) // binding
    }

    override fun getItemCount(): Int = texts.size

    /* View Holder 정의 */
    inner class TextPagerViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder
        (LayoutInflater.from(parent.context).inflate(R.layout.text_viewpager_item, parent, false)){
        private val textView: TextView = itemView.findViewById(R.id.textView)

        fun bind(text:String) {
            textView.text = text
        }
    }

}