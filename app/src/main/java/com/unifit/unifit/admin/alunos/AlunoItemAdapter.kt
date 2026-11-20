package com.unifit.unifit.admin.alunos

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.unifit.unifit.MainAdminActivity
import com.unifit.unifit.R
import com.unifit.unifit.server.Usuario
import com.unifit.unifit.utilitarios.mostrarBase64

class AlunoItemAdapter(
    private val mainAdminActivity: MainAdminActivity
) : RecyclerView.Adapter<AlunoItemAdapter.AlunoViewHolder>() {

    private var listaUsuarios = listOf<Usuario>()
    private var naoLidas = mapOf<String, Int>()

    fun updateData(lista: List<Usuario>, naoLidas: Map<String, Int> = this.naoLidas) {
        listaUsuarios = lista
        this.naoLidas = naoLidas
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlunoViewHolder =
        AlunoViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.card_aluno, parent, false))

    override fun onBindViewHolder(holder: AlunoViewHolder, position: Int) = holder.bind(listaUsuarios[position])

    override fun getItemCount(): Int = listaUsuarios.size

    inner class AlunoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val foto = itemView.findViewById<ImageView>(R.id.imageView31)
        private val nome = itemView.findViewById<TextView>(R.id.textView163)
        private val btnMenu = itemView.findViewById<ImageButton>(R.id.btn_menu_aluno)
        private val btnChat = itemView.findViewById<ImageButton>(R.id.btn_ir_chat_aluno)
        private val pontoOnline = itemView.findViewById<View>(R.id.view13)
        private val pontoMensagem = itemView.findViewById<View>(R.id.view14)

        fun bind(usuario: Usuario) {
            nome.text = usuario.nome
            foto.mostrarBase64(usuario.fotoPerfil)
            pontoOnline.visibility = if (usuario.online) View.VISIBLE else View.INVISIBLE
            pontoMensagem.visibility = if ((naoLidas[usuario.id] ?: 0) > 0) View.VISIBLE else View.INVISIBLE
            btnMenu.setOnClickListener { abrirMenu(itemView, usuario) }
            btnChat.setOnClickListener {
                mainAdminActivity.toggleContentMainFrameFullScreen(ChatAdminFragment.newInstance(usuario.id, usuario.nome))
            }
        }
    }

    private fun abrirMenu(anchor: View, usuario: Usuario) {
        val popupView = LayoutInflater.from(anchor.context).inflate(R.layout.menu_aluno_admin, null)
        val popupWindow = PopupWindow(
            popupView,
            (200 * anchor.resources.displayMetrics.density).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        popupWindow.isOutsideTouchable = true
        popupWindow.elevation = 12f
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        popupView.findViewById<Button>(R.id.btn_gerenciar_treinos_aluno).setOnClickListener {
            mainAdminActivity.toggleContentMainFrameFullScreen(GerenciarTreinoAlunoFragment.newInstance(usuario.id))
            popupWindow.dismiss()
        }
        popupView.findViewById<Button>(R.id.btn_ver_perfil_aluno).setOnClickListener {
            mainAdminActivity.toggleContentMainFrameFullScreen(PerfilAlunoAdminFragment.newInstance(usuario.id))
            popupWindow.dismiss()
        }

        popupWindow.showAsDropDown(anchor, 500, -70)
    }
}
