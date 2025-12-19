package com.codekhoda.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "network_alerts")
data class NetworkAlertEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val packageName: String,
    val destination: String,
    val threatType: String,
    val riskLevel: String,
    val description: String,
    val timestamp: Long
)

