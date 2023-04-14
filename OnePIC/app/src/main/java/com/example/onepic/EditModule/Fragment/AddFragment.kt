package com.example.onepic.EditModule.Fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.onepic.R
import com.example.onepic.databinding.FragmentAddBinding

class AddFragment : Fragment() {

    private lateinit var binding: FragmentAddBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentAddBinding.inflate(inflater, container, false)

        // save btn 클릭 시
        binding.addSaveBtn.setOnClickListener {
            findNavController().navigate(R.id.action_addFragment_to_editFragment)
        }

        // close btn 클릭 시
        binding.addCloseBtn.setOnClickListener {
            findNavController().navigate(R.id.action_addFragment_to_editFragment)
        }

        return binding.root
    }

}