package dz.medirappel.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** One medication with its daily intake times stored as "HH:mm" strings. */
@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dosage: String,
    /** Comma-separated "HH:mm" times, e.g. "08:00,20:30". */
    val timesCsv: String,
    val notes: String = "",
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun times(): List<String> = timesCsv.split(",").map { it.trim() }.filter { it.matches(Regex("\\d{2}:\\d{2}")) }.sorted()
}

@Entity(
    tableName = "dose_logs",
    indices = [Index(value = ["medicationId", "scheduledDay", "scheduledTime"])]
)
data class DoseLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Long,
    val medicationName: String,
    /** "yyyy-MM-dd" in device locale. */
    val scheduledDay: String,
    /** "HH:mm" that was scheduled. */
    val scheduledTime: String,
    val takenAt: Long?,
    /** TAKEN, SKIPPED or SNOOZED. */
    val status: String,
    val loggedAt: Long = System.currentTimeMillis()
)

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getById(id: Long): MedicationEntity?

    @Query("SELECT * FROM medications WHERE active = 1 ORDER BY name COLLATE NOCASE")
    suspend fun getAllActive(): List<MedicationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(med: MedicationEntity): Long

    @Update
    suspend fun update(med: MedicationEntity)

    @Delete
    suspend fun delete(med: MedicationEntity)
}

@Dao
interface DoseLogDao {
    @Query("SELECT * FROM dose_logs ORDER BY loggedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 200): Flow<List<DoseLogEntity>>

    @Query("SELECT * FROM dose_logs WHERE scheduledDay = :day ORDER BY scheduledTime")
    suspend fun getForDay(day: String): List<DoseLogEntity>

    @Insert
    suspend fun insert(log: DoseLogEntity)

    @Query("SELECT COUNT(*) FROM dose_logs WHERE status = 'TAKEN' AND loggedAt >= :since")
    suspend fun countTakenSince(since: Long): Int

    @Query("SELECT COUNT(*) FROM dose_logs WHERE loggedAt >= :since")
    suspend fun countAllSince(since: Long): Int
}
