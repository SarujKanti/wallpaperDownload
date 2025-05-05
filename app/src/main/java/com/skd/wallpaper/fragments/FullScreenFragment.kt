package com.skd.wallpaper.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.skd.wallpaper.R
import com.skd.wallpaper.activities.MainDashboardActivity
import com.skd.wallpaper.adapters.MultipleTabsAdapter
import com.skd.wallpaper.databinding.FragmentFullScreenBinding
import com.skd.wallpaper.utils.BaseFragment

class FullScreenFragment : BaseFragment<FragmentFullScreenBinding>(R.layout.fragment_full_screen){
    override lateinit var binding :FragmentFullScreenBinding
    private lateinit var context: Context
    private var toolBarTitle: String? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.context = context
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getBundleDate()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentFullScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setTabsFragment()
        tab()
    }

    override fun onResume() {
        super.onResume()
        containerActivity().supportActionBar?.title = toolBarTitle
    }

    private fun containerActivity() = (activity as MainDashboardActivity)

    private fun getBundleDate(){
        arguments?.let {

        }
    }

    private fun setTabsFragment(){
        val fragmentList = listOf(
            RecentTabsFragment(),
            BookmarkedTabsFragment(),
            DarkTabFragment(),
            ColorfullTabFragment()
        )

        val adapter = MultipleTabsAdapter(this, fragmentList)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager){ tab, position ->
            val tabView = LayoutInflater.from(context).inflate(R.layout.tab_item, null)
            val tabText = tabView.findViewById<TextView>(R.id.tabText)
            tab.text = null // remove default text
            tab.customView = tabView
            when (position){
                0 -> tabText.text = "Recent"
                1 -> tabText.text = "Liked"
                2 -> tabText.text = "Dark"
                3 -> tabText.text = "ColorFull"
            }
        }.attach()

        binding.tabLayout.post {
            val firstTab = binding.tabLayout.getTabAt(0)
            val tabView = firstTab?.customView
            tabView?.setBackgroundResource(R.drawable.tab_background_selected)
            tabView?.findViewById<TextView>(R.id.tabText)?.setTextColor(resources.getColor(R.color.black))
        }
    }

    private fun tab(){
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val tabView = tab.customView
                tabView?.setBackgroundResource(R.drawable.tab_background_selected)
                tabView?.findViewById<TextView>(R.id.tabText)?.setTextColor(resources.getColor(R.color.black))
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                val tabView = tab.customView
                tabView?.setBackgroundResource(R.drawable.tab_background_default)
                tabView?.findViewById<TextView>(R.id.tabText)?.setTextColor(resources.getColor(R.color.gray))
            }

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

    }
}