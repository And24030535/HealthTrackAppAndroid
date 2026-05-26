package com.itc.healthtrackandroid.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.itc.healthtrackandroid.services.ReminderScheduler

// receiver que se activa al terminar el arranque y reprograma el recordatorio diario porque los AlarmManager se borran al reiniciar
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // si habia un recordatorio guardado lo volvemos a programar tras el reinicio
            val savedTime = ReminderScheduler.getSavedTime(context)
            if (savedTime != null) {
                ReminderScheduler.schedule(context, savedTime.first, savedTime.second)
            }
        }
    }
}
