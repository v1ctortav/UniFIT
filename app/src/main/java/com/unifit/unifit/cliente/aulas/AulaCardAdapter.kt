package com.unifit.unifit.cliente.aulas

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.unifit.unifit.R
import com.unifit.unifit.server.AulaDetalhada
import com.unifit.unifit.server.Tempo
import com.unifit.unifit.utilitarios.abrirPopupConfirmacao

class AulaCardAdapter(
    private val aoAlternarMatricula: (AulaDetalhada) -> Unit
) : RecyclerView.Adapter<AulaCardAdapter.AulaViewHolder>() {

    private var listCardsAulas = mutableListOf<AulaDetalhada>()

    fun updateData(list: List<AulaDetalhada>) {
        listCardsAulas = list.toMutableList()
        notifyDataSetChanged()
    }

    fun atualizar(aula: AulaDetalhada) {
        val index = listCardsAulas.indexOfFirst { it.id == aula.id }
        if (index == -1) return
        listCardsAulas[index] = aula
        notifyItemChanged(index)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AulaViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.card_aula, parent, false)
        return AulaViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: AulaViewHolder, position: Int) = holder.bind(listCardsAulas[position])

    override fun getItemCount(): Int = listCardsAulas.size

    inner class AulaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textName = itemView.findViewById<TextView>(R.id.text_name_aula_card)
        private val textTimer = itemView.findViewById<TextView>(R.id.text_timer_aula_card)
        private val textNameInstrutor = itemView.findViewById<TextView>(R.id.text_name_instrutor_aula_card)
        private val textVagas = itemView.findViewById<TextView>(R.id.text_count_positions_aula_card)
        private val btnParticipar = itemView.findViewById<MaterialButton>(R.id.btn_register_aula_card)

        fun bind(aula: AulaDetalhada) {
            textName.text = aula.nome
            textTimer.text = "${aula.horarioInicio} - ${aula.horarioFim}"
            textNameInstrutor.text = aula.instrutor
            textVagas.text = "${aula.vagasOcupadas}/${aula.vagas}"

            configurarBotao(aula)
            btnParticipar.setOnClickListener { abrirPopup(aula, itemView.context) }
        }

        private fun configurarBotao(aula: AulaDetalhada) {
            val ctx = itemView.context
            when (aula.status) {
                StatusAula.DISPONIVEL -> {
                    btnParticipar.text = "Participar"
                    btnParticipar.isEnabled = true
                    btnParticipar.backgroundTintList = ContextCompat.getColorStateList(ctx, R.color.unifor)
                }
                StatusAula.INDISPONIVEL -> {
                    btnParticipar.text = "Indisponível"
                    btnParticipar.isEnabled = false
                    btnParticipar.backgroundTintList = ContextCompat.getColorStateList(ctx, R.color.gray)
                }
                StatusAula.MATRICULADO -> {
                    btnParticipar.text = "Cancelar"
                    btnParticipar.isEnabled = true
                    btnParticipar.backgroundTintList = ContextCompat.getColorStateList(ctx, R.color.red)
                }
            }
        }
    }

    private fun abrirPopup(aula: AulaDetalhada, context: Context) {
        val quando = "${Tempo.dataBr(aula.data)}, ${aula.horarioInicio} - ${aula.horarioFim}"
        if (aula.inscrito) {
            abrirPopupConfirmacao(
                context,
                titulo = "Cancelar inscrição?",
                descricao = "Tem certeza que deseja cancelar sua inscrição na aula ${aula.nome} ($quando)?",
                textoConfirmar = "Cancelar",
                icone = R.drawable.ic_alert
            ) { aoAlternarMatricula(aula) }
        } else {
            abrirPopupConfirmacao(
                context,
                titulo = "Participar da aula?",
                descricao = "Deseja confirmar sua inscrição na aula ${aula.nome} ($quando)?",
                textoConfirmar = "Inscrever-se",
                icone = R.drawable.ic_aula
            ) { aoAlternarMatricula(aula) }
        }
    }
}
