package com.recuerdamed.data.local

import androidx.room.TypeConverter
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class Converters {

    @TypeConverter
    fun fechaAString(valor: LocalDate?): String? = valor?.toString()

    @TypeConverter
    fun stringAFecha(valor: String?): LocalDate? = valor?.let { LocalDate.parse(it) }

    @TypeConverter
    fun horaAString(valor: LocalTime?): String? = valor?.toString()

    @TypeConverter
    fun stringAHora(valor: String?): LocalTime? = valor?.let { LocalTime.parse(it) }

    @TypeConverter
    fun fechaHoraAString(valor: LocalDateTime?): String? = valor?.toString()

    @TypeConverter
    fun stringAFechaHora(valor: String?): LocalDateTime? = valor?.let { LocalDateTime.parse(it) }

    // Días de la semana: se guardan como "1,2,3,4,5" (1 = lunes ... 7 = domingo)
    @TypeConverter
    fun diasAString(valor: Set<DayOfWeek>?): String? =
        valor?.sortedBy { it.value }?.joinToString(",") { it.value.toString() }

    @TypeConverter
    fun stringADias(valor: String?): Set<DayOfWeek>? =
        valor?.takeIf { it.isNotBlank() }
            ?.split(",")
            ?.map { DayOfWeek.of(it.trim().toInt()) }
            ?.toSet()
}