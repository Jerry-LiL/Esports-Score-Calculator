package com.lilranker.tournament.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit client for API calls
 */
object RetrofitClient {
    
    // TODO: Change this to your production server URL when deploying
    // For production: "https://your-backend.herokuapp.com/" or your cloud server
    // For development with real device: Use your computer's IP
    // For development with emulator: "http://10.0.2.2:3000/"
    // Use deployed backend for production
    private const val BASE_URL = "https://esports-score-calculator.onrender.com/"
    
    // Alternative: Use BuildConfig to switch between dev and prod
    // private val BASE_URL = if (BuildConfig.DEBUG) {
    //     "http://192.168.1.5:3000/"  // Development
    // } else {
    //     "https://your-backend.herokuapp.com/"  // Production
    // }
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val authApiService: AuthApiService = retrofit.create(AuthApiService::class.java)
}
