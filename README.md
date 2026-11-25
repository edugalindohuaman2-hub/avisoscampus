# AvisosCampus (`com.galindo.avisoscampus`)

**AvisosCampus** es una aplicación nativa Android desarrollada en **Kotlin** con **Jetpack Compose** y **Material 3** (100% declarativo, sin layouts XML). Implementa una arquitectura moderna **MVVM + Repository**, gestión de notificaciones locales, canales de notificación en la clase `Application`, receptores de broadcast del sistema (`BroadcastReceiver`) y corrutinas de Kotlin para cuentas regresivas y tareas en segundo plano.

---

## 📁 Estructura del Proyecto y Árbol de Paquetes

```text
com.galindo.avisoscampus
│
├── AvisosApp.kt                      # Clase Application (inicialización del canal de notificaciones)
├── MainActivity.kt                   # Actividad principal (punto de entrada Compose)
│
├── data/                             # Capa de Datos
│   └── AvisosRepository.kt           # Interfaz de repositorio e implementación en memoria (AvisosRepositoryMemoria)
│
├── model/                            # Modelos de Dominio y Estado
│   └── Modelos.kt                    # Entidades: Origen, Aviso, EstadoPermiso y PermisoUi
│
├── notificacion/                     # Gestión de Notificaciones del Sistema
│   └── Notificador.kt                # Creación de canales, verificación de permisos y envío de notificaciones
│
├── receiver/                         # Receptores de Broadcast del Sistema
│   └── ModoAvionReceiver.kt          # BroadcastReceiver para detectar cambios en Intent.ACTION_AIRPLANE_MODE_CHANGED
│
├── viewmodel/                        # Lógica de Presentación (MVVM)
│   ├── AvisosViewModel.kt            # ViewModel con StateFlow, SharedFlow y gestión de corrutinas
│   └── AvisosViewModelFactory.kt     # Factory para la inyección de dependencias del repositorio
│
└── ui/                               # Capa de Interfaz de Usuario (Jetpack Compose)
    ├── HistorialAvisos.kt            # Composable para mostrar el historial de avisos recibidos
    ├── PantallaAvisos.kt             # Pantalla principal con tarjetas de acciones, permisos y avisos
    └── theme/                        # Tema visual con Material 3
        ├── Color.kt
        ├── Theme.kt
        └── Type.kt
```

---

## 🏗️ Diagrama de Arquitectura MVVM + Repository

```mermaid
graph TD
    subgraph UI ["Capa de Interfaz de Usuario (UI Layer)"]
        A[PantallaAvisos.kt / HistorialAvisos.kt]
    end

    subgraph ViewModelLayer ["Capa de Presentación (ViewModel Layer)"]
        B[AvisosViewModel.kt]
        C[StateFlow: uiState, avisos, permiso, faltan, mensaje, escuchandoAvion]
        D[SharedFlow: eventos]
    end

    subgraph DataLayer ["Capa de Datos (Data Layer)"]
        E[AvisosRepository Interfaz]
        F[AvisosRepositoryMemoria]
    end

    subgraph SystemLayer ["Servicios del Sistema Android"]
        G[Notificador.kt - NotificationManager]
        H[ModoAvionReceiver.kt - BroadcastReceiver]
    end

    A -->|Observa estados con collectAsState| C
    A -->|Recolecta eventos efímeros| D
    A -->|Invoca intenciones de usuario| B
    B -->|Actualiza estados| C
    B -->|Emite eventos| D
    B -->|Consulta y persiste avisos| E
    E <|.. F
    A -.->|Registra receptor dinámico| H
    A -.->|Solicita mostrar notificación| G
    H -.->|Notifica cambio de estado| B
```

---

## 📷 Capturas de Ejecución

### 1. Interfaz Principal y Recordatorio

