package com.unifit.unifit.utilitarios

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.LruCache
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.unifit.unifit.R
import com.unifit.unifit.server.Youtube
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class YoutubePlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var aoMudarEstado: ((tocando: Boolean) -> Unit)? = null

    var aoTocarNaArea: (() -> Unit)? = null

    var tocando = false
        private set

    private var idVideo: String? = null
    private var jaCarregou = false
    private var pronto = false
    private var tocarQuandoPronto = false
    private var telaCheia: Dialog? = null
    private val principal = Handler(Looper.getMainLooper())
    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val webView: WebView = criarWebView()
    private val capa = ImageView(context).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
    }
    private val girando = ProgressBar(context).apply {
        isIndeterminate = true
        indeterminateTintList = ColorStateList.valueOf(Color.WHITE)
    }
    private val palco = FrameLayout(context)
    private val bloqueio = View(context).apply {
        isClickable = true
        setOnClickListener { (aoTocarNaArea ?: ::alternar).invoke() }
    }
    private var btnPlayPause: ImageButton? = null
    private var btnTelaCheia: ImageButton? = null

    init {
        setBackgroundColor(Color.BLACK)
        palco.addView(webView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        palco.addView(capa, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        val tamanho = (40 * resources.displayMetrics.density).toInt()
        palco.addView(girando, LayoutParams(tamanho, tamanho, Gravity.CENTER))
        addView(palco, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(bloqueio, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    fun vincularBotoes(playPause: ImageButton, telaCheia: ImageButton) {
        btnPlayPause = playPause
        btnTelaCheia = telaCheia
        playPause.setOnClickListener { alternar() }
        telaCheia.setOnClickListener { entrarTelaCheia() }
        atualizarIcones()
    }

    fun carregar(link: String?) {
        val id = link?.let { Youtube.idDoVideo(it) }
        if (jaCarregou && id == idVideo) return
        jaCarregou = true
        idVideo = id
        pronto = false
        tocando = false
        tocarQuandoPronto = false
        mostrarCapa(true)
        atualizarIcones()

        if (id == null) {
            webView.loadUrl("about:blank")
            capa.setImageResource(R.drawable.bg_not_found_background_banner)
            carregando(false)
            return
        }
        carregando(true)
        MiniaturaYoutube.carregar(escopo, capa, id) { if (!tocarQuandoPronto && !tocando) carregando(false) }
        webView.loadDataWithBaseURL(origem(), html(id), "text/html", "utf-8", null)
    }

    val temVideo: Boolean get() = idVideo != null

    fun alternar() = if (tocando) pausar() else tocar()

    fun tocar() {
        if (idVideo == null) {
            Toast.makeText(context, "Este exercício ainda não tem vídeo", Toast.LENGTH_SHORT).show()
            return
        }
        mostrarCapa(false)
        carregando(true)
        if (pronto) js("tocar()") else tocarQuandoPronto = true
    }

    fun pausar() {
        tocarQuandoPronto = false
        if (pronto) js("pausar()")
    }

    fun entrarTelaCheia() {
        if (idVideo == null) {
            Toast.makeText(context, "Este exercício ainda não tem vídeo", Toast.LENGTH_SHORT).show()
            return
        }
        if (telaCheia != null) return
        val activity = context.acharActivity() ?: return
        val estavaTocando = tocando || tocarQuandoPronto
        val orientacaoAnterior = activity.requestedOrientation

        val raiz = FrameLayout(activity).apply { setBackgroundColor(Color.BLACK) }
        removeView(palco)
        raiz.addView(palco, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        raiz.addView(View(activity).apply {
            isClickable = true
            setOnClickListener { alternar() }
        }, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        val dp = resources.displayMetrics.density
        val pilula = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundResource(R.drawable.bg_arredondado)
            backgroundTintList = ColorStateList.valueOf(Color.BLACK)
            setPadding((16 * dp).toInt(), (8 * dp).toInt(), (16 * dp).toInt(), (8 * dp).toInt())
        }
        val tamanho = (25 * dp).toInt()
        val btnPlay = ImageButton(activity).apply {
            background = null
            setPadding(0, 0, 0, 0)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setOnClickListener { alternar() }
        }
        val btnSair = ImageButton(activity).apply {
            background = null
            setPadding(0, 0, 0, 0)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setImageResource(R.drawable.ic_fs_sair)
        }
        pilula.addView(btnPlay, LinearLayout.LayoutParams(tamanho, tamanho))
        pilula.addView(View(activity), LinearLayout.LayoutParams((24 * dp).toInt(), 1))
        pilula.addView(btnSair, LinearLayout.LayoutParams(tamanho, tamanho))
        raiz.addView(pilula, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = (24 * dp).toInt()
        })

        val iconeTelaCheia = { btnPlay.setImageResource(if (tocando) R.drawable.ic_pause else R.drawable.ic_play) }
        iconeTelaCheia()
        val ouvinteAnterior = aoMudarEstado
        aoMudarEstado = { t -> iconeTelaCheia(); ouvinteAnterior?.invoke(t) }

        val dialog = Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(raiz)
        dialog.setOnDismissListener {
            raiz.removeView(palco)
            addView(palco, 0, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            aoMudarEstado = ouvinteAnterior
            activity.requestedOrientation = orientacaoAnterior
            telaCheia = null
            atualizarIcones()
            if (tocando) principal.postDelayed({ js("tocar()") }, 300)
        }
        btnSair.setOnClickListener { dialog.dismiss() }
        telaCheia = dialog

        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        dialog.show()
        dialog.window?.let { w ->
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            WindowCompat.getInsetsController(w, w.decorView).apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        if (estavaTocando) principal.postDelayed({ js("tocar()") }, 300)
    }

    fun liberar() {
        telaCheia?.dismiss()
        escopo.cancel()
        webView.stopLoading()
        webView.loadUrl("about:blank")
        webView.removeJavascriptInterface("UniFit")
        webView.destroy()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun criarWebView() = WebView(context).apply {
        setBackgroundColor(Color.BLACK)
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        webChromeClient = WebChromeClient()
        webViewClient = WebViewClient()
        addJavascriptInterface(Ponte(), "UniFit")
    }

    private fun origem() = "https://${context.packageName}"

    private fun html(id: String) = """
        <!DOCTYPE html><html><head>
        <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
        <style>
          html,body{margin:0;padding:0;width:100%;height:100%;background:#000;overflow:hidden}
          #player{position:absolute;top:0;left:0;width:100%;height:100%}
        </style></head><body>
        <div id="player"></div>
        <script src="https://www.youtube.com/iframe_api"></script>
        <script>
          var player;
          function onYouTubeIframeAPIReady(){
            player = new YT.Player('player', {
              width:'100%', height:'100%', videoId:'$id',
              playerVars:{controls:0, disablekb:1, fs:0, rel:0, playsinline:1, iv_load_policy:3,
                          modestbranding:1, cc_load_policy:0, hl:'pt-BR', origin:'${origem()}', widget_referrer:'${origem()}'},
              events:{
                onReady:function(){ UniFit.pronto(); },
                onStateChange:function(e){
                  if (e.data == 1) { try { player.unloadModule('captions'); player.unloadModule('cc'); } catch(x){} }
                  UniFit.estado(e.data);
                },
                onError:function(e){ UniFit.erro(e.data); }
              }
            });
          }
          function tocar(){ if(player && player.playVideo) player.playVideo(); }
          function pausar(){ if(player && player.pauseVideo) player.pauseVideo(); }
        </script></body></html>
    """.trimIndent()

    private fun js(codigo: String) = webView.evaluateJavascript(codigo, null)

    private inner class Ponte {
        @JavascriptInterface
        fun pronto() = principal.post {
            pronto = true
            if (tocarQuandoPronto) {
                tocarQuandoPronto = false
                js("tocar()")
            }
        }

        @JavascriptInterface
        fun estado(e: Int) = principal.post {
            val agoraTocando = e == 1 || e == 3
            if (e == 3) carregando(true)
            if (e == 1 || e == 2 || e == 0) carregando(false)
            if (e == 1) mostrarCapa(false)
            if (e == 2 || e == 0) mostrarCapa(true)
            if (agoraTocando != tocando) {
                tocando = agoraTocando
                atualizarIcones()
                aoMudarEstado?.invoke(tocando)
            }
        }

        @JavascriptInterface
        fun erro(codigo: Int) = principal.post {
            tocando = false
            tocarQuandoPronto = false
            carregando(false)
            mostrarCapa(true)
            atualizarIcones()
            Toast.makeText(context, "Não foi possível reproduzir este vídeo (erro $codigo)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun carregando(sim: Boolean) {
        girando.visibility = if (sim) View.VISIBLE else View.GONE
    }

    private fun mostrarCapa(mostrar: Boolean) {
        capa.visibility = if (mostrar) View.VISIBLE else View.GONE
    }

    private fun atualizarIcones() {
        btnPlayPause?.setImageResource(if (tocando) R.drawable.ic_pause else R.drawable.ic_play)
        btnPlayPause?.alpha = if (idVideo == null) 0.4f else 1f
        btnTelaCheia?.alpha = if (idVideo == null) 0.4f else 1f
    }

    private fun Context.acharActivity(): Activity? {
        var c: Context? = this
        while (c is ContextWrapper) {
            if (c is Activity) return c
            c = c.baseContext
        }
        return null
    }
}

object MiniaturaYoutube {

    private val cache = LruCache<String, Bitmap>(20)

    suspend fun baixar(id: String): Bitmap? {
        cache.get(id)?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                val conexao = URL("https://img.youtube.com/vi/$id/hqdefault.jpg").openConnection() as HttpURLConnection
                conexao.connectTimeout = 8000
                conexao.readTimeout = 8000
                conexao.inputStream.use { BitmapFactory.decodeStream(it) }
            }.getOrNull()
        }?.also { cache.put(id, it) }
    }

    fun carregar(escopo: CoroutineScope, imageView: ImageView, id: String?, aoTerminar: (() -> Unit)? = null) {
        if (id == null) {
            imageView.setImageResource(R.drawable.bg_not_found_background_banner)
            aoTerminar?.invoke()
            return
        }
        imageView.tag = id
        escopo.launch {
            val bmp = baixar(id)
            if (imageView.tag == id) {
                if (bmp != null) imageView.setImageBitmap(bmp)
                else imageView.setImageResource(R.drawable.bg_not_found_background_banner)
                aoTerminar?.invoke()
            }
        }
    }
}
