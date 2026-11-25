package com.galindo.avisoscampus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.galindo.avisoscampus.data.AvisosRepository
import com.galindo.avisoscampus.model.Aviso
import com.galindo.avisoscampus.model.PermisoUi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AvisoEvento {
    data class MostrarMensaje(val mensaje: String) : AvisoEvento
    data class PermisoSolicitado(val concedido: Boolean) : AvisoEvento
}

data class AvisosUiState(
    val avisos: List<Aviso> = emptyList(),
    val permisoUi: PermisoUi = PermisoUi(),
    val estaCargando: Boolean = false
)

class AvisosViewModel(
    private val repository: AvisosRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AvisosUiState())
    val uiState: StateFlow<AvisosUiState> = _uiState.asStateFlow()

    private val _eventos = MutableSharedFlow<AvisoEvento>()
    val eventos: SharedFlow<AvisoEvento> = _eventos.asSharedFlow()

    init {
        cargarAvisos()
    }

    fun cargarAvisos() {
        viewModelScope.launch {
            _uiState.update { it.copy(estaCargando = true) }
            repository.obtenerAvisos().collect { lista ->
                _uiState.update { it.copy(avisos = lista, estaCargando = false) }
            }
        }
    }

    fun seleccionarAviso(aviso: Aviso) {
        viewModelScope.launch {
            _eventos.emit(AvisoEvento.MostrarMensaje("Aviso seleccionado: ${aviso.titulo}"))
        }
    }

    fun actualizarEstadoPermiso(concedido: Boolean) {
        _uiState.update {
            it.copy(permisoUi = it.permisoUi.copy(concedido = concedido, debeSolicitar = !concedido))
        }
        viewModelScope.launch {
            _eventos.emit(AvisoEvento.PermisoSolicitado(concedido))
            val msg = if (concedido) "Notificaciones activadas" else "Notificaciones desactivadas"
            _eventos.emit(AvisoEvento.MostrarMensaje(msg))
        }
    }
}
