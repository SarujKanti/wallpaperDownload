package com.skd.wallpaper.fragments

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.skd.wallpaper.R
import com.skd.wallpaper.adapters.GenericAdapter
import com.skd.wallpaper.databinding.FragmentAllTabsViewBinding
import com.skd.wallpaper.databinding.ItemCategoryListBinding
import com.skd.wallpaper.network.RetrofitClient
import com.skd.wallpaper.network.dataModel.CategoryList
import com.skd.wallpaper.network.dataModel.WallpaperList
import com.skd.wallpaper.utils.BaseFragment
import com.skd.wallpaper.utils.Validations
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RecentTabsFragment : BaseFragment<FragmentAllTabsViewBinding>(R.layout.fragment_all_tabs_view) {

    override lateinit var binding : FragmentAllTabsViewBinding
    private lateinit var context: Context
    private lateinit var adapter : GenericAdapter<CategoryList, ItemCategoryListBinding>
    private val categoryList = mutableListOf<CategoryList>()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this. context = context
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
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        adapter = GenericAdapter(
            data = categoryList,
            bind = { binding, item, _ ->
                val screenWidth = Resources.getSystem().displayMetrics.widthPixels
                val layoutParams = binding.root.layoutParams
                layoutParams.width = screenWidth / 3
                binding.root.layoutParams = layoutParams
                binding.tvName.text = item.name
                if (!item.image.isNullOrEmpty()) {
                    try {
                        val decodedBytes = Base64.decode(item.image, Base64.DEFAULT)
                        val decodedImageUrl = String(decodedBytes)
                        if (decodedImageUrl.startsWith("http") && !decodedImageUrl.contains("undefined")) {
                            Glide.with(binding.categoryImage.context)
                                .load(decodedImageUrl)
                                .placeholder(R.drawable.ic_launcher_background)
                                .error(R.drawable.ic_launcher_background)
                                .into(binding.categoryImage)
                        } else {
                            Validations.setImageForName(item.name, binding.categoryImage)
                        }
                    } catch (e: IllegalArgumentException) {
                        Validations.setImageForName(item.name, binding.categoryImage)
                    }
                } else {
                    Validations.setImageForName(item.name, binding.categoryImage)
                }
            },
            inflater = { inflater, parent, _ ->
                ItemCategoryListBinding.inflate(inflater, parent, false)
            }
        )
        binding.recyclerView.adapter = adapter
    }


    private fun getBundleData(){
        val apiService = RetrofitClient.getListCategory(requireContext())
        apiService.getCategoryList()
            .enqueue(object : Callback<WallpaperList> {
                override fun onResponse(
                    call: Call<WallpaperList>,
                    response: Response<WallpaperList>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        categoryList.clear()
                        categoryList.addAll(response.body()!!.data)
                        adapter.notifyDataSetChanged()
                    }
                }

                override fun onFailure(call: Call<WallpaperList>, t: Throwable) {
                    Toast.makeText(requireContext(), "saruj", Toast.LENGTH_SHORT).show()
                }
            })

    }
}