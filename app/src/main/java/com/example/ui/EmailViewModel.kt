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
import kotlinx.coroutines.withContext
import java.util.UUID

class EmailViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = EmailRepository(application)
    private val preferences = PreferenceManager(application)

    // Local States loaded from Preferences
    val isDarkMode = MutableStateFlow(preferences.isDarkMode)
    val isBiometricEnabled = MutableStateFlow(preferences.isBiometricEnabled)
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
        repository.allMessages
    ) { filter, tag, allMails ->
        var list = allMails

        // 1. Filter by Account
        if (filter.accountEmail != null && filter.accountEmail != "All") {
            list = list.filter { it.accountEmail == filter.accountEmail }
        }

        // 2. Filter by Category
        if (filter.category != "All") {
            list = list.filter { it.category.equals(filter.category, ignoreCase = true) }
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
    val selectedMail: StateFlow<EmailMessage?> = _selectedMailId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else repository.allMessages.map { list -> list.find { it.id == id } }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // State indicators
    val isLoading = MutableStateFlow(false)
    val geminiResponse = MutableStateFlow("")
    val isGeneratingDraft = MutableStateFlow(false)

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
            repository.updateMessageStarredStatus(id, !currentStarred)
        }
    }

    fun markAsRead(id: String, read: Boolean = true) {
        viewModelScope.launch {
            repository.updateMessageReadStatus(id, read)
        }
    }

    fun markAllAsRead(read: Boolean) {
        viewModelScope.launch {
            repository.markAllMessagesReadStatus(read)
        }
    }

    fun deleteMail(id: String) {
        viewModelScope.launch {
            repository.deleteMessage(id)
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
        category: String = "Primary"
    ) {
        viewModelScope.launch {
            val success = repository.sendEmail(fromEmail, toEmail, subject, body)
            if (success) {
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
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        getApplication(),
                        "Email sent successfully!",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
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
    fun triggerSyncAll() {
        viewModelScope.launch {
            isLoading.value = true
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
                isLoading.value = false
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

    fun clearDraft() {
        preferences.draftRecipient = ""
        preferences.draftSubject = ""
        preferences.draftBody = ""
        preferences.draftCategory = "Primary"
    }
}
