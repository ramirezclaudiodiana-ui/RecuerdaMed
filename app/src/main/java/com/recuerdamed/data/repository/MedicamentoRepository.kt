package com.recuerdamed.data.repository

import androidx.room.withTransaction
import com.recuerdamed.data.local.AppDatabase
import com.recuerdamed.data.local.HorarioFijo
import com.recuerdamed.data.local.Medicamento
import com.recuerdamed.data.local.TipoHorario
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class MedicamentoRepository(
    private val db: AppDatabase,
    private val tomaRepository: TomaRepository
) {
    private val medicamentoDao = db.medicamentoDao()
    private val horarioDao = db.horarioFijoDao()
    private val tomaDao = db.tomaDao()

    fun observarActivos(): Flow<List<Medicamento>> = medicamentoDao.observarActivos()

    suspend fun obtenerPorId(id: Long): Medicamento? = medicamentoDao.obtenerPorId(id)

    suspend fun obtenerHoras(medicamentoId: Long): List<LocalTime> =
        horarioDao.obtenerPorMedicamento(medicamentoId).map { it.hora }

    /**
     * Guarda un medicamento nuevo con sus horas y genera sus tomas.
     * Todo en una transacción: si algo falla, no queda nada a medias.
     * [horas] solo se usa si el tipo es HORAS_FIJAS.
     */
    suspend fun guardar(medicamento: Medicamento, horas: List<LocalTime>): Long {
        validar(medicamento, horas)
        return db.withTransaction {
            val id = medicamentoDao.insertar(medicamento.copy(id = 0))
            guardarHoras(id, medicamento.tipoHorario, horas)
            tomaRepository.regenerarParaMedicamento(medicamento.copy(id = id))
            id
        }
        // TODO semana 2: después de guardar, programar las alarmas con AlarmScheduler
    }

    /** Edita un medicamento: reemplaza sus horas y rehace solo sus tomas futuras pendientes. */
    suspend fun editar(medicamento: Medicamento, horas: List<LocalTime>) {
        validar(medicamento, horas)
        db.withTransaction {
            medicamentoDao.actualizar(medicamento)
            horarioDao.eliminarPorMedicamento(medicamento.id)
            guardarHoras(medicamento.id, medicamento.tipoHorario, horas)
            tomaDao.eliminarPendientesFuturas(medicamento.id, ahora())
            tomaRepository.regenerarParaMedicamento(medicamento)
        }
        // TODO semana 2: reprogramar alarmas
    }

    /**
     * "Borrar" = desactivar. Se borran solo las tomas futuras sin resolver;
     * el historial conserva nombre y foto.
     */
    suspend fun desactivar(medicamentoId: Long) {
        db.withTransaction {
            medicamentoDao.desactivar(medicamentoId)
            tomaDao.eliminarPendientesFuturas(medicamentoId, ahora())
        }
        // TODO semana 2: cancelar las alarmas de esas tomas
    }

    // ---------- Auxiliares ----------

    private suspend fun guardarHoras(medicamentoId: Long, tipo: TipoHorario, horas: List<LocalTime>) {
        if (tipo != TipoHorario.HORAS_FIJAS) return
        val filas = horas.distinct().map { HorarioFijo(medicamentoId = medicamentoId, hora = it) }
        horarioDao.insertarTodos(filas)
    }

    private fun validar(medicamento: Medicamento, horas: List<LocalTime>) {
        when (medicamento.tipoHorario) {
            TipoHorario.HORAS_FIJAS ->
                require(horas.isNotEmpty()) { "Un medicamento a horas fijas necesita al menos una hora" }
            TipoHorario.CADA_X_HORAS ->
                require((medicamento.intervaloHoras ?: 0) > 0 && medicamento.horaInicio != null) {
                    "Un medicamento por intervalo necesita intervaloHoras y horaInicio"
                }
        }
    }

    private fun ahora(): LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
}