![Interfaz Principal y Recordatorio](file:///C:/Users/User/AvisosCampus/capturas/recordatorio.png)

---

### 2. Notificaciones en Panel

![Notificaciones en Panel](file:///C:/Users/User/AvisosCampus/capturas/notificacion_panel.png)

---

### 3. Modo Avión Detectado

![Modo Avión Detectado](file:///C:/Users/User/AvisosCampus/capturas/modo_avion.png)

---

### 4. Historial de Notificaciones Acumulado

![Historial de Notificaciones](file:///C:/Users/User/AvisosCampus/capturas/historial.png)

---

### 5. Permiso Denegado

![Permiso Denegado](file:///C:/Users/User/AvisosCampus/capturas/permiso_denegado.png)

---

## 🧠 Sección "Para Pensar" — Respuestas Técnicas

### 1. ¿Por qué el ViewModel no debe guardar ni usar directamente una referencia al Context de Android?
**Respuesta**:  
Guardar o hacer referencia directa a un `Context` de un `Activity` en un `ViewModel` causa **fugas de memoria (memory leaks)**. El ciclo de vida de un `ViewModel` está diseñado para sobrevivir a las recreaciones de la actividad (por ejemplo, cambios de orientación de pantalla o de tema). Si el `ViewModel` conserva una referencia al `Context` de la actividad destruida, el Garbage Collector (GC) no podrá liberar la actividad ni su árbol de vistas de la memoria RAM. Además, mantener el `ViewModel` libre de dependencias del framework Android facilita la ejecución de **pruebas unitarias puras en la JVM** de manera rápida e independiente.

### 2. ¿Qué diferencia existe entre un StateFlow y un SharedFlow al comunicar la lógica de negocio con la UI?
**Respuesta**:  
- **`StateFlow`**: Es un flujo caliente (*hot flow*) orientado a **almacenar y exponer el estado actual de la UI** (*state-holder*). Requiere siempre un valor inicial, mantiene en memoria únicamente la última emisión (`replay = 1`), emite de forma inmediata a los nuevos suscriptores y omite emisiones consecutivas del mismo valor (`conflated`).
- **`SharedFlow`**: Es un flujo caliente orientado a **eventos efímeros e intenciones puntuales** (*event-driven*). No requiere un valor inicial, por defecto no guarda historial (`replay = 0`) y distribuye el evento a todos los suscriptores activos en el momento exacto de la emisión. Es ideal para acciones puntuales como mostrar un `Snackbar`, lanzar una notificación o ejecutar una navegación sin re-emitir la acción tras rotaciones de pantalla.

### 3. ¿Por qué es necesario declarar los BroadcastReceiver con `RECEIVER_NOT_EXPORTED` en versiones recientes de Android (API 33+)?
**Respuesta**:  
A partir de Android 14 (API 34) y de acuerdo con las recomendaciones de seguridad desde API 33 (Tiramisu), el sistema exige especificar explícitamente las banderas `RECEIVER_NOT_EXPORTED` o `RECEIVER_EXPORTED` al registrar un `BroadcastReceiver` de manera dinámica. Al utilizar `RECEIVER_NOT_EXPORTED`, se garantiza que el receptor solo pueda recibir intenciones enviadas internamente por la misma aplicación o por el sistema operativo, impidiendo que aplicaciones de terceros o aplicaciones maliciosas envíen broadcasts falsos para vulnerar la seguridad de la app.

### 4. ¿Qué ventajas ofrece usar `LaunchedEffect` y `DisposableEffect` en Jetpack Compose para gestionar eventos e interacciones con el sistema?
**Respuesta**:  
- **`LaunchedEffect`**: Permite ejecutar bloques de código suspendible (Corruntinas) de manera segura dentro de la composición. Se ejecuta cuando el composable entra en el árbol de UI o cuando cambia alguna de sus claves (`key`). Al abandonar la composición, la corrutina se cancela automáticamente, evitando ejecuciones colgadas y fugas de memoria al recolectar flujos (`Flow` / `SharedFlow`).
- **`DisposableEffect`**: Está diseñado para efectos secundarios que requieren una **limpieza o desinicialización explícita** al salir del ámbito de la composición. Proporciona un bloque `onDispose` obligatorio donde se deben desregistrar receptores (`unregisterReceiver`), desvincular listeners o liberar recursos del sistema, garantizando que no queden referencias residuales.

### 5. ¿Cómo garantiza la arquitectura MVVM + Repository la separación de responsabilidades en la aplicación?
**Respuesta**:  
La arquitectura MVVM + Repository distribuye las tareas en tres capas independientes:
1. **Capa de Datos (Repository)**: Encapsula el acceso a las fuentes de datos (memoria, bases de datos o servicios de red) y abstrae la lógica de persistencia exponiendo interfaces limpias.
2. **Capa de Dominio / Presentación (ViewModel)**: Procesa la lógica de negocio, transforma los datos del repositorio en estados consumibles (`StateFlow`) y gestiona eventos sin tener conocimiento directo del dibujado o de la tecnología de interfaz de usuario.
3. **Capa de Interfaz de Usuario (UI - Compose)**: Se enfoca de forma reactiva y declarativa en renderizar los estados emitidos por el ViewModel y notificar las intenciones del usuario.  
Esta separación mejora significativamente la **mantenibilidad, escalabilidad y testabilidad** del proyecto, permitiendo probar la lógica de negocio mediante pruebas unitarias sin levantar la interfaz gráfica.
