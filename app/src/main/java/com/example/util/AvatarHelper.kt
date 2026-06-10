package com.example.util

import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

object AvatarHelper {
    // Thread-safe cache of failed avatar URLs to prevent repeated network retries
    private val failedUrls = ConcurrentHashMap.newKeySet<String>()

    fun getMd5Hash(email: String): String {
        val md = MessageDigest.getInstance("MD5")
        return md.digest(email.trim().lowercase().toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    fun markUrlAsFailed(url: String) {
        failedUrls.add(url)
    }

    fun isUrlFailed(url: String): Boolean {
        return failedUrls.contains(url)
    }
}
