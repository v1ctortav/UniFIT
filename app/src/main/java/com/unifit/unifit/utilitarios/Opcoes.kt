package com.unifit.unifit.utilitarios

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.unifit.unifit.R
import com.unifit.unifit.server.DiaSemana

fun Spinner.configurarOpcoes(itens: List<String>, selecionado: Int = -1, dica: String? = null) {
    val lista = if (dica != null) listOf(dica) + itens else itens
    adapter = OpcoesAdapter(context, lista, temDica = dica != null, spinner = this)
    setTag(R.id.spinner_tem_dica, dica != null)
    setPopupBackgroundResource(R.drawable.bg_spinner_popup)
    post { dropDownVerticalOffset = height + (4 * resources.displayMetrics.density).toInt() }

    val posicao = when {
        selecionado in itens.indices -> selecionado + (if (dica != null) 1 else 0)
        else -> 0
    }
    setSelection(posicao)
}

fun Spinner.indiceEscolhido(): Int {
    val temDica = getTag(R.id.spinner_tem_dica) as? Boolean ?: false
    val pos = selectedItemPosition
    return if (temDica) pos - 1 else pos
}

fun Spinner.configurarDiasSemana(diaAtual: Int?, semDiaFixo: Boolean) {
    if (semDiaFixo) {
        configurarOpcoes(listOf("Sem dia fixo") + DiaSemana.NOMES, selecionado = (diaAtual ?: -1) + 1)
    } else {
        configurarOpcoes(DiaSemana.NOMES, selecionado = diaAtual ?: -1, dica = "Selecione o dia")
    }
}

fun Spinner.diaEscolhido(semDiaFixo: Boolean): Int? {
    val i = indiceEscolhido()
    return if (semDiaFixo) (if (i <= 0) null else i - 1) else (if (i < 0) null else i)
}

fun Spinner.configurarDiasAgenda(diasAtuais: List<Int>?) {
    val opcoes = DiaSemana.OPCOES_AGENDA.toMutableList()
    val atuais = diasAtuais?.distinct()?.sorted()
    if (!atuais.isNullOrEmpty() && atuais !in opcoes) opcoes.add(0, atuais)
    setTag(R.id.spinner_opcoes_dias, opcoes)
    configurarOpcoes(opcoes.map { DiaSemana.juntar(it) }, selecionado = opcoes.indexOf(atuais), dica = "Selecione o dia")
}

@Suppress("UNCHECKED_CAST")
fun Spinner.diasEscolhidos(): List<Int>? {
    val opcoes = getTag(R.id.spinner_opcoes_dias) as? List<List<Int>> ?: return null
    return opcoes.getOrNull(indiceEscolhido())
}

private class OpcoesAdapter(
    context: Context,
    private val itens: List<String>,
    private val temDica: Boolean,
    private val spinner: Spinner
) : ArrayAdapter<String>(context, R.layout.item_spinner_selecionado, itens) {

    private val inflater = LayoutInflater.from(context)
    private val cinza = 0xFF7A7A7A.toInt()
    private val preto = ContextCompat.getColor(context, R.color.black)
    private val azul = ContextCompat.getColor(context, R.color.unifor)
    private val fonteNormal = ResourcesCompat.getFont(context, R.font.satoshi_regular)
    private val fonteForte = ResourcesCompat.getFont(context, R.font.satoshi_bold)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val v = (convertView ?: inflater.inflate(R.layout.item_spinner_selecionado, parent, false)) as TextView
        v.text = itens[position]
        v.setTextColor(if (temDica && position == 0) cinza else preto)
        return v
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val v = (convertView ?: inflater.inflate(R.layout.item_spinner_opcao, parent, false)) as TextView
        v.text = itens[position]
        val escolhido = position == spinner.selectedItemPosition && !(temDica && position == 0)
        v.setTextColor(
            when {
                temDica && position == 0 -> cinza
                escolhido -> azul
                else -> preto
            }
        )
        v.typeface = if (escolhido) fonteForte ?: Typeface.DEFAULT_BOLD else fonteNormal ?: Typeface.DEFAULT
        v.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, if (escolhido) R.drawable.ic_check else 0, 0)
        v.compoundDrawableTintList = if (escolhido) android.content.res.ColorStateList.valueOf(azul) else null
        return v
    }

    override fun isEnabled(position: Int): Boolean = !(temDica && position == 0)
}
