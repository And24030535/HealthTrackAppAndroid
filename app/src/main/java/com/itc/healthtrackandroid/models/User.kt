package com.itc.healthtrackandroid.models

// representa a cualquier usuario del sistema ya sea paciente doctor o administrador
// todos los campos tienen valores por defecto para que Firestore pueda reconstruir el objeto
data class User(
    var uid: String? = null,
    var email: String = "",
    var firstName: String = "",
    var lastName: String = "",
    // el rol puede ser patient doctor o admin
    var role: String = "",
    // fecha de nacimiento guardada como texto en formato yyyy-MM-dd
    var birthDate: String? = null,
    // genero del usuario puede ser M F u Other
    var gender: String? = null,
    // estatura en metros con decimales por ejemplo 1.75
    var height: Double? = null,
    // id del medico asignado al paciente solo tiene valor cuando el rol es patient
    var assignedDoctorId: String? = null,
    // nombre del medico asignado desnormalizado para mostrarlo sin consulta adicional
    var assignedDoctorName: String? = null,
    // lista de ids de pacientes asignados solo tiene valor cuando el rol es doctor
    var patientIds: List<String>? = null
)
