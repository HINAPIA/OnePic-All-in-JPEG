package com.goldenratio.onepicdiary.DiaryModule

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.goldenratio.onepicdiary.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.reflect.KMutableProperty0

class LayoutToolModule {
    @SuppressLint("MissingInflatedId", "CutPasteId")
    fun setSubImage(layoutInflater: LayoutInflater, date: Int, view: LinearLayout, currentDate: Int, clickFun: KMutableProperty0<Int>) {

        var currentLayout: ImageView? = null

        CoroutineScope(Dispatchers.Default).launch{
            for(i in 1 .. currentDate) {
                val subLayout =
                    layoutInflater.inflate(R.layout.date_layout, null)

                val imageView = subLayout.findViewById<ImageView>(R.id.imageView)

                // 위 불러온 layout에서 변경을 할 view가져오기
                val textView: TextView =
                    subLayout.findViewById(R.id.dateText)
                textView.text = i.toString()

                if (currentDate == i) {
                    currentLayout = imageView
                    // 현재 날짜 일 경우
                    imageView.visibility = View.VISIBLE
                }

                subLayout.setOnClickListener {
                    clickFun.set(i)
                    currentLayout?.visibility = View.GONE
                    currentLayout = imageView
                    imageView.visibility = View.VISIBLE
                }

                withContext(Dispatchers.Main) {
                    // main activity에 만들어둔 scrollbar 속 layout의 아이디를 통해 해당 layout에 넣기
                    view.addView(subLayout)
                }
            }
        }
    }
}