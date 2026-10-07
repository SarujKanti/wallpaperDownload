package com.skd.wallpaper.network

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Both APIs are completely free and need no API key:
 *  - Wallhaven (https://wallhaven.cc/help/api) – real wallpapers, search, categories, colors.
 *    Only SFW content is requested (purity=100).
 *  - Lorem Picsum (https://picsum.photos) – used as a fallback if Wallhaven is unreachable.
 */
object RetrofitClient {

    private const val WALLHAVEN_URL = "https://wallhaven.cc/api/v1/"
    private const val PICSUM_URL = "https://picsum.photos/"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", "WallpaperView-Android/1.0")
                        .build()
                )
            }
            .build()
    }

    private fun retrofit(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(GsonBuilder().disableHtmlEscaping().create()))
        .build()

    val wallhaven: WallhavenApi by lazy { retrofit(WALLHAVEN_URL).create(WallhavenApi::class.java) }
    val picsum: PicsumApi by lazy { retrofit(PICSUM_URL).create(PicsumApi::class.java) }
}
