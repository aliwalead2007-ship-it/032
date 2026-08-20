import sys

with open('app/src/main/java/com/example/RealServices.kt', 'r') as f:
    content = f.read()

start_func = "suspend fun analyzeIdea(idea: String): IdeaAnalysis = withContext(Dispatchers.IO) {"
start_idx = content.find(start_func)
if start_idx == -1:
    print("Could not find analyzeIdea")
    sys.exit(1)

end_func = "suspend fun transcribeAudio(audioFile: File): String = withContext(Dispatchers.IO) {"
end_idx = content.find(end_func)
if end_idx == -1:
    print("Could not find transcribeAudio")
    sys.exit(1)

new_func = """suspend fun analyzeIdea(idea: String): IdeaAnalysis = withContext(Dispatchers.IO) {
        val prefs = AppServices.appContext.getSharedPreferences("qabas_prefs", android.content.Context.MODE_PRIVATE)
        val apiKey = prefs.getString("gemini_key", "") ?: ""
        val fallbackResult = buildDefaultIdeaAnalysis(idea)
        
        if (apiKey.isBlank() || apiKey == "YOUR_GEMINI_API_KEY") {
            SystemLogsManager.addLog("WARN", "لم يتم العثور على مفتاح Gemini، سيتم استخدام التحليل المحلي.", androidx.compose.ui.graphics.Color(0xFFE8C547))
            return@withContext fallbackResult
        }
        
        return@withContext NetworkUtils.safeApiCallWithRetry(
            context = AppServices.appContext,
            maxRetries = 3,
            initialDelayMs = 1000L,
            fallback = { fallbackResult }
        ) {
            val tasteContext = TasteManager.getTasteContext(AppServices.appContext)
            val systemPrompt = \"\"\"
                أنت محلل محتوى دعوي إخراجي متخصص.
                يجب عليك استخراج النبرة والمحاور والمشاهد والخطاف من نص المستخدم فقط وبدقة متناهية. لا تختلق قصصاً أو أحداثاً أو أسماء غير مذكورة.
                إن كان النص قصيراً فقلل عدد المشاهد. لا تستخدم قوالب جاهزة.
                \$tasteContext
                
                يجب أن تكون النتيجة بتنسيق JSON حصراً:
                {
                    "hook": "جملة افتتاحية مستخرجة أو مصاغة مباشرة من معنى النص",
                    "tone": "النبرة المستخرجة من النص (مثل: خاشع، حماسي، تأملي، وعظي، تعليمي)",
                    "themes": ["محور 1", "محور 2"],
                    "scenes": [
                        { "title": "عنوان المشهد", "description": "وصف المشهد المشتق من النص", "durationHintSeconds": 5 }
                    ],
                    "confidence": 0.95
                }
            \"\"\".trimIndent()

            val contentsArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", "\$systemPrompt\\n\\n--- الفكرة ---\\n\$idea\\n--- نهاية ---") })
                    })
                })
            }
            
            val jsonBody = JSONObject().apply {
                put("contents", contentsArray)
                put("generationConfig", JSONObject().apply {
                    put("response_mime_type", "application/json")
                    put("temperature", 0.2)
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
                val responseJson = JSONObject(responseData)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val content = candidates.getJSONObject(0).optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text")
                        val cleanJson = text.replace("```json", "").replace("```", "").trim()
                        val resultObj = JSONObject(cleanJson)
                        
                        val confidence = resultObj.optDouble("confidence", 1.0)
                        if (confidence < 0.3) {
                            SystemLogsManager.addLog("WARN", "مستوى الثقة ضعيف (\${(confidence * 100).toInt()}%)، سيتم استخدام التحليل المحلي.", androidx.compose.ui.graphics.Color(0xFFE8C547))
                            return@safeApiCallWithRetry fallbackResult
                        }
                        
                        val themesList = mutableListOf<String>()
                        val themesArr = resultObj.optJSONArray("themes")
                        if (themesArr != null) {
                            for (i in 0 until themesArr.length()) themesList.add(themesArr.getString(i))
                        }
                        
                        val scenesList = mutableListOf<String>()
                        val scenesArr = resultObj.optJSONArray("scenes")
                        if (scenesArr != null) {
                            for (i in 0 until scenesArr.length()) {
                                val sObj = scenesArr.optJSONObject(i)
                                if (sObj != null) {
                                    scenesList.add(sObj.optString("title", "مشهد") + ": " + sObj.optString("description", ""))
                                }
                            }
                        }
                        
                        val hook = resultObj.optString("hook", "")
                        
                        return@safeApiCallWithRetry IdeaAnalysis(
                            summary = idea.take(100),
                            suggestedStyle = "سينمائي وقور",
                            goal = "رسالة إيمانية",
                            targetAudience = "الجمهور العام",
                            tone = resultObj.optString("tone", "روحاني"),
                            keywords = themesList,
                            proposedScenes = scenesList.takeIf { it.isNotEmpty() } ?: listOf("مشهد 1: عرض الفكرة الأساسية"),
                            viralityScore = (confidence * 100).toInt(),
                            hookSuggestions = if (hook.isNotBlank()) listOf(hook) else emptyList(),
                            ctaSuggestions = emptyList(),
                            confidence = confidence,
                            themes = themesList
                        )
                    }
                }
            }
            throw Exception("Failed to analyze idea")
        }
    }

    """

new_content = content[:start_idx] + new_func + content[end_idx:]

with open('app/src/main/java/com/example/RealServices.kt', 'w') as f:
    f.write(new_content)

print("success")
