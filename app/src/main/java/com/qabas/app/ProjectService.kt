package com.qabas.app

import android.content.Context
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProjectService(private val context: Context) {
    @Immutable
    data class Project(
        val id: String = "",
        val title: String = "",
        val idea: String = "",
        val analysis: String = "",
        val resources: String = "",
        val settings: String = "",
        val script: String = "",
        val finalVideoPath: String = "",
        val cost: Double = 0.0,
        val createdAt: Long = 0L,
        val updatedAt: Long = 0L,
        val status: String = "",
        val lastScreen: String = ""
    )

    private val repository: ProjectRepository by lazy {
        ProjectRepository(AppDatabase.getDatabase(context).projectDao())
    }

    /** قائمة سريعة من Room فقط — لا شبكة */
    suspend fun getLocalProjects(): List<Project> = withContext(Dispatchers.IO) {
        repository.getAllProjectsOnce()
            .map { it.toProject() }
            .sortedByDescending { it.updatedAt }
    }

    /**
     * الافتراضي: محلي فقط.
     * syncCloud = true عند شاشة التحميل / سحب للتحديث فقط.
     */
    suspend fun getAllProjects(syncCloud: Boolean = false): List<Project> =
        withContext(Dispatchers.IO) {
            val localProjects = repository.getAllProjectsOnce().map { it.toProject() }

            if (!syncCloud ||
                !CloudServices.isFirebaseInitialized ||
                CloudServices.Auth.getCurrentUserId() == null
            ) {
                return@withContext localProjects.sortedByDescending { it.updatedAt }
            }

            try {
                val cloudProjects = CloudServices.Database.getUserProjects()
                val merged = LinkedHashMap<String, Project>(localProjects.size + cloudProjects.size)
                localProjects.forEach { merged[it.id] = it }

                val toUpsert = ArrayList<ProjectEntity>()
                cloudProjects.forEach { cloudProj ->
                    val localProj = merged[cloudProj.id]
                    if (localProj == null || cloudProj.updatedAt > localProj.updatedAt) {
                        merged[cloudProj.id] = cloudProj
                        toUpsert.add(cloudProj.toEntity())
                    }
                }
                if (toUpsert.isNotEmpty()) {
                    repository.insertAll(toUpsert)
                }
                merged.values.sortedByDescending { it.updatedAt }
            } catch (_: Exception) {
                localProjects.sortedByDescending { it.updatedAt }
            }
        }

    suspend fun syncFromCloud(): List<Project> = getAllProjects(syncCloud = true)

    suspend fun getProject(id: String): Project? = withContext(Dispatchers.IO) {
        repository.getProject(id)?.toProject()
    }

    suspend fun saveProject(project: Project) = withContext(Dispatchers.IO) {
        repository.insert(project.toEntity())

        if (CloudServices.isFirebaseInitialized && CloudServices.Auth.getCurrentUserId() != null) {
            runCatching { CloudServices.Database.saveProjectToCloud(project) }
        }

        try {
            if (project.analysis.isNotEmpty()) {
                val obj = org.json.JSONObject(project.analysis)
                val style = obj.optString("suggestedStyle")
                if (style.isNotBlank()) TasteManager.registerPreference(context, "STYLE", style, 1)
                val tone = obj.optString("tone")
                if (tone.isNotBlank()) TasteManager.registerPreference(context, "TONE", tone, 1)
                val keywords = obj.optJSONArray("keywords")
                if (keywords != null && keywords.length() > 0) {
                    for (i in 0 until keywords.length()) {
                        TasteManager.registerPreference(context, "TOPIC", keywords.getString(i), 1)
                    }
                }
            }
        } catch (_: Exception) { }
    }

    suspend fun deleteProject(id: String) = withContext(Dispatchers.IO) {
        repository.delete(id)
        if (CloudServices.isFirebaseInitialized && CloudServices.Auth.getCurrentUserId() != null) {
            runCatching { CloudServices.Database.deleteProjectFromCloud(id) }
        }
    }

    private fun ProjectEntity.toProject() = Project(
        id = id, title = title, idea = idea, analysis = analysis,
        resources = resources, settings = settings, script = script,
        finalVideoPath = finalVideoPath, cost = cost,
        createdAt = createdAt, updatedAt = updatedAt,
        status = status, lastScreen = lastScreen
    )

    private fun Project.toEntity() = ProjectEntity(
        id = id, title = title, idea = idea, analysis = analysis,
        resources = resources, settings = settings, script = script,
        finalVideoPath = finalVideoPath, cost = cost,
        createdAt = createdAt, updatedAt = updatedAt,
        status = status, lastScreen = lastScreen
    )
}
