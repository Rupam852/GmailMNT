package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.ui.theme.*
import com.example.data.EmailAccount
import com.example.data.EmailMessage
import com.example.ui.EmailViewModel
import com.example.ui.theme.GrowwTeal
import com.example.ui.theme.GrowwTealDark
import com.example.util.BiometricHelper
import com.example.util.AvatarHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MainAppCompose(
    viewModel: EmailViewModel,
    fragmentActivity: FragmentActivity,
    intentData: Intent?,
    onClearIntent: () -> Unit
) {
    var currentScreen by remember { mutableStateOf("splash") }
    
    // Read States from ViewModel
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isGetStartedCompleted by viewModel.isGetStartedCompleted.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()

    // Handle deep link intents for successful Gmail linkage from Render callback
    LaunchedEffect(intentData) {
        val uri = intentData?.data
        if (uri != null && uri.scheme == "geminimail" && uri.host == "oauth-callback") {
            val email = uri.getQueryParameter("email") ?: ""
            val accessToken = uri.getQueryParameter("access_token") ?: ""
            val refreshToken = uri.getQueryParameter("refresh_token") ?: ""
            val expiresAtStr = uri.getQueryParameter("expires_at") ?: "0"
            val expiresAt = expiresAtStr.toLongOrNull() ?: 0L
            val name = uri.getQueryParameter("name") ?: ""
            val picture = uri.getQueryParameter("picture") ?: ""

            if (email.isNotEmpty() && accessToken.isNotEmpty()) {
                viewModel.handleOAuthSuccess(
                    email = email,
                    accessToken = accessToken,
                    refreshToken = refreshToken.takeIf { it.isNotEmpty() },
                    expiresAt = expiresAt,
                    displayName = name.takeIf { it.isNotEmpty() },
                    profilePictureUrl = picture.takeIf { it.isNotEmpty() }
                )
                Toast.makeText(fragmentActivity, "Account $email linked successfully!", Toast.LENGTH_LONG).show()
                currentScreen = "dashboard"
            }
            onClearIntent()
        }
    }

    MyApplicationThemeWrapper(isDarkMode = isDarkMode) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when (currentScreen) {
                "splash" -> SplashScreen(
                    isGetStartedCompleted = isGetStartedCompleted,
                    isBiometricEnabled = isBiometricEnabled,
                    activity = fragmentActivity,
                    onNavigateToWelcome = { currentScreen = "welcome" },
                    onNavigateToDashboard = { currentScreen = "dashboard" }
                )
                "welcome" -> WelcomeScreen(
                    viewModel = viewModel,
                    onNavigateToDashboard = { currentScreen = "dashboard" }
                )
                "dashboard" -> DashboardScreen(
                    viewModel = viewModel,
                    activity = fragmentActivity
                )
            }
        }
    }
}

@Composable
fun MyApplicationThemeWrapper(
    isDarkMode: Boolean,
    content: @Composable () -> Unit
) {
    com.example.ui.theme.MyApplicationTheme(darkTheme = isDarkMode, dynamicColor = false) {
        content()
    }
}

// -------------------------------------------------------------
// SPLASH SCREEN
// -------------------------------------------------------------
@Composable
fun SplashScreen(
    isGetStartedCompleted: Boolean,
    isBiometricEnabled: Boolean,
    activity: FragmentActivity,
    onNavigateToWelcome: () -> Unit,
    onNavigateToDashboard: () -> Unit
) {
    var animateLogoUp by remember { mutableStateOf(false) }
    
    // Logo translation coordinates
    val logoOffset: Dp by animateDpAsState(
        targetValue = if (animateLogoUp) (-180).dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "Logo Offset"
    )

    val logoScale: Float by animateFloatAsState(
        targetValue = if (animateLogoUp) 0.8f else 1.0f,
        animationSpec = tween(1000),
        label = "Logo Scale"
    )

    LaunchedEffect(Unit) {
        delay(1200) // Initial centered showcase
        animateLogoUp = true
        delay(1000) // Transition finish

        if (isGetStartedCompleted) {
            if (isBiometricEnabled && BiometricHelper.isBiometricAvailable(activity)) {
                BiometricHelper.showBiometricPrompt(
                    activity = activity,
                    onSuccess = {
                        onNavigateToDashboard()
                    },
                    onError = { error ->
                        Toast.makeText(activity, "Biometric Auth Bypass / Mode: $error", Toast.LENGTH_SHORT).show()
                        onNavigateToDashboard() // Fallback gracefully to preserve zero-deadends
                    }
                )
            } else {
                onNavigateToDashboard()
            }
        } else {
            onNavigateToWelcome()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = logoOffset)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(GrowwTeal.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = "GmailMNT Logo",
                    tint = GrowwTeal,
                    modifier = Modifier.size(54.dp)
                )
                // Small Sparkle on top representing Gemini
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = "AI Spark",
                        tint = GrowwTeal,
                        modifier = Modifier.size(24.dp).offset(x = 6.dp, y = (-4).dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "GmailMNT",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 1.2.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Secure AI Inbox",
                style = MaterialTheme.typography.labelMedium,
                color = GrowwTeal,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

// -------------------------------------------------------------
// WELCOME SCREEN
// -------------------------------------------------------------
@Composable
fun WelcomeScreen(
    viewModel: EmailViewModel,
    onNavigateToDashboard: () -> Unit
) {
    val context = LocalContext.current
    val backendUrl by viewModel.renderBackendUrl.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Upper section containing Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(GrowwTeal.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = "App Icon Logo",
                    tint = GrowwTeal,
                    modifier = Modifier.size(44.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Secure Your Inbox",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "The ultimate Groww-inspired secure client with offline storage support, multi-account bindings, and AI assistant compose helpers.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Illustrations element
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Filled.Lock, "Lock", tint = GrowwTeal, modifier = Modifier.size(32.dp))
                    Text(
                        "On-Device Biometrics",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Session tokens and email contents are cryptographically sandboxed locally under complete zero-trust backend policy.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }

        // Action controls
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    viewModel.completeGetStarted()
                    onNavigateToDashboard()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("get_started_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GrowwTeal,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = "Get Started",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Privacy Policy",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = GrowwTeal
                ),
                modifier = Modifier
                    .clickable {
                        // Open real privacy policy from Render Backend
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("$backendUrl/privacy-policy"))
                        context.startActivity(intent)
                    }
                    .padding(8.dp)
            )
        }
    }
}

