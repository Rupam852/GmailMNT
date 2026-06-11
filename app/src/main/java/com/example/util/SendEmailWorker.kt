package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.EmailDatabase
import com.example.data.EmailRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SendEmailWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d("SendEmailWorker", "SendEmailWorker started processing offline outbox queue.")
        val database = EmailDatabase.getDatabase(applicationContext)
        val dao = database.emailDao()
        val repository = EmailRepository(applicationContext)

        try {
            val pending = dao.getAllOutboxMessagesDirect()
            Log.d("SendEmailWorker", "Found ${pending.size} pending offline emails.")

            var allSuccessful = true
            for (email in pending) {
                Log.d("SendEmailWorker", "Attempting to send email from ${email.fromEmail} to ${email.toEmail}")
                
                // Parse attachment URIs if any
                val attachmentUris = if (email.attachmentUris.isNotBlank()) {
                    email.attachmentUris.split(",")
                        .filter { it.isNotBlank() }
                        .map { Uri.parse(it) }
                } else {
                    emptyList()
                }

                val success = repository.sendEmail(
                    fromEmail = email.fromEmail,
                    toEmail = email.toEmail,
                    subject = email.subject,
                    body = email.body,
                    threadId = email.threadId,
                    attachmentUris = attachmentUris
                )
                if (success) {
                    dao.deleteOutboxMessageById(email.id)
                    Log.d("SendEmailWorker", "Successfully sent offline email with id ${email.id}.")
                } else {
                    allSuccessful = false
                    Log.e("SendEmailWorker", "Failed to send offline email with id ${email.id}, will retry later.")
                }
            }

            if (allSuccessful) {
                Log.d("SendEmailWorker", "All pending emails sent successfully.")
                Result.success()
            } else {
                Log.d("SendEmailWorker", "Some emails failed to send. Requesting retry.")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("SendEmailWorker", "Error processing SendEmailWorker", e)
            Result.retry()
        }
    }
}
