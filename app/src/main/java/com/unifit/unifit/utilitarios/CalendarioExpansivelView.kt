package com.unifit.unifit.utilitarios

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import com.unifit.unifit.R
import java.util.Calendar
import java.util.Locale

class CalendarioExpansivelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var onDateSelected: ((dia: Int, mes: Int) -> Unit)? = null

    var onDataSelecionada: ((data: java.time.LocalDate) -> Unit)? = null

    fun dataSelecionada(): java.time.LocalDate = java.time.LocalDate.of(
        selecionada.get(Calendar.YEAR),
        selecionada.get(Calendar.MONTH) + 1,
        selecionada.get(Calendar.DAY_OF_MONTH)
    )

    private val azul = Color.rgb(7, 89, 245)
    private val cinzaCard = Color.rgb(49, 49, 49)
    private val branco = Color.WHITE
    private val diasSemana = arrayOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb")
    private val iniciaisSemana = arrayOf("D", "S", "T", "Q", "Q", "S", "S")
    private val nomesMeses = arrayOf(
        "jan.", "fev.", "mar.", "abr.", "mai.", "jun.",
        "jul.", "ago.", "set.", "out.", "nov.", "dez."
    )

    private val hoje: Calendar = Calendar.getInstance()
    private val selecionada: Calendar = Calendar.getInstance()
    private val mesExibido: Calendar = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
    }

    private var expandido = false
    private var selecionouManualmente = false

    private val scrollCompacto: HorizontalScrollView
    private val containerDiasCompactos: LinearLayout
    private val painelExpandido: LinearLayout
    private val tabelaDiasMes: TableLayout
    private val textoMesAno: TextView
    private val botaoAnterior: TextView
    private val botaoProximo: TextView
    private val botaoAlternar: TextView

    private val handler = Handler(Looper.getMainLooper())
    private val tarefaViradaDia = object : Runnable {
        override fun run() {
            atualizarParaHoje()
            programarViradaDia()
        }
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.view_calendario_expansivel, this, true)

        scrollCompacto = findViewById(R.id.scrollCompacto)
        containerDiasCompactos = findViewById(R.id.containerDiasCompactos)
        painelExpandido = findViewById(R.id.painelExpandido)
        tabelaDiasMes = findViewById(R.id.tabelaDiasMes)
        textoMesAno = findViewById(R.id.textoMesAno)
        botaoAnterior = findViewById(R.id.botaoMesAnterior)
        botaoProximo = findViewById(R.id.botaoProximoMes)
        botaoAlternar = findViewById(R.id.botaoAlternar)

        botaoAlternar.setOnClickListener {
            expandido = !expandido
            aplicarModo()
        }
        botaoAnterior.setOnClickListener {
            mesExibido.add(Calendar.MONTH, -1)
            renderizarMes()
        }
        botaoProximo.setOnClickListener {
            mesExibido.add(Calendar.MONTH, 1)
            renderizarMes()
        }

        renderizarCompacto()
        renderizarMes()
        aplicarModo()
    }

    fun atualizarParaHoje() {
        val dataSistema = Calendar.getInstance()
        if (mesmoDia(hoje, dataSistema)) return

        val mostrandoMesAtual = mesmoMes(hoje, mesExibido)
        hoje.timeInMillis = dataSistema.timeInMillis

        if (!selecionouManualmente) {
            selecionada.timeInMillis = dataSistema.timeInMillis
        }
        if (mostrandoMesAtual) {
            mesExibido.timeInMillis = dataSistema.timeInMillis
            mesExibido.set(Calendar.DAY_OF_MONTH, 1)
        }
        renderizarCompacto()
        renderizarMes()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        atualizarParaHoje()
        programarViradaDia()
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(tarefaViradaDia)
        super.onDetachedFromWindow()
    }

    private fun programarViradaDia() {
        handler.removeCallbacks(tarefaViradaDia)
        val agora = Calendar.getInstance()
        val proximaMeiaNoite = (agora.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val espera = (proximaMeiaNoite.timeInMillis - agora.timeInMillis)
            .coerceAtLeast(1000L)
        handler.postDelayed(tarefaViradaDia, espera)
    }

    private fun aplicarModo() {
        scrollCompacto.visibility = if (expandido) View.GONE else View.VISIBLE
        painelExpandido.visibility = if (expandido) View.VISIBLE else View.GONE
        botaoAlternar.text = if (expandido) "↑" else "↓"
        botaoAlternar.contentDescription =
            if (expandido) "Recolher calendário" else "Expandir calendário"

        (botaoAlternar.layoutParams as FrameLayout.LayoutParams).apply {
            gravity = if (expandido) Gravity.TOP or Gravity.END else Gravity.CENTER_VERTICAL or Gravity.END
            topMargin = if (expandido) dp(4) else 0
            botaoAlternar.layoutParams = this
        }
        botaoAlternar.bringToFront()
    }

    private fun renderizarCompacto() {
        containerDiasCompactos.removeAllViews()
        val data = hoje.clone() as Calendar

        repeat(14) {
            val dataDoCard = data.clone() as Calendar
            val escolhido = mesmoDia(dataDoCard, selecionada)
            val ehHoje = mesmoDia(dataDoCard, hoje)

            val card = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = fundoArredondado(
                    if (escolhido) azul else cinzaCard,
                    raioDp = 6,
                    contornoAzul = ehHoje && !escolhido
                )
                isClickable = true
                isFocusable = true
                contentDescription = "${dataDoCard.get(Calendar.DAY_OF_MONTH)} de " +
                        "${nomesMeses[dataDoCard.get(Calendar.MONTH)]}"
                setOnClickListener { selecionarData(dataDoCard) }
            }

            card.addView(textoCentralizado(
                String.format(Locale.ROOT, "%02d", dataDoCard.get(Calendar.DAY_OF_MONTH)), 18f
            ))
            card.addView(textoCentralizado(
                diasSemana[dataDoCard.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY], 14f
            ))

            containerDiasCompactos.addView(
                card,
                LinearLayout.LayoutParams(dp(58), dp(72)).apply {
                    rightMargin = dp(10)
                }
            )
            data.add(Calendar.DAY_OF_MONTH, 1)
        }
    }

    private fun renderizarMes() {
        val mes = mesExibido.get(Calendar.MONTH)
        val ano = mesExibido.get(Calendar.YEAR)
        val nome = nomesMeses[mes].replaceFirstChar { it.uppercaseChar() }
        textoMesAno.text = "$nome de $ano"
        tabelaDiasMes.removeAllViews()

        val cabecalho = TableRow(context)
        iniciaisSemana.forEach { sigla ->
            cabecalho.addView(
                textoCentralizado(sigla, 14f),
                TableRow.LayoutParams(0, dp(32), 1f)
            )
        }
        tabelaDiasMes.addView(cabecalho)

        val primeiroDia = (mesExibido.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val inicioNaSemana = primeiroDia.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
        val totalDias = primeiroDia.getActualMaximum(Calendar.DAY_OF_MONTH)
        val quantidadeDeLinhas = (inicioNaSemana + totalDias + 6) / 7

        repeat(quantidadeDeLinhas) { linhaIndex ->
            val linha = TableRow(context)
            repeat(7) { coluna ->
                val dia = linhaIndex * 7 + coluna - inicioNaSemana + 1
                val celula = FrameLayout(context)

                if (dia in 1..totalDias) {
                    val dataDaCelula = (primeiroDia.clone() as Calendar).apply {
                        set(Calendar.DAY_OF_MONTH, dia)
                    }
                    val textoDia = textoCentralizado(
                        String.format(Locale.ROOT, "%02d", dia), 16f
                    ).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            dp(34), dp(34), Gravity.CENTER
                        )
                        val escolhido = mesmoDia(dataDaCelula, selecionada)
                        val ehHoje = mesmoDia(dataDaCelula, hoje)
                        if (escolhido || ehHoje) {
                            background = fundoArredondado(
                                if (escolhido) azul else Color.TRANSPARENT,
                                raioDp = 50,
                                contornoAzul = ehHoje && !escolhido
                            )
                        }
                    }
                    celula.addView(textoDia)
                    celula.isClickable = true
                    celula.isFocusable = true
                    celula.contentDescription = "Selecionar $dia de ${nomesMeses[mes]} de $ano"
                    celula.setOnClickListener { selecionarData(dataDaCelula) }
                }

                linha.addView(celula, TableRow.LayoutParams(0, dp(38), 1f))
            }
            tabelaDiasMes.addView(linha)
        }
    }

    private fun selecionarData(data: Calendar) {
        selecionada.timeInMillis = data.timeInMillis
        selecionouManualmente = true
        mesExibido.timeInMillis = data.timeInMillis
        mesExibido.set(Calendar.DAY_OF_MONTH, 1)

        renderizarCompacto()
        renderizarMes()

        onDateSelected?.invoke(
            data.get(Calendar.DAY_OF_MONTH),
            data.get(Calendar.MONTH) + 1
        )
        onDataSelecionada?.invoke(dataSelecionada())
    }

    private fun textoCentralizado(valor: String, tamanhoSp: Float) = TextView(context).apply {
        text = valor
        setTextColor(branco)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, tamanhoSp)
        gravity = Gravity.CENTER
        includeFontPadding = false
    }

    private fun fundoArredondado(
        cor: Int,
        raioDp: Int,
        contornoAzul: Boolean = false
    ) = GradientDrawable().apply {
        setColor(cor)
        cornerRadius = dp(raioDp).toFloat()
        if (contornoAzul) setStroke(dp(2), azul)
    }

    private fun mesmoMes(a: Calendar, b: Calendar): Boolean =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
                a.get(Calendar.MONTH) == b.get(Calendar.MONTH)

    private fun mesmoDia(a: Calendar, b: Calendar): Boolean =
        mesmoMes(a, b) && a.get(Calendar.DAY_OF_MONTH) == b.get(Calendar.DAY_OF_MONTH)

    private fun dp(valor: Int): Int =
        (valor * resources.displayMetrics.density + 0.5f).toInt()
}
