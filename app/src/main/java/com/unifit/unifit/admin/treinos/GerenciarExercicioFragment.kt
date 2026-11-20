package com.unifit.unifit.admin.treinos

import android.net.Uri
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
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputLayout
import com.unifit.unifit.MainAdminActivity
import com.unifit.unifit.R
import com.unifit.unifit.server.Exercicio
import com.unifit.unifit.server.Supabase
import com.unifit.unifit.server.Youtube
import com.unifit.unifit.utilitarios.Categorias
import com.unifit.unifit.utilitarios.mostrarExpandido
import com.unifit.unifit.utilitarios.Resultados
import com.unifit.unifit.utilitarios.SeletorImagem
import com.unifit.unifit.utilitarios.abrirPopupConfirmacao
import com.unifit.unifit.utilitarios.emTopicos
import com.unifit.unifit.utilitarios.lancar
import com.unifit.unifit.utilitarios.texto
import com.unifit.unifit.utilitarios.toast
import androidx.lifecycle.lifecycleScope
import com.unifit.unifit.utilitarios.MiniaturaYoutube
import com.unifit.unifit.utilitarios.YoutubePlayerView
import com.unifit.unifit.utilitarios.aoDigitar
import com.unifit.unifit.utilitarios.mostrarIconeExercicio

class GerenciarExercicioFragment : Fragment() {

    private val seletorImagem = SeletorImagem(this)
    private var exercicio: Exercicio? = null

    private lateinit var textNome: TextView
    private lateinit var textEquipamentos: TextView
    private lateinit var player: YoutubePlayerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_gerenciar_exercicio, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mainActivity = requireActivity() as MainAdminActivity

        textNome = view.findViewById(R.id.textView211)
        textEquipamentos = view.findViewById(R.id.textView213)
        player = view.findViewById(R.id.player_video)
        player.vincularBotoes(view.findViewById(R.id.imageButton2), view.findViewById(R.id.imageButton3))

        view.findViewById<TextView>(R.id.link_voltar_gerenciar_exercicio).setOnClickListener {
            mainActivity.closeFullScreenFrame()
        }

        val btnEditarVideoIcone = view.findViewById<ImageButton>(R.id.btn_editar_video_exercicio)
        player.aoTocarNaArea = { btnEditarVideoIcone.visibility = View.VISIBLE }
        btnEditarVideoIcone.setOnClickListener {
            btnEditarVideoIcone.visibility = View.GONE
            editarVideo()
        }

        view.findViewById<Button>(R.id.btn_editar_exercicio).setOnClickListener { editarExercicio() }
        view.findViewById<Button>(R.id.btn_excluir_exercicio).setOnClickListener { excluir() }

