package com.example.ui

import android.net.Uri
import android.util.Log
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.database.MemoryEntity
import com.example.data.database.ProfileEntity
import com.example.ui.components.FuturisticBackground
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(
    viewModel: AssistantViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State bindings
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val speechText by viewModel.speechText.collectAsState()
    val voiceFeedbackMessage by viewModel.voiceFeedbackMessage.collectAsState()
    val sttError by viewModel.sttError.collectAsState()
    val isAutoWakeActive by viewModel.isAutoWakeActive.collectAsState()

    val memories by viewModel.memories.collectAsState()
    val profile by viewModel.userProfile.collectAsState()
    val backgroundVideoPath by viewModel.backgroundVideoPath.collectAsState()

    // UI Panel states
    val customApiKey by viewModel.customApiKey.collectAsState()
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showMemoryDialog by remember { mutableStateOf(false) }
    var showVideoDialog by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.saveBackgroundVideo(uri, context)
        }
    }

    var hasAudioPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasAudioPermission = isGranted
            if (isGranted) {
                if (viewModel.isAutoWakeActive.value) {
                    viewModel.startListening()
                } else {
                    viewModel.startListening()
                }
            } else {
                android.widget.Toast.makeText(
                    context,
                    "Mikrofon-Berechtigung wird für die Spracherkennung benötigt.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    )

    val listState = rememberLazyListState()

    // Auto-scroll to bottom on new messages
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF030712), // Very dark space slate
            Color(0xFF0F172A), // Dark slate blue
            Color(0xFF020617)  // Deep cosmic void
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        // 1. Background Video with animated canvas fallback
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            mp.setVolume(0f, 0f) // Mute background loop
                            start()
                        }
                        setOnErrorListener { _, _, _ ->
                            // Fail silently, letting the Compose FuturisticBackground render underneath
                            true
                        }
                    }
                },
                update = { videoView ->
                    val pathOrUrl = backgroundVideoPath
                    val currentTag = videoView.tag as? String
                    val newTag = pathOrUrl ?: "https://assets.mixkit.co/videos/preview/mixkit-abstract-digital-futuristic-technology-loop-41865-large.mp4"
                    if (currentTag != newTag) {
                        videoView.tag = newTag
                        try {
                            videoView.stopPlayback()
                            if (pathOrUrl != null) {
                                videoView.setVideoPath(pathOrUrl)
                            } else {
                                videoView.setVideoURI(Uri.parse(newTag))
                            }
                        } catch (e: Exception) {
                            Log.e("AssistantScreen", "Error updating video source: ", e)
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Render our gorgeous custom Canvas fallback underneath/overlaid with high transparency
            FuturisticBackground(
                modifier = Modifier.fillMaxSize(),
                isListening = isListening,
                isSpeaking = isSpeaking
            )

            // Semi-transparent dark overlay to ensure readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x99020617), // Deep dark translucent
                                Color(0xB30F172A),
                                Color(0xCC020617)
                            )
                        )
                    )
            )
        }

        // 2. Main Scaffold UI
        Scaffold(
            containerColor = Color.Transparent, // Transparent scaffold to show the background
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color(0x4D0F172A), // Soft glassmorphic background
                        titleContentColor = Color.White
                    ),
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "ERNA",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.SansSerif,
                                    letterSpacing = 6.sp,
                                    color = Color(0xFF22D3EE) // Bright Cyan
                                )
                            )
                            Text(
                                text = if (isAutoWakeActive) "Modus: Hey Erna (Aktiv)" else "Modus: Push-to-Talk",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isAutoWakeActive) Color(0xFF34D399) else Color(0xFF94A3B8),
                                    fontSize = 10.sp,
                                    letterSpacing = 1.sp
                                )
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { showProfileDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Nutzerprofil",
                                tint = Color(0xFF22D3EE)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showApiKeyDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "API-Schlüssel erneuern",
                                tint = Color(0xFFFFB020) // Golden / Yellow accent
                            )
                        }
                        IconButton(onClick = { showVideoDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "Hintergrund-Video ändern",
                                tint = Color(0xFF10B981) // Green / Emerald accent
                            )
                        }
                        IconButton(onClick = { showMemoryDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = "Erna's Gedächtnis",
                                tint = Color(0xFF818CF8) // Warm indigo
                            )
                        }
                        IconButton(onClick = { viewModel.clearChat() }) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Chat leeren",
                                tint = Color(0xFFF87171) // Red accent
                            )
                        }
                    },
                    modifier = Modifier.border(0.5.dp, Color(0x1F22D3EE), RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding() // Automatically resize when keyboard is shown
            ) {
                // Active Voice Recognition Error Notification
                sttError?.let { error ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xCC7F1D1D)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(8.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Error, "Fehler", tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(error, color = Color.White, fontSize = 12.sp)
                        }
                    }
                }

                // Chat Messages List
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(chatMessages) { msg ->
                        ChatBubble(message = msg)
                    }
                }

                // Voice status/partial speech live preview
                if (isListening || isProcessing || isSpeaking) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x3306B6D4))
                            .border(0.5.dp, Color(0xFF22D3EE), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            if (isListening) Color(0xFFEF4444) else if (isProcessing) Color(
                                                0xFFF59E0B
                                            ) else Color(0xFF3B82F6),
                                            CircleShape
                                        )
                                )
                                Text(
                                    text = if (isListening) "Erna hört zu..." else if (isProcessing) "Erna denkt nach..." else "Erna spricht...",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isListening) {
                                    if (speechText.isBlank()) "Spreche jetzt..." else "\"$speechText\""
                                } else if (isProcessing) {
                                    "Einen Augenblick bitte..."
                                } else {
                                    "Sprachausgabe aktiv."
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color(0xFFE2E8F0),
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                    }
                }

                // Bottom controls panel (Voice, Text input, wake-word activation)
                Surface(
                    color = Color(0x660F172A),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0x1F22D3EE), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Text input field + microphone button row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Text Input Field for quiet typing
                            OutlinedTextField(
                                value = textInput,
                                onValueChange = { textInput = it },
                                placeholder = { Text("Schreibe Erna...", color = Color(0xFF64748B)) },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF22D3EE),
                                    unfocusedBorderColor = Color(0x3322D3EE),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0x33020617),
                                    unfocusedContainerColor = Color(0x33020617)
                                ),
                                shape = RoundedCornerShape(24.dp),
                                trailingIcon = {
                                    if (textInput.isNotBlank()) {
                                        IconButton(onClick = {
                                            viewModel.askErna(textInput)
                                            // Add user text message locally
                                            viewModel.chatMessages.value // Trigger flow
                                            textInput = ""
                                        }) {
                                            Icon(Icons.Default.Send, "Senden", tint = Color(0xFF22D3EE))
                                        }
                                    }
                                }
                            )

                            // Large Futuristic Voice Button
                            IconButton(
                                onClick = {
                                    if (hasAudioPermission) {
                                        if (isListening) {
                                            viewModel.stopListening()
                                        } else {
                                            viewModel.startListening()
                                        }
                                    } else {
                                        permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = if (isListening) listOf(
                                                Color(0xFFEF4444),
                                                Color(0xFFB91C1C)
                                            ) else listOf(Color(0xFF06B6D4), Color(0xFF3B82F6))
                                        ),
                                        shape = CircleShape
                                    )
                                    .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = "Sprachsteuerung",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Voice Mode & Alexa Auto Wake option
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    if (hasAudioPermission) {
                                        val newActive = !isAutoWakeActive
                                        viewModel.isAutoWakeActive.value = newActive
                                        if (newActive) {
                                            viewModel.startListening()
                                        } else {
                                            viewModel.stopListening()
                                        }
                                    } else {
                                        viewModel.isAutoWakeActive.value = true
                                        permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            ) {
                                Switch(
                                    checked = isAutoWakeActive,
                                    onCheckedChange = { active ->
                                        if (hasAudioPermission) {
                                            viewModel.isAutoWakeActive.value = active
                                            if (active) {
                                                viewModel.startListening()
                                            } else {
                                                viewModel.stopListening()
                                            }
                                        } else {
                                            if (active) {
                                                viewModel.isAutoWakeActive.value = true
                                                permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                            } else {
                                                viewModel.isAutoWakeActive.value = false
                                                viewModel.stopListening()
                                            }
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF10B981),
                                        checkedTrackColor = Color(0x3310B981)
                                    ),
                                    modifier = Modifier.scale(0.8f)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "„Hey Erna“ Aktivierung",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Erna hört kontinuierlich auf ihren Namen",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            // Model information overlay
                            Text(
                                text = "S26 Ultra AI Engine",
                                color = Color(0xFF64748B),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. User Profile Setup Sheet / Dialog
        if (showProfileDialog) {
            ProfileConfigDialog(
                profile = profile,
                onDismiss = { showProfileDialog = false },
                onSave = { name, job, address, info ->
                    viewModel.updateProfile(name, job, address, info)
                    showProfileDialog = false
                }
            )
        }

        // 4. Memory Bank Sheet / Dialog
        if (showMemoryDialog) {
            MemoryBankDialog(
                memories = memories,
                onDismiss = { showMemoryDialog = false },
                onDelete = { id -> viewModel.deleteMemory(id) },
                onAddManual = { text -> viewModel.addMemory(text, "note") }
            )
        }

        // 5. Video Background Config Dialog
        if (showVideoDialog) {
            VideoBackgroundDialog(
                onDismiss = { showVideoDialog = false },
                onSelectVideo = {
                    videoPickerLauncher.launch("video/*")
                    showVideoDialog = false
                },
                onReset = {
                    viewModel.resetBackgroundVideo(context)
                    showVideoDialog = false
                }
            )
        }

        // 6. Gemini API Key Dialog
        if (showApiKeyDialog) {
            ApiKeyConfigDialog(
                currentApiKey = customApiKey,
                onDismiss = { showApiKeyDialog = false },
                onSave = { newKey ->
                    viewModel.saveApiKey(newKey)
                    showApiKeyDialog = false
                },
                onReset = {
                    viewModel.resetApiKey()
                    showApiKeyDialog = false
                }
            )
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val bubbleColor = if (message.isUser) Color(0x223B82F6) else Color(0x221E293B)
    val borderColor = if (message.isUser) Color(0x663B82F6) else Color(0x6606B6D4)
    val textColor = if (message.isUser) Color(0xFFEFF6FF) else Color(0xFFF1F5F9)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        Row(
            horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!message.isUser) {
                // Erna's Holographic Avatar
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF06B6D4), Color(0xFF8B5CF6))),
                            CircleShape
                        )
                        .border(1.5.dp, Color(0xFF22D3EE), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Face2,
                        contentDescription = "Erna Avatar",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Message Bubble Text
            Card(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (message.isUser) 16.dp else 4.dp,
                    bottomEnd = if (message.isUser) 4.dp else 16.dp
                ),
                colors = CardDefaults.cardColors(containerColor = bubbleColor),
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .border(1.dp, borderColor, RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isUser) 16.dp else 4.dp,
                        bottomEnd = if (message.isUser) 4.dp else 16.dp
                    ))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = message.text,
                        color = textColor,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }

            if (message.isUser) {
                Spacer(modifier = Modifier.width(8.dp))
                // User Avatar placeholder
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF334155), CircleShape)
                        .border(1.dp, Color(0xFF64748B), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "User Avatar",
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// Dialog to configure User Profile
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileConfigDialog(
    profile: ProfileEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(profile?.name ?: "") }
    var job by remember { mutableStateOf(profile?.occupation ?: "") }
    var address by remember { mutableStateOf(profile?.address ?: "") }
    var info by remember { mutableStateOf(profile?.additionalInfo ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF22D3EE), RoundedCornerShape(24.dp))
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Dein S26 Ultra Profile",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color(0xFF22D3EE),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Diese Daten werden absolut sicher und lokal auf deinem Gerät gespeichert. Erna nutzt sie, um deine Fragen personalisiert zu beantworten.",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Dein Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF22D3EE),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = job,
                    onValueChange = { job = it },
                    label = { Text("Dein Beruf") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF22D3EE),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Deine Adresse / Ort") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF22D3EE),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = info,
                    onValueChange = { info = it },
                    label = { Text("Zusätzliche Infos") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF22D3EE),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                        Text("Abbrechen")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(name, job, address, info) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4))
                    ) {
                        Text("Speichern", color = Color.White)
                    }
                }
            }
        }
    }
}

