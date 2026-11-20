package com.unifit.unifit.admin.aulas

import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputLayout
import com.unifit.unifit.R
import com.unifit.unifit.server.Aula
import com.unifit.unifit.server.Supabase
import com.unifit.unifit.server.Tempo
import com.unifit.unifit.utilitarios.lancar
import com.unifit.unifit.utilitarios.mostrarExpandido
import com.unifit.unifit.utilitarios.texto
import com.unifit.unifit.utilitarios.toast
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun Fragment.abrirFormularioAula(aula: Aula?, dataPadrao: LocalDate, aoSalvar: () -> Unit) {
    val dialog = BottomSheetDialog(requireContext())
    val view = layoutInflater.inflate(R.layout.bottomsheet_aulas_form_admin, null)
    dialog.setContentView(view)

    val campoNome = view.findViewById<TextInputLayout>(R.id.textInputLayout18)
    val campoData = view.findViewById<TextInputLayout>(R.id.textInputLayout19)
    val campoInicio = view.findViewById<TextInputLayout>(R.id.textInputLayout20)
    val campoFim = view.findViewById<TextInputLayout>(R.id.textInputLayout21)
    val campoVagas = view.findViewById<TextInputLayout>(R.id.textInputLayout22)
    val btnSalvar = view.findViewById<Button>(R.id.btn_confirmar_form_aula)

    if (aula != null) {
        view.findViewById<TextView>(R.id.text_label_form_aula).text = "Editar aula"
        btnSalvar.text = "Salvar edições"
        campoNome.texto = aula.nome
        campoData.texto = Tempo.dataBr(aula.data)
        campoInicio.texto = aula.horarioInicio
        campoFim.texto = aula.horarioFim
        campoVagas.texto = aula.vagas.toString()
    } else {
        campoData.texto = dataPadrao.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    }

    view.findViewById<ImageButton>(R.id.btn_cancelar_form_aula).setOnClickListener { dialog.dismiss() }

    btnSalvar.setOnClickListener {
        listOf(campoData, campoInicio, campoFim, campoVagas).forEach { it.error = null }

        val data = lerData(campoData.texto)
        if (data == null) {
            campoData.error = "Use o formato dd/mm/aa"
            return@setOnClickListener
        }
        val inicio = lerHora(campoInicio.texto)
        val fim = lerHora(campoFim.texto)
        if (inicio == null) { campoInicio.error = "00:00"; return@setOnClickListener }
        if (fim == null) { campoFim.error = "00:00"; return@setOnClickListener }
        val vagas = campoVagas.texto.trim().toIntOrNull()
        if (vagas == null) { campoVagas.error = "Número de vagas"; return@setOnClickListener }

        val nova = Aula(
            id = aula?.id,
            nome = campoNome.texto.trim(),
            data = Tempo.dataBanco(data),
            horarioInicio = inicio,
            horarioFim = fim,
            vagas = vagas
        )

        btnSalvar.isEnabled = false
        lancar(aoFalhar = { msg ->
            toast(msg)
            btnSalvar.isEnabled = true
        }) {
            if (aula == null) Supabase.Aulas.criar(nova) else Supabase.Aulas.atualizar(nova)
            toast(if (aula == null) "Aula criada" else "Aula atualizada")
            dialog.dismiss()
            aoSalvar()
        }
    }

    dialog.mostrarExpandido()
}

private fun lerData(texto: String): LocalDate? {
    val t = texto.trim()
    for (padrao in listOf("d/M/yyyy", "d/M/yy")) {
        val r = runCatching { LocalDate.parse(t, DateTimeFormatter.ofPattern(padrao)) }.getOrNull()
        if (r != null) return if (r.year < 100) r.plusYears(2000) else r
    }
    return null
}

private fun lerHora(texto: String): String? {
    val t = texto.trim().replace("h", ":").replace(".", ":")
    val partes = if (t.contains(":")) t.split(":") else if (t.length == 4) listOf(t.take(2), t.drop(2)) else return null
    val h = partes.getOrNull(0)?.toIntOrNull() ?: return null
    val m = partes.getOrNull(1)?.ifEmpty { "0" }?.toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59) return null
    return "%02d:%02d".format(h, m)
}
