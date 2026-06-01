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

// historial de metricas del paciente con listener en tiempo real y adaptador de colores clinicos
class HistoryActivity : AppCompatActivity() {

    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var auth: FirebaseAuth
    private lateinit var metricDao: GenericDAO<Metric>

    // el adaptador se crea una sola vez en onCreate y se actualiza con los datos que llegan de Firestore
    private lateinit var metricsAdapter: ColoredMetricAdapter

    // guardamos la referencia al listener para cancelarlo en onDestroy y evitar fugas de memoria
    private var metricsListener: ListenerRegistration? = null

    // bandera para mostrar el aviso de sin registros solo la primera vez y no repetirlo en cada actualizacion
    private var emptyToastShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        auth      = FirebaseAuth.getInstance()
        metricDao = GenericDAO(Metric::class.java, "metrics")

        historyRecyclerView = findViewById(R.id.historyRecyclerView)
        historyRecyclerView.layoutManager = LinearLayoutManager(this)

        metricsAdapter = ColoredMetricAdapter(mutableListOf())
        historyRecyclerView.adapter = metricsAdapter

        startListeningMetrics()
    }

    // registra el listener en tiempo real filtrando por el id del paciente actual
    private fun startListeningMetrics() {
        val currentUserId = auth.currentUser?.uid

        if (currentUserId == null) {
            Toast.makeText(this, "Sesión expirada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        metricsListener = metricDao.listenByField(
            "patientId",
            currentUserId,
            object : OnDataLoadedListener<Metric> {
                override fun onSuccess(data: List<Metric>) {
                    if (isFinishing || isDestroyed) return
                    if (data.isEmpty()) {
                        metricsAdapter.updateData(emptyList())
                        // avisamos al paciente solo la primera vez para no interrumpirlo con el mismo Toast repetido
                        if (!emptyToastShown) {
                            Toast.makeText(this@HistoryActivity, "Sin registros aún", Toast.LENGTH_SHORT).show()
                            emptyToastShown = true
                        }
                    } else {
                        // ordenamos de mas reciente a mas antiguo antes de actualizar el adaptador
                        val sortedList = data.sortedByDescending { it.timestamp }
                        metricsAdapter.updateData(sortedList)
                    }
                }

                override fun onFailure(error: Exception) {
                    if (isFinishing || isDestroyed) return
                    Toast.makeText(this@HistoryActivity, "Error al cargar el historial", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        // cerramos el listener para liberar recursos cuando la pantalla se cierra
        metricsListener?.remove()
        metricsListener = null
    }
}
