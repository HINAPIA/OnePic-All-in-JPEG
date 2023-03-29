package com.example.onepic

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.onepic.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 바인딩 초기화
        binding = ActivityMainBinding.inflate(layoutInflater);

        // 2. 레이아웃(root뷰) 표시
        setContentView(binding.root)
    }
}