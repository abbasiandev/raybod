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
    @SerializedName("last_update_time") val lastUpdateTime: Long? = null,
    @SerializedName("has_reflection") val hasReflection: Boolean? = null,
    @SerializedName("has_dynamic_loading") val hasDynamicLoading: Boolean? = null
)

data class DrebinFeaturesDto(
    @SerializedName("s1_hardware") val s1Hardware: List<String> = emptyList(),
    @SerializedName("s2_requested_permissions") val s2RequestedPermissions: List<String> = emptyList(),
    @SerializedName("s3_app_components") val s3AppComponents: List<String> = emptyList(),
    @SerializedName("s4_filtered_intents") val s4FilteredIntents: List<String> = emptyList(),
    @SerializedName("s5_restricted_apis") val s5RestrictedApis: List<String> = emptyList(),
    @SerializedName("s6_used_permissions") val s6UsedPermissions: List<String> = emptyList(),
    @SerializedName("s7_suspicious_apis") val s7SuspiciousApis: List<String> = emptyList(),
    @SerializedName("s8_network_addresses") val s8NetworkAddresses: List<String> = emptyList()
)

data class ScanResultDto(
    @SerializedName("package_name") val packageName: String,
    @SerializedName("risk_level") val riskLevel: String,
    @SerializedName("threat_type") val threatType: String,
    @SerializedName("description") val description: String,
    @SerializedName("heuristics_used") val heuristicsUsed: List<String>,
    @SerializedName("drebin_features") val drebinFeatures: DrebinFeaturesDto? = null
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

data class UserDto(
    @SerializedName("id") val id: Int,
    @SerializedName("username") val username: String,
    @SerializedName("role") val role: String,
    @SerializedName("plan") val plan: String,
    @SerializedName("permissions") val permissions: Map<String, Boolean>,
    @SerializedName("is_active") val isActive: Boolean
)

data class NetworkFlowDto(
    @SerializedName("source_app") val sourceApp: String,
    @SerializedName("destination_ip") val destinationIp: String,
    @SerializedName("destination_port") val destinationPort: Int,
    @SerializedName("protocol") val protocol: String,
    @SerializedName("domain") val domain: String? = null,
    @SerializedName("bytes_sent") val bytesSent: Long,
    @SerializedName("bytes_received") val bytesReceived: Long,
    @SerializedName("timestamp") val timestamp: Long
)

data class NetworkAnalysisRequestDto(
    @SerializedName("flows") val flows: List<NetworkFlowDto>
)

data class NetworkAnalysisResultDto(
    @SerializedName("alerts") val alerts: List<NetworkAlertDto>,
    @SerializedName("blocklist") val blocklist: List<BlocklistEntryDto>
)

data class NetworkAlertDto(
    @SerializedName("package_name") val packageName: String,
    @SerializedName("destination") val destination: String,
    @SerializedName("threat_type") val threatType: String,
    @SerializedName("risk_level") val riskLevel: String,
    @SerializedName("description") val description: String,
    @SerializedName("timestamp") val timestamp: Long
)

data class BlocklistEntryDto(
    @SerializedName("pattern") val pattern: String,
    @SerializedName("type") val type: String,
    @SerializedName("reason") val reason: String,
    @SerializedName("timestamp") val timestamp: Long
)
