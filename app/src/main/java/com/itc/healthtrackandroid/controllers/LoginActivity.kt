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

// pantalla de inicio de sesion para pacientes con flujo correo y contrasena hacia Firebase Auth
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

        auth    = FirebaseAuth.getInstance()
        userDao = GenericDAO(User::class.java, "users")

        emailEditText    = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        loginButton      = findViewById(R.id.loginButton)
        registerTextView = findViewById(R.id.registerTextView)

        loginButton.setOnClickListener { performLogin() }

        registerTextView.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // si el paciente ya tiene sesion activa lo mandamos directo al dashboard sin que tenga que escribir nada
        val existingUser = auth.currentUser
        if (existingUser != null) {
            loginButton.isEnabled = false
            fetchUserAndNavigate(existingUser.uid)
        }
    }

    private fun performLogin() {
        val email    = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa correo y contraseña", Toast.LENGTH_SHORT).show()
            return
        }

        // deshabilitamos el boton para evitar que el paciente lo toque dos veces seguidas
        loginButton.isEnabled = false

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

    // busca el perfil del usuario en Firestore para saber su rol y nombre antes de navegar
    private fun fetchUserAndNavigate(userId: String) {
        userDao.getById(userId, object : OnSingleDataLoadedListener<User> {

            override fun onSuccess(data: User?) {
                if (isFinishing || isDestroyed) return
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

    // lanza el dashboard limpiando el historial para que al presionar atras se cierre la app
    private fun navigateToDashboard(data: User) {
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