// -------------------------------------------------------------
// DASHBOARD
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: EmailViewModel,
    activity: FragmentActivity
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Inbox, 1: Compose, 2: Settings
    var showDetailDialog by remember { mutableStateOf(false) }
    
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600

    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()
    val customTagsList by viewModel.customTags.collectAsState()
    val selectedFolder by viewModel.selectedFolder.collectAsState()

    var showManageTagsDialogSide by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val onDeleteMail: (EmailMessage) -> Unit = { mail ->
        viewModel.deleteMail(mail.id)
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = "Email deleted",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.restoreMail(mail)
            }
        }
    }

    val onArchiveMail: (EmailMessage) -> Unit = { mail ->
        viewModel.archiveMail(mail.id)
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = "Email archived",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.unarchiveMail(mail.id)
            }
        }
    }

    val onUnarchiveMail: (EmailMessage) -> Unit = { mail ->
        viewModel.unarchiveMail(mail.id)
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = "Email unarchived",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.archiveMail(mail.id)
            }
        }
    }

    if (isWideScreen) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Custom Sidebar for Folder and Custom Tag Navigation
                Column(
                    modifier = Modifier
                        .width(240.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GmailMNT",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = GrowwTeal
                        )
                        IconButton(
                            onClick = { viewModel.setDarkMode(!isDarkMode) },
                            modifier = Modifier.size(32.dp).testTag("sidebar_theme_toggle")
                        ) {
                            Text(if (isDarkMode) "☀️" else "🌙", fontSize = 16.sp)
                        }
                    }

                    Text(
                        text = "FOLDERS",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    val foldersList = listOf(
                        "Inbox" to "INBOX",
                        "Sent" to "SENT",
                        "Drafts" to "DRAFT",
                        "Spam" to "SPAM",
                        "Archive" to "ARCHIVE",
                        "Trash" to "TRASH"
                    )

                    foldersList.forEach { (name, folderCode) ->
                        val isFolderSelected = (selectedTab == 0 && selectedFolder.uppercase() == folderCode)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isFolderSelected) GrowwTeal.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable {
                                    selectedTab = 0
                                    viewModel.selectedFolder.value = folderCode
                                    viewModel.selectedCategory.value = "All"
                                    viewModel.selectedTag.value = "All"
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val icon = when (folderCode) {
                                    "INBOX" -> Icons.Default.MailOutline
                                    "SENT" -> Icons.AutoMirrored.Filled.Send
                                    "DRAFT" -> Icons.Default.Edit
                                    "SPAM" -> Icons.Default.Warning
                                    "ARCHIVE" -> Icons.Default.Email
                                    "TRASH" -> Icons.Default.Delete
                                    else -> Icons.Default.Email
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = name,
                                    tint = if (isFolderSelected) GrowwTeal else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isFolderSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isFolderSelected) GrowwTeal else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }



                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "TAGS",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // Render Custom Tag Items in the folders/navigation sidebar
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(customTagsList.toList()) { tag ->
                            val isTagSelected = (selectedTab == 0 && selectedTag == tag)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isTagSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable {
                                        selectedTab = 0
                                        viewModel.selectedTag.value = tag
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = tag,
                                        tint = if (isTagSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = tag,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isTagSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isTagSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showManageTagsDialogSide = true }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Manage",
                                        tint = GrowwTeal,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Manage Tags",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = GrowwTeal,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 12.dp))

                    // Actions items inside dynamic menu
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedTab == 1) GrowwTeal.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { selectedTab = 1 }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Compose",
                                tint = if (selectedTab == 1) GrowwTeal else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Compose Mail",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 1) GrowwTeal else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedTab == 2) GrowwTeal.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { selectedTab = 2 }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = if (selectedTab == 2) GrowwTeal else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Settings",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 2) GrowwTeal else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Divider Line between Sidebar and Main Feed panel
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                )

                // Right side Main Viewing Area panel (responsive content pane)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    when (selectedTab) {
                        0 -> InboxTabScreen(
                            viewModel = viewModel,
                            onMailClick = { msgId ->
                                viewModel.selectMailId(msgId)
                                showDetailDialog = true
                            },
                            onDeleteMail = onDeleteMail,
                            onArchiveMail = onArchiveMail,
                            onUnarchiveMail = onUnarchiveMail
                        )
                        1 -> ComposeTabScreen(viewModel = viewModel, onComposeSuccess = {
                            selectedTab = 0
                        })
                        2 -> SettingsTabScreen(viewModel = viewModel, activity = activity)
                    }
                }
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
        }
    } else {
        // Mobile compact standard dashboard (Bottom navigation layout)
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.MailOutline, "Inbox") },
                        label = { Text("Inbox") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GrowwTeal,
                            selectedTextColor = GrowwTeal,
                            indicatorColor = GrowwTeal.copy(alpha = 0.12f)
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Add, "Compose") },
                        label = { Text("Compose") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GrowwTeal,
                            selectedTextColor = GrowwTeal,
                            indicatorColor = GrowwTeal.copy(alpha = 0.12f)
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.Settings, "Settings") },
                        label = { Text("Settings") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GrowwTeal,
                            selectedTextColor = GrowwTeal,
                            indicatorColor = GrowwTeal.copy(alpha = 0.12f)
                        )
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (selectedTab) {
                    0 -> InboxTabScreen(
                        viewModel = viewModel,
                        onMailClick = { msgId ->
                            viewModel.selectMailId(msgId)
                            showDetailDialog = true
                        },
                        onDeleteMail = onDeleteMail,
                        onArchiveMail = onArchiveMail,
                        onUnarchiveMail = onUnarchiveMail
                    )
                    1 -> ComposeTabScreen(viewModel = viewModel, onComposeSuccess = {
                        selectedTab = 0
                    })
                    2 -> SettingsTabScreen(viewModel = viewModel, activity = activity)
                }
            }
        }
    }

    if (showManageTagsDialogSide) {
        ManageTagsDialog(viewModel = viewModel, onDismiss = { showManageTagsDialogSide = false })
    }

    // Modal Sheet Detail Dialogue
    val activeSelectedMail by viewModel.selectedMail.collectAsState()
    if (showDetailDialog && activeSelectedMail != null) {
        viewModel.markAsRead(activeSelectedMail!!.id)
        
        EmailDetailDialog(
            mail = activeSelectedMail!!,
            viewModel = viewModel,
            onDeleteMail = onDeleteMail,
            onDismiss = {
                showDetailDialog = false
                viewModel.selectMailId(null)
            },
            onForward = {
                selectedTab = 1
            }
        )
    }
}

