package com.recuerdamed.alarm

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.recuerdamed.MainActivity
import com.recuerdamed.RecuerdaMedApp
import com.recuerdamed.data.preferences.PreferenciasRepository
import java.time.LocalDateTime

/** Construye y muestra la notificación agrupada de una hora de tomas. */
class NotificacionHelper(private val context: Context) {

    data class Item(val tomaId: Long, val nombre: String, val dosis: String)

    @SuppressLint("MissingPermission")   // se comprueba con areNotificationsEnabled()
    fun mostrar(grupo: LocalDateTime, items: List<Item>, paso: Int) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        val id = AlarmScheduler.codigoDe(grupo)
        val unico = items.size == 1

        val prefijo = if (paso > 0) "Recordatorio $paso de ${PreferenciasRepository.MAX_REPETICIONES}: " else ""
        val titulo = prefijo + if (unico) items[0].nombre else "Es hora de tus medicamentos"
        val textoCorto = if (unico) "Es hora de tu medicamento · ${items[0].dosis}" else "${items.size} medicamentos"
        val detalle = items.joinToString("\n") { "• ${it.nombre} · ${it.dosis}" }

        val abrir = PendingIntent.getActivity(
            context, id,
            Intent(context, DESTINO).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(AlarmReceiver.EXTRA_FECHA_HORA, grupo.toString())
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, RecuerdaMedApp.CANAL_ALARMAS)
            .setSmallIcon(ICONO)
            .setContentTitle(titulo)
            .setContentText(textoCorto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detalle))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(abrir)
            .setFullScreenIntent(abrir, true)
            .setAutoCancel(true)

        // "Ya lo tomé" desde la notificación solo si hay UN medicamento:
        // con varios, un solo toque podría registrar uno que no se tomó.
        if (unico) {
            val tomada = PendingIntent.getBroadcast(
                context, id,
                Intent(context, AlarmReceiver::class.java).apply {
                    action = AlarmReceiver.ACTION_TOMADA
                    putExtra(AlarmReceiver.EXTRA_TOMA_ID, items[0].tomaId)
                    putExtra(AlarmReceiver.EXTRA_FECHA_HORA, grupo.toString())
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "Ya lo tomé", tomada)
        }

        manager.notify(id, builder.build())   // mismo id = reemplaza y vuelve a sonar
    }

    fun cancelar(grupo: LocalDateTime) {
        NotificationManagerCompat.from(context).cancel(AlarmScheduler.codigoDe(grupo))
    }

    companion object {
        // TODO: cambiar a AlarmaActivity::class.java cuando exista la pantalla de alarma
        private val DESTINO = MainActivity::class.java

        // TODO: cambiar por res/drawable/ic_notificacion (ícono monocromo propio)
        private const val ICONO = android.R.drawable.ic_lock_idle_alarm
    }
}