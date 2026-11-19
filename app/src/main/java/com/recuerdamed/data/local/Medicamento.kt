package com.recuerdamed.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicamento")
data class Medicamento(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val dosis: String,
    val instrucciones: String? = null,
    val activo: Boolean = true
)
