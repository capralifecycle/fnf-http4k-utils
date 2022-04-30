package no.liflig.http4k

import arrow.core.Either
import org.http4k.core.Request

abstract class GetPrincipalDeviation: RuntimeException()

interface AuthService<P> {
    fun getPrincipal(request: Request): Either<GetPrincipalDeviation, P?>
}
