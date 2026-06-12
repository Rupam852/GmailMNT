package com.example.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = getPrefs(context)

    companion object {
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_GET_STARTED = "get_started_completed"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_RENDER_BACKEND_URL = "render_backend_url"
        private const val KEY_SWIPE_ACTIONS_ENABLED = "swipe_actions_enabled"
        private const val KEY_CUSTOM_TAGS = "custom_tags"
        private const val KEY_DRAFT_RECIPIENT = "draft_recipient"
        private const val KEY_DRAFT_SUBJECT = "draft_subject"
        private const val KEY_DRAFT_BODY = "draft_body"
        private const val KEY_DRAFT_CATEGORY = "draft_category"

        @Volatile
        private var sharedPrefsInstance: SharedPreferences? = null

        private fun getPrefs(context: Context): SharedPreferences {
            return sharedPrefsInstance ?: synchronized(this) {
                sharedPrefsInstance ?: try {
                    SafeSharedPreferences(context, "gemini_mail_prefs_secure", "gemini_mail_prefs_plain")
                } catch (e: Throwable) {
                    // Fallback to plain SharedPreferences if SafeSharedPreferences fails completely
                    context.applicationContext.getSharedPreferences("gemini_mail_prefs_plain", Context.MODE_PRIVATE)
                }.also {
                    sharedPrefsInstance = it
                }
            }
        }
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

    var isNotificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, value).apply()

    var isSwipeActionsEnabled: Boolean
        get() = prefs.getBoolean(KEY_SWIPE_ACTIONS_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_SWIPE_ACTIONS_ENABLED, value).apply()

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

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}

class SafeSharedPreferences(
    private val context: Context,
    private val securePrefsName: String,
    private val plainPrefsName: String
) : SharedPreferences {

    @Volatile
    private var delegate: SharedPreferences = initDelegate()

    private fun initDelegate(): SharedPreferences {
        val appContext = context.applicationContext
        return try {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                appContext,
                securePrefsName,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Throwable) {
            appContext.getSharedPreferences(plainPrefsName, Context.MODE_PRIVATE)
        }
    }

    private fun <T> safeCall(block: (SharedPreferences) -> T, fallbackBlock: () -> T): T {
        return try {
            block(delegate)
        } catch (e: Throwable) {
            val plain = context.applicationContext.getSharedPreferences(plainPrefsName, Context.MODE_PRIVATE)
            if (delegate != plain) {
                delegate = plain
            }
            try {
                block(delegate)
            } catch (ex: Throwable) {
                fallbackBlock()
            }
        }
    }

    override fun getAll(): Map<String, *> = safeCall({ it.all }, { emptyMap<String, Any>() })
    override fun getString(key: String, defValue: String?): String? = safeCall({ it.getString(key, defValue) }, { defValue })
    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? = safeCall({ it.getStringSet(key, defValues) }, { defValues })
    override fun getInt(key: String, defValue: Int): Int = safeCall({ it.getInt(key, defValue) }, { defValue })
    override fun getLong(key: String, defValue: Long): Long = safeCall({ it.getLong(key, defValue) }, { defValue })
    override fun getFloat(key: String, defValue: Float): Float = safeCall({ it.getFloat(key, defValue) }, { defValue })
    override fun getBoolean(key: String, defValue: Boolean): Boolean = safeCall({ it.getBoolean(key, defValue) }, { defValue })
    override fun contains(key: String): Boolean = safeCall({ it.contains(key) }, { false })

    override fun edit(): SharedPreferences.Editor {
        val currentDelegate = delegate
        return SafeEditor(currentDelegate.edit(), this)
    }

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
        delegate.registerOnSharedPreferenceChangeListener(listener)
    }
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
        delegate.unregisterOnSharedPreferenceChangeListener(listener)
    }

    class SafeEditor(
        private var editor: SharedPreferences.Editor,
        private val safePrefs: SafeSharedPreferences
    ) : SharedPreferences.Editor {
        private fun safeEdit(block: (SharedPreferences.Editor) -> SharedPreferences.Editor): SharedPreferences.Editor {
            return try {
                block(editor)
                this
            } catch (e: Throwable) {
                safePrefs.safeCall({
                    editor = it.edit()
                    block(editor)
                }, { })
                this
            }
        }

        override fun putString(key: String, value: String?): SharedPreferences.Editor = safeEdit { it.putString(key, value) }
        override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor = safeEdit { it.putStringSet(key, values) }
        override fun putInt(key: String, value: Int): SharedPreferences.Editor = safeEdit { it.putInt(key, value) }
        override fun putLong(key: String, value: Long): SharedPreferences.Editor = safeEdit { it.putLong(key, value) }
        override fun putFloat(key: String, value: Float): SharedPreferences.Editor = safeEdit { it.putFloat(key, value) }
        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor = safeEdit { it.putBoolean(key, value) }
        override fun remove(key: String): SharedPreferences.Editor = safeEdit { it.remove(key) }
        override fun clear(): SharedPreferences.Editor = safeEdit { it.clear() }

        override fun commit(): Boolean {
            return try {
                editor.commit()
            } catch (e: Throwable) {
                safePrefs.safeCall({
                    editor = it.edit()
                    editor.commit()
                }, { false })
            }
        }

        override fun apply() {
            try {
                editor.apply()
            } catch (e: Throwable) {
                safePrefs.safeCall({
                    editor = it.edit()
                    editor.apply()
                }, { })
            }
        }
    }
}
