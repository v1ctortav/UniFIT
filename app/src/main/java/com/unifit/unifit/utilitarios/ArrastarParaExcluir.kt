package com.unifit.unifit.utilitarios

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.unifit.unifit.R
import kotlin.math.abs
import kotlin.math.min

class ArrastarParaExcluir(
    private val podeArrastar: (RecyclerView.ViewHolder) -> Boolean,
    private val aoExcluir: (posicao: Int) -> Unit
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

    private val pintura = Paint(Paint.ANTI_ALIAS_FLAG)
    private val retangulo = RectF()

    override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, alvo: RecyclerView.ViewHolder) = false

    override fun getSwipeDirs(rv: RecyclerView, vh: RecyclerView.ViewHolder): Int =
        if (podeArrastar(vh)) super.getSwipeDirs(rv, vh) else 0

    override fun getSwipeThreshold(vh: RecyclerView.ViewHolder) = 0.4f

    override fun onSwiped(vh: RecyclerView.ViewHolder, direcao: Int) {
        val pos = vh.bindingAdapterPosition
        if (pos != RecyclerView.NO_POSITION) aoExcluir(pos)
    }

    override fun onChildDraw(
        c: Canvas, rv: RecyclerView, vh: RecyclerView.ViewHolder,
        dX: Float, dY: Float, acao: Int, ativo: Boolean
    ) {
        val item = vh.itemView
        if (acao == ItemTouchHelper.ACTION_STATE_SWIPE && dX < 0) {
            val ctx = item.context
            val dp = item.resources.displayMetrics.density
            val vermelho = ContextCompat.getColor(ctx, R.color.red)
            val progresso = min(1f, abs(dX) / item.width)

            retangulo.set(item.left + 8 * dp, item.top + 6 * dp, item.right - 8 * dp, item.bottom - 6 * dp)
            pintura.shader = LinearGradient(
                retangulo.left, 0f, retangulo.right, 0f,
                intArrayOf(android.graphics.Color.TRANSPARENT, (vermelho and 0x00FFFFFF) or (0x99 shl 24), vermelho),
                floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP
            )
            pintura.alpha = (80 + 175 * progresso).toInt().coerceAtMost(255)
            c.drawRoundRect(retangulo, 16 * dp, 16 * dp, pintura)

            ContextCompat.getDrawable(ctx, R.drawable.ic_lixeira)?.mutate()?.let { icone ->
                icone.setTint(android.graphics.Color.WHITE)
                val tamanho = (28 * dp).toInt()
                val margem = (28 * dp).toInt()
                val topo = item.top + (item.height - tamanho) / 2
                icone.setBounds(item.right - margem - tamanho, topo, item.right - margem, topo + tamanho)
                icone.alpha = (255 * min(1f, progresso * 3)).toInt()
                icone.draw(c)
            }
            item.alpha = 1f - progresso * 0.6f
        } else {
            item.alpha = 1f
        }
        super.onChildDraw(c, rv, vh, dX, dY, acao, ativo)
    }

    override fun clearView(rv: RecyclerView, vh: RecyclerView.ViewHolder) {
        super.clearView(rv, vh)
        vh.itemView.alpha = 1f
    }

    companion object {
        fun ligar(rv: RecyclerView, podeArrastar: (RecyclerView.ViewHolder) -> Boolean, aoExcluir: (Int) -> Unit) =
            ItemTouchHelper(ArrastarParaExcluir(podeArrastar, aoExcluir)).attachToRecyclerView(rv)
    }
}
