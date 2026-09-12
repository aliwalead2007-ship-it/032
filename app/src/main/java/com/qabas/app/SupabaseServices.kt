package com.qabas.app

import android.util.Log
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import java.util.UUID

/**
 * High-level Supabase service module.
 *
 * Mirrors the shape of [CloudServices] so callers can fall back to the local
 * handler when Supabase is not configured. Every public method returns
 * `null` / `emptyList()` / `false` when Supabase is unavailable so the app
 * degrades gracefully.
 *
 * Requires:
 *   - SUPABASE_URL in .env (BuildConfig.SUPABASE_URL)
 *   - SUPABASE_ANON_KEY in .env (BuildConfig.SUPABASE_ANON_KEY)
 */
object SupabaseServices {

    private const val TAG = "SupabaseServices"

    val isSupabaseAvailable: Boolean
        get() = SupabaseConfig.isConfigured

    private val client get() = SupabaseConfig.client

    // ---------- Typed rows ----------

    @Serializable
    data class UserRow(
        val id: String,
        @SerialName("external_id") val externalId: String? = null,
        val email: String? = null,
        val name: String? = null,
        val type: String = "free",
        val status: String? = "active",
        val strikes: Int = 0,
        @SerialName("is_eligible") val isEligible: Boolean = true,
        @SerialName("created_at") val createdAt: String? = null,
        @SerialName("updated_at") val updatedAt: String? = null
    )

    @Serializable
    data class HadithRow(
        val id: String,
        val title: String,
        val narrator: String,
        val text: String,
        val status: String,
        val source: String,
        @SerialName("theme_bg") val themeBg: String = "black_gold",
        @SerialName("suggested_aspect") val suggestedAspect: String = "9:16",
        @SerialName("display_order") val displayOrder: Int = 0,
        @SerialName("is_active") val isActive: Boolean = true
    )

    @Serializable
    data class ProjectRow(
        val id: String,
        @SerialName("user_id") val userId: String? = null,
        @SerialName("external_id") val externalId: String? = null,
        val title: String,
        val description: String? = null,
        val status: String = "draft",
        val cost: Double = 0.0,
        val tags: JsonElement = JsonArray(emptyList()),
        val metadata: JsonElement = JsonObject(emptyMap()),
        @SerialName("created_at") val createdAt: String? = null,
        @SerialName("updated_at") val updatedAt: String? = null
    )

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // ---------- Authentication ----------

    object Auth {
        private val _currentUser = MutableStateFlow<UserRow?>(null)
        val currentUser: StateFlow<UserRow?> = _currentUser.asStateFlow()

        fun getCurrentUserId(): String? {
            if (!isSupabaseAvailable) return null
            return runCatching { client.auth.currentUserOrNull()?.id }.getOrNull()
        }

        suspend fun register(email: String, password: String): Boolean {
            if (!isSupabaseAvailable) return false
            return runCatching {
                client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }
                true
            }.onFailure { Log.w(TAG, "Supabase register failed: ${it.message}") }
                .getOrDefault(false)
        }

        suspend fun login(email: String, password: String): Boolean {
            if (!isSupabaseAvailable) return false
            return runCatching {
                client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                true
            }.onFailure { Log.w(TAG, "Supabase login failed: ${it.message}") }
                .getOrDefault(false)
        }

        suspend fun resetPassword(email: String): Boolean {
            if (!isSupabaseAvailable) return false
            return runCatching {
                client.auth.resetPasswordForEmail(email)
                true
            }.onFailure { Log.w(TAG, "Supabase reset password failed: ${it.message}") }
                .getOrDefault(false)
        }

