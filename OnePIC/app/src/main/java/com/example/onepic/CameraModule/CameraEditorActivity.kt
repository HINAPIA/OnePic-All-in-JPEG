package com.example.onepic.CameraModule

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.onepic.R
import com.example.onepic.databinding.ActivityCameraEditorBinding

class CameraEditorActivity : AppCompatActivity() {

    private lateinit var binding : ActivityCameraEditorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCameraEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }
}