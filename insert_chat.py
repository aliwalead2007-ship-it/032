import sys

with open('app/src/main/java/com/example/RealServices.kt', 'r') as f:
    content = f.read()

new_func = """
    suspend fun chatWithAssistant(
        messages: List<Pair<Boolean, String>>,
        customSystemInstruction: String? = null
    ): String = withContext(Dispatchers.IO) {
        val prefs = AppServices.appContext.getSharedPreferences("qabas_prefs", android.content.Context.MODE_PRIVATE)
        val apiKey = prefs.getString("gemini_key", "") ?: ""
        if (apiKey.isBlank() || apiKey == "YOUR_GEMINI_API_KEY") {
            return@withContext "عذراً، مفتاح Gemini غير متوفر. يرجى إضافته من الإعدادات."
        }
        
        return@withContext NetworkUtils.safeApiCallWithRetry(
            context = AppServices.appContext,
            maxRetries = 2,
            initialDelayMs = 1000L,
            fallback = { "حدث خطأ أثناء الاتصال بالذكاء الاصطناعي." }
        ) {
            val systemPrompt = customSystemInstruction ?: TasteManager.getTasteContext(AppServices.appContext)
            val contentsArray = org.json.JSONArray()
            for (msg in messages) {
                contentsArray.put(org.json.JSONObject().apply {
                    put("role", if (msg.first) "user" else "model")
                    put("parts", org.json.JSONArray().apply {
                        put(org.json.JSONObject().apply { put("text", msg.second) })
                    })
                })
            }
            
            val jsonBody = org.json.JSONObject().apply {
                put("contents", contentsArray)
                put("systemInstruction", org.json.JSONObject().apply {
                    put("parts", org.json.JSONArray().apply {
                        put(org.json.JSONObject().apply { put("text", systemPrompt) })
                    })
                })
                put("generationConfig", org.json.JSONObject().apply {
                    put("temperature", 0.7)
                })
            }
            
            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=\$apiKey")
                .post(requestBody)
                .build()
                
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseData = response.body?.string() ?: ""
                val responseJson = org.json.JSONObject(responseData)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val content = candidates.getJSONObject(0).optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@safeApiCallWithRetry parts.getJSONObject(0).optString("text")
                    }
                }
            }
            "حدث خطأ أثناء معالجة الرد."
        }
    }
}
"""

content = content.replace("    }\n}\n\nobject RealGroqService {", new_func + "\nobject RealGroqService {")

with open('app/src/main/java/com/example/RealServices.kt', 'w') as f:
    f.write(content)
print("Updated RealServices.kt")
