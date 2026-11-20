package com.unifit.unifit.cliente.treino

import android.widget.Button
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputLayout
import com.unifit.unifit.R
import com.unifit.unifit.server.PesoExercicio
import com.unifit.unifit.server.Supabase
import com.unifit.unifit.utilitarios.br
import com.unifit.unifit.utilitarios.mostrarExpandido
import com.unifit.unifit.utilitarios.lancar
import com.unifit.unifit.utilitarios.paraDouble
import com.unifit.unifit.utilitarios.texto
import com.unifit.unifit.utilitarios.toast

fun Fragment.abrirEditarPeso(exercicioId: Long, pesoAtual: Double?, aoSalvar: (PesoExercicio) -> Unit) {
    val dialog = BottomSheetDialog(requireContext())
    val sheet = layoutInflater.inflate(R.layout.bottom_sheet_editar_peso, null)
    dialog.setContentView(sheet)

    val campoPeso = sheet.findViewById<TextInputLayout>(R.id.textInputLayout11)
    if (pesoAtual != null && pesoAtual > 0) campoPeso.texto = pesoAtual.br(1)

    val btnSalvar = sheet.findViewById<Button>(R.id.btn_save_peso)
    btnSalvar.setOnClickListener {
        val peso = campoPeso.texto.paraDouble()
        if (peso == null) {
            campoPeso.error = "Digite um peso válido"
            return@setOnClickListener
        }
        btnSalvar.isEnabled = false
        lancar(aoFalhar = { msg ->
            campoPeso.error = msg
            btnSalvar.isEnabled = true
        }) {
            val registro = Supabase.PesosExercicio.adicionarPeso(exercicioId, peso)
            aoSalvar(registro)
            toast("Peso registrado: ${registro.peso.br(1)} kg")
            dialog.dismiss()
        }
    }

    dialog.mostrarExpandido()
}

fun textoPeso(peso: Double?, espaco: Boolean = true): String =
    if (peso == null || peso <= 0) (if (espaco) "0 Kg" else "0Kg") else "${peso.br(1)}${if (espaco) " " else ""}Kg"
