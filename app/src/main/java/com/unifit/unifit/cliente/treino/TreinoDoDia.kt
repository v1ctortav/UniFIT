package com.unifit.unifit.cliente.treino

import android.view.View
import android.widget.Button
import android.widget.TextView
import com.unifit.unifit.R
import com.unifit.unifit.server.Supabase
import com.unifit.unifit.server.TreinoAluno
import com.unifit.unifit.server.TreinoCompleto

enum class OrigemTreino { BIBLIOTECA, ALUNO }

suspend fun carregarTreino(origem: OrigemTreino, id: Long): TreinoCompleto = when (origem) {
    OrigemTreino.BIBLIOTECA -> Supabase.Treinos.buscarCompleto(id)
    OrigemTreino.ALUNO -> Supabase.TreinosAluno.buscarCompleto(id)
}

fun preencherTreinoDoDia(container: View, treino: TreinoAluno, aoIniciar: (TreinoAluno) -> Unit) {
    val categoria = container.findViewById<TextView>(R.id.treino_category_text) ?: return
    val nome = container.findViewById<TextView>(R.id.treino_name_text_card)
    val descricao = container.findViewById<TextView>(R.id.treino_personal_name_text_card)
    val btn = container.findViewById<Button>(R.id.btn_go_to_treino_execute)

    preencherCardTreino(categoria, nome, descricao, treino.comoTreino())
    btn.isEnabled = true
    btn.setOnClickListener { aoIniciar(treino) }
}
