package com.unifit.unifit.utilitarios

import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.unifit.unifit.R
import com.unifit.unifit.server.Exercicio
import com.unifit.unifit.server.ItemTreino

class FormularioExercicios(
    private val lista: LinearLayout,
    private val exercicios: List<Exercicio>
) {
    private val inflater = LayoutInflater.from(lista.context)

    init {
        lista.removeAllViews()
    }

    fun adicionar(item: ItemTreino? = null) {
        val linha = inflater.inflate(R.layout.item_exercicio_treino_personalizado_lista, lista, false)
        val spinner = linha.findViewById<Spinner>(R.id.spinner)
        val posicao = exercicios.indexOfFirst { it.id == item?.exercicioId }
        spinner.configurarOpcoes(exercicios.map { it.nome }, selecionado = posicao, dica = "Selecione o exercício")

        val (series, repeticoes) = linha.textInputs().let { it[0] to it[1] }
        item?.let {
            series.texto = it.series.toString()
            repeticoes.texto = it.repeticoes.toString()
        }
        linha.findViewById<ImageButton>(R.id.btn_excluir_item_exercicio).setOnClickListener { lista.removeView(linha) }
        lista.addView(linha)
    }

    val vazio: Boolean get() = lista.childCount == 0

    fun ler(aoErro: (String) -> Unit): List<ItemTreino>? {
        val itens = mutableListOf<ItemTreino>()
        for (i in 0 until lista.childCount) {
            val linha = lista.getChildAt(i)
            if (!linha.isVisible) continue
            val spinner = linha.findViewById<Spinner>(R.id.spinner)
            val (campoSeries, campoRepeticoes) = linha.textInputs().let { it[0] to it[1] }
            campoSeries.error = null
            campoRepeticoes.error = null

            val ex = exercicios.getOrNull(spinner.indiceEscolhido())
            if (ex?.id == null) {
                aoErro("Selecione o exercício da linha ${i + 1}")
                return null
            }
            val series = campoSeries.texto.trim().toIntOrNull()
            val repeticoes = campoRepeticoes.texto.trim().toIntOrNull()
            if (series == null || series <= 0) {
                campoSeries.error = " "
                aoErro("Informe as séries de ${ex.nome}")
                return null
            }
            if (repeticoes == null || repeticoes <= 0) {
                campoRepeticoes.error = " "
                aoErro("Informe as repetições de ${ex.nome}")
                return null
            }
            itens.add(ItemTreino(exercicioId = ex.id, repeticoes = repeticoes, series = series))
        }
        return itens
    }
}

fun BottomSheetDialog.mostrarExpandido() {
    behavior.isDraggable = false
    behavior.skipCollapsed = true
    behavior.state = BottomSheetBehavior.STATE_EXPANDED
    setCanceledOnTouchOutside(true)
    show()
}
