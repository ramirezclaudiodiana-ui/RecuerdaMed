package com.recuerdamed.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(
    tableName = "toma",
    foreignKeys = [
        ForeignKey(
            entity = Medicamento::class,
            parentColumns = ["id"],
            childColumns = ["medicamentoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    // Impide duplicados: no puede haber dos tomas del mismo medicamento a la misma hora programada
    indices = [Index(value = ["medicamentoId", "fechaHoraProgramada"], unique = true)]
)
data class Toma(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicamentoId: Long,
    val fechaHoraProgramada: LocalDateTime,
    val estado: EstadoToma = EstadoToma.PENDIENTE,
    val fechaHoraReal: LocalDateTime? = null,
    val repeticionesAlerta: Int = 0,
    val familiarAvisado: Boolean = false
)
