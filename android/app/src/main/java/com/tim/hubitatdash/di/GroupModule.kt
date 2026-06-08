package com.tim.hubitatdash.di

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GroupModule {
    @Provides
    @Singleton
    @Named("group")
    fun provideGroupSharedPreferences(@ApplicationContext context: Context): SharedPreferences =
        context.getSharedPreferences("group_store", Context.MODE_PRIVATE)
}

