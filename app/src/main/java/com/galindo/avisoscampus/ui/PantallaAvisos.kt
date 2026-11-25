package com.galindo.avisoscampus.ui

import android.Manifest
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.galindo.avisoscampus.R
import com.galindo.avisoscampus.model.Aviso
import com.galindo.avisoscampus.model.EstadoPermiso
import com.galindo.avisoscampus.model.Origen
import com.galindo.avisoscampus.notificacion.Notificador
import com.galindo.avisoscampus.receiver.CargadorReceiver
import com.galindo.avisoscampus.receiver.ModoAvionReceiver
import com.galindo.avisoscampus.viewmodel.AvisoEvento
import com.galindo.avisoscampus.viewmodel.AvisosViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAvisos(
    viewModel: AvisosViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val estadoPermiso by viewModel.permiso.collectAsState()
    val faltan by viewModel.faltan.collectAsState()
    val escuchandoAvion by viewModel.escuchandoAvion.collectAsState()
    val escuchandoCargador by viewModel.escuchandoCargador.collectAsState()
    val listaAvisos by viewModel.avisos.collectAsState()
    val mensajeError by viewModel.mensaje.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { concedido ->
        viewModel.permisoRespondido(concedido)
    }

    LaunchedEffect(Unit) {
        val tienePermiso = Notificador.tienePermiso(context)
        viewModel.permisoRespondido(tienePermiso)
    }

    DisposableEffect(escuchandoAvion) {
        if (escuchandoAvion) {
            val receptor = ModoAvionReceiver { activo ->
                viewModel.alCambiarModoAvion(activo)
            }
            val filter = IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            ContextCompat.registerReceiver(
                context,
                receptor,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            onDispose {
                context.unregisterReceiver(receptor)
            }
        } else {
            onDispose { }
        }
    }

    DisposableEffect(escuchandoCargador) {
        if (escuchandoCargador) {
            val receptor = CargadorReceiver { conectado ->
                viewModel.alCambiarCargador(conectado)
            }
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
            }
            ContextCompat.registerReceiver(
                context,
                receptor,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            onDispose {
                context.unregisterReceiver(receptor)
            }
        } else {
            onDispose { }
        }
    }

    LaunchedEffect(key1 = true) {
        viewModel.eventos.collectLatest { evento ->
            when (evento) {
                is AvisoEvento.MostrarMensaje -> {
                    snackbarHostState.showSnackbar(evento.mensaje)
                }
                is AvisoEvento.PermisoSolicitado -> {
                    // Evento de solicitud de permiso
                }
                is AvisoEvento.MostrarNotificacion -> {
                    val enviado = Notificador.mostrar(context, evento.aviso)
                    if (!enviado) {
                        viewModel.noSePudoMostrar()
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = "Avisos Campus") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                actions = {
                    IconButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.permisoRespondido(true)
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_notificacion),
                            contentDescription = "Notificaciones",
                            tint = if (estadoPermiso == EstadoPermiso.CONCEDIDO) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.Gray
                            },
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            if (uiState.estaCargando) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (mensajeError != null) {
                        item {
                            RecuadroErrorMensaje(
                                mensaje = mensajeError!!,
                                onAbrirAjustes = {
                                    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                        }
                                    } else {
                                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.fromParts("package", context.packageName, null)
                                        }
                                    }
                                    context.startActivity(intent)
                                },
                            )
                        }
                    }

                    item {
                        SeccionAccionesNotificacion(
                            estadoPermiso = estadoPermiso,
                            onSolicitarPermiso = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.permisoRespondido(true)
                                }
                            },
                            onMostrarAviso = {
                                viewModel.avisarAhora()
                            },
                        )
                    }

                    item {
                        TarjetaRecordatorio(
                            faltan = faltan,
                            onRecordar = { viewModel.recordarEn(10) },
                            onCancelar = { viewModel.cancelarRecordatorio() },
                        )
                    }

                    item {
                        TarjetaModoAvion(
                            escuchando = escuchandoAvion,
                            onCambiarEscucha = { viewModel.cambiarEscucha(it) },
                        )
                    }

                    item {
                        TarjetaCargador(
                            escuchando = escuchandoCargador,
                            onCambiarEscucha = { viewModel.cambiarEscuchaCargador(it) },
                        )
                    }

                    item {
                        HistorialAvisos(avisos = listaAvisos)
                    }

                    items(uiState.avisos, key = { it.id }) { aviso ->
                        TarjetaAviso(
                            aviso = aviso,
                            onClick = { viewModel.seleccionarAviso(aviso) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecuadroErrorMensaje(
    mensaje: String,
    onAbrirAjustes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = mensaje,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onAbrirAjustes,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text(text = "Abrir ajustes")
            }
        }
    }
}

@Composable
fun SeccionAccionesNotificacion(
    estadoPermiso: EstadoPermiso,
    onSolicitarPermiso: () -> Unit,
    onMostrarAviso: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Estado de notificaciones",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    EtiquetaEstadoPermiso(estado = estadoPermiso)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onSolicitarPermiso,
                ) {
                    Text(text = "Permitir notificaciones")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onMostrarAviso,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text(text = "Mostrar aviso")
            }
        }
    }
}

@Composable
fun TarjetaRecordatorio(
    faltan: Int?,
    onRecordar: () -> Unit,
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = "Recordatorio",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (faltan != null) {
                Text(
                    text = "Faltan $faltan s",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onRecordar,
                    enabled = (faltan == null),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = "Recordar en 10 s")
                }

                OutlinedButton(
                    onClick = onCancelar,
                    enabled = (faltan != null),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = "Cancelar")
                }
            }
        }
    }
}

@Composable
fun TarjetaModoAvion(
    escuchando: Boolean,
    onCambiarEscucha: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Escuchar el modo avión",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Switch(
                checked = escuchando,
                onCheckedChange = onCambiarEscucha,
            )
        }
    }
}

@Composable
fun TarjetaCargador(
    escuchando: Boolean,
    onCambiarEscucha: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Escuchar el cargador",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Switch(
                checked = escuchando,
                onCheckedChange = onCambiarEscucha,
            )
        }
    }
}

@Composable
fun EtiquetaEstadoPermiso(
    estado: EstadoPermiso,
    modifier: Modifier = Modifier,
) {
    val (bgColor, textColor) = when (estado) {
        EstadoPermiso.CONCEDIDO -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        EstadoPermiso.DENEGADO -> Color(0xFFFFEBEE) to Color(0xFFC62828)
        EstadoPermiso.SIN_SOLICITAR -> Color(0xFFFFF8E1) to Color(0xFFF57F17)
    }

    Surface(
        modifier = modifier,
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = estado.etiqueta,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
        )
    }
}

@Composable
fun TarjetaAviso(
    aviso: Aviso,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EtiquetaOrigen(origen = aviso.origen)
                if (aviso.esImportante) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Aviso Importante",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = aviso.titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = aviso.contenido,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.outline,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = aviso.fecha,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
fun EtiquetaOrigen(
    origen: Origen,
    modifier: Modifier = Modifier,
) {
    val (bgColor, textColor) = when (origen) {
        Origen.ACADEMICO -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        Origen.ADMINISTRATIVO -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        Origen.EVENTOS -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        Origen.GENERAL -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurfaceVariant
        Origen.BOTON -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
        Origen.RECORDATORIO -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.secondary
        Origen.MODO_AVION -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        Origen.CARGADOR -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    }

    Surface(
        modifier = modifier,
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = origen.etiqueta,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
        )
    }
}
