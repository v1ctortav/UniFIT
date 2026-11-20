package com.unifit.unifit.cliente.perfil

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.unifit.unifit.MainActivity
import com.unifit.unifit.R
import com.unifit.unifit.server.Plano
import com.unifit.unifit.server.Supabase
import com.unifit.unifit.server.Tempo
import com.unifit.unifit.server.Usuario
import com.unifit.unifit.utilitarios.Resultados
import com.unifit.unifit.utilitarios.SeletorImagem
import com.unifit.unifit.utilitarios.abrirEditarFotoPerfil
import com.unifit.unifit.utilitarios.abrirPopupConfirmacao
import com.unifit.unifit.utilitarios.lancar
import com.unifit.unifit.utilitarios.mostrarBase64
import com.unifit.unifit.utilitarios.voltarParaLogin
import java.time.LocalDate

class PerfilFragment : Fragment() {

    private val seletorImagem = SeletorImagem(this)
    private var usuario: Usuario? = null

    private lateinit var imgPerfil: ImageView
    private lateinit var viewEscurecerImgPerfil: View
    private lateinit var btnEditImgPerfil: ImageButton
    private lateinit var statusPlano: TextView
    private lateinit var nomePlano: TextView
    private lateinit var vencimentoPlano: TextView
    private lateinit var btnRealizarPagamento: Button
    private lateinit var nomeBanner: TextView
    private lateinit var nomeInfo: TextView
    private lateinit var emailInfo: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_perfil, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mainActivity = requireActivity() as MainActivity

        nomeBanner = view.findViewById(R.id.nome_banner_user_perfil)
        nomeInfo = view.findViewById(R.id.nome_user_perfil)
        emailInfo = view.findViewById(R.id.email_user_perfil)
        nomePlano = view.findViewById(R.id.textView52)
        statusPlano = view.findViewById(R.id.text_status_plano)
        vencimentoPlano = view.findViewById(R.id.textView54)
        btnRealizarPagamento = view.findViewById(R.id.btn_ir_pagamento)

        imgPerfil = view.findViewById(R.id.img_perfil)
        viewEscurecerImgPerfil = view.findViewById(R.id.view_escurecer_img_perfil)
        btnEditImgPerfil = view.findViewById(R.id.btn_edit_img_perfil)
        imgPerfil.setOnClickListener {
            viewEscurecerImgPerfil.visibility = View.VISIBLE
            btnEditImgPerfil.visibility = View.VISIBLE
        }
        btnEditImgPerfil.setOnClickListener {
            viewEscurecerImgPerfil.visibility = View.GONE
            btnEditImgPerfil.visibility = View.GONE
            abrirEditarFotoPerfil(seletorImagem, usuario?.fotoPerfil) { base64 ->
                usuario = usuario?.copy(fotoPerfil = base64)
                imgPerfil.mostrarBase64(base64)
            }
        }

        view.findViewById<Button>(R.id.btn_edit_infos_perfil).setOnClickListener {
            mainActivity.toggleContentMainFrameFullScreen(EditarInfosPerfilFragment())
        }
        view.findViewById<Button>(R.id.btn_contrato).setOnClickListener {
            mainActivity.toggleContentMainFrameFullScreen(ContratoFragment())
        }
        view.findViewById<Button>(R.id.btn_sair_conta).setOnClickListener { confirmarSair() }
        view.findViewById<Button>(R.id.btn_excluir_conta).setOnClickListener { confirmarExcluir() }

        btnRealizarPagamento.setOnClickListener {
            mainActivity.toggleContentMainFrameFullScreen(PagementoFragment())
        }

        parentFragmentManager.setFragmentResultListener(Resultados.PERFIL_ALTERADO, viewLifecycleOwner) { _, _ ->
            carregarPerfil()
            carregarPlano()
        }

        carregarPerfil()
        carregarPlano()
    }

    private fun carregarPerfil() {
        lancar {
            val u = Supabase.Usuarios.meuPerfil()
            usuario = u
            nomeBanner.text = u.nome
            nomeInfo.text = u.nome
            emailInfo.text = u.email
            imgPerfil.mostrarBase64(u.fotoPerfil)
        }
    }

    private fun carregarPlano() {
        lancar {
            mostrarPlano(Supabase.Planos.meuPlano())
        }
    }

    private fun mostrarPlano(plano: Plano?) {
        val ctx = requireContext()
        nomePlano.text = plano?.nomePlano ?: "Sem plano"
        vencimentoPlano.text = Tempo.dataBr(plano?.dataVencimento)

        val vencido = plano?.dataVencimento?.let { runCatching { LocalDate.parse(it).isBefore(LocalDate.now()) }.getOrNull() } ?: false
        val ativo = plano?.status == true && !vencido

        if (ativo) {
            statusPlano.text = "Ativo"
            statusPlano.setTextColor(ContextCompat.getColorStateList(ctx, R.color.unifor))
            btnRealizarPagamento.isEnabled = false
        } else {
            statusPlano.text = if (vencido || plano?.dataVencimento != null) "Vencido" else "Inativo"
            statusPlano.setTextColor(ContextCompat.getColorStateList(ctx, R.color.red))
            btnRealizarPagamento.isEnabled = true
        }
    }

    private fun confirmarSair() {
        abrirPopupConfirmacao(
            requireContext(),
            titulo = "Tem certeza que seja sair da sua conta?",
            descricao = "Se você sair terá que realiazar o login novamente",
            textoConfirmar = "Sair da conta",
            estilizar = { confirmar, voltar ->
                val ctx = confirmar.context
                confirmar.setTextColor(ContextCompat.getColorStateList(ctx, R.color.unifor))
                confirmar.background = null
                voltar.background = ContextCompat.getDrawable(ctx, R.drawable.bg_arredondado)
                voltar.setTextColor(ContextCompat.getColorStateList(ctx, R.color.white))
            }
        ) {
            lancar {
                Supabase.Autenticacao.sair()
                requireActivity().voltarParaLogin()
            }
        }
    }

    private fun confirmarExcluir() {
        abrirPopupConfirmacao(
            requireContext(),
            titulo = "TEM CERTEZA QUE DESEJA EXCLUIR SUA CONTA",
            descricao = "Você realmente deseja excluir sua conta? Esta ação é irreversível!",
            textoConfirmar = "Excluir conta",
            estilizar = { confirmar, voltar ->
                val ctx = confirmar.context
                confirmar.setTextColor(ContextCompat.getColorStateList(ctx, R.color.red))
                confirmar.background = null
                confirmar.foreground = ContextCompat.getDrawable(ctx, R.drawable.bg_borda_arredondada_red)
                voltar.background = ContextCompat.getDrawable(ctx, R.drawable.bg_arredondado)
                voltar.setTextColor(ContextCompat.getColorStateList(ctx, R.color.white))
            }
        ) {
            (requireActivity() as MainActivity).toggleContentMainFrameFullScreen(ExcluirContaFragment())
        }
    }
}
