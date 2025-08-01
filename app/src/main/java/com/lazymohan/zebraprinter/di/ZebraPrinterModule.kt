package com.lazymohan.zebraprinter.di

import android.content.Context
import com.lazymohan.zebraprinter.PrinterImpl
import com.lazymohan.zebraprinter.PrinterService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object ZebraPrinterModule {

    @Provides
    @Singleton
    fun providePrinterService(
        @ApplicationContext context: Context,
    ): PrinterService = PrinterImpl(context = context)
}