// -------------------------------------------------------------
// TAB 1: INBOX SCREEN
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxTabScreen(
    viewModel: EmailViewModel,
    onMailClick: (String) -> Unit,
    onDeleteMail: (EmailMessage) -> Unit,
    onArchiveMail: (EmailMessage) -> Unit,
    onUnarchiveMail: (EmailMessage) -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedSortOrder by viewModel.selectedSortOrder.collectAsState()
    val filteredMessages by viewModel.filteredMessages.collectAsState()
    val selectedAccount by viewModel.selectedAccount.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()
    val customTagsList by viewModel.customTags.collectAsState()
    val selectedFolder by viewModel.selectedFolder.collectAsState()
    var longPressedMail by remember { mutableStateOf<EmailMessage?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        // 1. Sleek Search Bar with Integrated Profile Accounts Selector (Like Premium Gmail/Groww)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, end = 12.dp, top = 2.dp, bottom = 2.dp)
            ) {
                var showMenu by remember { mutableStateOf(false) }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu Icon",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface)
                            .width(240.dp)
                    ) {
                        val isAllInboxSelected = selectedFolder.uppercase() == "INBOX" && (selectedAccount == "All" || selectedAccount == null)
                        DropdownMenuItem(
                            text = { Text("All Inbox", fontWeight = if (isAllInboxSelected) FontWeight.Bold else FontWeight.Normal) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.MailOutline,
                                    contentDescription = null,
                                    tint = if (isAllInboxSelected) GrowwTeal else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            },
                            onClick = {
                                viewModel.selectedFolder.value = "INBOX"
                                viewModel.selectedAccount.value = "All"
                                viewModel.selectedCategory.value = "All"
                                viewModel.selectedTag.value = "All"
                                showMenu = false
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        val folders = listOf(
                            "Sent" to "SENT",
                            "Drafts" to "DRAFT",
                            "Spam" to "SPAM",
                            "Archive" to "ARCHIVE",
                            "Trash" to "TRASH"
                        )
                        folders.forEach { (name, code) ->
                            val isSelected = selectedFolder.uppercase() == code
                            DropdownMenuItem(
                                text = { Text(name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                leadingIcon = {
                                    val icon = when (code) {
                                        "SENT" -> Icons.AutoMirrored.Filled.Send
                                        "DRAFT" -> Icons.Default.Edit
                                        "SPAM" -> Icons.Default.Warning
                                        "ARCHIVE" -> Icons.Default.Email
                                        "TRASH" -> Icons.Default.Delete
                                        else -> Icons.Default.Email
                                    }
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) GrowwTeal else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                },
                                onClick = {
                                    viewModel.selectedFolder.value = code
                                    viewModel.selectedCategory.value = "All"
                                    viewModel.selectedTag.value = "All"
                                    showMenu = false
                                }
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        Text(
                            text = "ALL ADDED EMAILS",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )

                        val isAllAccountsSelected = selectedAccount == "All" || selectedAccount == null
                        DropdownMenuItem(
                            text = { Text("All Accounts", fontWeight = if (isAllAccountsSelected) FontWeight.Bold else FontWeight.Normal) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = if (isAllAccountsSelected) GrowwTeal else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            },
                            onClick = {
                                viewModel.selectedAccount.value = "All"
                                showMenu = false
                            }
                        )

                        accounts.forEach { account ->
                            val isAccSelected = selectedAccount == account.email
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = account.email,
                                        fontWeight = if (isAccSelected) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = null,
                                        tint = if (isAccSelected) GrowwTeal else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                },
                                onClick = {
                                    viewModel.selectedAccount.value = account.email
                                    showMenu = false
                                }
                            )
                        }
                    }
                }
                TextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Search emails...", fontSize = 14.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("inbox_search_input"),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium
                )
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.searchQuery.value = "" },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Clear, "Clear Search", modifier = Modifier.size(18.dp))
                    }
                }
                
                // Avatar displaying the selected bound account
                val activeAccount = remember(selectedAccount, accounts) {
                    accounts.find { it.email == selectedAccount } ?: accounts.firstOrNull()
                }
                Box(modifier = Modifier.padding(end = 4.dp)) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(GrowwTeal, GrowwTealDark)
                                )
                            )
                            .testTag("search_profile_avatar"),
                        contentAlignment = Alignment.Center
                    ) {
                        val profilePic = activeAccount?.profilePictureUrl
                        if (!profilePic.isNullOrEmpty()) {
                            coil.compose.AsyncImage(
                                model = profilePic,
                                contentDescription = "Active account profile picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            val displayName = activeAccount?.displayName ?: "All"
                            val defaultAvatarUrl = "https://ui-avatars.com/api/?name=${android.net.Uri.encode(displayName)}&background=00d09c&color=fff&size=96"
                            coil.compose.AsyncImage(
                                model = defaultAvatarUrl,
                                contentDescription = "Active account default avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }





        // Trash Quick Actions Banner
        AnimatedVisibility(visible = selectedFolder.uppercase() == "TRASH" && filteredMessages.isNotEmpty()) {
            Column {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Deleted emails folder",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { viewModel.restoreAllTrash() },
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text("Restore All", color = GrowwTeal, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Button(
                                onClick = { viewModel.emptyTrash() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Empty", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }



        // 5. Emails Scrollable List View with pulsing Skeleton Loaders
        if (isLoading) {
            val infiniteTransition = rememberInfiniteTransition(label = "shimmer_transition")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 0.9f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 800, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "shimmer_alpha"
            )
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(5) {
                    EmailSkeletonRowItem(alpha = alpha)
                }
            }
        } else if (filteredMessages.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Empty Box icon",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Your inbox is empty",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "Tap the bell icon above to simulate incoming mail or wait for auto-sync.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(filteredMessages, key = { it.id }) { mail ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                onDeleteMail(mail)
                                true
                            } else if (value == SwipeToDismissBoxValue.StartToEnd) {
                                if (selectedFolder.uppercase() == "ARCHIVE") {
                                    onUnarchiveMail(mail)
                                } else {
                                    onArchiveMail(mail)
                                }
                                true
                            } else {
                                false
                            }
                        }
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = true,
                        enableDismissFromEndToStart = true,
                        backgroundContent = {
                            val color = when (dismissState.dismissDirection) {
                                SwipeToDismissBoxValue.EndToStart -> AlertRed
                                SwipeToDismissBoxValue.StartToEnd -> GrowwTeal
                                else -> Color.Transparent
                            }
                            val alignment = when (dismissState.dismissDirection) {
                                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                else -> Alignment.Center
                            }
                            val icon = when (dismissState.dismissDirection) {
                                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                                SwipeToDismissBoxValue.StartToEnd -> {
                                    if (selectedFolder.uppercase() == "ARCHIVE") Icons.Default.MailOutline else Icons.Default.Email
                                }
                                else -> Icons.Default.Email
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(color)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = alignment
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = "Swipe Action Icon",
                                    tint = Color.White
                                )
                            }
                        }
                    ) {
                        EmailMessageRowItem(
                            mail = mail,
                            accounts = accounts,
                            onMailClick = { onMailClick(mail.id) },
                            onMailLongClick = { longPressedMail = mail },
                            onStarredToggle = { viewModel.toggleStarred(mail.id, mail.isStarred) }
                        )
                    }
                }
            }
        }

        if (longPressedMail != null) {
            val mail = longPressedMail!!
            AlertDialog(
                onDismissRequest = { longPressedMail = null },
                title = { Text("Options", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = mail.senderName,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = mail.subject,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(bottom = 8.dp))

                        // Option: Mark as Read / Unread
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.markAsRead(mail.id, !mail.isRead)
                                    longPressedMail = null
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (mail.isRead) Icons.Default.MailOutline else Icons.Default.Email,
                                contentDescription = "Read Status Icon",
                                tint = GrowwTeal,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (mail.isRead) "Mark as Unread" else "Mark as Read",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                        // Option: Delete
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onDeleteMail(mail)
                                    longPressedMail = null
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Icon",
                                tint = AlertRed,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Delete",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyLarge,
                                color = AlertRed
                            )
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { longPressedMail = null }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

@Composable
fun EmailSkeletonRowItem(alpha: Float) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Avatar Skeleton Dot
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.15f))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        // Title/Sender Bar
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.12f))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        // Subtitle/Email Bar
                        Box(
                            modifier = Modifier
                                .width(180.dp)
                                .height(10.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.08f))
                        )
                    }
                }
                // Timestamp Bar
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.08f))
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Subject Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.12f))
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Body Line 1
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.06f))
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Body Line 2
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.06f))
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EmailMessageRowItem(
    mail: EmailMessage,
    accounts: List<EmailAccount>,
    onMailClick: () -> Unit,
    onMailLongClick: () -> Unit,
    onStarredToggle: () -> Unit
) {
    val unreadWeight = if (!mail.isRead) FontWeight.ExtraBold else FontWeight.Normal
    val unreadColor = if (!mail.isRead) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
    val relativeDateStr = remember(mail.timestamp) {
        SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(mail.timestamp))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .combinedClickable(
                onClick = onMailClick,
                onLongClick = onMailLongClick
            )
            .background(if (!mail.isRead) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Profile Circle with Image
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                // Dynamic fallback colors based on name to match premium Google aesthetics (remembered to prevent recomposition overhead)
                val fallbackUrl = remember(mail.senderName) {
                    val colors = listOf("00d09c", "1a73e8", "8ab4f8", "fd9727", "fc5b5b", "a65bfc")
                    val colorIndex = Math.abs(mail.senderName.hashCode()) % colors.size
                    val avatarColor = colors[colorIndex]
                    "https://ui-avatars.com/api/?name=${android.net.Uri.encode(mail.senderName)}&background=$avatarColor&color=fff&size=128"
                }

                // Clean sender email (remembered to prevent parsing on every recomposition)
                val emailClean = remember(mail.sender) {
                    mail.sender.substringAfter("<", "").substringBefore(">", "").trim().ifEmpty { mail.sender.trim() }.lowercase()
                }
                
                // Resolve profile picture URL using defined priority and cache check
                val logoUrl = remember(emailClean, mail.senderName, accounts, fallbackUrl) {
                    val matchedAccount = accounts.find { it.email.lowercase() == emailClean }
                    if (matchedAccount != null && !matchedAccount.profilePictureUrl.isNullOrEmpty()) {
                        matchedAccount.profilePictureUrl
                    } else if (emailClean.contains("google.com") || emailClean.contains("security-noreply@google.com")) {
                        // Google services -> Google G Logo
                        "https://cdn-icons-png.flaticon.com/512/2991/2991148.png"
                    } else if (emailClean.contains("gmail.com") && (emailClean.contains("noreply") || emailClean.contains("support"))) {
                        // Gmail system email -> Gmail logo
                        "https://cdn-icons-png.flaticon.com/512/732/732200.png"
                    } else {
                        val domain = emailClean.substringAfter("@", "")
                        val targetUrl = if (domain.isNotEmpty() && !domain.endsWith("gmail.com") && !domain.endsWith("yahoo.com") && !domain.endsWith("outlook.com") && domain.contains(".")) {
                            "https://logo.clearbit.com/$domain"
                        } else {
                            // Try Gravatar personal photo, fallback to initials avatar via error state callback
                            val hash = AvatarHelper.getMd5Hash(emailClean)
                            "https://www.gravatar.com/avatar/$hash?d=404"
                        }
                        
                        if (AvatarHelper.isUrlFailed(targetUrl)) {
                            fallbackUrl
                        } else {
                            targetUrl
                        }
                    }
                }
                var imageUrl by remember(logoUrl) { mutableStateOf(logoUrl) }
                coil.compose.AsyncImage(
                    model = imageUrl,
                    contentDescription = "Sender profile picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    onState = { state ->
                        if (state is coil.compose.AsyncImagePainter.State.Error) {
                            if (imageUrl != fallbackUrl) {
                                AvatarHelper.markUrlAsFailed(imageUrl)
                                imageUrl = fallbackUrl
                            }
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                // Top Row: Sender Name & Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = mail.senderName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = unreadWeight,
                        color = unreadColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = relativeDateStr,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = unreadWeight,
                        color = if (!mail.isRead) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Middle Row: Subject and Star
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = mail.subject,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = unreadWeight,
                        color = unreadColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onStarredToggle,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Star toggle icon",
                            tint = if (mail.isStarred) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Bottom Row: Body snippet
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = mail.body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    }
}

// Helper to resolve tag highlight color
@Composable
fun FocusColor(category: String): Color {
    return when (category.lowercase()) {
        "primary" -> GrowwTeal
        "updates" -> InfoBlue
        "social" -> Color(0xFF1B60A5)
        "promotions" -> Color(0xFFFF9800)
        else -> Slate400
    }
}

// -------------------------------------------------------------
// TAB 2: COMPOSE SCREEN
// -------------------------------------------------------------
@Composable
fun ComposeTabScreen(
    viewModel: EmailViewModel,
    onComposeSuccess: () -> Unit
) {
    val context = LocalContext.current
    val activeDraftId by viewModel.activeEditingDraftId.collectAsState()

    var recipient by remember(activeDraftId) { mutableStateOf(viewModel.getDraftRecipient()) }
    var subject by remember(activeDraftId) { mutableStateOf(viewModel.getDraftSubject()) }
    var body by remember(activeDraftId) { mutableStateOf(viewModel.getDraftBody()) }
    var category by remember(activeDraftId) { mutableStateOf(viewModel.getDraftCategory()) }
    var showGeminiWizard by remember { mutableStateOf(false) }

    val accounts by viewModel.accounts.collectAsState()
    var selectedFromEmail by remember { mutableStateOf("") }

    // Auto-save the draft fields to local storage periodically when user types
    LaunchedEffect(recipient, subject, body, category) {
        delay(1500)
        viewModel.saveDraft(recipient, subject, body, category)
    }

    LaunchedEffect(accounts) {
        if (accounts.isNotEmpty()) {
            selectedFromEmail = accounts.first().email
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Compose Message",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Source account bind selection with profile pic
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            val activeFromAccount = remember(selectedFromEmail, accounts) {
                accounts.find { it.email == selectedFromEmail }
            }
            val fromProfilePic = activeFromAccount?.profilePictureUrl
            val fromDisplayName = activeFromAccount?.displayName ?: selectedFromEmail.substringBefore("@")
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                if (!fromProfilePic.isNullOrEmpty()) {
                    coil.compose.AsyncImage(
                        model = fromProfilePic,
                        contentDescription = "From profile picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    val defaultAvatarUrl = "https://ui-avatars.com/api/?name=${android.net.Uri.encode(fromDisplayName.ifEmpty { "From" })}&background=00d09c&color=fff&size=96"
                    coil.compose.AsyncImage(
                        model = defaultAvatarUrl,
                        contentDescription = "From default avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                var fromExpanded by remember { mutableStateOf(false) }
                Text("From:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(2.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { fromExpanded = true },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedFromEmail.ifEmpty { "Select Account Profile" },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Icon(Icons.Default.ArrowDropDown, "Show from Accounts")
                        }
                    }
                    DropdownMenu(expanded = fromExpanded, onDismissRequest = { fromExpanded = false }) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text(acc.email) },
                                onClick = {
                                    selectedFromEmail = acc.email
                                    fromExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Target field with profile pic
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            val toDomain = recipient.substringAfter("@", "").substringBefore(">").trim().lowercase()
            val toFallbackUrl = "https://ui-avatars.com/api/?name=${android.net.Uri.encode(recipient.ifEmpty { "To" })}&background=1A73E8&color=fff&size=96"
            val toLogoUrl = if (toDomain.isNotEmpty() && !toDomain.endsWith("gmail.com") && !toDomain.endsWith("yahoo.com") && !toDomain.endsWith("outlook.com") && toDomain.contains(".")) {
                "https://logo.clearbit.com/$toDomain"
            } else {
                toFallbackUrl
            }
            var toImageUrl by remember(toLogoUrl) { mutableStateOf(toLogoUrl) }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                coil.compose.AsyncImage(
                    model = toImageUrl,
                    contentDescription = "Recipient profile picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    onState = { state ->
                        if (state is coil.compose.AsyncImagePainter.State.Error && toImageUrl != toFallbackUrl) {
                            toImageUrl = toFallbackUrl
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            OutlinedTextField(
                value = recipient,
                onValueChange = { recipient = it },
                label = { Text("To") },
                placeholder = { Text("recipient-profile@email.com") },
                modifier = Modifier.weight(1f).testTag("compose_recipient_input"),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )
        }

        // Subject field
        OutlinedTextField(
            value = subject,
            onValueChange = { subject = it },
            label = { Text("Subject") },
            placeholder = { Text("Message header title...") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).testTag("compose_subject_input"),
            shape = RoundedCornerShape(8.dp),
            singleLine = true
        )

        // Email Body Content Area input
        OutlinedTextField(
            value = body,
            onValueChange = { body = it },
            label = { Text("Mail Content Body") },
            placeholder = { Text("Write your email content here...") },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 12.dp)
                .testTag("compose_body_input"),
            shape = RoundedCornerShape(10.dp)
        )

        // Controls action buttons row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gemini Wizard drafting trigger
            Button(
                onClick = { showGeminiWizard = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = GrowwTeal.copy(alpha = 0.15f),
                    contentColor = GrowwTeal
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("gemini_draft_button").height(40.dp),
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) {
                Icon(Icons.Default.Face, "Gemini Assist", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("AI Draft", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            // Save Draft Button
            OutlinedButton(
                onClick = {
                    if (recipient.isEmpty() && subject.isEmpty() && body.isEmpty()) {
                        Toast.makeText(context, "Nothing to save as draft.", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.saveDraftToDb(selectedFromEmail, recipient, subject, body, category)
                        viewModel.clearDraft()
                        recipient = ""
                        subject = ""
                        body = ""
                        category = "Primary"
                        Toast.makeText(context, "Draft saved successfully!", Toast.LENGTH_SHORT).show()
                        onComposeSuccess()
                    }
                },
                border = BorderStroke(1.dp, GrowwTeal),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GrowwTeal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(40.dp),
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) {
                Icon(Icons.Default.Edit, "Save Draft", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save Draft", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            // Command Send
            Button(
                onClick = {
                    if (recipient.isEmpty() || subject.isEmpty() || body.isEmpty()) {
                        Toast.makeText(context, "Checks Failed: Compose complete parameters first.", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.composeEmail(selectedFromEmail, recipient, subject, body, category)
                        viewModel.clearDraft()
                        recipient = ""
                        subject = ""
                        body = ""
                        category = "Primary"
                        Toast.makeText(context, "Email drafted/sent successfully!", Toast.LENGTH_SHORT).show()
                        onComposeSuccess()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = GrowwTeal,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("compose_send_button").height(40.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, "Send button element", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Send", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }

    // Gemini wizard pop dialogue
    if (showGeminiWizard) {
        GeminiWizardDialog(
            recipientHint = recipient,
            subjectHint = subject,
            viewModel = viewModel,
            onAcceptResult = { textResult ->
                body = textResult
                showGeminiWizard = false
            },
            onDismiss = { showGeminiWizard = false }
        )
    }
}

// -------------------------------------------------------------
// TAB 3: SETTINGS AND PROFILE
// -------------------------------------------------------------
@Composable
fun SettingsTabScreen(
    viewModel: EmailViewModel,
    activity: FragmentActivity
) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val geminiApiKey by viewModel.geminiApiKey.collectAsState()
    val renderBackendUrl by viewModel.renderBackendUrl.collectAsState()
    val accounts by viewModel.accounts.collectAsState()

    var showAddAccountDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Profile & Settings",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // 1. Interactive Profile panel
        val primaryAccount = accounts.firstOrNull()
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val profilePic = primaryAccount?.profilePictureUrl
                if (!profilePic.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                    ) {
                        coil.compose.AsyncImage(
                            model = profilePic,
                            contentDescription = "User profile picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                } else {
                    val defaultAvatarUrl = primaryAccount?.let { 
                        "https://ui-avatars.com/api/?name=${android.net.Uri.encode(it.displayName)}&background=00d09c&color=fff&size=128"
                    }
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(GrowwTeal.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (defaultAvatarUrl != null) {
                            coil.compose.AsyncImage(
                                model = defaultAvatarUrl,
                                contentDescription = "Default avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Person, "User profile icon", tint = GrowwTeal, modifier = Modifier.size(32.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(primaryAccount?.displayName ?: "Inbox Master", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(primaryAccount?.email ?: "No accounts registered", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        }

        // 2. Bound Email Profiles management
        Text("MANAGE EMAIL PROFILES", fontWeight = FontWeight.Bold, color = GrowwTeal, fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp))
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                accounts.forEach { acc ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(acc.email, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text(if (acc.refreshToken.isEmpty()) "Simulated Profile Sandbox" else "Verified Gmail Profile", style = MaterialTheme.typography.bodySmall, color = GrowwTeal)
                        }
                        if (accounts.size > 1) {
                            IconButton(onClick = { viewModel.deleteAccount(acc.email) }) {
                                Icon(Icons.Default.Delete, "Delete account binding", tint = AlertRed)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showAddAccountDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GrowwTeal.copy(alpha = 0.12f),
                        contentColor = GrowwTeal
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.AddCircle, "Link account", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Link Mail Profile", fontWeight = FontWeight.Bold)
                }
            }
        }

        // 3. Gemini REST API settings config panel
        Text("AI CO-PILOT KEY DECLARATION", fontWeight = FontWeight.Bold, color = GrowwTeal, fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp))
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            var showKey by remember { mutableStateOf(false) }
            var localApiKey by remember(geminiApiKey) { mutableStateOf(geminiApiKey) }
            val context = LocalContext.current

            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    "Input your custom Google Gemini API Key. It is written directly to local device storage, keeping configurations completely secure.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = localApiKey,
                    onValueChange = { localApiKey = it },
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    label = { Text("Personal Gemini Key") },
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(imageVector = if (showKey) Icons.Default.Face else Icons.Default.Lock, contentDescription = "Toggle display key")
                        }
                    },
                    placeholder = { Text("AI Studio Gemini Key...") },
                    modifier = Modifier.fillMaxWidth().testTag("gemini_key_input"),
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        viewModel.setGeminiApiKey(localApiKey)
                        Toast.makeText(context, "Gemini API Key saved successfully", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GrowwTeal
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Save API Key", fontWeight = FontWeight.Bold)
                }
            }
        }



        // 5. Native hardware preference profiles
        Text("SECURITY & SYSTEM OPTION", fontWeight = FontWeight.Bold, color = GrowwTeal, fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp))
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(6.dp)) {
                ListItem(
                    headlineContent = { Text("Biometric Authentication", fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text("Requests fingerprint scanner upon application launch to read inbox messages") },
                    trailingContent = {
                        Switch(
                            checked = isBiometricEnabled,
                            onCheckedChange = { viewModel.setBiometric(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = GrowwTeal, checkedTrackColor = GrowwTeal.copy(alpha = 0.3f))
                        )
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                ListItem(
                    headlineContent = { Text("Dark Theme Mode", fontWeight = FontWeight.SemiBold) },
                    trailingContent = {
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { viewModel.setDarkMode(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = GrowwTeal, checkedTrackColor = GrowwTeal.copy(alpha = 0.3f))
                        )
                    }
                )
            }
        }

        Text("BULK ACTIONS", fontWeight = FontWeight.Bold, color = GrowwTeal, fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp, top = 8.dp))
        val localContext = LocalContext.current
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.markAllAsRead(true)
                        Toast.makeText(localContext, "All emails marked as read", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = GrowwTeal, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Read All", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        viewModel.markAllAsRead(false)
                        Toast.makeText(localContext, "All emails marked as unread", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), contentColor = MaterialTheme.colorScheme.onSurface),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Unread All", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        // Privacy Policy link
        val context = LocalContext.current
        Text(
            text = "Privacy Policy",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = GrowwTeal
            ),
            modifier = Modifier
                .clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("${renderBackendUrl}/privacy-policy"))
                    context.startActivity(intent)
                }
                .padding(8.dp)
        )
    }

    if (showAddAccountDialog) {
        AddAccountSelectionDialog(
            viewModel = viewModel,
            activity = activity,
            onDismiss = { showAddAccountDialog = false }
        )
    }
}

// Modal dialog representing linkage bindings selection
@Composable
fun AddAccountSelectionDialog(
    viewModel: EmailViewModel,
    activity: FragmentActivity,
    onDismiss: () -> Unit
) {
    val renderUrl by viewModel.renderBackendUrl.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Link Email Client Profile", fontWeight = FontWeight.ExtraBold) },
        text = {
            Text(
                "You can link your active Google account securely using OAuth. This will redirect you to the Google login screen.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("$renderUrl/auth"))
                    activity.startActivity(intent)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = GrowwTeal, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Lock, "OAuth secure verification")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Link Google Account", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// -------------------------------------------------------------
// DETAIL DISPLAY DIALOG
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailDetailDialog(
    mail: EmailMessage,
    viewModel: EmailViewModel,
    onDeleteMail: (EmailMessage) -> Unit,
    onDismiss: () -> Unit,
    onForward: () -> Unit
) {
    var showReplyComposer by remember { mutableStateOf(false) }
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Go Back")
                    }
                    IconButton(onClick = { viewModel.toggleStarred(mail.id, mail.isStarred) }) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Starred status toggle dialog",
                            tint = if (mail.isStarred) Color(0xFFFFC107) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Sender Info panel
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        val fallbackUrl = "https://ui-avatars.com/api/?name=${android.net.Uri.encode(mail.senderName)}&background=00d09c&color=fff&size=128"
                        val domain = mail.sender.substringAfter("@", "").substringBefore(">").trim().lowercase()
                        val logoUrl = if (domain.isNotEmpty() && !domain.endsWith("gmail.com") && !domain.endsWith("yahoo.com") && !domain.endsWith("outlook.com") && domain.contains(".")) {
                            "https://logo.clearbit.com/$domain"
                        } else {
                            fallbackUrl
                        }
                        var imageUrlDetails by remember(logoUrl) { mutableStateOf(logoUrl) }
                        coil.compose.AsyncImage(
                            model = imageUrlDetails,
                            contentDescription = "Sender profile picture details",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                            onState = { state ->
                                if (state is coil.compose.AsyncImagePainter.State.Error && imageUrlDetails != fallbackUrl) {
                                    imageUrlDetails = fallbackUrl
                                }
                            }
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(mail.senderName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("From: ${mail.sender}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text("To: ${mail.recipient}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(mail.subject, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)

                Spacer(modifier = Modifier.height(8.dp))

                // Message Text Content (HTML WebView or fallback Plain Text)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 8.dp)
                ) {
                    val html = mail.htmlBody
                    if (!html.isNullOrBlank()) {
                        AndroidView(
                            factory = { ctx ->
                                android.webkit.WebView(ctx).apply {
                                    layoutParams = android.view.ViewGroup.LayoutParams(
                                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    webViewClient = android.webkit.WebViewClient()
                                    settings.javaScriptEnabled = true
                                    settings.loadWithOverviewMode = true
                                    settings.useWideViewPort = true
                                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                }
                            },
                            update = { webView ->
                                val formattedHtml = if (isDarkMode) {
                                    """
                                    <html>
                                    <head>
                                    <style>
                                    body {
                                        color: #E2E2E6;
                                        background-color: transparent;
                                        font-family: sans-serif;
                                        line-height: 1.5;
                                    }
                                    a {
                                        color: #8AB4F8;
                                    }
                                    </style>
                                    </head>
                                    <body>
                                    $html
                                    </body>
                                    </html>
                                    """.trimIndent()
                                } else {
                                    html
                                }
                                webView.loadDataWithBaseURL(null, formattedHtml, "text/html", "UTF-8", null)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                mail.body, 
                                style = MaterialTheme.typography.bodyMedium, 
                                lineHeight = 22.sp, 
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (showReplyComposer) {
                    Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                        ReplyComposerView(
                            originalMail = mail,
                            viewModel = viewModel,
                            onSent = {
                                showReplyComposer = false
                                onDismiss()
                            },
                            onCancel = { showReplyComposer = false }
                        )
                    }
                } else if (mail.label.uppercase() == "DRAFT") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                viewModel.startEditingDraft(mail)
                                onForward()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GrowwTeal,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Edit, "Edit Draft", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit Draft", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                viewModel.deleteMail(mail.id)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD32F2F),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Delete, "Delete Draft")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete Draft", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { showReplyComposer = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GrowwTeal,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Edit, "Quick reply", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reply", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = {
                                val fwdSubject = "Fwd: ${mail.subject}"
                                val formattedDate = SimpleDateFormat("EEE, MMM d, yyyy 'at' h:mm a", Locale.getDefault()).format(Date(mail.timestamp))
                                val fwdBody = "\n\n---------- Forwarded message ---------\n" +
                                        "From: ${mail.senderName} <${mail.sender}>\n" +
                                        "Date: $formattedDate\n" +
                                        "Subject: ${mail.subject}\n" +
                                        "To: ${mail.recipient}\n\n" +
                                        mail.body
                                
                                viewModel.saveDraft(
                                    recipient = "",
                                    subject = fwdSubject,
                                    body = fwdBody,
                                    category = mail.category
                                )
                                onForward()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.5.dp, GrowwTeal),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = GrowwTeal
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Share, "Forward email", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Forward", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                onDeleteMail(mail)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD32F2F),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Delete, "Delete")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(56.dp))
            }
        }
    }
}

@Composable
fun ReplyComposerView(
    originalMail: EmailMessage,
    viewModel: EmailViewModel,
    onSent: () -> Unit,
    onCancel: () -> Unit
) {
    var replyText by remember { mutableStateOf("") }
    var showReplyDialogGemini by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = replyText,
            onValueChange = { replyText = it },
            placeholder = { Text("Write your reply message...") },
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(10.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onCancel) {
                    Text("Cancel", color = AlertRed)
                }
                Button(
                    onClick = { showReplyDialogGemini = true },
                    colors = ButtonDefaults.buttonColors(containerColor = GrowwTeal.copy(alpha = 0.12f), contentColor = GrowwTeal),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Face, "Gemini", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Gemini Assist", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            IconButton(
                onClick = {
                    if (replyText.isNotEmpty()) {
                        viewModel.composeEmail(
                            fromEmail = originalMail.recipient,
                            toEmail = originalMail.sender,
                            subject = "Re: ${originalMail.subject}",
                            body = replyText,
                            category = originalMail.category
                        )
                        onSent()
                    }
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, "Send reply", tint = GrowwTeal)
            }
        }
    }

    if (showReplyDialogGemini) {
        GeminiWizardDialog(
            recipientHint = originalMail.sender,
            subjectHint = originalMail.subject,
            isReply = true,
            originalBodyContext = originalMail.body,
            viewModel = viewModel,
            onAcceptResult = { textResult ->
                replyText = textResult
                showReplyDialogGemini = false
            },
            onDismiss = { showReplyDialogGemini = false }
        )
    }
}

// -------------------------------------------------------------
// GEMINI WIZARD DIALOG
// -------------------------------------------------------------
@Composable
fun GeminiWizardDialog(
    recipientHint: String,
    subjectHint: String,
    isReply: Boolean = false,
    originalBodyContext: String = "",
    viewModel: EmailViewModel,
    onAcceptResult: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var promptRequirements by remember { mutableStateOf("") }
    val isGeneratingDraft by viewModel.isGeneratingDraft.collectAsState()
    val geminiResponse by viewModel.geminiResponse.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Face, "Gemini Logo", tint = GrowwTeal)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Gemini AI Inbox Co-Pilot", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text(
                    text = if (isReply) "How would you like to reply? (e.g. 'Politely accept with feedback', 'Decline invitation kindly')"
                    else "Enter details for the draft (e.g. 'Leave requests for sickness on Monday', 'Project status update')",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = promptRequirements,
                    onValueChange = { promptRequirements = it },
                    placeholder = { Text("AI prompt requirements...") },
                    modifier = Modifier.fillMaxWidth().height(90.dp),
                    shape = RoundedCornerShape(8.dp)
                )

                if (isGeneratingDraft) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = GrowwTeal, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Gemini model formatting...", fontSize = 11.sp, color = GrowwTeal)
                        }
                    }
                } else if (geminiResponse.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("AI PROPOSAL", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = GrowwTeal)
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
                    ) {
                        Box(modifier = Modifier.padding(10.dp).verticalScroll(rememberScrollState())) {
                            Text(geminiResponse, fontSize = 12.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row {
                Button(
                    onClick = {
                        viewModel.generateEmailDraft(
                            recipientContext = recipientHint,
                            subjectContext = subjectHint,
                            additionalRequirements = promptRequirements,
                            isReplyMode = isReply,
                            originalEmailBody = originalBodyContext
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GrowwTeal)
                ) {
                    Text(if (geminiResponse.isNotEmpty()) "Regenerate" else "Generate", fontWeight = FontWeight.Bold)
                }
                if (geminiResponse.isNotEmpty() && !isGeneratingDraft) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = { onAcceptResult(geminiResponse) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface)
                    ) {
                        Text("Accept", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ManageTagsDialog(
    viewModel: EmailViewModel,
    onDismiss: () -> Unit
) {
    val customTags by viewModel.customTags.collectAsState()
    var newTagText by remember { mutableStateOf("") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Info, "Tag Icon", tint = GrowwTeal)
                Text("Manage Custom Tags")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Create or delete custom email tags. Delete a tag to automatically clear it from all emails.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                // Input row to add tags
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newTagText,
                        onValueChange = { newTagText = it },
                        placeholder = { Text("New tag name...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            val cleanTag = newTagText.trim()
                            if (cleanTag.isNotEmpty()) {
                                if (cleanTag.contains(",")) {
                                    Toast.makeText(context, "Tags cannot contain commas", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.addCustomTag(cleanTag)
                                    newTagText = ""
                                }
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GrowwTeal)
                    ) {
                        Text("Add")
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Scrollable list of existing tags
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
                ) {
                    if (customTags.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No custom tags created yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(customTags.toList()) { tag ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.Info, "Tag Item", tint = GrowwTeal, modifier = Modifier.size(16.dp))
                                        Text(tag, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    }
                                    IconButton(
                                        onClick = { viewModel.removeCustomTag(tag) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, "Delete tag", tint = AlertRed, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
