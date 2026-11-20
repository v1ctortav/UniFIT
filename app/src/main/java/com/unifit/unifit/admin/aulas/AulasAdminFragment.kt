package com.unifit.unifit.admin.aulas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.unifit.unifit.R
import com.unifit.unifit.server.AulaDetalhada
import com.unifit.unifit.server.FiltroData
import com.unifit.unifit.server.Supabase
import com.unifit.unifit.server.Tempo
import com.unifit.unifit.utilitarios.CalendarioExpansivelView
import com.unifit.unifit.utilitarios.mostrarExpandido
import com.unifit.unifit.utilitarios.abrirPopupConfirmacao
import com.unifit.unifit.utilitarios.lancar
import com.unifit.unifit.utilitarios.toast
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.unifit.unifit.utilitarios.EstadoVazioAdapter
import com.unifit.unifit.utilitarios.usarAdapterComVazio

class AulasAdminFragment : Fragment() {

    private lateinit var adapter: AulaCardAdminAdapter
    private lateinit var calendario: CalendarioExpansivelView
    private lateinit var textTitulo: TextView
    private var dataFiltro: LocalDate = LocalDate.now()
    private lateinit var vazio: EstadoVazioAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_aulas_admin, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textTitulo = view.findViewById(R.id.textView162)

        adapter = AulaCardAdminAdapter(
            aoEditar = { aula -> abrirFormularioAula(aula.paraAula(), dataFiltro) { carregar() } },
            aoDetalhes = { detalhes(it) },
            aoExcluir = { confirmarExcluir(it) }
        )
        view.findViewById<RecyclerView>(R.id.rv_cards_aulas_admin).apply {
            layoutManager = LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)
            vazio = usarAdapterComVazio(this@AulasAdminFragment.adapter, "Nenhuma aula sua neste dia. Toque em \"Adicionar aula\".")
        }

        calendario = view.findViewById(R.id.calendario_admin)
        dataFiltro = calendario.dataSelecionada()
        calendario.onDataSelecionada = { data ->
            dataFiltro = data
            carregar()
        }

        view.findViewById<Button>(R.id.btn_adicionar_card_aula).setOnClickListener {
            abrirFormularioAula(null, dataFiltro) { carregar() }
        }

        carregar()
    }

    private fun carregar() {
        textTitulo.text = if (dataFiltro == LocalDate.now()) "Aulas de hoje"
        else "Aulas de ${dataFiltro.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}"
        adapter.updateData(emptyList())
        vazio.carregando()
        lancar {
            val lista = Supabase.Aulas.listar(FiltroData.doDia(dataFiltro))
            adapter.updateData(lista)
            vazio.mostrar(lista.isEmpty())
        }
    }

    private fun detalhes(aula: AulaDetalhada) {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottomshett_datalhes_aulas_admin, null)
        dialog.setContentView(view)
        view.findViewById<ImageButton>(R.id.btn_fechar_detalhes_aula).setOnClickListener { dialog.dismiss() }

        val texto = view.findViewById<TextView>(R.id.textView167)
        val base = "• Nome: ${aula.nome}\n" +
            "• Data: ${Tempo.dataBr(aula.data)}\n" +
            "• Horário: ${aula.horarioInicio} - ${aula.horarioFim}\n" +
            "• Quantidade de alunos: ${aula.vagasOcupadas}/${aula.vagas}"
        texto.text = base

        lancar {
            val inscritos = Supabase.Aulas.listarInscritos(aula.id)
            if (inscritos.isNotEmpty()) {
                texto.text = base + "\n\nAlunos inscritos:\n" +
                    inscritos.joinToString("\n") { "  – ${it.usuarios?.nome ?: "Aluno"}" }
            }
        }
        dialog.mostrarExpandido()
    }

    private fun confirmarExcluir(aula: AulaDetalhada) {
        abrirPopupConfirmacao(
            requireContext(),
            titulo = "Excluir aula?",
            descricao = "Tem certeza que deseja excluir a aula coletiva?",
            textoConfirmar = "Excluir aula"
        ) {
            lancar {
                Supabase.Aulas.excluir(aula.id)
                toast("Aula excluída")
                carregar()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        view?.findViewById<CalendarioExpansivelView>(R.id.calendario_admin)?.atualizarParaHoje()
    }
}
