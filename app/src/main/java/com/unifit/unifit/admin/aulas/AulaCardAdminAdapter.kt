package com.unifit.unifit.admin.aulas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.unifit.unifit.R
import com.unifit.unifit.server.AulaDetalhada

class AulaCardAdminAdapter(
    private val aoEditar: (AulaDetalhada) -> Unit,
    private val aoDetalhes: (AulaDetalhada) -> Unit,
    private val aoExcluir: (AulaDetalhada) -> Unit
) : RecyclerView.Adapter<AulaCardAdminAdapter.AulaViewHolder>() {

    private var listCardsAulas = listOf<AulaDetalhada>()

    fun updateData(list: List<AulaDetalhada>) {
        listCardsAulas = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AulaViewHolder =
        AulaViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.card_aula_admin, parent, false))

    override fun onBindViewHolder(holder: AulaViewHolder, position: Int) = holder.bind(listCardsAulas[position])

    override fun getItemCount(): Int = listCardsAulas.size

    inner class AulaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textNome = itemView.findViewById<TextView>(R.id.textView148)
        private val textHorario = itemView.findViewById<TextView>(R.id.textView151)
        private val textVagas = itemView.findViewById<TextView>(R.id.textView152)
        private val btnEditarAula = itemView.findViewById<Button>(R.id.btn_edit_aula_card)
        private val btnDetalhesAula = itemView.findViewById<Button>(R.id.btn_detalhes_aula_card)
        private val btnExcluirAula = itemView.findViewById<ImageButton>(R.id.btn_excluir_aula)

        fun bind(aula: AulaDetalhada) {
            textNome.text = aula.nome
            textHorario.text = "${aula.horarioInicio} - ${aula.horarioFim}"
            textVagas.text = "${aula.vagasOcupadas}/${aula.vagas}"
            btnEditarAula.setOnClickListener { aoEditar(aula) }
            btnDetalhesAula.setOnClickListener { aoDetalhes(aula) }
            btnExcluirAula.setOnClickListener { aoExcluir(aula) }
        }
    }
}
