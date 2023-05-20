package com.goldenratio.onepicdiary.DiaryModule

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.goldenratio.onepicdiary.R
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class CalendarAdapter(private val context: Context, private val days: MutableList<Int?>) : BaseAdapter() {

    val cellViewMap = mutableMapOf<Int, View>()
    var day = 0
    var selectView: View? = null

    override fun getCount(): Int {
        return days.size
    }

    override fun getItem(position: Int): Int? {
        return days[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val day = days[position]

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val cellView = inflater.inflate(R.layout.grid_cell_layout, parent, false)

        val dateText = cellView.findViewById<TextView>(R.id.dateText)
        if (day == null) {
            dateText.text = " "
            cellView.findViewById<ImageView>(R.id.lineImage).visibility = View.INVISIBLE
        } else {
            dateText.text = day.toString()
            cellViewMap[day] = cellView
            cellView.setOnClickListener {
                this.day = day
                if(selectView != null) {
                    selectView?.visibility = View.GONE
                }
                selectView = cellViewMap[day]!!
                selectView = cellViewMap[day]?.findViewById<ImageView>(R.id.currentIcon)
                selectView?.visibility = View.VISIBLE
            }
        }

        return cellView
    }

    suspend fun addDiaryImage(uri: Uri, date: Int, clickFun: (Uri) -> Unit) : Boolean = suspendCoroutine { result ->
        CoroutineScope(Dispatchers.Default).launch {

            while (date > cellViewMap.size) {
                delay(300)
            }

            withContext(Dispatchers.Main) {
                cellViewMap[date]?.findViewById<TextView>(R.id.dateText)?.setTextColor(Color.WHITE)
                val imageView = cellViewMap[date]?.findViewById<ImageView>(R.id.imageView)!!
                withContext(Dispatchers.Main) {
                    Glide.with(imageView)
                        .load(uri)
                        .into(imageView)
                }
                cellViewMap[date]?.findViewById<ImageView>(R.id.sticker)?.visibility = View.VISIBLE

                cellViewMap[date]?.setOnClickListener {
                    clickFun(uri)
                }
            }

            result.resume(true)
        }
    }

    suspend fun setMainImage(date: Int) : Boolean = suspendCoroutine { result ->
        CoroutineScope(Dispatchers.Default).launch {

            while (date > cellViewMap.size) {
            }

            day =  date

            withContext(Dispatchers.Main) {
//                cellViewMap[date]?.findViewById<TextView>(R.id.dateText)?.setTextColor(Color.WHITE)
                selectView = cellViewMap[date]?.findViewById<ImageView>(R.id.currentIcon)
                selectView?.visibility = View.VISIBLE
            }

            result.resume(true)
        }
    }

}
