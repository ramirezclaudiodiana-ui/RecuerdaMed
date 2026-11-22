package com.recuerdamed.domain

import com.recuerdamed.data.local.Medicamento
import com.recuerdamed.data.local.TipoHorario
import com.recuerdamed.data.local.Toma
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class GeneradorDeTomas {

    /**
     * Devuelve las tomas de [medicamento] en el rango [desde, hasta).
     * Todas salen con segundos en cero, para que el índice único y las
     * comparaciones de texto en Room funcionen bien.
     */
    fun generar(
        medicamento: Medicamento,
        horas: List<LocalTime>,
        desde: LocalDateTime,
        hasta: LocalDateTime
    ): List<Toma> {
        if (!medicamento.activo) return emptyList()

        val momentos = when (medicamento.tipoHorario) {
            TipoHorario.HORAS_FIJAS -> momentosHorasFijas(medicamento, horas, desde, hasta)
            TipoHorario.CADA_X_HORAS -> momentosPorIntervalo(medicamento, desde, hasta)
        }

        return momentos.distinct().sorted().map { momento ->
            Toma(medicamentoId = medicamento.id, fechaHoraProgramada = momento)
        }
    }

    /**
     * Horas fijas: una toma por cada hora, solo en los días de la semana elegidos
     * y dentro de fechaInicio..fechaFin.
     */
    private fun momentosHorasFijas(
        med: Medicamento,
        horas: List<LocalTime>,
        desde: LocalDateTime,
        hasta: LocalDateTime
    ): List<LocalDateTime> {
        val resultado = mutableListOf<LocalDateTime>()
        var dia = desde.toLocalDate()
        val ultimoDia = hasta.toLocalDate()

        while (!dia.isAfter(ultimoDia)) {
            val aplica = !dia.isBefore(med.fechaInicio) &&
                    (med.fechaFin == null || !dia.isAfter(med.fechaFin)) &&
                    dia.dayOfWeek in med.diasSemana

            if (aplica) {
                for (hora in horas) {
                    val momento = dia.atTime(hora).truncatedTo(ChronoUnit.MINUTES)
                    if (!momento.isBefore(desde) && momento.isBefore(hasta)) {
                        resultado.add(momento)
                    }
                }
            }
            dia = dia.plusDays(1)
        }
        return resultado
    }

    /**
     * Cada X horas: corre de forma continua desde fechaInicio + horaInicio,
     * SIN mirar diasSemana, y siempre calculado desde la hora programada
     * (nunca desde la hora real en que se tomó).
     */
    private fun momentosPorIntervalo(
        med: Medicamento,
        desde: LocalDateTime,
        hasta: LocalDateTime
    ): List<LocalDateTime> {
        val intervalo = med.intervaloHoras ?: return emptyList()
        val horaInicio = med.horaInicio ?: return emptyList()
        if (intervalo <= 0) return emptyList()

        val pasoMinutos = intervalo * 60L
        val inicio = med.fechaInicio.atTime(horaInicio).truncatedTo(ChronoUnit.MINUTES)

        // Primer momento de la serie que sea >= desde
        var momento = if (!inicio.isBefore(desde)) {
            inicio
        } else {
            val transcurridos = Duration.between(inicio, desde).toMinutes()
            val pasos = (transcurridos + pasoMinutos - 1) / pasoMinutos   // redondeo hacia arriba
            inicio.plusMinutes(pasos * pasoMinutos)
        }

        val resultado = mutableListOf<LocalDateTime>()
        while (momento.isBefore(hasta)) {
            val fin = med.fechaFin
            if (fin != null && momento.toLocalDate().isAfter(fin)) break
            resultado.add(momento)
            momento = momento.plusMinutes(pasoMinutos)
        }
        return resultado
    }

    companion object {
        /** Ventana que se mantiene generada por adelantado (sección 9 del documento). */
        const val DIAS_VENTANA = 7L
    }
}