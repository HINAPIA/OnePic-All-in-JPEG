package com.goldenratio.onepicdiary.DiaryModule

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import android.widget.*
import androidx.lifecycle.MutableLiveData
import com.goldenratio.onepicdiary.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList
import kotlin.reflect.KMutableProperty0


class LayoutToolModule {

    var isAutoScrollMove = true

    var month = 0

    @SuppressLint("MissingInflatedId", "CutPasteId", "ResourceAsColor")
    fun setSubImage(
        layoutInflater: LayoutInflater, view: LinearLayout, date: Int, currentDate: Int,
        list: ArrayList<Int>?, clickFun: KMutableProperty0<MutableLiveData<Int>>) {

        var currentLayout: ImageView? = null
//        isAutoScrollMove = true

        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH) + 1
        val currentDay = calendar.get(Calendar.DATE)

        list?.sort()

        var listNum = 0

        CoroutineScope(Dispatchers.Default).launch {
            for (i in 1..date) {

                val subLayout =
                    layoutInflater.inflate(R.layout.date_layout, null)

                val imageView = subLayout.findViewById<ImageView>(R.id.imageView)
                val monthImageView = subLayout.findViewById<ImageView>(R.id.monthImageView)

                // 위 불러온 layout에서 변경을 할 view가져오기
                val textView: TextView =
                    subLayout.findViewById(R.id.dateText)
                textView.text = i.toString()

                if (currentDate == i) {
                    if (date > 12) {
                        currentLayout = imageView
                        // 현재 날짜 일 경우
                        imageView.visibility = View.VISIBLE
                    } else {
                        currentLayout = monthImageView
                        monthImageView.visibility = View.VISIBLE
                    }
                }

                if (date == 12 && i > month ||
                    date > 12 && month > currentMonth || month == currentMonth && currentDay < i) {
                    textView.setTextColor(Color.parseColor("#9E9E9E"))
                }

                if (list != null) {
//                textView.setTextColor(Color.parseColor("#9E9E9E"))
                    textView.setTypeface(textView.typeface, Typeface.ITALIC)
                    if (list.size > listNum && list[listNum] == i) {
                        CoroutineScope(Dispatchers.Main).launch {
//                        subLayout.findViewById<TextView>(R.id.dateText).setTextColor(Color.BLACK)
                            subLayout.findViewById<ImageView>(R.id.ContainDiary).visibility =
                                View.VISIBLE
                        }
                        listNum++
                    }
                }

                subLayout.setOnClickListener {
                    isAutoScrollMove = false
                    clickFun.get().value = i
                    currentLayout?.visibility = View.INVISIBLE
                    if (date > 12) {
                        currentLayout = imageView
                        // 현재 날짜 일 경우
                        imageView.visibility = View.VISIBLE
                    } else {
                        month = i
                        currentLayout = monthImageView
                        monthImageView.visibility = View.VISIBLE

                    }
                }

                withContext(Dispatchers.Main) {
                    // main activity에 만들어둔 scrollbar 속 layout의 아이디를 통해 해당 layout에 넣기
                    view.addView(subLayout)
                }
                if (date > 12 && currentDate == i) {
                    val viewTreeObserver = subLayout.viewTreeObserver

                    viewTreeObserver.addOnGlobalLayoutListener(object :
                        ViewTreeObserver.OnGlobalLayoutListener {
                        override fun onGlobalLayout() {
                            if (isAutoScrollMove) {
                                // 동적으로 추가된 뷰가 다 그려진 후에 실행되어야 할 코드 작성
                                val x = subLayout.width * (currentDate - 6)
                                Log.d("view Move", "currentDate : $currentDate || ${subLayout.width}")

                                val scroll: HorizontalScrollView =
                                    view.parent as HorizontalScrollView
                                CoroutineScope(Dispatchers.Main).launch {
                                    scroll.scrollTo(x, 0)
                                }
                            } else {
                                // 더 이상 리스너가 필요하지 않으면 제거합니다.
                                subLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                            }
                        }
                    })
                }
            }
        }
    }
}