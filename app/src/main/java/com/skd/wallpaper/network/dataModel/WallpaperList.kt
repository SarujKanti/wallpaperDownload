package com.skd.wallpaper.network.dataModel

import retrofit2.Call
import retrofit2.http.GET
import com.skd.wallpaper.network.ApiEndPoints

data class WallpaperList (
    val data : List<CategoryList>
)

data class CategoryList(
    val _id: String,
    val category: String,
    val name: String,
    val image: String
)

interface ListCategoryApi {
    @GET(ApiEndPoints.CATEGORY_LIST)
    fun getCategoryList() : Call<WallpaperList>
}