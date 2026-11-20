package com.unifit.unifit.cliente.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import com.unifit.unifit.MainActivity
import com.unifit.unifit.R
import com.unifit.unifit.server.Supabase
import com.unifit.unifit.utilitarios.TelaChat

class ChatFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_chat, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ImageButton>(R.id.btn_back_ajuda_home).setOnClickListener {
            (requireActivity() as MainActivity).closeFullScreenFrame()
        }

        val eu = Supabase.idUsuarioLogado() ?: return
        TelaChat(
            fragment = this,
            alunoId = eu,
            souAdmin = false,
            lista = view.findViewById(R.id.rv_mensagens_chat),
            campo = view.findViewById(R.id.textInputLayout12),
            btnEnviar = view.findViewById(R.id.imageView28)
        )
    }
}
