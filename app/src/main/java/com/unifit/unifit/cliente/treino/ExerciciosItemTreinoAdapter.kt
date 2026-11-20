package com.unifit.unifit.cliente.treino

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.unifit.unifit.R
import com.unifit.unifit.server.Exercicio
import com.unifit.unifit.server.ItemTreino
import com.unifit.unifit.utilitarios.mostrarIconeExercicio

class ExerciciosItemTreinoAdapter : RecyclerView.Adapter<ExerciciosItemTreinoAdapter.ExercicioViewHolder>() {

    private var listaExerciciosItens = listOf<Pair<ItemTreino, Exercicio?>>()

    fun updateData(list: List<Pair<ItemTreino, Exercicio?>>) {
        listaExerciciosItens = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExercicioViewHolder =
        ExercicioViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_lista_exercicios_treino_execucao, parent, false)
        )

    override fun onBindViewHolder(holder: ExercicioViewHolder, position: Int) = holder.bind(listaExerciciosItens[position])

    override fun getItemCount(): Int = listaExerciciosItens.size

    inner class ExercicioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imagem: ImageView = itemView.findViewById(R.id.imageView12)
        private val nome: TextView = itemView.findViewById(R.id.textView73)
        private val series: TextView = itemView.findViewById(R.id.textView74)

        fun bind(par: Pair<ItemTreino, Exercicio?>) {
            val (item, exercicio) = par
            nome.text = exercicio?.nome ?: "Exercício removido"
            series.text = "${item.series} Séries • ${item.repeticoes} Rep."
            imagem.mostrarIconeExercicio(exercicio)
        }
    }
}
