package com.unifit.unifit.utilitarios

import android.content.res.ColorStateList
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputLayout
import com.unifit.unifit.R
import com.unifit.unifit.server.Autor
import com.unifit.unifit.server.Mensagem
import com.unifit.unifit.server.Supabase
import com.unifit.unifit.server.Tempo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class TelaChat(
    private val fragment: Fragment,
    private val alunoId: String,
    private val souAdmin: Boolean,
    private val lista: RecyclerView,
    private val campo: TextInputLayout,
    private val btnEnviar: View
) {
    private val adapter = MensagensAdapter()
    private val meuId = Supabase.idUsuarioLogado().orEmpty()

    init {
        lista.layoutManager = LinearLayoutManager(lista.context).apply { stackFromEnd = true }
        val vazio = lista.usarAdapterComVazio(
            adapter,
            if (souAdmin) "Nenhuma mensagem com este aluno ainda.\nMande a primeira!"
            else "Ficou com alguma dúvida? Mande uma mensagem e um professor responde por aqui."
        )
        adapter.aoMudar = { vazio.mostrar(adapter.itemCount == 0) }

        btnEnviar.setOnClickListener { enviar() }
        campo.editText?.apply {
            imeOptions = EditorInfo.IME_ACTION_SEND
            setSingleLine(false)
            maxLines = 4
            setOnEditorActionListener { _, acao, evento ->
                if (acao == EditorInfo.IME_ACTION_SEND || evento?.keyCode == KeyEvent.KEYCODE_ENTER) { enviar(); true } else false
            }
        }

        carregar(rolar = true)

        fragment.viewLifecycleOwner.lifecycleScope.launch {
            fragment.viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                runCatching {
                    Supabase.Chat.escutar(alunoId).collect { nova -> receber(nova) }
                }
            }
        }
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            fragment.viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    delay(20_000)
                    carregar(rolar = false)
                }
            }
        }
    }

    private fun carregar(rolar: Boolean) {
        fragment.lancar {
            val msgs = Supabase.Chat.mensagens(alunoId)
            carregarAutores(msgs)
            val mudou = adapter.definir(msgs)
            if (rolar || mudou) rolarParaFim()
            marcarLidas()
        }
    }

    private suspend fun carregarAutores(msgs: List<Mensagem>) {
        msgs.map { it.autorId }.distinct().filter { it != meuId && !adapter.autores.containsKey(it) }.forEach { id ->
            runCatching { adapter.autores[id] = Supabase.Chat.autor(id) }
        }
    }

    private fun receber(m: Mensagem) {
        fragment.lancar {
            carregarAutores(listOf(m))
            if (adapter.adicionar(m)) {
                rolarParaFim()
                if (m.autorId != meuId) marcarLidas()
            }
        }
    }

    private fun enviar() {
        val texto = campo.texto.trim()
        if (texto.isEmpty()) return
        campo.texto = ""
        btnEnviar.isEnabled = false
        fragment.lancar(aoFalhar = { msg ->
            fragment.toast(msg)
            campo.texto = texto
            btnEnviar.isEnabled = true
        }) {
            val enviada = Supabase.Chat.enviar(alunoId, texto)
            if (adapter.adicionar(enviada)) rolarParaFim()
            btnEnviar.isEnabled = true
        }
    }

    private fun aoTocarDuasVezes(m: Mensagem) {
        if (m.apagada) return
        if (m.autorId != meuId) {
            fragment.toast("Você só pode apagar as suas mensagens")
            return
        }
        abrirPopupConfirmacao(
            fragment.requireContext(),
            titulo = "Apagar mensagem?",
            descricao = "A mensagem será apagada para todos da conversa.",
            textoConfirmar = "Apagar para todos"
        ) {
            fragment.lancar {
                adapter.adicionar(Supabase.Chat.apagar(m.id))
            }
        }
    }

    private suspend fun marcarLidas() {
        runCatching { Supabase.Chat.marcarLidas(alunoId, souAdmin) }
    }

    private fun rolarParaFim() {
        if (adapter.itemCount > 0) lista.post { lista.smoothScrollToPosition(adapter.itemCount - 1) }
    }

    private inner class MensagensAdapter : RecyclerView.Adapter<MensagensAdapter.Holder>() {

        private val itens = mutableListOf<Mensagem>()
        val autores = mutableMapOf<String, Autor>()
        var aoMudar: (() -> Unit)? = null
        private val hora = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

        private val diaMes = DateTimeFormatter.ofPattern("dd/MM").withZone(ZoneId.systemDefault())

        private fun quando(iso: String): String {
            val instante = Tempo.parseInstante(iso)
            val dia = instante.atZone(ZoneId.systemDefault()).toLocalDate()
            val hoje = java.time.LocalDate.now()
            return when (dia) {
                hoje -> hora.format(instante)
                hoje.minusDays(1) -> "Ontem " + hora.format(instante)
                else -> diaMes.format(instante) + " " + hora.format(instante)
            }
        }

        private fun minha(m: Mensagem) = if (souAdmin) !m.doAluno else m.autorId == meuId

        fun definir(lista: List<Mensagem>): Boolean {
            val mudou = lista.map { Triple(it.id, it.lida, it.apagada) } != itens.map { Triple(it.id, it.lida, it.apagada) }
            if (mudou) {
                itens.clear(); itens.addAll(lista)
                notifyDataSetChanged()
            }
            aoMudar?.invoke()
            return mudou && lista.size > 0
        }

        fun adicionar(m: Mensagem): Boolean {
            val i = itens.indexOfFirst { it.id == m.id }
            if (i >= 0) {
                if (itens[i] != m) { itens[i] = m; notifyItemChanged(i) }
                return false
            }
            itens.add(m); itens.sortBy { it.criadaEm }
            notifyDataSetChanged()
            aoMudar?.invoke()
            return true
        }

        override fun getItemViewType(position: Int) = if (minha(itens[position])) 1 else 0

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val layout = if (viewType == 1) R.layout.item_mensagem_enviada else R.layout.item_mensagem_recebida
            return Holder(LayoutInflater.from(parent.context).inflate(layout, parent, false))
        }

        override fun getItemCount() = itens.size

        override fun onBindViewHolder(h: Holder, position: Int) {
            val m = itens[position]
            if (m.apagada) {
                h.texto.text = "Mensagem apagada"
                h.texto.setTypeface(h.fonteNormal, android.graphics.Typeface.ITALIC)
                h.texto.alpha = 0.6f
            } else {
                h.texto.text = m.texto
                h.texto.typeface = h.fonteNormal
                h.texto.alpha = 1f
            }

            var ultimoToque = 0L
            h.itemView.setOnClickListener {
                val agora = android.os.SystemClock.elapsedRealtime()
                if (agora - ultimoToque < 350) {
                    ultimoToque = 0L
                    itens.getOrNull(h.bindingAdapterPosition)?.let { aoTocarDuasVezes(it) }
                } else ultimoToque = agora
            }
            h.hora.text = runCatching { quando(m.criadaEm) }.getOrDefault("")

            val anterior = itens.getOrNull(position - 1)
            val outroAutor = m.autorId != meuId && (anterior == null || anterior.autorId != m.autorId)
            val nome = autores[m.autorId]?.nome
            val mostrarNome = outroAutor && nome != null && !m.doAluno
            h.autor.visibility = if (mostrarNome) View.VISIBLE else View.GONE
            h.autor.text = "Prof. ${primeiroNome(nome.orEmpty())}"

            h.status?.let { st ->
                val cor = if (m.lida) R.color.unifor else android.R.color.darker_gray
                st.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(st.context, cor))
            }
        }

        inner class Holder(v: View) : RecyclerView.ViewHolder(v) {
            val texto: TextView = v.findViewById(R.id.texto_mensagem)
            val fonteNormal: android.graphics.Typeface? = texto.typeface
            val hora: TextView = v.findViewById(R.id.hora_mensagem)
            val autor: TextView = v.findViewById(R.id.autor_mensagem)
            val status: ImageView? = v.findViewById(R.id.status_mensagem)
        }
    }
}
