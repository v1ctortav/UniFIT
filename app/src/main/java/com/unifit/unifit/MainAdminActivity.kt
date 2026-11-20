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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import com.unifit.unifit.admin.alunos.AlunosFragment
import com.unifit.unifit.admin.aulas.AulasAdminFragment
import com.unifit.unifit.admin.home.HomeAdminFragment
import com.unifit.unifit.admin.perfil.PerfilAdminFragment
import com.unifit.unifit.admin.treinos.TreinosAdminFragment
import com.unifit.unifit.cliente.aulas.AulasFragment
import com.unifit.unifit.cliente.home.HomeFragment
import com.unifit.unifit.cliente.perfil.PerfilFragment
import com.unifit.unifit.cliente.progresso.ProgressoFragment
import com.unifit.unifit.cliente.treino.TreinosFragment

class MainAdminActivity : AppCompatActivity() {
    lateinit var navBarContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_admin)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.frame_conteudo_main_admin, HomeAdminFragment())
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
            .replace(R.id.frame_conteudo_main_admin, content)
            .addToBackStack(null)
            .commit()
    }

    fun toggleNavBarItem() {
        val listaNavBtns = listOf(
            findViewById<ImageButton>(R.id.btn_nav_home_admin),
            findViewById<ImageButton>(R.id.btn_nav_alunos),
            findViewById<ImageButton>(R.id.btn_nav_treino_admin),
            findViewById<ImageButton>(R.id.btn_nav_aulas_admin),
            findViewById<ImageButton>(R.id.btn_nav_perfil_admin)
        )

        listaNavBtns.forEach { btnNavBar ->
            btnNavBar.isSelected = (btnNavBar == listaNavBtns[0])
            btnNavBar.setOnClickListener {
                if (btnNavBar.isSelected) return@setOnClickListener
                listaNavBtns.forEach { it.isSelected = (it == btnNavBar) }
                btnNavBar.setBackgroundResource(R.drawable.bg_arredondado)

                val content = when (btnNavBar.id) {
                    R.id.btn_nav_home_admin -> HomeAdminFragment()
                    R.id.btn_nav_alunos -> AlunosFragment()
                    R.id.btn_nav_treino_admin -> TreinosAdminFragment()
                    R.id.btn_nav_aulas_admin -> AulasAdminFragment()
                    R.id.btn_nav_perfil_admin -> PerfilAdminFragment()
                    else -> HomeAdminFragment()
                }
                toggleContentMainFrame(content)
            }
        }
    }
    fun SetNavBarItem(tela: String) {
        val listaNavBtns = listOf(
            findViewById<ImageButton>(R.id.btn_nav_home_admin),
            findViewById<ImageButton>(R.id.btn_nav_alunos),
            findViewById<ImageButton>(R.id.btn_nav_treino_admin),
            findViewById<ImageButton>(R.id.btn_nav_aulas_admin),
            findViewById<ImageButton>(R.id.btn_nav_perfil_admin)
        )

        val btnIdSelecionado = when (tela) {
            "Home" -> R.id.btn_nav_home_admin
            "Alunos" -> R.id.btn_nav_alunos
            "Treino" -> R.id.btn_nav_treino_admin
            "Aulas" -> R.id.btn_nav_aulas_admin
            "Perfil" -> R.id.btn_nav_perfil_admin
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
        navBarContainer = findViewById(R.id.menu_bar_admin)
        navBarContainer.visibility = View.GONE

        val frame = findViewById<FragmentContainerView>(R.id.frame_conteudo_tela_cheia_main_admin)
        frame.visibility = View.VISIBLE

        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.frame_conteudo_tela_cheia_main_admin, content)
            .addToBackStack(null)
            .commit()
    }

    fun closeFullScreenFrame() {
        supportFragmentManager.popBackStack()
    }

    private fun atualizarVisibilidadeTelaCheia() {
        val fragmento = supportFragmentManager.findFragmentById(
            R.id.frame_conteudo_tela_cheia_main_admin
        )

        val telaCheiaAberta = fragmento?.isAdded == true

        val frame = findViewById<FragmentContainerView>(
            R.id.frame_conteudo_tela_cheia_main_admin
        )

        navBarContainer = findViewById(R.id.menu_bar_admin)

        frame.visibility =
            if (telaCheiaAberta) View.VISIBLE else View.GONE

        navBarContainer.visibility =
            if (telaCheiaAberta) View.GONE else View.VISIBLE
    }

}