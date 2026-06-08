package com.tim.hubitatdash.di

// Retrofit is initialized with a placeholder base URL (http://localhost/).
// Repositories must rebuild Retrofit with the resolved URL before making API calls:
//
//   val baseUrl = connectionResolver.resolveBaseUrl()
//   val dynamicRetrofit = retrofit.newBuilder().baseUrl("$baseUrl/").build()
//   val dynamicService = dynamicRetrofit.create(HubitatApiService::class.java)
//   val devices = dynamicService.getAllDevices(settingsRepository.makerToken)

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.tim.hubitatdash.data.api.HubitatApiService
import com.tim.hubitatdash.data.model.DeviceState
import com.tim.hubitatdash.data.model.DeviceStateDeserializer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder()
        .registerTypeAdapter(DeviceState::class.java, DeviceStateDeserializer())
        .create()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        // Base URL is a placeholder — actual URL injected per-call via ConnectionResolver
        return Retrofit.Builder()
            .baseUrl("http://localhost/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideHubitatApiService(retrofit: Retrofit): HubitatApiService {
        return retrofit.create(HubitatApiService::class.java)
    }
}

