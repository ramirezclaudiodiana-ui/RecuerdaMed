package com.recuerdamed

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build

class RecuerdaMedApp : Application() {

    override fun onCreate() {
        super.onCreate()
        crearCanalDeAlarmas()
        // programarRegeneracionDeTomas()   // descomenta cuando exista RegenerarTomasWorker
    }

    /**
     * Canal de notificaciones para las alarmas de medicamentos.
     * Se crea aquí para que exista antes de que suene la primera alarma.
     * Crearlo varias veces es seguro: si ya existe, Android lo ignora.
     */
    private fun crearCanalDeAlarmas() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val sonido = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val atributos = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val canal = NotificationChannel(
            CANAL_ALARMAS,
            "Alarmas de medicamentos",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Avisa cuando es hora de tomar un medicamento"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 300, 500)
            setSound(sonido, atributos)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }

        getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    /*
    // Descomenta en el paso 6 de la semana 1 (necesita work-runtime-ktx y RegenerarTomasWorker)
    private fun programarRegeneracionDeTomas() {
        val trabajo = PeriodicWorkRequestBuilder<RegenerarTomasWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "regenerar_tomas",
            ExistingPeriodicWorkPolicy.KEEP,
            trabajo
        )
    }
    */

    companion object {
        const val CANAL_ALARMAS = "alarma_medicamentos"
    }
}