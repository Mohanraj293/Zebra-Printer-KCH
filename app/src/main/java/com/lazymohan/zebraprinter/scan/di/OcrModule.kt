// app/src/main/java/com/lazymohan/zebraprinter/scan/di/OcrModule.kt
package com.lazymohan.zebraprinter.scan.di

import com.lazymohan.zebraprinter.scan.data.OcrApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OcrModule {
    @Provides @Singleton
    fun provideOcrApi(retrofit: Retrofit): OcrApi =
        retrofit.create(OcrApi::class.java)
}
