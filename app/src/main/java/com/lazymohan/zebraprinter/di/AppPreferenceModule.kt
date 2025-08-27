package com.lazymohan.zebraprinter.di

import android.content.Context
import com.lazymohan.zebraprinter.app.AppPref
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppPreferenceModule {
    @Provides
    @Singleton
    fun provideAppPreference(@ApplicationContext context: Context) = AppPref(context)
}