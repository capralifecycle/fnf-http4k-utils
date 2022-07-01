package no.liflig.http4k

import arrow.core.Either
import arrow.core.computations.EitherEffect
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import org.http4k.core.Response
import org.http4k.core.Status
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Http4k Response with error info used for logging purposes.
 */
data class ErrorResponse(val response: Response, val throwable: Throwable?)

/**
 * Create a [ErrorResponse] based on a [Response] while also
 * wrapping or creating a new exception to attach a stack trace to be
 * able to track to the code location this happened in logs.
 */
fun Response.asErrorResponse(throwable: Throwable? = null): ErrorResponse =
    ErrorResponse(
        this,
        // Copy the message to help viewing logs.
        RuntimeException(
            throwable.let {
                if (it != null) {
                    it.message
                } else {
                    "This exception is only for providing a stacktrace. No exception was thrown"
                }
            },
            throwable
        )
    )

/**
 * Expect a value to be not null or generate a not found error.
 *
 * Only use this if the resource pointed to by the URL cannot be found.
 * See [mapNotFoundAs400] and [expectValue] for other situations.
 */
fun <T> Either<ErrorResponse, T?>.mapNotFoundAs404(): Either<ErrorResponse, T> =
    flatMap {
        it
            ?.right()
            ?: notFound(RuntimeException("Resource not found")).left()
    }

/**
 * Expect a value to be not null or generate a bad user input error.
 *
 * This can be used when resolving referenced items given by the user that does not exist.
 */
fun <T> Either<ErrorResponse, T?>.mapNotFoundAs400(): Either<ErrorResponse, T> =
    flatMap {
        it
            ?.right()
            ?: badUserInput("A referenced resource was not found.").left()
    }

/**
 * Expect a value to be not null or generate a internal server error.
 */
fun <T> Either<ErrorResponse, T?>.expectValue(): Either<ErrorResponse, T> =
    flatMap {
        it
            ?.right()
            ?: internalServiceError().left()
    }

/**
 * Create a [ErrorResponse] for an unrecoverable internal server error.
 */
fun internalServiceError(throwable: Throwable? = null): ErrorResponse =
    Response(Status.INTERNAL_SERVER_ERROR).asErrorResponse(throwable)

/**
 * Create a [ErrorResponse] for the service being unavailable.
 */
fun serviceUnavailable(throwable: Throwable? = null): ErrorResponse =
    Response(Status.SERVICE_UNAVAILABLE).asErrorResponse(throwable)

/**
 * Create a [ErrorResponse] for a forbidden operation (user is not authorized).
 */
fun forbidden(throwable: Throwable? = null): ErrorResponse =
    Response(Status.FORBIDDEN).asErrorResponse(throwable)

/**
 * Create a [ErrorResponse] for a not found resource.
 */
fun notFound(throwable: Throwable? = null): ErrorResponse =
    Response(Status.NOT_FOUND).asErrorResponse(throwable)

/**
 * Create a [ErrorResponse] for a conflicting resource.
 */
fun conflict(throwable: Throwable? = null): ErrorResponse =
    Response(Status.CONFLICT).asErrorResponse(throwable)

/**
 * Create a [ErrorResponse] for the user not being authenticated.
 */
fun notAuthenticated(throwable: Throwable? = null): ErrorResponse =
    Response(Status.UNAUTHORIZED).asErrorResponse(throwable)

/**
 * Create a [ErrorResponse] for bad user input.
 */
fun badUserInput(
    userMessage: String? = null,
    throwable: Throwable? = null,
): ErrorResponse = Response(Status.BAD_REQUEST)
    .let {
        // TODO: Should the response be structured somehow?
        if (userMessage != null) it.body(userMessage)
        else it
    }
    .asErrorResponse(throwable)

/**
 * Create a [ErrorResponse] for bad user input and bind it.
 *
 * Convenience to simplify handlers.
 */
suspend fun EitherEffect<ErrorResponse, *>.badUserInputBind(
    userMessage: String,
    throwable: Throwable? = null,
): Nothing {
    @Suppress("IMPLICIT_NOTHING_TYPE_ARGUMENT_IN_RETURN_POSITION")
    badUserInput(userMessage, throwable).left().bind()
}

/**
 * Bind a BAD_REQUEST in case the check fails.
 */
@ExperimentalContracts
suspend fun EitherEffect<ErrorResponse, *>.checkInput(
    value: Boolean,
    lazyMessage: () -> String,
) {
    contract {
        returns() implies value
    }

    if (!value) {
        badUserInputBind(lazyMessage())
    }
}

fun <T> T?.orNotFound(): Either<ErrorResponse, T> = this?.right() ?: notFound().left()
