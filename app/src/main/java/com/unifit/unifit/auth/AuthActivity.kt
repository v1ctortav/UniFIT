package com.unifit.unifit.auth

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.unifit.unifit.R
import com.unifit.unifit.server.Supabase
import com.unifit.unifit.utilitarios.abrirAreaDoUsuario
import kotlinx.coroutines.launch

class AuthActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_auth)

        val veioDoPerfil = intent.getBooleanExtra("recuperar_senha_perfil", false)

        if (!veioDoPerfil) {
            lifecycleScope.launch {
                Supabase.aguardarSessao()
                if (Supabase.estaLogado()) {
                    try {
                        abrirAreaDoUsuario(Supabase.Usuarios.meuPerfil())
                    } catch (e: Exception) {
                        Supabase.Autenticacao.sair()
                    }
                }
            }
        }

        if (savedInstanceState == null) {
            val telaInicial: Fragment = if (veioDoPerfil) ResetPasswordFragment() else WelcomeFragment()
            supportFragmentManager.beginTransaction()
                .replace(R.id.frame_conteudo, telaInicial)
                .commit()
        }
    }

    fun goTo(secao: Fragment) {
        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.frame_conteudo, secao)
            .addToBackStack(null)
            .commit()
    }
}
