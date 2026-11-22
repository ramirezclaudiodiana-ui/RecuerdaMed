package com.recuerdamed.alarm

import android.content.Context
import com.recuerdamed.RecuerdaMedApp
import com.recuerdamed.data.local.Toma
import com.recuerdamed.data.preferences.PreferenciasRepository
import com.recuerdamed.voz.VozHelper
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class ManejadorAlertas(context: Context) {

    private val appContext = context.applicationContext
    private val app = appContext as RecuerdaMedApp
    private val tomas = app.tomaRepository
    private val medicamentos = app.medicamentoRepository
    private val preferencias = app.preferencias
    private val scheduler = AlarmScheduler(appContext)
    private val notificaciones = NotificacionHelper(appContext)
    private val sms = SmsSender(appContext)

    private val max = PreferenciasRepository.MAX_REPETICIONES

    private fun ahora(): LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)

    // ---------- Cuando suena una alarma ----------

    /**
     * Paso 0 = alerta inicial; 1..MAX = repeticiones; MAX+1 = se acabó el tiempo.
     * Devuelve la frase que debe decir la voz, o null si no hay que hablar.
     */
    suspend fun alSonarAlarma(grupo: LocalDateTime, paso: Int): String? {
        val pendientes = tomas.obtenerPendientesEn(grupo)
        if (pendientes.isEmpty()) {          // ya se resolvieron (o se desactivó el medicamento)
            notificaciones.cancelar(grupo)
            return null
        }
        val ajustes = preferencias.obtener()

        if (paso > max) {                    // sin respuesta tras todas las repeticiones
            omitirGrupo(grupo, pendientes, enviarSms = true)
            return null
        }

        if (paso > 0) pendientes.forEach { tomas.sumarRepeticion(it.id) }
        notificaciones.mostrar(grupo, itemsDe(pendientes), paso)

        // Cada paso programa el siguiente
        val siguiente = LocalDateTime.now().plusMinutes(ajustes.minutosEntreRepeticiones.toLong())
        scheduler.programarPaso(grupo, paso + 1, siguiente)

        return when {
            !ajustes.vozAlarmaActiva -> null
            paso == 0 -> VozHelper.FRASE_ALARMA
            else -> VozHelper.FRASE_REPETICION
        }
    }

    // ---------- Acciones del usuario (las usan las pantallas) ----------

    /**
     * "Ya lo tomé". true si se registró; false si la toma ya no estaba Pendiente
     * (Tomada u Omitida: una Omitida nunca se puede registrar después).
     */
    suspend fun confirmarTomada(tomaId: Long): Boolean {
        val toma = tomas.obtenerPorId(tomaId) ?: return false
        val registrada = tomas.marcarTomada(tomaId)
        if (registrada) {
            val grupo = toma.fechaHoraProgramada
            if (tomas.obtenerPendientesEn(grupo).isEmpty()) {
                scheduler.cancelar(grupo)            // se cancelan las alertas de esa hora
                notificaciones.cancelar(grupo)
            }
        }
        return registrada
    }

    /**
     * "Recordarme en X min": mueve el siguiente paso a dentro de X minutos.
     * No suma repeticiones extra: solo adelanta o atrasa la que ya correspondía.
     * false si ya se usaron las 3 repeticiones (no hay posposiciones extra).
     */
    suspend fun recordarme(grupo: LocalDateTime): Boolean {
        val pendientes = tomas.obtenerPendientesEn(grupo)
        if (pendientes.isEmpty()) return false

        val usadas = pendientes.maxOf { it.repeticionesAlerta }
        if (usadas >= max) return false

        val ajustes = preferencias.obtener()
        notificaciones.cancelar(grupo)
        scheduler.programarPaso(
            grupo,
            usadas + 1,
            LocalDateTime.now().plusMinutes(ajustes.minutosEntreRepeticiones.toLong())
        )
        return true
    }

    // ---------- Reprogramación (Worker, reinicio, cambio de hora, al abrir la app) ----------

    /**
     * Regenera la ventana de 7 días y deja programada una alarma por cada hora con
     * tomas pendientes. Es seguro llamarla varias veces: no toca lo que ya está programado.
     */
    suspend fun reprogramarTodo() {
        tomas.regenerarVentana()

        val ahora = ahora()
        val minutos = preferencias.obtener().minutosEntreRepeticiones
        val ventanaTotal = (max + 1) * minutos.toLong()

        val pendientes = tomas.obtenerPendientesVencidas() + tomas.obtenerPendientesDesdeAhora()

        pendientes.groupBy { it.fechaHoraProgramada }.forEach { (grupo, lista) ->
            if (scheduler.estaProgramada(grupo)) return@forEach

            if (grupo.isAfter(ahora)) {
                scheduler.programarPaso(grupo, 0, grupo)      // toma futura normal
                return@forEach
            }

            // Hora ya pasada y sin alarma (el teléfono estuvo apagado o se perdió)
            val transcurridos = Duration.between(grupo, ahora).toMinutes()
            if (transcurridos >= ventanaTotal) {
                omitirGrupo(grupo, lista, enviarSms = false)  // demasiado tarde: ver nota
            } else {
                val reps = lista.maxOf { it.repeticionesAlerta }
                val paso = if (reps == 0) 0 else reps + 1
                scheduler.programarPaso(grupo, paso, LocalDateTime.now().plusSeconds(5))
            }
        }
    }

    // ---------- Auxiliares ----------

    /** Pasa a Omitida cada toma del grupo y, si corresponde, avisa por SMS (una vez por toma). */
    private suspend fun omitirGrupo(grupo: LocalDateTime, pendientes: List<Toma>, enviarSms: Boolean) {
        val ajustes = preferencias.obtener()

        for (toma in pendientes) {
            val cambio = tomas.marcarOmitida(toma.id)   // solo cambia si seguía Pendiente
            if (cambio && enviarSms && ajustes.hayFamiliar && !toma.familiarAvisado) {
                val med = medicamentos.obtenerPorId(toma.medicamentoId)
                val enviado = sms.enviarAvisoOmitida(
                    telefono = ajustes.familiarTelefono,
                    nombrePersona = ajustes.nombreParaSms,
                    medicamento = med?.nombre ?: "medicamento",
                    hora = toma.fechaHoraProgramada.toLocalTime()
                )
                if (enviado) tomas.marcarFamiliarAvisado(toma.id)
            }
        }
        notificaciones.cancelar(grupo)
    }

    private suspend fun itemsDe(lista: List<Toma>): List<NotificacionHelper.Item> =
        lista.map { toma ->
            val med = medicamentos.obtenerPorId(toma.medicamentoId)
            NotificacionHelper.Item(toma.id, med?.nombre ?: "Medicamento", med?.dosis ?: "")
        }
}