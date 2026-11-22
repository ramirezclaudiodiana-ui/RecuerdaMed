package com.recuerdamed.alarm

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class SmsSender(private val context: Context) {

    fun tienePermiso(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) ==
                PackageManager.PERMISSION_GRANTED

    /**
     * Mensaje de la sección 9:
     * "RecuerdaMed: [nombre] no confirmó su [medicamento] de las [hora]."
     * [nombrePersona] ya debe venir como "Tu familiar" si el nombre está vacío
     * (usa Ajustes.nombreParaSms). Devuelve true si el sistema aceptó enviarlo.
     */
    fun enviarAvisoOmitida(
        telefono: String,
        nombrePersona: String,
        medicamento: String,
        hora: LocalTime
    ): Boolean {
        if (!tienePermiso() || telefono.isBlank()) return false

        val horaTexto = hora.format(DateTimeFormatter.ofPattern("H:mm"))
        val texto = "RecuerdaMed: $nombrePersona no confirmó su $medicamento de las $horaTexto."

        return try {
            val manager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            val partes = manager.divideMessage(texto)
            manager.sendMultipartTextMessage(telefono, null, partes, null, null)
            true
        } catch (e: Exception) {
            false   // sin telefonía, número inválido, etc.
        }
    }
}