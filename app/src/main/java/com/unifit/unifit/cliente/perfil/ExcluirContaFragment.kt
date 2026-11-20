package com.unifit.unifit.cliente.perfil

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputLayout
import com.unifit.unifit.MainActivity
import com.unifit.unifit.R
import com.unifit.unifit.auth.AuthActivity
import com.unifit.unifit.server.Supabase
import com.unifit.unifit.utilitarios.lancar
import com.unifit.unifit.utilitarios.texto
import com.unifit.unifit.utilitarios.toast
import com.unifit.unifit.utilitarios.voltarParaLogin

class ExcluirContaFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_excluir_conta, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ImageButton>(R.id.btn_back_perfil_excluir_conta).setOnClickListener {
            (requireActivity() as MainActivity).closeFullScreenFrame()
        }
        view.findViewById<TextView>(R.id.textView66).setOnClickListener {
            startActivity(Intent(requireContext(), AuthActivity::class.java).putExtra("recuperar_senha_perfil", true))
        }

        val campoEmail = view.findViewById<TextInputLayout>(R.id.textInputLayout9)
        val campoSenha = view.findViewById<TextInputLayout>(R.id.textInputLayout10)
        val btnExcluir = view.findViewById<Button>(R.id.button)

        btnExcluir.setOnClickListener {
            campoEmail.error = null
            campoSenha.error = null

            if (!campoEmail.texto.trim().equals(Supabase.Autenticacao.emailLogado(), ignoreCase = true)) {
                campoEmail.error = "Digite o e-mail desta conta"
                return@setOnClickListener
            }
            if (campoSenha.texto.isBlank()) {
                campoSenha.error = "Digite sua senha"
                return@setOnClickListener
            }

            btnExcluir.isEnabled = false
            lancar(aoFalhar = { msg ->
                campoSenha.error = msg
                btnExcluir.isEnabled = true
            }) {
                Supabase.Autenticacao.excluirConta(campoSenha.texto)
                toast("Conta excluída")
                requireActivity().voltarParaLogin()
            }
        }
    }
}
