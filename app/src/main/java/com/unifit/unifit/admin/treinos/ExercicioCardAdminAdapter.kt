package com.unifit.unifit.admin.treinos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.unifit.unifit.R
import com.unifit.unifit.server.Exercicio
import com.unifit.unifit.utilitarios.mostrarIconeExercicio

class ExercicioCardAdminAdapter(
    val tipo: String,
    private val click: (Exercicio) -> Unit
) : RecyclerView.Adapter<ExercicioCardAdminAdapter.ExercicioViewHolder>() {

    private var listaCardsExercicios = listOf<Exercicio>()

    fun updateData(lista: List<Exercicio>) {
        listaCardsExercicios = lista
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExercicioViewHolder {
        val layout = if (tipo == "Card") R.layout.card_exercicio else R.layout.item_lista_exercicios_treinos
        return ExercicioViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false))
    }

    override fun onBindViewHolder(holder: ExercicioViewHolder, position: Int) = holder.bind(listaCardsExercicios[position])

    override fun getItemCount(): Int = listaCardsExercicios.size

    inner class ExercicioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textExercicioNome: TextView = itemView.findViewById(
            if (tipo == "Card") R.id.text_nome_card_exercicio else R.id.text_nome_exercicio_treinos_item_lista
        )
        private val btnVer: Button = itemView.findViewById(
            if (tipo == "Card") R.id.btn_ver_exercicio_biblioteca else R.id.btn_ver_exercicio
        )
        private val imagem: ImageView = itemView.findViewById(
            if (tipo == "Card") R.id.imageView27 else R.id.imageView25
        )

        fun bind(exercicio: Exercicio) {
            textExercicioNome.text = exercicio.nome
            imagem.mostrarIconeExercicio(exercicio)
            btnVer.setOnClickListener { click(exercicio) }
        }
    }
}
