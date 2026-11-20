package com.unifit.unifit.admin.alunos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.unifit.unifit.MainAdminActivity
import com.unifit.unifit.R
import com.unifit.unifit.server.Supabase
import com.unifit.unifit.server.Tempo
import com.unifit.unifit.utilitarios.lancar
import com.unifit.unifit.utilitarios.mostrarBase64
import java.time.LocalDate

class PerfilAlunoAdminFragment : Fragment() {

    private val alunoId: String by lazy { requireArguments().getString(ARG_ALUNO_ID).orEmpty() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_perfil_aluno_admin, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ImageButton>(R.id.btn_voltar_perfil_aluno).setOnClickListener {
            (requireActivity() as MainAdminActivity).closeFullScreenFrame()
        }

        val nomeBanner = view.findViewById<TextView>(R.id.textView170)
        val nome = view.findViewById<TextView>(R.id.textView175)
        val email = view.findViewById<TextView>(R.id.textView176)
        val foto = view.findViewById<ImageView>(R.id.imageView33)
        val nomePlano = view.findViewById<TextView>(R.id.textView181)
        val statusPlano = view.findViewById<TextView>(R.id.text_status_plano2)
        val vencimento = view.findViewById<TextView>(R.id.textView182)

        lancar {
            val aluno = Supabase.Usuarios.buscar(alunoId)
            nomeBanner.text = aluno.nome
            nome.text = aluno.nome
            email.text = aluno.email
            foto.mostrarBase64(aluno.fotoPerfil)

            val plano = Supabase.Planos.planoDe(alunoId)
            nomePlano.text = plano?.nomePlano ?: "Sem plano"
            vencimento.text = Tempo.dataBr(plano?.dataVencimento)
            val vencido = plano?.dataVencimento?.let { runCatching { LocalDate.parse(it).isBefore(LocalDate.now()) }.getOrNull() } ?: false
            val ativo = plano?.status == true && !vencido
            statusPlano.text = if (ativo) "Ativo" else if (plano?.dataVencimento != null) "Vencido" else "Inativo"
            statusPlano.setTextColor(ContextCompat.getColorStateList(requireContext(), if (ativo) R.color.unifor else R.color.red))
        }
    }

    companion object {
        private const val ARG_ALUNO_ID = "aluno_id"

        @JvmStatic
        fun newInstance(alunoId: String) = PerfilAlunoAdminFragment().apply {
            arguments = Bundle().apply { putString(ARG_ALUNO_ID, alunoId) }
        }
    }
}
