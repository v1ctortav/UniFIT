package com.unifit.unifit.server

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.LruCache
import android.widget.ImageView
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import java.io.ByteArrayOutputStream
import java.text.Normalizer
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Serializable
data class Usuario(
    val id: String,
    val nome: String,
    val email: String,
    @SerialName("foto_perfil") val fotoPerfil: String? = null,
    @SerialName("is_admin") val isAdmin: Boolean = false,
    @SerialName("visto_em") val vistoEm: String? = null
) {
    val online: Boolean
        get() = vistoEm?.let { runCatching { Duration.between(Tempo.parseInstante(it), Instant.now()).toMinutes() < 2 }.getOrNull() } ?: false
}

@Serializable
data class ResumoOnline(
    @SerialName("alunos_online") val alunosOnline: Int = 0,
    @SerialName("alunos_total") val alunosTotal: Int = 0,
    @SerialName("professores_online") val professoresOnline: Int = 0,
    @SerialName("professores_total") val professoresTotal: Int = 0
)

@Serializable
data class TreinoRealizado(
    val id: Long? = null,
    val nome: String,
    val minutos: Int,
    val exercicios: Int,
    @SerialName("realizado_em") val realizadoEm: String? = null
)

@Serializable
data class Plano(
    @SerialName("user_id") val userId: String,
    @SerialName("nome_plano") val nomePlano: String,
    val status: Boolean,
    @SerialName("data_vencimento") val dataVencimento: String? = null
)

@Serializable
data class OfertaPlano(
    val codigo: String,
    val nome: String,
    val valor: Double,
    val descricao: String = "",
    val ordem: Int = 0
)

@Serializable
data class CobrancaPix(
    val id: String,
    val plano: String,
    @SerialName("nome_plano") val nomePlano: String? = null,
    val valor: Double,
    val status: String,
    @SerialName("qr_code") val qrCode: String? = null,
    @SerialName("qr_code_base64") val qrCodeBase64: String? = null,
    @SerialName("expira_em") val expiraEm: String? = null,
    @SerialName("vencimento_novo") val vencimentoNovo: String? = null
) {
    val aprovado: Boolean get() = status == "aprovado"
    val encerrado: Boolean get() = status == "expirado" || status == "cancelado" || status == "erro"
}

@Serializable
data class Contrato(
    @SerialName("user_id") val userId: String,
    val texto: String,
    @SerialName("criado_em") val criadoEm: String? = null
)

@Serializable
data class Notificacao(
    val id: Long,
    @SerialName("user_id") val userId: String,
    val nome: String,
    val descricao: String,
    @SerialName("tipo_icone") val tipoIcone: String,
    val lida: Boolean,
    @SerialName("criada_em") val criadaEm: String,
    val referencia: String? = null
) {
    val isSistema: Boolean get() = tipoIcone == TIPO_SISTEMA
    val isTreino: Boolean get() = tipoIcone == TIPO_TREINO

    val alunoDoChat: String? get() = referencia?.takeIf { it.startsWith("chat:") }?.removePrefix("chat:")

    val tempoDecorrido: String get() = Tempo.decorrido(criadaEm)

    companion object {
        const val TIPO_SISTEMA = "sistema"
        const val TIPO_GERAL = "geral"
        const val TIPO_TREINO = "treino"
    }
}

@Serializable
private data class NovaNotificacao(
    @SerialName("user_id") val userId: String,
    val nome: String,
    val descricao: String,
    @SerialName("tipo_icone") val tipoIcone: String
)

@Serializable
data class Medidas(
    @SerialName("user_id") val userId: String,
    val peso: Double = 0.0,
    val altura: Double = 0.0,
    @SerialName("gordura_corporal") val gorduraCorporal: Double = 0.0,
    @SerialName("historico_pesos") val historicoPesos: List<Double> = List(12) { 0.0 },
    @SerialName("atualizado_em") val atualizadoEm: String? = null
) {
    companion object {
        val MESES = listOf("Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez")
    }
}

@Serializable
data class Exercicio(
    val id: Long? = null,
    val nome: String,
    val categoria: String,
    val equipamentos: List<String> = emptyList(),
    val repeticoes: Int = 0,
    val series: Int = 0,
    @SerialName("link_youtube") val linkYoutube: String? = null,
    val imagem: String? = null,
    @SerialName("criado_por") val criadoPor: String? = null
)

@Serializable
data class PesoExercicio(
    val id: Long? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("exercicio_id") val exercicioId: Long,
    val peso: Double,
    @SerialName("registrado_em") val registradoEm: String? = null
)

@Serializable
data class ItemTreino(
    @SerialName("exercicio_id") val exercicioId: Long,
    val repeticoes: Int,
    val series: Int
)

@Serializable
data class Treino(
    val id: Long? = null,
    val nome: String,
    val descricao: String = "",
    val categoria: String,
    val exercicios: List<ItemTreino> = emptyList(),
    @SerialName("criado_por") val criadoPor: String? = null,
    @SerialName("dia_semana") val diaSemana: Int? = null
)

@Serializable
data class TreinoAluno(
    val id: Long? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("dia_semana") val diaSemana: Int,
    @SerialName("treino_id") val treinoId: Long? = null,
    val nome: String,
    val descricao: String = "",
    val categoria: String = "",
    val exercicios: List<ItemTreino> = emptyList()
) {
    fun comoTreino() = Treino(id = id, nome = nome, descricao = descricao, categoria = categoria, exercicios = exercicios, diaSemana = diaSemana)
}