// Dialog to view and manage Local memories
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryBankDialog(
    memories: List<MemoryEntity>,
    onDismiss: () -> Unit,
    onDelete: (Long) -> Unit,
    onAddManual: (String) -> Unit
) {
    var newMemoryText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .border(1.dp, Color(0xFF818CF8), RoundedCornerShape(24.dp))
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxSize()
            ) {
                Text(
                    text = "Ernas Gedächtnis",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color(0xFF818CF8),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Hier siehst du alle Fakten, Erlebnisse und Notizen, die Erna über dich weiß. Du kannst manuell etwas hinzufügen oder Einträge löschen.",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Manual memory entry fields
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newMemoryText,
                        onValueChange = { newMemoryText = it },
                        placeholder = { Text("Z.B. Brille liegt im Flur...", color = Color(0xFF64748B), fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF818CF8),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    IconButton(
                        onClick = {
                            if (newMemoryText.isNotBlank()) {
                                onAddManual(newMemoryText)
                                newMemoryText = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFF818CF8), CircleShape)
                    ) {
                        Icon(Icons.Default.Save, "Hinzufügen", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable memories list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (memories.isEmpty()) {
                        item {
                            Text(
                                "Keine Notizen oder Erlebnisse gespeichert. Sprich einfach mit Erna und bitte sie, sich etwas zu merken!",
                                color = Color(0xFF64748B),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp)
                            )
                        }
                    } else {
                        items(memories) { memo ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0x331E293B)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(0.5.dp, Color(0x33818CF8), RoundedCornerShape(12.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(memo.content, color = Color.White, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY)
                                                .format(java.util.Date(memo.timestamp)),
                                            color = Color(0xFF64748B),
                                            fontSize = 9.sp
                                        )
                                    }
                                    IconButton(onClick = { onDelete(memo.id) }) {
                                        Icon(Icons.Default.Delete, "Löschen", tint = Color(0xFFF87171))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Schließen")
                }
            }
        }
    }
}

// Dialog to configure Video Background
@Composable
fun VideoBackgroundDialog(
    onDismiss: () -> Unit,
    onSelectVideo: () -> Unit,
    onReset: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF10B981), RoundedCornerShape(24.dp))
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.VideoLibrary,
                    contentDescription = "Video Hintergrund",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(48.dp)
                )

                Text(
                    text = "Live-Hintergrund einrichten",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Wähle das hochgeladene Video der Roboter-Frau (oder ein beliebiges anderes Video) aus deiner Galerie aus, damit es als interaktiver Hintergrund für Erna dient. Die App kopiert das Video sicher lokal auf dein S26 Ultra.",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onSelectVideo,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Movie, "Galerie", tint = Color.White)
                        Text("Video aus Galerie wählen", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = onReset,
                    border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Refresh, "Zurücksetzen", tint = Color.White)
                        Text("Standard-Hintergrund wiederherstellen", color = Color.White)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                    ) {
                        Text("Abbrechen")
                    }
                }
            }
        }
    }
}

