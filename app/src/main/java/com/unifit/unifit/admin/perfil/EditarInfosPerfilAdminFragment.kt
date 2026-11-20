package com.unifit.unifit.admin.perfil

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.unifit.unifit.MainAdminActivity
import com.unifit.unifit.R
import com.unifit.unifit.auth.AuthActivity
import com.unifit.unifit.utilitarios.EditarInfosPerfil
import com.unifit.unifit.utilitarios.Resultados

class EditarInfosPerfilAdminFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_editar_infos_perfil_admin, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mainActivity = requireActivity() as MainAdminActivity

        view.findViewById<ImageButton>(R.id.btn_back_perfil_edit_infos_admin).setOnClickListener {
            mainActivity.closeFullScreenFrame()
        }
        view.findViewById<TextView>(R.id.textView146).setOnClickListener {
            startActivity(Intent(requireContext(), AuthActivity::class.java).putExtra("recuperar_senha_perfil", true))
        }

        EditarInfosPerfil(
            fragment = this,
            campoNome = view.findViewById(R.id.textInputLayout13),
            campoEmail = view.findViewById(R.id.textInputLayout14),
            campoSenhaAtual = view.findViewById(R.id.textInputLayout15),
            campoNovaSenha = view.findViewById(R.id.textInputLayout16),
            campoConfirmacao = view.findViewById(R.id.textInputLayout17),
            btnSalvar = view.findViewById(R.id.button4)
        ) {
            parentFragmentManager.setFragmentResult(Resultados.PERFIL_ALTERADO, Bundle())
            mainActivity.closeFullScreenFrame()
        }
    }
}
