package io.github.zenhelix.dependanger.core.spi

import io.github.zenhelix.dependanger.core.model.Library

public interface LibraryFilter {
    public val filterId: String
    public fun filter(libraries: List<Library>): List<Library>
}
