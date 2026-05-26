package com.itc.healthtrackandroid.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.itc.healthtrackandroid.receivers.ReminderReceiver
import java.util.Calendar
import java.util.TimeZone

// gestiona la programacion y cancelacion del recordatorio diario con AlarmManager usando el huso horario local
object ReminderScheduler {

    private const val REQUEST_CODE = 2001
    private const val PREFS_NAME   = "reminder_prefs"
    private const val KEY_HOUR     = "reminder_hour"
    private const val KEY_MINUTE   = "reminder_minute"
    private const val KEY_ENABLED  = "reminder_enabled"

    // programa el recordatorio diario a la hora indicada y cancela cualquier alarma anterior
    fun schedule(context: Context, hour: Int, minute: Int) {
        // guardamos la hora elegida en SharedPreferences para poder recuperarla despues
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putInt(KEY_HOUR, hour)
            .putInt(KEY_MINUTE, minute)
            .putBoolean(KEY_ENABLED, true)
            .apply()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildPendingIntent(context)

        // cancelamos la alarma anterior para evitar duplicados
        alarmManager.cancel(pendingIntent)

        // calculamos el proximo disparo usando el huso horario local del dispositivo
        val calendar = Calendar.getInstance(TimeZone.getDefault())
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // si la hora ya paso hoy programamos para manana
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // setInexactRepeating no requiere permiso especial y alcanza para recordatorios diarios
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    // cancela el recordatorio activo y borra la configuracion guardada
    fun cancel(context: Context) {
        // desactivamos el recordatorio y borramos la configuracion guardada
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ENABLED, false)
            .apply()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(buildPendingIntent(context))
    }

    // devuelve la hora y minuto guardados o null si no hay recordatorio activo
    fun getSavedTime(context: Context): Pair<Int, Int>? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_ENABLED, false)) return null
        val hour   = prefs.getInt(KEY_HOUR, -1)
        val minute = prefs.getInt(KEY_MINUTE, -1)
        return if (hour >= 0 && minute >= 0) Pair(hour, minute) else null
    }

    private fun buildPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
