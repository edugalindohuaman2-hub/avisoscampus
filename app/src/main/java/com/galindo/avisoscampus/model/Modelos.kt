package com.galindo.avisoscampus.model

enum class Origen(val etiqueta: String) {
    ACADEMICO("Académico"),
    ADMINISTRATIVO("Administrativo"),
    EVENTOS("Eventos"),
    GENERAL("General"),
    BOTON("Botón"),
    RECORDATORIO("Recordatorio"),
    MODO_AVION("Modo avión"),
}

data class Aviso(
    val id: String,
    val titulo: String,
    val contenido: String,
    val fecha: String,
    val origen: Origen,
    val esImportante: Boolean = false,
) {
    val texto: String get() = contenido
}

enum class EstadoPermiso(val etiqueta: String) {
    SIN_SOLICITAR("Sin solicitar"),
    CONCEDIDO("Concedido"),
    DENEGADO("Denegado"),
}

data class PermisoUi(
    val concedido: Boolean = false,
    val debeSolicitar: Boolean = true,
    val mostrarRacional: Boolean = false,
    val estado: EstadoPermiso = EstadoPermiso.SIN_SOLICITAR,
)
