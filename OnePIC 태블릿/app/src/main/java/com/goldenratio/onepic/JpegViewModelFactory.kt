package com.goldenratio.onepic

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class JpegViewModelFactory (private val context: Context): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JpegViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return JpegViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}