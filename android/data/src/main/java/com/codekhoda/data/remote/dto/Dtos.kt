package com.codekhoda.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AppMetadataDto(
    @SerializedName("package_name") val packageName: String,
    @SerializedName("version_code") val versionCode: Long,
    @SerializedName("signature") val signature: String,
    @SerializedName("permissions") val permissions: List<String>
)

data class ScanResultDto(
    @SerializedName("package_name") val packageName: String,
    @SerializedName("risk_level") val riskLevel: String,
    @SerializedName("threat_type") val threatType: String,
    @SerializedName("description") val description: String,
    @SerializedName("heuristics_used") val heuristicsUsed: List<String>
)
