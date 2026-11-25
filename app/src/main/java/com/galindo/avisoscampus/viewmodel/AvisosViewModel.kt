package com.galindo.avisoscampus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.galindo.avisoscampus.data.AvisosRepository
import com.galindo.avisoscampus.model.Aviso
import com.galindo.avisoscampus.model.EstadoPermiso
import com.galindo.avisoscampus.model.Origen
import com.galindo.avisoscampus.model.PermisoUi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    data class MostrarNotificacion(val aviso: Aviso) : AvisoEvento
}

data class AvisosUiState(
    val avisos: List<Aviso> = emptyList(),
    val permisoUi: PermisoUi = PermisoUi(),
    val estaCargando: Boolean = false,
)

class AvisosViewModel(
    private val repository: AvisosRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AvisosUiState())
    val uiState: StateFlow<AvisosUiState> = _uiState.asStateFlow()

    private val _avisos = MutableStateFlow<List<Aviso>>(emptyList())
    val avisos: StateFlow<List<Aviso>> = _avisos.asStateFlow()

    private val _permiso = MutableStateFlow(EstadoPermiso.SIN_SOLICITAR)
    val permiso: StateFlow<EstadoPermiso> = _permiso.asStateFlow()

    private val _eventos = MutableSharedFlow<AvisoEvento>()
    val eventos: SharedFlow<AvisoEvento> = _eventos.asSharedFlow()

    private var trabajo: Job? = null
    private val _faltan = MutableStateFlow<Int?>(null)
    val faltan: StateFlow<Int?> = _faltan.asStateFlow()

    private val _escuchandoAvion = MutableStateFlow(false)
    val escuchandoAvion: StateFlow<Boolean> = _escuchandoAvion.asStateFlow()

    init {
        cargarAvisos()
    }

    fun cargarAvisos() {
        viewModelScope.launch {
            _uiState.update { it.copy(estaCargando = true) }
            repository.obtenerAvisos().collect { lista ->
                _avisos.value = lista
                _uiState.update { it.copy(avisos = lista, estaCargando = false) }
            }
        }
    }

    fun seleccionarAviso(aviso: Aviso) {
        viewModelScope.launch {
            _eventos.emit(AvisoEvento.MostrarMensaje("Aviso seleccionado: ${aviso.titulo}"))
        }
    }

    fun permisoRespondido(concedido: Boolean) {
        val nuevoEstado = if (concedido) EstadoPermiso.CONCEDIDO else EstadoPermiso.DENEGADO
        _permiso.value = nuevoEstado
        _uiState.update {
            it.copy(
                permisoUi = it.permisoUi.copy(
                    concedido = concedido,
                    debeSolicitar = !concedido,
                    estado = nuevoEstado,
                ),
            )
        }
        viewModelScope.launch {
            _eventos.emit(AvisoEvento.PermisoSolicitado(concedido))
            val msg = if (concedido) "Notificaciones permitidas" else "Permiso de notificaciones denegado"
            _eventos.emit(AvisoEvento.MostrarMensaje(msg))
        }
    }

    fun actualizarEstadoPermiso(concedido: Boolean) {
        permisoRespondido(concedido)
    }

    fun avisarAhora() {
        viewModelScope.launch {
            val nuevoAviso = Aviso(
                id = System.currentTimeMillis().toString(),
                titulo = "Tarea lista",
                contenido = "Tu entrega fue registrada",
                fecha = "Ahora",
                origen = Origen.BOTON,
                esImportante = true,
            )
            repository.agregarAviso(nuevoAviso)
            _eventos.emit(AvisoEvento.MostrarNotificacion(nuevoAviso))
        }
    }

    fun recordarEn(segundos: Int = 10) {
        if (trabajo?.isActive == true) return

        trabajo = viewModelScope.launch {
            try {
                for (i in segundos downTo 1) {
                    _faltan.value = i
                    delay(1000)
                }
                val nuevoAviso = Aviso(
                    id = System.currentTimeMillis().toString(),
                    titulo = "Recordatorio",
                    contenido = "Revisa tu laboratorio pendiente",
                    fecha = "Ahora",
                    origen = Origen.RECORDATORIO,
                    esImportante = true,
                )
                repository.agregarAviso(nuevoAviso)
                _eventos.emit(AvisoEvento.MostrarNotificacion(nuevoAviso))
            } finally {
                _faltan.value = null
                trabajo = null
            }
        }
    }

    fun cancelarRecordatorio() {
        trabajo?.cancel()
    }

    fun cambiarEscucha(valor: Boolean) {
        _escuchandoAvion.value = valor
    }

    fun alCambiarModoAvion(activo: Boolean) {
        viewModelScope.launch {
            val mensajeState = if (activo) "Activado" else "Desactivado"
            val nuevoAviso = Aviso(
                id = System.currentTimeMillis().toString(),
                titulo = "Modo avión",
                contenido = mensajeState,
                fecha = "Ahora",
                origen = Origen.MODO_AVION,
                esImportante = true,
            )
            repository.agregarAviso(nuevoAviso)
            _eventos.emit(AvisoEvento.MostrarNotificacion(nuevoAviso))
        }
    }
}
