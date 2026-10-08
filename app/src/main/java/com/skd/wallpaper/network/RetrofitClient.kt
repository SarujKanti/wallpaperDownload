package com.skd.wallpaper.network

import com.google.gson.GsonBuilder
import com.skd.wallpaper.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Wallpaper sources (Wallhaven and Picsum are free and need no API key):
 *  - Wallhaven (https://wallhaven.cc/help/api) – real wallpapers, search, categories, colors.
 *    Only SFW content is requested (purity=100).
 *  - Lorem Picsum (https://picsum.photos) – used as a fallback if Wallhaven is unreachable.
 *  - Supabase Storage – your own uploaded wallpapers (URL + anon key from local.properties).
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

    /** Same client plus the Supabase anon key, which the Storage API requires on every call. */
    private val supabaseClient: OkHttpClient by lazy {
        client.newBuilder()
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                        .header("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
                        .build()
                )
            }
            .build()
    }

    val isSupabaseConfigured: Boolean
        get() = BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    private fun retrofit(baseUrl: String, httpClient: OkHttpClient = client): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create(GsonBuilder().disableHtmlEscaping().create()))
        .build()

    val wallhaven: WallhavenApi by lazy { retrofit(WALLHAVEN_URL).create(WallhavenApi::class.java) }
    val picsum: PicsumApi by lazy { retrofit(PICSUM_URL).create(PicsumApi::class.java) }
    val supabase: SupabaseStorageApi by lazy {
        retrofit(BuildConfig.SUPABASE_URL.trimEnd('/') + "/", supabaseClient).create(SupabaseStorageApi::class.java)
    }
}
