package com.unifit.unifit.utilitarios

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputLayout
import com.unifit.unifit.R
import com.unifit.unifit.server.AbaNotificacao
import com.unifit.unifit.server.FiltroMenuNotificacao
import com.unifit.unifit.server.FiltroNotificacao
import com.unifit.unifit.server.Notificacao
import com.unifit.unifit.server.Supabase
import com.unifit.unifit.server.aplicarFiltro
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import com.unifit.unifit.utilitarios.EstadoVazioAdapter
import com.unifit.unifit.utilitarios.usarAdapterComVazio

class TelaNotificacoes(
    private val fragment: Fragment,
    view: View,
    idBusca: Int,
    idBtnBusca: Int,
    idBtnMenu: Int,
    idBtnTodas: Int,
    idBtnNaoLidas: Int,
    idLista: Int,
    private val aoAbrirChat: ((alunoId: String) -> Unit)? = null
) {
    private var todas: List<Notificacao> = emptyList()
    private var filtro = FiltroNotificacao()
    private val adapter = NotificationItemAdapter { aoTocar(it) }
    private lateinit var lista: RecyclerView
    private val btnMenu = view.findViewById<ImageButton>(idBtnMenu)
    private lateinit var vazio: EstadoVazioAdapter

    init {
        lista = view.findViewById<RecyclerView>(idLista).apply {
            layoutManager = LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)
            vazio = usarAdapterComVazio(this@TelaNotificacoes.adapter, "Nenhuma notificação por enquanto")
        }
        ArrastarParaExcluir.ligar(
            lista,
            podeArrastar = { it is NotificationItemAdapter.NotificationViewHolder },
            aoExcluir = { pos -> adapter.itemEm(pos)?.let { excluir(it) } ?: mostrar() }
        )

        configurarPesquisa(view.findViewById(idBtnBusca), view.findViewById<TextInputLayout>(idBusca)) { termo ->
            filtro = filtro.copy(busca = termo)
            mostrar()
        }

        btnMenu.setOnClickListener { abrirMenuFiltro(btnMenu) }

        val btnTodas = view.findViewById<Button>(idBtnTodas)
        val btnNaoLidas = view.findViewById<Button>(idBtnNaoLidas)
        listOf(btnTodas, btnNaoLidas).forEach { btn ->
            btn.isSelected = btn == btnTodas
            btn.setOnClickListener {
                btnTodas.isSelected = btn == btnTodas
                btnNaoLidas.isSelected = btn == btnNaoLidas
                filtro = filtro.copy(aba = if (btn == btnTodas) AbaNotificacao.TODAS else AbaNotificacao.NAO_LIDAS)
                mostrar()
            }
        }

        carregar()

        fragment.viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                delay(60_000)
                adapter.atualizarTempos()
            }
        }
    }

    fun carregar() {
        fragment.lancar {
            todas = Supabase.Notificacoes.listarMinhas()
            mostrar()
        }
    }

    private fun mostrar() {
        val lista = todas.aplicarFiltro(filtro)
        adapter.updateData(lista)
        vazio.mostrar(lista.isEmpty(), if (todas.isEmpty()) "Nenhuma notificação" else "Nenhuma notificação")
    }

    private fun aoTocar(n: Notificacao) {
        marcarComoLida(n)
        n.alunoDoChat?.let { aluno -> aoAbrirChat?.invoke(aluno) }
    }

    private fun excluir(n: Notificacao) {
        todas = todas.filterNot { it.id == n.id }
        mostrar()
        Snackbar.make(lista, "Notificação excluída", Snackbar.LENGTH_LONG)
            .setAction("Desfazer") {
                todas = (todas + n).sortedByDescending { it.criadaEm }
                mostrar()
            }
            .setActionTextColor(android.graphics.Color.parseColor("#8FB0FF"))
            .addCallback(object : Snackbar.Callback() {
                override fun onDismissed(barra: Snackbar?, evento: Int) {
                    if (evento != DISMISS_EVENT_ACTION) {
                        CoroutineScope(Dispatchers.IO).launch { runCatching { Supabase.Notificacoes.excluir(n.id) } }
                    }
                }
            })
            .show()
    }

    private fun marcarComoLida(n: Notificacao) {
        if (n.lida) return
        fragment.lancar {
            Supabase.Notificacoes.marcarComoLida(n.id)
            todas = todas.map { if (it.id == n.id) it.copy(lida = true) else it }
            mostrar()
        }
    }

    private fun abrirMenuFiltro(anchor: View) {
        val ctx = anchor.context
        val popupView = LayoutInflater.from(ctx).inflate(R.layout.menu_filter_notifications, null)
        val popupWindow = PopupWindow(
            popupView,
            (200 * ctx.resources.displayMetrics.density).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        popupWindow.isOutsideTouchable = true
        popupWindow.elevation = 12f
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val btnNaoLidas = popupView.findViewById<Button>(R.id.btn_nlidas_filter_notifications)
        val btnSistema = popupView.findViewById<Button>(R.id.btn_sistema_filter_notifications)

        val azul = ContextCompat.getColor(ctx, R.color.unifor)
        if (filtro.menu == FiltroMenuNotificacao.NAO_LIDA) btnNaoLidas.setTextColor(azul)
        if (filtro.menu == FiltroMenuNotificacao.SISTEMA) btnSistema.setTextColor(azul)

        fun escolher(opcao: FiltroMenuNotificacao) {
            filtro = filtro.copy(menu = if (filtro.menu == opcao) FiltroMenuNotificacao.NENHUM else opcao)
            mostrar()
            popupWindow.dismiss()
        }
        btnNaoLidas.setOnClickListener { escolher(FiltroMenuNotificacao.NAO_LIDA) }
        btnSistema.setOnClickListener { escolher(FiltroMenuNotificacao.SISTEMA) }

        popupWindow.showAsDropDown(anchor, 0, -45)
    }
}