object DiaSemana {
    val NOMES = listOf("Domingo", "Segunda-feira", "Terça-feira", "Quarta-feira", "Quinta-feira", "Sexta-feira", "Sábado")
    val CURTOS = listOf("DOM", "SEG", "TER", "QUA", "QUI", "SEX", "SAB")

    fun nome(dia: Int?): String = dia?.let { NOMES.getOrNull(it) } ?: "Sem dia fixo"

    private val SIMPLES = listOf("Domingo", "Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado")

    fun juntar(dias: Collection<Int>): String {
        val ord = dias.distinct().sorted()
        return when (ord.size) {
            0 -> "Sem dia"
            1 -> nome(ord[0])
            else -> ord.dropLast(1).joinToString(", ") { SIMPLES[it] } + " e " + SIMPLES[ord.last()]
        }
    }

    val OPCOES_AGENDA: List<List<Int>> =
        (0..6).map { listOf(it) } + listOf(
            listOf(1, 3), listOf(1, 4), listOf(2, 4), listOf(2, 5), listOf(3, 5), listOf(3, 6),
            listOf(1, 3, 5), listOf(2, 4, 6), listOf(1, 2, 3, 4, 5)
        )

    fun de(data: LocalDate): Int = data.dayOfWeek.value % 7

    fun hoje(): Int = de(LocalDate.now())
}

data class TreinoCompleto(
    val treino: Treino,
    val itens: List<Pair<ItemTreino, Exercicio?>>
)

@Serializable
data class Aula(
    val id: Long? = null,
    @SerialName("criado_por") val criadoPor: String? = null,
    val nome: String,
    val data: String,
    @SerialName("horario_inicio") val horarioInicio: String,
    @SerialName("horario_fim") val horarioFim: String,
    val vagas: Int
)

@Serializable
data class AulaDetalhada(
    val id: Long,
    @SerialName("criado_por") val criadoPor: String,
    val nome: String,
    val data: String,
    @SerialName("horario_inicio") val horarioInicio: String,
    @SerialName("horario_fim") val horarioFim: String,
    val vagas: Int,
    @SerialName("vagas_ocupadas") val vagasOcupadas: Int,
    @SerialName("vagas_disponiveis") val vagasDisponiveis: Int,
    val inscrito: Boolean,
    @SerialName("nome_instrutor") val nomeInstrutor: String? = null
) {
    val lotada: Boolean get() = vagasDisponiveis <= 0
    fun paraAula() = Aula(id, criadoPor, nome, data, horarioInicio, horarioFim, vagas)
}

@Serializable
data class InscritoAula(
    @SerialName("user_id") val userId: String,
    val usuarios: UsuarioResumo? = null
)

@Serializable
data class UsuarioResumo(val nome: String, val email: String)

@Serializable
data class Mensagem(
    val id: Long,
    @SerialName("aluno_id") val alunoId: String,
    @SerialName("autor_id") val autorId: String,
    val texto: String,
    val lida: Boolean = false,
    @SerialName("criada_em") val criadaEm: String,
    val apagada: Boolean = false
) {
    val doAluno: Boolean get() = autorId == alunoId
}

@Serializable
data class Conversa(
    @SerialName("aluno_id") val alunoId: String,
    @SerialName("ultima_em") val ultimaEm: String,
    @SerialName("ultima_mensagem") val ultimaMensagem: String,
    @SerialName("nao_lidas_admin") val naoLidasAdmin: Int = 0,
    @SerialName("nao_lidas_aluno") val naoLidasAluno: Int = 0
)

data class Autor(val nome: String, val foto: String?, val isAdmin: Boolean)

object Supabase {

    private const val SUPABASE_URL = "https://oumwmakoduhavlirjudr.supabase.co"
    private const val SUPABASE_ANON_KEY = "sb_publishable_wi0OP0dCFHdVR9bw0aBAaQ_Z6hDcKfr"

    val client: SupabaseClient by lazy {
        createSupabaseClient(SUPABASE_URL, SUPABASE_ANON_KEY) {
            defaultSerializer = KotlinXSerializer(Json {
                ignoreUnknownKeys = true
                encodeDefaults = false
            })
            install(Auth)
            install(Postgrest)
            install(Realtime)
        }
    }

    suspend fun aguardarSessao() = client.auth.awaitInitialization()

    fun idUsuarioLogado(): String? = client.auth.currentUserOrNull()?.id

    fun estaLogado(): Boolean = client.auth.currentSessionOrNull() != null

    private fun uid(): String = idUsuarioLogado() ?: throw SupabaseErro("Sessão expirada. Faça login novamente.")

    object Autenticacao {

        suspend fun cadastrar(nome: String, email: String, senha: String, confirmacaoSenha: String = senha): Boolean {
            validarNome(nome); validarEmail(email); validarSenha(senha)
            if (senha != confirmacaoSenha) throw SupabaseErro("As senhas não conferem")
            client.auth.signUpWith(Email) {
                this.email = email.trim()
                this.password = senha
                this.data = buildJsonObject { put("nome", nome.trim()) }
            }
            return estaLogado()
        }

        suspend fun login(email: String, senha: String): Usuario {
            validarEmail(email)
            if (senha.isBlank()) throw SupabaseErro("Digite a senha")
            client.auth.signInWith(Email) {
                this.email = email.trim()
                this.password = senha
            }
            return Usuarios.meuPerfil()
        }

