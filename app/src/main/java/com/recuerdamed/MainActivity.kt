package com.recuerdamed

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.recuerdamed.alarm.ManejadorAlertas
import com.recuerdamed.data.local.EstadoToma
import com.recuerdamed.data.local.Medicamento
import com.recuerdamed.data.local.TipoHorario
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // El documento pide regenerar tomas y reprogramar alarmas al abrir la app
        lifecycleScope.launch { ManejadorAlertas(applicationContext).reprogramarTodo() }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PantallaPrueba(application as RecuerdaMedApp)
                }
            }
        }
    }
}

// ---------------- PANTALLA TEMPORAL DE PRUEBA (borrar al hacer las pantallas reales) ----------------

@Composable
private fun PantallaPrueba(app: RecuerdaMedApp) {
    val scope = rememberCoroutineScope()
    val tomas by app.tomaRepository.observarHoy().collectAsState(initial = emptyList())
    var mensaje by remember { mutableStateOf("") }

    val pedirNotificaciones = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* sin acción: el sistema ya guarda la respuesta */ }

    Column(
        modifier = Modifier
            .statusBarsPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Prueba de alarmas", style = MaterialTheme.typography.headlineSmall)

        Button(onClick = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pedirNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }) { Text("1. Permitir notificaciones") }

        Button(onClick = {
            scope.launch {
                var paso = "guardar el medicamento"
                try {
                    val hora = LocalTime.now().plusMinutes(2).withSecond(0).withNano(0)
                    val medicamento = Medicamento(
                        nombre = "Paracetamol (prueba)",
                        dosis = "1 pastilla",
                        tipoHorario = TipoHorario.HORAS_FIJAS,
                        fechaInicio = LocalDate.now()
                    )
                    app.medicamentoRepository.guardar(medicamento, listOf(hora))
                    paso = "programar la alarma"
                    ManejadorAlertas(app).reprogramarTodo()
                    mensaje = "Listo: toma creada para las $hora"
                } catch (e: Throwable) {
                    mensaje = "Falló al $paso: ${e::class.java.simpleName}: ${e.message}"
                }
            }
        }) { Text("2. Crear toma de prueba en 2 minutos") }

        if (mensaje.isNotEmpty()) Text(mensaje, style = MaterialTheme.typography.bodyLarge)

        Text("Tomas de hoy:", style = MaterialTheme.typography.titleMedium)

        if (tomas.isEmpty()) Text("Todavía no hay tomas hoy.")

        tomas.forEach { toma ->
            Text("${toma.fechaHoraProgramada.toLocalTime()} · ${toma.estado} · repeticiones: ${toma.repeticionesAlerta}")
            if (toma.estado == EstadoToma.PENDIENTE) {
                Button(onClick = {
                    scope.launch { ManejadorAlertas(app).confirmarTomada(toma.id) }
                }) { Text("Ya lo tomé") }
            }
        }
    }
}