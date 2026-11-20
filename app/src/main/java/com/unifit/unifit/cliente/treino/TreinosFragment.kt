package com.unifit.unifit.cliente.treino

import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.unifit.unifit.MainActivity
import com.unifit.unifit.R
import com.unifit.unifit.cliente.home.TreinoCardAdapter
import com.unifit.unifit.server.DiaSemana
import com.unifit.unifit.server.Exercicio
import com.unifit.unifit.server.Supabase
import com.unifit.unifit.server.Treino
import com.unifit.unifit.server.TreinoAluno
import com.unifit.unifit.server.TreinoRealizado
import com.unifit.unifit.server.Tempo
import com.unifit.unifit.utilitarios.Categorias
import com.unifit.unifit.utilitarios.EstadoVazioAdapter
import com.unifit.unifit.utilitarios.EstadoVazioCard
import com.unifit.unifit.utilitarios.lancar
import com.unifit.unifit.utilitarios.usarAdapterComVazio

class TreinosFragment : Fragment() {

    private lateinit var treinoCardAdapter: TreinoCardAdapter
    private lateinit var exercicioCardAdapter: ExercicioCardAdapter
    private lateinit var vazioTreinos: EstadoVazioAdapter
    private lateinit var vazioExercicios: EstadoVazioAdapter

    private var treinos: List<Treino> = emptyList()
    private var categoria: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_treinos, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mainActivity = requireActivity() as MainActivity

        val areaTreinoDoDia = view.findViewById<View>(R.id.frameLayout2)
        val cardTreinoDoDia = EstadoVazioCard(
            (areaTreinoDoDia as ViewGroup).getChildAt(0),
            "Nenhum treino para hoje por enquanto.\nSeu professor ainda não montou o treino deste dia."
        )

        Categorias.configurarBotoes(
            listOf(
                view.findViewById<Button>(R.id.btn_toggle_todos_view_itens_treino_lista_treinos),
                view.findViewById(R.id.btn_toggle_peito_view_itens_treino_lista_treinos),
                view.findViewById(R.id.btn_toggle_costas_view_itens_treino_lista_treinos),
                view.findViewById(R.id.btn_toggle_ombro_view_itens_treino_lista_treinos),
                view.findViewById(R.id.btn_toggle_pernas_view_itens_treino_lista_treinos)
            )
        ) { cat ->
            categoria = cat
            mostrarTreinos()
        }

        val rvListaExercicios = view.findViewById<RecyclerView>(R.id.rv_lista_exercicios_treinos)
        exercicioCardAdapter = ExercicioCardAdapter { abrirExercicio(it) }
        vazioExercicios = rvListaExercicios.usarAdapterComVazio(exercicioCardAdapter, "Nenhum exercício cadastrado por enquanto")
        rvListaExercicios.layoutManager = LinearLayoutManager(view.context, RecyclerView.HORIZONTAL, false)
        rvListaExercicios.addItemDecoration(dividerHorizontal())

        val rvListaTreinos = view.findViewById<RecyclerView>(R.id.rv_list_treinos_treinos)
        treinoCardAdapter = TreinoCardAdapter { abrirTreino(it) }
        vazioTreinos = rvListaTreinos.usarAdapterComVazio(treinoCardAdapter, "Nenhum treino nesta categoria por enquanto")
        rvListaTreinos.layoutManager = LinearLayoutManager(view.context, RecyclerView.HORIZONTAL, false)
        rvListaTreinos.addItemDecoration(dividerHorizontal())

        view.findViewById<TextView>(R.id.link_ver_todos_exercicios).setOnClickListener {
            mainActivity.toggleContentMainFrame(ExerciciosBibliotecaFragment())
        }
        view.findViewById<TextView>(R.id.link_ver_todos_treinos).setOnClickListener {
            mainActivity.toggleContentMainFrame(TreinosBibliotecaFragment())
        }

        lancar {
            val lista = Supabase.Exercicios.listar()
            exercicioCardAdapter.updateData(lista)
            vazioExercicios.mostrar(lista.isEmpty())
        }
        lancar {
            treinos = Supabase.Treinos.listar()
            mostrarTreinos()
        }
        parentFragmentManager.setFragmentResultListener(TreinoConcluidoFragment.RESULTADO, viewLifecycleOwner) { _, _ ->
            lancar { marcarSemana(view, Supabase.TreinosAluno.minhaAgenda(), Supabase.TreinosRealizados.daSemana()) }
        }

        lancar {
            val agenda = Supabase.TreinosAluno.minhaAgenda()
            marcarSemana(view, agenda, runCatching { Supabase.TreinosRealizados.daSemana() }.getOrDefault(emptyList()))
            val hoje = agenda.firstOrNull { it.diaSemana == DiaSemana.hoje() }
            cardTreinoDoDia.mostrar(hoje == null)
            hoje?.let { preencherTreinoDoDia(areaTreinoDoDia, it) { t -> abrirTreinoAluno(t) } }
        }
    }

    private fun marcarSemana(view: View, agenda: List<TreinoAluno>, realizados: List<TreinoRealizado>) {
        val icones = listOf(R.id.imageView18, R.id.imageView19, R.id.imageView20, R.id.imageView21,
            R.id.imageView22, R.id.imageView23, R.id.imageView24)
        val textos = listOf(R.id.textView92, R.id.textView93, R.id.textView94, R.id.textView95,
            R.id.textView96, R.id.textView97, R.id.textView98)
        val azul = ContextCompat.getColor(requireContext(), R.color.unifor)
        val cinza = ContextCompat.getColor(requireContext(), R.color.light_gray)
        val azulClaro = ContextCompat.getColor(requireContext(), R.color.unifor50)
        val agendados = agenda.map { it.diaSemana }.toSet()
        val feitos = realizados.mapNotNull { r ->
            r.realizadoEm?.let { runCatching { DiaSemana.de(Tempo.parseInstante(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate()) }.getOrNull() }
        }.toSet()
        val hoje = DiaSemana.hoje()

        for (dia in 0..6) {
            val cor = when {
                dia in feitos -> azul
                dia in agendados && dia >= hoje -> azulClaro
                else -> cinza
            }
            view.findViewById<ImageView>(icones[dia])?.imageTintList = ColorStateList.valueOf(cor)
            view.findViewById<TextView>(textos[dia])?.let { t ->
                if (dia == hoje) {
                    t.setTextColor(azul)
                    t.setTypeface(t.typeface, Typeface.BOLD)
                }
            }
        }
    }

    private fun dividerHorizontal() = DividerItemDecoration(context, LinearLayoutManager.HORIZONTAL).apply {
        setDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.vertical_divider)!!)
    }

    private fun mostrarTreinos() {
        val lista = treinos.filter { Categorias.combina(it.categoria, categoria) }
        treinoCardAdapter.updateData(lista)
        vazioTreinos.mostrar(lista.isEmpty())
    }

    private fun abrirTreino(treino: Treino) {
        val id = treino.id ?: return
        (requireActivity() as MainActivity).toggleContentMainFrameFullScreen(TreinoExecucaoFragment.newInstance(id))
    }

    private fun abrirTreinoAluno(treino: TreinoAluno) {
        val id = treino.id ?: return
        (requireActivity() as MainActivity).toggleContentMainFrameFullScreen(
            TreinoExecucaoFragment.newInstance(id, OrigemTreino.ALUNO)
        )
    }

    private fun abrirExercicio(exercicio: Exercicio) {
        val id = exercicio.id ?: return
        (requireActivity() as MainActivity).toggleContentMainFrameFullScreen(ExercicioExecucaoFragment.newInstance(id))
    }
}
