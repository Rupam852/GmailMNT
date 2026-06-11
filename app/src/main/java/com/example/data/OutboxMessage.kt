package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "outbox_messages")
data class OutboxMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fromEmail: String,
    val toEmail: String,
    val subject: String,
    val body: String,
    val threadId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val attachmentUris: String = ""
) : Serializable
