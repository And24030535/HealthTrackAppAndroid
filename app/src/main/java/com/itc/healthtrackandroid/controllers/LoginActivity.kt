package com.itc.healthtrackandroid.controllers

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
import com.itc.healthtrackandroid.dao.OnSingleDataLoadedListener
import com.itc.healthtrackandroid.models.User

// pantalla de inicio de sesion para pacientes con flujo correo y contrasena hacia firebase auth
class LoginActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var registerTextView: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var userDao: GenericDAO<User>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // inicializamos firebase auth y el dao de usuarios
        auth    = FirebaseAuth.getInstance()
        userDao = GenericDAO(User::class.java, "users")

        // conectamos cada vista con su variable en kotlin
        emailEditText    = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        loginButton      = findViewById(R.id.loginButton)
        registerTextView = findViewById(R.id.registerTextView)

        // asignamos los listeners de los botones
        loginButton.setOnClickListener { performLogin() }

        registerTextView.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // si el usuario ya tiene sesion activa lo mandamos directo al dashboard
        val existingUser = auth.currentUser
        if (existingUser != null) {
            loginButton.isEnabled = false
            fetchUserAndNavigate(existingUser.uid)
        }
    }

    // login

    private fun performLogin() {
        val email    = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        // verificamos que los campos no esten vacios antes de continuar
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa correo y contraseña", Toast.LENGTH_SHORT).show()
            return
        }

        // deshabilitamos el boton para evitar doble envio
        loginButton.isEnabled = false

        // intentamos autenticar al usuario con firebase
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    if (userId != null) fetchUserAndNavigate(userId)
                } else {
                    Toast.makeText(
                        this,
                        "Error de autenticación: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    loginButton.isEnabled = true
                }
            }
    }

    private fun fetchUserAndNavigate(userId: String) {
        // buscamos el perfil del usuario en firestore para saber su rol
        userDao.getById(userId, object : OnSingleDataLoadedListener<User> {

            override fun onSuccess(data: User?) {
                // evitamos actualizar la pantalla si ya se cerro
                if (isFinishing || isDestroyed) return
                // si no existe el perfil en firestore avisamos y cancelamos
                if (data == null) {
                    Toast.makeText(
                        this@LoginActivity,
                        "Usuario no encontrado en la base de datos",
                        Toast.LENGTH_SHORT
                    ).show()
                    loginButton.isEnabled = true
                    return
                }
                navigateToDashboard(data)
            }

            override fun onFailure(error: Exception) {
                if (isFinishing || isDestroyed) return
                Toast.makeText(
                    this@LoginActivity,
                    "Error al obtener datos del usuario: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
                loginButton.isEnabled = true
            }
        })
    }

    // navegacion

    private fun navigateToDashboard(data: User) {
        // limpiamos el historial de actividades para que al presionar atras se cierre la app
        val intent = Intent(this, DashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("USER_ROLE", data.role)
            putExtra("USER_UID",  data.uid)
            putExtra("USER_NAME", "${data.firstName} ${data.lastName}")
        }
        startActivity(intent)
        finish()
    }
}
