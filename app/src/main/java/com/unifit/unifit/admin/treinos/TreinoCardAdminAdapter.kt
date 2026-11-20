package com.unifit.unifit.admin.treinos

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
import com.unifit.unifit.utilitarios.mostrarCapaTreino

class TreinoCardAdminAdapter(
    val tipo: String,
    private val click: (Treino) -> Unit
) : Adapter<TreinoCardAdminAdapter.TreinoViewHolder>() {

    private var listaCardsTreinos = listOf<Treino>()

    fun updateData(lista: List<Treino>) {
        listaCardsTreinos = lista
        notifyDataSetChanged()
    }

    inner class TreinoViewHolder(itemView: View) : ViewHolder(itemView) {
        fun bind(treino: Treino) {
            if (tipo == "Card2") {
                preencherCardTreino(
                    itemView.findViewById(R.id.treino_category_text),
                    itemView.findViewById(R.id.treino_name_text_card),
                    itemView.findViewById(R.id.treino_personal_name_text_card),
                    treino
                )
                val btnVer = itemView.findViewById<Button>(R.id.btn_go_to_treino_execute)
                btnVer.text = "Editar treino"
                btnVer.setOnClickListener { click(treino) }
            } else {
                itemView.findViewById<android.widget.ImageView>(R.id.imageView4).mostrarCapaTreino(treino)
                itemView.findViewById<TextView>(R.id.text_category_card2).text = treino.categoria
                itemView.findViewById<TextView>(R.id.text_name_card2).text = treino.nome
                itemView.findViewById<TextView>(R.id.text_desc_card2).text =
                    treino.descricao.ifBlank { "${treino.exercicios.size} exercícios" }
                itemView.findViewById<View>(R.id.card2_treino_item).setOnClickListener { click(treino) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TreinoViewHolder {
        val layout = if (tipo == "Card2") R.layout.card_treino_item else R.layout.card2_treino_item
        return TreinoViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false))
    }

    override fun onBindViewHolder(holder: TreinoViewHolder, position: Int) = holder.bind(listaCardsTreinos[position])

    override fun getItemCount(): Int = listaCardsTreinos.size
}
