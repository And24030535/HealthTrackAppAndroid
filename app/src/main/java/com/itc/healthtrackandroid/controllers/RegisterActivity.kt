package com.itc.healthtrackandroid.controllers

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.itc.healthtrackandroid.R
import com.itc.healthtrackandroid.dao.GenericDAO
import com.itc.healthtrackandroid.dao.OnOperationCompleteListener
import com.itc.healthtrackandroid.models.User
import java.util.Calendar

// pantalla de registro de pacientes que crea la cuenta en firebase auth y guarda el perfil completo en firestore
class RegisterActivity : AppCompatActivity() {

    private lateinit var firstNameEditText: EditText
    private lateinit var lastNameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var heightEditText: EditText
    private lateinit var birthDateButton: Button
    private lateinit var birthDateTextView: TextView
    private lateinit var registerButton: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var userDao: GenericDAO<User>

    // fecha seleccionada en el picker en formato YYYY-MM-DD
    private var selectedBirthDate: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // inicializamos firebase auth y el dao para guardar el perfil
        auth    = FirebaseAuth.getInstance()
        userDao = GenericDAO(User::class.java, "users")

        // conectamos los campos del formulario con sus variables
        firstNameEditText  = findViewById(R.id.firstNameEditText)
        lastNameEditText   = findViewById(R.id.lastNameEditText)
        emailEditText      = findViewById(R.id.emailEditText)
        passwordEditText   = findViewById(R.id.passwordEditText)
        heightEditText     = findViewById(R.id.heightEditText)
        birthDateButton    = findViewById(R.id.birthDateButton)
        birthDateTextView  = findViewById(R.id.birthDateTextView)
        registerButton     = findViewById(R.id.registerButton)

        birthDateButton.setOnClickListener { openDatePicker() }
        registerButton.setOnClickListener { performRegistration() }
    }

    // abre el selector y guarda la fecha en formato ISO
    private fun openDatePicker() {
        // abrimos el selector de fecha y guardamos el resultado en formato ISO
        val cal = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                selectedBirthDate = "%04d-%02d-%02d".format(year, month + 1, day)
                birthDateTextView.text = selectedBirthDate
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // valida los campos obligatorios y crea la cuenta
    private fun performRegistration() {
        val firstName = firstNameEditText.text.toString().trim()
        val lastName  = lastNameEditText.text.toString().trim()
        val email     = emailEditText.text.toString().trim()
        val password  = passwordEditText.text.toString().trim()

        // validamos que todos los campos obligatorios tengan valor
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        // la contrasena debe tener al menos 6 caracteres para firebase
        if (password.length < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
            return
        }

        // la altura es opcional pero la necesitamos para calcular el IMC despues
        val heightRaw = heightEditText.text.toString().trim()
        val height = heightRaw.toDoubleOrNull()

        // validamos que la altura sea un numero razonable si se ingresa
        if (heightRaw.isNotEmpty() && (height == null || height <= 0.0 || height > 3.0)) {
            Toast.makeText(this, "Estatura inválida (usa metros, ej. 1.75)", Toast.LENGTH_SHORT).show()
            return
        }

        // exigimos la fecha de nacimiento para que el doctor tenga la edad del paciente
        if (selectedBirthDate.isNullOrEmpty()) {
            Toast.makeText(this, "Por favor selecciona tu fecha de nacimiento", Toast.LENGTH_SHORT).show()
            return
        }

        // deshabilitamos el boton para evitar doble registro
        registerButton.isEnabled = false

        // creamos la cuenta en firebase auth con correo y contrasena
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    if (userId != null) saveUserProfile(userId, firstName, lastName, email, height)
                } else {
                    Toast.makeText(
                        this,
                        "Error al registrar: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    registerButton.isEnabled = true
                }
            }
    }

    // guarda el perfil del paciente en firestore y navega al dashboard
    private fun saveUserProfile(
        userId: String,
        firstName: String,
        lastName: String,
        email: String,
        height: Double?
    ) {
        // armamos el objeto de usuario con el rol de paciente y lo guardamos en firestore
        val newUser = User(
            uid       = userId,
            email     = email,
            firstName = firstName,
            lastName  = lastName,
            role      = "patient",
            height    = height,
            birthDate = selectedBirthDate
        )

        userDao.save(userId, newUser, object : OnOperationCompleteListener {

            override fun onSuccess() {
                if (isFinishing || isDestroyed) return
                Toast.makeText(this@RegisterActivity, "Cuenta creada exitosamente", Toast.LENGTH_SHORT).show()
                // limpiamos el historial de actividades igual que en el login
                val intent = Intent(this@RegisterActivity, DashboardActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("USER_ROLE", "patient")
                    putExtra("USER_UID",  userId)
                    putExtra("USER_NAME", "$firstName $lastName")
                }
                startActivity(intent)
                finish()
            }

            override fun onFailure(error: Exception) {
                // revertimos la cuenta de firebase auth para no dejar cuentas huerfanas sin perfil en firestore
                auth.currentUser?.delete()
                if (isFinishing || isDestroyed) return
                Toast.makeText(
                    this@RegisterActivity,
                    "Error al guardar el perfil: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
                registerButton.isEnabled = true
            }
        })
    }
}
