package com.example.onepic

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.onepic.databinding.ActivityMainBinding
import android.view.View


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater);

        setContentView(binding.root)

        closeBottomBar()
    }
    private fun closeBottomBar() {
        // 전체 화면 모드로 설정
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                )

        // 하단 네비게이션 바 숨기기
        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            if ((visibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
                window.decorView.systemUiVisibility = (
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        )
            }
        }
    }
}