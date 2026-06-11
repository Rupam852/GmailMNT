package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

class EmailViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = EmailRepository(application)
    private val preferences = PreferenceManager(application)

    // Local States loaded from Preferences
    val isDarkMode = MutableStateFlow(preferences.isDarkMode)
    val isBiometricEnabled = MutableStateFlow(preferences.isBiometricEnabled)
    val isNotificationsEnabled = MutableStateFlow(preferences.isNotificationsEnabled)
    val geminiApiKey = MutableStateFlow(preferences.geminiApiKey)
    val renderBackendUrl = MutableStateFlow(preferences.renderBackendUrl)
    val isGetStartedCompleted = MutableStateFlow(preferences.isGetStartedCompleted)

    // Search and Filters
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("All") // All, Primary, Updates, Social, Promotions
    val selectedAccount = MutableStateFlow<String?>("All") // "All" or a specific email
    val selectedSortOrder = MutableStateFlow("Newest") // Newest, Oldest, Starred
    val selectedTag = MutableStateFlow<String?>("All") // "All" or a custom tag
    val customTags = MutableStateFlow<Set<String>>(preferences.customTags)
    val selectedFolder = MutableStateFlow("INBOX") // INBOX, SENT, DRAFT, TRASH
    val activeEditingDraftId = MutableStateFlow<String?>(null)
    val isAppInForeground = MutableStateFlow(true)
    val selectedTab = MutableStateFlow(0) // 0: Inbox, 1: Compose, 2: Settings

    // Track which notification IDs have already been cancelled to avoid repeated calls
    private val cancelledNotificationIds = mutableSetOf<String>()

    init {
        // Initial sync on start only; periodic sync is handled by WorkManager (every 15 min)
        viewModelScope.launch {
            kotlinx.coroutines.delay(1500)
            triggerSyncAll()
        }

        // Cancel notifications only for newly-read emails (dedup to avoid repeated system calls)
        viewModelScope.launch(Dispatchers.Default) {
            filteredMessages.collect { messages ->
                messages.filter { it.isRead }.forEach { msg ->
                    if (!cancelledNotificationIds.contains(msg.id)) {
                        cancelledNotificationIds.add(msg.id)
                        com.example.util.NotificationHelper.cancelNotification(getApplication(), msg.id)
                    }
                }
            }
        }
    }

    // Accounts & Filtering Stream combines
    // Multi‑select state for bulk actions
    val selectedMailIds = MutableStateFlow<Set<String>>(emptySet())

    fun toggleMailSelection(id: String) {
        val current = selectedMailIds.value.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        selectedMailIds.value = current
    }

    fun clearMailSelection() {
        selectedMailIds.value = emptySet()
    }
    val accounts: StateFlow<List<EmailAccount>> = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMessages: Flow<List<EmailMessage>> = repository.allMessages

    private data class FilterState(
        val query: String,
        val category: String,
        val accountEmail: String?,
        val sortOrder: String
    )

    private val filterStateFlow = combine(
        searchQuery,
        selectedCategory,
        selectedAccount,
        selectedSortOrder
    ) { query, category, account, sort ->
        FilterState(query, category, account, sort)
    }

    // Direct reactive messaging stream combining filters, sorting & searches!
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val filteredMessages: StateFlow<List<EmailMessage>> = combine(
        filterStateFlow,
        selectedTag,
        selectedFolder,
        repository.allMessages
    ) { filter, tag, folder, allMails ->
        var list = allMails

        // 0. Filter by Folder
        list = list.filter { it.label.uppercase() == folder.uppercase() }

        // 1. Filter by Account
        if (filter.accountEmail != null && filter.accountEmail != "All") {
            list = list.filter { it.accountEmail == filter.accountEmail }
        }

        // 2. Filter by Category / Default Inbox exclusion
        if (folder.uppercase() == "INBOX") {
            if (filter.category != "All") {
                list = list.filter { it.category.equals(filter.category, ignoreCase = true) }
            } else {
                // Show only Primary, Updates, Social, Forums, Promotions in All Inbox feed.
                list = list.filter {
                    val cat = it.category.uppercase()
                    cat == "PRIMARY" || cat == "UPDATES" || cat == "SOCIAL" || cat == "FORUMS" || cat == "PROMOTIONS"
                }
            }
        }

        // 3. Filter by Custom Tag
        if (tag != null && tag != "All") {
            list = list.filter { mail ->
                val tagsList = mail.tagsString.split(",")
                    .map { t -> t.trim() }
                    .filter { t -> t.isNotEmpty() }
                tagsList.contains(tag)
            }
        }

        // 4. Search query
        if (filter.query.isNotBlank()) {
            list = list.filter {
                it.subject.contains(filter.query, ignoreCase = true) ||
                it.senderName.contains(filter.query, ignoreCase = true) ||
                it.body.contains(filter.query, ignoreCase = true) ||
                it.tagsString.contains(filter.query, ignoreCase = true)
            }
        }

        // 5. Sort
        when (filter.sortOrder) {
            "Newest" -> list.sortedByDescending { it.timestamp }
            "Oldest" -> list.sortedBy { it.timestamp }
            "Starred" -> list.filter { it.isStarred }.sortedByDescending { it.timestamp }
            else -> list.sortedByDescending { it.timestamp }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addCustomTag(tag: String) {
        val updated = customTags.value.toMutableSet()
        if (updated.add(tag)) {
            preferences.customTags = updated
            customTags.value = updated
        }
    }

    fun removeCustomTag(tag: String) {
        val updated = customTags.value.toMutableSet()
        if (updated.remove(tag)) {
            preferences.customTags = updated
            customTags.value = updated
            if (selectedTag.value == tag) {
                selectedTag.value = "All"
            }
            // Clean up deleted tag from all messages asynchronously
            viewModelScope.launch {
                val allMails = repository.allMessages.first()
                allMails.forEach { email ->
                    val tags = email.tagsString.split(",").map { it.trim() }.filter { it.isNotEmpty() && it != tag }
                    if (tags.size != email.tagsString.split(",").filter { it.isNotEmpty() }.size) {
                        val updatedString = tags.joinToString(",")
                        repository.updateMessage(email.copy(tagsString = updatedString))
                    }
                }
            }
        }
    }

    fun toggleMessageTag(messageId: String, tag: String) {
        viewModelScope.launch {
            val allMails = repository.allMessages.first()
            val email = allMails.find { it.id == messageId } ?: return@launch
            val currentTags = email.tagsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
            if (currentTags.contains(tag)) {
                currentTags.remove(tag)
            } else {
                currentTags.add(tag)
            }
            val updatedString = currentTags.joinToString(",")
            repository.updateMessage(email.copy(tagsString = updatedString))
        }
    }

    // Detail Mail State
    private val _selectedMailId = MutableStateFlow<String?>(null)
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedMail: StateFlow<EmailMessage?> = _selectedMailId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else repository.allMessages.map { list -> list.find { it.id == id } }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // State indicators
    val isLoading = MutableStateFlow(false)
    val isRefreshing = MutableStateFlow(false)
    val geminiResponse = MutableStateFlow("")
    val isGeneratingDraft = MutableStateFlow(false)

    private val syncMutex = Mutex()
    private var isManualSyncActive = false
    private var isBackgroundSyncActive = false

    init {
        // No default simulated account is created to ensure a clean first-time user experience.
    }

    // Toggle and setting operations
    fun setDarkMode(enabled: Boolean) {
        preferences.isDarkMode = enabled
        isDarkMode.value = enabled
    }

    fun setBiometric(enabled: Boolean) {
        preferences.isBiometricEnabled = enabled
        isBiometricEnabled.value = enabled
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        preferences.isNotificationsEnabled = enabled
        isNotificationsEnabled.value = enabled
    }

    fun setGeminiApiKey(key: String) {
        preferences.geminiApiKey = key
        geminiApiKey.value = key
    }

    fun setRenderBackendUrl(url: String) {
        preferences.renderBackendUrl = url
        renderBackendUrl.value = url
    }

    fun completeGetStarted() {
        preferences.isGetStartedCompleted = true
        isGetStartedCompleted.value = true
    }

    fun selectMailId(id: String?) {
        _selectedMailId.value = id
    }

    // Operations on email
    fun toggleStarred(id: String, currentStarred: Boolean) {
        viewModelScope.launch {
            val newStarred = !currentStarred
            repository.updateMessageStarredStatus(id, newStarred)
            val msg = repository.getMessageById(id)
            if (msg != null && !msg.accountEmail.lowercase().contains("simulated")) {
                val addLabels = if (newStarred) listOf("STARRED") else emptyList()
                val removeLabels = if (newStarred) emptyList() else listOf("STARRED")
                repository.modifyGmailLabels(msg.accountEmail, msg.id, addLabels, removeLabels)
            }
        }
    }

    fun markAsRead(id: String, read: Boolean = true) {
        viewModelScope.launch {
            repository.updateMessageReadStatus(id, read)
            if (read) {
                com.example.util.NotificationHelper.cancelNotification(getApplication(), id)
            }
            val msg = repository.getMessageById(id)
            if (msg != null && !msg.accountEmail.lowercase().contains("simulated")) {
                val addLabels = if (read) emptyList() else listOf("UNREAD")
                val removeLabels = if (read) listOf("UNREAD") else emptyList()
                repository.modifyGmailLabels(msg.accountEmail, msg.id, addLabels, removeLabels)
            }
        }
    }

    fun markAllAsRead(read: Boolean) {
        viewModelScope.launch {
            repository.markAllMessagesReadStatus(read)
            val currentMessages = filteredMessages.value
            currentMessages.forEach { msg ->
                if (read) {
                    com.example.util.NotificationHelper.cancelNotification(getApplication(), msg.id)
                }
                if (msg.isRead != read && !msg.accountEmail.lowercase().contains("simulated")) {
                    launch {
                        val addLabels = if (read) emptyList() else listOf("UNREAD")
                        val removeLabels = if (read) listOf("UNREAD") else emptyList()
                        repository.modifyGmailLabels(msg.accountEmail, msg.id, addLabels, removeLabels)
                    }
                }
            }
        }
    }

    fun deleteMail(id: String) {
        viewModelScope.launch {
            com.example.util.NotificationHelper.cancelNotification(getApplication(), id)
            val email = repository.getMessageById(id)
            if (email != null) {
                if (email.label.uppercase() == "TRASH") {
                    repository.deleteMessage(id) // Permanently delete
                    if (!email.accountEmail.lowercase().contains("simulated")) {
                        repository.permanentlyDeleteGmailMessage(email.accountEmail, email.id)
                    }
                } else {
                    repository.updateMessageLabel(id, "TRASH") // Move to Trash
                    if (!email.accountEmail.lowercase().contains("simulated")) {
                        repository.modifyGmailLabels(email.accountEmail, email.id, listOf("TRASH"), listOf("INBOX"))
                    }
                }
            }
        }
    }

    fun restoreMailFromTrash(id: String) {
        viewModelScope.launch {
            repository.updateMessageLabel(id, "INBOX")
            val email = repository.getMessageById(id)
            if (email != null && !email.accountEmail.lowercase().contains("simulated")) {
                repository.modifyGmailLabels(email.accountEmail, email.id, listOf("INBOX"), listOf("TRASH"))
            }
        }
    }

    fun archiveMail(id: String) {
        viewModelScope.launch {
            com.example.util.NotificationHelper.cancelNotification(getApplication(), id)
            val email = repository.getMessageById(id)
            if (email != null) {
                repository.updateMessageLabel(id, "ARCHIVE")
                if (!email.accountEmail.lowercase().contains("simulated")) {
                    repository.modifyGmailLabels(email.accountEmail, email.id, emptyList(), listOf("INBOX"))
                }
            }
        }
    }

    fun unarchiveMail(id: String) {
        viewModelScope.launch {
            val email = repository.getMessageById(id)
            if (email != null) {
                repository.updateMessageLabel(id, "INBOX")
                if (!email.accountEmail.lowercase().contains("simulated")) {
                    repository.modifyGmailLabels(email.accountEmail, email.id, listOf("INBOX"), emptyList())
                }
            }
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            val trashMessages = repository.allMessages.first().filter { it.label.uppercase() == "TRASH" }
            repository.emptyTrash()
            trashMessages.forEach { msg ->
                if (!msg.accountEmail.lowercase().contains("simulated")) {
                    launch {
                        repository.permanentlyDeleteGmailMessage(msg.accountEmail, msg.id)
                    }
                }
            }
        }
    }

    fun restoreAllTrash() {
        viewModelScope.launch {
            val trashMessages = repository.allMessages.first().filter { it.label.uppercase() == "TRASH" }
            trashMessages.forEach { msg ->
                repository.updateMessageLabel(msg.id, "INBOX")
                if (!msg.accountEmail.lowercase().contains("simulated")) {
                    launch {
                        repository.modifyGmailLabels(msg.accountEmail, msg.id, listOf("INBOX"), listOf("TRASH"))
                    }
                }
            }
        }
    }

    fun saveDraftToDb(fromEmail: String, recipient: String, subject: String, body: String, category: String) {
        viewModelScope.launch {
            if (fromEmail.isBlank() && recipient.isBlank() && subject.isBlank() && body.isBlank()) {
                return@launch
            }
            val draftId = activeEditingDraftId.value ?: "draft_${UUID.randomUUID()}"
            if (activeEditingDraftId.value == null) {
                activeEditingDraftId.value = draftId
            }
            val draftMsg = EmailMessage(
                id = draftId,
                accountEmail = fromEmail,
                senderName = "Me",
                sender = fromEmail,
                recipient = recipient,
                subject = subject,
                body = body,
                timestamp = System.currentTimeMillis(),
                isRead = true,
                isStarred = false,
                label = "DRAFT",
                category = category
            )
            repository.insertMessage(draftMsg)
        }
    }

    fun restoreMail(message: EmailMessage) {
        viewModelScope.launch {
            repository.insertMessage(message)
        }
    }

    fun deleteAccount(email: String) {
        viewModelScope.launch {
            repository.deleteAccount(email)
        }
    }

    fun composeEmail(
        fromEmail: String,
        toEmail: String,
        subject: String,
        body: String,
        category: String = "Primary",
        threadId: String? = null
    ) {
        viewModelScope.launch {
            val hasNetwork = isNetworkAvailable()
            var success = false
            if (hasNetwork) {
                success = repository.sendEmail(fromEmail, toEmail, subject, body, threadId)
            }
            
            if (success) {
                val draftId = activeEditingDraftId.value
                if (draftId != null) {
                    repository.deleteMessage(draftId) // remove draft from db
                    activeEditingDraftId.value = null
                }
                val sentMsg = EmailMessage(
                    id = "composed_${UUID.randomUUID()}",
                    accountEmail = fromEmail,
                    senderName = "Me",
                    sender = fromEmail,
                    recipient = toEmail,
                    subject = subject,
                    body = body,
                    timestamp = System.currentTimeMillis(),
                    isRead = true,
                    isStarred = false,
                    label = "SENT",
                    category = category
                )
                repository.insertMessage(sentMsg)
                triggerSyncAll() // Auto-fetch new emails immediately after sending!
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        getApplication(),
                        "Email sent successfully!",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                // Save to local outbox database
                try {
                    val database = EmailDatabase.getDatabase(getApplication())
                    val dao = database.emailDao()
                    dao.insertOutboxMessage(
                        OutboxMessage(
                            fromEmail = fromEmail,
                            toEmail = toEmail,
                            subject = subject,
                            body = body,
                            threadId = threadId
                        )
                    )

                    // Schedule Outbox Worker using WorkManager
                    scheduleOfflineEmailSync()

                    // Delete draft from DB if it was a draft so it doesn't stay in draft list
                    val draftId = activeEditingDraftId.value
                    if (draftId != null) {
                        repository.deleteMessage(draftId)
                        activeEditingDraftId.value = null
                    }

                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            getApplication(),
                            "Offline: Email will be sent when connection is restored.",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    Log.e("EmailViewModel", "Error saving offline email", e)
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            getApplication(),
                            "Failed to send email. Check connection or credentials.",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getApplication<Application>()
            .getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun scheduleOfflineEmailSync() {
        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()

        val workRequest = androidx.work.OneTimeWorkRequestBuilder<com.example.util.SendEmailWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                androidx.work.BackoffPolicy.EXPONENTIAL,
                androidx.work.WorkRequest.MIN_BACKOFF_MILLIS,
                java.util.concurrent.TimeUnit.MILLISECONDS
            )
            .build()

        androidx.work.WorkManager.getInstance(getApplication()).enqueueUniqueWork(
            "SendOfflineEmails",
            androidx.work.ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }


    // Backend deep linking handle
    fun handleOAuthSuccess(
        email: String,
        accessToken: String,
        refreshToken: String?,
        expiresAt: Long,
        displayName: String? = null,
        profilePictureUrl: String? = null
    ) {
        viewModelScope.launch {
            val existing = repository.getAccountByEmail(email)
            val updated = EmailAccount(
                email = email,
                displayName = displayName ?: existing?.displayName ?: email.substringBefore("@").replaceFirstChar { it.uppercase() },
                provider = "Gmail",
                accessToken = accessToken,
                refreshToken = refreshToken ?: existing?.refreshToken ?: "",
                expiresAt = expiresAt,
                profilePictureUrl = profilePictureUrl ?: existing?.profilePictureUrl ?: ""
            )
            repository.addAccount(updated)
            // Synchronize of newly configured account completed inside addAccount
        }
    }

    // Manual or scheduled trigger syncing
    fun triggerSyncAll(isManual: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            syncMutex.withLock {
                if (isManualSyncActive || isBackgroundSyncActive) {
                    // Already syncing, skip concurrent execution to prevent DB locks
                    return@launch
                }

                if (isManual) {
                    isManualSyncActive = true
                    isRefreshing.value = true
                } else {
                    isBackgroundSyncActive = true
                    // Background/periodic sync: only show isLoading if we don't have messages yet
                    try {
                        val currentMessages = repository.allMessages.first()
                        if (currentMessages.isEmpty()) {
                            isLoading.value = true
                        }
                    } catch (_: Exception) { }
                }

                try {
                    // If the access token is expiring soon, trigger background Render renew
                    val activeAccounts = repository.allAccounts.first()
                    for (account in activeAccounts) {
                        if (account.refreshToken.isNotEmpty() && account.expiresAt < System.currentTimeMillis() + 5 * 60 * 1000) {
                            repository.refreshAccessToken(account.email, renderBackendUrl.value)
                        }
                        repository.syncEmailsForAccount(account.email)
                    }
                } catch (e: Exception) {
                    Log.e("ViewModel", "Sync Error", e)
                } finally {
                    if (isManual) {
                        isManualSyncActive = false
                    } else {
                        isBackgroundSyncActive = false
                    }

                    if (!isManualSyncActive) {
                        isRefreshing.value = false
                    }
                    isLoading.value = false
                }
            }
        }
    }

    // Push notification simulator
    fun simulateIncomingEmail() {
        viewModelScope.launch {
            val senders = listOf(
                Pair("Warren (Groww)", "warren@groww.co"),
                Pair("Binance Support", "noreply@binance.com"),
                Pair("Google Account Security", "no-reply@accounts.google.com")
            )
            val selectedSender = senders.random()
            val subjectStr = listOf(
                "Exclusive Mutual Fund update: top trending SIP assets",
                "New device logged in near Bangalore, India",
                "Important: Please verify your annual account statements"
            ).random()
            val snippet = "Greetings, we detected actions on your account dashboard. Ensure you have reviewed historical entries on your profile settings. Complete documentation lookup if required."

            val targetAccount = selectedAccount.value.takeIf { it != "All" } 
                ?: accounts.value.firstOrNull()?.email 
                ?: return@launch

            val incomingMessage = EmailMessage(
                id = "incoming_${UUID.randomUUID()}",
                accountEmail = targetAccount,
                senderName = selectedSender.first,
                sender = selectedSender.second,
                recipient = targetAccount,
                subject = subjectStr,
                body = snippet,
                timestamp = System.currentTimeMillis(),
                isRead = false,
                isStarred = false,
                category = listOf("Primary", "Updates", "Promotions", "Social").random()
            )

            repository.insertMessage(incomingMessage)
            
            // Deliver actual Android operating system notification
            NotificationHelper.showEmailNotification(
                getApplication(),
                incomingMessage.id,
                incomingMessage.senderName,
                incomingMessage.subject,
                incomingMessage.body
            )
        }
    }

    // Call Gemini to help draft an email or reply
    fun generateEmailDraft(
        recipientContext: String,
        subjectContext: String,
        additionalRequirements: String,
        isReplyMode: Boolean = false,
        originalEmailBody: String = ""
    ) {
        viewModelScope.launch {
            isGeneratingDraft.value = true
            geminiResponse.value = ""
            
            val systemInstruction = """
                You are GmailMNT, an highly smart AI inbox co-pilot embedded inside a gorgeous, modern Material 3 email client.
                Generate high-quality, professional, and contextually rich email drafts or replies.
                Ensure the tone matches perfectly (polite, business-grade, and friendly).
                Output only the composed body text of the email. Do not output subject titles, preamble, or signatures unless requested.
            """.trimIndent()

            val prompt = if (isReplyMode) {
                """
                    Compose an email reply to:
                    Sender: $recipientContext
                    Subject: $subjectContext
                    Original Message: "$originalEmailBody"
                    My reply thoughts: "$additionalRequirements"
                """.trimIndent()
            } else {
                """
                    Compose a new email from scratch:
                    To: $recipientContext
                    Subject: $subjectContext
                    Focus points: "$additionalRequirements"
                """.trimIndent()
            }

            try {
                val result = repository.generateWithGemini(prompt, systemInstruction, geminiApiKey.value)
                geminiResponse.value = result
            } catch (e: Exception) {
                geminiResponse.value = "AI Generation error: ${e.message}"
            } finally {
                isGeneratingDraft.value = false
            }
        }
    }

    // Draft Autosave support helpers
    fun getDraftRecipient(): String = preferences.draftRecipient
    fun getDraftSubject(): String = preferences.draftSubject
    fun getDraftBody(): String = preferences.draftBody
    fun getDraftCategory(): String = preferences.draftCategory

    fun saveDraft(recipient: String, subject: String, body: String, category: String) {
        preferences.draftRecipient = recipient
        preferences.draftSubject = subject
        preferences.draftBody = body
        preferences.draftCategory = category
    }

    fun startEditingDraft(mail: EmailMessage) {
        saveDraft(mail.recipient ?: "", mail.subject, mail.body, mail.category)
        activeEditingDraftId.value = mail.id
    }

    fun clearDraft() {
        preferences.draftRecipient = ""
        preferences.draftSubject = ""
        preferences.draftBody = ""
        preferences.draftCategory = "Primary"
        activeEditingDraftId.value = null
    }
}
