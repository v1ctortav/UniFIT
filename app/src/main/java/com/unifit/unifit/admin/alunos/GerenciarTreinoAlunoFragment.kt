package com.unifit.unifit.admin.alunos

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputLayout
import com.unifit.unifit.MainAdminActivity
import com.unifit.unifit.R
import com.unifit.unifit.server.DiaSemana
import com.unifit.unifit.server.Exercicio
import com.unifit.unifit.server.Notificacao
import com.unifit.unifit.server.Supabase
import com.unifit.unifit.server.Treino
import com.unifit.unifit.server.TreinoAluno
import com.unifit.unifit.utilitarios.Categorias
import com.unifit.unifit.utilitarios.FormularioExercicios
import com.unifit.unifit.utilitarios.abrirPopupConfirmacao
import com.unifit.unifit.utilitarios.adicionarCarregando
import com.unifit.unifit.utilitarios.adicionarEstadoVazio
import com.unifit.unifit.utilitarios.configurarDiasAgenda
import com.unifit.unifit.utilitarios.diasEscolhidos
import com.unifit.unifit.utilitarios.lancar
import com.unifit.unifit.utilitarios.mostrarBase64
import com.unifit.unifit.utilitarios.mostrarExpandido
import com.unifit.unifit.utilitarios.texto
import com.unifit.unifit.utilitarios.toast

class GerenciarTreinoAlunoFragment : Fragment() {

    private data class Grupo(val dias: List<Int>, val linhas: List<TreinoAluno>) {
        val modelo: TreinoAluno get() = linhas.first()
    }

    private val alunoId: String by lazy { requireArguments().getString(ARG_ALUNO_ID).orEmpty() }

    private var agenda: List<TreinoAluno> = emptyList()
    private var exercicios: List<Exercicio> = emptyList()

    private var diasPendentes: List<Int>? = null
    private var treinoEscolhido: Treino? = null
    private var editando: List<TreinoAluno> = emptyList()

    private lateinit var listaTreinosView: LinearLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_gerenciar_treino_aluno, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ImageButton>(R.id.btn_voltar_gerenciar_treino).setOnClickListener {
            (requireActivity() as MainAdminActivity).closeFullScreenFrame()
        }
        listaTreinosView = view.findViewById(R.id.lista_treinos_card_aluno_admin)
        listaTreinosView.removeAllViews()
        listaTreinosView.adicionarCarregando()

        view.findViewById<Button>(R.id.btn_adicionar_treino_aluno_admin).setOnClickListener {
            editando = emptyList()
            diasPendentes = null
            treinoEscolhido = null
            mostrarEscolherTipo()
        }

        parentFragmentManager.setFragmentResultListener("reabrir_treino_pronto", viewLifecycleOwner) { _, resultado ->
            val id = resultado.getLong("treino_id", -1L)
            if (id <= 0) {
                mostrarFormularioPronto()
                return@setFragmentResultListener
            }
            lancar {
                treinoEscolhido = Supabase.Treinos.buscar(id)
                if (diasPendentes == null) diasPendentes = treinoEscolhido?.diaSemana?.let { listOf(it) }
                mostrarFormularioPronto()
            }
        }

        val nomeAluno = view.findViewById<TextView>(R.id.textView183)
        val fotoAluno = view.findViewById<ImageView>(R.id.imageView34)
        lancar {
            val aluno = Supabase.Usuarios.buscar(alunoId)
            nomeAluno.text = aluno.nome
            fotoAluno.mostrarBase64(aluno.fotoPerfil)
        }

