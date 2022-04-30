package no.liflig.http4k

import no.liflig.logging.PrincipalLog

interface Principal<PL : PrincipalLog> {
    fun toLog(): PL
}