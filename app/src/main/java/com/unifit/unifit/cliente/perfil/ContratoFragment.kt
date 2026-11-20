package com.unifit.unifit.cliente.perfil

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.unifit.unifit.MainActivity
import com.unifit.unifit.R
import com.unifit.unifit.server.Supabase
import com.unifit.unifit.utilitarios.lancar
import com.unifit.unifit.utilitarios.toast

class ContratoFragment : Fragment() {

    private var textoContrato: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_contrato, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ImageButton>(R.id.btn_back_perfil_contrato).setOnClickListener {
            (requireActivity() as MainActivity).closeFullScreenFrame()
        }

        val folha = view.findViewById<ImageView>(R.id.imageView8)
        folha.scaleX = 1f
        folha.setOnClickListener { textoContrato?.let { abrirTextoCompleto(it) } }

        val progresso = view.findViewById<View>(R.id.progresso_contrato)
        lancar(aoFalhar = { msg -> progresso.visibility = View.GONE; toast(msg) }) {
            val texto = Supabase.Planos.meuContrato()?.texto ?: "Nenhum contrato encontrado para esta conta."
            textoContrato = texto
            folha.post {
                if (isAdded) {
                    desenharFolha(folha, texto)
                    progresso.visibility = View.GONE
                }
            }
        }
    }

    private fun desenharFolha(imageView: ImageView, texto: String) {
        val largura = imageView.width
        val altura = imageView.height
        if (largura <= 0 || altura <= 0) return

        val densidade = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 1f, resources.displayMetrics)
        val margem = (16 * resources.displayMetrics.density).toInt()
        val fonte = ResourcesCompat.getFont(requireContext(), R.font.satoshi_regular) ?: Typeface.DEFAULT

        val paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            typeface = fonte
        }

        var tamanhoSp = 14f
        var layout: StaticLayout
        do {
            paint.textSize = tamanhoSp * densidade
            layout = StaticLayout.Builder.obtain(texto, 0, texto.length, paint, largura - 2 * margem)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.15f)
                .build()
            tamanhoSp -= 0.5f
        } while (layout.height > altura - 2 * margem && tamanhoSp >= 7f)

        val bitmap = Bitmap.createBitmap(largura, altura, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        canvas.translate(margem.toFloat(), margem.toFloat())
        layout.draw(canvas)

        imageView.setImageBitmap(bitmap)
    }

    private fun abrirTextoCompleto(texto: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Contrato")
            .setMessage(texto)
            .setPositiveButton("Fechar", null)
            .show()
    }
}
