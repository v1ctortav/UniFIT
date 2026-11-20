package com.unifit.unifit.cliente.treino

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
import com.unifit.unifit.MainActivity
import com.unifit.unifit.R
import com.unifit.unifit.server.Exercicio
import com.unifit.unifit.server.Supabase
import com.unifit.unifit.utilitarios.Categorias
import com.unifit.unifit.utilitarios.configurarPesquisa
import com.unifit.unifit.utilitarios.contemBusca
import com.unifit.unifit.utilitarios.lancar
import com.unifit.unifit.utilitarios.EstadoVazioAdapter
import com.unifit.unifit.utilitarios.usarAdapterComVazio

class ExerciciosBibliotecaFragment : Fragment() {

    private lateinit var adapter: ExerciciosCardBibliotecaAdapter
    private var exercicios: List<Exercicio> = emptyList()
    private var categoria: String? = null
    private var busca = ""
    private lateinit var vazio: EstadoVazioAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_exercicios_biblioteca, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mainActivity = requireActivity() as MainActivity

        view.findViewById<ImageButton>(R.id.btn_voltar_exercicios_treinos).setOnClickListener {
            mainActivity.toggleContentMainFrame(TreinosFragment())
        }

        configurarPesquisa(view.findViewById(R.id.btn_pesquisar_exercicio), view.findViewById(R.id.input_pesquisa_exercicio)) {
            busca = it
            mostrar()
        }

        Categorias.configurarBotoes(
            listOf(
                view.findViewById<Button>(R.id.btn_toggle_todos_view_itens_treino_lista_exercicios),
                view.findViewById(R.id.btn_toggle_peito_view_itens_treino_lista_exercicios),
                view.findViewById(R.id.btn_toggle_costas_view_itens_treino_lista_exercicios),
                view.findViewById(R.id.btn_toggle_ombro_view_itens_treino_lista_exercicios),
                view.findViewById(R.id.btn_toggle_pernas_view_itens_treino_lista_exercicios)
            )
        ) {
            categoria = it
            mostrar()
        }

        val rv = view.findViewById<RecyclerView>(R.id.rv_cards_exercicios)
        adapter = ExerciciosCardBibliotecaAdapter { ex ->
            ex.id?.let { mainActivity.toggleContentMainFrameFullScreen(ExercicioExecucaoFragment.newInstance(it)) }
        }
        vazio = rv.usarAdapterComVazio(adapter, "Nenhum exercício encontrado por enquanto")
        rv.layoutManager = LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)
        rv.addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL).apply {
            setDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.horizontal_divider)!!)
        })

        lancar {
            exercicios = Supabase.Exercicios.listar()
            mostrar()
        }
    }

    private fun mostrar() {
        val visiveis = exercicios.filter { Categorias.combina(it.categoria, categoria) && it.nome.contemBusca(busca) }
        adapter.updateData(visiveis)
        vazio.mostrar(visiveis.isEmpty())
    }
}
