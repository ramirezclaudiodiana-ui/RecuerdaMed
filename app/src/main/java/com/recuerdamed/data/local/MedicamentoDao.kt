package com.recuerdamed.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicamentoDao {

    @Insert
    suspend fun insertar(medicamento: Medicamento): Long   // devuelve el id generado

    @Update
    suspend fun actualizar(medicamento: Medicamento)

    @Query("SELECT * FROM medicamento WHERE id = :id")
    suspend fun obtenerPorId(id: Long): Medicamento?

    // Para la pantalla Medicinas (se actualiza sola cuando cambian los datos)
    @Query("SELECT * FROM medicamento WHERE activo = 1 ORDER BY nombre")
    fun observarActivos(): Flow<List<Medicamento>>

    // Para el Worker que genera las tomas
    @Query("SELECT * FROM medicamento WHERE activo = 1")
    suspend fun obtenerActivos(): List<Medicamento>

    // "Borrar" un medicamento = desactivarlo, para conservar su historial
    @Query("UPDATE medicamento SET activo = 0 WHERE id = :id")
    suspend fun desactivar(id: Long)
}