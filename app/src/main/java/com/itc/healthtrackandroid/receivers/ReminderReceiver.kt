package com.itc.healthtrackandroid.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.itc.healthtrackandroid.services.NotificationHelper

// el AlarmManager activa este receiver a la hora programada y solo tiene que mostrar el recordatorio
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("ReminderReceiver", "onReceive disparado por el AlarmManager")
        NotificationHelper.showReminderNotification(context)
    }
}
