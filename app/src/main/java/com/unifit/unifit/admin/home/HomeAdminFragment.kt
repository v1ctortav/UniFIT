package com.unifit.unifit.admin.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.unifit.unifit.MainAdminActivity
import com.unifit.unifit.R
import com.unifit.unifit.admin.notificacoes.NotificacoesAdminFragment
import com.unifit.unifit.admin.treinos.ExerciciosBibliotecaAdminFragment
import com.unifit.unifit.admin.treinos.TreinosBibliotecaAdminFragment
import com.unifit.unifit.server.AulaDetalhada
import com.unifit.unifit.server.FiltroData
import com.unifit.unifit.server.Supabase
import com.unifit.unifit.utilitarios.adicionarCarregando
import com.unifit.unifit.utilitarios.adicionarEstadoVazio
import com.unifit.unifit.utilitarios.lancar
import com.unifit.unifit.utilitarios.mostrarContador
import com.unifit.unifit.utilitarios.mostrarBase64
import com.unifit.unifit.utilitarios.primeiroNome
import java.time.LocalDate

class HomeAdminFragment : Fragment() {

    private lateinit var listaAulasDia: LinearLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_home_admin, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mainAdmin = requireActivity() as MainAdminActivity

        listaAulasDia = view.findViewById(R.id.lista_itens_aula_dia)
        listaAulasDia.removeAllViews()
        listaAulasDia.adicionarCarregando(claro = true)

        view.findViewById<ImageButton>(R.id.btn_go_to_notifications_admin).setOnClickListener {
            mainAdmin.toggleContentMainFrame(NotificacoesAdminFragment())
        }
        view.findViewById<ImageButton>(R.id.btn_go_to_exercicios_acess).setOnClickListener {
            mainAdmin.toggleContentMainFrame(ExerciciosBibliotecaAdminFragment())
        }
        view.findViewById<ImageButton>(R.id.btn_go_to_treinos_prontos_acess).setOnClickListener {
            mainAdmin.toggleContentMainFrame(TreinosBibliotecaAdminFragment())
        }

        val textNome = view.findViewById<TextView>(R.id.textView119)
        val textNotificacoes = view.findViewById<TextView>(R.id.notifications_count_text_admin)
        textNotificacoes.mostrarContador(null)
        val totalAlunos = view.findViewById<TextView>(R.id.textView123)
        val totalProfessores = view.findViewById<TextView>(R.id.textView126)

        lancar {
            val eu = Supabase.Usuarios.meuPerfil()
            textNome.text = "${primeiroNome(eu.nome)}!"
            view.findViewById<android.widget.ImageView>(R.id.imageView30).mostrarBase64(eu.fotoPerfil)
        }
        lancar {
            textNotificacoes.mostrarContador(Supabase.Notificacoes.contarNaoLidas())
        }
        lancar {
            runCatching { Supabase.Presenca.ping() }
            val r = Supabase.Presenca.resumo()
            view.findViewById<TextView>(R.id.textView122).text = r.alunosOnline.toString()
            totalAlunos.text = "de ${r.alunosTotal}"
            view.findViewById<TextView>(R.id.textView125).text = r.professoresOnline.toString()
            totalProfessores.text = "de ${r.professoresTotal}"
        }
        lancar {
            mostrarAulasDoDia(Supabase.Aulas.listar(FiltroData.doDia(LocalDate.now())))
        }
    }

    private fun mostrarAulasDoDia(aulas: List<AulaDetalhada>) {
        val inflater = LayoutInflater.from(listaAulasDia.context)
        listaAulasDia.removeAllViews()
        if (aulas.isEmpty()) {
            listaAulasDia.adicionarEstadoVazio("Nenhuma aula sua hoje por enquanto", claro = true)
            return
        }
        aulas.forEachIndexed { index, aula ->
            val item = inflater.inflate(R.layout.item_aula_dia_admin, listaAulasDia, false)
            item.findViewById<TextView>(R.id.textView132).text = aula.nome
            item.findViewById<TextView>(R.id.textView131).text = "${aula.horarioInicio} - ${aula.horarioFim}"
            item.findViewById<TextView>(R.id.textView133).text = "${aula.vagasOcupadas}/${aula.vagas}"
            listaAulasDia.addView(item)
            if (index != aulas.lastIndex) {
                listaAulasDia.addView(inflater.inflate(R.layout.view_diviter_aulas_dias, listaAulasDia, false))
            }
        }
    }
}
