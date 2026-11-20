package com.unifit.unifit.cliente.progresso

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import com.unifit.unifit.R
import com.unifit.unifit.server.Medidas
import com.unifit.unifit.server.Supabase
import com.unifit.unifit.server.Tempo
import com.unifit.unifit.utilitarios.br
import com.unifit.unifit.utilitarios.mostrarExpandido
import com.unifit.unifit.utilitarios.lancar
import com.unifit.unifit.utilitarios.paraDouble
import com.unifit.unifit.utilitarios.toast

class ProgressoFragment : Fragment() {

    private lateinit var grafico: LineChart
    private lateinit var pesoText: TextView
    private lateinit var alturaText: TextView
    private lateinit var bfText: TextView
    private lateinit var atualizadoTexts: List<TextView>

    private var medidas: Medidas? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_progresso, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        grafico = view.findViewById(R.id.graficoPeso)
        pesoText = view.findViewById(R.id.peso_text)
        alturaText = view.findViewById(R.id.altura_text)
        bfText = view.findViewById(R.id.bf_text)
        atualizadoTexts = listOf(
            view.findViewById(R.id.textView25),
            view.findViewById(R.id.textView36),
            view.findViewById(R.id.textView34)
        )

        configurarGrafico()

        view.findViewById<Button>(R.id.btn_edit_infos_progresso).setOnClickListener { abrirPopupEditar() }

        lancar { mostrar(Supabase.Progresso.minhasMedidas()) }
    }

    private fun mostrar(m: Medidas) {
        medidas = m
        pesoText.text = "${m.peso.br(1)} kg"
        alturaText.text = "${m.altura.br(2)} m"
        bfText.text = "${m.gorduraCorporal.br(1)}%"

        val atualizado = m.atualizadoEm?.let { "Atualizado ${Tempo.decorrido(it)}" } ?: "Sem atualizações"
        atualizadoTexts.forEach { it.text = atualizado }

        val entradas = m.historicoPesos.mapIndexedNotNull { mes, peso ->
            if (peso > 0) Entry(mes.toFloat(), peso.toFloat()) else null
        }
        val dataSet = LineDataSet(entradas, "Peso (kg)").apply {
            color = Color.BLUE
            setCircleColor(Color.BLUE)
            lineWidth = 2f
            circleRadius = 5f
            setDrawValues(false)
            mode = LineDataSet.Mode.LINEAR
        }
        grafico.data = LineData(dataSet)
        grafico.invalidate()
    }

    private fun configurarGrafico() {
        grafico.description.isEnabled = false
        grafico.legend.isEnabled = false
        grafico.axisRight.isEnabled = false
        grafico.setNoDataText("Registre seu peso para ver o gráfico")
        grafico.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            axisMinimum = 0f
            axisMaximum = 11f
            valueFormatter = IndexAxisValueFormatter(Medidas.MESES)
            setDrawGridLines(false)
        }
    }

    private fun abrirPopupEditar() {
        val dialog = BottomSheetDialog(requireContext())
        val sheet = layoutInflater.inflate(R.layout.bottomsheet_editar_progresso, null)
        dialog.setContentView(sheet)

        val pesoInput = sheet.findViewById<TextInputEditText>(R.id.peso_input_text_progresso)
        val alturaInput = sheet.findViewById<TextInputEditText>(R.id.altura_input_text_progresso)
        val bfInput = sheet.findViewById<TextInputEditText>(R.id.bf_input_text_progresso)

        medidas?.let { m ->
            if (m.peso > 0) pesoInput.setText(m.peso.br(1))
            if (m.altura > 0) alturaInput.setText(m.altura.br(2))
            if (m.gorduraCorporal > 0) bfInput.setText(m.gorduraCorporal.br(1))
        }

        val btnSalvar = sheet.findViewById<Button>(R.id.btn_save_edit_progresso)
        btnSalvar.setOnClickListener {
            val peso = pesoInput.text.toString().paraDouble()
            val altura = alturaInput.text.toString().paraDouble()
            val bf = bfInput.text.toString().paraDouble()
            if (peso == null || altura == null || bf == null) {
                toast("Preencha peso, altura e gordura corporal")
                return@setOnClickListener
            }

            btnSalvar.isEnabled = false
            lancar(aoFalhar = { msg ->
                toast(msg)
                btnSalvar.isEnabled = true
            }) {
                mostrar(Supabase.Progresso.atualizarMedidas(peso, altura, bf))
                toast("Medidas atualizadas")
                dialog.dismiss()
            }
        }

        dialog.mostrarExpandido()
    }
}
