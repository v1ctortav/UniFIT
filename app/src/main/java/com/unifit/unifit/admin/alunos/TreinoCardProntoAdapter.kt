package com.unifit.unifit.admin.alunos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.unifit.unifit.R
import com.unifit.unifit.cliente.treino.preencherCardTreino
import com.unifit.unifit.server.Treino

class TreinoCardProntoAdapter(
    private val click: (Treino) -> Unit
) : Adapter<TreinoCardProntoAdapter.TreinoViewHolder>() {

    private var listaCardsTreinos = listOf<Treino>()

    fun updateData(lista: List<Treino>) {
        listaCardsTreinos = lista
        notifyDataSetChanged()
    }

    inner class TreinoViewHolder(itemView: View) : ViewHolder(itemView) {
        private val textTreinoCardCategory: TextView = itemView.findViewById(R.id.treino_category_text)
        private val textTreinoCardName: TextView = itemView.findViewById(R.id.treino_name_text_card)
        private val textTreinoCardDesc: TextView = itemView.findViewById(R.id.treino_personal_name_text_card)
        private val btnEscolherTreino: Button = itemView.findViewById(R.id.btn_go_to_treino_execute)

        fun bind(treino: Treino) {
            preencherCardTreino(textTreinoCardCategory, textTreinoCardName, textTreinoCardDesc, treino)
            btnEscolherTreino.text = "Escolher treino"
            btnEscolherTreino.setOnClickListener { click(treino) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TreinoViewHolder =
        TreinoViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.card_treino_item, parent, false))

    override fun onBindViewHolder(holder: TreinoViewHolder, position: Int) = holder.bind(listaCardsTreinos[position])

    override fun getItemCount(): Int = listaCardsTreinos.size
}
