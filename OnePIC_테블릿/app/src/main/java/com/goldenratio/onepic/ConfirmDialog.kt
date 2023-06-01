package com.goldenratio.onepic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.goldenratio.onepic.databinding.AudioDialogBinding

class ConfirmDialog(val _text : String, confirmDialogInterface: ConfirmDialogInterface) : DialogFragment() {

    // 뷰 바인딩 정의
    private var _binding: AudioDialogBinding? = null
    private val binding get() = _binding!!

    private var confirmDialogInterface: ConfirmDialogInterface? = null

    init {
        this.confirmDialogInterface = confirmDialogInterface

    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AudioDialogBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.confirmTextView.text = _text
        // 취소 버튼 클릭
        binding.noButton.setOnClickListener {
            dismiss()
        }

        // 확인 버튼 클릭
        binding.yesButton.setOnClickListener {
            this.confirmDialogInterface?.onYesButtonClick(id!!)
            dismiss()
        }
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
interface ConfirmDialogInterface {
    fun onYesButtonClick(id: Int)
}