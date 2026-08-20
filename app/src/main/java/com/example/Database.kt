package com.example

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val title: String,
    val idea: String,
    val analysis: String,
    val resources: String, // serialized
    val settings: String, // serialized
    val script: String, // serialized
    val finalVideoPath: String,
    val cost: Double,
    val createdAt: Long,
    val updatedAt: Long,
    val status: String,
    val lastScreen: String
)

@Entity(tableName = "taste_profile")
data class TasteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String, // e.g., "STYLE", "PACING", "TONE", "TOPIC"
    val preferredValue: String,
    val weight: Int,
    val updatedAt: Long
)

@Entity(tableName = "hadith_cards")
data class HadithCardEntity(
    @PrimaryKey val id: String,
    val text: String,
    val narrator: String,
    val source: String,
    val styleName: String,
    val aspectRatio: String,
    val imagePath: String,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "audio_tracks")
data class AudioTrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artistOrVibe: String,
    val typeCategory: String, // "NASHEED", "RECITATION", "AI_VOICEOVER", "SFX"
    val audioUrlOrPath: String,
    val duration: String,
    val scriptText: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reel_scripts")
data class ReelScriptEntity(
    @PrimaryKey val id: String,
    val topic: String,
    val hookText: String,
    val bodyText: String,
    val ctaText: String,
    val duration: String,
    val vibe: String,
    val captionStyle: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "series_records")
data class SeriesEntity(
    @PrimaryKey val id: String,
    val title: String,
    val topicPreset: String,
    val episodeCount: Int,
    val format: String,
    val epilogTemplate: String,
    val episodesJson: String,
    val scheduleFrequency: String = "DAILY",
    val status: String = "READY",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    /** قراءة مرة واحدة — أسرع من Flow.firstOrNull للقوائم المتكررة */
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    suspend fun getAllProjectsOnce(): List<ProjectEntity>

    @Query("SELECT * FROM projects ORDER BY updatedAt DESC LIMIT :limit")
    fun getRecentProjects(limit: Int): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun getRecentProjectsOnce(limit: Int): List<ProjectEntity>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjects(projects: List<ProjectEntity>)

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: String)

    @Query("DELETE FROM projects")
    suspend fun clearAllProjects()
}

@Dao
interface TasteDao {
    @Query("SELECT * FROM taste_profile ORDER BY weight DESC")
    suspend fun getAllTastes(): List<TasteEntity>

    @Query("SELECT * FROM taste_profile ORDER BY weight DESC")
    fun getAllTastesFlow(): Flow<List<TasteEntity>>

    @Query("SELECT * FROM taste_profile WHERE category = :category AND preferredValue = :value LIMIT 1")
    suspend fun getTaste(category: String, value: String): TasteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaste(taste: TasteEntity)

    @Query("UPDATE taste_profile SET weight = weight + :amount, updatedAt = :time WHERE id = :id")
    suspend fun updateTasteWeight(id: Int, amount: Int, time: Long)

    @Query("DELETE FROM taste_profile")
    suspend fun clearTastes()
}

@Dao
interface HadithCardDao {
    @Query("SELECT * FROM hadith_cards ORDER BY createdAt DESC")
    fun getAllHadithCards(): Flow<List<HadithCardEntity>>

    @Query("SELECT * FROM hadith_cards WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteCards(): Flow<List<HadithCardEntity>>

    @Query("SELECT * FROM hadith_cards WHERE id = :id")
    suspend fun getCardById(id: String): HadithCardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: HadithCardEntity)

    @Update
    suspend fun updateCard(card: HadithCardEntity)

    @Query("DELETE FROM hadith_cards WHERE id = :id")
    suspend fun deleteCardById(id: String)
}

@Dao
interface AudioTrackDao {
    @Query("SELECT * FROM audio_tracks ORDER BY createdAt DESC")
    fun getAllAudioTracks(): Flow<List<AudioTrackEntity>>

    @Query("SELECT * FROM audio_tracks WHERE typeCategory = :category ORDER BY createdAt DESC")
    fun getTracksByCategory(category: String): Flow<List<AudioTrackEntity>>

    @Query("SELECT * FROM audio_tracks WHERE id = :id")
    suspend fun getTrackById(id: String): AudioTrackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: AudioTrackEntity)

    @Query("DELETE FROM audio_tracks WHERE id = :id")
    suspend fun deleteTrackById(id: String)
}

@Dao
interface ReelScriptDao {
    @Query("SELECT * FROM reel_scripts ORDER BY createdAt DESC")
    fun getAllReelScripts(): Flow<List<ReelScriptEntity>>

    @Query("SELECT * FROM reel_scripts WHERE id = :id")
    suspend fun getScriptById(id: String): ReelScriptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScript(script: ReelScriptEntity)

    @Query("DELETE FROM reel_scripts WHERE id = :id")
    suspend fun deleteScriptById(id: String)
}