        val id = arguments?.getLong(ARG_ID, -1L)?.takeIf { it > 0 }
        if (id == null) {
            textNome.text = "Novo exercício"
            textEquipamentos.text = "Toque em \"Editar exercício\" para preencher nome, categoria e equipamentos."
            player.carregar(null)
        } else {
            lancar { mostrar(Supabase.Exercicios.buscar(id)) }
        }
    }

    private fun mostrar(ex: Exercicio) {
        exercicio = ex
        textNome.text = ex.nome
        textEquipamentos.text = ex.equipamentos.emTopicos()
        player.carregar(ex.linkYoutube)
    }

    private fun avisarListas() {
        parentFragmentManager.setFragmentResult(Resultados.EXERCICIOS_ALTERADOS, Bundle())
    }

    private fun editarExercicio() {
        val dialog = BottomSheetDialog(requireContext())
        val sheet = layoutInflater.inflate(R.layout.bottom_sheet_editar_exercicio, null)
        dialog.setContentView(sheet)

        val campoNome = sheet.findViewById<TextInputLayout>(R.id.textInputLayout)
        val spinnerCategoria = sheet.findViewById<Spinner>(R.id.spinner)
        val lista = sheet.findViewById<LinearLayout>(R.id.lista_equipamentos_exercicio)

        val atual = exercicio
        campoNome.texto = atual?.nome.orEmpty()
        Categorias.configurarSpinner(spinnerCategoria, atual?.categoria)

        fun adicionarEquipamento(valor: String) {
            val item = layoutInflater.inflate(R.layout.item_exercicio_equipamento_lista, lista, false)
            item.findViewById<TextInputLayout>(R.id.textInputLayout).texto = valor
            item.findViewById<ImageButton>(R.id.btn_excluir_equipamento).setOnClickListener { lista.removeView(item) }
            lista.addView(item)
        }
        atual?.equipamentos?.forEach { adicionarEquipamento(it) }

        sheet.findViewById<ImageButton>(R.id.btn_fechar_editar_exercicio).setOnClickListener { dialog.dismiss() }
        sheet.findViewById<Button>(R.id.btn_adicionar_equipamento).setOnClickListener { adicionarEquipamento("") }

        val btnSalvar = sheet.findViewById<Button>(R.id.btn_salvar_edicao_exercicio)
        btnSalvar.setOnClickListener {
            val nome = campoNome.texto.trim()
            val categoria = Categorias.escolhida(spinnerCategoria)
            if (categoria.isEmpty()) {
                toast("Selecione a categoria")
                return@setOnClickListener
            }
            val equipamentos = (0 until lista.childCount).map {
                lista.getChildAt(it).findViewById<TextInputLayout>(R.id.textInputLayout).texto.trim()
            }.filter { it.isNotEmpty() }

            if (nome.isEmpty()) {
                campoNome.error = "Digite o nome do exercício"
                return@setOnClickListener
            }

            btnSalvar.isEnabled = false
            lancar(aoFalhar = { msg ->
                toast(msg)
                btnSalvar.isEnabled = true
            }) {
                val id = exercicio?.id
                val salvo = if (id == null) {
                    Supabase.Exercicios.criar(Exercicio(nome = nome, categoria = categoria, equipamentos = equipamentos))
                } else {
                    Supabase.Exercicios.atualizarInfo(id, nome, categoria, equipamentos)
                    Supabase.Exercicios.buscar(id)
                }
                mostrar(salvo)
                avisarListas()
                toast(if (id == null) "Exercício criado" else "Exercício atualizado")
                dialog.dismiss()
            }
        }

        dialog.mostrarExpandido()
    }

    private fun editarVideo() {
        val atual = exercicio
        val id = atual?.id
        if (id == null) {
            toast("Salve primeiro o nome e a categoria em \"Editar exercício\"")
            return
        }

        val dialog = BottomSheetDialog(requireContext())
        val sheet = layoutInflater.inflate(R.layout.bottom_sheet_editar_video_icone_exercicio, null)
        dialog.setContentView(sheet)

        val campoLink = sheet.findViewById<TextInputLayout>(R.id.textInputLayout24)
        val previaVideo = sheet.findViewById<ImageView>(R.id.imageView36)
        val previaIcone = sheet.findViewById<ImageView>(R.id.imageView37)

        campoLink.editText?.isSingleLine = true
        campoLink.texto = atual.linkYoutube.orEmpty()
        MiniaturaYoutube.carregar(viewLifecycleOwner.lifecycleScope, previaVideo, atual.linkYoutube?.let { Youtube.idDoVideo(it) })
        previaIcone.mostrarIconeExercicio(atual)
        campoLink.editText?.aoDigitar { link ->
            val id = Youtube.idDoVideo(link.trim())
            if (id != null || link.isBlank()) MiniaturaYoutube.carregar(viewLifecycleOwner.lifecycleScope, previaVideo, id)
        }

        var novaImagem: Uri? = null

        sheet.findViewById<ImageButton>(R.id.btn_fechar_editar_video_exercicio).setOnClickListener { dialog.dismiss() }

        sheet.findViewById<Button>(R.id.button5).setOnClickListener {
            seletorImagem.abrir { uri ->
                novaImagem = uri
                previaIcone.setImageURI(uri)
            }
        }

        val btnConfirmar = sheet.findViewById<Button>(R.id.btn_confirmar_edicao_video)
        btnConfirmar.setOnClickListener {
            campoLink.error = null
            val link = campoLink.texto.trim()
            if (link.isNotEmpty() && !Youtube.linkValido(link)) {
                campoLink.error = "Link do YouTube inválido"
                return@setOnClickListener
            }

            btnConfirmar.isEnabled = false
            lancar(aoFalhar = { msg ->
                toast(msg)
                btnConfirmar.isEnabled = true
            }) {
                Supabase.Exercicios.atualizarMidia(requireContext(), id, link, novaImagem)
                mostrar(Supabase.Exercicios.buscar(id))
                avisarListas()
                toast("Vídeo e ícone atualizados")
                dialog.dismiss()
            }
        }

        dialog.mostrarExpandido()
    }

    private fun excluir() {
        val mainActivity = requireActivity() as MainAdminActivity
        val id = exercicio?.id
        if (id == null) {
            mainActivity.closeFullScreenFrame()
            return
        }
        abrirPopupConfirmacao(
            requireContext(),
            titulo = "Excluir exercício?",
            descricao = "Tem certeza que deseja excluir o exercício da biblioteca de treino?",
            textoConfirmar = "Excluir exercício"
        ) {
            lancar {
                Supabase.Exercicios.excluir(id)
                avisarListas()
                toast("Exercício excluído")
                mainActivity.closeFullScreenFrame()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (::player.isInitialized) player.pausar()
    }

    override fun onDestroyView() {
        if (::player.isInitialized) player.liberar()
        super.onDestroyView()
    }

    companion object {
        private const val ARG_ID = "exercicio_id"

        @JvmStatic
        fun newInstance(id: Long?) = GerenciarExercicioFragment().apply {
            arguments = Bundle().apply { putLong(ARG_ID, id ?: -1L) }
        }
    }
}
