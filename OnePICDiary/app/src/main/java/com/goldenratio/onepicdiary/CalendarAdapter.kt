package com.goldenratio.onepicdiary

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CalendarAdapter(private val context: Context, private val days: MutableList<Int?>) : BaseAdapter() {

    val cellViewMap = mutableMapOf<Int, View>()

    override fun getCount(): Int {
        return days.size
    }

    override fun getItem(position: Int): Int? {
        return days[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    @SuppressLint("MissingInflatedId")
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
        }


        // 셀의 모양이나 기능을 추가로 커스텀할 수 있습니다.

        return cellView
    }

    fun addDiaryImage(uri: Uri, date: Int) {
        CoroutineScope(Dispatchers.Default).launch {

            while (date > cellViewMap.size) {
            }

            withContext(Dispatchers.Main) {
                cellViewMap[date]?.findViewById<TextView>(R.id.dateText)
                    ?.setTextColor(Color.WHITE)
                cellViewMap[date]?.findViewById<ImageView>(R.id.imageView)?.setImageURI(uri)
            }
        }
    }
}
