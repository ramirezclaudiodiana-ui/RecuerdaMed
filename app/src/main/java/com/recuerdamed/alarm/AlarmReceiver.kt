package com.recuerdamed.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.recuerdamed.voz.VozHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/** Recibe dos cosas: que "sonó" una alarma, o que se pulsó "Ya lo tomé" en la notificación. */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        val pendingResult = goAsync()   // da tiempo para usar la base de datos y la voz

        CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
            try {
                val manejador = ManejadorAlertas(appContext)
                when (intent.action) {
                    ACTION_ALARMA -> {
                        val grupo = intent.getStringExtra(EXTRA_FECHA_HORA)?.let { LocalDateTime.parse(it) }
                        val paso = intent.getIntExtra(EXTRA_PASO, 0)
                        if (grupo != null) {
                            val frase = manejador.alSonarAlarma(grupo, paso)
                            if (frase != null) {
                                VozHelper(appContext, comoAlarma = true).hablarYEsperar(frase)
                            }
                        }
                    }
                    ACTION_TOMADA -> {
                        val tomaId = intent.getLongExtra(EXTRA_TOMA_ID, -1L)
                        if (tomaId > 0) manejador.confirmarTomada(tomaId)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_ALARMA = "com.recuerdamed.ACCION_ALARMA"
        const val ACTION_TOMADA = "com.recuerdamed.ACCION_TOMADA"
        const val EXTRA_FECHA_HORA = "fecha_hora_grupo"
        const val EXTRA_PASO = "paso"
        const val EXTRA_TOMA_ID = "toma_id"
    }
}