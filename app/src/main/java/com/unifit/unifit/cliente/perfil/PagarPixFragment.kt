package com.unifit.unifit.cliente.perfil

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.unifit.unifit.MainActivity
import com.unifit.unifit.R
import com.unifit.unifit.server.CobrancaPix
import com.unifit.unifit.server.Supabase
import com.unifit.unifit.server.Tempo
import com.unifit.unifit.utilitarios.Resultados
import com.unifit.unifit.utilitarios.abrirPopupConfirmacao
import com.unifit.unifit.utilitarios.br
import com.unifit.unifit.utilitarios.lancar
import com.unifit.unifit.utilitarios.toast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class PagarPixFragment : Fragment() {

    private lateinit var imagemQr: ImageView
    private lateinit var progresso: ProgressBar
    private lateinit var textoValor: TextView
    private lateinit var textoCodigo: TextView
    private lateinit var btnCopiar: Button
    private lateinit var progressoStatus: ProgressBar
    private lateinit var textoStatus: TextView

    private var cobranca: CobrancaPix? = null
    private var verificacao: Job? = null
    private var concluido = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_pagar_pix, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imagemQr = view.findViewById(R.id.imageView29)
        progresso = view.findViewById(R.id.progresso_pix)
        textoValor = view.findViewById(R.id.texto_valor_pix)
        textoCodigo = view.findViewById(R.id.textView117)
        btnCopiar = view.findViewById(R.id.button2)
        progressoStatus = view.findViewById(R.id.progresso_status_pix)
        textoStatus = view.findViewById(R.id.texto_status_pix)

        view.findViewById<ImageButton>(R.id.btn_encerrar_pagamento_pix_perfil).setOnClickListener { confirmarSaida() }

        gerar()
    }

    private fun gerar() {
        progresso.visibility = View.VISIBLE
        imagemQr.setImageDrawable(null)
        textoCodigo.text = ""
        textoValor.text = ""
        status("Gerando o Pix...", carregando = false)
        btnCopiar.text = "Copiar código"
        btnCopiar.isEnabled = false
        btnCopiar.alpha = 0.5f

        lancar(aoFalhar = { msg -> falhou(msg) }) {
            val c = Supabase.Pagamentos.gerarPix()
            mostrar(c)
            if (c.aprovado) sucesso(c) else iniciarVerificacao()
        }
    }

    private fun mostrar(c: CobrancaPix) {
        cobranca = c
        progresso.visibility = View.GONE

        val bytes = runCatching { Base64.decode(c.qrCodeBase64.orEmpty(), Base64.DEFAULT) }.getOrNull()
        bytes?.let { imagemQr.setImageBitmap(BitmapFactory.decodeByteArray(it, 0, it.size)) }

        textoValor.text = "${c.nomePlano ?: "Plano"} • R$ ${c.valor.br(2)}"
        textoCodigo.text = c.qrCode.orEmpty()
        textoCodigo.setOnClickListener { copiar() }

        btnCopiar.text = "Copiar código"
        btnCopiar.isEnabled = !c.qrCode.isNullOrEmpty()
        btnCopiar.alpha = 1f
        btnCopiar.setOnClickListener { copiar() }

        val validade = c.expiraEm?.let {
            runCatching {
                DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()).format(Tempo.parseInstante(it))
            }.getOrNull()
        }
        status("Aguardando pagamento..." + (validade?.let { "\nO código vale até $it." } ?: ""), carregando = true)
    }

    private fun copiar() {
        val codigo = cobranca?.qrCode ?: return
        val area = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        area.setPrimaryClip(ClipData.newPlainText("Pix copia e cola", codigo))
        toast("Código Pix copiado! Cole no app do seu banco.")
    }

    private fun falhou(msg: String) {
        progresso.visibility = View.GONE
        status(msg, carregando = false, erro = true)
        btnCopiar.text = "Tentar novamente"
        btnCopiar.isEnabled = true
        btnCopiar.alpha = 1f
        btnCopiar.setOnClickListener { gerar() }
    }

    private fun iniciarVerificacao() {
        verificacao?.cancel()
        verificacao = viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (!concluido) {
                    delay(5_000)
                    val id = cobranca?.id ?: break
                    try {
                        val c = Supabase.Pagamentos.consultar(id)
                        cobranca = c
                        when {
                            c.aprovado -> { sucesso(c); break }
                            c.encerrado -> { expirou(); break }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    private fun expirou() {
        status("Este Pix expirou. Gere um novo para pagar.", carregando = false, erro = true)
        imagemQr.alpha = 0.2f
        btnCopiar.text = "Gerar novo Pix"
        btnCopiar.setOnClickListener {
            imagemQr.alpha = 1f
            gerar()
        }
    }

    private fun sucesso(c: CobrancaPix) {
        if (concluido || !isAdded) return
        concluido = true
        verificacao?.cancel()
        status("Pagamento confirmado!", carregando = false)
        textoStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.unifor))
        parentFragmentManager.setFragmentResult(Resultados.PERFIL_ALTERADO, Bundle())

        abrirPopupConfirmacao(
            requireContext(),
            titulo = "Pagamento confirmado!",
            descricao = "${c.nomePlano ?: "Seu plano"} está ativo até ${Tempo.dataBr(c.vencimentoNovo)}.",
            textoConfirmar = "Concluir",
            icone = R.drawable.ic_check,
            textoCancelar = "Ficar aqui"
        ) { fechar() }
    }

    private fun fechar() {
        val main = activity as? MainActivity ?: return
        main.closeFullScreenFrame()
        main.closeFullScreenFrame()
    }

    private fun confirmarSaida() {
        if (concluido) {
            fechar()
            return
        }
        abrirPopupConfirmacao(
            requireContext(),
            titulo = "Tem certeza que deseja voltar?",
            descricao = "Se você já pagou, pode sair: o plano é ativado automaticamente assim que o Pix cair.",
            textoConfirmar = "Já paguei"
        ) { verificarAgoraESair() }
    }

    private fun verificarAgoraESair() {
        val id = cobranca?.id ?: return fechar()
        status("Conferindo o pagamento...", carregando = true)
        lancar(aoFalhar = { fechar() }) {
            val c = Supabase.Pagamentos.consultar(id)
            if (c.aprovado) {
                sucesso(c)
            } else {
                toast("Ainda não recebemos o Pix. Assim que ele cair, seu plano é ativado sozinho.")
                parentFragmentManager.setFragmentResult(Resultados.PERFIL_ALTERADO, Bundle())
                fechar()
            }
        }
    }

    private fun status(texto: String, carregando: Boolean, erro: Boolean = false) {
        textoStatus.text = texto
        textoStatus.setTextColor(ContextCompat.getColor(requireContext(), if (erro) R.color.red else R.color.black))
        progressoStatus.visibility = if (carregando) View.VISIBLE else View.GONE
    }

}
