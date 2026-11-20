package com.unifit.unifit.cliente.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import com.unifit.unifit.MainActivity
import com.unifit.unifit.R
import com.unifit.unifit.cliente.home.HomeFragment
import com.unifit.unifit.utilitarios.TelaNotificacoes

class NotificationsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_notifications, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ImageButton>(R.id.btn_back_to_home_notifications).setOnClickListener {
            (requireActivity() as MainActivity).toggleContentMainFrame(HomeFragment())
        }

        TelaNotificacoes(
            fragment = this,
            view = view,
            idBusca = R.id.search_input,
            idBtnBusca = R.id.btn_search_notification,
            idBtnMenu = R.id.btn_menu_filter_notification,
            idBtnTodas = R.id.btn_toggle_todas_notifications,
            idBtnNaoLidas = R.id.btn_toggle_nlidas_notifications,
            idLista = R.id.rv_notifications,
            aoAbrirChat = { (requireActivity() as MainActivity).toggleContentMainFrameFullScreen(com.unifit.unifit.cliente.chat.ChatFragment()) }
        )
    }
}
