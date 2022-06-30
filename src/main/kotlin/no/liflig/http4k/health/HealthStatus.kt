@file:UseSerializers(InstantSerializer::class)

package no.liflig.http4k.health

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant
import java.time.format.DateTimeFormatter

object InstantSerializer : KSerializer<Instant> {
    private val formatter = DateTimeFormatter.ISO_INSTANT

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("InstantSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant): Unit =
        encoder.encodeString(formatter.format(value))

    override fun deserialize(decoder: Decoder): Instant =
        formatter.parse(decoder.decodeString(), Instant::from)
}

@Serializable
data class HealthStatus(
    val name: String,
    val timestamp: Instant,
    val runningSince: Instant,
    val build: HealthBuildInfo
)

@Serializable
data class HealthBuildInfo(
    /**
     * During local development this will be null.
     */
    val timestamp: Instant?,
    val commit: String,
    val branch: String,
    val number: Int
)
