package com.codekhoda.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AppMetadataDto(
    @SerializedName("package_name") val packageName: String,
    @SerializedName("version_code") val versionCode: Long,
    @SerializedName("signature") val signature: String,
    @SerializedName("permissions") val permissions: List<String>,
    @SerializedName("ensemble_metadata") val ensembleMetadata: Map<String, Float>? = null,
    // NEW FIELDS
    @SerializedName("intents") val intents: List<String> = emptyList(),
    @SerializedName("version_name") val versionName: String? = null,
    @SerializedName("install_time") val installTime: Long? = null,
    @SerializedName("last_update_time") val lastUpdateTime: Long? = null
)

data class ScanResultDto(
    @SerializedName("package_name") val packageName: String,
    @SerializedName("risk_level") val riskLevel: String,
    @SerializedName("threat_type") val threatType: String,
    @SerializedName("description") val description: String,
    @SerializedName("heuristics_used") val heuristicsUsed: List<String>
)

data class AllowlistCheckDto(
    @SerializedName("package_name") val packageName: String,
    @SerializedName("is_allowed") val isAllowed: Boolean
)

data class ThreatFeedDto(
    @SerializedName("threats") val threats: List<ThreatDto>,
    @SerializedName("total") val total: Int
)

data class ThreatDto(
    @SerializedName("package_name") val packageName: String,
    @SerializedName("risk_level") val riskLevel: String,
    @SerializedName("threat_type") val threatType: String?,
    @SerializedName("timestamp") val timestamp: String
)

data class ReputationDto(
    @SerializedName("package_name") val packageName: String,
    @SerializedName("reputation_score") val reputationScore: Float,
    @SerializedName("confidence") val confidence: String,
    @SerializedName("total_scans") val totalScans: Int
)

data class DeviceRegistrationDto(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("device_model") val deviceModel: String,
    @SerializedName("os_version") val osVersion: String,
    @SerializedName("app_version") val appVersion: String
)

data class DeviceDto(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("last_seen") val lastSeen: String
)

data class BatchScanRequestDto(
    @SerializedName("packages") val packages: List<AppMetadataDto>
)

data class BatchScanResultDto(
    @SerializedName("results") val results: List<ScanResultDto>
)

data class ModelVersionDto(
    @SerializedName("version") val version: String,
    @SerializedName("file_size") val fileSize: Long,
    @SerializedName("checksum_sha256") val checksum: String,
    @SerializedName("download_url") val downloadUrl: String
)
