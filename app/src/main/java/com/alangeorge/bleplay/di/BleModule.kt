package com.alangeorge.bleplay.di

import com.alangeorge.bleplay.repository.BleRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
open class BleModule {
    @Singleton
    @Provides
    fun provideBleRepository(): BleRepository = BleRepository()
}