        suspend fun recuperarSenha(email: String) {
            validarEmail(email)
            client.auth.resetPasswordForEmail(email.trim())
        }

        fun emailLogado(): String = client.auth.currentUserOrNull()?.email.orEmpty()

        suspend fun sair() {
            runCatching { client.auth.signOut() }
            runCatching { client.auth.clearSession() }
        }

        suspend fun verificarSenha(senha: String): Boolean {
            if (senha.isBlank()) return false
            return client.postgrest
                .rpc("verificar_senha", buildJsonObject { put("p_senha", senha) })
                .decodeAs<Boolean>()
        }

        private suspend fun exigirSenha(senhaAtual: String) {
            if (!verificarSenha(senhaAtual)) throw SupabaseErro("Senha atual incorreta")
        }

        suspend fun alterarDados(
            nome: String,
            email: String,
            novaSenha: String?,
            confirmacaoNovaSenha: String?,
            senhaAtual: String
        ): Boolean {
            validarNome(nome); validarEmail(email)
            if (!novaSenha.isNullOrEmpty()) {
                validarSenha(novaSenha)
                if (novaSenha != confirmacaoNovaSenha) throw SupabaseErro("A confirmação não confere com a nova senha")
            }
            exigirSenha(senhaAtual)

            val emailAtual = client.auth.currentUserOrNull()?.email.orEmpty()
            val trocarEmail = !email.trim().equals(emailAtual, ignoreCase = true)

            client.from("usuarios").update({ set("nome", nome.trim()) }) {
                filter { eq("id", uid()) }
            }
            if (trocarEmail || !novaSenha.isNullOrEmpty()) {
                client.auth.updateUser {
                    if (trocarEmail) this.email = email.trim()
                    if (!novaSenha.isNullOrEmpty()) this.password = novaSenha
                }
            }
            return trocarEmail
        }

        suspend fun alterarSenha(senhaAtual: String, novaSenha: String, confirmacao: String) {
            if (novaSenha != confirmacao) throw SupabaseErro("A confirmação não confere com a nova senha")
            if (novaSenha == senhaAtual) throw SupabaseErro("A nova senha precisa ser diferente da atual")
            validarSenha(novaSenha)
            exigirSenha(senhaAtual)
            client.auth.updateUser { password = novaSenha }
        }

        suspend fun excluirConta(senhaAtual: String) {
            if (senhaAtual.isBlank()) throw SupabaseErro("Digite sua senha para confirmar")
            client.postgrest.rpc("excluir_minha_conta", buildJsonObject { put("p_senha", senhaAtual) })
            sair()
        }

        private fun validarNome(nome: String) {
            if (nome.trim().length < 2) throw SupabaseErro("Digite um nome válido")
        }

        private fun validarEmail(email: String) {
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches())
                throw SupabaseErro("Digite um e-mail válido")
        }

