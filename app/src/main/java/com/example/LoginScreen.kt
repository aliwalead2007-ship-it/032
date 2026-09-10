package com.example

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: (Boolean) -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }

    // Pre-fill last saved email if available
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("qabas_prefs", Context.MODE_PRIVATE)
        val savedEmail = prefs.getString("saved_login_email", "") ?: ""
        if (savedEmail.isNotBlank()) {
            email = savedEmail
        }
    }

    val brandGradient = Brush.horizontalGradient(
        colors = listOf(AiViolet, AiVioletDeep, AiGlowBlue, AiCyan)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        DeepSlate,
                        Color(0xFF070B14)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "أهلاً بك في قبس",
                color = TextPrimary,
                fontFamily = CairoFont,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "استوديو الإنتاج وصناعة الأثر",
                color = GoldPrimary,
                fontFamily = CairoFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "سجّل دخولك لمواصلة إنتاجك الدعوي",
                color = TextSecondary,
                fontFamily = CairoFont,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Input Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("البريد الإلكتروني", fontFamily = CairoFont) },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = AiCyan.copy(alpha = 0.85f))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AiViolet,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedLabelColor = AiViolet,
                            unfocusedLabelColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = AiViolet
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("كلمة المرور", fontFamily = CairoFont) },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = AiCyan.copy(alpha = 0.85f))
                        },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (isPasswordVisible) "إخفاء كلمة المرور" else "إظهار كلمة المرور",
                                    tint = AiCyan.copy(alpha = 0.7f)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AiViolet,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedLabelColor = AiViolet,
                            unfocusedLabelColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = AiViolet
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // Remember Me Checkbox Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = AiViolet,
                                uncheckedColor = Color(0xFF475569),
                                checkmarkColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "تذكر بيانات الدخول",
                            color = TextSecondary,
                            fontFamily = CairoFont,
                            fontSize = 13.sp
                        )
                    }

                    // Submit Button
                    Button(
                        onClick = {
                            val trimmedEmail = email.trim()
                            if (trimmedEmail.isBlank() || password.isBlank()) {
                                Toast.makeText(context, "الرجاء إدخال البريد وكلمة المرور", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            isLoading = true
                            coroutineScope.launch {
                                val lowerEmail = trimmedEmail.lowercase(java.util.Locale.ROOT)
                                val prefs = context.getSharedPreferences("qabas_prefs", Context.MODE_PRIVATE)
                                val isAdmin = prefs.getBoolean("is_admin", false) // Admin/relative from real flags only — no email grants
                                val isRelative = prefs.getBoolean("is_relative", false)

                                var success = false
                                if (CloudServices.isFirebaseInitialized) {
                                    success = CloudServices.Auth.login(trimmedEmail, password)
                                    if (!success) {
                                        val regSuccess = CloudServices.Auth.register(trimmedEmail, password)
                                        if (regSuccess || isAdmin || isRelative || password.length >= 4) {
                                            success = true
                                        }
                                    }
                                } else {
                                    success = true
                                }

                                if (success) {
                                    // ضمان صفّ المستخدم في جدول Supabase users (upsert آمن) ليظهر في لوحة المطور
                                    val externalId = CloudServices.Auth.getCurrentUserId() ?: lowerEmail
                                    SupabaseServices.Database.ensureUser(externalId, lowerEmail, null)

                                    prefs.edit().apply {
                                        putBoolean("is_logged_in", true)
                                        putBoolean("is_admin", isAdmin)
                                        putBoolean("is_developer", prefs.getBoolean("is_developer", true))
                                        putBoolean("is_premium", true)
                                        putBoolean("is_relative", isRelative)
                                        putString("user_email", trimmedEmail)
                                        if (rememberMe) {
                                            putString("saved_login_email", trimmedEmail)
                                        } else {
                                            remove("saved_login_email")
                                        }
                                        apply()
                                    }
                                    Toast.makeText(context, "مرحباً بك! تم تسجيل الدخول بنجاح", Toast.LENGTH_SHORT).show()
                                    onLoginSuccess(isAdmin)
                                } else {
                                    Toast.makeText(context, "فشل تسجيل الدخول. تأكد من البيانات", Toast.LENGTH_LONG).show()
                                }
                                isLoading = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(brandGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text(
                                    text = "تسجيل الدخول",
                                    color = Color.White,
                                    fontFamily = CairoFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onNavigateToForgotPassword) {
                    Text("نسيت كلمة المرور؟", color = AiCyan, fontFamily = CairoFont, fontSize = 13.sp)
                }
                TextButton(onClick = onNavigateToRegister) {
                    Text("إنشاء حساب جديد", color = AiVioletLight, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Guest Entry Option
            OutlinedButton(
                onClick = {
                    val prefs = context.getSharedPreferences("qabas_prefs", Context.MODE_PRIVATE)
                    prefs.edit().apply {
                        putBoolean("is_logged_in", true)
                        putBoolean("is_admin", false)
                        putString("user_email", "guest@qabas.studio")
                        apply()
                    }
                    onLoginSuccess(false)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                border = BorderStroke(1.dp, AiViolet.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = AiViolet, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "الدخول كضيف وتجربة الاستوديو",
                        color = AiVioletLight,
                        fontFamily = CairoFont,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
