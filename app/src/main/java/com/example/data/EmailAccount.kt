package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "email_accounts")
data class EmailAccount(
    @PrimaryKey val email: String,
    val displayName: String,
    val provider: String = "Gmail",
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long
) : Serializable