        private fun validarSenha(senha: String) {
            if (senha.length < 6) throw SupabaseErro("A senha precisa ter pelo menos 6 caracteres")
        }
    }

    object Usuarios {

        suspend fun meuPerfil(): Usuario =
            client.from("usuarios").select { filter { eq("id", uid()) } }.decodeSingle()

        suspend fun souAdmin(): Boolean = meuPerfil().isAdmin

        suspend fun alterarFotoPerfil(fotoBase64: String) {
            client.from("usuarios").update({ set("foto_perfil", fotoBase64) }) {
                filter { eq("id", uid()) }
            }
        }

        suspend fun alterarFotoPerfil(context: Context, uri: Uri): String {
            val base64 = Imagem.uriParaBase64(context, uri)
            alterarFotoPerfil(base64)
            return base64
        }

        suspend fun removerFotoPerfil() {
            client.from("usuarios").update({ set<String?>("foto_perfil", null) }) {
                filter { eq("id", uid()) }
            }
        }

        suspend fun listarTodos(): List<Usuario> =
            client.from("usuarios").select { order("nome", Order.ASCENDING) }.decodeList()

        suspend fun listarClientes(): List<Usuario> =
            client.from("usuarios").select {
                filter { eq("is_admin", false) }
                order("nome", Order.ASCENDING)
            }.decodeList()

        suspend fun buscar(id: String): Usuario =
            client.from("usuarios").select { filter { eq("id", id) } }.decodeSingle()
    }

    object Planos {

        suspend fun meuPlano(): Plano? =
            client.from("planos").select { filter { eq("user_id", uid()) } }.decodeSingleOrNull()

        suspend fun planoDe(userId: String): Plano? =
            client.from("planos").select { filter { eq("user_id", userId) } }.decodeSingleOrNull()

        suspend fun meuContrato(): Contrato? =
            client.from("contratos").select { filter { eq("user_id", uid()) } }.decodeSingleOrNull()

        suspend fun atualizarPlano(userId: String, nomePlano: String, status: Boolean, dataVencimento: String?) {
            client.from("planos").update({
                set("nome_plano", nomePlano)
                set("status", status)
                set("data_vencimento", dataVencimento)
            }) {
                filter { eq("user_id", userId) }
            }
        }
    }

    object Pagamentos {

        private val http by lazy { io.ktor.client.HttpClient(io.ktor.client.engine.okhttp.OkHttp) }
        private val json = Json { ignoreUnknownKeys = true }

        suspend fun planosDisponiveis(): List<OfertaPlano> =
            client.from("planos_disponiveis").select { order("ordem", Order.ASCENDING) }.decodeList()

        suspend fun gerarPix(): CobrancaPix =
            chamar(buildJsonObject { put("acao", "criar") })

        suspend fun consultar(id: String): CobrancaPix =
            chamar(buildJsonObject { put("acao", "consultar"); put("id", id) })

        private suspend fun chamar(corpo: JsonObject): CobrancaPix {
            val token = client.auth.currentAccessTokenOrNull()
                ?: throw SupabaseErro("Sessão expirada. Faça login novamente.")
            val resposta = http.post("$SUPABASE_URL/functions/v1/pix") {
                header("apikey", SUPABASE_ANON_KEY)
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(corpo.toString())
            }
            val texto = resposta.bodyAsText()
            val dados = runCatching { json.parseToJsonElement(texto) as? JsonObject }.getOrNull()
            dados?.get("erro")?.jsonPrimitive?.contentOrNull?.let { throw SupabaseErro(it) }
            if (!resposta.status.isSuccess() || dados == null) {
                throw SupabaseErro("Não foi possível gerar o pagamento agora. Tente novamente.")
            }
            return json.decodeFromJsonElement(CobrancaPix.serializer(), dados)
        }
    }

    object Notificacoes {

        suspend fun listarMinhas(): List<Notificacao> =
            client.from("notificacoes").select {
                filter { eq("user_id", uid()) }
                order("criada_em", Order.DESCENDING)
            }.decodeList()

        suspend fun contarNaoLidas(): Int = listarMinhas().count { !it.lida }

        suspend fun marcarComoLida(id: Long, lida: Boolean = true) {
            client.from("notificacoes").update({ set("lida", lida) }) { filter { eq("id", id) } }
        }

        suspend fun marcarTodasComoLidas() {
            client.from("notificacoes").update({ set("lida", true) }) {
                filter {
                    eq("user_id", uid())
                    eq("lida", false)
                }
            }
        }

        suspend fun excluir(id: Long) {
            client.from("notificacoes").delete { filter { eq("id", id) } }
        }

        suspend fun enviar(userId: String, nome: String, descricao: String, tipo: String = Notificacao.TIPO_GERAL) {
            require(tipo in listOf(Notificacao.TIPO_SISTEMA, Notificacao.TIPO_GERAL, Notificacao.TIPO_TREINO)) { "tipo inválido" }
            client.from("notificacoes").insert(NovaNotificacao(userId, nome, descricao, tipo))
        }
    }

    object Progresso {

        suspend fun minhasMedidas(): Medidas =
            client.from("medidas").select { filter { eq("user_id", uid()) } }.decodeSingle()

        suspend fun atualizarMedidas(peso: Double, altura: Double, gorduraCorporal: Double): Medidas {
            if (peso <= 0 || peso > 400) throw SupabaseErro("Peso inválido")
            if (altura <= 0 || altura > 2.6) throw SupabaseErro("Altura inválida (use metros, ex.: 1.75)")
            if (gorduraCorporal < 0 || gorduraCorporal > 70) throw SupabaseErro("Gordura corporal inválida")
            return client.from("medidas").update({
                set("peso", peso)
                set("altura", altura)
                set("gordura_corporal", gorduraCorporal)
            }) {
                select()
                filter { eq("user_id", uid()) }
            }.decodeSingle()
        }
    }

    object Exercicios {

        suspend fun listar(comImagem: Boolean = true): List<Exercicio> {
            val colunas = if (comImagem) Columns.ALL
            else Columns.list("id", "nome", "categoria", "equipamentos", "repeticoes", "series", "link_youtube", "criado_por")
            return client.from("exercicios").select(colunas) { order("nome", Order.ASCENDING) }.decodeList()
        }

        suspend fun buscar(id: Long): Exercicio =
            client.from("exercicios").select { filter { eq("id", id) } }.decodeSingle()

        suspend fun buscarVarios(ids: List<Long>): List<Exercicio> {
            if (ids.isEmpty()) return emptyList()
            return client.from("exercicios").select { filter { isIn("id", ids) } }.decodeList()
        }

        suspend fun criar(exercicio: Exercicio): Exercicio {
            validarInfo(exercicio.nome, exercicio.categoria)
            return client.from("exercicios")
                .insert(exercicio.copy(id = null, criadoPor = null)) { select() }
                .decodeSingle()
        }

        suspend fun atualizarInfo(
            id: Long,
            nome: String,
            categoria: String,
            equipamentos: List<String>,
            repeticoes: Int? = null,
            series: Int? = null
        ) {
            validarInfo(nome, categoria)
            val lista = equipamentos.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
            client.from("exercicios").update({
                set("nome", nome.trim())
                set("categoria", categoria.trim())
                set("equipamentos", lista)
                repeticoes?.let { set("repeticoes", it) }
                series?.let { set("series", it) }
            }) {
                filter { eq("id", id) }
            }
        }

        suspend fun atualizarMidia(id: Long, linkYoutube: String?, imagemBase64: String?) {
            val link = linkYoutube?.trim().orEmpty()
            if (link.isNotEmpty() && !Youtube.linkValido(link)) throw SupabaseErro("Link do YouTube inválido")
            client.from("exercicios").update({
                set("link_youtube", link.ifEmpty { null })
                if (imagemBase64 != null) set("imagem", imagemBase64)
            }) {
                filter { eq("id", id) }
            }
        }

        suspend fun atualizarMidia(context: Context, id: Long, linkYoutube: String?, imagemUri: Uri?) {
            val base64 = imagemUri?.let { Imagem.uriParaBase64(context, it) }
            atualizarMidia(id, linkYoutube, base64)
        }

        suspend fun removerImagem(id: Long) {
            client.from("exercicios").update({ set<String?>("imagem", null) }) { filter { eq("id", id) } }
        }

        suspend fun excluir(id: Long) {
            client.from("exercicios").delete { filter { eq("id", id) } }
        }

        private fun validarInfo(nome: String, categoria: String) {
            if (nome.isBlank()) throw SupabaseErro("Digite o nome do exercício")
            if (categoria.isBlank()) throw SupabaseErro("Digite a categoria")
        }
    }

    object PesosExercicio {

        suspend fun adicionarPeso(exercicioId: Long, peso: Double): PesoExercicio {
            if (peso <= 0 || peso > 1000) throw SupabaseErro("Digite um peso válido")
            return client.from("pesos_exercicio")
                .insert(PesoExercicio(exercicioId = exercicioId, peso = peso)) { select() }
                .decodeSingle()
        }

        suspend fun listar(exercicioId: Long): List<PesoExercicio> =
            client.from("pesos_exercicio").select {
                filter {
                    eq("user_id", uid())
                    eq("exercicio_id", exercicioId)
                }
                order("registrado_em", Order.DESCENDING)
            }.decodeList()

        suspend fun ultimoPeso(exercicioId: Long): PesoExercicio? = listar(exercicioId).firstOrNull()

        suspend fun excluir(id: Long) {
            client.from("pesos_exercicio").delete { filter { eq("id", id) } }
        }
    }

    object Treinos {

        suspend fun listar(categoria: String? = null): List<Treino> =
            client.from("treinos").select {
                if (!categoria.isNullOrBlank()) filter { eq("categoria", categoria) }
                order("nome", Order.ASCENDING)
            }.decodeList()

        suspend fun buscar(id: Long): Treino =
            client.from("treinos").select { filter { eq("id", id) } }.decodeSingle()

        suspend fun buscarCompleto(id: Long): TreinoCompleto {
            val treino = buscar(id)
            val exercicios = Exercicios.buscarVarios(treino.exercicios.map { it.exercicioId }.distinct())
                .associateBy { it.id }
            return TreinoCompleto(treino, treino.exercicios.map { it to exercicios[it.exercicioId] })
        }

        suspend fun criar(treino: Treino): Treino {
            validar(treino)
            return client.from("treinos")
                .insert(treino.copy(id = null, criadoPor = null)) { select() }
                .decodeSingle()
        }

        suspend fun atualizar(treino: Treino) {
            val id = treino.id ?: throw SupabaseErro("Treino sem id")
            validar(treino)
            client.from("treinos").update({
                set("nome", treino.nome.trim())
                set("descricao", treino.descricao.trim())
                set("categoria", treino.categoria.trim())
                set("exercicios", treino.exercicios)
                set("dia_semana", treino.diaSemana)
            }) {
                filter { eq("id", id) }
            }
        }

        suspend fun adicionarExercicio(treinoId: Long, item: ItemTreino) {
            val t = buscar(treinoId)
            atualizar(t.copy(exercicios = t.exercicios + item))
        }

        suspend fun removerExercicio(treinoId: Long, exercicioId: Long) {
            val t = buscar(treinoId)
            atualizar(t.copy(exercicios = t.exercicios.filterNot { it.exercicioId == exercicioId }))
        }

        suspend fun excluir(id: Long) {
            client.from("treinos").delete { filter { eq("id", id) } }
        }

        private fun validar(t: Treino) {
            if (t.nome.isBlank()) throw SupabaseErro("Digite o nome do treino")
            if (t.categoria.isBlank()) throw SupabaseErro("Digite a categoria do treino")
            if (t.exercicios.any { it.repeticoes <= 0 || it.series <= 0 })
                throw SupabaseErro("Repetições e séries precisam ser maiores que zero")
        }
    }

    object TreinosAluno {

        suspend fun listarDoAluno(userId: String): List<TreinoAluno> =
            client.from("treinos_aluno").select {
                filter { eq("user_id", userId) }
                order("dia_semana", Order.ASCENDING)
            }.decodeList()

        suspend fun minhaAgenda(): List<TreinoAluno> = listarDoAluno(uid())

        suspend fun meuTreinoDoDia(dia: Int = DiaSemana.hoje()): TreinoAluno? =
            client.from("treinos_aluno").select {
                filter {
                    eq("user_id", uid())
                    eq("dia_semana", dia)
                }
            }.decodeSingleOrNull()

        suspend fun buscar(id: Long): TreinoAluno =
            client.from("treinos_aluno").select { filter { eq("id", id) } }.decodeSingle()

        suspend fun buscarCompleto(id: Long): TreinoCompleto {
            val t = buscar(id)
            val exercicios = Exercicios.buscarVarios(t.exercicios.map { it.exercicioId }.distinct()).associateBy { it.id }
            return TreinoCompleto(t.comoTreino(), t.exercicios.map { it to exercicios[it.exercicioId] })
        }

        suspend fun salvar(treino: TreinoAluno, anterior: TreinoAluno? = null): TreinoAluno {
            if (treino.nome.isBlank()) throw SupabaseErro("Escolha ou digite o nome do treino")
            if (treino.diaSemana !in 0..6) throw SupabaseErro("Selecione o dia da semana")
            if (treino.exercicios.isEmpty()) throw SupabaseErro("Adicione pelo menos um exercício")
            if (treino.exercicios.any { it.repeticoes <= 0 || it.series <= 0 })
                throw SupabaseErro("Repetições e séries precisam ser maiores que zero")

            if (anterior?.id != null) excluir(anterior.id)
            client.from("treinos_aluno").delete {
                filter {
                    eq("user_id", treino.userId)
                    eq("dia_semana", treino.diaSemana)
                }
            }
            return client.from("treinos_aluno")
                .insert(treino.copy(id = null)) { select() }
                .decodeSingle()
        }

        suspend fun excluir(id: Long) {
            client.from("treinos_aluno").delete { filter { eq("id", id) } }
        }

        suspend fun salvarDias(base: TreinoAluno, dias: List<Int>, anteriores: List<TreinoAluno> = emptyList()): List<TreinoAluno> {
            if (dias.isEmpty()) throw SupabaseErro("Selecione o dia da semana")
            if (base.nome.isBlank()) throw SupabaseErro("Escolha ou digite o nome do treino")
            if (base.exercicios.isEmpty()) throw SupabaseErro("Adicione pelo menos um exercício")
            if (base.exercicios.any { it.repeticoes <= 0 || it.series <= 0 })
                throw SupabaseErro("Repetições e séries precisam ser maiores que zero")

            anteriores.mapNotNull { it.id }.takeIf { it.isNotEmpty() }?.let { ids ->
                client.from("treinos_aluno").delete { filter { isIn("id", ids) } }
            }
            client.from("treinos_aluno").delete {
                filter {
                    eq("user_id", base.userId)
                    isIn("dia_semana", dias)
                }
            }
            return client.from("treinos_aluno")
                .insert(dias.distinct().sorted().map { base.copy(id = null, diaSemana = it) }) { select() }
                .decodeList()
        }
    }

    object Chat {

        private val autores = mutableMapOf<String, Autor>()

        suspend fun mensagens(alunoId: String): List<Mensagem> =
            client.from("mensagens").select {
                filter { eq("aluno_id", alunoId) }
                order("criada_em", Order.ASCENDING)
            }.decodeList()

        suspend fun minhasMensagens(): List<Mensagem> = mensagens(uid())

        suspend fun enviar(alunoId: String, texto: String): Mensagem {
            val t = texto.trim()
            if (t.isEmpty()) throw SupabaseErro("Digite uma mensagem")
            if (t.length > 2000) throw SupabaseErro("Mensagem muito longa")
            return client.from("mensagens")
                .insert(buildJsonObject {
                    put("aluno_id", alunoId)
                    put("autor_id", uid())
                    put("texto", t)
                }) { select() }
                .decodeSingle()
        }

        suspend fun apagar(id: Long): Mensagem =
            client.postgrest.rpc("apagar_mensagem", buildJsonObject { put("p_id", id) }).decodeAs()

        suspend fun marcarLidas(alunoId: String, souAdmin: Boolean) {
            client.from("mensagens").update({ set("lida", true) }) {
                filter {
                    eq("aluno_id", alunoId)
                    eq("lida", false)
                    if (souAdmin) eq("autor_id", alunoId) else neq("autor_id", alunoId)
                }
            }
        }

        suspend fun conversas(): List<Conversa> =
            client.from("conversas").select { order("ultima_em", Order.DESCENDING) }.decodeList()

        suspend fun naoLidasDoAluno(): Int =
            client.from("conversas").select { filter { eq("aluno_id", uid()) } }
                .decodeList<Conversa>().firstOrNull()?.naoLidasAluno ?: 0

        suspend fun autor(id: String): Autor {
            autores[id]?.let { return it }
            val json = client.postgrest.rpc("autor_mensagem", buildJsonObject { put("p_id", id) })
                .decodeAsOrNull<JsonObject>()
            val autor = Autor(
                nome = json?.get("nome")?.jsonPrimitive?.contentOrNull ?: "Professor(a)",
                foto = json?.get("foto")?.jsonPrimitive?.contentOrNull,
                isAdmin = json?.get("is_admin")?.jsonPrimitive?.booleanOrNull ?: true
            )
            autores[id] = autor
            return autor
        }

        fun escutar(alunoId: String): Flow<Mensagem> = callbackFlow {
            val canal = client.channel("chat-$alunoId-${System.nanoTime()}")
            val mudancas = canal.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "mensagens"
                filter("aluno_id", FilterOperator.EQ, alunoId)
            }
            val coleta = launch {
                mudancas.collect { acao ->
                    val msg = when (acao) {
                        is PostgresAction.Insert -> runCatching { acao.decodeRecord<Mensagem>() }.getOrNull()
                        is PostgresAction.Update -> runCatching { acao.decodeRecord<Mensagem>() }.getOrNull()
                        else -> null
                    }
                    msg?.let { trySend(it) }
                }
            }
            canal.subscribe()
            awaitClose {
                coleta.cancel()
                CoroutineScope(Dispatchers.IO).launch { runCatching { client.realtime.removeChannel(canal) } }
            }
        }
    }

    object Presenca {
        suspend fun ping() {
            val id = idUsuarioLogado() ?: return
            client.from("usuarios").update({ set("visto_em", Instant.now().toString()) }) {
                filter { eq("id", id) }
            }
        }

        suspend fun resumo(): ResumoOnline =
            client.postgrest.rpc("resumo_online").decodeAsOrNull<ResumoOnline>() ?: ResumoOnline()
    }

    object TreinosRealizados {
        suspend fun registrar(nome: String, minutos: Int, exercicios: Int): TreinoRealizado =
            client.from("treinos_realizados")
                .insert(TreinoRealizado(nome = nome, minutos = minutos, exercicios = exercicios)) { select() }
                .decodeSingle()

        suspend fun daSemana(hoje: LocalDate = LocalDate.now()): List<TreinoRealizado> {
            val domingo = hoje.minusDays(DiaSemana.de(hoje).toLong())
            return client.from("treinos_realizados").select {
                filter {
                    eq("user_id", uid())
                    gte("realizado_em", domingo.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toString())
                }
                order("realizado_em", Order.ASCENDING)
            }.decodeList()
        }
    }

    object Aulas {

        suspend fun listar(filtroData: FiltroData? = null): List<AulaDetalhada> =
            client.from("aulas_detalhadas").select {
                filtroData?.let { f ->
                    val (inicio, fim) = f.intervalo()
                    filter {
                        gte("data", inicio.toString())
                        lte("data", fim.toString())
                    }
                }
                order("data", Order.ASCENDING)
                order("horario_inicio", Order.ASCENDING)
            }.decodeList()

        suspend fun buscar(id: Long): AulaDetalhada =
            client.from("aulas_detalhadas").select { filter { eq("id", id) } }.decodeSingle()

        suspend fun matricular(aulaId: Long): AulaDetalhada {
            client.from("matriculas_aula").insert(buildJsonObject { put("aula_id", aulaId) })
            return buscar(aulaId)
        }

        suspend fun cancelarMatricula(aulaId: Long): AulaDetalhada {
            client.from("matriculas_aula").delete {
                filter {
                    eq("aula_id", aulaId)
                    eq("user_id", uid())
                }
            }
            return buscar(aulaId)
        }

        suspend fun alternarMatricula(aula: AulaDetalhada): AulaDetalhada =
            if (aula.inscrito) cancelarMatricula(aula.id) else matricular(aula.id)

        suspend fun criar(aula: Aula) {
            validar(aula)
            client.from("aulas").insert(aula.copy(id = null, criadoPor = null))
        }

        suspend fun atualizar(aula: Aula) {
            val id = aula.id ?: throw SupabaseErro("Aula sem id")
            validar(aula)
            client.from("aulas").update({
                set("nome", aula.nome.trim())
                set("data", aula.data)
                set("horario_inicio", aula.horarioInicio)
                set("horario_fim", aula.horarioFim)
                set("vagas", aula.vagas)
            }) {
                filter { eq("id", id) }
            }
        }

        suspend fun excluir(id: Long) {
            client.from("aulas").delete { filter { eq("id", id) } }
        }

        suspend fun listarInscritos(aulaId: Long): List<InscritoAula> =
            client.from("matriculas_aula")
                .select(Columns.raw("user_id, usuarios(nome, email)")) { filter { eq("aula_id", aulaId) } }
                .decodeList()

        private fun validar(a: Aula) {
            val hora = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")
            if (a.nome.isBlank()) throw SupabaseErro("Digite o nome da aula")
            if (!hora.matches(a.horarioInicio) || !hora.matches(a.horarioFim)) throw SupabaseErro("Use horários no formato HH:mm")
            if (a.horarioFim <= a.horarioInicio) throw SupabaseErro("O horário final precisa ser depois do inicial")
            if (a.vagas < 1) throw SupabaseErro("A aula precisa ter pelo menos 1 vaga")
        }
    }
}

