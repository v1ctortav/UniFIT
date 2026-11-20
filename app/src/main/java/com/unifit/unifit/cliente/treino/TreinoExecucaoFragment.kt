package com.unifit.unifit.cliente.treino

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.unifit.unifit.MainActivity
import com.unifit.unifit.R
import com.unifit.unifit.server.TreinoCompleto
import com.unifit.unifit.utilitarios.lancar
import com.unifit.unifit.utilitarios.toast
import com.unifit.unifit.utilitarios.EstadoVazioAdapter
import com.unifit.unifit.utilitarios.usarAdapterComVazio
import com.unifit.unifit.utilitarios.mostrarCapaTreino

class TreinoExecucaoFragment : Fragment() {

    private val treinoId: Long by lazy { requireArguments().getLong(ARG_TREINO_ID) }
    private val origem: OrigemTreino by lazy {
        OrigemTreino.valueOf(requireArguments().getString(ARG_ORIGEM) ?: OrigemTreino.BIBLIOTECA.name)
    }
    private var completo: TreinoCompleto? = null

    private lateinit var rvAdapter: ExerciciosItemTreinoAdapter
    private lateinit var vazio: EstadoVazioAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_treino_execucao, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mainActivity = requireActivity() as MainActivity

        view.findViewById<ImageButton>(R.id.btn_back_treino_execucao).setOnClickListener {
            mainActivity.closeFullScreenFrame()
        }

        rvAdapter = ExerciciosItemTreinoAdapter()
        view.findViewById<RecyclerView>(R.id.rv_exercicios_treino_execucao).apply {
            layoutManager = LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)
            vazio = usarAdapterComVazio(rvAdapter, "Este treino ainda não tem exercícios")
        }

        val textNome = view.findViewById<TextView>(R.id.textView68)
        val textDescricao = view.findViewById<TextView>(R.id.textView69)
        val textCategoria = view.findViewById<TextView>(R.id.textView70)

        lancar {
            val t = carregarTreino(origem, treinoId)
            completo = t
            view.findViewById<android.widget.ImageView>(R.id.imageView10).mostrarCapaTreino(t.treino)
            textNome.text = t.treino.nome
            textDescricao.text = t.treino.descricao.ifBlank { "${t.itens.size} exercícios" }
            textCategoria.text = t.treino.categoria
            rvAdapter.updateData(t.itens)
            vazio.mostrar(t.itens.isEmpty())
        }

        view.findViewById<Button>(R.id.btn_iniciar_treino_execucao).setOnClickListener {
            val t = completo ?: return@setOnClickListener
            if (t.itens.none { it.second != null }) {
                toast("Este treino ainda não tem exercícios")
                return@setOnClickListener
            }
            mainActivity.toggleContentMainFrameFullScreen(ExercicioTreinoExecucaoFragment.newInstance(treinoId, origem))
        }
    }

    companion object {
        private const val ARG_TREINO_ID = "treino_id"
        private const val ARG_ORIGEM = "origem"

        @JvmStatic
        fun newInstance(treinoId: Long, origem: OrigemTreino = OrigemTreino.BIBLIOTECA) = TreinoExecucaoFragment().apply {
            arguments = Bundle().apply {
                putLong(ARG_TREINO_ID, treinoId)
                putString(ARG_ORIGEM, origem.name)
            }
        }
    }
}
