package com.unifit.unifit.utilitarios

import android.net.Uri
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.unifit.unifit.R
import com.unifit.unifit.server.Imagem
import com.unifit.unifit.server.Supabase

fun Fragment.abrirEditarFotoPerfil(seletor: SeletorImagem, fotoAtual: String?, aoSalvar: (String) -> Unit) {
    val dialog = BottomSheetDialog(requireContext())
    val sheet = layoutInflater.inflate(R.layout.bottomsheet_editar_foto_perfil, null)
    dialog.setContentView(sheet)

    val previa = sheet.findViewById<ImageView>(R.id.imageView9)
    previa.mostrarBase64(fotoAtual)

    var escolhida: Uri? = null

    sheet.findViewById<ImageButton>(R.id.btn_back_to_perfil_editar_foto).setOnClickListener { dialog.dismiss() }

    sheet.findViewById<Button>(R.id.button3).setOnClickListener {
        seletor.abrir { uri ->
            escolhida = uri
            previa.setImageURI(uri)
        }
    }

    val btnConfirmar = sheet.findViewById<Button>(R.id.button7)
    btnConfirmar.setOnClickListener {
        val uri = escolhida
        if (uri == null) {
            toast("Escolha uma foto em \"Subir foto\"")
            return@setOnClickListener
        }
        btnConfirmar.isEnabled = false
        lancar(aoFalhar = { msg ->
            toast(msg)
            btnConfirmar.isEnabled = true
        }) {
            val base64 = Imagem.uriParaBase64(requireContext(), uri)
            Supabase.Usuarios.alterarFotoPerfil(base64)
            aoSalvar(base64)
            toast("Foto de perfil atualizada")
            dialog.dismiss()
        }
    }

    dialog.mostrarExpandido()
}
