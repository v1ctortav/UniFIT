package com.unifit.unifit.cliente.treino

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.unifit.unifit.MainActivity
import com.unifit.unifit.R
import com.unifit.unifit.server.Supabase
import com.unifit.unifit.utilitarios.YoutubePlayerView
import com.unifit.unifit.utilitarios.emTopicos
import com.unifit.unifit.utilitarios.lancar

class ExercicioExecucaoFragment : Fragment() {

    private val exercicioId: Long by lazy { requireArguments().getLong(ARG_EXERCICIO_ID) }
    private var ultimoPeso: Double? = null
    private var player: YoutubePlayerView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_exercicio_execucao, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.link_back_treinos_exercicio).setOnClickListener {
            (requireActivity() as MainActivity).closeFullScreenFrame()
        }

        val textPeso = view.findViewById<TextView>(R.id.textView108)
        val textNome = view.findViewById<TextView>(R.id.textView109)
        val textEquipamentos = view.findViewById<TextView>(R.id.textView111)

        val player = view.findViewById<YoutubePlayerView>(R.id.player_video)
        this.player = player
        player.vincularBotoes(view.findViewById<ImageButton>(R.id.imageButton), view.findViewById(R.id.imageButton4))

        view.findViewById<Button>(R.id.btn_editar_peso_exercicio_execucao).setOnClickListener {
            abrirEditarPeso(exercicioId, ultimoPeso) { registro ->
                ultimoPeso = registro.peso
                textPeso.text = textoPeso(registro.peso, espaco = false)
            }
        }

        lancar {
            val ex = Supabase.Exercicios.buscar(exercicioId)
            player.carregar(ex.linkYoutube)
            textNome.text = ex.nome
            textEquipamentos.text = ex.equipamentos.emTopicos()

            ultimoPeso = Supabase.PesosExercicio.ultimoPeso(exercicioId)?.peso
            textPeso.text = textoPeso(ultimoPeso, espaco = false)
        }
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

    companion object {
        private const val ARG_EXERCICIO_ID = "exercicio_id"

        @JvmStatic
        fun newInstance(exercicioId: Long) = ExercicioExecucaoFragment().apply {
            arguments = Bundle().apply { putLong(ARG_EXERCICIO_ID, exercicioId) }
        }
    }
}
