package com.example.util

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.EmailRepository
import com.example.data.PreferenceManager
import kotlinx.coroutines.flow.first

class EmailSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d("EmailSyncWorker", "Periodic background email sync worker started.")
        val repository = EmailRepository(applicationContext)
        val preferences = PreferenceManager(applicationContext)

        return try {
            val activeAccounts = repository.allAccounts.first()
            val backendUrl = preferences.renderBackendUrl

            for (account in activeAccounts) {
                // If OAuth token has expired or is expiring soon, refresh it
                if (account.refreshToken.isNotEmpty() && account.expiresAt < System.currentTimeMillis() + 5 * 60 * 1000) {
                    Log.d("EmailSyncWorker", "Token expiring soon for ${account.email}, refreshing...")
                    repository.refreshAccessToken(account.email, backendUrl)
                }
                Log.d("EmailSyncWorker", "Syncing emails for ${account.email}")
                repository.syncEmailsForAccount(account.email)
            }
            Log.d("EmailSyncWorker", "Periodic background email sync completed successfully.")
            Result.success()
        } catch (e: Exception) {
            Log.e("EmailSyncWorker", "Periodic background email sync error", e)
            Result.retry()
        }
    }
}
