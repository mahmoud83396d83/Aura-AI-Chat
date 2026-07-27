package com.example.ui

import android.app.Activity
import android.widget.Toast
import com.example.ads.AdManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.data.ChatMessage
import com.example.data.ChatSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val geminiKey by viewModel.geminiApiKey.collectAsState()
    val openRouterKey by viewModel.openRouterApiKey.collectAsState()
    val customModel by viewModel.customModel.collectAsState()

    var showSettings by remember { mutableStateOf(false) }

    // Models available in the selector
    val availableModels = remember(customModel) {
        val baseList = mutableListOf(
            "gemini-2.5-flash" to "♊ Gemini 2.5 Flash (Recommended)",
            "gemini-2.0-flash" to "♊ Gemini 2.0 Flash (Fast Direct)",
            "gemini-1.5-flash" to "♊ Gemini 1.5 Flash (Direct)",
            "deepseek/deepseek-r1:free" to "🧠 DeepSeek R1 (OpenRouter)",
            "meta-llama/llama-3.3-70b-instruct:free" to "🦙 Llama 3.3 70B (OpenRouter)",
            "openrouter/free" to "⚡ Auto Free Agent (OpenRouter)"
        )
        if (customModel.isNotBlank()) {
            baseList.add(0, customModel to "⚙️ Custom: $customModel")
        }
        baseList
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "💬 Chat Sessions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                Text(
                    text = "Aura local history",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Add new chat button
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Chat")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Chat")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(sessions) { session ->
                        val isSelected = session.id == activeSessionId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    viewModel.selectSession(session.id)
                                    scope.launch { drawerState.close() }
                                    activity?.let { AdManager.onSessionChanged(it) }
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.ChatBubble else Icons.Default.ChatBubbleOutline,
                                    contentDescription = "Session",
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = session.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }
                            
                            if (sessions.size > 1) {
                                IconButton(
                                    onClick = { viewModel.deleteSession(session.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete Chat",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
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
            topBar = {
                Surface(
                    tonalElevation = 4.dp,
                    shadowElevation = 2.dp
                ) {
                    Column {
                        // Top Header (Menu, Name, Settings)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Drawer Menu")
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.SmartToy,
                                    contentDescription = "AI Agent",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Aura AI Chat",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                    Text(
                                        text = "Intelligent Multitask Assistant",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }

                            IconButton(
                                onClick = { showSettings = true },
                                modifier = Modifier.testTag("settings_button")
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (geminiKey.isBlank() && openRouterKey.isBlank()) {
                                            Badge(containerColor = MaterialTheme.colorScheme.error) {
                                                Text("!")
                                            }
                                        } else {
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

                        // Selectors (Model + Agent Personality)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Model dropdown selector
                            var modelExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1.2f)) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { modelExpanded = true }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.background(Color.Transparent)
                                    ) {
                                        val displayModel = availableModels.find { it.first == selectedModel }?.second ?: selectedModel
                                        Text(
                                            text = displayModel,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Expand", modifier = Modifier.size(16.dp))
                                    }
                                }
                                DropdownMenu(
                                    expanded = modelExpanded,
                                    onDismissRequest = { modelExpanded = false }
                                ) {
                                    availableModels.forEach { (modelId, label) ->
                                        DropdownMenuItem(
                                            text = { Text(label, fontSize = 13.sp) },
                                            onClick = {
                                                viewModel.selectModel(modelId)
                                                modelExpanded = false
                                            }
                                        )
                                    }
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
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.background(Color.Transparent)
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
                    modifier = Modifier.imePadding()
                ) {
                    Column {
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
                                    text = "Aura is thinking... 🧠",
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
                                .navigationBarsPadding()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.clearHistory() },
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Clear Chat History")
                            }

                            TextField(
                                value = textInput,
                                onValueChange = { textInput = it },
                                placeholder = { Text("Type a message...") },
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

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = {
                                    if (textInput.isNotBlank()) {
                                        viewModel.sendMessage(textInput)
                                        textInput = ""
                                        activity?.let { AdManager.onUserAction(it) }
                                    }
                                },
                                enabled = textInput.isNotBlank() && !isAiLoading,
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("send_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    modifier = Modifier.size(20.dp)
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
                    listState.animateScrollToItem(messages.size - 1)
                }
            }

            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (messages.isEmpty()) {
                    // Empty Welcome Layout
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
                            modifier = Modifier.size(120.dp),
                            shadowElevation = 6.dp
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.aura_hero_logo_1784436203588),
                                contentDescription = "Aura AI Logo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
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
                            text = "A hyper-capable AI client and multi-agent assistant with full reasoning visualization.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "💡 Getting Started Tips:",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("• Switch the agent personality to 'Coding Agent' for specialized Kotlin, Java, or Python help.", style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("• Select 'DeepSeek R1' from the top dropdown to see complete thought chains and reasoning tokens.", style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("• Click the ⚙️ Settings icon to customize your local API keys for maximum speed.", style = MaterialTheme.typography.bodySmall)
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

    // Settings & Keys Dialog
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
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "⚙️ API Keys & Model Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Enter your custom API keys to activate high-performance queries for Gemini or OpenRouter models.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    var tempGeminiKey by remember { mutableStateOf(geminiKey) }
                    var tempOpenRouterKey by remember { mutableStateOf(openRouterKey) }
                    var tempCustomModel by remember { mutableStateOf(customModel) }

                    OutlinedTextField(
                        value = tempGeminiKey,
                        onValueChange = { tempGeminiKey = it },
                        label = { Text("Gemini API Key (Direct)") },
                        placeholder = { Text("AI Studio Key...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            if (tempGeminiKey.isNotBlank()) {
                                Icon(Icons.Default.Check, "Configured", tint = Color(0xFF4CAF50))
                            }
                        }
                    )

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
                        label = { Text("Custom Model Name") },
                        placeholder = { Text("e.g. google/gemini-2.5-pro") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            if (tempCustomModel.isNotBlank()) {
                                Icon(Icons.Default.Settings, "Custom Model", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Info, 
                                contentDescription = "Warning", 
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Security Note: API keys are securely saved locally on your device and are never shared with external services.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

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
                                viewModel.saveGeminiApiKey(tempGeminiKey)
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

        // Stats Display Row for AI Responses (Latency, t/s, prompt, completion, reasoning)
        if (!isUser && message.latencyMs > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            
            val latencySeconds = message.latencyMs / 1000.0
            val totalTokens = message.promptTokens + message.completionTokens
            val speed = if (latencySeconds > 0) (totalTokens / latencySeconds).toInt() else 0

            Column(
                modifier = Modifier
                    .widthIn(max = 310.dp)
                    .padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Performance Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PerformanceBadge(text = "⏱️ ${String.format("%.2f", latencySeconds)}s")
                    if (speed > 0) {
                        PerformanceBadge(text = "⚡ $speed t/s")
                    }
                    PerformanceBadge(text = "📥 ${message.promptTokens} p")
                    PerformanceBadge(text = "📤 ${message.completionTokens} c")
                }

                // Reasoning Tokens Display (DeepSeek R1 style)
                if (message.reasoningTokens > 0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Lightbulb, 
                                contentDescription = "Reasoning", 
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Reasoning Tokens used: ${message.reasoningTokens}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
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
    // A robust, custom markdown parser that splits text into code blocks and normal paragraphs.
    val parts = text.split("```")
    
    parts.forEachIndexed { index, part ->
        if (index % 2 == 1) {
            // This is a Code Block!
            val lines = part.trim().split("\n")
            val language = lines.firstOrNull()?.trim() ?: "code"
            val code = if (lines.size > 1) {
                lines.drop(1).joinToString("\n")
            } else {
                part
            }
            CodeBlock(code = code, language = language)
        } else {
            // Normal Text (which may contain bold text)
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
            // Code header (Language & Copy Button)
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

            // Raw Code Content
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

/**
 * Super simple bold parser. Converts **text** to annotated string styled with bold.
 */
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
