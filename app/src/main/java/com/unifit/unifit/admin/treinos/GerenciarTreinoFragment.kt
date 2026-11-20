package com.unifit.unifit.admin.treinos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputLayout
import com.unifit.unifit.MainAdminActivity
import com.unifit.unifit.R
import com.unifit.unifit.server.Exercicio
import com.unifit.unifit.server.Supabase
import com.unifit.unifit.server.Treino
import com.unifit.unifit.utilitarios.Categorias
import com.unifit.unifit.utilitarios.Resultados
import com.unifit.unifit.utilitarios.abrirPopupConfirmacao
import com.unifit.unifit.utilitarios.lancar
import com.unifit.unifit.utilitarios.texto
import com.unifit.unifit.utilitarios.toast
import com.unifit.unifit.server.DiaSemana
import com.unifit.unifit.utilitarios.FormularioExercicios
import com.unifit.unifit.utilitarios.configurarDiasSemana
import com.unifit.unifit.utilitarios.diaEscolhido
import com.unifit.unifit.utilitarios.mostrarExpandido

class GerenciarTreinoFragment : Fragment() {

    private var treino: Treino? = null
    private var exercicios: List<Exercicio> = emptyList()

    private lateinit var textNome: TextView
    private lateinit var textCategoria: TextView
    private lateinit var textDescricao: TextView
    private lateinit var textExercicios: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_gerenciar_treino, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mainActivity = requireActivity() as MainAdminActivity

        textNome = view.findViewById(R.id.textView229)
        textCategoria = view.findViewById(R.id.textView230)
        textDescricao = view.findViewById(R.id.textView233)
        textExercicios = view.findViewById(R.id.textView235)

        view.findViewById<TextView>(R.id.link_voltar_gerenciar_treino).setOnClickListener {
            mainActivity.closeFullScreenFrame()
        }
        view.findViewById<Button>(R.id.btn_editar_treino).setOnClickListener { editarTreino() }
        view.findViewById<Button>(R.id.btn_excluir_treino).setOnClickListener { excluir() }

        val id = arguments?.getLong(ARG_ID, -1L)?.takeIf { it > 0 }
        if (id == null) {
            textNome.text = "Novo treino"
            textCategoria.text = "Categoria: Sem categoria"
            textDescricao.text = "Nenhuma descrição adicionada ainda. Edite o treino!"
            textExercicios.text = "Nenhum exercício adicionado ainda. Edite o treino!"
        }
        lancar {
            exercicios = Supabase.Exercicios.listar(comImagem = false)
            if (id != null) mostrar(Supabase.Treinos.buscar(id))
        }
    }

    private fun mostrar(t: Treino) {
        treino = t
        textNome.text = t.nome
        textCategoria.text = "Categoria: ${t.categoria.ifBlank { "Sem categoria" }}" +
            (t.diaSemana?.let { " • ${DiaSemana.nome(it)}" } ?: "")
        textDescricao.text = t.descricao.ifBlank { "Nenhuma descrição adicionada ainda. Edite o treino!" }

        val porId = exercicios.associateBy { it.id }
        textExercicios.text = if (t.exercicios.isEmpty()) {
            "Nenhum exercício adicionado ainda. Edite o treino!"
        } else {
            t.exercicios.mapIndexed { i, item ->
                "${i + 1}. ${porId[item.exercicioId]?.nome ?: "Exercício removido"} — ${item.series} séries de ${item.repeticoes} repetições"
            }.joinToString("\n")
        }
    }

    private fun editarTreino() {
        if (exercicios.isEmpty()) toast("Cadastre exercícios antes de montar um treino")

        val dialog = BottomSheetDialog(requireContext())
        val bs = layoutInflater.inflate(R.layout.bottom_sheet_treino_biblioteca, null)
        dialog.setContentView(bs)

        val campoNome = bs.findViewById<TextInputLayout>(R.id.textInputLayout)
        val campoDescricao = bs.findViewById<EditText>(R.id.edit_descricao_treino)
        val spinnerCategoria = bs.findViewById<Spinner>(R.id.spinner)
        val spinnerDia = bs.findViewById<Spinner>(R.id.spinner_dias_semana_treino)
        val formulario = FormularioExercicios(bs.findViewById(R.id.lista_exercicio_personalizado), exercicios)

        val atual = treino
        campoNome.texto = atual?.nome.orEmpty()
        campoDescricao.setText(atual?.descricao.orEmpty())
        Categorias.configurarSpinner(spinnerCategoria, atual?.categoria)
        spinnerDia.configurarDiasSemana(atual?.diaSemana, semDiaFixo = true)
        atual?.exercicios?.forEach { formulario.adicionar(it) }
        if (formulario.vazio && exercicios.isNotEmpty()) formulario.adicionar()

        bs.findViewById<ImageButton>(R.id.btn_fechar_treino_personalizado).setOnClickListener { dialog.dismiss() }
        bs.findViewById<Button>(R.id.btn_adicionar_exercicio_treino).setOnClickListener {
            if (exercicios.isEmpty()) toast("Nenhum exercício cadastrado") else formulario.adicionar()
        }

        val btnSalvar = bs.findViewById<Button>(R.id.btn_salvar_treino_personalizado)
        btnSalvar.setOnClickListener {
            campoNome.error = null
            val categoria = Categorias.escolhida(spinnerCategoria)
            if (campoNome.texto.isBlank()) { campoNome.error = "Digite o nome do treino"; return@setOnClickListener }
            if (categoria.isEmpty()) { toast("Selecione a categoria"); return@setOnClickListener }
            val itens = formulario.ler { toast(it) } ?: return@setOnClickListener

            val novo = Treino(
                id = atual?.id,
                nome = campoNome.texto.trim(),
                descricao = campoDescricao.text.toString().trim(),
                categoria = categoria,
                exercicios = itens,
                diaSemana = spinnerDia.diaEscolhido(semDiaFixo = true)
            )

            btnSalvar.isEnabled = false
            lancar(aoFalhar = { msg ->
                toast(msg)
                btnSalvar.isEnabled = true
            }) {
                val salvo = if (novo.id == null) Supabase.Treinos.criar(novo)
                else {
                    Supabase.Treinos.atualizar(novo)
                    Supabase.Treinos.buscar(novo.id)
                }
                mostrar(salvo)
                parentFragmentManager.setFragmentResult(Resultados.TREINOS_ALTERADOS, Bundle())
                toast(if (novo.id == null) "Treino criado" else "Treino atualizado")
                dialog.dismiss()
            }
        }

        dialog.mostrarExpandido()
    }

    private fun excluir() {
        val mainActivity = requireActivity() as MainAdminActivity
        val id = treino?.id
        if (id == null) {
            mainActivity.closeFullScreenFrame()
            return
        }
        abrirPopupConfirmacao(
            requireContext(),
            titulo = "Excluir treino?",
            descricao = "Tem certeza que deseja excluir o treino da biblioteca de treinos prontos?",
            textoConfirmar = "Excluir treino"
        ) {
            lancar {
                Supabase.Treinos.excluir(id)
                parentFragmentManager.setFragmentResult(Resultados.TREINOS_ALTERADOS, Bundle())
                toast("Treino excluído")
                mainActivity.closeFullScreenFrame()
            }
        }
    }

    companion object {
        private const val ARG_ID = "treino_id"

        @JvmStatic
        fun newInstance(id: Long?) = GerenciarTreinoFragment().apply {
            arguments = Bundle().apply { putLong(ARG_ID, id ?: -1L) }
        }
    }
}
