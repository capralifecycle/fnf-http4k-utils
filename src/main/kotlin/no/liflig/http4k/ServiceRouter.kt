package no.liflig.http4k

import no.liflig.http4k.health.HealthService
import no.liflig.http4k.health.health
import no.liflig.logging.ErrorLog
import no.liflig.logging.NormalizedStatus
import no.liflig.logging.PrincipalLog
import no.liflig.logging.RequestResponseLog
import no.liflig.logging.http4k.CatchAllExceptionFilter
import no.liflig.logging.http4k.ErrorHandlerFilter
import no.liflig.logging.http4k.ErrorResponseRendererWithLogging
import no.liflig.logging.http4k.LoggingFilter
import no.liflig.logging.http4k.RequestIdMdcFilter
import no.liflig.logging.http4k.RequestLensFailureFilter
import org.http4k.contract.JsonErrorResponseRenderer
import org.http4k.core.Filter
import org.http4k.core.Request
import org.http4k.core.RequestContexts
import org.http4k.core.Response
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.CorsPolicy
import org.http4k.filter.ServerFilters
import org.http4k.format.Jackson
import org.http4k.lens.RequestContextKey
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.routes
import java.util.UUID

/**
 * This encapsulates the basic router setup for all of our services so that
 * they handle requests similarly.
 */
class ServiceRouter<P, PL : PrincipalLog>(
    logHandler: (RequestResponseLog<PL>) -> Unit,
    principalToLog: (P) -> PL,
    corsPolicy: CorsPolicy?,
    authService: AuthService<P>,
    private val healthService: HealthService? = null,
    principalDeviationToResponse: (GetPrincipalDeviation) -> Response,
) {
    val contexts = RequestContexts()
    val requestIdChainLens = RequestContextKey.required<List<UUID>>(contexts)
    val errorLogLens = RequestContextKey.optional<ErrorLog>(contexts)
    val normalizedStatusLens = RequestContextKey.optional<NormalizedStatus>(contexts)
    val principalLens = RequestContextKey.optional<P>(contexts)

    val errorResponseRenderer = ErrorResponseRendererWithLogging(
        errorLogLens,
        normalizedStatusLens,
        JsonErrorResponseRenderer(Jackson),
    )

    val errorToContext: (Request, Throwable) -> Unit = { request, throwable ->
        request.with(errorLogLens of ErrorLog(throwable))
    }

    val handler = ApiHandler(principalLens, errorToContext)

    private val principalLog = { request: Request ->
        principalLens(request)?.let(principalToLog)
    }

    val coreFilters =
        ServerFilters
            .InitialiseRequestContext(contexts)
            .then(RequestIdMdcFilter(requestIdChainLens))
            .then(CatchAllExceptionFilter())
            .then(
                LoggingFilter(
                    principalLog,
                    errorLogLens,
                    normalizedStatusLens,
                    requestIdChainLens,
                    logHandler,
                ),
            )
            .let {
                if (corsPolicy != null) it.then(ServerFilters.Cors(corsPolicy))
                else it
            }
            .then(ErrorHandlerFilter(errorLogLens))
            .then(RequestLensFailureFilter(errorResponseRenderer))
            .then(PrincipalFilter(principalLens, authService, principalDeviationToResponse))

    class RoutingBuilder<P>(
        val apiHandler: ApiHandler<P>,
        val errorResponseRenderer: ErrorResponseRendererWithLogging,
    ) {
        val additionalFilters = org.http4k.util.Appendable<Filter>()
        val routes = org.http4k.util.Appendable<RoutingHttpHandler>()
    }

    fun routingHandler(funk: RoutingBuilder<P>.() -> Unit): RoutingHttpHandler {
        val builder = RoutingBuilder(handler, errorResponseRenderer)
        builder.funk()

        var current = coreFilters
        builder.additionalFilters.all.forEach {
            current = current.then(it)
        }

        val routes = builder.routes.all + listOfNotNull(healthService?.let { health(it) })

        return current.then(
            routes(*(routes).toTypedArray()),
        )
    }
}
