package com.recuerdamed.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface HorarioFijoDao {

    @Insert
    suspend fun insertarTodos(horarios: List<HorarioFijo>)

    @Query("SELECT * FROM horario_fijo WHERE medicamentoId = :medicamentoId ORDER BY hora")
    suspend fun obtenerPorMedicamento(medicamentoId: Long): List<HorarioFijo>

    // Se usa al editar un medicamento: se borran sus horas y se insertan las nuevas
    @Query("DELETE FROM horario_fijo WHERE medicamentoId = :medicamentoId")
    suspend fun eliminarPorMedicamento(medicamentoId: Long)
}