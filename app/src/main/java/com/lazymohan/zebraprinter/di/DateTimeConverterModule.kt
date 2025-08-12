package com.lazymohan.zebraprinter.di

import com.lazymohan.zebraprinter.utils.DateTimeConverter
import com.lazymohan.zebraprinter.utils.DateTimeFormat
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DateTimeConverterModule {
    @Provides
    @Singleton
    fun providesDateTimeConfig(
    ): DateTimeConverter = DateTimeConverter(
        dateTimeFormat = DateTimeFormat(
            dateFormat = "yyyy-MM-dd",
            timeFormat = "HH:mm:ss"
        )
    )
}