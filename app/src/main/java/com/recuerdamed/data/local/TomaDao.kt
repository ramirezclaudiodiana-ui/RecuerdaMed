package com.recuerdamed.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface TomaDao {

    // IGNORE + índice único = se puede regenerar la ventana de 7 días sin duplicar tomas
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarTodas(tomas: List<Toma>)

    @Query("SELECT * FROM toma WHERE id = :id")
    suspend fun obtenerPorId(id: Long): Toma?

    // Pantalla Hoy: tomas entre dos momentos (desde 00:00 de hoy hasta 00:00 de mañana)
    @Query("""
        SELECT * FROM toma
        WHERE fechaHoraProgramada >= :desde AND fechaHoraProgramada < :hasta
        ORDER BY fechaHoraProgramada
    """)
    fun observarEntre(desde: LocalDateTime, hasta: LocalDateTime): Flow<List<Toma>>

    // Tarjeta "Próxima toma"
    @Query("""
        SELECT * FROM toma
        WHERE estado = 'PENDIENTE' AND fechaHoraProgramada >= :ahora
        ORDER BY fechaHoraProgramada
        LIMIT 1
    """)
    fun observarProximaPendiente(ahora: LocalDateTime): Flow<Toma?>

    // Para programar alarmas (AlarmScheduler)
    @Query("""
        SELECT * FROM toma
        WHERE estado = 'PENDIENTE' AND fechaHoraProgramada >= :ahora
        ORDER BY fechaHoraProgramada
    """)
    suspend fun obtenerPendientesDesde(ahora: LocalDateTime): List<Toma>

    // Todas las tomas pendientes de una misma hora (para una sola notificación agrupada)
    @Query("SELECT * FROM toma WHERE estado = 'PENDIENTE' AND fechaHoraProgramada = :fechaHora")
    suspend fun obtenerPendientesEn(fechaHora: LocalDateTime): List<Toma>

    // Pendientes que ya pasaron (útil tras reiniciar el teléfono)
    @Query("SELECT * FROM toma WHERE estado = 'PENDIENTE' AND fechaHoraProgramada < :ahora")
    suspend fun obtenerPendientesVencidas(ahora: LocalDateTime): List<Toma>

    // Pantalla Historial: todo lo que ya se resolvió, lo más reciente primero
    @Query("SELECT * FROM toma WHERE estado != 'PENDIENTE' ORDER BY fechaHoraProgramada DESC")
    fun observarHistorial(): Flow<List<Toma>>

    // Solo cambia si sigue PENDIENTE. Devuelve cuántas filas cambió (0 = ya estaba resuelta)
    @Query("""
        UPDATE toma SET estado = 'TOMADA', fechaHoraReal = :ahora
        WHERE id = :id AND estado = 'PENDIENTE'
    """)
    suspend fun marcarTomada(id: Long, ahora: LocalDateTime): Int

    @Query("UPDATE toma SET estado = 'OMITIDA' WHERE id = :id AND estado = 'PENDIENTE'")
    suspend fun marcarOmitida(id: Long): Int

    @Query("""
        UPDATE toma SET repeticionesAlerta = repeticionesAlerta + 1
        WHERE id = :id AND estado = 'PENDIENTE'
    """)
    suspend fun sumarRepeticion(id: Long)

    @Query("UPDATE toma SET familiarAvisado = 1 WHERE id = :id")
    suspend fun marcarFamiliarAvisado(id: Long)

    // Al desactivar un medicamento: solo se borran las tomas futuras sin resolver
    @Query("""
        DELETE FROM toma
        WHERE medicamentoId = :medicamentoId
        AND estado = 'PENDIENTE' AND fechaHoraProgramada >= :desde
    """)
    suspend fun eliminarPendientesFuturas(medicamentoId: Long, desde: LocalDateTime)
}