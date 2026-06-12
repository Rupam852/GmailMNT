package com.example.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.api.Content
import com.example.api.GeminiRetrofitClient
import com.example.api.GenerateContentRequest
import com.example.api.Part
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

class EmailRepository(private val context: Context) {
    private val db = EmailDatabase.getDatabase(context)
    private val dao = db.emailDao()
    private val preferences = PreferenceManager(context)  // Reuse single instance
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // Exposed Flows for UI
    val allAccounts: Flow<List<EmailAccount>> = dao.getAllAccounts()
    val allMessages: Flow<List<EmailMessage>> = dao.getAllMessages()

    fun getMessagesForAccount(email: String): Flow<List<EmailMessage>> {
        return dao.getMessagesForAccount(email)
    }

    fun searchMessages(query: String): Flow<List<EmailMessage>> {
        return dao.searchMessages(query)
    }

    suspend fun getAccountByEmail(email: String): EmailAccount? = withContext(Dispatchers.IO) {
        dao.getAccountByEmail(email)
    }

    suspend fun addAccount(account: EmailAccount) = withContext(Dispatchers.IO) {
        dao.insertAccount(account)
        // Auto sync after adding
        syncEmailsForAccount(account.email)
    }

    suspend fun deleteAccount(email: String) = withContext(Dispatchers.IO) {
        dao.deleteAccount(email)
    }

