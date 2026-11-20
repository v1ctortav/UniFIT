package com.unifit.unifit.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.unifit.unifit.MainAdminActivity
import com.unifit.unifit.R
import com.unifit.unifit.server.Supabase
import com.unifit.unifit.utilitarios.lancar

class ResetPasswordFragment : Fragment() {

    private lateinit var linkBackToLogin: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_reset_password, container, false)

        linkBackToLogin = view.findViewById(R.id.link_back_to_login)
        if (requireActivity() is MainAdminActivity) linkBackToLogin.text = "Voltar"

        val veioDoPerfil = requireActivity().intent.getBooleanExtra("recuperar_senha_perfil", false)
        if (veioDoPerfil) linkBackToLogin.text = "Voltar"

        linkBackToLogin.setOnClickListener {
            if (veioDoPerfil) requireActivity().finish()
            else (requireActivity() as AuthActivity).goTo(LoginFragment())
        }

        val campoEmail = view.findViewById<EditText>(R.id.email_edit_text_reset_password)
        val feedback = view.findViewById<TextView>(R.id.feedback_email_reset_password_text)
        val btnEnviar = view.findViewById<Button>(R.id.btn_send_reset_password_email)

        if (veioDoPerfil) campoEmail.setText(Supabase.Autenticacao.emailLogado())

        btnEnviar.setOnClickListener {
            btnEnviar.isEnabled = false
            lancar(aoFalhar = { msg ->
                feedback.text = msg
                feedback.visibility = View.VISIBLE
                btnEnviar.isEnabled = true
            }) {
                Supabase.Autenticacao.recuperarSenha(campoEmail.text.toString())
                feedback.text = "Se existir uma conta com o e-mail informado, será enviado o link de alteração da senha."
                feedback.visibility = View.VISIBLE
                btnEnviar.isEnabled = true
            }
        }

        return view
    }
}
