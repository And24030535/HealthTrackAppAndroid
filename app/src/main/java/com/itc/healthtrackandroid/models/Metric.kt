package com.itc.healthtrackandroid.models

import com.google.firebase.Timestamp

// representa una medicion individual registrada por el paciente
data class Metric(
    var id: String? = null,
    var patientId: String = "",
    // usamos el Timestamp de Firebase para fecha y hora exacta
    var timestamp: Timestamp? = null,
    var metricType: String? = null,
    // sistolica numero de arriba
    var systolic: Int? = null,
    // diastolica numero de abajo
    var diastolic: Int? = null,
    // frecuencia cardiaca en latidos por minuto
    var heartRate: Int? = null,
    // peso en kilogramos
    var weight: Double? = null,
    // indice de masa corporal calculado
    var bmi: Double? = null,
    // nivel de glucosa en la sangre
    var glucoseLevel: Double? = null
)