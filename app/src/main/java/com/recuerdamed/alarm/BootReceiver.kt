package com.recuerdamed.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Reinicio, cambio de hora o de zona horaria: se pierden las alarmas, hay que reprogramarlas. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val accionesValidas = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED
        )
        if (intent.action !in accionesValidas) return

        val appContext = context.applicationContext
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
            try {
                ManejadorAlertas(appContext).reprogramarTodo()
            } finally {
                pendingResult.finish()
            }
        }
    }
}