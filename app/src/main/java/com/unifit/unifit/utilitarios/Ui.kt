package com.unifit.unifit.utilitarios

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputLayout
import com.unifit.unifit.MainActivity
import com.unifit.unifit.MainAdminActivity
import com.unifit.unifit.R
import com.unifit.unifit.auth.AuthActivity
import com.unifit.unifit.server.Imagem
import com.unifit.unifit.server.Usuario
import com.unifit.unifit.server.mensagemDeErro
import com.unifit.unifit.server.normalizar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale

fun Fragment.lancar(
    aoFalhar: ((String) -> Unit)? = null,
    bloco: suspend CoroutineScope.() -> Unit
): Job = viewLifecycleOwner.lifecycleScope.launch {
    try {
        bloco()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        val msg = mensagemDeErro(e)
        if (aoFalhar != null) aoFalhar(msg) else context?.let { toast(it, msg) }
    }
}

fun toast(context: Context, msg: String) =
    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

fun Fragment.toast(msg: String) {
    context?.let { toast(it, msg) }
}

var TextInputLayout.texto: String
    get() = editText?.text?.toString().orEmpty()
    set(valor) {
        editText?.setText(valor)
    }

fun EditText.aoDigitar(acao: (String) -> Unit) {
    addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) = acao(s?.toString().orEmpty())
    })
}

fun String.paraDouble(): Double? = trim().replace(",", ".").toDoubleOrNull()

fun Double.br(casas: Int = 1): String =
    String.format(Locale.forLanguageTag("pt-BR"), "%.${casas}f", this)

fun primeiroNome(nome: String): String = nome.trim().substringBefore(" ").ifEmpty { nome }

fun List<String>.emTopicos(vazio: String = "• Nenhum equipamento"): String =
    if (isEmpty()) vazio else joinToString("\n") { "• $it" }

fun View.textInputs(): List<TextInputLayout> {
    if (this is TextInputLayout) return listOf(this)
    if (this !is ViewGroup) return emptyList()
    return (0 until childCount).flatMap { getChildAt(it).textInputs() }
}

fun ImageView.mostrarBase64(base64: String?) {
    val padrao = (getTag(R.id.imagem_padrao) as? Drawable.ConstantState)
        ?: drawable?.constantState?.also { setTag(R.id.imagem_padrao, it) }
    val bmp = Imagem.base64ParaBitmap(base64)
    when {
        bmp != null -> setImageBitmap(bmp)
        padrao != null -> setImageDrawable(padrao.newDrawable(resources))
    }
}

object Categorias {
    val LISTA = listOf("Peito", "Costas", "Ombro", "Pernas", "Braços", "Abdômen", "Cardio", "Força")

    fun configurarBotoes(botoesXml: List<Button>, aoTrocar: (String?) -> Unit) {
        val botoes = botoesXml + botoesQueFaltam(botoesXml)
        botoes.forEachIndexed { i, btn ->
            btn.isSelected = i == 0
            btn.setOnClickListener {
                botoes.forEach { it.isSelected = it == btn }
                aoTrocar(if (i == 0) null else btn.text.toString())
            }
        }
    }

    private fun botoesQueFaltam(existentes: List<Button>): List<Button> {
        val ultimo = existentes.lastOrNull() ?: return emptyList()
        val pai = ultimo.parent as? ViewGroup ?: return emptyList()
        val jaTem = existentes.map { it.text.toString().normalizar() }.toSet()
        val inflater = LayoutInflater.from(ultimo.context)
        (pai as? android.widget.LinearLayout)?.isBaselineAligned = false
        return LISTA.filter { it.normalizar() !in jaTem }.map { categoria ->
            val btn = inflater.inflate(R.layout.item_botao_categoria, pai, false) as Button
            btn.text = categoria
            btn.layoutParams = when (val lp = ultimo.layoutParams) {
                is android.widget.LinearLayout.LayoutParams -> android.widget.LinearLayout.LayoutParams(lp).apply {
                    width = ViewGroup.LayoutParams.WRAP_CONTENT
                    weight = 0f
                }
                is ViewGroup.MarginLayoutParams -> ViewGroup.MarginLayoutParams(lp)
                else -> ViewGroup.LayoutParams(lp)
            }
            pai.addView(btn)
            btn
        }
    }

    fun combina(categoriaItem: String, filtro: String?): Boolean =
        filtro == null || categoriaItem.normalizar() == filtro.normalizar()

    fun configurarSpinner(spinner: Spinner, atual: String?) {
        val itens = LISTA.toMutableList()
        if (!atual.isNullOrBlank() && itens.none { it.normalizar() == atual.normalizar() }) itens.add(0, atual)
        val pos = itens.indexOfFirst { it.normalizar() == atual?.normalizar() }
        spinner.configurarOpcoes(itens, selecionado = pos, dica = "Selecione")
    }

    fun escolhida(spinner: Spinner): String =
        if (spinner.indiceEscolhido() < 0) "" else spinner.selectedItem?.toString().orEmpty()
}

fun String.contemBusca(termo: String): Boolean =
    termo.isBlank() || normalizar().contains(termo.normalizar())

fun configurarPesquisa(botao: View, barra: TextInputLayout, aoBuscar: (String) -> Unit) {
    botao.setOnClickListener {
        val abrir = barra.visibility != View.VISIBLE
        barra.visibility = if (abrir) View.VISIBLE else View.GONE
        if (!abrir) {
            barra.texto = ""
            aoBuscar("")
        }
    }
    barra.editText?.aoDigitar(aoBuscar)
}