enum class FiltroMenuNotificacao { NENHUM, NAO_LIDA, SISTEMA }
enum class AbaNotificacao { TODAS, NAO_LIDAS }

data class FiltroNotificacao(
    val busca: String = "",
    val menu: FiltroMenuNotificacao = FiltroMenuNotificacao.NENHUM,
    val aba: AbaNotificacao = AbaNotificacao.TODAS
)

fun List<Notificacao>.aplicarFiltro(f: FiltroNotificacao): List<Notificacao> {
    val termo = f.busca.normalizar()
    return filter { n ->
        val passaBusca = termo.isEmpty() || n.nome.normalizar().contains(termo)
        val passaMenu = when (f.menu) {
            FiltroMenuNotificacao.NENHUM -> true
            FiltroMenuNotificacao.NAO_LIDA -> !n.lida
            FiltroMenuNotificacao.SISTEMA -> n.isSistema
        }
        val passaAba = when (f.aba) {
            AbaNotificacao.TODAS -> true
            AbaNotificacao.NAO_LIDAS -> !n.lida
        }
        passaBusca && passaMenu && passaAba
    }
}

fun String.normalizar(): String =
    Normalizer.normalize(trim().lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

data class FiltroData(val ano: Int, val mes: Int? = null, val dia: Int? = null) {

    fun intervalo(): Pair<LocalDate, LocalDate> = when {
        mes != null && dia != null -> LocalDate.of(ano, mes, dia).let { it to it }
        mes != null -> YearMonth.of(ano, mes).let { it.atDay(1) to it.atEndOfMonth() }
        else -> LocalDate.of(ano, 1, 1) to LocalDate.of(ano, 12, 31)
    }

    fun descricao(): String = when {
        mes != null && dia != null -> "%02d/%02d/%d".format(dia, mes, ano)
        mes != null -> "%02d/%d".format(mes, ano)
        else -> "$ano"
    }

    companion object {
        fun doDia(data: LocalDate) = FiltroData(data.year, data.monthValue, data.dayOfMonth)
        fun doMes(data: LocalDate) = FiltroData(data.year, data.monthValue)
        fun doAno(data: LocalDate) = FiltroData(data.year)
    }
}

enum class AbaAulas { TODAS, AGENDADAS }

fun List<AulaDetalhada>.aplicarAba(aba: AbaAulas): List<AulaDetalhada> = when (aba) {
    AbaAulas.TODAS -> this
    AbaAulas.AGENDADAS -> filter { it.inscrito }
}

fun List<AulaDetalhada>.substituir(atualizada: AulaDetalhada): List<AulaDetalhada> =
    map { if (it.id == atualizada.id) atualizada else it }

object Imagem {

    private const val TAMANHO_MAX = 600
    private const val QUALIDADE = 75

    private val cache = object : LruCache<Int, Bitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: Int, value: Bitmap) = value.byteCount
    }

    suspend fun uriParaBase64(context: Context, uri: Uri, tamanhoMax: Int = TAMANHO_MAX, qualidade: Int = QUALIDADE): String =
        withContext(Dispatchers.IO) {
            val bmp = carregarReduzido(context, uri, tamanhoMax) ?: throw SupabaseErro("Não foi possível abrir a imagem")
            val saida = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, qualidade, saida)
            Base64.encodeToString(saida.toByteArray(), Base64.NO_WRAP)
        }

    fun base64ParaBitmap(base64: String?): Bitmap? {
        if (base64.isNullOrBlank()) return null
        val chave = base64.hashCode()
        cache.get(chave)?.let { return it }
        return runCatching {
            val bytes = Base64.decode(base64.substringAfter("base64,"), Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()?.also { cache.put(chave, it) }
    }

    private fun carregarReduzido(context: Context, uri: Uri, tamanhoMax: Int): Bitmap? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                val w = info.size.width
                val h = info.size.height
                val escala = minOf(1f, tamanhoMax.toFloat() / maxOf(w, h))
                decoder.setTargetSize((w * escala).toInt().coerceAtLeast(1), (h * escala).toInt().coerceAtLeast(1))
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, limites) }
            var amostra = 1
            while (maxOf(limites.outWidth, limites.outHeight) / (amostra * 2) >= tamanhoMax) amostra *= 2
            val opcoes = BitmapFactory.Options().apply { inSampleSize = amostra }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opcoes) }?.let { bmp ->
                val escala = minOf(1f, tamanhoMax.toFloat() / maxOf(bmp.width, bmp.height))
                if (escala < 1f) Bitmap.createScaledBitmap(bmp, (bmp.width * escala).toInt(), (bmp.height * escala).toInt(), true)
                else bmp
            }
        }
}