@Dao
interface SeriesDao {
    @Query("SELECT * FROM series_records ORDER BY updatedAt DESC")
    fun getAllSeries(): Flow<List<SeriesEntity>>

    @Query("SELECT * FROM series_records WHERE id = :id")
    suspend fun getSeriesById(id: String): SeriesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeries(series: SeriesEntity)

    @Update
    suspend fun updateSeries(series: SeriesEntity)

    @Query("DELETE FROM series_records WHERE id = :id")
    suspend fun deleteSeriesById(id: String)
}

@Database(
    entities = [
        ProjectEntity::class,
        TasteEntity::class,
        HadithCardEntity::class,
        AudioTrackEntity::class,
        ReelScriptEntity::class,
        SeriesEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun tasteDao(): TasteDao
    abstract fun hadithCardDao(): HadithCardDao
    abstract fun audioTrackDao(): AudioTrackDao
    abstract fun reelScriptDao(): ReelScriptDao
    abstract fun seriesDao(): SeriesDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "qabas_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

class ProjectRepository(private val projectDao: ProjectDao) {
    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()

    fun getRecentProjects(limit: Int = 10): Flow<List<ProjectEntity>> =
        projectDao.getRecentProjects(limit)

    suspend fun getAllProjectsOnce(): List<ProjectEntity> =
        projectDao.getAllProjectsOnce()

    suspend fun getRecentProjectsOnce(limit: Int = 10): List<ProjectEntity> =
        projectDao.getRecentProjectsOnce(limit)

    suspend fun getProject(id: String) = projectDao.getProjectById(id)
    suspend fun insert(project: ProjectEntity) = projectDao.insertProject(project)
    suspend fun insertAll(projects: List<ProjectEntity>) {
        if (projects.isNotEmpty()) projectDao.insertProjects(projects)
    }
    suspend fun update(project: ProjectEntity) = projectDao.updateProject(project)
    suspend fun delete(id: String) = projectDao.deleteProjectById(id)
    suspend fun clearAll() = projectDao.clearAllProjects()
}

class SeriesRepository(private val seriesDao: SeriesDao) {
    val allSeries: Flow<List<SeriesEntity>> = seriesDao.getAllSeries()

    suspend fun getSeries(id: String) = seriesDao.getSeriesById(id)
    suspend fun insert(series: SeriesEntity) = seriesDao.insertSeries(series)
    suspend fun update(series: SeriesEntity) = seriesDao.updateSeries(series)
    suspend fun delete(id: String) = seriesDao.deleteSeriesById(id)
}

class TasteRepository(private val tasteDao: TasteDao) {
    val allTastesFlow: Flow<List<TasteEntity>> = tasteDao.getAllTastesFlow()

    suspend fun getAllTastes() = tasteDao.getAllTastes()

    suspend fun registerPreference(category: String, value: String, amount: Int = 1) {
        val existing = tasteDao.getTaste(category, value)
        val now = System.currentTimeMillis()
        if (existing != null) {
            tasteDao.updateTasteWeight(existing.id, amount, now)
        } else {
            tasteDao.insertTaste(
                TasteEntity(
                    category = category,
                    preferredValue = value,
                    weight = amount,
                    updatedAt = now
                )
            )
        }
    }

    suspend fun clearAll() = tasteDao.clearTastes()
}

class HadithCardRepository(private val hadithCardDao: HadithCardDao) {
    val allCards: Flow<List<HadithCardEntity>> = hadithCardDao.getAllHadithCards()
    val favoriteCards: Flow<List<HadithCardEntity>> = hadithCardDao.getFavoriteCards()

    suspend fun getCard(id: String) = hadithCardDao.getCardById(id)
    suspend fun insert(card: HadithCardEntity) = hadithCardDao.insertCard(card)
    suspend fun update(card: HadithCardEntity) = hadithCardDao.updateCard(card)
    suspend fun delete(id: String) = hadithCardDao.deleteCardById(id)
}

class AudioTrackRepository(private val audioTrackDao: AudioTrackDao) {
    val allTracks: Flow<List<AudioTrackEntity>> = audioTrackDao.getAllAudioTracks()

    fun getCategoryTracks(category: String): Flow<List<AudioTrackEntity>> =
        audioTrackDao.getTracksByCategory(category)

    suspend fun getTrack(id: String) = audioTrackDao.getTrackById(id)
    suspend fun insert(track: AudioTrackEntity) = audioTrackDao.insertTrack(track)
    suspend fun delete(id: String) = audioTrackDao.deleteTrackById(id)
}

class ReelScriptRepository(private val reelScriptDao: ReelScriptDao) {
    val allScripts: Flow<List<ReelScriptEntity>> = reelScriptDao.getAllReelScripts()

    suspend fun getScript(id: String) = reelScriptDao.getScriptById(id)
    suspend fun insert(script: ReelScriptEntity) = reelScriptDao.insertScript(script)
    suspend fun delete(id: String) = reelScriptDao.deleteScriptById(id)
}
