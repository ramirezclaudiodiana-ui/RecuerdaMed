package com.recuerdamed.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "medicamento")
data class Medicamento(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val dosis: String,
    val fotoRuta: String? = null,
    val tipoHorario: TipoHorario,
    val intervaloHoras: Int? = null,      // solo si es CADA_X_HORAS
    val horaInicio: LocalTime? = null,    // solo si es CADA_X_HORAS
    val diasSemana: Set<DayOfWeek> = DayOfWeek.values().toSet(),
    val fechaInicio: LocalDate,
    val fechaFin: LocalDate? = null,
    val activo: Boolean = true
)