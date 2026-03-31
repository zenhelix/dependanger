package io.github.zenhelix.dependanger.features.license.spi

import io.github.zenhelix.dependanger.core.pipeline.ProcessingContextKey
import io.github.zenhelix.dependanger.effective.spi.ContextContributor
import java.util.ServiceLoader

public class LicenseSpiContextContributor : ContextContributor {
    private val providers = ServiceLoader.load(LicenseSourceProvider::class.java).toList()

    override fun contribute(): Map<ProcessingContextKey<*>, Any> =
        mapOf(LicenseSourceProvidersKey to providers)
}
