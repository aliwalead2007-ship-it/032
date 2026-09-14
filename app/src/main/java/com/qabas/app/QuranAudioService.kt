package com.qabas.app

import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * QuranAudioService.kt
 *
 * تلاوة صفحات المصحف عبر Quran Foundation API (المشعّر Alafasy).
 * - المفاتيح من BuildConfig (ملف `.env` المحلي / GitHub Secrets) — لا مفاتيح في الكود.
 * - عند غياب المفاتيح: isConfigured()=false والقارئ يعرض رسالة صادقة (لا وهم).
 * - التوثيق: api-docs.quran.foundation (OAuth2 client_credentials + verses/by_page).
 */
object QuranAudioService {

    private const val TAG = "QuranAudioService"
    private const val TOKEN_URL = "https://oauth2.quran.foundation/oauth2/token"
    private const val API_BASE = "https://apis.quran.foundation/content/api/v4"
    private const val VERSE_AUDIO_BASE = "https://verses.quran.foundation/"
    private const val RECITER_ALAFASY = 7
    private const val MUSHAF_QCF_V2 = 1

    private val client = OkHttpClient()

    @Volatile
    private var cachedToken: String? = null

    @Volatile
    private var tokenExpiryMs: Long = 0L

    fun isConfigured(): Boolean {
        val id = BuildConfig.QF_CLIENT_ID
        val secret = BuildConfig.QF_CLIENT_SECRET
        return id.isNotBlank() && id != "your_key" &&
            secret.isNotBlank() && secret != "your_key"
    }

    private fun clientId(): String = BuildConfig.QF_CLIENT_ID
    private fun clientSecret(): String = BuildConfig.QF_CLIENT_SECRET

    /**
     * توكن OAuth2 (client_credentials + scope=content) مع تخزين مؤقت حتى انتهائه.
     * يُرجع null عند الفشل — لا توكن وهمي.
     */
    private suspend fun ensureToken(context: Context): String? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (cachedToken != null && now < tokenExpiryMs - 60_000) return@withContext cachedToken
        val t0 = System.nanoTime()
        var ok = false
        try {
            val body = FormBody.Builder()
                .add("grant_type", "client_credentials")
                .add("scope", "content")
                .build()
            // المحاولة 1: Basic auth (المعيار في OAuth2)
            var token = requestToken(body, Credentials.basic(clientId(), clientSecret()))
            // المحاولة 2: المفاتيح في body (بعض البوابات تقبلها هكذا)
            if (token == null) {
                val body2 = FormBody.Builder()
                    .add("grant_type", "client_credentials")
                    .add("scope", "content")
                    .add("client_id", clientId())
                    .add("client_secret", clientSecret())
                    .build()
                token = requestToken(body2, null)
            }
            ok = token != null
            return@withContext token
        } catch (e: Exception) {
            Log.w(TAG, "Token failed: ${e.message}")
            return@withContext null
        } finally {
            try {
                ApiUsageTracker.recordCall(context, "Quran Foundation", (System.nanoTime() - t0) / 1_000_000, ok)
            } catch (_: Exception) {
            }
        }
    }

    private fun requestToken(body: FormBody, authHeader: String?): String? {
        val builder = Request.Builder().url(TOKEN_URL).post(body)
        if (authHeader != null) builder.header("Authorization", authHeader)
        client.newCall(builder.build()).execute().use { resp ->
            if (!resp.isSuccessful) {
                Log.w(TAG, "Token HTTP ${resp.code}")
                return null
            }
            val json = JSONObject(resp.body?.string() ?: return null)
            val token = json.optString("access_token")
            if (token.isBlank()) return null
            val expiresIn = json.optLong("expires_in", 3600)
            cachedToken = token
            tokenExpiryMs = System.currentTimeMillis() + expiresIn * 1000
            return token
        }
    }

    /**
     * روابط mp3 لآيات صفحة معينة بالترتيب. قائمة فارغة عند أي فشل (لا روابط وهمية).
     */
    suspend fun getPageAudioUrls(context: Context, page: Int): List<String> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext emptyList()
        val token = ensureToken(context) ?: return@withContext emptyList()
        val t0 = System.nanoTime()
        var ok = false
        try {
            val urls = ArrayList<String>()
            var apiPage = 1
            repeat(3) {
                val url = "$API_BASE/verses/by_page/$page?words=false&audio=$RECITER_ALAFASY" +
                    "&mushaf=$MUSHAF_QCF_V2&per_page=all&page=$apiPage"
                val req = Request.Builder().url(url)
                    .header("x-auth-token", token)
                    .header("x-client-id", clientId())
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        Log.w(TAG, "Verses HTTP ${resp.code} for page $page")
                        return@repeat
                    }
                    val root = JSONObject(resp.body?.string() ?: return@repeat)
                    val verses = root.optJSONArray("verses") ?: return@repeat
                    for (i in 0 until verses.length()) {
                        val audio = verses.optJSONObject(i)?.optJSONObject("audio") ?: continue
                        val rel = audio.optString("url")
                        if (rel.isNotBlank()) {
                            urls.add(if (rel.startsWith("http")) rel else VERSE_AUDIO_BASE + rel)
                        }
                    }
                    val pagination = root.optJSONObject("pagination")
                    val next = pagination?.optInt("next_page", -1) ?: -1
                    if (next <= 0) {
                        ok = urls.isNotEmpty()
                        return@withContext urls
                    }
                    apiPage = next
                }
            }
            ok = urls.isNotEmpty()
            return@withContext urls
        } catch (e: Exception) {
            Log.w(TAG, "Page audio failed: ${e.message}")
            return@withContext emptyList()
        } finally {
            try {
                ApiUsageTracker.recordCall(context, "Quran Foundation", (System.nanoTime() - t0) / 1_000_000, ok)
            } catch (_: Exception) {
            }
        }
    }
}

/**
 * مشغّل بسيط لقائمة آيات الصفحة (Media3 ExoPlayer — موجود في المشروع).
 */
object QuranAudioPlayer {

    private var player: ExoPlayer? = null

    fun playUrls(context: Context, urls: List<String>) {
        stop()
        if (urls.isEmpty()) return
        val p = ExoPlayer.Builder(context.applicationContext).build()
        p.setMediaItems(urls.map { MediaItem.fromUri(it) })
        p.prepare()
        p.play()
        player = p
    }

    fun toggle(): Boolean {
        val p = player ?: return false
        if (p.isPlaying) p.pause() else p.play()
        return p.isPlaying
    }

    fun isPlaying(): Boolean = player?.isPlaying == true

    fun hasPlayer(): Boolean = player != null

    fun stop() {
        try {
            player?.stop()
            player?.release()
        } catch (_: Exception) {
        }
        player = null
    }
}
