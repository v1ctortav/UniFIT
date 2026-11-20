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

class ExerciciosCardBibliotecaAdapter(
    private val click: (Exercicio) -> Unit
) : RecyclerView.Adapter<ExerciciosCardBibliotecaAdapter.ExercicioViewHolder>() {

    private var listaCardsExercicios = listOf<Exercicio>()

    fun updateData(lista: List<Exercicio>) {
        listaCardsExercicios = lista
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExercicioViewHolder =
        ExercicioViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.card_exercicio, parent, false))

    override fun onBindViewHolder(holder: ExercicioViewHolder, position: Int) = holder.bind(listaCardsExercicios[position])

    override fun getItemCount(): Int = listaCardsExercicios.size

    inner class ExercicioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textExercicioNome: TextView = itemView.findViewById(R.id.text_nome_card_exercicio)
        private val imagem: ImageView = itemView.findViewById(R.id.imageView27)
        private val btnVer: Button = itemView.findViewById(R.id.btn_ver_exercicio_biblioteca)

        fun bind(exercicio: Exercicio) {
            textExercicioNome.text = exercicio.nome
            imagem.mostrarIconeExercicio(exercicio)
            btnVer.setOnClickListener { click(exercicio) }
        }
    }
}
