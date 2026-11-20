package com.unifit.unifit.admin.alunos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.unifit.unifit.MainAdminActivity
import com.unifit.unifit.R
import com.unifit.unifit.server.Supabase
import com.unifit.unifit.utilitarios.TelaChat
import com.unifit.unifit.utilitarios.lancar
import com.unifit.unifit.utilitarios.mostrarBase64

class ChatAdminFragment : Fragment() {

    private val alunoId: String by lazy { requireArguments().getString(ARG_ALUNO).orEmpty() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_chat_admin, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ImageButton>(R.id.btn_voltar_chat_aluno).setOnClickListener {
            (requireActivity() as MainAdminActivity).closeFullScreenFrame()
        }

        val nome = view.findViewById<TextView>(R.id.textView169)
        val foto = view.findViewById<ImageView>(R.id.imageView32)
        arguments?.getString(ARG_NOME)?.let { nome.text = it }
        lancar {
            val aluno = Supabase.Usuarios.buscar(alunoId)
            nome.text = aluno.nome
            foto.mostrarBase64(aluno.fotoPerfil)
        }

        TelaChat(
            fragment = this,
            alunoId = alunoId,
            souAdmin = true,
            lista = view.findViewById(R.id.rv_mensagens_chat_admin),
            campo = view.findViewById(R.id.input_chat_admin),
            btnEnviar = view.findViewById(R.id.imageButton5)
        )
    }

    companion object {
        private const val ARG_ALUNO = "aluno_id"
        private const val ARG_NOME = "nome_aluno"

        @JvmStatic
        fun newInstance(alunoId: String, nomeAluno: String) = ChatAdminFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_ALUNO, alunoId)
                putString(ARG_NOME, nomeAluno)
            }
        }
    }
}