// Dialog to configure Gemini API Key
@Composable
fun ApiKeyConfigDialog(
    currentApiKey: String?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onReset: () -> Unit
) {
    var keyInput by remember { mutableStateOf(currentApiKey ?: "") }
    var showPassword by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFFFB020), RoundedCornerShape(24.dp))
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = "API-Schlüssel",
                    tint = Color(0xFFFFB020),
                    modifier = Modifier.size(48.dp)
                )

                Text(
                    text = "Gemini API-Schlüssel",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color(0xFFFFB020),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Hier kannst du deinen Gemini API-Schlüssel eingeben oder erneuern. Der Schlüssel wird sicher lokal auf deinem S26 Ultra gespeichert. Falls das Feld leer ist, wird der in der App eingebaute Standard-Schlüssel verwendet.",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    label = { Text("Gemini API-Schlüssel", color = Color(0xFF94A3B8)) },
                    placeholder = { Text("AIzaSy...", color = Color(0x66FFFFFF)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFFB020),
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedLabelColor = Color(0xFFFFB020),
                        unfocusedLabelColor = Color(0xFF94A3B8)
                    ),
                    visualTransformation = if (showPassword) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    trailingIcon = {
                        val icon = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(imageVector = icon, contentDescription = "Sichtbarkeit umschalten", tint = Color(0xFF94A3B8))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = { onSave(keyInput) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB020)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Schlüssel speichern", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                }

                if (currentApiKey != null) {
                    OutlinedButton(
                        onClick = onReset,
                        border = BorderStroke(1.dp, Color(0xFFF87171)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF87171)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Delete, "Löschen", tint = Color(0xFFF87171))
                            Text("Eigenen Schlüssel löschen", color = Color(0xFFF87171))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                    ) {
                        Text("Abbrechen")
                    }
                }
            }
        }
    }
}

