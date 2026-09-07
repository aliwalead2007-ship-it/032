package com.example

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.functions.Functions

/**
 * Single Supabase client configured once per process.
 *
 * Credentials come from BuildConfig (populated by the secrets gradle plugin
 * via local.properties / .env). When keys are missing the client is replaced
 * with a no-op sentinel — every caller must still check
 * [SupabaseServices.isSupabaseAvailable] before using the API so the app
 * degrades gracefully on devices where Supabase is not yet wired up.
 */
object SupabaseConfig {

    private const val TAG = "SupabaseConfig"

    @Volatile
    private var _client: SupabaseClient? = null

    val isConfigured: Boolean
        get() = url.isNotBlank() && key.isNotBlank()

    /** Reads SUPABASE_URL/SUPABASE_ANON_KEY from BuildConfig. Empty when not set. */
    val url: String
        get() = runCatching { BuildConfig.SUPABASE_URL }.getOrNull()?.takeIf { !it.isNullOrBlank() } ?: ""

    val key: String
        get() = runCatching { BuildConfig.SUPABASE_ANON_KEY }.getOrNull()?.takeIf { !it.isNullOrBlank() } ?: ""

    /** Lazily created on first access. Safe to call from any thread. */
    val client: SupabaseClient
        get() = _client ?: synchronized(this) {
            _client ?: build().also { _client = it }
        }

    private fun build(): SupabaseClient {
        if (!isConfigured) {
            // Build a placeholder client that will fail loudly on every call.
            // This keeps the public surface stable when keys are missing.
            android.util.Log.w(
                TAG,
                "Supabase not configured. Set SUPABASE_URL and SUPABASE_ANON_KEY in .env. " +
                    "Cloud features that depend on Supabase will be skipped at runtime."
            )
            return createSupabaseClient(
                supabaseUrl = "https://invalid.supabase.co",
                supabaseKey = "invalid"
            ) {
                install(Postgrest)
                install(Auth) { alwaysAutoRefresh = false }
                install(Storage)
                install(Functions)
            }
        }

        return createSupabaseClient(
            supabaseUrl = url,
            supabaseKey = key
        ) {
            install(Postgrest) {
                defaultSchema = "public"
            }
            install(Auth) {
                alwaysAutoRefresh = true
            }
            install(Storage)
            install(Functions)
        }
    }

    /** Current auth session, if any. */
    fun currentSession(): UserSession? = runCatching {
        if (!isConfigured) null else client.auth.currentSessionOrNull()
    }.getOrNull()
}
