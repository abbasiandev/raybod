package com.codekhoda.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "network_blocklist")
data class BlocklistEntity(
    @PrimaryKey val pattern: String, // IP or Domain
    val type: String, // IP, DOMAIN
    val reason: String,
    val timestamp: Long
)


