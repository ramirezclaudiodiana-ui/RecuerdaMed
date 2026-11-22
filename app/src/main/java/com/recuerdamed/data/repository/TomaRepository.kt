package com.recuerdamed.data.repository

import com.recuerdamed.data.local.AppDatabase
import com.recuerdamed.data.local.Medicamento
import com.recuerdamed.data.local.Toma
import com.recuerdamed.domain.GeneradorDeTomas
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class TomaRepository(db: AppDatabase) {

    private val tomaDao = db.tomaDao()
    private val medicamentoDao = db.medicamentoDao()
    private val horarioDao = db.horarioFijoDao()
    private val generador = GeneradorDeTomas()

    private fun ahora(): LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)

    // ---------- Lecturas (para las pantallas) ----------

    /** Tomas de hoy, de 00:00 a 00:00 del día siguiente. */
    fun observarHoy(): Flow<List<Toma>> {
        val hoy = LocalDate.now()
        return tomaDao.observarEntre(hoy.atStartOfDay(), hoy.plusDays(1).atStartOfDay())
    }

    fun observarProxima(): Flow<Toma?> = tomaDao.observarProximaPendiente(ahora())

    fun observarHistorial(): Flow<List<Toma>> = tomaDao.observarHistorial()

    suspend fun obtenerPorId(id: Long): Toma? = tomaDao.obtenerPorId(id)

    // ---------- Para alarmas ----------

    suspend fun obtenerPendientesDesdeAhora(): List<Toma> = tomaDao.obtenerPendientesDesde(ahora())

    suspend fun obtenerPendientesEn(fechaHora: LocalDateTime): List<Toma> =
        tomaDao.obtenerPendientesEn(fechaHora)

    suspend fun obtenerPendientesVencidas(): List<Toma> = tomaDao.obtenerPendientesVencidas(ahora())

    // ---------- Cambios de estado ----------

    /** true si se registró; false si la toma ya no estaba Pendiente (Tomada u Omitida). */
    suspend fun marcarTomada(id: Long): Boolean = tomaDao.marcarTomada(id, ahora()) == 1

    /** true si pasó a Omitida; false si ya estaba resuelta. */
    suspend fun marcarOmitida(id: Long): Boolean = tomaDao.marcarOmitida(id) == 1

    suspend fun sumarRepeticion(id: Long) = tomaDao.sumarRepeticion(id)

    suspend fun marcarFamiliarAvisado(id: Long) = tomaDao.marcarFamiliarAvisado(id)

    // ---------- Generación de tomas (ventana de 7 días) ----------

    /** Para el Worker, al abrir la app, al reiniciar y al cambiar la hora. */
    suspend fun regenerarVentana() {
        val desde = ahora()
        val hasta = desde.plusDays(GeneradorDeTomas.DIAS_VENTANA)
        val tomas = medicamentoDao.obtenerActivos().flatMap { med ->
            generador.generar(med, horasDe(med.id), desde, hasta)
        }
        tomaDao.insertarTodas(tomas)   // IGNORE: lo que ya existe no se duplica
    }

    /** Para cuando se guarda o edita un solo medicamento. */
    suspend fun regenerarParaMedicamento(medicamento: Medicamento) {
        val desde = ahora()
        val hasta = desde.plusDays(GeneradorDeTomas.DIAS_VENTANA)
        tomaDao.insertarTodas(
            generador.generar(medicamento, horasDe(medicamento.id), desde, hasta)
        )
    }

    private suspend fun horasDe(medicamentoId: Long): List<LocalTime> =
        horarioDao.obtenerPorMedicamento(medicamentoId).map { it.hora }
}