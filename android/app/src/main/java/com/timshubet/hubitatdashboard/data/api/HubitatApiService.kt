package com.timshubet.hubitatdashboard.data.api

import com.google.gson.JsonElement
import com.timshubet.hubitatdashboard.data.model.DeviceState
import com.timshubet.hubitatdashboard.data.model.HubMode
import com.timshubet.hubitatdashboard.data.model.HubVariable
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface HubitatApiService {

    @GET("devices/all")
    suspend fun getAllDevices(
        @Query("access_token") token: String
    ): List<DeviceState>

    // Cloud Maker API uses /devices (not /devices/all)
    @GET("devices")
    suspend fun listDevicesCloud(
        @Query("access_token") token: String
    ): List<DeviceState>

    @GET("devices/{id}")
    suspend fun getDevice(
        @Path("id") deviceId: String,
        @Query("access_token") token: String
    ): DeviceState

    @GET("devices/{id}/{command}")
    suspend fun sendCommand(
        @Path("id") deviceId: String,
        @Path("command") command: String,
        @Query("access_token") token: String
    ): Response<JsonElement>

    @GET("devices/{id}/{command}/{value}")
    suspend fun sendCommandWithValue(
        @Path("id") deviceId: String,
        @Path("command") command: String,
        @Path("value") value: String,
        @Query("access_token") token: String
    ): Response<JsonElement>

    @GET("hsm")
    suspend fun getHsmStatus(
        @Query("access_token") token: String
    ): HsmStatusResponse

    @GET("hsm/{mode}")
    suspend fun setHsmMode(
        @Path("mode") mode: String,
        @Query("access_token") token: String
    ): Response<JsonElement>

    @GET("modes")
    suspend fun getModes(
        @Query("access_token") token: String
    ): List<HubMode>

    @GET("modes/{id}")
    suspend fun setMode(
        @Path("id") modeId: String,
        @Query("access_token") token: String
    ): Response<JsonElement>

    @GET("hubvariables")
    suspend fun getHubVariables(
        @Query("access_token") token: String
    ): List<HubVariable>

    @GET("hubvariables/{name}/{value}")
    suspend fun setHubVariable(
        @Path("name") name: String,
        @Path("value", encoded = true) value: String,
        @Query("access_token") token: String
    ): Response<JsonElement>
}

data class HsmStatusResponse(val hsm: String)
