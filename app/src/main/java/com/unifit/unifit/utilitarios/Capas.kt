package com.unifit.unifit.utilitarios

import android.content.Context
import android.graphics.Outline
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import com.unifit.unifit.R
import com.unifit.unifit.server.Exercicio
import com.unifit.unifit.server.Imagem
import com.unifit.unifit.server.Treino
import com.unifit.unifit.server.normalizar
import kotlin.math.abs

object Capas {

    const val CATEGORIA_PADRAO = "geral"
    private const val MAX_VARIACOES = 30

    private val cache = mutableMapOf<String, List<Int>>()

    private fun chave(categoria: String?): String {
        val c = categoria.orEmpty().normalizar()
        return when {
            c.startsWith("peito") -> "peito"
            c.startsWith("costa") -> "costas"
            c.startsWith("ombro") -> "ombro"
            c.startsWith("perna") || c.contains("inferior") || c.contains("gluteo") -> "pernas"
            c.startsWith("brac") || c.contains("biceps") || c.contains("triceps") -> "bracos"
            c.startsWith("abd") || c.contains("core") -> "abdomen"
            c.startsWith("cardio") || c.contains("aerob") -> "cardio"
            c.startsWith("forc") -> "forca"
            else -> CATEGORIA_PADRAO
        }
    }

    @android.annotation.SuppressLint("DiscouragedApi")
    private fun imagens(context: Context, categoria: String): List<Int> = cache.getOrPut(categoria) {
        val res = context.resources
        (1..MAX_VARIACOES).asSequence()
            .map { res.getIdentifier("capa_${categoria}_$it", "drawable", context.packageName) }
            .takeWhile { it != 0 }
            .toList()
    }

    fun de(context: Context, categoria: String?, semente: Any?): Int {
        val lista = imagens(context, chave(categoria)).ifEmpty { imagens(context, CATEGORIA_PADRAO) }
        if (lista.isEmpty()) return R.drawable.bg_not_found_background_banner
        return lista[abs((semente ?: categoria).hashCode()) % lista.size]
    }

    fun deTreino(context: Context, t: Treino): Int = de(context, t.categoria, t.id ?: t.nome)

    fun deExercicio(context: Context, e: Exercicio): Int = de(context, e.categoria, e.id ?: e.nome)
}

fun View.arredondar(raioDp: Float = 16f) {
    val raio = raioDp * resources.displayMetrics.density
    outlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) =
            outline.setRoundRect(0, 0, view.width, view.height, raio)
    }
    clipToOutline = true
}

fun ImageView.mostrarCapaTreino(treino: Treino, raioDp: Float? = null) {
    scaleType = ImageView.ScaleType.CENTER_CROP
    setImageResource(Capas.deTreino(context, treino))
    raioDp?.let { arredondar(it) }
}

fun ImageView.mostrarIconeExercicio(exercicio: Exercicio?, raioDp: Float = 16f) {
    scaleType = ImageView.ScaleType.CENTER_CROP
    imageTintList = null
    setPadding(0, 0, 0, 0)
    val bmp = Imagem.base64ParaBitmap(exercicio?.imagem)
    when {
        bmp != null -> setImageBitmap(bmp)
        exercicio != null -> setImageResource(Capas.deExercicio(context, exercicio))
        else -> setImageResource(Capas.de(context, null, null))
    }
    arredondar(raioDp)
}
