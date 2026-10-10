package dz.medirappel.app

import android.app.Application
import androidx.room.Room
import dz.medirappel.app.data.AppDatabase
import dz.medirappel.app.data.MedicationRepository

class MainApplication : Application() {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "medirappel.db").build()
    }
    val repository: MedicationRepository by lazy { MedicationRepository(database) }
}
