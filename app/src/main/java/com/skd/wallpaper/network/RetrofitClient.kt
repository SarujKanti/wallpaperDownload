package com.skd.wallpaper.network

import android.content.Context
import com.google.gson.GsonBuilder
import com.skd.wallpaper.network.dataModel.ListCategoryApi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class AuthInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)
        val requestBuilder = chain.request().newBuilder()

        // Add Authorization header with Bearer token if available
        token?.let {
            requestBuilder.addHeader("Authorization", "Bearer $it")
        }

        return chain.proceed(requestBuilder.build())
    }
}

object RetrofitClient {

    private const val URL = "https://prod.gruppie.in/api/v1/groups/61a7c52f97d24b6a5eb75252/"
    private fun getRetrofit(context: Context): Retrofit {
        // Logging interceptor to log the request and response bodies
//        val logging = HttpLoggingInterceptor().apply {
//            level = HttpLoggingInterceptor.Level.BODY
//        }

    val client = OkHttpClient.Builder()
//        .addInterceptor(logging) // For logging
        .addInterceptor(AuthInterceptor(context))
        .build()

    val builder = GsonBuilder().disableHtmlEscaping().create()
    return Retrofit.Builder()
    .baseUrl(URL)
    .client(client)
    .addConverterFactory(GsonConverterFactory.create(builder))
    .build()
    }

    fun getListCategory(context: Context): ListCategoryApi {
        return getRetrofit(context).create(ListCategoryApi::class.java)
    }
}