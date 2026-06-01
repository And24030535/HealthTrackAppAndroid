package com.itc.healthtrackandroid.controllers

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.itc.healthtrackandroid.R
import com.itc.healthtrackandroid.dao.GenericDAO
import com.itc.healthtrackandroid.dao.OnOperationCompleteListener
import com.itc.healthtrackandroid.dao.OnSingleDataLoadedListener
import com.itc.healthtrackandroid.models.Metric
import com.itc.healthtrackandroid.models.User
import com.itc.healthtrackandroid.services.ReminderScheduler
import kotlin.math.roundToInt

// pantalla para registrar metricas con calculo automatico de IMC cuando el paciente ingresa su peso
class AddMetricActivity : AppCompatActivity() {

    private lateinit var weightEditText: EditText
    private lateinit var systolicEditText: EditText
    private lateinit var diastolicEditText: EditText
    private lateinit var heartRateEditText: EditText
    private lateinit var glucoseEditText: EditText
    private lateinit var saveMetricButton: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var metricDao: GenericDAO<Metric>
    private lateinit var userDao: GenericDAO<User>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_metric)

        auth      = FirebaseAuth.getInstance()
        metricDao = GenericDAO(Metric::class.java, "metrics")
        userDao   = GenericDAO(User::class.java, "users")

        weightEditText    = findViewById(R.id.weightEditText)
        systolicEditText  = findViewById(R.id.systolicEditText)
        diastolicEditText = findViewById(R.id.diastolicEditText)
        heartRateEditText = findViewById(R.id.heartRateEditText)
        glucoseEditText   = findViewById(R.id.glucoseEditText)
        saveMetricButton  = findViewById(R.id.saveMetricButton)

        saveMetricButton.setOnClickListener { saveMetricToDatabase() }
    }

    // lee los campos del formulario y decide si hay que descargar la altura para calcular el IMC
    private fun saveMetricToDatabase() {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            Toast.makeText(this, "Sesión de usuario no encontrada", Toast.LENGTH_SHORT).show()
            return
        }

        saveMetricButton.isEnabled = false

        val weightValue    = weightEditText.text.toString().toDoubleOrNull()
        val systolicValue  = systolicEditText.text.toString().toIntOrNull()
        val diastolicValue = diastolicEditText.text.toString().toIntOrNull()
        val heartRateValue = heartRateEditText.text.toString().toIntOrNull()
        val glucoseValue   = glucoseEditText.text.toString().toDoubleOrNull()

        // el paciente debe ingresar al menos un valor para que guardemos la metrica
        if (weightValue == null && systolicValue == null && diastolicValue == null &&
            heartRateValue == null && glucoseValue == null) {
            Toast.makeText(this, "Por favor ingresa al menos una métrica", Toast.LENGTH_SHORT).show()
            saveMetricButton.isEnabled = true
            return
        }

        // si hay peso descargamos el perfil para obtener la altura y calcular el IMC
        if (weightValue != null) {
            userDao.getById(currentUserId, object : OnSingleDataLoadedListener<User> {
                override fun onSuccess(data: User?) {
                    if (isFinishing || isDestroyed) return
                    val height = data?.height
                    val bmi = if (height != null && height > 0.0) {
                        val raw = weightValue / (height * height)
                        (raw * 10).roundToInt() / 10.0
                    } else null
                    buildAndSaveMetric(currentUserId, weightValue, systolicValue, diastolicValue,
                        heartRateValue, glucoseValue, bmi)
                }
                override fun onFailure(error: Exception) {
                    if (isFinishing || isDestroyed) return
                    // si falla la descarga del perfil guardamos la metrica sin IMC
                    buildAndSaveMetric(currentUserId, weightValue, systolicValue, diastolicValue,
                        heartRateValue, glucoseValue, null)
                }
            })
        } else {
            buildAndSaveMetric(currentUserId, null, systolicValue, diastolicValue,
                heartRateValue, glucoseValue, null)
        }
    }

    // arma el objeto Metric con todos los campos calculados y lo envia a Firestore
    private fun buildAndSaveMetric(
        userId: String, weight: Double?, systolic: Int?, diastolic: Int?,
        heartRate: Int?, glucose: Double?, bmi: Double?
    ) {
        val newMetricId = metricDao.createDocumentId()
        val newMetric = Metric(
            id           = newMetricId,
            patientId    = userId,
            timestamp    = Timestamp.now(),
            metricType   = "General",
            weight       = weight,
            bmi          = bmi,
            systolic     = systolic,
            diastolic    = diastolic,
            heartRate    = heartRate,
            glucoseLevel = glucose
        )

        metricDao.save(newMetricId, newMetric, object : OnOperationCompleteListener {
            override fun onSuccess() {
                if (isFinishing || isDestroyed) return
                Toast.makeText(this@AddMetricActivity, "Métrica guardada correctamente", Toast.LENGTH_SHORT).show()
                // programamos el recordatorio por defecto a las 9am solo si el paciente no tiene uno configurado aun
                if (ReminderScheduler.getSavedTime(this@AddMetricActivity) == null) {
                    ReminderScheduler.schedule(this@AddMetricActivity, 9, 0)
                    Log.d("AddMetricActivity", "ReminderScheduler.schedule(9 0) ejecutado")
                } else {
                    Log.d("AddMetricActivity", "Recordatorio ya configurado no se sobreescribe")
                }
                finish()
            }
            override fun onFailure(error: Exception) {
                if (isFinishing || isDestroyed) return
                Toast.makeText(this@AddMetricActivity, "Error al guardar: ${error.message}", Toast.LENGTH_LONG).show()
                saveMetricButton.isEnabled = true
            }
        })
    }
}
