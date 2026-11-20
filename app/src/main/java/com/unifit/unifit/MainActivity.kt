package com.unifit.unifit

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.unifit.unifit.server.Supabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import com.unifit.unifit.cliente.aulas.AulasFragment
import com.unifit.unifit.cliente.home.HomeFragment
import com.unifit.unifit.cliente.perfil.PerfilFragment
import com.unifit.unifit.cliente.progresso.ProgressoFragment
import com.unifit.unifit.cliente.treino.TreinosFragment

class MainActivity : AppCompatActivity() {

    lateinit var navBarContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.frame_conteudo_main, HomeFragment())
                .commit()
        }

        toggleNavBarItem()

        supportFragmentManager.addOnBackStackChangedListener {
            atualizarVisibilidadeTelaCheia()
        }

        atualizarVisibilidadeTelaCheia()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    runCatching { Supabase.Presenca.ping() }
                    delay(60_000)
                }
            }
        }
    }

     fun toggleContentMainFrame(content: Fragment) {
        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.frame_conteudo_main, content)
            .addToBackStack(null)
            .commit()
    }

    fun toggleNavBarItem() {
        val listaNavBtns = listOf(
            findViewById<ImageButton>(R.id.btn_nav_home),
            findViewById<ImageButton>(R.id.btn_nav_aulas),
            findViewById<ImageButton>(R.id.btn_nav_treino),
            findViewById<ImageButton>(R.id.btn_nav_progresso),
            findViewById<ImageButton>(R.id.btn_nav_perfil)
        )

        listaNavBtns.forEach { btnNavBar ->
            btnNavBar.isSelected = (btnNavBar == listaNavBtns[0])
            btnNavBar.setOnClickListener {
                if (btnNavBar.isSelected) return@setOnClickListener
                listaNavBtns.forEach { it.isSelected = (it == btnNavBar) }
                btnNavBar.setBackgroundResource(R.drawable.bg_arredondado)

                val content = when (btnNavBar.id) {
                    R.id.btn_nav_home -> HomeFragment()
                    R.id.btn_nav_aulas -> AulasFragment()
                    R.id.btn_nav_treino -> TreinosFragment()
                    R.id.btn_nav_progresso -> ProgressoFragment()
                    R.id.btn_nav_perfil -> PerfilFragment()
                    else -> HomeFragment()
                }
                toggleContentMainFrame(content)
            }
        }
    }
    fun SetNavBarItem(tela: String) {
        val listaNavBtns = listOf(
            findViewById<ImageButton>(R.id.btn_nav_home),
            findViewById<ImageButton>(R.id.btn_nav_aulas),
            findViewById<ImageButton>(R.id.btn_nav_treino),
            findViewById<ImageButton>(R.id.btn_nav_progresso),
            findViewById<ImageButton>(R.id.btn_nav_perfil)
        )

        val btnIdSelecionado = when (tela) {
            "Home" -> R.id.btn_nav_home
            "Aulas" -> R.id.btn_nav_aulas
            "Treino" -> R.id.btn_nav_treino
            "Progresso" -> R.id.btn_nav_progresso
            "Perfil" -> R.id.btn_nav_perfil
            else -> return
        }

        listaNavBtns.forEach { btn ->
            btn.isSelected = (btn.id == btnIdSelecionado)

            btn.setBackgroundResource(
                if (btn.isSelected) R.drawable.bg_arredondado
                else 0
            )
        }
    }

    fun toggleContentMainFrameFullScreen(content: Fragment) {
        navBarContainer = findViewById(R.id.menu_bar)
        navBarContainer.visibility = View.GONE

        val frame = findViewById<FragmentContainerView>(R.id.frame_conteudo_tela_cheia_main)
        frame.visibility = View.VISIBLE

        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.frame_conteudo_tela_cheia_main, content)
            .addToBackStack(null)
            .commit()
    }

    fun closeFullScreenFrame() {
        supportFragmentManager.popBackStack()
    }

    fun fecharTodasTelasCheias() {
        val fm = supportFragmentManager
        while (fm.findFragmentById(R.id.frame_conteudo_tela_cheia_main) != null && fm.backStackEntryCount > 0) {
            if (!fm.popBackStackImmediate()) break
        }
    }

    private fun atualizarVisibilidadeTelaCheia() {
        val fragmento = supportFragmentManager.findFragmentById(
            R.id.frame_conteudo_tela_cheia_main
        )

        val telaCheiaAberta = fragmento?.isAdded == true

        val frame = findViewById<FragmentContainerView>(
            R.id.frame_conteudo_tela_cheia_main
        )

        navBarContainer = findViewById(R.id.menu_bar)

        frame.visibility =
            if (telaCheiaAberta) View.VISIBLE else View.GONE

        navBarContainer.visibility =
            if (telaCheiaAberta) View.GONE else View.VISIBLE
    }

}