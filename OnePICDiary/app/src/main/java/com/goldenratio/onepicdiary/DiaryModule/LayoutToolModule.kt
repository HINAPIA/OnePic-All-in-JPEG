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
        layoutInflater: LayoutInflater, view: LinearLayout, date: Int, currentDate: Int?,
        list: ArrayList<Int>?, clickFun: KMutableProperty0<MutableLiveData<Int>>) {

        var currentLayout: ImageView? = null

        list?.sort()

        var listNum = 0

        CoroutineScope(Dispatchers.Default).launch {
            for (i in 1..date) {

                val subLayout =
                    layoutInflater.inflate(R.layout.date_layout, null)

                val imageView = subLayout.findViewById<ImageView>(R.id.imageView)
                // 위 불러온 layout에서 변경을 할 view가져오기
                val textView: TextView =
                    subLayout.findViewById(R.id.dateText)
                textView.text = i.toString()

                if (currentDate != null && currentDate == i) {
                    currentLayout = imageView
                    // 현재 날짜 일 경우
                    imageView.visibility = View.VISIBLE
                }

                if (list != null) {
//                textView.setTextColor(Color.parseColor("#9E9E9E"))
                    if (list.size > listNum && list[listNum] == i) {
                        listNum++
                    }
                    else {
                        CoroutineScope(Dispatchers.Main).launch {
                            subLayout.findViewById<TextView>(R.id.dateText)
                                .setTextColor(Color.parseColor("#9C9C9C"))
                        }
                    }
                }

                subLayout.setOnClickListener {
                    isAutoScrollMove = false
                    clickFun.get().value = i
                    currentLayout?.visibility = View.INVISIBLE

                    currentLayout = imageView
                    // 현재 날짜 일 경우
                    imageView.visibility = View.VISIBLE
                }

                withContext(Dispatchers.Main) {
                    // main activity에 만들어둔 scrollbar 속 layout의 아이디를 통해 해당 layout에 넣기
                    view.addView(subLayout)
                }
                val viewTreeObserver = subLayout.viewTreeObserver

                viewTreeObserver.addOnGlobalLayoutListener(object :
                    ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        if (currentDate != null && isAutoScrollMove) {
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
    fun setMonthLayer(
        layoutInflater: LayoutInflater, view: LinearLayout, date: Int, currentDate: Int, clickFun: KMutableProperty0<MutableLiveData<Int>>) {

        var currentLayout: ImageView? = null

        CoroutineScope(Dispatchers.Default).launch {
            for (i in 1..date) {

                val subLayout =
                    layoutInflater.inflate(R.layout.month_layout, null)

                val monthImageView = subLayout.findViewById<ImageView>(R.id.monthImageView)

                // 위 불러온 layout에서 변경을 할 view가져오기
                val textView: TextView =
                    subLayout.findViewById(R.id.dateText)
                textView.text = i.toString()

                if (currentDate == i) {
                        currentLayout = monthImageView
                        monthImageView.visibility = View.VISIBLE
                }

                subLayout.setOnClickListener {
                    isAutoScrollMove = false
                    clickFun.get().value = i
                    currentLayout?.visibility = View.INVISIBLE
                    month = i
                    currentLayout = monthImageView
                    monthImageView.visibility = View.VISIBLE
                }

                withContext(Dispatchers.Main) {
                    // main activity에 만들어둔 scrollbar 속 layout의 아이디를 통해 해당 layout에 넣기
                    view.addView(subLayout)
                }
            }
        }
    }
}