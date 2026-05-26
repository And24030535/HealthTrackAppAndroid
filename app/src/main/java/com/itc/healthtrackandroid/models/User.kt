package com.itc.healthtrackandroid.models

// representa a cualquier usuario del sistema (paciente doctor o administrador) con valores por defecto para que Firebase pueda crear el objeto
data class User(
    var uid: String? = null,
    var email: String = "",
    var firstName: String = "",
    var lastName: String = "",
    // rol patient doctor o admin
    var role: String = "",
    // fecha de nacimiento guardada como texto
    var birthDate: String? = null,
    // genero M F u Other
    var gender: String? = null,
    // estatura en metros con decimales (ej 1.75)
    var height: Double? = null,
    // id del medico que atiende al paciente solo aplica si el rol es patient
    var assignedDoctorId: String? = null,
    // nombre del medico asignado para mostrarlo facilmente en la pantalla
    var assignedDoctorName: String? = null,
    // lista de ids de pacientes asignados solo aplica si el rol es doctor
    var patientIds: List<String>? = null
)