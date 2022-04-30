package no.liflig.http4k

import arrow.core.Either
import arrow.core.computations.EitherEffect
import arrow.core.computations.either
import arrow.core.getOrHandle
import arrow.core.left
import arrow.core.right
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.lens.BiDiLens

class ApiHandler<P>(
    private val principalLens: BiDiLens<Request, P?>,
    private val errorToContext: (Request, Throwable) -> Unit
) {
    private val Request.principal: P? get() = principalLens(this)
    private fun P?.orUserNotAuthenticatedResponse(): Either<ErrorResponse, P> =
        this
            ?.right()
            ?: notAuthenticated().left()


    private fun Either<ErrorResponse, Response>.handleError(request: Request): Response =
        getOrHandle {
            if (it.throwable != null) {
                errorToContext(request, it.throwable)
            }
            it.response
        }

    /**
     * Request handler that runs the request in a coroutine.
     */
    private fun coroutineHandler(
        block: suspend CoroutineScope.(request: Request) -> Response
    ): HttpHandler =
        { request ->
            runBlocking(CoroutineName("no/liflig/http4k") + MDCContext()) {
                block(request)
            }
        }

    /**
     * Request handler that does not require authentication but
     * provides the principal if available.
     *
     * The calling block will be called with an suspending Either binding
     * by running the request in a coroutine so we can use suspending code.
     */
    fun authNotChecked(
        block: suspend EitherEffect<ErrorResponse, *>.(
            request: Request,
            principal: P?,
        ) -> Response
    ): HttpHandler =
        coroutineHandler { request ->
            either<ErrorResponse, Response> {
                block(request, request.principal)
            }.handleError(request)
        }

    /**
     * Request handler that requires authentication and provides the
     * [P] object for processing.
     *
     * The calling block will be called with an suspending Either binding
     * by running the request in a coroutine so we can use suspending code.
     */
    fun authed(
        block: suspend EitherEffect<ErrorResponse, *>.(
            request: Request,
            principal: P
        ) -> Response
    ): HttpHandler =
        // Auth is checked inside.
        authNotChecked { request, principal ->
            block(
                request,
                principal.orUserNotAuthenticatedResponse().bind()
            )
        }
}
