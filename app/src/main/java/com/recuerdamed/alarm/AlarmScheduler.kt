package com.recuerdamed.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Programa las alarmas con AlarmManager. Cada hora con tomas ("grupo") tiene como
 * máximo UNA alarma pendiente a la vez: la del siguiente paso de su cadena.
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    /** Desde Android 12 el permiso de alarmas exactas puede no estar concedido. */
    fun puedeProgramarExactas(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    /** Abre la pantalla de ajustes donde el usuario permite las alarmas exactas (Android 12+). */
    fun intentPermisoExactas(): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(
                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                Uri.parse("package:${context.packageName}")
            )
        } else {
            null
        }

    /**
     * Programa el [paso] de la cadena del [grupo] para [momento]. Reemplaza el anterior.
     * Nunca lanza excepción por falta de permiso: si no puede ser exacta, usa una alarma
     * aproximada (puede retrasarse unos minutos). Devuelve true si quedó exacta.
     */
    fun programarPaso(grupo: LocalDateTime, paso: Int, momento: LocalDateTime): Boolean {
        val alarma = crearPendingIntent(grupo, paso)
        val millis = momento.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        if (puedeProgramarExactas()) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, alarma)
                return true
            } catch (e: SecurityException) {
                // Sin permiso: se usa el respaldo de abajo
            }
        }
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, alarma)
        return false
    }

    fun cancelar(grupo: LocalDateTime) {
        val existente = buscar(grupo) ?: return
        alarmManager.cancel(existente)
        existente.cancel()
    }

    /** true si ese grupo tiene una alarma pendiente (todavía no ha sonado). */
    fun estaProgramada(grupo: LocalDateTime): Boolean = buscar(grupo) != null

    // ---------- Auxiliares ----------

    private fun crearPendingIntent(grupo: LocalDateTime, paso: Int): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARMA
            putExtra(AlarmReceiver.EXTRA_FECHA_HORA, grupo.toString())
            putExtra(AlarmReceiver.EXTRA_PASO, paso)
        }
        // ONE_SHOT: al sonar, el PendingIntent se consume. Así estaProgramada() es fiable.
        return PendingIntent.getBroadcast(
            context, codigoDe(grupo), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buscar(grupo: LocalDateTime): PendingIntent? {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARMA
        }
        return PendingIntent.getBroadcast(
            context, codigoDe(grupo), intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        /** Código único por hora: minutos desde 1970 (cabe en un Int). */
        fun codigoDe(grupo: LocalDateTime): Int =
            (grupo.atZone(ZoneId.systemDefault()).toEpochSecond() / 60).toInt()
    }
}