package no.liflig.http4k

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import mu.KotlinLogging
import org.http4k.core.Filter
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.with
import org.http4k.lens.BiDiLens

private val logger = KotlinLogging.logger {}

object PrincipalFilter {
    operator fun <P> invoke(
        principalLens: BiDiLens<Request, P?>,
        authService: AuthService<P>,
        principalDeviationToResponse: (GetPrincipalDeviation) -> Response,
    ): Filter = Filter { next ->
        { req ->
            runBlocking(CoroutineName("no/liflig/http4k") + MDCContext()) {
                authService.getPrincipal(req)
            }
                .fold(
                    principalDeviationToResponse
                ) {
                    next(req.with(principalLens of it))
                }
        }
    }
}
