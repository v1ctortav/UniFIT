package com.unifit.unifit.utilitarios

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import com.unifit.unifit.R

class EstadoVazioAdapter(private var mensagem: String) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private enum class Estado { CARREGANDO, VAZIO, NENHUM }

    private var estado = Estado.CARREGANDO

    fun mostrar(vazio: Boolean, novaMensagem: String? = null) {
        novaMensagem?.let { mensagem = it }
        mudarPara(if (vazio) Estado.VAZIO else Estado.NENHUM)
    }

    fun carregando() = mudarPara(Estado.CARREGANDO)

    private fun mudarPara(novo: Estado) {
        val antes = estado
        estado = novo
        when {
            antes == Estado.NENHUM && novo != Estado.NENHUM -> notifyItemInserted(0)
            antes != Estado.NENHUM && novo == Estado.NENHUM -> notifyItemRemoved(0)
            novo != Estado.NENHUM -> notifyItemChanged(0)
        }
    }

    class Vazio(view: View) : RecyclerView.ViewHolder(view) {
        val texto: TextView = view.findViewById(R.id.texto_estado_vazio)
    }

    class Carregando(view: View) : RecyclerView.ViewHolder(view)

    override fun getItemViewType(position: Int) = if (estado == Estado.CARREGANDO) 1 else 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        if (viewType == 1) {
            val v = inflater.inflate(R.layout.item_carregando, parent, false)
            v.layoutParams = v.layoutParams.apply { width = ViewGroup.LayoutParams.MATCH_PARENT }
            return Carregando(v)
        }
        val v = inflater.inflate(R.layout.item_estado_vazio, parent, false)
        v.layoutParams = (v.layoutParams as ViewGroup.MarginLayoutParams).apply { width = ViewGroup.LayoutParams.MATCH_PARENT }
        return Vazio(v)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is Vazio) holder.texto.text = mensagem
    }

    override fun getItemCount() = if (estado == Estado.NENHUM) 0 else 1
}

fun RecyclerView.usarAdapterComVazio(adapterReal: RecyclerView.Adapter<*>, mensagem: String): EstadoVazioAdapter {
    val vazio = EstadoVazioAdapter(mensagem)
    adapter = ConcatAdapter(adapterReal, vazio)
    return vazio
}

fun LinearLayout.adicionarEstadoVazio(mensagem: String, claro: Boolean = false) {
    val v = LayoutInflater.from(context).inflate(R.layout.item_estado_vazio, this, false)
    val texto = v.findViewById<TextView>(R.id.texto_estado_vazio)
    texto.text = mensagem
    if (claro) {
        v.background = null
        v.backgroundTintList = null
        texto.setTextColor(Color.WHITE)
        (v as ViewGroup).getChildAt(0).let { (it as? ImageView)?.imageTintList = ColorStateList.valueOf(Color.WHITE) }
        v.minimumHeight = 0
        (v.layoutParams as ViewGroup.MarginLayoutParams).setMargins(0, 0, 0, 0)
    }
    addView(v)
}

fun LinearLayout.adicionarCarregando(claro: Boolean = false) {
    val v = LayoutInflater.from(context).inflate(R.layout.item_carregando, this, false)
    if (claro) {
        v.findViewById<View>(R.id.progresso_carregando).let {
            (it as android.widget.ProgressBar).indeterminateTintList = ColorStateList.valueOf(Color.WHITE)
        }
        v.minimumHeight = (80 * resources.displayMetrics.density).toInt()
    }
    addView(v)
}

class EstadoVazioCard(private val conteudo: View, mensagem: String) {

    private val vazio: View
    private val carregando: View

    init {
        val pai = conteudo.parent as ViewGroup
        val container: ViewGroup = if (pai is FrameLayout && pai.childCount == 1) {
            pai
        } else {
            val indice = pai.indexOfChild(conteudo)
            val params = conteudo.layoutParams
            val idOriginal = conteudo.id
            pai.removeView(conteudo)
            FrameLayout(conteudo.context).also { frame ->
                frame.id = idOriginal
                conteudo.id = View.generateViewId()
                frame.addView(conteudo, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))
                pai.addView(frame, indice, params)
            }
        }
        val inflater = LayoutInflater.from(conteudo.context)
        vazio = inflater.inflate(R.layout.item_estado_vazio, container, false)
        (vazio.layoutParams as ViewGroup.MarginLayoutParams).apply {
            setMargins(0, 0, 0, 0)
            marginStart = 0
            marginEnd = 0
        }
        vazio.findViewById<TextView>(R.id.texto_estado_vazio).text = mensagem
        vazio.visibility = View.GONE
        container.addView(vazio)

        carregando = inflater.inflate(R.layout.item_carregando, container, false)
        container.addView(carregando)
        conteudo.visibility = View.GONE
    }

    fun mostrar(semConteudo: Boolean, mensagem: String? = null) {
        mensagem?.let { vazio.findViewById<TextView>(R.id.texto_estado_vazio).text = it }
        carregando.visibility = View.GONE
        vazio.visibility = if (semConteudo) View.VISIBLE else View.GONE
        conteudo.visibility = if (semConteudo) View.GONE else View.VISIBLE
    }
}