        lancar {
            exercicios = Supabase.Exercicios.listar(comImagem = false)
            carregarAgenda()
        }
    }

    private fun carregarAgenda() {
        lancar {
            agenda = Supabase.TreinosAluno.listarDoAluno(alunoId)
            mostrarAgenda()
        }
    }

    private fun grupos(): List<Grupo> =
        agenda.groupBy { listOf(it.nome, it.treinoId, it.categoria, it.descricao, it.exercicios) }
            .values
            .map { linhas -> Grupo(linhas.map { it.diaSemana }.sorted(), linhas.sortedBy { it.diaSemana }) }
            .sortedBy { it.dias.first() }

    private fun mostrarAgenda() {
        listaTreinosView.removeAllViews()
        if (agenda.isEmpty()) {
            listaTreinosView.adicionarEstadoVazio("Este aluno ainda não tem treinos na agenda.\nToque em \"Adicionar treino\".")
            return
        }
        val nomes = exercicios.associateBy { it.id }
        grupos().forEach { grupo ->
            val t = grupo.modelo
            val card = layoutInflater.inflate(R.layout.card_treino_aluno_admin, listaTreinosView, false)
            card.findViewById<TextView>(R.id.textView187).text = DiaSemana.juntar(grupo.dias)
            card.findViewById<TextView>(R.id.textView188).text = "Nome: ${t.nome}"
            card.findViewById<TextView>(R.id.textView189).text =
                if (t.exercicios.isEmpty()) "Nenhum exercício"
                else t.exercicios.mapIndexed { i, item ->
                    "${i + 1}. ${nomes[item.exercicioId]?.nome ?: "Exercício removido"} — ${item.series} séries de ${item.repeticoes} repetições"
                }.joinToString("\n")

            card.findViewById<ImageButton>(R.id.btn_editar_treino_aluno).setOnClickListener { editar(grupo) }
            card.findViewById<ImageButton>(R.id.btn_excluir_treino_aluno).setOnClickListener { confirmarExcluir(grupo) }
            listaTreinosView.addView(card)
        }
    }

    private fun editar(grupo: Grupo) {
        editando = grupo.linhas
        diasPendentes = grupo.dias
        val origem = grupo.modelo.treinoId
        if (origem != null) {
            lancar {
                treinoEscolhido = runCatching { Supabase.Treinos.buscar(origem) }.getOrNull()
                if (treinoEscolhido == null) mostrarFormularioPersonalizado(grupo)
                else mostrarFormularioPronto()
            }
        } else {
            treinoEscolhido = null
            mostrarFormularioPersonalizado(grupo)
        }
    }

    private fun confirmarExcluir(grupo: Grupo) {
        abrirPopupConfirmacao(
            requireContext(),
            titulo = "Excluir treino?",
            descricao = "Tem certeza que deseja excluir o treino de ${DiaSemana.juntar(grupo.dias).lowercase()} do aluno(a)?",
            textoConfirmar = "Excluir treino"
        ) {
            lancar {
                grupo.linhas.mapNotNull { it.id }.forEach { Supabase.TreinosAluno.excluir(it) }
                toast("Treino excluído da agenda")
                carregarAgenda()
            }
        }
    }

    private fun mostrarEscolherTipo() {
        val dialog = BottomSheetDialog(requireContext())
        val v = layoutInflater.inflate(R.layout.bottom_sheet_escolher_tipo_treino, null)
        dialog.setContentView(v)
        v.findViewById<ImageButton>(R.id.btn_fechar_escolher_tipo_treino_admin).setOnClickListener { dialog.dismiss() }
        v.findViewById<Button>(R.id.btn_treino_pronto).setOnClickListener {
            dialog.dismiss()
            mostrarFormularioPronto()
        }
        v.findViewById<Button>(R.id.btn_treino_personalizado).setOnClickListener {
            dialog.dismiss()
            mostrarFormularioPersonalizado(null)
        }
        dialog.mostrarExpandido()
    }

    private fun mostrarFormularioPronto() {
        val dialog = BottomSheetDialog(requireContext())
        val v = layoutInflater.inflate(R.layout.bottom_sheet_treino_pronto, null)
        dialog.setContentView(v)

        val spinnerDia = v.findViewById<Spinner>(R.id.spinner)
        spinnerDia.configurarDiasAgenda(diasPendentes)

        val icone = v.findViewById<ImageView>(R.id.imageView35)
        val texto = v.findViewById<TextView>(R.id.textView193)
        treinoEscolhido?.let { t ->
            icone.setImageResource(R.drawable.ic_check)
            icone.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.unifor))
            texto.text = "${t.nome}\n${t.categoria} • ${t.exercicios.size} exercícios"
            texto.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
        }

        v.findViewById<ImageButton>(R.id.btn_fechar_treino_pronto).setOnClickListener { dialog.dismiss() }

        v.findViewById<Button>(R.id.btn_escolher_treino_pronto).setOnClickListener {
            diasPendentes = spinnerDia.diasEscolhidos()
            dialog.dismiss()
            (requireActivity() as MainAdminActivity).toggleContentMainFrameFullScreen(EscolherTreinoProntoFragment())
        }

        v.findViewById<Button>(R.id.btn_salvar_treino_pronto).setOnClickListener {
            val dias = spinnerDia.diasEscolhidos()
            val t = treinoEscolhido
            when {
                dias == null -> toast("Selecione o dia da semana")
                t == null -> toast("Toque em \"Escolher treino\" para selecionar um treino pronto")
                t.exercicios.isEmpty() -> toast("Esse treino pronto ainda não tem exercícios")
                else -> salvar(
                    TreinoAluno(
                        userId = alunoId, diaSemana = dias.first(), treinoId = t.id,
                        nome = t.nome, descricao = t.descricao, categoria = t.categoria, exercicios = t.exercicios
                    ),
                    dias, dialog
                )
            }
        }

        dialog.mostrarExpandido()
    }

    private fun mostrarFormularioPersonalizado(atual: Grupo?) {
        val dialog = BottomSheetDialog(requireContext())
        val v = layoutInflater.inflate(R.layout.bottom_sheet_treino_personalizado, null)
        dialog.setContentView(v)

        val campoNome = v.findViewById<TextInputLayout>(R.id.textInputLayout)
        val spinnerCategoria = v.findViewById<Spinner>(R.id.spinner)
        val spinnerDia = v.findViewById<Spinner>(R.id.spinner_dias_semana_treino)
        val formulario = FormularioExercicios(v.findViewById(R.id.lista_exercicio_personalizado), exercicios)

        val modelo = atual?.modelo
        campoNome.texto = modelo?.nome.orEmpty()
        Categorias.configurarSpinner(spinnerCategoria, modelo?.categoria)
        spinnerDia.configurarDiasAgenda(atual?.dias ?: diasPendentes)
        if (modelo != null) modelo.exercicios.forEach { formulario.adicionar(it) } else formulario.adicionar()

        v.findViewById<ImageButton>(R.id.btn_fechar_treino_personalizado).setOnClickListener { dialog.dismiss() }
        v.findViewById<Button>(R.id.btn_adicionar_exercicio_treino).setOnClickListener {
            if (exercicios.isEmpty()) toast("Nenhum exercício cadastrado na biblioteca") else formulario.adicionar()
        }

        v.findViewById<Button>(R.id.btn_salvar_treino_personalizado).setOnClickListener {
            campoNome.error = null
            val nome = campoNome.texto.trim()
            val dias = spinnerDia.diasEscolhidos()
            val categoria = Categorias.escolhida(spinnerCategoria)
            if (nome.isEmpty()) { campoNome.error = "Digite o nome do treino"; return@setOnClickListener }
            if (categoria.isEmpty()) { toast("Selecione a categoria"); return@setOnClickListener }
            if (dias == null) { toast("Selecione o dia da semana"); return@setOnClickListener }
            if (formulario.vazio) { toast("Adicione pelo menos um exercício"); return@setOnClickListener }
            val itens = formulario.ler { toast(it) } ?: return@setOnClickListener

            salvar(
                TreinoAluno(
                    userId = alunoId, diaSemana = dias.first(), treinoId = null,
                    nome = nome, descricao = modelo?.descricao.orEmpty(), categoria = categoria, exercicios = itens
                ),
                dias, dialog
            )
        }

        dialog.mostrarExpandido()
    }

    private fun salvar(base: TreinoAluno, dias: List<Int>, dialog: BottomSheetDialog) {
        val idsEditando = editando.mapNotNull { it.id }.toSet()
        val ocupados = agenda.filter { it.diaSemana in dias && it.id !in idsEditando }
        val executar = {
            lancar {
                Supabase.TreinosAluno.salvarDias(base, dias, editando)
                runCatching {
                    Supabase.Notificacoes.enviar(
                        alunoId,
                        "Novo treino na sua agenda",
                        "${base.nome} foi adicionado para ${DiaSemana.juntar(dias).lowercase()}.",
                        Notificacao.TIPO_TREINO
                    )
                }
                toast("Treino salvo para ${DiaSemana.juntar(dias).lowercase()}")
                editando = emptyList()
                diasPendentes = null
                treinoEscolhido = null
                dialog.dismiss()
                carregarAgenda()
            }
        }
        if (ocupados.isEmpty()) {
            executar()
        } else {
            abrirPopupConfirmacao(
                requireContext(),
                titulo = "Substituir treino?",
                descricao = "${DiaSemana.juntar(ocupados.map { it.diaSemana })} já tem treino " +
                    "(\"${ocupados.first().nome}\"). Deseja substituir?",
                textoConfirmar = "Substituir"
            ) { executar() }
        }
    }

    companion object {
        private const val ARG_ALUNO_ID = "aluno_id"

        @JvmStatic
        fun newInstance(alunoId: String) = GerenciarTreinoAlunoFragment().apply {
            arguments = Bundle().apply { putString(ARG_ALUNO_ID, alunoId) }
        }
    }
}
