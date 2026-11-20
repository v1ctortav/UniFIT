package com.unifit.unifit.utilitarios

import android.widget.Button
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputLayout
import com.unifit.unifit.server.Supabase

class EditarInfosPerfil(
    private val fragment: Fragment,
    private val campoNome: TextInputLayout,
    private val campoEmail: TextInputLayout,
    private val campoSenhaAtual: TextInputLayout,
    private val campoNovaSenha: TextInputLayout,
    private val campoConfirmacao: TextInputLayout,
    private val btnSalvar: Button,
    private val aoConcluir: () -> Unit
) {
    private var nomeOriginal = ""
    private var emailOriginal = ""

    init {
        fragment.lancar {
            val u = Supabase.Usuarios.meuPerfil()
            nomeOriginal = u.nome
            emailOriginal = Supabase.Autenticacao.emailLogado().ifEmpty { u.email }
            campoNome.texto = nomeOriginal
            campoEmail.texto = emailOriginal
        }
        btnSalvar.setOnClickListener { salvar() }
    }

    private fun salvar() {
        listOf(campoNome, campoEmail, campoSenhaAtual, campoNovaSenha, campoConfirmacao).forEach { it.erroAbaixo(null) }

        val nome = campoNome.texto.trim()
        val email = campoEmail.texto.trim()
        val senhaAtual = campoSenhaAtual.texto
        val novaSenha = campoNovaSenha.texto
        val confirmacao = campoConfirmacao.texto

        if (senhaAtual.isBlank()) {
            campoSenhaAtual.erroAbaixo("Digite sua senha atual para confirmar")
            return
        }
        if (novaSenha.isNotEmpty() && novaSenha != confirmacao) {
            campoConfirmacao.erroAbaixo("A confirmação não confere com a nova senha")
            return
        }

        val mudouDados = nome != nomeOriginal || !email.equals(emailOriginal, ignoreCase = true)
        if (!mudouDados && novaSenha.isEmpty()) {
            fragment.toast("Nenhuma alteração para salvar")
            return
        }

        btnSalvar.isEnabled = false
        fragment.lancar(aoFalhar = { msg ->
            val campo = when {
                msg.contains("senha atual", true) || msg.contains("senha para confirmar", true) -> campoSenhaAtual
                msg.contains("confirmação", true) -> campoConfirmacao
                msg.contains("nova senha", true) || msg.contains("6 caracteres", true) || msg.contains("senha fraca", true) -> campoNovaSenha
                msg.contains("nome", true) -> campoNome
                msg.contains("e-mail", true) -> campoEmail
                else -> null
            }
            if (campo != null) campo.erroAbaixo(msg) else fragment.toast(msg)
            btnSalvar.isEnabled = true
        }) {
            if (mudouDados) {
                val trocouEmail = Supabase.Autenticacao.alterarDados(nome, email, novaSenha, confirmacao, senhaAtual)
                fragment.toast(
                    if (trocouEmail) "Dados salvos! Confirme o novo e-mail pelo link enviado para ele."
                    else "Informações atualizadas"
                )
            } else {
                Supabase.Autenticacao.alterarSenha(senhaAtual, novaSenha, confirmacao)
                fragment.toast("Senha alterada com sucesso")
            }
            btnSalvar.isEnabled = true
            aoConcluir()
        }
    }
}
