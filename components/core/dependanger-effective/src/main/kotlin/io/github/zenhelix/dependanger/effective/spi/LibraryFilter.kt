package io.github.zenhelix.dependanger.effective.spi

import io.github.zenhelix.dependanger.effective.model.EffectiveLibrary
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext

public interface LibraryFilter {
    public val filterId: String
    public fun shouldInclude(alias: String, library: EffectiveLibrary, context: ProcessingContext): Boolean
}
