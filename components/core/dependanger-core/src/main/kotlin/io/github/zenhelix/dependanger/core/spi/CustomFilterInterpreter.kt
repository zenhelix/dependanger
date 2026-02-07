package io.github.zenhelix.dependanger.core.spi

import io.github.zenhelix.dependanger.core.model.Library
import kotlinx.serialization.json.JsonElement

public interface CustomFilterInterpreter {
    public val filterId: String
    public fun filter(libraries: List<Library>, config: JsonElement): List<Library>
}
