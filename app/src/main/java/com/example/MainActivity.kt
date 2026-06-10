package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModelProvider
import com.example.ui.EmailViewModel
import com.example.ui.screens.MainAppCompose

class MainActivity : androidx.fragment.app.FragmentActivity() {
    private lateinit var viewModel: EmailViewModel
    private val intentState = mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel = ViewModelProvider(this)[EmailViewModel::class.java]
        
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intentState.value = intent
    }
}
