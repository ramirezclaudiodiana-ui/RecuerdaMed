package com.recuerdamed.voz

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume

/**
 * Voz en español. [comoAlarma] = true usa el volumen de alarma (para cuando suena la alarma);
 * false usa el volumen normal (botón de bocina en pantallas).
 * Crear en el hilo principal y llamar a cerrar() al terminar.
 */
class VozHelper(context: Context, private val comoAlarma: Boolean = false) {

    private var tts: TextToSpeech? = null
    private var listo = false
    private var fallo = false
    private var pendiente: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext) { estado ->
            if (estado == TextToSpeech.SUCCESS) {
                configurar()
                listo = true
            } else {
                fallo = true
            }
            pendiente?.invoke()
            pendiente = null
        }
    }

    private fun configurar() {
        val t = tts ?: return
        val resultado = t.setLanguage(Locale.forLanguageTag("es-MX"))
        if (resultado == TextToSpeech.LANG_MISSING_DATA || resultado == TextToSpeech.LANG_NOT_SUPPORTED) {
            t.setLanguage(Locale.forLanguageTag("es"))
        }
        t.setSpeechRate(0.9f)   // un poco más lento, pensando en adultos mayores
        if (comoAlarma) {
            t.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
        }
    }

    /** Dice el texto. Si el motor aún no está listo, espera a que lo esté. */
    fun hablar(texto: String, alTerminar: (() -> Unit)? = null) {
        val accion: () -> Unit = {
            if (listo) decir(texto, alTerminar) else alTerminar?.invoke()
        }
        if (listo || fallo) accion() else pendiente = accion
    }

    /** Para el receptor de alarma: habla y espera a terminar (máximo [timeoutMs]). */
    suspend fun hablarYEsperar(texto: String, timeoutMs: Long = 8_000) {
        withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine<Unit> { cont ->
                hablar(texto) { if (cont.isActive) cont.resume(Unit) }
            }
        }
        cerrar()
    }

    private fun decir(texto: String, alTerminar: (() -> Unit)?) {
        val t = tts ?: run { alTerminar?.invoke(); return }
        t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { alTerminar?.invoke() }
            @Suppress("OVERRIDE_DEPRECATION")
            override fun onError(utteranceId: String?) { alTerminar?.invoke() }
        })
        val resultado = t.speak(texto, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
        if (resultado != TextToSpeech.SUCCESS) alTerminar?.invoke()
    }

    fun cerrar() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        listo = false
    }

    companion object {
        const val FRASE_ALARMA = "Es hora de tu medicamento"
        const val FRASE_REPETICION = "Recuerda tomar tu medicamento"
        const val FRASE_REGISTRADO = "Registrado, buen trabajo"
    }
}