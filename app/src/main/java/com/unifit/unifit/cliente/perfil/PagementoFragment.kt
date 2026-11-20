package com.unifit.unifit.cliente.perfil

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.unifit.unifit.MainActivity
import com.unifit.unifit.R
import com.unifit.unifit.server.Supabase
import com.unifit.unifit.utilitarios.adicionarCarregando
import com.unifit.unifit.utilitarios.adicionarEstadoVazio
import com.unifit.unifit.utilitarios.br
import com.unifit.unifit.utilitarios.lancar

class PagementoFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_pagemento, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mainActivity = requireActivity() as MainActivity

        view.findViewById<ImageButton>(R.id.btn_back_pagamento_perfil).setOnClickListener {
            mainActivity.closeFullScreenFrame()
        }

        val btnPix = view.findViewById<Button>(R.id.btn_pagar_pix)
        btnPix.isEnabled = false
        btnPix.alpha = 0.5f
        btnPix.setOnClickListener {
            mainActivity.toggleContentMainFrameFullScreen(PagarPixFragment())
        }

        val area = view.findViewById<LinearLayout>(R.id.lista_planos_pagamento)
        area.removeAllViews()
        area.adicionarCarregando()
        lancar(aoFalhar = { msg ->
            area.removeAllViews()
            area.adicionarEstadoVazio(msg)
        }) {
            val nome = Supabase.Planos.meuPlano()?.nomePlano
            val oferta = Supabase.Pagamentos.planosDisponiveis().firstOrNull { it.nome == nome }
            area.removeAllViews()
            if (oferta == null) {
                area.adicionarEstadoVazio("O ${nome ?: "seu plano"} não pode ser pago pelo app. Fale com a academia.")
                return@lancar
            }
            val item = LayoutInflater.from(area.context).inflate(R.layout.item_plano_pagamento, area, false)
            item.findViewById<TextView>(R.id.nome_plano_pagamento).text = oferta.nome
            item.findViewById<TextView>(R.id.descricao_plano_pagamento).text = oferta.descricao
            item.findViewById<TextView>(R.id.valor_plano_pagamento).text = "R$ ${oferta.valor.br(2)}\n/mês"
            item.isClickable = false
            area.addView(item)
            btnPix.isEnabled = true
            btnPix.alpha = 1f
        }
    }
}
