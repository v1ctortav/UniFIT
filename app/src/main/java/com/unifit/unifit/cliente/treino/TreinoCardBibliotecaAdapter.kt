package com.unifit.unifit.cliente.treino

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.unifit.unifit.R
import com.unifit.unifit.server.Treino
import com.unifit.unifit.utilitarios.mostrarCapaTreino

class TreinoCardBibliotecaAdapter(
    private val click: (Treino) -> Unit
) : Adapter<TreinoCardBibliotecaAdapter.TreinoViewHolder>() {

    private var listaCardsTreinos = listOf<Treino>()

    fun updateData(lista: List<Treino>) {
        listaCardsTreinos = lista
        notifyDataSetChanged()
    }

    inner class TreinoViewHolder(itemView: View) : ViewHolder(itemView) {
        private val textTreinoCardCategory: TextView = itemView.findViewById(R.id.treino_category_text)
        private val textTreinoCardName: TextView = itemView.findViewById(R.id.treino_name_text_card)
        private val textTreinoCardDesc: TextView = itemView.findViewById(R.id.treino_personal_name_text_card)
        private val btnGoTreino: Button = itemView.findViewById(R.id.btn_go_to_treino_execute)

        fun bind(treino: Treino) {
            preencherCardTreino(textTreinoCardCategory, textTreinoCardName, textTreinoCardDesc, treino)
            btnGoTreino.setOnClickListener { click(treino) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TreinoViewHolder =
        TreinoViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.card_treino_item, parent, false))

    override fun onBindViewHolder(holder: TreinoViewHolder, position: Int) = holder.bind(listaCardsTreinos[position])

    override fun getItemCount(): Int = listaCardsTreinos.size
}

fun preencherCardTreino(categoria: TextView, nome: TextView, descricao: TextView, treino: Treino) {
    (categoria.parent as? android.view.View)
        ?.findViewById<android.widget.ImageView>(R.id.bg_treino_banner_card)
        ?.mostrarCapaTreino(treino)
    categoria.text = treino.categoria
    nome.text = treino.nome
    descricao.text = treino.descricao.ifBlank { "${treino.exercicios.size} exercícios" }
}
