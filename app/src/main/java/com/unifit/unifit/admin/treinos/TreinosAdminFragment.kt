package com.unifit.unifit.admin.treinos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.unifit.unifit.MainAdminActivity
import com.unifit.unifit.R
import com.unifit.unifit.server.Supabase
import com.unifit.unifit.server.Treino
import com.unifit.unifit.utilitarios.Categorias
import com.unifit.unifit.utilitarios.Resultados
import com.unifit.unifit.utilitarios.lancar
import com.unifit.unifit.utilitarios.EstadoVazioAdapter
import com.unifit.unifit.utilitarios.usarAdapterComVazio

class TreinosAdminFragment : Fragment() {

    private lateinit var treinoCardAdapter: TreinoCardAdminAdapter
    private lateinit var exercicioCardAdapter: ExercicioCardAdminAdapter
    private lateinit var totalExercicios: TextView
    private lateinit var totalTreinos: TextView

    private var treinos: List<Treino> = emptyList()
    private var categoria: String? = null
    private lateinit var vazioExercicios: EstadoVazioAdapter
    private lateinit var vazioTreinos: EstadoVazioAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_treinos_admin, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mainActivity = requireActivity() as MainAdminActivity

        totalExercicios = view.findViewById(R.id.textView204)
        totalTreinos = view.findViewById(R.id.textView206)

        Categorias.configurarBotoes(
            listOf(
                view.findViewById<Button>(R.id.btn_toggle_todos_view_itens_treino_lista_treinos_biblioteca_admin),
                view.findViewById(R.id.btn_toggle_peito_view_itens_treino_lista_treinos_biblioteca_admin),
                view.findViewById(R.id.btn_toggle_costas_view_itens_treino_lista_treinos_biblioteca_admin),
                view.findViewById(R.id.btn_toggle_ombro_view_itens_treino_lista_treinos_biblioteca_admin),
                view.findViewById(R.id.btn_toggle_pernas_view_itens_treino_lista_treinos_biblioteca_admin)
            )
        ) {
            categoria = it
            mostrarTreinos()
        }

        val rvExercicios = view.findViewById<RecyclerView>(R.id.rv_exercicios_biblioteca_admin)
        exercicioCardAdapter = ExercicioCardAdminAdapter("Item") { ex ->
            mainActivity.toggleContentMainFrameFullScreen(GerenciarExercicioFragment.newInstance(ex.id))
        }
        vazioExercicios = rvExercicios.usarAdapterComVazio(exercicioCardAdapter, "Nenhum exercício cadastrado por enquanto")
        rvExercicios.layoutManager = LinearLayoutManager(view.context, RecyclerView.HORIZONTAL, false)
        rvExercicios.addItemDecoration(dividerHorizontal())

        val rvTreinos = view.findViewById<RecyclerView>(R.id.rv_list_treinos_treinos_admin)
        treinoCardAdapter = TreinoCardAdminAdapter("Card") { t ->
            mainActivity.toggleContentMainFrameFullScreen(GerenciarTreinoFragment.newInstance(t.id))
        }
        vazioTreinos = rvTreinos.usarAdapterComVazio(treinoCardAdapter, "Nenhum treino nesta categoria por enquanto")
        rvTreinos.layoutManager = LinearLayoutManager(view.context, RecyclerView.HORIZONTAL, false)
        rvTreinos.addItemDecoration(dividerHorizontal())

        view.findViewById<TextView>(R.id.link_ver_todos_exercicios_admin).setOnClickListener {
            mainActivity.toggleContentMainFrame(ExerciciosBibliotecaAdminFragment())
        }
        view.findViewById<TextView>(R.id.link_ver_todos_treinos_admin).setOnClickListener {
            mainActivity.toggleContentMainFrame(TreinosBibliotecaAdminFragment())
        }

        parentFragmentManager.setFragmentResultListener(Resultados.EXERCICIOS_ALTERADOS, viewLifecycleOwner) { _, _ -> carregarExercicios() }
        parentFragmentManager.setFragmentResultListener(Resultados.TREINOS_ALTERADOS, viewLifecycleOwner) { _, _ -> carregarTreinos() }

        carregarExercicios()
        carregarTreinos()
    }

    private fun carregarExercicios() {
        lancar {
            val lista = Supabase.Exercicios.listar()
            totalExercicios.text = lista.size.toString()
            exercicioCardAdapter.updateData(lista)
            vazioExercicios.mostrar(lista.isEmpty())
        }
    }

    private fun carregarTreinos() {
        lancar {
            treinos = Supabase.Treinos.listar()
            totalTreinos.text = treinos.size.toString()
            mostrarTreinos()
        }
    }

    private fun mostrarTreinos() {
        val lista = treinos.filter { Categorias.combina(it.categoria, categoria) }
        treinoCardAdapter.updateData(lista)
        vazioTreinos.mostrar(lista.isEmpty())
    }

    private fun dividerHorizontal() = DividerItemDecoration(context, LinearLayoutManager.HORIZONTAL).apply {
        setDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.vertical_divider)!!)
    }
}
