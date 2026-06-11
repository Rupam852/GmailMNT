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

        // Request notification permission at runtime on API 33+ with a delay to prevent clashing with startup transitions
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                window.decorView.postDelayed({
                    if (!isFinishing && !isDestroyed) {
                        androidx.core.app.ActivityCompat.requestPermissions(this, arrayOf(permission), 101)
                    }
                }, 3000)
            }
        }

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
