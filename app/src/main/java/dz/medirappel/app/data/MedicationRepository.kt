package dz.medirappel.app.data

import kotlinx.coroutines.flow.Flow

/** Single entry point for ViewModels. All data stays on-device. */
class MedicationRepository(private val db: AppDatabase) {

    fun observeMedications(): Flow<List<MedicationEntity>> = db.medicationDao().observeAll()
    fun observeHistory(limit: Int = 200): Flow<List<DoseLogEntity>> = db.doseLogDao().observeRecent(limit)

    suspend fun save(name: String, dosage: String, timesCsv: String, notes: String, id: Long = 0): Long {
        val clean = timesCsv.split(",").map { it.trim() }
            .filter { it.matches(Regex("\\d{2}:\\d{2}")) }.distinct().sorted()
            .joinToString(",")
        require(name.isNotBlank()) { "name required" }
        require(clean.isNotEmpty()) { "at least one time required" }
        return if (id == 0L) {
            db.medicationDao().upsert(MedicationEntity(name = name.trim(), dosage = dosage.trim(), timesCsv = clean, notes = notes.trim()))
        } else {
            val existing = db.medicationDao().getById(id) ?: error("not found")
            val updated = existing.copy(name = name.trim(), dosage = dosage.trim(), timesCsv = clean, notes = notes.trim(), active = true)
            db.medicationDao().update(updated)
            id
        }
    }

    suspend fun delete(med: MedicationEntity) = db.medicationDao().delete(med)

    suspend fun setActive(med: MedicationEntity, active: Boolean) =
        db.medicationDao().update(med.copy(active = active))

    suspend fun logDose(medId: Long, name: String, time: String, day: String, status: String, takenAt: Long? = System.currentTimeMillis()) {
        db.doseLogDao().insert(
            DoseLogEntity(
                medicationId = medId, medicationName = name,
                scheduledDay = day, scheduledTime = time,
                takenAt = if (status == "TAKEN") takenAt else null, status = status
            )
        )
    }

    suspend fun weekAdherence(): Int {
        val since = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        val taken = db.doseLogDao().countTakenSince(since)
        val total = db.doseLogDao().countAllSince(since)
        if (total == 0) return 100
        return ((taken * 100.0) / total).toInt().coerceIn(0, 100)
    }
}
