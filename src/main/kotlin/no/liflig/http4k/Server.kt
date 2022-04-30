package no.liflig.http4k

import org.eclipse.jetty.server.HttpConnectionFactory
import org.http4k.routing.RoutingHttpHandler
import org.http4k.server.ConnectorBuilder
import org.http4k.server.Http4kServer
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.http4k.server.http

private fun getPortToListenOn(developmentPort: Int): Int {
    val servicePortFromEnv = System.getenv("SERVICE_PORT")
    return servicePortFromEnv?.toInt() ?: developmentPort
}

fun RoutingHttpHandler.asServer(developmentPort: Int): Http4kServer {
    val port = getPortToListenOn(developmentPort)
    return asServer(Jetty(port, httpNoServerVersionHeader(port)))
}

// Avoid leaking Jetty version in http response header "Server".
private fun httpNoServerVersionHeader(port: Int): ConnectorBuilder = { server ->
    http(port)(server).apply {
        connectionFactories
            .filterIsInstance<HttpConnectionFactory>()
            .forEach { it.httpConfiguration.sendServerVersion = false }
    }
}