    suspend fun exchangeCodeForTokens(exchangeCode: String, backendUrl: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("exchange_code", exchangeCode)
            }
            val requestBody = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$backendUrl/exchange")
                .post(requestBody)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: return@withContext null
                    return@withContext JSONObject(responseBody)
                }
            }
        } catch (e: Exception) {
            Log.e("EmailRepository", "Error exchanging code for tokens", e)
        }
        return@withContext null
    }

    suspend fun updateMessage(message: EmailMessage) = withContext(Dispatchers.IO) {
        dao.updateMessage(message)
    }

    suspend fun getMessageById(id: String): EmailMessage? = withContext(Dispatchers.IO) {
        dao.getMessageById(id)
    }

    suspend fun updateMessageReadStatus(id: String, isRead: Boolean) = withContext(Dispatchers.IO) {
        dao.updateMessageReadStatus(id, isRead)
    }

    suspend fun markAllMessagesReadStatus(isRead: Boolean) = withContext(Dispatchers.IO) {
        dao.updateAllMessagesReadStatus(isRead)
    }

    suspend fun updateMessageStarredStatus(id: String, isStarred: Boolean) = withContext(Dispatchers.IO) {
        dao.updateMessageStarredStatus(id, isStarred)
    }

    suspend fun deleteMessage(id: String) = withContext(Dispatchers.IO) {
        dao.deleteMessageById(id)
    }

    suspend fun updateMessageLabel(id: String, label: String) = withContext(Dispatchers.IO) {
        dao.updateMessageLabel(id, label)
    }

    suspend fun emptyTrash() = withContext(Dispatchers.IO) {
        dao.emptyTrash()
    }

    suspend fun restoreAllTrash() = withContext(Dispatchers.IO) {
        dao.restoreAllTrash()
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        db.clearAllTables()
        preferences.clearAll()
    }

    suspend fun insertMessage(message: EmailMessage) = withContext(Dispatchers.IO) {
        dao.insertMessage(message)
    }

    /**
     * Call Render backend to refresh access token using local refresh token.
     */
    suspend fun refreshAccessToken(accountEmail: String, backendUrl: String): Boolean = withContext(Dispatchers.IO) {
        val account = dao.getAccountByEmail(accountEmail) ?: return@withContext false
        if (account.refreshToken.isEmpty()) {
            return@withContext false
        }

        try {
            val json = JSONObject().apply {
                put("refresh_token", account.refreshToken)
            }
            val requestBody = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$backendUrl/refresh")
                .post(requestBody)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: return@withContext false
                    val responseJson = JSONObject(responseBody)
                    val newAccessToken = responseJson.getString("access_token")
                    val expiresAt = responseJson.getLong("expires_at")

                    val updatedAccount = account.copy(
                        accessToken = newAccessToken,
                        expiresAt = expiresAt
                    )
                    dao.insertAccount(updatedAccount)
                    Log.d("EmailRepository", "Token refreshed successfully for $accountEmail")
                    return@withContext true
                }
            }
        } catch (e: Exception) {
            Log.e("EmailRepository", "Error refreshing token", e)
        }
        return@withContext false
    }

    /**
     * Synchronize emails for a specific account.
     * If it's a real OAuth account, fetches from Gmail API (or simulates detailed fetch).
     * Populates database with structured records.
     */
    suspend fun syncEmailsForAccount(accountEmail: String) = withContext(Dispatchers.IO) {
        val account = dao.getAccountByEmail(accountEmail)
        
        val pref = preferences
        if (account != null && !accountEmail.lowercase().contains("simulated")) {
            val savedHistoryId = pref.getLastHistoryId(accountEmail)
            var activeToken = account.accessToken
            if (account.refreshToken.isNotEmpty() && account.expiresAt < System.currentTimeMillis() + 5 * 60 * 1000) {
                val backendUrl = pref.renderBackendUrl
                val refreshed = refreshAccessToken(accountEmail, backendUrl)
                if (refreshed) {
                    activeToken = dao.getAccountByEmail(accountEmail)?.accessToken ?: account.accessToken
                }
            }

            if (!savedHistoryId.isNullOrEmpty()) {
                Log.d("EmailRepository", "Attempting differential sync using historyId: $savedHistoryId")
                val result = syncEmailsHistory(accountEmail, activeToken, savedHistoryId)
                when (result) {
                    is HistorySyncResult.Success -> {
                        Log.d("EmailRepository", "Differential history sync succeeded!")
                        return@withContext
                    }
                    is HistorySyncResult.Expired -> {
                        Log.w("EmailRepository", "History ID expired, falling back to full sync.")
                    }
                    is HistorySyncResult.Error -> {
                        Log.e("EmailRepository", "History sync failed: ${result.message}, falling back to full sync.")
                    }
                }
            }
        }
        
        // Populate realistic clean, production-ready emails
        val currentTime = System.currentTimeMillis()
        val mockMessages = if (accountEmail.lowercase().contains("simulated") || account == null) {
            listOf(
                EmailMessage(
                    id = "msg_1_${accountEmail}",
                    accountEmail = accountEmail,
                    senderName = "GmailMNT Team",
                    sender = "copilot@gmail-mnt.com",
                    recipient = accountEmail,
                    subject = "Welcome to your GmailMNT Co-Pilot Workspace",
                    body = "We are thrilled to welcome you to GmailMNT! Your account is fully configured for smart composing, custom categorize streams, and biometric verification. Tap on the Spark icon when drafting a new email to let Gemini assist you with writing high-quality professional responses instantly.",
                    timestamp = currentTime - 5 * 60 * 1000,
                    isRead = false,
                    isStarred = true,
                    category = "Primary"
                ),
                EmailMessage(
                    id = "msg_2_${accountEmail}",
                    accountEmail = accountEmail,
                    senderName = "Google Security",
                    sender = "security-noreply@google.com",
                    recipient = accountEmail,
                    subject = "Security Status: Device successfully authorized",
                    body = "We detected a successful connection from your authorized Android client to your workspace account. To protect your email, GmailMNT automatically enforces local biometric lock and local encryption. If this was you, no action is required.",
                    timestamp = currentTime - 30 * 60 * 1000,
                    isRead = true,
                    isStarred = false,
                    category = "Updates"
                ),
                EmailMessage(
                    id = "msg_3_${accountEmail}",
                    accountEmail = accountEmail,
                    senderName = "Workspace Operations",
                    sender = "dev-team@gemini-mail.io",
                    recipient = accountEmail,
                    subject = "Custom Tagging and Folder Categorization Released",
                    body = "Organizing your messages has never been easier. You can now define custom tags dynamically from the tag management drawer. These tags integrate directly with our clean vertical and horizontal feed selectors to let you stay focused.",
                    timestamp = currentTime - 120 * 60 * 1000,
                    isRead = true,
                    isStarred = true,
                    category = "Updates"
                ),
                EmailMessage(
                    id = "msg_4_${accountEmail}",
                    accountEmail = accountEmail,
                    senderName = "Spam Promo Group",
                    sender = "spammer@spamlink.xyz",
                    recipient = accountEmail,
                    subject = "Congratulations! You won a $1000 Gift Card!",
                    body = "Click this link now to claim your reward. This is a limited time offer and will expire in 2 hours. Go to spamlink.xyz to verify your identity.",
                    timestamp = currentTime - 8 * 60 * 1000,
                    isRead = true,
                    isStarred = false,
                    category = "Promotions",
                    label = "SPAM"
                ),
                EmailMessage(
                    id = "msg_5_${accountEmail}",
                    accountEmail = accountEmail,
                    senderName = "Old Invoice / Archive",
                    sender = "billing@cloud-services.net",
                    recipient = accountEmail,
                    subject = "Invoice for May 2026 - Paid",
                    body = "Your payment of $49.00 for cloud resources has been successfully processed. This message has been archived for bookkeeping purposes.",
                    timestamp = currentTime - 10 * 60 * 1000,
                    isRead = true,
                    isStarred = false,
                    category = "Primary",
                    label = "ARCHIVE"
                )
            )
        } else {
            // For a real account, we query Google APIs using the access token.
            // Under normal circumstances, this uses OAuth.
            // If OAuth token has expired, we attempt refreshing.
            val list = mutableListOf<EmailMessage>()
            try {
                // Re-fetch fresh account to get the latest token (may have been refreshed above)
                val freshAccount = dao.getAccountByEmail(accountEmail) ?: account
                val activeToken = freshAccount.accessToken
                val url = "https://gmail.googleapis.com/gmail/v1/users/me/messages?maxResults=150"
                var request = Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $activeToken")
                    .build()

                var response = okHttpClient.newCall(request).execute()
                if (response.code == 401) {
                    response.close()
                    Log.w("EmailRepository", "401 Unauthorized syncing emails. Refreshing token...")
                    val backendUrl = pref.renderBackendUrl
                    val refreshed = refreshAccessToken(accountEmail, backendUrl)
                    if (refreshed) {
                        val freshAccount = dao.getAccountByEmail(accountEmail)
                        if (freshAccount != null) {
                            request = Request.Builder()
                                .url(url)
                                .header("Authorization", "Bearer ${freshAccount.accessToken}")
                                .build()
                            response = okHttpClient.newCall(request).execute()
                        }
                    }
                }

                response.use { resp ->
                    if (resp.isSuccessful) {
                        val stringRes = resp.body?.string() ?: ""
                        val messagesArray = JSONObject(stringRes).optJSONArray("messages")
                        if (messagesArray != null) {
                            val activeToken = dao.getAccountByEmail(accountEmail)?.accessToken ?: account.accessToken
                            val semaphore = Semaphore(5)
                            val detailsList = kotlinx.coroutines.coroutineScope {
                                (0 until messagesArray.length()).map { index ->
                                    val msgObj = messagesArray.getJSONObject(index)
                                    val msgId = msgObj.getString("id")
                                    async {
                                        semaphore.withPermit {
                                            val existing = dao.getMessageById(msgId)
                                            // Cache Optimization: skip network details fetch for emails that already
                                            // exist in our local cache, unless they are the top 20 most recent messages.
                                            if (existing == null || index < 20) {
                                                fetchGmailDetails(msgId, activeToken, accountEmail)
                                            } else {
                                                existing
                                            }
                                        }
                                    }
                                }.awaitAll()
                            }
                            detailsList.filterNotNull().forEach { list.add(it) }
                            
                            val currentHistoryId = fetchCurrentHistoryId(activeToken)
                            if (currentHistoryId != null) {
                                pref.saveLastHistoryId(accountEmail, currentHistoryId)
                            }
                        }
                    } else {
                        val errBody = resp.body?.string() ?: ""
                        Log.e("EmailRepository", "Gmail sync list failed: ${resp.code} - $errBody")
                    }
                }
            } catch (e: Exception) {
                Log.e("EmailRepository", "Error fetching real Gmail messages", e)
            }

            dao.deleteMessageById("real_msg_fallback_1")
            list
        }
        
        val mergedMessages = mockMessages.map { msg ->
            val existing = dao.getMessageById(msg.id)
            if (existing != null) {
                val isMock = msg.id.startsWith("msg_") || msg.id == "real_msg_fallback_1" || msg.accountEmail.lowercase().contains("simulated")
                if (isMock) {
                    msg.copy(
                        isRead = existing.isRead,
                        isStarred = existing.isStarred,
                        label = existing.label,
                        category = existing.category,
                        tagsString = existing.tagsString
                    )
                } else {
                    if (!existing.isRead && msg.isRead) {
                        com.example.util.NotificationHelper.cancelNotification(context, msg.id)
                    }
                    msg.copy(
                        tagsString = existing.tagsString
                    )
                }
            } else {
                msg
            }
        }

        val newMessages = mergedMessages.filter { msg ->
            dao.getMessageById(msg.id) == null
        }
        
        dao.insertMessages(mergedMessages)

        newMessages.forEach { msg ->
            if (!msg.isRead && (currentTime - msg.timestamp) < 30 * 60 * 1000) {
                com.example.util.NotificationHelper.showEmailNotification(
                    context,
                    msg.id,
                    msg.senderName,
                    msg.subject,
                    msg.body
                )
            }
        }
    }

    private suspend fun fetchGmailDetails(msgId: String, token: String, accountEmail: String): EmailMessage? = withContext(Dispatchers.IO) {
        val url = "https://gmail.googleapis.com/gmail/v1/users/me/messages/$msgId?format=full"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .build()
        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: return@withContext null
                    val json = JSONObject(bodyStr)
                    val payload = json.getJSONObject("payload")
                    val headers = payload.getJSONArray("headers")
                    
                    var subject = "No Subject"
                    var from = "unknown"
                    var fromName = "Unknown"
                    var dateLong = System.currentTimeMillis()

                    for (i in 0 until headers.length()) {
                        val header = headers.getJSONObject(i)
                        val name = header.getString("name").lowercase()
                        val value = header.getString("value")
                        if (name == "subject") {
                            subject = value
                        } else if (name == "from") {
                            val rawValue = value.trim()
                            if (rawValue.contains("<") && rawValue.contains(">")) {
                                fromName = rawValue.substringBefore("<").trim().replace("\"", "").replace("'", "")
                                from = rawValue.substringAfter("<").substringBefore(">").trim()
                            } else {
                                from = rawValue
                                fromName = rawValue.substringBefore("@").trim().replace("\"", "").replace("'", "")
                            }
                            if (fromName.isEmpty()) {
                                fromName = from
                            }
                        }
                    }

                    val internalDate = json.optLong("internalDate", System.currentTimeMillis())
                    val snippet = json.optString("snippet", "")

                    val plainBody = extractMessageBody(payload).takeIf { it.isNotEmpty() } ?: snippet
                    val htmlBody = extractHtmlBody(payload)

                    val labelIdsArray = json.optJSONArray("labelIds")
                    var isRead = true
                    var isStarred = false
                    var label = "INBOX"
                    var category = "Primary"
                    
                    if (labelIdsArray != null) {
                        val labels = (0 until labelIdsArray.length()).map { labelIdsArray.getString(it) }
                        if (labels.contains("UNREAD")) {
                            isRead = false
                        }
                        if (labels.contains("STARRED")) {
                            isStarred = true
                        }
                        if (labels.contains("SENT")) {
                            label = "SENT"
                        } else if (labels.contains("TRASH")) {
                            label = "TRASH"
                        } else if (labels.contains("DRAFT")) {
                            label = "DRAFT"
                        } else if (labels.contains("SPAM")) {
                            label = "SPAM"
                        } else if (!labels.contains("INBOX")) {
                            label = "ARCHIVE"
                        }

                        // Parse Category
                        if (labels.contains("CATEGORY_UPDATES")) {
                            category = "Updates"
                        } else if (labels.contains("CATEGORY_SOCIAL")) {
                            category = "Social"
                        } else if (labels.contains("CATEGORY_PROMOTIONS")) {
                            category = "Promotions"
                        } else if (labels.contains("CATEGORY_FORUMS")) {
                            category = "Forums"
                        } else if (labels.contains("CATEGORY_PERSONAL")) {
                            category = "Primary"
                        }
                    }

                    // Extract attachments from MIME parts
                    val attachments = extractAttachmentsFromPayload(msgId, payload)
                    val hasAttachments = attachments.isNotEmpty()

                    // Insert attachments into DB
                    if (attachments.isNotEmpty()) {
                        dao.deleteAttachmentsForMessage(msgId)
                        dao.insertAttachments(attachments)
                    }

                    return@withContext EmailMessage(
                        id = msgId,
                        accountEmail = accountEmail,
                        senderName = fromName,
                        sender = from,
                        recipient = accountEmail,
                        subject = subject,
                        body = plainBody,
                        timestamp = internalDate,
                        isRead = isRead,
                        isStarred = isStarred,
                        label = label,
                        category = category,
                        htmlBody = htmlBody,
                        hasAttachments = hasAttachments
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("EmailRepository", "Error fetching details for msg $msgId", e)
        }
        return@withContext null
    }

    private fun extractAttachmentsFromPayload(messageId: String, payload: JSONObject): List<Attachment> {
        val attachments = mutableListOf<Attachment>()
        val parts = payload.optJSONArray("parts") ?: return attachments

        fun processParts(partsArray: org.json.JSONArray) {
            for (i in 0 until partsArray.length()) {
                val part = partsArray.getJSONObject(i)
                val mimeType = part.optString("mimeType", "")
                val filename = part.optString("filename", "")
                val body = part.optJSONObject("body")
                val attachmentId = body?.optString("attachmentId", "") ?: ""
                val size = body?.optLong("size", 0L) ?: 0L

                // A part is an attachment if it has a non-empty filename or is not text/plain or text/html
                if (filename.isNotEmpty() && attachmentId.isNotEmpty()) {
                    attachments.add(
                        Attachment(
                            id = "${messageId}_${attachmentId}",
                            messageId = messageId,
                            fileName = filename,
                            mimeType = mimeType,
                            sizeBytes = size,
                            gmailAttachmentId = attachmentId
                        )
                    )
                }

                // Recurse into nested parts
                val nestedParts = part.optJSONArray("parts")
                if (nestedParts != null) {
                    processParts(nestedParts)
                }
            }
        }

        processParts(parts)
        return attachments
    }

    /**
     * Download an attachment from Gmail API and save to cache.
     * Returns the local file URI or null on failure.
     */
    suspend fun downloadAttachment(
        messageId: String,
        attachmentId: String,
        accountEmail: String,
        fileName: String
    ): Uri? = withContext(Dispatchers.IO) {
        val account = dao.getAccountByEmail(accountEmail) ?: return@withContext null
        val url = "https://gmail.googleapis.com/gmail/v1/users/me/messages/$messageId/attachments/$attachmentId"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${account.accessToken}")
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: return@withContext null
                    val json = JSONObject(bodyStr)
                    val data = json.optString("data", "")
                    if (data.isNotEmpty()) {
                        val decodedBytes = android.util.Base64.decode(
                            data, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP
                        )
                        val cacheDir = context.cacheDir
                        val file = java.io.File(cacheDir, fileName)
                        file.writeBytes(decodedBytes)
                        return@withContext Uri.fromFile(file)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("EmailRepository", "Error downloading attachment", e)
        }
        null
    }

    private fun extractMessageBody(payload: JSONObject): String {
        val mimeType = payload.optString("mimeType", "")
        val bodyObj = payload.optJSONObject("body")
        val directData = bodyObj?.optString("data", "") ?: ""
        if (mimeType.equals("text/plain", ignoreCase = true) && directData.isNotEmpty()) {
            return decodeBase64Url(directData)
        }

        val parts = payload.optJSONArray("parts")
        if (parts != null) {
            return findPlainPart(parts)
        }
        return ""
    }

    private fun findPlainPart(parts: org.json.JSONArray): String {
        for (i in 0 until parts.length()) {
            val part = parts.getJSONObject(i)
            val mimeType = part.optString("mimeType", "")
            val body = part.optJSONObject("body")
            val data = body?.optString("data", "") ?: ""

            if (mimeType.equals("text/plain", ignoreCase = true) && data.isNotEmpty()) {
                return decodeBase64Url(data)
            } else {
                val nestedParts = part.optJSONArray("parts")
                if (nestedParts != null) {
                    val res = findPlainPart(nestedParts)
                    if (res.isNotEmpty()) return res
                }
            }
        }
        return ""
    }

    private fun extractHtmlBody(payload: JSONObject): String? {
        val mimeType = payload.optString("mimeType", "")
        val bodyObj = payload.optJSONObject("body")
        val directData = bodyObj?.optString("data", "") ?: ""
        if (mimeType.equals("text/html", ignoreCase = true) && directData.isNotEmpty()) {
            return decodeBase64Url(directData)
        }

        val parts = payload.optJSONArray("parts")
        if (parts != null) {
            return findHtmlPart(parts)
        }
        return null
    }

    private fun findHtmlPart(parts: org.json.JSONArray): String? {
        for (i in 0 until parts.length()) {
            val part = parts.getJSONObject(i)
            val mimeType = part.optString("mimeType", "")
            val body = part.optJSONObject("body")
            val data = body?.optString("data", "") ?: ""

            if (mimeType.equals("text/html", ignoreCase = true) && data.isNotEmpty()) {
                return decodeBase64Url(data)
            } else {
                val nestedParts = part.optJSONArray("parts")
                if (nestedParts != null) {
                    val res = findHtmlPart(nestedParts)
                    if (res != null) return res
                }
            }
        }
        return null
    }

    private fun decodeBase64Url(base64Str: String): String {
        return try {
            val decodedBytes = android.util.Base64.decode(base64Str, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
            String(decodedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Generate email drafts or automatic replies using Gemini API.
     */
    suspend fun generateWithGemini(
        prompt: String,
        systemInstruction: String,
        userApiKey: String?
    ): String = withContext(Dispatchers.IO) {
        val apiKey = if (!userApiKey.isNullOrBlank()) {
            userApiKey
        } else {
            // Check System Config (it should hold the secret if set in panel)
            com.example.BuildConfig.GEMINI_API_KEY
        }
        generateGeminiResponse(apiKey, prompt, systemInstruction)
    }
    suspend fun generateGeminiResponse(apiKey: String, prompt: String, systemInstruction: String): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Error: Gemini API Key is missing. Please configure it in Settings."
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
        )

        val models = listOf(
            "v1beta/models/gemini-2.5-flash:generateContent",
            "v1beta/models/gemini-2.0-flash:generateContent",
            "v1beta/models/gemini-1.5-flash:generateContent",
            "v1beta/models/gemini-1.5-pro:generateContent"
        )

        var lastError: Exception? = null
        for (modelUrl in models) {
            try {
                Log.d("EmailRepository", "Attempting content generation with model: $modelUrl")
                val response = GeminiRetrofitClient.service.generateContent(modelUrl, apiKey, request)
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!text.isNullOrEmpty()) {
                    Log.d("EmailRepository", "Content successfully generated using model: $modelUrl")
                    return@withContext text
                }
            } catch (e: Exception) {
                Log.w("EmailRepository", "Gemini content generation failed with model $modelUrl: ${e.message}")
                lastError = e
            }
        }

        val errorMsg = lastError?.localizedMessage ?: "Connection Timeout"
        Log.e("EmailRepository", "All Gemini models in fallback chain failed.", lastError)
        "AI Assistant Generation Error: $errorMsg"
    }

    suspend fun sendEmail(
        fromEmail: String,
        toEmail: String,
        subject: String,
        body: String,
        threadId: String? = null,
        attachmentUris: List<Uri> = emptyList()
    ): Boolean = withContext(Dispatchers.IO) {
        val account = dao.getAccountByEmail(fromEmail)
        
        // If it's a mock/simulated account (or no account exists), we treat it as successful mock send.
        if (account == null || account.email.lowercase().contains("simulated")) {
            Log.d("EmailRepository", "Mock email sent locally for simulated account: $fromEmail")
            return@withContext true
        }

        // Real account: Refresh token if expiring
        val pref = preferences
        val backendUrl = pref.renderBackendUrl
        if (account.refreshToken.isNotEmpty() && account.expiresAt < System.currentTimeMillis() + 5 * 60 * 1000) {
            refreshAccessToken(fromEmail, backendUrl)
        }

        val updatedAccount = dao.getAccountByEmail(fromEmail) ?: return@withContext false

        // Build MIME message
        val mimeMessage: String
        if (attachmentUris.isEmpty()) {
            // Simple text-only MIME message
            mimeMessage = "From: $fromEmail\r\n" +
                    "To: $toEmail\r\n" +
                    "Subject: $subject\r\n" +
                    "Content-Type: text/plain; charset=UTF-8\r\n\r\n" +
                    body
        } else {
            // Multipart MIME message with attachments
            val boundary = "----=_Part_${UUID.randomUUID().toString().replace("-", "")}"
            val sb = StringBuilder()
            sb.append("From: $fromEmail\r\n")
            sb.append("To: $toEmail\r\n")
            sb.append("Subject: $subject\r\n")
            sb.append("MIME-Version: 1.0\r\n")
            sb.append("Content-Type: multipart/mixed; boundary=\"$boundary\"\r\n\r\n")

            // Text body part
            sb.append("--$boundary\r\n")
            sb.append("Content-Type: text/plain; charset=UTF-8\r\n")
            sb.append("Content-Transfer-Encoding: 7bit\r\n\r\n")
            sb.append(body)
            sb.append("\r\n")

            // Attachment parts
            for (uri in attachmentUris) {
                try {
                    val (fileName, fileBytes, mimeType) = readAttachmentFromUri(uri)
                    if (fileName != null && fileBytes != null) {
                        val base64Data = android.util.Base64.encodeToString(
                            fileBytes,
                            android.util.Base64.DEFAULT or android.util.Base64.NO_WRAP
                        )
                        val encodedFileName = java.net.URLEncoder.encode(fileName, "UTF-8")
                        sb.append("--$boundary\r\n")
                        sb.append("Content-Type: ${mimeType ?: "application/octet-stream"}; name=\"$fileName\"\r\n")
                        sb.append("Content-Transfer-Encoding: base64\r\n")
                        sb.append("Content-Disposition: attachment; filename=\"$encodedFileName\"\r\n\r\n")
                        sb.append(base64Data)
                        sb.append("\r\n")
                    }
                } catch (e: Exception) {
                    Log.e("EmailRepository", "Error reading attachment $uri", e)
                }
            }

            sb.append("--$boundary--\r\n")
            mimeMessage = sb.toString()
        }

        val base64Raw = android.util.Base64.encodeToString(
            mimeMessage.toByteArray(Charsets.UTF_8),
            android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING
        ).trim()

        val jsonBody = JSONObject().apply {
            put("raw", base64Raw)
            if (!threadId.isNullOrEmpty()) {
                put("threadId", threadId)
            }
        }.toString()

        val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
        var request = Request.Builder()
            .url("https://gmail.googleapis.com/gmail/v1/users/me/messages/send")
            .header("Authorization", "Bearer ${updatedAccount.accessToken}")
            .post(requestBody)
            .build()

        try {
            var response = okHttpClient.newCall(request).execute()
            if (response.code == 401) {
                response.close()
                Log.w("EmailRepository", "401 Unauthorized sending email. Refreshing token...")
                val refreshed = refreshAccessToken(fromEmail, backendUrl)
                if (refreshed) {
                    val freshAccount = dao.getAccountByEmail(fromEmail)
                    if (freshAccount != null) {
                        request = Request.Builder()
                            .url("https://gmail.googleapis.com/gmail/v1/users/me/messages/send")
                            .header("Authorization", "Bearer ${freshAccount.accessToken}")
                            .post(requestBody)
                            .build()
                        response = okHttpClient.newCall(request).execute()
                    }
                }
            }

            response.use { resp ->
                if (resp.isSuccessful) {
                    Log.d("EmailRepository", "Gmail sent successfully via OAuth API.")
                    true
                } else {
                    val errBody = resp.body?.string() ?: ""
                    Log.e("EmailRepository", "Gmail send failed: ${resp.code} - $errBody")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e("EmailRepository", "Exception in sendEmail", e)
            false
        }
    }

    private fun readAttachmentFromUri(uri: Uri): Triple<String?, ByteArray?, String?> {
        return try {
            val resolver = context.contentResolver
            var fileName: String? = null
            var mimeType: String? = resolver.getType(uri)

            // Try to get filename from ContentResolver
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) fileName = cursor.getString(nameIndex)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex >= 0) {
                        // Just for logging
                    }
                }
            }

            if (fileName == null) {
                fileName = "attachment_${System.currentTimeMillis()}"
            }

            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            Triple(fileName, bytes, mimeType)
        } catch (e: Exception) {
            Log.e("EmailRepository", "Error reading attachment from URI: $uri", e)
            Triple(null, null, null)
        }
    }

    /**
     * Modify labels of a message in Gmail API to sync read/unread, starred/unstarred, or deletion status.
     */
    suspend fun modifyGmailLabels(
        accountEmail: String,
        msgId: String,
        addLabels: List<String>,
        removeLabels: List<String>
    ): Boolean = withContext(Dispatchers.IO) {
        val account = dao.getAccountByEmail(accountEmail) ?: return@withContext false
        if (account.email.lowercase().contains("simulated")) {
            return@withContext true
        }

        val pref = preferences
        val backendUrl = pref.renderBackendUrl
        if (account.refreshToken.isNotEmpty() && account.expiresAt < System.currentTimeMillis() + 5 * 60 * 1000) {
            refreshAccessToken(accountEmail, backendUrl)
        }

        val updatedAccount = dao.getAccountByEmail(accountEmail) ?: return@withContext false

        val json = JSONObject().apply {
            put("addLabelIds", org.json.JSONArray(addLabels))
            put("removeLabelIds", org.json.JSONArray(removeLabels))
        }

        val requestBody = json.toString().toRequestBody("application/json".toMediaType())
        val url = "https://gmail.googleapis.com/gmail/v1/users/me/messages/$msgId/modify"
        var request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${updatedAccount.accessToken}")
            .post(requestBody)
            .build()

        try {
            var response = okHttpClient.newCall(request).execute()
            if (response.code == 401) {
                response.close()
                val refreshed = refreshAccessToken(accountEmail, backendUrl)
                if (refreshed) {
                    val freshAccount = dao.getAccountByEmail(accountEmail)
                    if (freshAccount != null) {
                        request = Request.Builder()
                            .url(url)
                            .header("Authorization", "Bearer ${freshAccount.accessToken}")
                            .post(requestBody)
                            .build()
                        response = okHttpClient.newCall(request).execute()
                    }
                }
            }
            response.use { resp ->
                resp.isSuccessful
            }
        } catch (e: Exception) {
            Log.e("EmailRepository", "Error modifying labels for $msgId", e)
            false
        }
    }

    /**
     * Permanently delete a message from Gmail.
     */
    suspend fun permanentlyDeleteGmailMessage(accountEmail: String, msgId: String): Boolean = withContext(Dispatchers.IO) {
        val account = dao.getAccountByEmail(accountEmail) ?: return@withContext false
        if (account.email.lowercase().contains("simulated")) {
            return@withContext true
        }

        val pref = preferences
        val backendUrl = pref.renderBackendUrl
        if (account.refreshToken.isNotEmpty() && account.expiresAt < System.currentTimeMillis() + 5 * 60 * 1000) {
            refreshAccessToken(accountEmail, backendUrl)
        }

        val updatedAccount = dao.getAccountByEmail(accountEmail) ?: return@withContext false
        val url = "https://gmail.googleapis.com/gmail/v1/users/me/messages/$msgId"
        var request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${updatedAccount.accessToken}")
            .delete()
            .build()

        try {
            var response = okHttpClient.newCall(request).execute()
            if (response.code == 401) {
                response.close()
                val refreshed = refreshAccessToken(accountEmail, backendUrl)
                if (refreshed) {
                    val freshAccount = dao.getAccountByEmail(accountEmail)
                    if (freshAccount != null) {
                        request = Request.Builder()
                            .url(url)
                            .header("Authorization", "Bearer ${freshAccount.accessToken}")
                            .delete()
                            .build()
                        response = okHttpClient.newCall(request).execute()
                    }
                }
            }
            response.use { resp ->
                resp.isSuccessful
            }
        } catch (e: Exception) {
            Log.e("EmailRepository", "Error permanently deleting $msgId", e)
            false
        }
    }

    private suspend fun fetchCurrentHistoryId(accessToken: String): String? {
        val url = "https://gmail.googleapis.com/gmail/v1/users/me/profile"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .build()
        try {
            okHttpClient.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: ""
                    return JSONObject(body).optString("historyId").takeIf { it.isNotEmpty() }
                }
            }
        } catch (e: Exception) {
            Log.e("EmailRepository", "Error fetching mailbox profile historyId", e)
        }
        return null
    }

    suspend fun syncEmailsHistory(
        accountEmail: String,
        accessToken: String,
        startHistoryId: String
    ): HistorySyncResult = withContext(Dispatchers.IO) {
        val url = "https://gmail.googleapis.com/gmail/v1/users/me/history?startHistoryId=$startHistoryId"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .build()

        try {
            okHttpClient.newCall(request).execute().use { resp ->
                if (resp.code == 404 || resp.code == 400) {
                    return@withContext HistorySyncResult.Expired
                }
                if (!resp.isSuccessful) {
                    return@withContext HistorySyncResult.Error("API error: ${resp.code}")
                }
                val stringRes = resp.body?.string() ?: ""
                val root = JSONObject(stringRes)
                val newHistoryId = root.optString("historyId")
                
                val historyArray = root.optJSONArray("history")
                var newEmailsFound = false
                if (historyArray != null) {
                    for (i in 0 until historyArray.length()) {
                        val histObj = historyArray.getJSONObject(i)
                        
                        val added = histObj.optJSONArray("messagesAdded")
                        if (added != null) {
                            for (j in 0 until added.length()) {
                                val msgItem = added.getJSONObject(j).optJSONObject("message")
                                if (msgItem != null) {
                                    val msgId = msgItem.getString("id")
                                    val fullMsg = fetchGmailDetails(msgId, accessToken, accountEmail)
                                    if (fullMsg != null) {
                                        dao.insertMessage(fullMsg)
                                        newEmailsFound = true
                                        if (!fullMsg.isRead && fullMsg.label == "INBOX") {
                                            com.example.util.NotificationHelper.showEmailNotification(
                                                context,
                                                fullMsg.id,
                                                fullMsg.senderName,
                                                fullMsg.subject,
                                                fullMsg.body.take(80)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        val deleted = histObj.optJSONArray("messagesDeleted")
                        if (deleted != null) {
                            for (j in 0 until deleted.length()) {
                                val msgItem = deleted.getJSONObject(j).optJSONObject("message")
                                if (msgItem != null) {
                                    val msgId = msgItem.getString("id")
                                    dao.deleteMessageById(msgId)
                                    com.example.util.NotificationHelper.cancelNotification(context, msgId)
                                }
                            }
                        }

                        val labelsAdded = histObj.optJSONArray("labelsAdded")
                        if (labelsAdded != null) {
                            for (j in 0 until labelsAdded.length()) {
                                val item = labelsAdded.getJSONObject(j)
                                val msgItem = item.optJSONObject("message")
                                val labelIds = item.optJSONArray("labelIds")
                                if (msgItem != null && labelIds != null) {
                                    val msgId = msgItem.getString("id")
                                    val existing = dao.getMessageById(msgId)
                                    if (existing != null) {
                                        var updated: EmailMessage = existing
                                        for (k in 0 until labelIds.length()) {
                                            val label = labelIds.getString(k)
                                            if (label == "UNREAD") {
                                                updated = updated.copy(isRead = false)
                                            } else if (label == "STARRED") {
                                                updated = updated.copy(isStarred = true)
                                            } else if (label == "TRASH") {
                                                updated = updated.copy(label = "TRASH")
                                            } else if (label == "INBOX") {
                                                updated = updated.copy(label = "INBOX")
                                            } else if (label == "SPAM") {
                                                updated = updated.copy(label = "SPAM")
                                            }
                                        }
                                        dao.updateMessage(updated)
                                    }
                                }
                            }
                        }

                        val labelsRemoved = histObj.optJSONArray("labelsRemoved")
                        if (labelsRemoved != null) {
                            for (j in 0 until labelsRemoved.length()) {
                                val item = labelsRemoved.getJSONObject(j)
                                val msgItem = item.optJSONObject("message")
                                val labelIds = item.optJSONArray("labelIds")
                                if (msgItem != null && labelIds != null) {
                                    val msgId = msgItem.getString("id")
                                    val existing = dao.getMessageById(msgId)
                                    if (existing != null) {
                                        var updated: EmailMessage = existing
                                        for (k in 0 until labelIds.length()) {
                                            val label = labelIds.getString(k)
                                            if (label == "UNREAD") {
                                                updated = updated.copy(isRead = true)
                                                com.example.util.NotificationHelper.cancelNotification(context, msgId)
                                            } else if (label == "STARRED") {
                                                updated = updated.copy(isStarred = false)
                                            } else if (label == "INBOX") {
                                                updated = updated.copy(label = "ARCHIVE")
                                            }
                                        }
                                        dao.updateMessage(updated)
                                    }
                                }
                            }
                        }
                    }
                }

                if (newHistoryId.isNotEmpty()) {
                    preferences.saveLastHistoryId(accountEmail, newHistoryId)
                }

                if (newEmailsFound || (historyArray != null && historyArray.length() > 0)) {
                    Log.d("EmailRepository", "History sync processed ${historyArray?.length() ?: 0} events")
                    HistorySyncResult.Success
                } else {
                    Log.d("EmailRepository", "History sync returned no changes, falling back to full sync")
                    HistorySyncResult.Error("No new changes in history")
                }
            }
        } catch (e: Exception) {
            Log.e("EmailRepository", "Error running Gmail History API sync", e)
            HistorySyncResult.Error(e.message ?: "Unknown error")
        }
    }
}

sealed class HistorySyncResult {
    object Success : HistorySyncResult()
    object Expired : HistorySyncResult()
    data class Error(val message: String) : HistorySyncResult()
}
