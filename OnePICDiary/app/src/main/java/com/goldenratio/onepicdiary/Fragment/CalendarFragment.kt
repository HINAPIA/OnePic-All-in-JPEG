package com.goldenratio.onepicdiary.Fragment

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.goldenratio.onepic.JpegViewModel
import com.goldenratio.onepic.LoadModule.LoadResolver
import com.goldenratio.onepicdiary.CalendarAdapter
import com.goldenratio.onepicdiary.R
import com.goldenratio.onepicdiary.databinding.FragmentCalendarBinding
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.*
import kotlin.properties.Delegates

class CalendarFragment : Fragment() {

    private lateinit var binding: FragmentCalendarBinding
    private val jpegViewModel by activityViewModels<JpegViewModel>()

    private var currentMonth by Delegates.notNull<Int>()
    private var currentYear by Delegates.notNull<Int>()

    private lateinit var calendar: Calendar
    private lateinit var adapter: CalendarAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentCalendarBinding.inflate(inflater, container, false)

        calendar = Calendar.getInstance()

        currentYear = calendar.get(Calendar.YEAR)
        currentMonth = calendar.get(Calendar.MONTH)

//        Log.d("calendar","##### "+calendar.get(Calendar.DATE))

        val cellList = jpegViewModel.diaryCellArrayList

        if(cellList.size > 0) {
            currentYear = cellList[cellList.size-1].year
            currentMonth = cellList[cellList.size-1].month - 1
        }

        setDiary()

        // 이전 달로 변경
        binding.previousMonth.setOnClickListener {
            currentMonth--
            if (currentMonth == -1) {
                currentMonth = 11
                currentYear--
            }
            setDiary()
        }

        // 다음 달로 변경
        binding.afterMonth.setOnClickListener {
            currentMonth++
            if (currentMonth == 12) {
                currentMonth = 0
                currentYear++
            }
            setDiary()
        }

        // 다이어리 추가 버튼
        binding.diaryAddBtn.setOnClickListener {
            findNavController().navigate(R.id.action_calendarFragment_to_addDiaryFragment)
        }

        return binding.root
    }

    private fun generateCalendarDays(currentYear: Int, currentMonth: Int): MutableList<Int?> {
        calendar.set(currentYear, currentMonth, 1)

        binding.dateText.text = "${currentYear}년 ${currentMonth + 1}월 "

        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        // 일요일 부터 시작
        val startDay = calendar.get(Calendar.DAY_OF_WEEK) + 1

        val days: MutableList<Int?> = mutableListOf()

        // 이전 달의 날짜 추가
        val previousMonth = currentMonth - 1
        calendar.set(currentYear, previousMonth, 1)
        val daysInPreviousMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val startDayOfWeek = (startDay + 5) % 7
        for (i in startDayOfWeek downTo 1) {
            days.add(null)
        }

        // 현재 달의 날짜 추가
        for (i in 1..daysInMonth) {
            days.add(i)
        }

        // 다음 달의 날짜 추가
        val nextMonth = currentMonth + 1
        calendar.set(currentYear, nextMonth, 1)
        val remainingDays = 42 - days.size
        for (i in 1..remainingDays) {
            days.add(null)
        }
        return days
    }

    private fun setDiary() {
        binding.progressBar.visibility = View.VISIBLE

        val days = generateCalendarDays(currentYear, currentMonth)

        adapter = CalendarAdapter(requireContext(), days)
        binding.calendarGrid.adapter = adapter

        val cellList = jpegViewModel.diaryCellArrayList

        CoroutineScope(Dispatchers.Default).launch {
            for (i in 0 until cellList.size) {
                val cellYear = cellList[i].year
                val cellMonth = cellList[i].month - 1

                if (currentYear == cellYear && currentMonth == cellMonth)
                    adapter.addDiaryImage(cellList[i].currentUri, cellList[i].day, ::viewDiary)
            }
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
            }
        }

        CoroutineScope(Dispatchers.Default).launch {
            calendar = Calendar.getInstance()

            val nowYear = calendar.get(Calendar.YEAR)
            val nowMonth = calendar.get(Calendar.MONTH)

            if(currentYear == nowYear && currentMonth == nowMonth) {
                adapter.setMainImage(calendar.get(Calendar.DATE))
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun viewDiary(uri: Uri) {

        jpegViewModel.currentUri = uri
        jpegViewModel.setCurrentMCContainer()

        findNavController().navigate(R.id.action_calendarFragment_to_viewDiaryFragment)
    }
}
