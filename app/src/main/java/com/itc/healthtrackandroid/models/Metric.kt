package com.itc.healthtrackandroid.models

import com.google.firebase.Timestamp

// representa una medicion individual que el paciente registra en la pantalla de nueva metrica
data class Metric(
    var id: String? = null,
    var patientId: String = "",
    // usamos el Timestamp de Firestore para guardar fecha y hora exactas
    var timestamp: Timestamp? = null,
    var metricType: String? = null,
    // presion sistolica el numero de arriba
    var systolic: Int? = null,
    // presion diastolica el numero de abajo
    var diastolic: Int? = null,
    // frecuencia cardiaca en latidos por minuto
    var heartRate: Int? = null,
    // peso del paciente en kilogramos
    var weight: Double? = null,
    // indice de masa corporal calculado a partir del peso y la estatura
    var bmi: Double? = null,
    // nivel de glucosa en sangre en mg/dL
    var glucoseLevel: Double? = null
)
