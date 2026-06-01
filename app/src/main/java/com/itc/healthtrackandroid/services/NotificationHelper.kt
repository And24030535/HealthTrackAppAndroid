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

// objeto auxiliar que crea los canales de notificacion y muestra los avisos de recordatorio y medicamentos
object NotificationHelper {

    private const val CHANNEL_ID      = "health_reminder_channel"
    private const val CHANNEL_NAME    = "Recordatorios de Salud"
    private const val NOTIFICATION_ID = 1001

    // canal dedicado para que los recordatorios de medicamentos tengan prioridad alta independiente
    private const val MED_CHANNEL_ID   = "medication_reminder_channel"
    private const val MED_CHANNEL_NAME = "Recordatorios de Medicamentos"

    // registra los dos canales de notificacion en el sistema y en Android anterior a 8 no hace nada
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            // canal de recordatorio diario para registrar metricas de salud
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Recordatorio diario para registrar tus metricas de salud" }
            manager.createNotificationChannel(channel)
            // canal de alta prioridad para que los medicamentos no se pierdan entre otras notificaciones
            val medChannel = NotificationChannel(
                MED_CHANNEL_ID, MED_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Recordatorios para tomar tus medicamentos a tiempo" }
            manager.createNotificationChannel(medChannel)
        }
    }

    // muestra la notificacion de un medicamento especifico con su nombre y dosis al momento de la alarma
    // llamamos a createChannel aqui como red de seguridad porque el receiver puede dispararse sin que
    // el app este en primer plano y por lo tanto sin que DashboardActivity haya creado los canales antes
    fun showMedicationNotification(context: Context, medicineName: String,
                                   dose: String, notifId: Int) {
        createChannel(context)
        val doseText = if (dose.isNotBlank()) " · $dose" else ""
        val notification = NotificationCompat.Builder(context, MED_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_healthtrack_logo)
            .setContentTitle("HealthTrack  Recordatorio de Medicamento")
            .setContentText("Hora de tomar $medicineName$doseText")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Hora de tomar $medicineName$doseText"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(notifId, notification)
    }

    // muestra el recordatorio diario de metricas y al tocarlo lleva al paciente al panel principal
    fun showReminderNotification(context: Context) {
        // igual que en showMedicationNotification llamamos createChannel por si no se creo antes
        createChannel(context)
        Log.d("NotificationHelper", "showReminderNotification llamado")
        // al tocar la notificacion el paciente llega directo al dashboard
        val intent = Intent(context, DashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_healthtrack_logo)
            .setContentTitle("HealthTrack  Recordatorio")
            .setContentText("No olvides registrar tus metricas de salud de hoy")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }
}
