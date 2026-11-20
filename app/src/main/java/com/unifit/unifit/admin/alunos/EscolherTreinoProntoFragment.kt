package com.unifit.unifit.admin.alunos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.unifit.unifit.R
import com.unifit.unifit.server.Supabase
import com.unifit.unifit.server.Treino
import com.unifit.unifit.utilitarios.Categorias
import com.unifit.unifit.utilitarios.configurarPesquisa
import com.unifit.unifit.utilitarios.contemBusca
import com.unifit.unifit.utilitarios.lancar
import com.unifit.unifit.utilitarios.EstadoVazioAdapter
import com.unifit.unifit.utilitarios.usarAdapterComVazio

class EscolherTreinoProntoFragment : Fragment() {

    private lateinit var adapter: TreinoCardProntoAdapter
    private var treinos: List<Treino> = emptyList()
    private var categoria: String? = null
    private var busca = ""
    private lateinit var vazio: EstadoVazioAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_escolher_treino_pronto, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ImageButton>(R.id.btn_voltar_escolher_treino).setOnClickListener {
            voltar(null)
        }

        configurarPesquisa(view.findViewById(R.id.btn_pesquisar_treino_pronto), view.findViewById(R.id.input_pesquisa_treino_pronto)) {
            busca = it
            mostrar()
        }

        val rv = view.findViewById<RecyclerView>(R.id.rv_cards_treinos_prontos)
        adapter = TreinoCardProntoAdapter { voltar(it) }
        vazio = rv.usarAdapterComVazio(adapter, "Nenhum treino pronto encontrado")
        rv.layoutManager = LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)
        rv.addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL).apply {
            setDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.horizontal_divider)!!)
        })

        Categorias.configurarBotoes(
            listOf(
                view.findViewById<Button>(R.id.btn_toggle_todos_view_itens_treino_lista_treinos_prontos),
                view.findViewById(R.id.btn_toggle_peito_view_itens_treino_lista_treinos_prontos),
                view.findViewById(R.id.btn_toggle_costas_view_itens_treino_lista_treinos_prontos),
                view.findViewById(R.id.btn_toggle_ombro_view_itens_treino_lista_treinos_prontos),
                view.findViewById(R.id.btn_toggle_pernas_view_itens_treino_lista_treinos_prontos)
            )
        ) {
            categoria = it
            mostrar()
        }

        lancar {
            treinos = Supabase.Treinos.listar()
            mostrar()
        }
    }

    private fun mostrar() {
        val visiveis = treinos.filter { Categorias.combina(it.categoria, categoria) && it.nome.contemBusca(busca) }
        adapter.updateData(visiveis)
        vazio.mostrar(visiveis.isEmpty())
    }

    private fun voltar(treino: Treino?) {
        parentFragmentManager.setFragmentResult(
            "reabrir_treino_pronto",
            Bundle().apply {
                treino?.let {
                    putString("treino_nome", it.nome)
                    putLong("treino_id", it.id ?: -1L)
                }
            }
        )
        parentFragmentManager.popBackStack()
    }
}
