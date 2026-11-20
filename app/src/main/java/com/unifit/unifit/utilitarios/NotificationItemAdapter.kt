package com.unifit.unifit.utilitarios

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.unifit.unifit.R
import com.unifit.unifit.server.Notificacao

class NotificationItemAdapter(
    private val aoClicar: (Notificacao) -> Unit = {}
) : Adapter<NotificationItemAdapter.NotificationViewHolder>() {

    private var listItensNotificacao = listOf<Notificacao>()

    fun updateData(lista: List<Notificacao>) {
        listItensNotificacao = lista
        notifyDataSetChanged()
    }

    fun atualizarTempos() = notifyItemRangeChanged(0, itemCount)

    fun itemEm(posicao: Int): Notificacao? = listItensNotificacao.getOrNull(posicao)

    inner class NotificationViewHolder(itemView: View) : ViewHolder(itemView) {
        private val imgIconNotification: ImageView = itemView.findViewById(R.id.img_notification_item)
        private val textNotification: TextView = itemView.findViewById(R.id.text_name_notification_item)
        private val textDescNotification: TextView = itemView.findViewById(R.id.text_desc_notification_item)
        private val textTimeNotification: TextView = itemView.findViewById(R.id.text_time_notification_item)
        private val newAlertNotification: View = itemView.findViewById(R.id.new_alert_notification_item)

        fun bind(notificacao: Notificacao) {
            textNotification.text = notificacao.nome
            textDescNotification.text = notificacao.descricao
            textTimeNotification.text = notificacao.tempoDecorrido.replaceFirstChar { it.uppercase() }

            imgIconNotification.setImageResource(
                when {
                    notificacao.isSistema -> R.drawable.ic_error_outline
                    notificacao.isTreino -> R.drawable.ic_fitness_center
                    notificacao.alunoDoChat != null -> R.drawable.ic_chat
                    else -> R.drawable.ic_notifications
                }
            )

            val naoLida = !notificacao.lida
            imgIconNotification.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(itemView.context, if (naoLida) R.color.unifor else R.color.black)
            )
            imgIconNotification.setPadding((16 * itemView.resources.displayMetrics.density).toInt())

            newAlertNotification.visibility = if (naoLida) View.VISIBLE else View.GONE

            itemView.setOnClickListener { aoClicar(notificacao) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.card_notification_item, parent, false)
        return NotificationViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) =
        holder.bind(listItensNotificacao[position])

    override fun getItemCount(): Int = listItensNotificacao.size
}
