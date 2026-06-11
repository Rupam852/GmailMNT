package com.example.data

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("gemini_mail_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_GET_STARTED = "get_started_completed"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_RENDER_BACKEND_URL = "render_backend_url"
        private const val KEY_CUSTOM_TAGS = "custom_tags"
        private const val KEY_DRAFT_RECIPIENT = "draft_recipient"
        private const val KEY_DRAFT_SUBJECT = "draft_subject"
        private const val KEY_DRAFT_BODY = "draft_body"
        private const val KEY_DRAFT_CATEGORY = "draft_category"
    }

    var customTags: Set<String>
        get() = prefs.getStringSet(KEY_CUSTOM_TAGS, setOf("Work", "Personal", "Finance")) ?: setOf("Work", "Personal", "Finance")
        set(value) = prefs.edit().putStringSet(KEY_CUSTOM_TAGS, HashSet(value)).apply()

    var geminiApiKey: String
        get() = prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GEMINI_API_KEY, value).apply()

    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, true)
        set(value) = prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()

    var isGetStartedCompleted: Boolean
        get() = prefs.getBoolean(KEY_GET_STARTED, false)
        set(value) = prefs.edit().putBoolean(KEY_GET_STARTED, value).apply()

    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, value).apply()

    var renderBackendUrl: String
        get() = prefs.getString(KEY_RENDER_BACKEND_URL, "https://gmailmnt.onrender.com") ?: "https://gmailmnt.onrender.com"
        set(value) = prefs.edit().putString(KEY_RENDER_BACKEND_URL, value).apply()

    var draftRecipient: String
        get() = prefs.getString(KEY_DRAFT_RECIPIENT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_DRAFT_RECIPIENT, value).apply()

    var draftSubject: String
        get() = prefs.getString(KEY_DRAFT_SUBJECT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_DRAFT_SUBJECT, value).apply()

    var draftBody: String
        get() = prefs.getString(KEY_DRAFT_BODY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_DRAFT_BODY, value).apply()

    var draftCategory: String
        get() = prefs.getString(KEY_DRAFT_CATEGORY, "Primary") ?: "Primary"
        set(value) = prefs.edit().putString(KEY_DRAFT_CATEGORY, value).apply()

    fun getLastHistoryId(email: String): String? {
        return prefs.getString("history_id_${email.trim().lowercase()}", null)
    }

    fun saveLastHistoryId(email: String, historyId: String) {
        prefs.edit().putString("history_id_${email.trim().lowercase()}", historyId).apply()
    }
}
