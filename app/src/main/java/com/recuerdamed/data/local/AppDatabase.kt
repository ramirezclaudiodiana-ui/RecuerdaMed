package com.recuerdamed.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Medicamento::class, HorarioFijo::class, Toma::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun medicamentoDao(): MedicamentoDao
    abstract fun horarioFijoDao(): HorarioFijoDao
    abstract fun tomaDao(): TomaDao

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        // Una sola base de datos para toda la app
        fun obtener(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "recuerdamed.db"
                ).build().also { instancia = it }
            }
    }
}