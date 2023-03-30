package com.example.onepic.Edit.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.example.onepic.R
import com.example.onepic.databinding.FragmentEditBinding


class EditFragment : Fragment(R.layout.fragment_edit) {

    private lateinit var binding: FragmentEditBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 뷰 바인딩 설정
        binding = FragmentEditBinding.inflate(inflater, container, false)

        // Button에 OnClickListener를 설정합니다.
        val view = binding.root

        return view
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rewindBtn.setOnClickListener {
            println("########### click")
            findNavController().navigate(R.id.action_editFragment_to_rewindFragment)
        }
    }
}