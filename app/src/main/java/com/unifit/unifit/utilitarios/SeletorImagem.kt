package com.unifit.unifit.utilitarios

import android.net.Uri
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment

class SeletorImagem(fragment: Fragment) {

    private var aoEscolher: ((Uri) -> Unit)? = null

    private val launcher = fragment.registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) aoEscolher?.invoke(uri)
        aoEscolher = null
    }

    fun abrir(aoEscolher: (Uri) -> Unit) {
        this.aoEscolher = aoEscolher
        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
}
