package dz.medirappel.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dz.medirappel.app.MainApplication
import dz.medirappel.app.data.DoseLogEntity
import dz.medirappel.app.data.MedicationEntity
import dz.medirappel.app.data.MedicationRepository
import dz.medirappel.app.domain.ScheduleCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MedViewModel(val repo: MedicationRepository) : ViewModel() {
    val medications = repo.observeMedications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val history = repo.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList< DoseLogEntity>())

    fun save(name: String, dosage: String, times: String, notes: String, id: Long = 0, onDone: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val savedId = repo.save(name, dosage, times, notes, id)
            onDone(savedId)
        }
    }

    fun delete(med: MedicationEntity) = viewModelScope.launch { repo.delete(med) }
    fun setActive(med: MedicationEntity, active: Boolean) = viewModelScope.launch { repo.setActive(med, active) }

    fun logNow(med: MedicationEntity, time: String, status: String) = viewModelScope.launch {
        repo.logDose(med.id, med.name, time, ScheduleCalculator.dayKey(), status)
    }

    companion object {
        fun factory(app: MainApplication): ViewModelProvider.Factory = viewModelFactory {
            initializer { MedViewModel(app.repository) }
        }
    }
}
