package com.unifit.unifit.admin.notificacoes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import com.unifit.unifit.MainAdminActivity
import com.unifit.unifit.R
import com.unifit.unifit.admin.home.HomeAdminFragment
import com.unifit.unifit.utilitarios.TelaNotificacoes

class NotificacoesAdminFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_notificacoes_admin, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ImageButton>(R.id.btn_back_to_home_notifications_admin).setOnClickListener {
            (requireActivity() as MainAdminActivity).toggleContentMainFrame(HomeAdminFragment())
        }

        TelaNotificacoes(
            fragment = this,
            view = view,
            idBusca = R.id.search_input_admin,
            idBtnBusca = R.id.btn_search_notification_admin,
            idBtnMenu = R.id.btn_menu_filter_notification_admin,
            idBtnTodas = R.id.btn_toggle_todas_notifications_admin,
            idBtnNaoLidas = R.id.btn_toggle_nlidas_notifications_admin,
            idLista = R.id.rv_notifications_admin,
            aoAbrirChat = { aluno ->
                (requireActivity() as MainAdminActivity).toggleContentMainFrameFullScreen(
                    com.unifit.unifit.admin.alunos.ChatAdminFragment.newInstance(aluno, "")
                )
            }
        )
    }
}
