package com.goldenratio.onepic.CameraModule

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.goldenratio.onepic.JpegViewModel
import com.goldenratio.onepic.JpegViewModelFactory
import com.goldenratio.onepic.PictureModule.MCContainer
import com.goldenratio.onepic.R
import com.goldenratio.onepic.databinding.ActivityCameraEditorBinding

class CameraEditorActivity : AppCompatActivity() {

    private lateinit var binding : ActivityCameraEditorBinding

    private lateinit var jpegViewModelFactory: JpegViewModelFactory
    private lateinit var jpegViewModels: JpegViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCameraEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        jpegViewModelFactory = JpegViewModelFactory(this)
        jpegViewModels = ViewModelProvider(this, jpegViewModelFactory).get(JpegViewModel::class.java)

        var MCContainer = MCContainer(this)
        jpegViewModels.setContainer(MCContainer)

    }
}