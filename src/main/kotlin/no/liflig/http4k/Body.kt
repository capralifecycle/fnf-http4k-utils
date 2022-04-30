package no.liflig.http4k

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.lens.BiDiBodyLens
import org.http4k.lens.string

@PublishedApi
internal val json = Json {
    encodeDefaults = false
    ignoreUnknownKeys = true
}

fun <T> T.asSuccessResponse(
    bodyLens: BiDiBodyLens<T>,
    status: Status = Status.OK,
) = Response(status).with(bodyLens of this)

inline fun <reified T : Any> createBodyLens(serializer: KSerializer<T>): BiDiBodyLens<T> {
    return Body
        .string(ContentType.APPLICATION_JSON)
        .map(
            { json.decodeFromString(serializer, it) },
            { json.encodeToString(serializer, it) }
        )
        .toLens()
}
