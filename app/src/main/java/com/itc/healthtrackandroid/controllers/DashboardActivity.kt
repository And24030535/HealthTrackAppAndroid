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
import com.google.firebase.firestore.ListenerRegistration
import com.itc.healthtrackandroid.R
import com.itc.healthtrackandroid.dao.GenericDAO
import com.itc.healthtrackandroid.dao.OnDataLoadedListener
import com.itc.healthtrackandroid.models.AppNotification
import com.itc.healthtrackandroid.services.MedicationReminderScheduler
import com.itc.healthtrackandroid.services.NotificationHelper
import com.itc.healthtrackandroid.services.ReminderScheduler
import java.util.Calendar

// panel principal del paciente con navegacion a todas las secciones y gestion de recordatorios
class DashboardActivity : AppCompatActivity() {

    private lateinit var welcomeTextView: TextView
    private lateinit var addMetricButton: Button
    private lateinit var viewHistoryButton: Button
    private lateinit var reminderButton: Button
    // boton de contactos de emergencia
    private lateinit var emergencyContactsButton: Button
    // boton de citas medicas
    private lateinit var appointmentsButton: Button
    // boton de alergias del paciente
    private lateinit var allergiesButton: Button
    // boton de notificaciones con contador de no leidas en el texto
    private lateinit var notificationsButton: Button
    private lateinit var logoutButton: Button

    private lateinit var auth: FirebaseAuth

    // listener del contador de no leidas que hay que cancelar en onDestroy para no desperdiciar recursos
    private var unreadNotifListener: ListenerRegistration? = null

    // lanzador del permiso de notificaciones que solo aplica en Android 13 y superior
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

        // creamos los canales de notificacion la primera vez que se abre el dashboard
        NotificationHelper.createChannel(this)

        auth = FirebaseAuth.getInstance()

        // programamos los recordatorios de medicamentos para los tratamientos activos del paciente
        auth.currentUser?.uid?.let { uid ->
            MedicationReminderScheduler.scheduleForPatient(this, uid)
        }

        welcomeTextView         = findViewById(R.id.welcomeTextView)
        addMetricButton         = findViewById(R.id.addMetricButton)
        viewHistoryButton       = findViewById(R.id.viewHistoryButton)
        reminderButton          = findViewById(R.id.reminderButton)
        emergencyContactsButton = findViewById(R.id.emergencyContactsButton)
        appointmentsButton      = findViewById(R.id.appointmentsButton)
        allergiesButton         = findViewById(R.id.allergiesButton)
        notificationsButton     = findViewById(R.id.notificationsButton)
        logoutButton            = findViewById(R.id.logoutButton)

        val userName = intent.getStringExtra("USER_NAME") ?: ""
        welcomeTextView.text = if (userName.isNotEmpty())
            "Bienvenido $userName\nPaciente"
        else
            "Bienvenido a HealthTrack"

        updateReminderButtonLabel()

        addMetricButton.setOnClickListener {
            startActivity(Intent(this, AddMetricActivity::class.java))
        }

        viewHistoryButton.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        reminderButton.setOnClickListener { requestNotificationPermissionOrOpenPicker() }

        emergencyContactsButton.setOnClickListener {
            startActivity(Intent(this, EmergencyContactsActivity::class.java))
        }

        appointmentsButton.setOnClickListener {
            startActivity(Intent(this, AppointmentsActivity::class.java))
        }

        allergiesButton.setOnClickListener {
            startActivity(Intent(this, AllergiesActivity::class.java))
        }

        notificationsButton.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        logoutButton.setOnClickListener { performLogout() }

        // arrancamos el listener de no leidas solo si hay sesion activa
        // pasamos el dao como parametro para no necesitar un campo lateinit que podria quedar sin inicializar
        auth.currentUser?.uid?.let { uid ->
            val notifDao = GenericDAO(AppNotification::class.java, "notifications")
            startListeningUnreadCount(uid, notifDao)
        }
    }

    // notificaciones

    // actualiza el texto del boton con la cantidad de notificaciones no leidas en tiempo real
    private fun startListeningUnreadCount(userId: String, notifDao: GenericDAO<AppNotification>) {
        unreadNotifListener = notifDao.listenByField(
            "userId",
            userId,
            object : OnDataLoadedListener<AppNotification> {
                override fun onSuccess(data: List<AppNotification>) {
                    if (isFinishing || isDestroyed) return
                    val unread = data.count { !it.read }
                    notificationsButton.text = if (unread > 0) "Notificaciones ($unread)" else "Notificaciones"
                }
                override fun onFailure(error: Exception) {
                    // silenciamos el error y el boton mantiene su texto por defecto
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        unreadNotifListener?.remove()
        unreadNotifListener = null
    }

    // recordatorio

    // verifica el permiso POST_NOTIFICATIONS en Android 13 antes de abrir el selector de hora
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

    // muestra el selector de hora y programa el recordatorio cuando el paciente confirma
    private fun openTimePicker() {
        val calendar      = Calendar.getInstance()
        val currentHour   = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(this, { _, hour, minute ->
            ReminderScheduler.schedule(this, hour, minute)
            updateReminderButtonLabel()
            val label = String.format("%02d:%02d", hour, minute)
            Toast.makeText(this, "Recordatorio programado a las $label", Toast.LENGTH_SHORT).show()
        }, currentHour, currentMinute, true).show()
    }

    // actualiza el texto del boton con la hora guardada o el mensaje por defecto si no hay recordatorio activo
    private fun updateReminderButtonLabel() {
        val saved = ReminderScheduler.getSavedTime(this)
        reminderButton.text = if (saved != null) {
            val label = String.format("%02d:%02d", saved.first, saved.second)
            "Recordatorio: $label"
        } else {
            "Configurar Recordatorio"
        }
    }

    // sesion

    // cierra la sesion en Firebase y vuelve al login limpiando el historial de actividades
    private fun performLogout() {
        auth.signOut()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
