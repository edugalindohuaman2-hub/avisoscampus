# PROMPTS.md — Registro de Prompts y Validaciones Arquitectónicas

Este documento registra los últimos 3 prompts de desarrollo utilizados en la aplicación **AvisosCampus** (`com.galindo.avisoscampus`), las respuestas de la IA (Gemini), las decisiones tomadas y la justificación técnica de las validaciones arquitectónicas.

---

## 📋 Tabla de Registro de Prompts

| Prompt que escribí | Qué respondió Gemini | Qué cambié o descarté | Por qué (Justificación Arquitectónica/Técnica) |
| :--- | :--- | :--- | :--- |
| *"En com.galindo.avisoscampus, mejora Notificador.kt y AvisosApp.kt agregando un segundo canal const val CANAL_URGENTES = "urgentes" llamado 'Urgentes' con NotificationManager.IMPORTANCE_HIGH en crearCanal(context). En mostrar(context, aviso), usa CANAL_URGENTES y PRIORITY_HIGH cuando aviso.origen == Origen.RECORDATORIO, y CANAL con PRIORITY_DEFAULT para los demás. Añade .setStyle(NotificationCompat.BigTextStyle().bigText(aviso.texto)) al builder. No agregues Context ni NotificationManager en AvisosViewModel.kt."* | Se registraron los canales (`CANAL` y `CANAL_URGENTES`) en `Notificador.kt` e invocados en `AvisosApp.kt`. Se aplicó la selección de canal y prioridad según el origen `RECORDATORIO` y se añadió `BigTextStyle`. | Se verificó que `AvisosViewModel` permaneciera libre de referencias a `Context` o `NotificationManager`. | Mantiene el principio de MVVM: los ViewModels no deben importar ni interactuar directamente con clases del sistema Android (`NotificationManager`, `Context`), garantizando pruebas unitarias puras en la JVM. |
| *"En com.galindo.avisoscampus, implementa la detección de conexión/desconexión del cargador siguiendo el patrón MVVM: 1) Agrega CARGADOR("Cargador") a Origen en model/Modelos.kt. 2) Crea receiver/CargadorReceiver.kt escuchando Intent.ACTION_POWER_CONNECTED (true) y Intent.ACTION_POWER_DISCONNECTED (false) con onReceive corto. 3) En AvisosViewModel.kt (sin Context ni BroadcastReceiver), agrega escuchandoCargador: StateFlow<Boolean>, cambiarEscuchaCargador(valor) y alCambiarCargador(conectado) registrando ('Cargador', if (conectado) "Conectado" else "Desconectado", Origen.CARGADOR). 4) En PantallaAvisos.kt, agrega el Switch 'Escuchar el cargador' y regístralo en DisposableEffect con RECEIVER_NOT_EXPORTED, liberándolo en onDispose."* | Se creó `CargadorReceiver`, el origen `CARGADOR`, las propiedades en `AvisosViewModel` y la suscripción reactiva en `PantallaAvisos.kt` usando `DisposableEffect` con `RECEIVER_NOT_EXPORTED`. | Se validó la desinicialización (`unregisterReceiver`) en el bloque `onDispose`. | La bandera `RECEIVER_NOT_EXPORTED` es obligatoria desde API 34 para receptores dinámicos no públicos, evitando vulnerabilidades de seguridad. `DisposableEffect` asegura desregistrar el receptor liberando recursos cuando la UI abandona la composición. |
| *"En com.galindo.avisoscampus, crea AvisosRepositoryLocal(context: Context) : AvisosRepository en data/AvisosRepository.kt que persista los avisos en SharedPreferences serializados como JSON (org.json.JSONArray/JSONObject). Actualiza AvisosViewModelFactory.kt y MainActivity.kt para inyectar AvisosRepositoryLocal(context.applicationContext) sin que AvisosViewModel reciba Context."* | Se implementó `AvisosRepositoryLocal` persisiendo avisos en `SharedPreferences` con `JSONArray`/`JSONObject`, inyectándose desde `MainActivity` con `applicationContext` vía `AvisosViewModelFactory`. | Se utilizó exclusivamente `applicationContext` al instanciar el repositorio local. | `applicationContext` evita retener el contexto del `Activity`, previniendo fugas de memoria (*memory leaks*). Abstraer la fuente de datos bajo `AvisosRepository` permite cambiar la capa de datos sin modificar la lógica del ViewModel. |

---

## 🏛️ Resumen de Validaciones Arquitectónicas Realizadas

1. **Desacoplamiento del ViewModel**:
   - `AvisosViewModel` se mantiene 100% puro en Kotlin sin importar clases de Android como `Context`, `NotificationManager` o `BroadcastReceiver`.
2. **Ciclo de Vida y Seguridad con Jetpack Compose**:
   - El registro del receptor `CargadorReceiver` se realiza reactivamente mediante `DisposableEffect` asegurando su liberación explícita en `onDispose`.
   - Se aplican las banderas de seguridad `RECEIVER_NOT_EXPORTED` en API 33+ y `PendingIntent.FLAG_IMMUTABLE` en API 31+.
3. **Persistencia Abstraída (Patrón Repository)**:
   - La persistencia en `SharedPreferences` con serialización JSON queda confinada en `AvisosRepositoryLocal`, respetando el contrato de la interfaz `AvisosRepository`.
