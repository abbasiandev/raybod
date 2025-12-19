package com.codekhoda.data.remote.api

import com.codekhoda.data.remote.dto.*
import okhttp3.ResponseBody
import retrofit2.http.*

interface CloudBrainApi {
    @POST("api/v1/scan/analyze")
    suspend fun analyzeApp(@Body metadata: AppMetadataDto): ScanResultDto

    @GET("api/v1/allowlist/check/{package_name}")
    suspend fun checkAllowlist(@Path("package_name") packageName: String): AllowlistCheckDto

    @GET("api/v1/threats/feed")
    suspend fun getThreatFeed(
        @Query("limit") limit: Int = 10,
        @Query("offset") offset: Int = 0
    ): ThreatFeedDto

    @GET("api/v1/reputation/{package_name}")
    suspend fun getReputation(@Path("package_name") packageName: String): ReputationDto

    @POST("api/v1/devices/register")
    suspend fun registerDevice(@Body request: DeviceRegistrationDto): DeviceDto

    @POST("api/v1/scan/batch")
    suspend fun batchScan(@Body request: BatchScanRequestDto): BatchScanResultDto

    @GET("api/v1/models/current")
    suspend fun getCurrentModel(): ModelVersionDto

    @GET("api/v1/models/download/{version}")
    suspend fun downloadModel(@Path("version") version: String): ResponseBody
}
