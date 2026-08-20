import re

with open('app/src/main/java/com/example/RealServices.kt', 'r') as f:
    content = f.read()

pattern = re.compile(r'suspend fun analyzeIdea\(idea: String\): IdeaAnalysis = withContext\(Dispatchers\.IO\) \{.*?return@safeApiCallWithRetry IdeaAnalysis\([\s\S]*?\}\n\s*\}\n\s*\}', re.MULTILINE | re.DOTALL)

replacement = """suspend fun analyzeIdea(idea: String): IdeaAnalysis = withContext(Dispatchers.IO) {
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

            val contentsArray = org.json.JSONArray().apply {
                put(org.json.JSONObject().apply {
                    put("role", "user")
                    put("parts", org.json.JSONArray().apply {
                        put(org.json.JSONObject().apply { put("text", "\$systemPrompt\\n\\n--- الفكرة ---\\n\$idea\\n--- نهاية ---") })
                    })
                })
            }
            
            val jsonBody = org.json.JSONObject().apply {
                put("contents", contentsArray)
                put("generationConfig", org.json.JSONObject().apply {
                    put("response_mime_type", "application/json")
                    put("temperature", 0.2)
                })
            }

            val request = okhttp3.Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=\$apiKey")
                .post(okhttp3.RequestBody.create(okhttp3.MediaType.parse("application/json"), jsonBody.toString()))
                .build()

            NetworkUtils.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    SystemLogsManager.addLog("ERROR", "فشل تحليل Gemini: \${response.code}", androidx.compose.ui.graphics.Color(0xFFEF4444))
                    return@safeApiCallWithRetry fallbackResult
                }
                val bodyStr = response.body()?.string() ?: ""
                val responseJson = org.json.JSONObject(bodyStr)
                val textResponse = responseJson.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text", "") ?: ""

                val cleanJson = textResponse.replace("```json", "").replace("```", "").trim()
                if (cleanJson.isBlank()) {
                    SystemLogsManager.addLog("ERROR", "رد Gemini فارغ", androidx.compose.ui.graphics.Color(0xFFEF4444))
                    return@safeApiCallWithRetry fallbackResult
                }

                val resultObj = org.json.JSONObject(cleanJson)
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
                    ctaSuggestions = listOf("شارك المقطع لتعم الفائدة"),
                    confidence = confidence,
                    themes = themesList
                )
            }
        }
    }"""

match = pattern.search(content)
if match:
    content = pattern.sub(replacement, content, count=1)
    with open('app/src/main/java/com/example/RealServices.kt', 'w') as f:
        f.write(content)
    print("Success")
else:
    print("Pattern not found")

