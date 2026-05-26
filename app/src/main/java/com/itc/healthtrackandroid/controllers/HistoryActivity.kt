package com.itc.healthtrackandroid.controllers

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.itc.healthtrackandroid.R
import com.itc.healthtrackandroid.adapters.ColoredMetricAdapter
import com.itc.healthtrackandroid.dao.GenericDAO
import com.itc.healthtrackandroid.dao.OnDataLoadedListener
import com.itc.healthtrackandroid.models.Metric

// historial de metricas del paciente con listener en tiempo real y adaptador reutilizado
class HistoryActivity : AppCompatActivity() {

    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var auth: FirebaseAuth
    private lateinit var metricDao: GenericDAO<Metric>

    // adaptador creado una sola vez en onCreate que no se reemplaza con cada actualizacion de firestore
    private lateinit var metricsAdapter: ColoredMetricAdapter

    // referencia al listener en tiempo real que se cancela en onDestroy para evitar fugas de memoria
    private var metricsListener: ListenerRegistration? = null

    // bandera para mostrar el aviso de sin registros solo la primera vez
    private var emptyToastShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        // inicializamos firebase y el dao de metricas
        auth = FirebaseAuth.getInstance()
        metricDao = GenericDAO(Metric::class.java, "metrics")

        historyRecyclerView = findViewById(R.id.historyRecyclerView)
        historyRecyclerView.layoutManager = LinearLayoutManager(this)

        // creamos el adaptador con lista vacia y lo asignamos una unica vez
        metricsAdapter = ColoredMetricAdapter(mutableListOf())
        historyRecyclerView.adapter = metricsAdapter

        // arrancamos el listener en tiempo real para ver los registros del paciente
        startListeningMetrics()
    }

    // registra un listener en tiempo real en firestore que solo llama a updateData en el adaptador existente
    private fun startListeningMetrics() {
        // si no hay sesion activa mandamos al login
        val currentUserId = auth.currentUser?.uid

        if (currentUserId == null) {
            Toast.makeText(this, "Sesión expirada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // registramos el listener en tiempo real filtrando por el id del paciente
        metricsListener = metricDao.listenByField(
            "patientId",
            currentUserId,
            object : OnDataLoadedListener<Metric> {
                override fun onSuccess(data: List<Metric>) {
                    // evitamos refrescar la lista si la pantalla ya se cerro
                    if (isFinishing || isDestroyed) return
                    if (data.isEmpty()) {
                        // si no hay registros limpiamos el adaptador y avisamos al paciente solo una vez
                        metricsAdapter.updateData(emptyList())
                        if (!emptyToastShown) {
                            Toast.makeText(this@HistoryActivity, "Sin registros aún", Toast.LENGTH_SHORT).show()
                            emptyToastShown = true
                        }
                    } else {
                        // ordenamos la lista de mas nueva a mas vieja y actualizamos el adaptador
                        val sortedList = data.sortedByDescending { it.timestamp }
                        metricsAdapter.updateData(sortedList)
                    }
                }

                override fun onFailure(error: Exception) {
                    if (isFinishing || isDestroyed) return
                    Toast.makeText(
                        this@HistoryActivity,
                        "Error al cargar el historial",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        // cerramos el listener para no desperdiciar recursos cuando la pantalla se cierra
        metricsListener?.remove()
        metricsListener = null
    }
}