fun abrirPopupConfirmacao(
    context: Context,
    titulo: String,
    descricao: String,
    textoConfirmar: String,
    icone: Int = R.drawable.ic_alert,
    textoCancelar: String? = null,
    estilizar: ((confirmar: Button, cancelar: Button) -> Unit)? = null,
    aoConfirmar: () -> Unit
) {
    val dialog = Dialog(context)
    val view = LayoutInflater.from(context).inflate(R.layout.popup_confirmacao, null)
    dialog.setContentView(view)

    view.findViewById<TextView>(R.id.text_name_popup_confirmacao).text = titulo
    view.findViewById<TextView>(R.id.text_desc_popup_confirmacao).text = descricao
    view.findViewById<ImageView>(R.id.icon_popup_confirmacao).setImageResource(icone)
    val btnCancelar = view.findViewById<Button>(R.id.btn_cancel_action_popup_confirmacao)
    val btnConfirmar = view.findViewById<Button>(R.id.btn_confirm_action_popup_confirmacao)
    btnConfirmar.text = textoConfirmar
    textoCancelar?.let { btnCancelar.text = it }
    estilizar?.invoke(btnConfirmar, btnCancelar)

    btnCancelar.setOnClickListener { dialog.dismiss() }
    btnConfirmar.setOnClickListener {
        dialog.dismiss()
        aoConfirmar()
    }

    dialog.show()
    dialog.window?.apply {
        setBackgroundDrawableResource(android.R.color.transparent)
        setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
        setDimAmount(0.6f)
        setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }
}

fun Activity.abrirAreaDoUsuario(usuario: Usuario) {
    val destino = if (usuario.isAdmin) MainAdminActivity::class.java else MainActivity::class.java
    startActivity(Intent(this, destino).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
    finish()
}

fun Activity.voltarParaLogin() {
    startActivity(Intent(this, AuthActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
    finish()
}

object Resultados {
    const val PERFIL_ALTERADO = "perfil_alterado"
    const val EXERCICIOS_ALTERADOS = "exercicios_alterados"
    const val TREINOS_ALTERADOS = "treinos_alterados"
}

fun TextView.mostrarContador(total: Int?) {
    if (total == null || total <= 0) {
        visibility = View.INVISIBLE
        return
    }
    text = if (total > 9) "9+" else total.toString()
    visibility = View.VISIBLE
}

private val limparErroDos = java.util.WeakHashMap<EditText, () -> Unit>()

fun destacarErro(campos: List<EditText>, linhas: List<View> = emptyList(), aoLimpar: (() -> Unit)? = null) {
    if (campos.isEmpty()) return
    campos.forEach { limparErroDos[it]?.invoke() }

    val vermelho = android.content.res.ColorStateList.valueOf(
        androidx.core.content.ContextCompat.getColor(campos.first().context, R.color.red)
    )
    val coresTexto = campos.map { it.textColors }
    val coresLinha = linhas.map { it.backgroundTintList }
    val observadores = mutableListOf<Pair<EditText, TextWatcher>>()

    lateinit var limpar: () -> Unit
    limpar = {
        campos.forEachIndexed { i, c -> c.setTextColor(coresTexto[i]); limparErroDos.remove(c) }
        linhas.forEachIndexed { i, l -> l.backgroundTintList = coresLinha[i] }
        observadores.forEach { (c, w) -> c.post { c.removeTextChangedListener(w) } }
        observadores.clear()
        aoLimpar?.invoke()
    }

    campos.forEach { it.setTextColor(vermelho) }
    linhas.forEach { it.backgroundTintList = vermelho }
    campos.forEach { c ->
        val w = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { if (limparErroDos[c] === limpar) limpar() }
        }
        c.addTextChangedListener(w)
        observadores += c to w
        limparErroDos[c] = limpar
    }
}

fun TextInputLayout.erroAbaixo(mensagem: String?) {
    val campo = this
    val pai = parent as? androidx.constraintlayout.widget.ConstraintLayout ?: run { error = mensagem; return }
    var aviso = pai.findViewWithTag<TextView>("erro_abaixo_${campo.id}")
    if (aviso == null) {
        if (mensagem == null) return
        val dp = resources.displayMetrics.density
        aviso = TextView(context).apply {
            id = View.generateViewId()
            tag = "erro_abaixo_${campo.id}"
            setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.red))
            textSize = 14f
            typeface = androidx.core.content.res.ResourcesCompat.getFont(context, R.font.satoshi_medium)
            setPadding((8 * dp).toInt(), 0, 0, 0)
        }
        for (i in 0 until pai.childCount) {
            val v = pai.getChildAt(i)
            val p = v.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams ?: continue
            if (p.topToBottom == campo.id) {
                p.topToBottom = aviso.id
                v.layoutParams = p
            }
        }
        pai.addView(aviso, androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topToBottom = campo.id
            startToStart = campo.id
            endToEnd = campo.id
            topMargin = (4 * dp).toInt()
        })
        editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { pai.findViewWithTag<View>("erro_abaixo_${campo.id}")?.visibility = View.GONE }
        })
    }
    aviso.text = mensagem
    aviso.visibility = if (mensagem == null) View.GONE else View.VISIBLE
}
