package com.lazymohan.zebraprinter.scan.di

import com.lazymohan.zebraprinter.scan.data.FakeScanGrnRepository
import com.lazymohan.zebraprinter.scan.data.ScanGrnRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ScanGrnModule {
    @Provides @Singleton
    fun provideScanGrnRepository(): ScanGrnRepository = FakeScanGrnRepository()
}
