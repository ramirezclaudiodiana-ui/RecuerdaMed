package com.recuerdamed.data.local

import androidx.room.ForeignKey
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalTime

@Entity(
    tableName = "horario_fijo",
    foreignKeys = [
        ForeignKey(
            entity = Medicamento::class,
            parentColumns = ["id"],
            childColumns = ["medicamentoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("medicamentoId")]
)
data class HorarioFijo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicamentoId: Long,
    val hora: LocalTime
)