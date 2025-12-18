package com.codekhoda.data.remote.api

import com.codekhoda.data.remote.dto.AppMetadataDto
import com.codekhoda.data.remote.dto.ScanResultDto
import retrofit2.http.Body
import retrofit2.http.POST

interface CloudBrainApi {
    @POST("api/v1/scan/analyze")
    suspend fun analyzeApp(@Body metadata: AppMetadataDto): ScanResultDto
}
