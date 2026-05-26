package com.itc.healthtrackandroid.controllers

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.itc.healthtrackandroid.R
import com.itc.healthtrackandroid.services.NotificationHelper
import com.itc.healthtrackandroid.services.ReminderScheduler
import java.util.Calendar

// panel principal del paciente con navegacion a metricas historial recordatorios y cierre de sesion
class DashboardActivity : AppCompatActivity() {

    private lateinit var welcomeTextView: TextView
    private lateinit var addMetricButton: Button
    private lateinit var viewHistoryButton: Button
    private lateinit var reminderButton: Button
    private lateinit var logoutButton: Button

    private lateinit var auth: FirebaseAuth

    // lanzador del permiso de notificaciones que solo aplica en android 13 y superior
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openTimePicker()
        } else {
            Toast.makeText(this, "Permiso de notificaciones denegado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // creamos el canal de notificaciones la primera vez que se abre el dashboard (en android menor a 8 no hace nada)
        NotificationHelper.createChannel(this)

        // conectamos firebase auth y las vistas del dashboard
        auth = FirebaseAuth.getInstance()

        welcomeTextView   = findViewById(R.id.welcomeTextView)
        addMetricButton   = findViewById(R.id.addMetricButton)
        viewHistoryButton = findViewById(R.id.viewHistoryButton)
        reminderButton    = findViewById(R.id.reminderButton)
        logoutButton      = findViewById(R.id.logoutButton)

        // mostramos el nombre del paciente que viene del intent del login
        val userName = intent.getStringExtra("USER_NAME") ?: ""
        welcomeTextView.text = if (userName.isNotEmpty())
            "Bienvenido, $userName\nPaciente"
        else
            "Bienvenido a HealthTrack"

        // mostramos la hora del recordatorio activo si ya fue configurado antes
        updateReminderButtonLabel()

        // navegamos a las distintas pantallas segun el boton presionado
        addMetricButton.setOnClickListener {
            startActivity(Intent(this, AddMetricActivity::class.java))
        }

        viewHistoryButton.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        reminderButton.setOnClickListener { requestNotificationPermissionOrOpenPicker() }

        logoutButton.setOnClickListener { performLogout() }
    }

    // recordatorio

    // checamos el permiso POST_NOTIFICATIONS en android 13 antes de abrir el selector de hora
    private fun requestNotificationPermissionOrOpenPicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
                openTimePicker()
            } else {
                notificationPermissionLauncher.launch(permission)
            }
        } else {
            openTimePicker()
        }
    }

    // muestra el selector de hora para configurar el recordatorio diario usando el huso horario local
    private fun openTimePicker() {
        // tomamos la hora actual del telefono como valor inicial del selector
        val calendar      = Calendar.getInstance()
        val currentHour   = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        // cuando el paciente elige una hora la programamos y actualizamos el boton
        TimePickerDialog(this, { _, hour, minute ->
            ReminderScheduler.schedule(this, hour, minute)
            updateReminderButtonLabel()
            val label = String.format("%02d:%02d", hour, minute)
            Toast.makeText(this, "Recordatorio programado a las $label", Toast.LENGTH_SHORT).show()
        }, currentHour, currentMinute, true).show()
    }

    // actualiza el texto del boton segun la hora del recordatorio activo o si esta inactivo
    private fun updateReminderButtonLabel() {
        // mostramos la hora guardada en el boton o el texto por defecto si no hay recordatorio
        val saved = ReminderScheduler.getSavedTime(this)
        reminderButton.text = if (saved != null) {
            val label = String.format("%02d:%02d", saved.first, saved.second)
            "Recordatorio: $label"
        } else {
            "Configurar Recordatorio"
        }
    }

    // sesion

    // cierra la sesion en firebase y vuelve al login
    private fun performLogout() {
        // cerramos la sesion en firebase y limpiamos el historial de actividades
        auth.signOut()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
