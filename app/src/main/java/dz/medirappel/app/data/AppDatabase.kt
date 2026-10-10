package dz.medirappel.app.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [MedicationEntity::class, DoseLogEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun doseLogDao(): DoseLogDao
}
