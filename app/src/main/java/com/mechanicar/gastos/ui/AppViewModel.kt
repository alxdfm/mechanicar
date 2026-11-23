package com.mechanicar.gastos.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mechanicar.gastos.GastosCorsaApp
import com.mechanicar.gastos.data.DashboardStats
import com.mechanicar.gastos.data.ExpenseRepository
import com.mechanicar.gastos.data.ImportResult
import com.mechanicar.gastos.data.ServiceDraft
import com.mechanicar.gastos.data.SpreadsheetImporter
import com.mechanicar.gastos.data.VehicleEntity
import com.mechanicar.gastos.data.VisitEntity
import com.mechanicar.gastos.data.VisitWithServices
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class AppViewModel(
    application: Application,
    private val repository: ExpenseRepository
) : AndroidViewModel(application) {

    val vehicle: StateFlow<VehicleEntity?> = repository.observeVehicle()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val visits: StateFlow<List<VisitWithServices>> = repository.observeVisits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val dashboard: StateFlow<DashboardStats> = repository.observeDashboard()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            DashboardStats(0.0, 0, 0, emptyList(), emptyList(), null, null)
        )

    private val _message = MutableSharedFlow<String>()
    val message = _message.asSharedFlow()

    private val _importing = MutableStateFlow(false)
    val importing = _importing.asStateFlow()

    fun visitFlow(id: Long) = repository.observeVisit(id)

    fun saveVehicle(vehicle: VehicleEntity, onSaved: () -> Unit) {
        viewModelScope.launch {
            repository.saveVehicle(vehicle)
            _message.emit("Dados do veículo salvos.")
            onSaved()
        }
    }

    fun saveVisit(
        visitId: Long?,
        date: LocalDate,
        odometerKm: Int?,
        origin: String,
        workshop: String,
        notes: String,
        services: List<ServiceDraft>,
        onSaved: (Long) -> Unit
    ) {
        viewModelScope.launch {
            if (services.isEmpty()) {
                _message.emit("Adicione pelo menos um serviço.")
                return@launch
            }
            val id = repository.saveVisit(visitId, date, odometerKm, origin, workshop, notes, services)
            _message.emit("Manutenção salva.")
            onSaved(id)
        }
    }

    fun deleteVisit(visit: VisitEntity) {
        viewModelScope.launch {
            repository.deleteVisit(visit)
            _message.emit("Manutenção removida.")
        }
    }

    fun importFromUri(uri: Uri, replaceExisting: Boolean) {
        viewModelScope.launch {
            _importing.value = true
            try {
                val result = withContext(Dispatchers.IO) {
                    val rows = SpreadsheetImporter.importFromUri(getApplication(), uri)
                    if (rows.isEmpty()) null else repository.importRows(rows, replaceExisting)
                }
                if (result == null) {
                    _message.emit("Nenhum lançamento válido encontrado na planilha.")
                } else {
                    emitImportSuccess(result)
                }
            } catch (ex: Throwable) {
                _message.emit("Falha ao importar: ${ex.message ?: ex.javaClass.simpleName}")
            } finally {
                _importing.value = false
            }
        }
    }

    fun importSeedData(replaceExisting: Boolean) {
        viewModelScope.launch {
            _importing.value = true
            try {
                val result = withContext(Dispatchers.IO) {
                    val rows = SpreadsheetImporter.importFromAssets(getApplication())
                    repository.importRows(rows, replaceExisting)
                }
                emitImportSuccess(result)
            } catch (ex: Throwable) {
                _message.emit("Falha ao carregar dados iniciais: ${ex.message ?: ex.javaClass.simpleName}")
            } finally {
                _importing.value = false
            }
        }
    }

    private suspend fun emitImportSuccess(result: ImportResult) {
        _message.emit(
            "Importação concluída: ${result.visitsCreated} manutenções e ${result.servicesCreated} serviços."
        )
    }

    companion object {
        fun factory(app: GastosCorsaApp): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AppViewModel(app, app.repository) as T
                }
            }
    }
}
