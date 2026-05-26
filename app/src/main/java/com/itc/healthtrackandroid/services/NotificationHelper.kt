package com.itc.healthtrackandroid.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.itc.healthtrackandroid.R
import com.itc.healthtrackandroid.controllers.DashboardActivity

// clase auxiliar para crear el canal de notificaciones y mostrar el recordatorio diario
object NotificationHelper {

    private const val CHANNEL_ID      = "health_reminder_channel"
    private const val CHANNEL_NAME    = "Recordatorios de Salud"
    private const val NOTIFICATION_ID = 1001

    // crea el canal de notificaciones obligatorio en android 8 y superior (en versiones anteriores no hace nada)
    fun createChannel(context: Context) {
        // creamos el canal de notificaciones para android 8 y superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = "Recordatorio diario para registrar tus metricas de salud"
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    // muestra la notificacion de recordatorio que al tocarla abre el panel principal del paciente
    fun showReminderNotification(context: Context) {
        // nos aseguramos que el canal exista antes de mostrar la notificacion
        createChannel(context)
        Log.d("NotificationHelper", "showReminderNotification llamado")
        // al tocar la notificacion el paciente va directo al dashboard
        val intent = Intent(context, DashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_healthtrack_logo)
            .setContentTitle("HealthTrack — Recordatorio")
            .setContentText("No olvides registrar tus metricas de salud de hoy.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }
}
