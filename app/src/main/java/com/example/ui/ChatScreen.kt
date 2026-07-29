package com.example.ui

import android.app.Activity
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.ads.AdManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.R
import com.example.data.ChatMessage
import com.example.data.ChatSession
import com.example.data.api.OpenRouterModelItem
import kotlinx.coroutines.launch
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as? Activity

    val sessions by viewModel.sessions.collectAsState()
    val activeSessionId by viewModel.activeSessionId.collectAsState()
    val messages by viewModel.activeSessionMessages.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    
    val selectedModel by viewModel.selectedModel.collectAsState()
    val selectedPersonality by viewModel.selectedPersonality.collectAsState()
    val openRouterKey by viewModel.openRouterApiKey.collectAsState()
    val customModel by viewModel.customModel.collectAsState()

    val availableModels by viewModel.availableModels.collectAsState()
    val isFetchingModels by viewModel.isFetchingModels.collectAsState()

    var showSettings by remember { mutableStateOf(false) }
    var showModelDialog by remember { mutableStateOf(false) }
    var showImageGenDialog by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // Image Picker Launcher for Vision
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "💬 AI Conversations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                Text(
                    text = "Aura Local Sessions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        viewModel.createNewSession("New Chat ${sessions.size + 1}")
                        scope.launch { drawerState.close() }
                        activity?.let { AdManager.onSessionChanged(it) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("new_chat_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Chat")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Session")
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(sessions) { session ->
                        val isSelected = session.id == activeSessionId
                        Surface(
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectSession(session.id)
                                    scope.launch { drawerState.close() }
                                    activity?.let { AdManager.onSessionChanged(it) }
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChatBubbleOutline,
                                        contentDescription = "Session",
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = session.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteSession(session.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                Surface(
                    shadowElevation = 4.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                    ) {
                        // Top Header Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Menu Drawer")
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Aura AI Client",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Image Gen Quick Action
                                IconButton(onClick = { showImageGenDialog = true }) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = "Generate Image", tint = MaterialTheme.colorScheme.primary)
                                }

                                // Settings Action Button
                                IconButton(onClick = { showSettings = true }) {
                                    BadgedBox(
                                        badge = {
                                            if (openRouterKey.isNotBlank()) {
                                                Badge(containerColor = Color(0xFF4CAF50)) {
                                                    Icon(Icons.Default.Check, "Connected", modifier = Modifier.size(8.dp))
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                                    }
                                }
                            }
                        }

                        // Selectors (OpenRouter Models + Agent Personality)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Model button selector opening full model dialog
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1.3f)
                                    .clickable { showModelDialog = true }
                                    .padding(vertical = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    val shortModelName = selectedModel.split("/").lastOrNull() ?: selectedModel
                                    Text(
                                        text = "⚡ $shortModelName",
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Expand", modifier = Modifier.size(16.dp))
                                }
                            }

                            // Personality dropdown selector
                            var personalityExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { personalityExpanded = true }
                                        .padding(vertical = 2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "🤖 ${selectedPersonality.displayName}",
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Icon(
                                            Icons.Default.ArrowDropDown, 
                                            contentDescription = "Expand", 
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                DropdownMenu(
                                    expanded = personalityExpanded,
                                    onDismissRequest = { personalityExpanded = false }
                                ) {
                                    ChatViewModel.Personality.values().forEach { pers ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(pers.displayName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Text(pers.description, fontSize = 11.sp, color = Color.Gray)
                                                }
                                            },
                                            onClick = {
                                                viewModel.selectPersonality(pers)
                                                personalityExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                Surface(
                    tonalElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .imePadding()
                        .navigationBarsPadding()
                ) {
                    Column {
                        // Image Attachment Preview Box
                        selectedImageUri?.let { uri ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = "Selected Photo",
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "🖼️ Attached Photo for Vision Analysis",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                IconButton(onClick = { selectedImageUri = null }) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove Photo", tint = Color.Red)
                                }
                            }
                        }

                        // AI Loading Indicator
                        AnimatedVisibility(
                            visible = isAiLoading,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                                    .padding(vertical = 8.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "AI is thinking and generating... 🧠",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Message Input Field Row
                        var textInput by remember { mutableStateOf("") }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Pick Image / Vision Attachment
                            IconButton(
                                onClick = { imagePickerLauncher.launch("image/*") }
                            ) {
                                Icon(
                                    Icons.Default.AddPhotoAlternate, 
                                    contentDescription = "Attach Image", 
                                    tint = if (selectedImageUri != null) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                            }

                            TextField(
                                value = textInput,
                                onValueChange = { textInput = it },
                                placeholder = { Text("Type a message or ask AI...") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chat_input"),
                                shape = RoundedCornerShape(24.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent
                                ),
                                maxLines = 4
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            IconButton(
                                onClick = {
                                    if (textInput.isNotBlank() || selectedImageUri != null) {
                                        var base64Image: String? = null
                                        selectedImageUri?.let { uri ->
                                            base64Image = uriToBase64(context, uri)
                                        }

                                        viewModel.sendMessage(textInput, base64Image)
                                        textInput = ""
                                        selectedImageUri = null
                                        activity?.let { AdManager.onUserAction(it) }
                                    }
                                },
                                enabled = (textInput.isNotBlank() || selectedImageUri != null) && !isAiLoading,
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("send_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            val listState = rememberLazyListState()

            // Auto-scroll to bottom when new messages arrive
            LaunchedEffect(messages.size, isAiLoading) {
                if (messages.isNotEmpty()) {
                    try {
                        listState.animateScrollToItem(messages.size - 1)
                    } catch (_: Throwable) { }
                }
            }

            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (messages.isEmpty()) {
                    // Welcome Screen Layout
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(110.dp),
                            shadowElevation = 6.dp
                        ) {
                            AsyncImage(
                                model = R.drawable.aura_hero_logo_1784436203588,
                                contentDescription = "Aura AI Logo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Welcome to Aura AI Chat!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "All-in-one AI Assistant with Vision OCR, Deep Reasoning, Image Generation, and Exports.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "💡 Quick Features & Usage:",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("• Tap 🖼️ to attach photos for Vision & OCR analysis.", style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("• Select 'DeepSeek R1' for step-by-step reasoning thoughts.", style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("• Use 🎨 button to generate high-resolution AI art.", style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("• Export formatted tables directly to Excel CSV with one tap.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(messages) { message ->
                            MessageBubbleItem(message = message)
                        }
                    }
                }
            }
        }
    }

    // OpenRouter All Models Selection Dialog
    if (showModelDialog) {
        OpenRouterModelSelectionDialog(
            availableModels = availableModels,
            selectedModel = selectedModel,
            isFetching = isFetchingModels,
            onSelectModel = { modelId ->
                viewModel.selectModel(modelId)
                showModelDialog = false
            },
            onRefresh = { viewModel.fetchOpenRouterModels() },
            onDismiss = { showModelDialog = false }
        )
    }

    // Image Generation Dialog
    if (showImageGenDialog) {
        var imagePrompt by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showImageGenDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "🎨 AI Image Generation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Enter a detailed prompt to generate a high-quality image (FLUX.1):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = imagePrompt,
                        onValueChange = { imagePrompt = it },
                        label = { Text("Image Description (Prompt)") },
                        placeholder = { Text("e.g., Futuristic cyberpunk car in Tokyo neon lights...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showImageGenDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (imagePrompt.isNotBlank()) {
                                    viewModel.generateImage(imagePrompt)
                                    showImageGenDialog = false
                                }
                            },
                            enabled = imagePrompt.isNotBlank()
                        ) {
                            Text("Generate")
                        }
                    }
                }
            }
        }
    }

    // Settings & OpenRouter Key Dialog
    if (showSettings) {
        Dialog(onDismissRequest = { showSettings = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "⚙️ OpenRouter API Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Enter your OpenRouter API Key to unlock all available free & premium AI models.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    var tempOpenRouterKey by remember { mutableStateOf(openRouterKey) }
                    var tempCustomModel by remember { mutableStateOf(customModel) }

                    OutlinedTextField(
                        value = tempOpenRouterKey,
                        onValueChange = { tempOpenRouterKey = it },
                        label = { Text("OpenRouter API Key") },
                        placeholder = { Text("sk-or-v1-...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            if (tempOpenRouterKey.isNotBlank()) {
                                Icon(Icons.Default.Check, "Configured", tint = Color(0xFF4CAF50))
                            }
                        }
                    )

                    OutlinedTextField(
                        value = tempCustomModel,
                        onValueChange = { tempCustomModel = it },
                        label = { Text("Custom Model ID (Optional)") },
                        placeholder = { Text("e.g., deepseek/deepseek-r1:free") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showSettings = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.saveOpenRouterApiKey(tempOpenRouterKey)
                                viewModel.saveCustomModel(tempCustomModel)
                                if (tempCustomModel.isNotBlank()) {
                                    viewModel.selectModel(tempCustomModel)
                                }
                                showSettings = false
                                Toast.makeText(context, "Settings saved successfully!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

// Convert Uri to Base64 String for Vision Processing
fun uriToBase64(context: android.content.Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        if (bitmap == null) return null

        // Downscale bitmap if larger than 1024x1024 to prevent OutOfMemory crashes and keep payloads fast
        val maxDimension = 1024
        val scaledBitmap = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
            val ratio = Math.min(
                maxDimension.toFloat() / bitmap.width,
                maxDimension.toFloat() / bitmap.height
            )
            val width = Math.round(ratio * bitmap.width)
            val height = Math.round(ratio * bitmap.height)
            android.graphics.Bitmap.createScaledBitmap(bitmap, width, height, true)
        } else {
            bitmap
        }

        val outputStream = java.io.ByteArrayOutputStream()
        scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
        val byteArray = outputStream.toByteArray()
        "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
    } catch (t: Throwable) {
        android.util.Log.e("ChatScreen", "Error converting image to Base64 safely", t)
        null
    }
}

// Full-featured OpenRouter Models Dialog with Search, Live API Sync, and Filter Categories
@Composable
fun OpenRouterModelSelectionDialog(
    availableModels: List<OpenRouterModelItem>,
    selectedModel: String,
    isFetching: Boolean,
    onSelectModel: (String) -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterTab by remember { mutableStateOf(0) } // 0: الكل, 1: مجاني, 2: صور/Vision, 3: تفكير/Thinking

    // Live API fetch on dialog opening
    LaunchedEffect(Unit) {
        if (availableModels.isEmpty()) {
            onRefresh()
        }
    }

    val displayList = availableModels

    val filteredModels = remember(displayList, searchQuery, selectedFilterTab) {
        displayList.filter { item ->
            val matchesQuery = searchQuery.isBlank() || 
                item.id.contains(searchQuery, ignoreCase = true) || 
                (item.name ?: "").contains(searchQuery, ignoreCase = true)

            val matchesTab = when (selectedFilterTab) {
                1 -> item.id.contains(":free") || (item.pricing?.prompt == "0") // Free
                2 -> item.id.contains("vision") || item.id.contains("vl") || item.id.contains("gemini") || item.id.contains("claude") // Vision
                3 -> item.id.contains("r1") || item.id.contains("think") || item.id.contains("reasoning") || item.id.contains("qwq") || item.id.contains("o1") || item.id.contains("o3") // Thinking
                else -> true
            }

            matchesQuery && matchesTab
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "🤖 Live OpenRouter Models",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Live Sync API",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4CAF50),
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh live list")
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search model name or type custom model ID...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )

                // Option to use custom typed model ID directly
                if (searchQuery.isNotBlank() && filteredModels.none { it.id.equals(searchQuery.trim(), ignoreCase = true) }) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectModel(searchQuery.trim())
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Use Model", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Use Custom Model: \"${searchQuery.trim()}\"",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Category Filter Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedFilterTab,
                    edgePadding = 0.dp,
                    divider = {}
                ) {
                    Tab(selected = selectedFilterTab == 0, onClick = { selectedFilterTab = 0 }) {
                        Text("All (${displayList.size})", modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp), fontSize = 12.sp)
                    }
                    Tab(selected = selectedFilterTab == 1, onClick = { selectedFilterTab = 1 }) {
                        Text("🆓 Free", modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp), fontSize = 12.sp)
                    }
                    Tab(selected = selectedFilterTab == 2, onClick = { selectedFilterTab = 2 }) {
                        Text("👁️ Vision", modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp), fontSize = 12.sp)
                    }
                    Tab(selected = selectedFilterTab == 3, onClick = { selectedFilterTab = 3 }) {
                        Text("💡 Reasoning", modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp), fontSize = 12.sp)
                    }
                }

                if (isFetching && availableModels.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Fetching live models from OpenRouter API...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredModels) { model ->
                            val isSelected = model.id == selectedModel
                            val isFree = model.id.contains(":free") || (model.pricing?.prompt == "0")

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = if (isSelected) CardDefaults.outlinedCardBorder() else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectModel(model.id) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = model.name ?: model.id,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = model.id,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (isFree) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF4CAF50).copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "FREE",
                                                color = Color(0xFF2E7D32),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubbleItem(message: ChatMessage) {
    val isUser = message.role == "user"
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showSlidePreview by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        // Role Label / Model Name Indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Icon(
                imageVector = if (isUser) Icons.Default.Person else Icons.Default.SmartToy,
                contentDescription = message.role,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isUser) "You" else (message.modelName.ifBlank { "Aura Agent" }),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        // Deep Thinking Accordion Card (If Reasoning exists)
        if (!message.reasoningContent.isNullOrBlank()) {
            var expandedThinking by remember { mutableStateOf(false) }
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedThinking = !expandedThinking },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lightbulb, contentDescription = "Thought", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "💡 Thought Process",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Icon(
                            imageVector = if (expandedThinking) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle",
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (expandedThinking) {
                        Spacer(modifier = Modifier.height(6.dp))
                        SelectionContainer {
                            Text(
                                text = message.reasoningContent,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Image Attachment Preview inside Chat
        if (!message.imageUri.isNullOrBlank()) {
            AsyncImage(
                model = message.imageUri,
                contentDescription = "Attached Photo",
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .heightIn(max = 220.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .padding(vertical = 4.dp),
                contentScale = ContentScale.Crop
            )
        }

        // Message Card Bubble
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = bubbleColor,
                contentColor = contentColor
            ),
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                SelectionContainer {
                    MarkdownContent(text = message.content)
                }
            }
        }

        // Interactive Artifacts Exporter Toolbar (Excel / Slides)
        if (!isUser && message.content.isNotBlank()) {
            val hasTable = message.content.contains("|") && message.content.contains("\n")
            val hasSlides = message.content.contains("---") || message.content.contains("Slide") || message.content.contains("عرض")

            if (hasTable || hasSlides) {
                Row(
                    modifier = Modifier
                        .widthIn(max = 310.dp)
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (hasTable) {
                        OutlinedButton(
                            onClick = {
                                val csvData = extractTableToCsv(message.content)
                                clipboardManager.setText(AnnotatedString(csvData))
                                Toast.makeText(context, "Copied Excel CSV table to clipboard!", Toast.LENGTH_LONG).show()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("📊 Export Excel", fontSize = 10.sp)
                        }
                    }

                    if (hasSlides) {
                        OutlinedButton(
                            onClick = { showSlidePreview = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("📽️ View Slides", fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // Stats Display Row for AI Responses (Latency, t/s, prompt, completion, reasoning)
        if (!isUser && message.latencyMs > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            
            val latencySeconds = message.latencyMs / 1000.0
            val totalTokens = message.promptTokens + message.completionTokens
            val speed = if (latencySeconds > 0) (totalTokens / latencySeconds).toInt() else 0

            Row(
                modifier = Modifier
                    .widthIn(max = 310.dp)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PerformanceBadge(text = "⏱️ ${String.format(java.util.Locale.US, "%.2f", latencySeconds)}s")
                if (speed > 0) {
                    PerformanceBadge(text = "⚡ $speed t/s")
                }
                if (message.promptTokens > 0) PerformanceBadge(text = "📥 ${message.promptTokens} p")
                if (message.completionTokens > 0) PerformanceBadge(text = "📤 ${message.completionTokens} c")
            }
        }
    }

    // Slides Presentation Dialog Preview
    if (showSlidePreview) {
        val slides = remember(message.content) {
            message.content.split("---").filter { it.isNotBlank() }
        }
        var currentSlideIndex by remember { mutableStateOf(0) }

        Dialog(onDismissRequest = { showSlidePreview = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "📽️ Presentation (Slide ${currentSlideIndex + 1} of ${slides.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            MarkdownContent(text = slides.getOrElse(currentSlideIndex) { message.content })
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { if (currentSlideIndex > 0) currentSlideIndex-- },
                            enabled = currentSlideIndex > 0
                        ) {
                            Text("Previous")
                        }

                        Button(
                            onClick = { if (currentSlideIndex < slides.size - 1) currentSlideIndex++ },
                            enabled = currentSlideIndex < slides.size - 1
                        ) {
                            Text("Next")
                        }

                        TextButton(onClick = { showSlidePreview = false }) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}

// Convert markdown table text into clean CSV string for Excel export
fun extractTableToCsv(markdown: String): String {
    val lines = markdown.lines().filter { it.contains("|") }
    return lines.mapNotNull { line ->
        val parts = line.split("|").map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.all { it.contains("-") }) null // Skip separator row
        else parts.joinToString(",")
    }.joinToString("\n")
}

@Composable
fun PerformanceBadge(text: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun MarkdownContent(text: String) {
    val parts = text.split("```")
    
    parts.forEachIndexed { index, part ->
        if (index % 2 == 1) {
            val lines = part.trim().split("\n")
            val language = lines.firstOrNull()?.trim() ?: "code"
            val code = if (lines.size > 1) {
                lines.drop(1).joinToString("\n")
            } else {
                part
            }
            CodeBlock(code = code, language = language)
        } else {
            if (part.isNotBlank()) {
                val formattedText = parseBoldText(part)
                Text(
                    text = formattedText,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun CodeBlock(code: String, language: String) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E),
            contentColor = Color(0xFFD4D4D4)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2D2D2D))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language.uppercase(),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9CDCFE)
                )
                
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(code))
                        Toast.makeText(context, "Code copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy, 
                        contentDescription = "Copy Code", 
                        tint = Color.LightGray,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = code,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

fun parseBoldText(input: String): AnnotatedString {
    val builder = AnnotatedString.Builder()
    val parts = input.split("**")
    
    parts.forEachIndexed { index, part ->
        if (index % 2 == 1) {
            builder.pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold))
            builder.append(part)
            builder.pop()
        } else {
            builder.append(part)
        }
    }
    return builder.toAnnotatedString()
}
