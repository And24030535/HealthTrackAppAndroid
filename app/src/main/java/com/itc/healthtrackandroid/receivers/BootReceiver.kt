package com.itc.healthtrackandroid.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.itc.healthtrackandroid.services.MedicationReminderScheduler
import com.itc.healthtrackandroid.services.ReminderScheduler

// se activa al terminar el arranque del sistema para reprogramar las alarmas porque
// el AlarmManager las pierde cuando el dispositivo se apaga o reinicia
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // restauramos el recordatorio diario de metricas si el paciente lo tenia configurado
            val savedTime = ReminderScheduler.getSavedTime(context)
            if (savedTime != null) {
                ReminderScheduler.schedule(context, savedTime.first, savedTime.second)
            }
            // restauramos los recordatorios de medicamentos usando solo SharedPreferences
            // para no bloquear el arranque del sistema con consultas a Firestore
            MedicationReminderScheduler.rescheduleFromPrefs(context)
        }
    }
}
