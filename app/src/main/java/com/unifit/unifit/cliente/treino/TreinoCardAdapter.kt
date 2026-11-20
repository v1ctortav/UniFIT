package com.unifit.unifit.cliente.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.unifit.unifit.R
import com.unifit.unifit.server.Treino
import com.unifit.unifit.utilitarios.mostrarCapaTreino

class TreinoCardAdapter(
    private val click: (Treino) -> Unit
) : Adapter<TreinoCardAdapter.TreinoViewHolder>() {

    private var listaCardsTreinos = listOf<Treino>()

    fun updateData(lista: List<Treino>) {
        listaCardsTreinos = lista
        notifyDataSetChanged()
    }

    inner class TreinoViewHolder(itemView: View) : ViewHolder(itemView) {
        private val textTreinoCardCategory: TextView = itemView.findViewById(R.id.text_category_card2)
        private val textTreinoCardName: TextView = itemView.findViewById(R.id.text_name_card2)
        private val textTreinoCardDesc: TextView = itemView.findViewById(R.id.text_desc_card2)
        private val card: ConstraintLayout = itemView.findViewById(R.id.card2_treino_item)
        private val capa: android.widget.ImageView = itemView.findViewById(R.id.imageView4)

        fun bind(treino: Treino) {
            capa.mostrarCapaTreino(treino)
            textTreinoCardCategory.text = treino.categoria
            textTreinoCardName.text = treino.nome
            textTreinoCardDesc.text = treino.descricao.ifBlank { "${treino.exercicios.size} exercícios" }
            card.setOnClickListener { click(treino) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TreinoViewHolder =
        TreinoViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.card2_treino_item, parent, false))

    override fun onBindViewHolder(holder: TreinoViewHolder, position: Int) = holder.bind(listaCardsTreinos[position])

    override fun getItemCount(): Int = listaCardsTreinos.size
}
