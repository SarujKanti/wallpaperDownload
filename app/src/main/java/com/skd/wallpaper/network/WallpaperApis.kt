package com.skd.wallpaper.network

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface WallhavenApi {

    @GET("search")
    suspend fun search(
        @Query("q") query: String? = null,
        @Query("colors") colors: String? = null,
        @Query("sorting") sorting: String = "favorites",
        @Query("page") page: Int = 1,
        @Query("categories") categories: String = "111",
        // 100 = SFW only
        @Query("purity") purity: String = "100",
        @Query("ratios") ratios: String = "portrait",
        @Query("topRange") topRange: String? = null
    ): WallhavenResponse
}

interface PicsumApi {

    @GET("v2/list")
    suspend fun list(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 30
    ): List<PicsumPhoto>
}

data class WallhavenResponse(
    @SerializedName("data") val data: List<WallhavenWallpaper> = emptyList(),
    @SerializedName("meta") val meta: WallhavenMeta? = null
)

data class WallhavenMeta(
    @SerializedName("current_page") val currentPage: Int = 1,
    @SerializedName("last_page") val lastPage: Int = 1,
    @SerializedName("total") val total: Int = 0
)

data class WallhavenWallpaper(
    @SerializedName("id") val id: String,
    @SerializedName("url") val url: String?,
    @SerializedName("views") val views: Int = 0,
    @SerializedName("favorites") val favorites: Int = 0,
    @SerializedName("category") val category: String?,
    @SerializedName("dimension_x") val width: Int = 0,
    @SerializedName("dimension_y") val height: Int = 0,
    @SerializedName("file_size") val fileSize: Long = 0,
    @SerializedName("file_type") val fileType: String?,
    @SerializedName("colors") val colors: List<String>?,
    @SerializedName("path") val path: String,
    @SerializedName("thumbs") val thumbs: WallhavenThumbs?
)

data class WallhavenThumbs(
    @SerializedName("large") val large: String?,
    @SerializedName("original") val original: String?,
    @SerializedName("small") val small: String?
)

data class PicsumPhoto(
    @SerializedName("id") val id: String,
    @SerializedName("author") val author: String?,
    @SerializedName("width") val width: Int,
    @SerializedName("height") val height: Int,
    @SerializedName("url") val url: String?,
    @SerializedName("download_url") val downloadUrl: String
)
