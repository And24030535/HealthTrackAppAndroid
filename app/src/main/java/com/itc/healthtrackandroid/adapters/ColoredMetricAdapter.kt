package com.itc.healthtrackandroid.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.itc.healthtrackandroid.R
import com.itc.healthtrackandroid.models.Metric
import java.text.SimpleDateFormat
import java.util.Locale

// adaptador con franja de color clinico por fila y rojo si hay alerta de presion o glucosa
// ambar si el imc es alto y verde si todos los valores son normales
class ColoredMetricAdapter(
    items: MutableList<Metric>
) : BaseAdapter<Metric, ColoredMetricAdapter.MetricViewHolder>(items) {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    inner class MetricViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val colorStripe: View           = itemView.findViewById(R.id.metricColorStripe)
        val dateTextView: TextView      = itemView.findViewById(R.id.metricDateTextView)
        val bpTextView: TextView        = itemView.findViewById(R.id.metricBpTextView)
        val heartRateTextView: TextView = itemView.findViewById(R.id.metricHeartRateTextView)
        val glucoseTextView: TextView   = itemView.findViewById(R.id.metricGlucoseTextView)
        val bmiTextView: TextView       = itemView.findViewById(R.id.metricBmiTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MetricViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_metric_colored, parent, false)
        return MetricViewHolder(view)
    }

    override fun onBindViewHolder(holder: MetricViewHolder, position: Int) {
        val metric = items[position]

        // copiamos las propiedades var a val para que Kotlin permita el smart cast en los when
        val timestamp    = metric.timestamp
        val glucoseLevel = metric.glucoseLevel
        val bmi          = metric.bmi
        val sys          = metric.systolic
        val dia          = metric.diastolic

        holder.dateTextView.text = if (timestamp != null) {
            dateFormat.format(timestamp.toDate())
        } else "—"

        holder.bpTextView.text       = if (sys != null && dia != null) "PA: $sys/$dia mmHg" else "PA: —"
        holder.heartRateTextView.text = if (metric.heartRate != null) "FC: ${metric.heartRate} lpm" else "FC: —"
        holder.glucoseTextView.text  = if (glucoseLevel != null) "Glucosa: $glucoseLevel mg/dL" else "Glucosa: —"
        holder.bmiTextView.text      = if (bmi != null) "IMC: $bmi" else "IMC: —"

        // franja lateral con color clinico mientras que el fondo de la fila se mantiene oscuro fijo
        val stripeColor = when {
            (sys != null && sys > 140) ||
            (dia != null && dia > 90)  ||
            (glucoseLevel != null && glucoseLevel > 200.0) ->
                Color.parseColor("#C0392B") // rojo para riesgo alto

            bmi != null && bmi > 30.0 ->
                Color.parseColor("#E6A817") // ambar para imc elevado

            else ->
                Color.parseColor("#2E7D32") // verde para valores normales
        }
        holder.colorStripe.setBackgroundColor(stripeColor)
        holder.itemView.setBackgroundColor(Color.parseColor("#152231"))
    }
}
