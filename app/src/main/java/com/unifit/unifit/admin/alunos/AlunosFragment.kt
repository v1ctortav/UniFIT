package com.unifit.unifit.admin.alunos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.unifit.unifit.MainAdminActivity
import com.unifit.unifit.R
import com.unifit.unifit.server.Supabase
import com.unifit.unifit.server.Usuario
import com.unifit.unifit.utilitarios.configurarPesquisa
import com.unifit.unifit.utilitarios.contemBusca
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.unifit.unifit.utilitarios.EstadoVazioAdapter
import com.unifit.unifit.utilitarios.usarAdapterComVazio

class AlunosFragment : Fragment() {

    private lateinit var alunoItemAdapter: AlunoItemAdapter
    private var alunos: List<Usuario> = emptyList()
    private var naoLidas: Map<String, Int> = emptyMap()
    private var busca = ""
    private lateinit var vazio: EstadoVazioAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_alunos, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configurarPesquisa(view.findViewById(R.id.btn_search_aluno), view.findViewById(R.id.search_input_aluno)) {
            busca = it
            mostrar()
        }

        alunoItemAdapter = AlunoItemAdapter(requireActivity() as MainAdminActivity)
        view.findViewById<RecyclerView>(R.id.rv_alunos).apply {
            layoutManager = LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)
            vazio = usarAdapterComVazio(alunoItemAdapter, "Nenhum aluno encontrado")
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    runCatching {
                        alunos = Supabase.Usuarios.listarClientes()
                        naoLidas = Supabase.Chat.conversas().associate { it.alunoId to it.naoLidasAdmin }
                        mostrar()
                    }
                    delay(15_000)
                }
            }
        }
    }

    private fun mostrar() {
        val lista = alunos.filter { it.nome.contemBusca(busca) || it.email.contemBusca(busca) }
            .sortedWith(compareByDescending<Usuario> { (naoLidas[it.id] ?: 0) > 0 }.thenBy { it.nome })
        alunoItemAdapter.updateData(lista, naoLidas)
        vazio.mostrar(lista.isEmpty())
    }
}
