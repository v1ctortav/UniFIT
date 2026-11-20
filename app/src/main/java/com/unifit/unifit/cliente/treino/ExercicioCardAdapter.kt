package com.unifit.unifit.cliente.treino

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

class ExercicioCardAdapter(
    private val click: (Exercicio) -> Unit
) : RecyclerView.Adapter<ExercicioCardAdapter.ExercicioViewHolder>() {

    private var listaCardsExercicios = listOf<Exercicio>()

    fun updateData(lista: List<Exercicio>) {
        listaCardsExercicios = lista
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExercicioViewHolder =
        ExercicioViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_lista_exercicios_treinos, parent, false))

    override fun onBindViewHolder(holder: ExercicioViewHolder, position: Int) = holder.bind(listaCardsExercicios[position])

    override fun getItemCount(): Int = listaCardsExercicios.size

    inner class ExercicioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textExercicioNome: TextView = itemView.findViewById(R.id.text_nome_exercicio_treinos_item_lista)
        private val imagem: ImageView = itemView.findViewById(R.id.imageView25)
        private val btnVer: Button = itemView.findViewById(R.id.btn_ver_exercicio)

        fun bind(exercicio: Exercicio) {
            textExercicioNome.text = exercicio.nome
            imagem.mostrarIconeExercicio(exercicio)
            btnVer.setOnClickListener { click(exercicio) }
        }
    }
}
