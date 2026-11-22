package com.recuerdamed.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.recuerdamed.alarm.ManejadorAlertas

/** Corre cada día: mantiene 7 días de tomas generadas y las alarmas programadas. */
class RegenerarTomasWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            ManejadorAlertas(applicationContext).reprogramarTodo()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}