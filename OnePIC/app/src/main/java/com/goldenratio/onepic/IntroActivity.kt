package com.goldenratio.onepic

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.goldenratio.onepic.CameraModule.CameraEditorActivity
import com.goldenratio.onepic.databinding.ActivityIntroBinding


class IntroActivity : AppCompatActivity() {
    private lateinit var binding: ActivityIntroBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 상태바 색상 변경
        val window: Window = this.window
            ?: throw IllegalStateException("Fragment is not attached to an activity")
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.setStatusBarColor(ContextCompat.getColor(applicationContext, R.color.intro_color))


        binding = ActivityIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Loadingstart()
    }
    private fun Loadingstart() {
        val handler = Handler()
        Glide.with(this).load(R.raw.onepic_intro).into(binding.intoImage)
        handler.postDelayed(Runnable {
            val intent = Intent(applicationContext, CameraEditorActivity::class.java)
            startActivity(intent)
            finish()
        }, 2800)
    }
}