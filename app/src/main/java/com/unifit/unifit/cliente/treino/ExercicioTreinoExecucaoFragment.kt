package com.unifit.unifit.cliente.treino

import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.unifit.unifit.MainActivity
import com.unifit.unifit.R
import com.unifit.unifit.server.Exercicio
import com.unifit.unifit.server.ItemTreino
import com.unifit.unifit.server.Supabase
import com.unifit.unifit.utilitarios.abrirPopupConfirmacao
import com.unifit.unifit.utilitarios.emTopicos
import com.unifit.unifit.utilitarios.lancar
import com.unifit.unifit.utilitarios.toast
import com.unifit.unifit.utilitarios.YoutubePlayerView

class ExercicioTreinoExecucaoFragment : Fragment() {

    private val treinoId: Long by lazy { requireArguments().getLong(ARG_TREINO_ID) }
    private val origem: OrigemTreino by lazy {
        OrigemTreino.valueOf(requireArguments().getString(ARG_ORIGEM) ?: OrigemTreino.BIBLIOTECA.name)
    }
    private var player: YoutubePlayerView? = null

    private var itens: List<Pair<ItemTreino, Exercicio>> = emptyList()
    private var indice = 0
    private var inicio = 0L
    private var nomeTreino = "Treino"
    private val ultimoPeso = mutableMapOf<Long, Double?>()

    private lateinit var textNome: TextView
    private lateinit var textSeries: TextView
    private lateinit var textRepeticoes: TextView
    private lateinit var textPeso: TextView
    private lateinit var textEquipamentos: TextView
    private lateinit var btnProx: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        indice = savedInstanceState?.getInt(STATE_INDICE) ?: 0
        inicio = savedInstanceState?.getLong(STATE_INICIO) ?: SystemClock.elapsedRealtime()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_INDICE, indice)
        outState.putLong(STATE_INICIO, inicio)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_exercicio_treino_execucao, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val player = view.findViewById<YoutubePlayerView>(R.id.player_video)
        this.player = player
        player.vincularBotoes(view.findViewById(R.id.imageButton2), view.findViewById(R.id.imageButton3))
        textNome = view.findViewById(R.id.textView79)
        textSeries = view.findViewById(R.id.textView83)
        textRepeticoes = view.findViewById(R.id.textView84)
        textPeso = view.findViewById(R.id.textView85)
        textEquipamentos = view.findViewById(R.id.textView87)
        btnProx = view.findViewById(R.id.btn_proximo_exercicio)

        view.findViewById<ImageButton>(R.id.btn_close_exercicio).setOnClickListener { confirmarEncerrar() }

        view.findViewById<Button>(R.id.btn_editar_peso_exercicio).setOnClickListener {
            val exercicioId = itens.getOrNull(indice)?.second?.id ?: return@setOnClickListener
            abrirEditarPeso(exercicioId, ultimoPeso[exercicioId]) { registro ->
                ultimoPeso[exercicioId] = registro.peso
                textPeso.text = textoPeso(registro.peso)
            }
        }

        btnProx.setOnClickListener {
            if (indice < itens.lastIndex) {
                indice++
                mostrarExercicioAtual()
            } else {
                concluirTreino()
            }
        }

        lancar {
            val completo = carregarTreino(origem, treinoId)
            itens = completo.itens.mapNotNull { (item, ex) -> ex?.let { item to it } }
            nomeTreino = completo.treino.nome
            indice = indice.coerceIn(0, (itens.size - 1).coerceAtLeast(0))
            mostrarExercicioAtual()
        }
    }

    private fun mostrarExercicioAtual() {
        val (item, exercicio) = itens.getOrNull(indice) ?: return
        player?.carregar(exercicio.linkYoutube)
        textNome.text = exercicio.nome
        textSeries.text = "${item.series} Séries"
        textRepeticoes.text = "${item.repeticoes} Rep."
        textEquipamentos.text = exercicio.equipamentos.emTopicos()
        btnProx.text = if (indice < itens.lastIndex) "Próximo exercício" else "Concluir treino"

        val id = exercicio.id ?: return
        if (ultimoPeso.containsKey(id)) {
            textPeso.text = textoPeso(ultimoPeso[id])
        } else {
            textPeso.text = textoPeso(null)
            lancar {
                val peso = Supabase.PesosExercicio.ultimoPeso(id)?.peso
                ultimoPeso[id] = peso
                if (itens.getOrNull(indice)?.second?.id == id) textPeso.text = textoPeso(peso)
            }
        }
    }

    private fun concluirTreino() {
        val segundos = (SystemClock.elapsedRealtime() - inicio) / 1000
        val minutos = ((segundos + 30) / 60).toInt().coerceAtLeast(1)
        btnProx.isEnabled = false
        lancar(aoFalhar = { msg ->
            toast("Não foi possível salvar o treino: $msg")
            abrirConcluido(minutos)
        }) {
            Supabase.TreinosRealizados.registrar(nomeTreino, minutos, itens.size)
            abrirConcluido(minutos)
        }
    }

    private fun abrirConcluido(minutos: Int) {
        btnProx.isEnabled = true
        (requireActivity() as MainActivity).toggleContentMainFrameFullScreen(
            TreinoConcluidoFragment.newInstance(minutos, itens.size)
        )
    }

    override fun onPause() {
        super.onPause()
        player?.pausar()
    }

    override fun onDestroyView() {
        player?.liberar()
        player = null
        super.onDestroyView()
    }

    private fun confirmarEncerrar() {
        abrirPopupConfirmacao(
            requireContext(),
            titulo = "Encerrar treino?",
            descricao = "Tem certeza que deseja encerrar o treino antes de concluir?",
            textoConfirmar = "Encerrar",
            textoCancelar = "Cancelar"
        ) {
            val mainActivity = requireActivity() as MainActivity
            mainActivity.fecharTodasTelasCheias()

            val telaPrincipal = mainActivity.supportFragmentManager.findFragmentById(R.id.frame_conteudo_main)
            if (telaPrincipal !is TreinosFragment) mainActivity.toggleContentMainFrame(TreinosFragment())
            mainActivity.SetNavBarItem("Treino")
        }
    }

    companion object {
        private const val ARG_TREINO_ID = "treino_id"
        private const val ARG_ORIGEM = "origem"
        private const val STATE_INDICE = "indice"
        private const val STATE_INICIO = "inicio"

        @JvmStatic
        fun newInstance(treinoId: Long, origem: OrigemTreino = OrigemTreino.BIBLIOTECA) = ExercicioTreinoExecucaoFragment().apply {
            arguments = Bundle().apply {
                putLong(ARG_TREINO_ID, treinoId)
                putString(ARG_ORIGEM, origem.name)
            }
        }
    }
}
