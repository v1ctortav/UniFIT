package com.unifit.unifit.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.unifit.unifit.R
import com.unifit.unifit.server.Supabase
import com.unifit.unifit.utilitarios.abrirAreaDoUsuario
import com.unifit.unifit.utilitarios.lancar

class RegisterFragment : Fragment() {

    private lateinit var linkGoToLogin: TextView
    private lateinit var btnRegistrar: Button
    private lateinit var campoNome: EditText
    private lateinit var campoEmail: EditText
    private lateinit var campoSenha: EditText
    private lateinit var mensagemFeedback: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_register, container, false)
        linkGoToLogin = view.findViewById(R.id.link_go_to_login)
        btnRegistrar = view.findViewById(R.id.btn_register)
        campoNome = view.findViewById(R.id.name_edit_text_register)
        campoEmail = view.findViewById(R.id.email_edit_text_register)
        campoSenha = view.findViewById(R.id.password_edit_text_register)
        mensagemFeedback = view.findViewById(R.id.error_register_message_text)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        linkGoToLogin.setOnClickListener { (requireActivity() as AuthActivity).goTo(LoginFragment()) }

        btnRegistrar.setOnClickListener {
            val nome = campoNome.text.toString()
            val email = campoEmail.text.toString()
            val senha = campoSenha.text.toString()

            btnRegistrar.isEnabled = false
            mensagemFeedback.text = ""
            lancar(aoFalhar = { msg ->
                mensagemFeedback.text = msg
                btnRegistrar.isEnabled = true
            }) {
                val logado = Supabase.Autenticacao.cadastrar(nome, email, senha)
                if (logado) {
                    requireActivity().abrirAreaDoUsuario(Supabase.Usuarios.meuPerfil())
                } else {
                    mensagemFeedback.text = "Conta criada! Confirme seu e-mail pelo link enviado e depois faça login."
                    btnRegistrar.isEnabled = true
                }
            }
        }
    }
}
