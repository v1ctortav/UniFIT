package com.unifit.unifit.cliente.aulas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.unifit.unifit.R
import com.unifit.unifit.server.AbaAulas
import com.unifit.unifit.server.AulaDetalhada
import com.unifit.unifit.server.FiltroData
import com.unifit.unifit.server.Supabase
import com.unifit.unifit.server.aplicarAba
import com.unifit.unifit.server.substituir
import com.unifit.unifit.utilitarios.CalendarioExpansivelView
import com.unifit.unifit.utilitarios.lancar
import com.unifit.unifit.utilitarios.toast
import java.time.LocalDate
import com.unifit.unifit.utilitarios.EstadoVazioAdapter
import com.unifit.unifit.utilitarios.usarAdapterComVazio

class AulasFragment : Fragment() {

    private lateinit var rvListAula: RecyclerView
    private lateinit var aulaCardAdapter: AulaCardAdapter
    private lateinit var calendario: CalendarioExpansivelView
    private lateinit var vazio: EstadoVazioAdapter

    private var aulasDoDia: List<AulaDetalhada> = emptyList()
    private var aba = AbaAulas.TODAS
    private var dataFiltro: LocalDate = LocalDate.now()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_aulas, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        aulaCardAdapter = AulaCardAdapter { aula -> alternarMatricula(aula) }
        rvListAula = view.findViewById(R.id.rv_aulas)
        vazio = rvListAula.usarAdapterComVazio(aulaCardAdapter, "Nenhuma aula neste dia por enquanto")
        rvListAula.layoutManager = LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)

        toggleCategoryAula(view)

        calendario = view.findViewById(R.id.calendario)
        dataFiltro = calendario.dataSelecionada()
        calendario.onDataSelecionada = { data ->
            dataFiltro = data
            carregarAulas()
        }

        carregarAulas()
    }

    private fun carregarAulas() {
        aulaCardAdapter.updateData(emptyList())
        vazio.carregando()
        lancar {
            aulasDoDia = Supabase.Aulas.listar(FiltroData.doDia(dataFiltro))
            mostrar()
        }
    }

    private fun mostrar() {
        val lista = aulasDoDia.aplicarAba(aba)
        aulaCardAdapter.updateData(lista)
        vazio.mostrar(lista.isEmpty(), if (aba == AbaAulas.AGENDADAS) "Você não tem aulas agendadas neste dia" else "Nenhuma aula neste dia por enquanto")
    }

    private fun alternarMatricula(aula: AulaDetalhada) {
        lancar {
            val atualizada = Supabase.Aulas.alternarMatricula(aula)
            aulasDoDia = aulasDoDia.substituir(atualizada)
            if (aba == AbaAulas.AGENDADAS && !atualizada.inscrito) mostrar()
            else aulaCardAdapter.atualizar(atualizada)
            toast(if (atualizada.inscrito) "Inscrição confirmada em ${atualizada.nome}" else "Inscrição cancelada")
        }
    }

    private fun toggleCategoryAula(view: View) {
        val btnTodas = view.findViewById<Button>(R.id.btn_toggle_todas_aulas)
        val btnAgendadas = view.findViewById<Button>(R.id.btn_toggle_agendadas_auls)
        val botoes = listOf(btnTodas, btnAgendadas)

        botoes.forEach { btn ->
            btn.isSelected = btn == btnTodas
            btn.setOnClickListener {
                botoes.forEach { it.isSelected = it == btn }
                aba = if (btn == btnTodas) AbaAulas.TODAS else AbaAulas.AGENDADAS
                mostrar()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        view?.findViewById<CalendarioExpansivelView>(R.id.calendario)?.atualizarParaHoje()
    }
}
