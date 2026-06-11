package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "email_messages")
data class EmailMessage(
    @PrimaryKey val id: String,
    val accountEmail: String,
    val sender: String,
    val senderName: String,
    val recipient: String,
    val subject: String,
    val body: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val isStarred: Boolean = false,
    val label: String = "INBOX", // INBOX, SENT, DRAFT, TRASH
    val category: String = "Primary", // Primary, Updates, Social, Promotions, Forums
    val tagsString: String = "",
    val htmlBody: String? = null,
    val hasAttachments: Boolean = false
) : Serializable
