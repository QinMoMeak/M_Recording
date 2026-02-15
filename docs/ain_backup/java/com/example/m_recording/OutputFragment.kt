 if ($args[0].Groups[1].Value -eq '.data') { 'package com.qinmomeak.recording.data' } else { 'package com.qinmomeak.recording' } 

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.qinmomeak.recording.databinding.FragmentOutputBinding

class OutputFragment : Fragment() {

    private var tabType: Int = 0
    private var binding: FragmentOutputBinding? = null
    private var outputViewModel: OutputViewModel? = null
    private var observer: Observer<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tabType = arguments?.getInt(ARG_TYPE) ?: 0
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val fragBinding = FragmentOutputBinding.inflate(inflater, container, false)
        binding = fragBinding
        return fragBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val model = ViewModelProvider(requireActivity())[OutputViewModel::class.java]
        outputViewModel = model
        observer = Observer { text ->
            val shown = if (text.isBlank()) "鏆傛棤鍐呭" else text
            binding?.outputText?.text = shown
        }
        if (tabType == 0) {
            model.transcript.observe(viewLifecycleOwner, observer!!)
        } else {
            model.summary.observe(viewLifecycleOwner, observer!!)
        }
        val initial = if (tabType == 0) model.transcript.value.orEmpty() else model.summary.value.orEmpty()
        val shown = if (initial.isBlank()) "鏆傛棤鍐呭" else initial
        binding?.outputText?.text = shown
    }

    override fun onDestroyView() {
        super.onDestroyView()
        observer = null
        binding = null
    }

    companion object {
        private const val ARG_TYPE = "arg_type"

        fun newInstance(type: Int): OutputFragment {
            return OutputFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_TYPE, type)
                }
            }
        }
    }
}