fun ImageView.carregarBase64(base64: String?, placeholder: Int) {
    val bmp = Imagem.base64ParaBitmap(base64)
    if (bmp != null) setImageBitmap(bmp) else setImageResource(placeholder)
}

object Tempo {

    private val formatoBr = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun parseInstante(iso: String): Instant =
        runCatching { OffsetDateTime.parse(iso).toInstant() }.getOrElse { Instant.parse(iso) }

    fun decorrido(criadaEmIso: String, agora: Instant = Instant.now()): String {
        val minutos = Duration.between(parseInstante(criadaEmIso), agora).toMinutes().coerceAtLeast(0)
        return when {
            minutos < 1 -> "agora"
            minutos < 60 -> plural(minutos, "minuto", "minutos")
            minutos < 60 * 24 -> plural(minutos / 60, "hora", "horas")
            else -> plural(minutos / (60 * 24), "dia", "dias")
        }
    }

    private fun plural(n: Long, um: String, varios: String) = "há $n ${if (n == 1L) um else varios}"

    fun dataBr(isoData: String?): String =
        isoData?.let { runCatching { LocalDate.parse(it).format(formatoBr) }.getOrDefault(it) } ?: "—"

    fun dataBanco(data: LocalDate): String = data.toString()
}

object Youtube {

