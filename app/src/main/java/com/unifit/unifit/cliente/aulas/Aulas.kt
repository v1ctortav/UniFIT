package com.unifit.unifit.cliente.aulas

import com.unifit.unifit.server.AulaDetalhada

enum class StatusAula {
    DISPONIVEL,
    INDISPONIVEL,
    MATRICULADO
}

val AulaDetalhada.status: StatusAula
    get() = when {
        inscrito -> StatusAula.MATRICULADO
        lotada -> StatusAula.INDISPONIVEL
        else -> StatusAula.DISPONIVEL
    }

val AulaDetalhada.instrutor: String
    get() = nomeInstrutor?.takeIf { it.isNotBlank() } ?: "Professor(a) UniFit"
