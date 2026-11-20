package com.unifit.unifit.admin.perfil

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
import com.unifit.unifit.MainAdminActivity
import com.unifit.unifit.R
import com.unifit.unifit.server.Supabase
import com.unifit.unifit.server.Usuario
import com.unifit.unifit.utilitarios.Resultados
import com.unifit.unifit.utilitarios.SeletorImagem
import com.unifit.unifit.utilitarios.abrirEditarFotoPerfil
import com.unifit.unifit.utilitarios.abrirPopupConfirmacao
import com.unifit.unifit.utilitarios.lancar
import com.unifit.unifit.utilitarios.mostrarBase64
import com.unifit.unifit.utilitarios.voltarParaLogin

class PerfilAdminFragment : Fragment() {

    private val seletorImagem = SeletorImagem(this)
    private var usuario: Usuario? = null

    private lateinit var imgPerfil: ImageView
    private lateinit var viewEscurecerImgPerfil: View
    private lateinit var btnEditImgPerfil: ImageButton
    private lateinit var nomeBanner: TextView
    private lateinit var nomeInfo: TextView
    private lateinit var emailInfo: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_perfil_admin, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mainActivity = requireActivity() as MainAdminActivity

        nomeBanner = view.findViewById(R.id.textView134)
        nomeInfo = view.findViewById(R.id.textView139)
        emailInfo = view.findViewById(R.id.textView142)

        imgPerfil = view.findViewById(R.id.img_perfil_admin)
        viewEscurecerImgPerfil = view.findViewById(R.id.view_escurecer_img_perfil_admin)
        btnEditImgPerfil = view.findViewById(R.id.btn_edit_img_perfil_admin)
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

        view.findViewById<Button>(R.id.btn_edit_infos_perfil_admin).setOnClickListener {
            mainActivity.toggleContentMainFrameFullScreen(EditarInfosPerfilAdminFragment())
        }
        view.findViewById<Button>(R.id.btn_sair_conta_admin).setOnClickListener { confirmarSair() }

        parentFragmentManager.setFragmentResultListener(Resultados.PERFIL_ALTERADO, viewLifecycleOwner) { _, _ ->
            carregarPerfil()
        }
        carregarPerfil()
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
}
