package com.galindo.avisoscampus.data

import android.content.Context
import android.content.SharedPreferences
import com.galindo.avisoscampus.model.Aviso
import com.galindo.avisoscampus.model.Origen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

interface AvisosRepository {
    fun obtenerAvisos(): Flow<List<Aviso>>
    suspend fun agregarAviso(aviso: Aviso)
    suspend fun obtenerAvisoPorId(id: String): Aviso?
}

class AvisosRepositoryMemoria : AvisosRepository {
    private val _avisos = MutableStateFlow(
        listOf(
            Aviso(
                id = "1",
                titulo = "Inicio de Inscripciones 2026",
                contenido = "El proceso de inscripción para el ciclo 2026-II estará abierto desde el 1 de Diciembre en la plataforma virtual.",
                fecha = "25 Nov 2026",
                origen = Origen.ACADEMICO,
                esImportante = true,
            ),
            Aviso(
                id = "2",
                titulo = "Mantenimiento del Portal Estudiantil",
                contenido = "El servidor de servicios académicos estará en mantenimiento programado este domingo de 02:00 a 06:00 AM.",
                fecha = "24 Nov 2026",
                origen = Origen.ADMINISTRATIVO,
                esImportante = false,
            ),
            Aviso(
                id = "3",
                titulo = "Feria de Ciencias y Tecnología 2026",
                contenido = "Invitamos a toda la comunidad universitaria a participar en los talleres e inscripciones de proyectos de innovación.",
                fecha = "22 Nov 2026",
                origen = Origen.EVENTOS,
                esImportante = true,
            ),
            Aviso(
                id = "4",
                titulo = "Protocolo de Acceso al Campus",
                contenido = "Recuerda presentar tu credencial universitaria digital o física para ingresar por las puertas principales.",
                fecha = "20 Nov 2026",
                origen = Origen.GENERAL,
                esImportante = false,
            ),
        ),
    )

    override fun obtenerAvisos(): Flow<List<Aviso>> = _avisos.asStateFlow()

    override suspend fun agregarAviso(aviso: Aviso) {
        _avisos.value = _avisos.value + aviso
    }

    override suspend fun obtenerAvisoPorId(id: String): Aviso? {
        return _avisos.value.find { it.id == id }
    }
}

class AvisosRepositoryLocal(context: Context) : AvisosRepository {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("avisos_prefs", Context.MODE_PRIVATE)

    private val _avisos = MutableStateFlow<List<Aviso>>(emptyList())

    init {
        val avisosGuardados = cargarAvisosDesdePrefs()
        if (avisosGuardados.isEmpty()) {
            val iniciales = listOf(
                Aviso(
                    id = "1",
                    titulo = "Inicio de Inscripciones 2026",
                    contenido = "El proceso de inscripción para el ciclo 2026-II estará abierto desde el 1 de Diciembre en la plataforma virtual.",
                    fecha = "25 Nov 2026",
                    origen = Origen.ACADEMICO,
                    esImportante = true,
                ),
                Aviso(
                    id = "2",
                    titulo = "Mantenimiento del Portal Estudiantil",
                    contenido = "El servidor de servicios académicos estará en mantenimiento programado este domingo de 02:00 a 06:00 AM.",
                    fecha = "24 Nov 2026",
                    origen = Origen.ADMINISTRATIVO,
                    esImportante = false,
                ),
                Aviso(
                    id = "3",
                    titulo = "Feria de Ciencias y Tecnología 2026",
                    contenido = "Invitamos a toda la comunidad universitaria a participar en los talleres e inscripciones de proyectos de innovación.",
                    fecha = "22 Nov 2026",
                    origen = Origen.EVENTOS,
                    esImportante = true,
                ),
                Aviso(
                    id = "4",
                    titulo = "Protocolo de Acceso al Campus",
                    contenido = "Recuerda presentar tu credencial universitaria digital o física para ingresar por las puertas principales.",
                    fecha = "20 Nov 2026",
                    origen = Origen.GENERAL,
                    esImportante = false,
                ),
            )
            _avisos.value = iniciales
            guardarAvisosEnPrefs(iniciales)
        } else {
            _avisos.value = avisosGuardados
        }
    }

    private fun cargarAvisosDesdePrefs(): List<Aviso> {
        val jsonString = prefs.getString("key_avisos", null) ?: return emptyList()
        val lista = mutableListOf<Aviso>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val origenEnum = try {
                    Origen.valueOf(obj.getString("origen"))
                } catch (e: Exception) {
                    Origen.GENERAL
                }
                lista.add(
                    Aviso(
                        id = obj.getString("id"),
                        titulo = obj.getString("titulo"),
                        contenido = obj.getString("contenido"),
                        fecha = obj.getString("fecha"),
                        origen = origenEnum,
                        esImportante = obj.optBoolean("esImportante", false),
                    ),
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return lista
    }

    private fun guardarAvisosEnPrefs(lista: List<Aviso>) {
        val jsonArray = JSONArray()
        for (aviso in lista) {
            val obj = JSONObject().apply {
                put("id", aviso.id)
                put("titulo", aviso.titulo)
                put("contenido", aviso.contenido)
                put("fecha", aviso.fecha)
                put("origen", aviso.origen.name)
                put("esImportante", aviso.esImportante)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString("key_avisos", jsonArray.toString()).apply()
    }

    override fun obtenerAvisos(): Flow<List<Aviso>> = _avisos.asStateFlow()

    override suspend fun agregarAviso(aviso: Aviso) {
        val nuevaLista = _avisos.value + aviso
        _avisos.value = nuevaLista
        guardarAvisosEnPrefs(nuevaLista)
    }

    override suspend fun obtenerAvisoPorId(id: String): Aviso? {
        return _avisos.value.find { it.id == id }
    }
}