    private val regexId = Regex("(?:youtube\\.com/(?:watch\\?(?:.*&)?v=|shorts/|embed/)|youtu\\.be/)([A-Za-z0-9_-]{11})")

    fun idDoVideo(link: String): String? = regexId.find(link)?.groupValues?.get(1)

    fun linkValido(link: String): Boolean = idDoVideo(link) != null

    fun abrir(context: Context, link: String) {
        val id = idDoVideo(link) ?: throw SupabaseErro("Link do YouTube inválido")
        val app = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$id"))
        val web = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$id"))
        runCatching { context.startActivity(app) }.onFailure { context.startActivity(web) }
    }
}

class SupabaseErro(mensagem: String) : Exception(mensagem)

fun mensagemDeErro(e: Throwable): String = when (e) {
    is SupabaseErro -> e.message.orEmpty()
    is HttpRequestException -> "Sem conexão com o servidor. Verifique sua internet."
    is RestException -> when (e.error) {
        "invalid_credentials", "invalid_grant" -> "E-mail ou senha incorretos"
        "email_not_confirmed" -> "Confirme seu e-mail antes de entrar"
        "user_already_exists", "email_exists" -> "Este e-mail já está cadastrado"
        "weak_password" -> "Senha fraca. Use pelo menos 6 caracteres"
        "same_password" -> "A nova senha precisa ser diferente da atual"
        "over_email_send_rate_limit", "over_request_rate_limit" -> "Muitas tentativas. Aguarde um pouco."
        else -> e.error
    }
    else -> e.message ?: "Erro inesperado"
}