        suspend fun logout() {
            if (!isSupabaseAvailable) return
            runCatching { client.auth.signOut() }
                .onFailure { Log.w(TAG, "Supabase logout failed: ${it.message}") }
        }
    }

    // ---------- Database ----------

    object Database {
        /** Lightweight health check used by dashboards and the developer screen. */
        suspend fun ping(): Boolean {
            if (!isSupabaseAvailable) return false
            return runCatching {
                client.postgrest.from("users").select().decodeListOrEmpty<UserRow>()
                true
            }.onFailure { Log.w(TAG, "Supabase ping failed: ${it.message}") }
                .getOrDefault(false)
        }

        /**
         * Look up a user row by external_id (typically the Firebase Auth uid).
         * If no row is found, inserts a fresh row and returns the inserted
         * representation. Returns `null` when Supabase is unavailable.
         */
        suspend fun ensureUser(externalId: String, email: String?, name: String?): UserRow? {
            if (!isSupabaseAvailable) return null
            return runCatching {
                val existing = client.postgrest.from("users").select {
                    filter { eq("external_id", externalId) }
                    limit(1)
                }.decodeListOrEmpty<UserRow>().firstOrNull()
                existing ?: client.postgrest.from("users").upsert(
                    JsonArray(
                        listOf(
                            json.encodeToJsonElement(
                                UserRow(
                                    id = UUID.randomUUID().toString(),
                                    externalId = externalId,
                                    email = email,
                                    name = name
                                )
                            )
                        )
                    )
                ) {
                    onConflict = "external_id"
                }.decodeSingle<UserRow>()
            }.onFailure { Log.w(TAG, "ensureUser failed: ${it.message}") }
                .getOrNull()
        }

        suspend fun saveProject(
            title: String,
            description: String?,
            status: String = "draft",
            cost: Double = 0.0,
            tags: List<String> = emptyList(),
            metadata: Map<String, String> = emptyMap()
        ): ProjectRow? {
            if (!isSupabaseAvailable) return null
            return runCatching {
                val userId = Auth.getCurrentUserId()
                val row = ProjectRow(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    externalId = userId,
                    title = title,
                    description = description,
                    status = status,
                    cost = cost,
                    tags = JsonArray(tags.map { JsonPrimitive(it) }),
                    metadata = JsonObject(metadata.mapValues { (_, v) -> JsonPrimitive(v) })
                )
                client.postgrest.from("projects").insert(
                    JsonArray(listOf(json.encodeToJsonElement(row)))
                ).decodeSingle<ProjectRow>()
            }.onFailure { Log.w(TAG, "saveProject failed: ${it.message}") }
                .getOrNull()
        }

        suspend fun getFeed(limit: Long = 20L): List<ProjectRow> {
            if (!isSupabaseAvailable) return emptyList()
            return runCatching {
                client.postgrest.from("projects").select {
                    order("created_at", Order.DESCENDING)
                    limit(limit)
                }.decodeList<ProjectRow>()
            }.onFailure { Log.w(TAG, "getFeed failed: ${it.message}") }
                .getOrDefault(emptyList())
        }

        /**
         * Fetch the full, real user list from the Supabase `users` table for the
         * developer dashboard. Returns an empty list when Supabase is unavailable.
         */
        suspend fun getAllUsers(): List<UserRow> {
            if (!isSupabaseAvailable) return emptyList()
            return runCatching {
                client.postgrest.from("users").select {
                    order("created_at", Order.DESCENDING)
                }.decodeList<UserRow>()
            }.onFailure { Log.w(TAG, "getAllUsers failed: ${it.message}") }
                .getOrDefault(emptyList())
        }

        /** Build a JSON object for jsonb columns from vararg pairs. */
        fun jsonObject(vararg pairs: Pair<String, Any?>): String = JsonObject(
            pairs.associate { (k, v) ->
                k to when (v) {
                    null -> JsonNull
                    is Number -> JsonPrimitive(v)
                    is Boolean -> JsonPrimitive(v)
                    else -> JsonPrimitive(v.toString())
                }
            }
        ).toString()

        /**
         * Fetch the active hadith preset catalog from the cloud. Ordered by
         * `display_order` so admins control the sequence. Returns an empty
         * list when Supabase is unavailable so callers can fall back to a
         * hard-coded list without crashing.
         */
        suspend fun getHadithPresets(): List<HadithRow> {
            if (!isSupabaseAvailable) return emptyList()
            return runCatching {
                client.postgrest.from("hadith_presets").select {
                    filter { eq("is_active", true) }
                    order("display_order", Order.ASCENDING)
                }.decodeList<HadithRow>()
            }.onFailure { Log.w(TAG, "getHadithPresets failed: ${it.message}") }
                .getOrDefault(emptyList())
        }
    }
}

/** Best-effort decode that returns an empty list instead of throwing. */
private suspend inline fun <reified T> io.github.jan.supabase.postgrest.result.PostgrestResult
    .decodeListOrEmpty(): List<T> = runCatching { decodeList<T>() }.getOrDefault(emptyList())
