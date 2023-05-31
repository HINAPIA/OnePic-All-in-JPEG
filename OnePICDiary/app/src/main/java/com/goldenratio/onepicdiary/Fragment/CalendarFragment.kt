package com.goldenratio.onepicdiary.Fragment

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.SharedPreferences
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
import androidx.viewpager2.widget.ViewPager2
import com.goldenratio.onepic.JpegViewModel
import com.goldenratio.onepicdiary.DiaryModule.CalendarAdapter
import com.goldenratio.onepicdiary.DiaryModule.CalendarViewPagerAdapter
import com.goldenratio.onepicdiary.DiaryModule.DiaryCellData
import com.goldenratio.onepicdiary.R
import com.goldenratio.onepicdiary.databinding.FragmentCalendarBinding
import kotlinx.coroutines.*
import java.io.IOException
import java.io.InputStream
import java.time.Month
import java.util.*
import kotlin.properties.Delegates

class CalendarFragment : Fragment() {

    private lateinit var binding: FragmentCalendarBinding
    private val jpegViewModel by activityViewModels<JpegViewModel>()

    private var currentMonth by Delegates.notNull<Int>()
    private var currentYear by Delegates.notNull<Int>()

    private lateinit var calendar: Calendar
    private lateinit var adapter: CalendarAdapter


    @SuppressLint("ClickableViewAccessibility")
    override fun onResume() {
        super.onResume()
        jpegViewModel.jpegMCContainer.value!!.init()

        jpegViewModel.isAudioPlay.value = 0
        jpegViewModel.isAddedAudio.value = false
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentCalendarBinding.inflate(inflater, container, false)

        calendar = Calendar.getInstance()

        currentYear = calendar.get(Calendar.YEAR)
        currentMonth = calendar.get(Calendar.MONTH)

        jpegViewModel.currentMonth = currentMonth + 1
        jpegViewModel.currentDay = calendar.get(Calendar.DATE)

//        Log.d("calendar","##### "+calendar.get(Calendar.DATE))

        getPreference()
        val cellList = jpegViewModel.diaryCellArrayList

        if (cellList.size > 0) {
            currentYear = cellList[cellList.size - 1].year
            currentMonth = cellList[cellList.size - 1].month
        }

        setDiary(currentMonth)

        // 이전 달로 변경
        binding.previousMonth.setOnClickListener {

            setDiary(-1)
        }

        // 다음 달로 변경
        binding.afterMonth.setOnClickListener {

            setDiary(+1)
        }

        // 다이어리 추가 버튼
        binding.diaryAddBtn.setOnClickListener {
            jpegViewModel.selectDay = adapter.day

            findNavController().navigate(R.id.action_calendarFragment_to_addDiaryFragment)
        }

        val pageChangeCallback = MyPageChangeCallback(::setDiary)
        binding.viewPager.adapter = CalendarViewPagerAdapter(requireContext())
        binding.viewPager.registerOnPageChangeCallback(pageChangeCallback)

        binding.viewPager.setOnTouchListener { _, event ->
            // 터치 이벤트를 처리하지 않고 하위 뷰로 전달되도록 함
            Log.d("Click Touch", "viewPager")
            false
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

        jpegViewModel.daysInMonth = daysInMonth

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

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun setDiary(addValue : Int) {

        if(addValue == -1) {
            currentMonth--
            if (currentMonth == -1) {
                currentMonth = 11
                currentYear--
            }
        }
        else {
            currentMonth++
            if (currentMonth == 12) {
                currentMonth = 0
                currentYear++
            }
        }

        binding.progressBar.visibility = View.VISIBLE

        jpegViewModel.selectMonth = currentMonth

        val days = generateCalendarDays(currentYear, currentMonth)

        adapter = CalendarAdapter(requireContext(), days)
        binding.calendarGrid.adapter = adapter

        val cellList = jpegViewModel.diaryCellArrayList


        CoroutineScope(Dispatchers.Default).launch {
            for (i in 0 until cellList.size) {
                val cellYear = cellList[i].year
                val cellMonth = cellList[i].month

                if (currentYear == cellYear && currentMonth == cellMonth) {
                    adapter.addDiaryImage(cellList[i].currentUri, cellList[i].day, ::viewDiary)
                }
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
                val date = calendar.get(Calendar.DATE)
                adapter.setMainImage(date)
                jpegViewModel.selectDay = date
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun viewDiary(uri: Uri) {
        jpegViewModel.jpegMCContainer.value!!.init()
        jpegViewModel.setCurrentMCContainer(uri)

        findNavController().navigate(R.id.action_calendarFragment_to_viewDiaryFragment)
    }

    fun getPreference() {
        val allPreferences: Map<String, *> = jpegViewModel.preferences.all
        jpegViewModel.diaryCellArrayList.clear()

        for ((key, value) in allPreferences) {
            // 키와 값을 출력
            println("Key: $key")
            println("Value: $value")

            val date = key.split("/")

            val imageUri = Uri.parse(value as String?)

            val contentResolver: ContentResolver = requireContext().getContentResolver()

            try {
                val inputStream: InputStream? = contentResolver.openInputStream(imageUri)
                // inputStream이 null이 아니라면 사진 URI가 존재하는 것입니다.
                // 원하는 작업을 수행하거나 결과를 처리할 수 있습니다.
                if(inputStream != null) {
                        val cell = DiaryCellData(imageUri, Integer.parseInt(date[0]), Integer.parseInt(date[1]), Integer.parseInt(date[2]))
                        Log.d("Cell Text", "````````````` ${cell.toString()}")
                        jpegViewModel.diaryCellArrayList.add(cell)
                }
                inputStream?.close()
            } catch (e: IOException ) {
                // 사진 URI가 존재하지 않는 경우 발생하는 예외입니다.
                // 예외 처리 코드를 추가할 수 있습니다.

                val editor: SharedPreferences.Editor = jpegViewModel.preferences.edit()
                editor.remove(key) // 삭제할 값의 키를 지정합니다.
                editor.apply()
            } catch (e: NumberFormatException) {
                e.printStackTrace()
            }
        }
    }


    class MyPageChangeCallback(val clickFun: (Int) -> Unit) : ViewPager2.OnPageChangeCallback() {

        private var prePosition = 0
        override fun onPageSelected(position: Int) {
            // 페이지 선택 시 호출할 함수를 여기에 작성합니다.
            if (position > prePosition)
                clickFun(1)
            else
                clickFun(-1)
        }
    }
 }
