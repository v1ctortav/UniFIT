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
import com.unifit.unifit.utilitarios.destacarErro
import com.unifit.unifit.utilitarios.lancar

class LoginFragment : Fragment() {

    private lateinit var linkGoToRegister: TextView
    private lateinit var linkGoToResetPassword: TextView
    private lateinit var btnGoToMain: Button
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var errorMensagem: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)
        linkGoToRegister = view.findViewById(R.id.link_go_to_register)
        linkGoToResetPassword = view.findViewById(R.id.link_go_to_reset_password)
        btnGoToMain = view.findViewById(R.id.btn_go_to_main)
        emailEditText = view.findViewById(R.id.email_edit_text)
        passwordEditText = view.findViewById(R.id.password_edit_text)
        errorMensagem = view.findViewById(R.id.error_login_message_text)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        linkGoToRegister.setOnClickListener { (requireActivity() as AuthActivity).goTo(RegisterFragment()) }
        linkGoToResetPassword.setOnClickListener { (requireActivity() as AuthActivity).goTo(ResetPasswordFragment()) }

        btnGoToMain.setOnClickListener {
            val email = emailEditText.text.toString()
            val senha = passwordEditText.text.toString()

            btnGoToMain.isEnabled = false
            errorMensagem.text = ""
            lancar(aoFalhar = { msg ->
                errorMensagem.text = msg
                btnGoToMain.isEnabled = true
                if (msg.contains("incorret", ignoreCase = true) || msg.contains("e-mail", ignoreCase = true)) {
                    destacarErro(
                        listOf(emailEditText, passwordEditText),
                        listOf(view.findViewById(R.id.view2), view.findViewById(R.id.view3))
                    ) { errorMensagem.text = "" }
                }
            }) {
                val usuario = Supabase.Autenticacao.login(email, senha)
                requireActivity().abrirAreaDoUsuario(usuario)
            }
        }
    }
}
