package io.github.zenhelix.dependanger.effective.spi

import io.github.zenhelix.dependanger.core.pipeline.ProcessingContextKey

/**
 * SPI for contributing typed values to [ProcessingContext] at pipeline construction time.
 * Implementations are discovered via ServiceLoader and invoked once per pipeline run.
 *
 * Use this to register SPI-discovered services (filters, handlers, providers)
 * so that processors can read them from context instead of calling ServiceLoader themselves.
 */
public interface ContextContributor {
    public fun contribute(): Map<ProcessingContextKey<*>, Any>
}
