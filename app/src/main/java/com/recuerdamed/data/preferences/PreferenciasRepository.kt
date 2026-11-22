package com.recuerdamed.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

// Un solo DataStore para toda la app (debe declararse a nivel de archivo)
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ajustes")

/** Todos los ajustes juntos, con sus valores por defecto. */
data class Ajustes(
    val tamanoLetra: Int = 1,                 // 0 = pequeña, 1 = mediana, 2 = grande
    val modoOscuro: Boolean = false,
    val vozAlarmaActiva: Boolean = true,
    val minutosEntreRepeticiones: Int = 10,   // solo 5, 10 o 15
    val nombrePersona: String = "",           // opcional, para el SMS
    val familiarNombre: String = "",
    val familiarTelefono: String = ""
) {
    /** Si el nombre está vacío, el SMS usa "Tu familiar". */
    val nombreParaSms: String get() = nombrePersona.ifBlank { "Tu familiar" }

    /** Sin teléfono no hay a quién mandar el SMS. */
    val hayFamiliar: Boolean get() = familiarTelefono.isNotBlank()
}

class PreferenciasRepository(context: Context) {

    private val dataStore = context.applicationContext.dataStore

    private object Claves {
        val TAMANO_LETRA = intPreferencesKey("tamano_letra")
        val MODO_OSCURO = booleanPreferencesKey("modo_oscuro")
        val VOZ_ALARMA = booleanPreferencesKey("voz_alarma")
        val MINUTOS_REPETICION = intPreferencesKey("minutos_repeticion")
        val NOMBRE_PERSONA = stringPreferencesKey("nombre_persona")
        val FAMILIAR_NOMBRE = stringPreferencesKey("familiar_nombre")
        val FAMILIAR_TELEFONO = stringPreferencesKey("familiar_telefono")
    }

    /** Se actualiza sola cada vez que cambia un ajuste. */
    val ajustes: Flow<Ajustes> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { p ->
            Ajustes(
                tamanoLetra = p[Claves.TAMANO_LETRA] ?: 1,
                modoOscuro = p[Claves.MODO_OSCURO] ?: false,
                vozAlarmaActiva = p[Claves.VOZ_ALARMA] ?: true,
                minutosEntreRepeticiones = p[Claves.MINUTOS_REPETICION] ?: 10,
                nombrePersona = p[Claves.NOMBRE_PERSONA] ?: "",
                familiarNombre = p[Claves.FAMILIAR_NOMBRE] ?: "",
                familiarTelefono = p[Claves.FAMILIAR_TELEFONO] ?: ""
            )
        }

    /** Lectura única, para receptores de alarma y Workers (que no observan). */
    suspend fun obtener(): Ajustes = ajustes.first()

    suspend fun guardarTamanoLetra(nivel: Int) {
        dataStore.edit { it[Claves.TAMANO_LETRA] = nivel.coerceIn(0, 2) }
    }

    suspend fun guardarModoOscuro(activo: Boolean) {
        dataStore.edit { it[Claves.MODO_OSCURO] = activo }
    }

    suspend fun guardarVozAlarma(activa: Boolean) {
        dataStore.edit { it[Claves.VOZ_ALARMA] = activa }
    }

    /** Solo acepta 5, 10 o 15; cualquier otro valor se guarda como 10. */
    suspend fun guardarMinutosRepeticion(minutos: Int) {
        val valido = if (minutos in OPCIONES_REPETICION) minutos else 10
        dataStore.edit { it[Claves.MINUTOS_REPETICION] = valido }
    }

    suspend fun guardarNombrePersona(nombre: String) {
        dataStore.edit { it[Claves.NOMBRE_PERSONA] = nombre.trim() }
    }

    suspend fun guardarFamiliar(nombre: String, telefono: String) {
        dataStore.edit {
            it[Claves.FAMILIAR_NOMBRE] = nombre.trim()
            it[Claves.FAMILIAR_TELEFONO] = telefono.trim()
        }
    }

    companion object {
        /** Constante de la app, no una preferencia (sección 8.3 del documento). */
        const val MAX_REPETICIONES = 3

        val OPCIONES_REPETICION = listOf(5, 10, 15)
    }
}