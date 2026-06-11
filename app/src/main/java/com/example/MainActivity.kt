package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModelProvider
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import com.example.util.EmailSyncWorker
import com.example.ui.EmailViewModel
import com.example.ui.screens.MainAppCompose
import java.util.concurrent.TimeUnit

class MainActivity : androidx.fragment.app.FragmentActivity() {
    private lateinit var viewModel: EmailViewModel
    private val intentState = mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Permission checks are handled dynamically inside Compose screen entry points for stability.

        viewModel = ViewModelProvider(this)[EmailViewModel::class.java]
        
        // Schedule periodic background email synchronization using WorkManager
        scheduleBackgroundEmailSync()

        // Handle deep link from launch intent
        intentState.value = intent

        setContent {
            MainAppCompose(
                viewModel = viewModel,
                fragmentActivity = this,
                intentData = intentState.value,
                onClearIntent = { intentState.value = null }
            )
        }
    }

    private fun scheduleBackgroundEmailSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<EmailSyncWorker>(
            15, TimeUnit.MINUTES
        ).setConstraints(constraints)
            .build()

        try {
            WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                "EmailBackgroundSyncWork",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intentState.value = intent
    }
}
