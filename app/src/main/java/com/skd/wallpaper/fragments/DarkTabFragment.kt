package com.skd.wallpaper.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.skd.wallpaper.R
import com.skd.wallpaper.databinding.FragmentAllTabsViewBinding
import com.skd.wallpaper.utils.BaseFragment

class DarkTabFragment: BaseFragment<FragmentAllTabsViewBinding>(R.layout.fragment_all_tabs_view) {

    override lateinit var binding : FragmentAllTabsViewBinding
    private lateinit var context: Context

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.context = context
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getBundleData()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAllTabsViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init(){
        binding.tvTitle.text = "Dark Text"
    }


    private fun getBundleData(){

    }
}