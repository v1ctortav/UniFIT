package com.unifit.unifit.cliente.treino

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.unifit.unifit.MainActivity
import com.unifit.unifit.R

class TreinoConcluidoFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_treino_concluido, container, false)

        val args = arguments
        view.findViewById<TextView>(R.id.textView75).text = (args?.getInt(ARG_MINUTOS) ?: 0).toString()
        view.findViewById<TextView>(R.id.textView77).text = (args?.getInt(ARG_EXERCICIOS) ?: 0).toString()

        view.findViewById<Button>(R.id.btn_close_treino_execucao).setOnClickListener {
            parentFragmentManager.setFragmentResult(RESULTADO, Bundle())
            (requireActivity() as MainActivity).fecharTodasTelasCheias()
        }
        return view
    }

    companion object {
        const val RESULTADO = "treino_concluido"
        private const val ARG_MINUTOS = "minutos"
        private const val ARG_EXERCICIOS = "exercicios"

        @JvmStatic
        fun newInstance(minutos: Int, exercicios: Int) = TreinoConcluidoFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_MINUTOS, minutos)
                putInt(ARG_EXERCICIOS, exercicios)
            }
        }
    }
}
