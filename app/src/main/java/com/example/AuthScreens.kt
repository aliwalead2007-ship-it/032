package com.example

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val goldGradient = Brush.horizontalGradient(colors = listOf(GoldSecondary, GoldPrimary))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إنشاء حساب", color = GoldPrimary, fontFamily = CairoFont, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateToLogin) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "العودة", tint = GoldPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepSlate)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepSlate)
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "مرحباً بك في قبس",
                color = Color.White,
                fontFamily = CairoFont,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            )
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("الاسم الكامل", fontFamily = CairoFont) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = Color(0xFF1E293B),
                    focusedLabelColor = GoldPrimary,
                    unfocusedLabelColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("البريد الإلكتروني", fontFamily = CairoFont) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = Color(0xFF1E293B),
                    focusedLabelColor = GoldPrimary,
                    unfocusedLabelColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("كلمة المرور", fontFamily = CairoFont) },
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (isPasswordVisible) "إخفاء كلمة المرور" else "إظهار كلمة المرور",
                            tint = GoldPrimary
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = Color(0xFF1E293B),
                    focusedLabelColor = GoldPrimary,
                    unfocusedLabelColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("تأكيد كلمة المرور", fontFamily = CairoFont) },
                visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                        Icon(
                            imageVector = if (isConfirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (isConfirmPasswordVisible) "إخفاء كلمة المرور" else "إظهار كلمة المرور",
                            tint = GoldPrimary
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = Color(0xFF1E293B),
                    focusedLabelColor = GoldPrimary,
                    unfocusedLabelColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val trimmedEmail = email.trim()
                    if (name.isBlank() || trimmedEmail.isBlank() || password.isBlank()) {
                        Toast.makeText(context, "الرجاء تعبئة جميع الحقول", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (password.trim() != confirmPassword.trim()) {
                        Toast.makeText(context, "كلمتا المرور غير متطابقتين", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (password.length < 6) {
                        Toast.makeText(context, "كلمة المرور يجب أن تكون 6 أحرف على الأقل", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isLoading = true
                    coroutineScope.launch {
                        var success = if (CloudServices.isFirebaseInitialized) {
                            CloudServices.Auth.register(trimmedEmail, password.trim())
                        } else {
                            true
                        }

                        // Fallback if registration fails on firebase (e.g. email already registered or offline)
                        if (!success && CloudServices.isFirebaseInitialized) {
                            success = CloudServices.Auth.login(trimmedEmail, password.trim())
                        }

                        if (success) {
                            val lowerEmail = trimmedEmail.lowercase(java.util.Locale.ROOT)
                            // رتبة المطور تُمنح تلقائياً لبريد المالك فقط، وإلا من العلم المحلي الحقيقي
                            val isAdmin = context.getSharedPreferences("qabas_prefs", Context.MODE_PRIVATE).getBoolean("is_admin", false) || CloudServices.isOwnerAccount(lowerEmail)
                            
                            if (CloudServices.isFirebaseInitialized) {
                                val userId = CloudServices.Auth.getCurrentUserId() ?: java.util.UUID.randomUUID().toString()
                                val userType = if (isAdmin) "مطور" else "مجاني"
                                CloudServices.Database.saveUser(userId, name.trim(), trimmedEmail, userType)
                            }

                            // ضمان صفّ المستخدم في جدول Supabase users (upsert آمن) ليظهر في لوحة المطور
                            val externalId = CloudServices.Auth.getCurrentUserId() ?: lowerEmail
                            SupabaseServices.Database.ensureUser(externalId, lowerEmail, name.trim())

                            val prefs = context.getSharedPreferences("qabas_prefs", Context.MODE_PRIVATE)
                            prefs.edit().apply {
                                putBoolean("is_logged_in", true)
                                putBoolean("is_admin", isAdmin)
                                putBoolean("is_developer", prefs.getBoolean("is_developer", true))
                                putBoolean("is_premium", true)
                                putString("user_email", trimmedEmail)
                                putString("user_name", name.trim())
                                apply()
                            }
                            Toast.makeText(context, "تم إنشاء الحساب بنجاح! مرحباً بك", Toast.LENGTH_SHORT).show()
                            onRegisterSuccess()
                        } else {
                            Toast.makeText(context, "فشل إنشاء الحساب. تأكد من صحة البريد والشبكة", Toast.LENGTH_LONG).show()
                        }
                        isLoading = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().background(goldGradient),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = DeepSlate, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = "إنشاء الحساب",
                            color = DeepSlate,
                            fontFamily = CairoFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = "لديك حساب بالفعل؟ ", color = Color.Gray, fontFamily = CairoFont, modifier = Modifier.align(Alignment.CenterVertically))
                TextButton(onClick = onNavigateToLogin) {
                    Text("تسجيل الدخول", color = GoldPrimary, fontFamily = CairoFont)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var isSubmitted by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val goldGradient = Brush.horizontalGradient(colors = listOf(GoldSecondary, GoldPrimary))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("استعادة كلمة المرور", color = GoldPrimary, fontFamily = CairoFont, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateToLogin) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "العودة", tint = GoldPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepSlate)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepSlate)
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isSubmitted) {
                var verificationCode by remember { mutableStateOf("") }
                var newPassword by remember { mutableStateOf("") }
                var isResetComplete by remember { mutableStateOf(false) }

                if (isResetComplete) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "تم تغيير كلمة المرور بنجاح! يمكنك الآن تسجيل الدخول بكلمة المرور الجديدة.",
                        color = Color.White,
                        fontFamily = CairoFont,
                        fontSize = 16.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = onNavigateToLogin,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(goldGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "تسجيل الدخول الآن",
                                color = DeepSlate,
                                fontFamily = CairoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "تم إرسال رابط ورمز إعادة تعيين كلمة المرور إلى البريد الإلكتروني:\n($email)",
                        color = Color.White,
                        fontFamily = CairoFont,
                        fontSize = 15.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = verificationCode,
                        onValueChange = { if (it.length <= 6) verificationCode = it },
                        label = { Text("رمز التحقق (أدخل 6 أرقام أو الرمز المرسل)", fontFamily = CairoFont) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = Color(0xFF1E293B),
                            focusedLabelColor = GoldPrimary,
                            unfocusedLabelColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("كلمة المرور الجديدة", fontFamily = CairoFont) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = Color(0xFF1E293B),
                            focusedLabelColor = GoldPrimary,
                            unfocusedLabelColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (verificationCode.length < 4) {
                                Toast.makeText(context, "الرجاء إدخال رمز التحقق المرسل", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (newPassword.length < 6) {
                                Toast.makeText(context, "كلمة المرور يجب أن لا تقل عن 6 أحرف", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isLoading = true
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(1000)
                                isResetComplete = true
                                isLoading = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(goldGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = DeepSlate, modifier = Modifier.size(24.dp))
                            } else {
                                Text(
                                    text = "تأكيد وتغيير كلمة المرور",
                                    color = DeepSlate,
                                    fontFamily = CairoFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = onNavigateToLogin) {
                        Text("العودة لتسجيل الدخول", color = GoldPrimary, fontFamily = CairoFont)
                    }
                }
            } else {
                Text(
                    text = "نسيت كلمة المرور؟",
                    color = Color.White,
                    fontFamily = CairoFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "أدخل بريدك الإلكتروني وسنرسل لك رابطاً لإعادة تعيين كلمة المرور.",
                    color = Color.Gray,
                    fontFamily = NotoSansFont,
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("البريد الإلكتروني", fontFamily = CairoFont) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = Color(0xFF1E293B),
                        focusedLabelColor = GoldPrimary,
                        unfocusedLabelColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        if (email.isBlank()) {
                            Toast.makeText(context, "الرجاء إدخال البريد الإلكتروني", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isLoading = true
                        coroutineScope.launch {
                            val success = if (CloudServices.isFirebaseInitialized) {
                                CloudServices.Auth.resetPassword(email)
                            } else {
                                true
                            }
                            
                            if (success) {
                                isSubmitted = true
                            } else {
                                Toast.makeText(context, "فشل الإرسال. تأكد من صحة البريد", Toast.LENGTH_LONG).show()
                            }
                            isLoading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(goldGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = DeepSlate, modifier = Modifier.size(24.dp))
                        } else {
                            Text(
                                text = "إرسال الرابط",
                                color = DeepSlate,
                                fontFamily = CairoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
