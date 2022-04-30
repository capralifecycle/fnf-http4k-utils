package no.liflig.http4k

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
        principalDeviationToResponse: (GetPrincipalDeviation) -> Response
    ): Filter = Filter { next ->
        { req ->
            authService.getPrincipal(req)
                .fold(
                    principalDeviationToResponse
                ) {
                    logger.info { "User $it" }
                    next(req.with(principalLens of it))
                }
        }
    }
}
