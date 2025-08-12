// app/src/main/java/com/lazymohan/zebraprinter/di/FusionModule.kt
package com.lazymohan.zebraprinter.di

import com.lazymohan.zebraprinter.BuildConfig
import com.lazymohan.zebraprinter.grn.data.FusionApi
import com.lazymohan.zebraprinter.grn.data.GrnRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FusionModule {

    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient {
        val authHeader = Credentials.basic(
            BuildConfig.FUSION_USERNAME,
            BuildConfig.FUSION_PASSWORD
        )

        val authInterceptor = Interceptor { chain ->
            val req = chain.request().newBuilder()
                .addHeader("Authorization", authHeader)
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .build()
            chain.proceed(req)
        }

        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
            else HttpLoggingInterceptor.Level.NONE
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(45, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            // Make sure FUSION_BASE_URL ends with a trailing slash in BuildConfig.
            .baseUrl(BuildConfig.FUSION_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideFusionApi(retrofit: Retrofit): FusionApi =
        retrofit.create(FusionApi::class.java)

    @Provides
    @Singleton
    fun provideGrnRepository(api: FusionApi): GrnRepository =
        GrnRepository(api)
}
