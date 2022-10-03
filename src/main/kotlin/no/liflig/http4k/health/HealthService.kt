package no.liflig.http4k.health

import java.lang.management.ManagementFactory
import java.time.Instant
import java.time.temporal.ChronoUnit

class HealthService(
    private val name: String,
    private val buildInfo: HealthBuildInfo,
) {

    private val runningSince: Instant = getRunningSince()

    fun healthStatus() = HealthStatus(
        name = name,
        timestamp = Instant.now(),
        runningSince = runningSince,
        build = buildInfo,
    )

    private fun getRunningSince(): Instant {
        val uptimeInMillis = ManagementFactory.getRuntimeMXBean().uptime
        return Instant.now().minus(uptimeInMillis, ChronoUnit.MILLIS)
    }
}

/**
 * Create [HealthService] by using provided build.properties.
 */
fun createHealthService(name: String, healthBuildInfo: HealthBuildInfo) =
    HealthService(name, healthBuildInfo)
