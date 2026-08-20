package com.example

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestChatScreen(requestId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val prefs = context.getSharedPreferences("qabas_prefs", Context.MODE_PRIVATE)
    val isAdmin = prefs.getBoolean("is_admin", false)
    val currentUserEmail = prefs.getString("user_email", if (isAdmin) "admin@qabas.studio" else "user@example.com") ?: "user@example.com"
    
    val request = remember { mutableStateOf(AppRequestService.getRequests(context, isDeveloper = true).find { it.id == requestId }) }
    val messages = remember { mutableStateOf(AppRequestService.getMessages(context, requestId)) }
    var newMessage by remember { mutableStateOf("") }
    
    // AI Prompt loading for developer
    var isLoadingPrompts by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(request.value?.title ?: Translator.tr("محادثة التطبيق"), fontFamily = CairoFont, fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GoldPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepSlate)
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                if (isAdmin) {
                    IconButton(
                        onClick = {
                            val progress = request.value?.progress ?: 0
                            val msg = "مرحباً عميلنا العزيز، نود طمأنتك بأن العمل على مشروعك يسير بشكل ممتاز بفضل الله. لقد أنجزنا حتى الآن $progress% من العمل المخطط له، ونحن ملتزمون بتقديم أعلى جودة ممكنة. إذا كان لديك أي استفسار، نحن هنا لخدمتك!"
                            newMessage = msg
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFF151B2B), RoundedCornerShape(24.dp))
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI Report", tint = GoldPrimary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                OutlinedTextField(
                    value = newMessage,
                    onValueChange = { newMessage = it },
                    placeholder = { Text(Translator.tr("اكتب رسالتك..."), fontFamily = CairoFont) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = Color(0xFF1E293B),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = CardSurface,
                        unfocusedContainerColor = CardSurface
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (newMessage.isNotBlank()) {
                            val msg = AppRequestService.ChatMessage(
                                requestId = requestId,
                                senderEmail = currentUserEmail,
                                isDeveloper = isAdmin,
                                message = newMessage
                            )
                            AppRequestService.sendMessage(context, msg)
                            messages.value = AppRequestService.getMessages(context, requestId)
                            newMessage = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(GoldPrimary, RoundedCornerShape(24.dp))
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = DeepSlate)
                }
            }
        },
        containerColor = DeepSlate
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Show Prompts if admin
            val currentReq = request.value
            if (isAdmin && currentReq != null) {
                val promptsText = currentReq.generatedPrompts
                if (promptsText.isNullOrBlank()) {
                    Button(
                        onClick = {
                            scope.launch {
                                isLoadingPrompts = true
                                val prompts = AppRequestService.generatePromptsForDeveloper(currentReq)
                                AppRequestService.updateRequestPrompts(context, requestId, prompts)
                                request.value = AppRequestService.getRequests(context, isDeveloper = true).find { it.id == requestId }
                                isLoadingPrompts = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                    ) {
                        if (isLoadingPrompts) {
                            CircularProgressIndicator(color = DeepSlate, modifier = Modifier.size(20.dp))
                        } else {
                            Text(Translator.tr("توليد خطة البرمجة بالذكاء الاصطناعي"), color = DeepSlate, fontFamily = CairoFont, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = CardSurface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(Translator.tr("خطة البرمجة المقترحة:"), color = GoldPrimary, fontFamily = CairoFont, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(promptsText, color = Color.White, fontFamily = NotoSansFont, fontSize = 12.sp)
                        }
                    }
                }
            }
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                reverseLayout = true,
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages.value.reversed()) { msg ->
                    val isMine = msg.isDeveloper == isAdmin
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .background(
                                    color = if (isMine) GoldPrimary else CardSurface,
                                    shape = RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isMine) 16.dp else 0.dp,
                                        bottomEnd = if (isMine) 0.dp else 16.dp
                                    )
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                text = msg.message,
                                color = if (isMine) DeepSlate else Color.White,
                                fontFamily = NotoSansFont,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
