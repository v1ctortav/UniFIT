package com.unifit.unifit.cliente.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.unifit.unifit.MainActivity
import com.unifit.unifit.R
import com.unifit.unifit.cliente.chat.ChatFragment
import com.unifit.unifit.cliente.notifications.NotificationsFragment
import com.unifit.unifit.cliente.perfil.PerfilFragment
import com.unifit.unifit.cliente.progresso.ProgressoFragment
import com.unifit.unifit.cliente.treino.ExerciciosBibliotecaFragment
import com.unifit.unifit.cliente.treino.TreinoExecucaoFragment
import com.unifit.unifit.cliente.treino.TreinosBibliotecaFragment
import com.unifit.unifit.cliente.treino.OrigemTreino
import com.unifit.unifit.cliente.treino.preencherTreinoDoDia
import com.unifit.unifit.server.Supabase
import com.unifit.unifit.server.Treino
import com.unifit.unifit.server.TreinoAluno
import com.unifit.unifit.utilitarios.EstadoVazioAdapter
import com.unifit.unifit.utilitarios.EstadoVazioCard
import com.unifit.unifit.utilitarios.usarAdapterComVazio
import com.unifit.unifit.utilitarios.Categorias
import com.unifit.unifit.utilitarios.lancar
import com.unifit.unifit.utilitarios.mostrarContador
import com.unifit.unifit.utilitarios.mostrarBase64
import com.unifit.unifit.utilitarios.primeiroNome

class HomeFragment : Fragment() {

    private lateinit var rvListTreinosMain: RecyclerView
    private lateinit var treinoCardAdapter: TreinoCardAdapter
    private lateinit var vazioTreinos: EstadoVazioAdapter

    private var treinos: List<Treino> = emptyList()
    private var categoria: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mainActivity = requireActivity() as MainActivity

        val cardTreinoDoDia = EstadoVazioCard(
            view.findViewById(R.id.treinodia_card),
            "Nenhum treino para hoje por enquanto.\nSeu professor ainda não montou o treino deste dia."
        )
        val textNome = view.findViewById<TextView>(R.id.name_text_main)
        val textNotificacoes = view.findViewById<TextView>(R.id.notifications_count_text)
        textNotificacoes.mostrarContador(null)

        rvListTreinosMain = view.findViewById(R.id.rv_list_treinos_main)
        treinoCardAdapter = TreinoCardAdapter { treino -> abrirTreino(treino) }
        vazioTreinos = rvListTreinosMain.usarAdapterComVazio(treinoCardAdapter, "Nenhum treino nesta categoria por enquanto")
        rvListTreinosMain.layoutManager = LinearLayoutManager(view.context, RecyclerView.HORIZONTAL, false)
        val divider = DividerItemDecoration(context, LinearLayoutManager.HORIZONTAL)
        divider.setDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.vertical_divider)!!)
        rvListTreinosMain.addItemDecoration(divider)

        Categorias.configurarBotoes(
            listOf(
                view.findViewById<Button>(R.id.btn_toggle_todos_view_itens_treino_lista_main),
                view.findViewById(R.id.btn_toggle_peito_view_itens_treino_lista_main),
                view.findViewById(R.id.btn_toggle_costas_view_itens_treino_lista_main),
                view.findViewById(R.id.btn_toggle_ombro_view_itens_treino_lista_main),
                view.findViewById(R.id.btn_toggle_pernas_view_itens_treino_lista_main)
            )
        ) { cat ->
            categoria = cat
            mostrarTreinos()
        }

        view.findViewById<ImageButton>(R.id.btn_go_to_notifications).setOnClickListener {
            mainActivity.toggleContentMainFrame(NotificationsFragment())
        }
        view.findViewById<ImageButton>(R.id.btn_go_to_execucoes_acess).setOnClickListener {
            mainActivity.SetNavBarItem("Treino")
            mainActivity.toggleContentMainFrame(ExerciciosBibliotecaFragment())
        }
        view.findViewById<ImageButton>(R.id.btn_go_to_progresso_acess).setOnClickListener {
            mainActivity.SetNavBarItem("Progresso")
            mainActivity.toggleContentMainFrame(ProgressoFragment())
        }
        view.findViewById<ImageButton>(R.id.btn_go_to_plano_acess).setOnClickListener {
            mainActivity.SetNavBarItem("Perfil")
            mainActivity.toggleContentMainFrame(PerfilFragment())
        }
        view.findViewById<ImageButton>(R.id.btn_go_to_ajuda_acess).setOnClickListener {
            mainActivity.toggleContentMainFrameFullScreen(ChatFragment())
        }
        view.findViewById<TextView>(R.id.link_go_to_treino_library).setOnClickListener {
            mainActivity.SetNavBarItem("Treino")
            mainActivity.toggleContentMainFrame(TreinosBibliotecaFragment())
        }

        lancar {
            val usuario = Supabase.Usuarios.meuPerfil()
            textNome.text = "${primeiroNome(usuario.nome)}!"
            view.findViewById<android.widget.ImageView>(R.id.imageView3).mostrarBase64(usuario.fotoPerfil)
        }
        lancar {
            textNotificacoes.mostrarContador(Supabase.Notificacoes.contarNaoLidas())
        }
        lancar {
            treinos = Supabase.Treinos.listar()
            mostrarTreinos()
        }
        lancar {
            val hoje = Supabase.TreinosAluno.meuTreinoDoDia()
            cardTreinoDoDia.mostrar(hoje == null)
            hoje?.let { preencherTreinoDoDia(view.findViewById(R.id.treinodia_card), it) { t -> abrirTreinoAluno(t) } }
        }
    }

    private fun mostrarTreinos() {
        val lista = treinos.filter { Categorias.combina(it.categoria, categoria) }
        treinoCardAdapter.updateData(lista)
        vazioTreinos.mostrar(lista.isEmpty())
    }

    private fun abrirTreinoAluno(treino: TreinoAluno) {
        val id = treino.id ?: return
        (requireActivity() as MainActivity).toggleContentMainFrameFullScreen(
            TreinoExecucaoFragment.newInstance(id, OrigemTreino.ALUNO)
        )
    }

    private fun abrirTreino(treino: Treino) {
        val id = treino.id ?: return
        (requireActivity() as MainActivity).toggleContentMainFrameFullScreen(TreinoExecucaoFragment.newInstance(id))
    }
}
