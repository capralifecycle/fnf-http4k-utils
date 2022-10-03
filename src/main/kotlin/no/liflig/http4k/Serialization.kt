package no.liflig.http4k

import org.http4k.format.AutoMappingConfiguration
import org.http4k.lens.BiDiMapping
import org.http4k.lens.BiDiPathLens
import org.http4k.lens.Path
import java.util.UUID

fun <T, OUT> AutoMappingConfiguration<T>.withPreregisteredIdMappings(idMappers: List<BiDiMapping<String, out OUT>>) = idMappers.fold(this) { acc, mapper ->
    acc.text(mapper)
}

inline fun <reified T> createUUIDMapper(crossinline factory: (UUID) -> T) =
    BiDiMapping<String, T>(
        { factory(UUID.fromString(it)) },
        { it.toString() },
    )

fun <T> createUUIDPathLens(name: String, factory: (UUID) -> T): BiDiPathLens<T> =
    Path.map({ factory(UUID.fromString(it)) }, { it.toString() }).of(name)
