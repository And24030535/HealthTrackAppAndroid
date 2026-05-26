package com.itc.healthtrackandroid.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.itc.healthtrackandroid.services.NotificationHelper

// receiver que el sistema activa cuando llega la hora del recordatorio y solo muestra la notificacion
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("ReminderReceiver", "onReceive disparado por el AlarmManager")
        // cuando suena la alarma mostramos la notificacion de recordatorio al paciente
        NotificationHelper.showReminderNotification(context)
    }
}
