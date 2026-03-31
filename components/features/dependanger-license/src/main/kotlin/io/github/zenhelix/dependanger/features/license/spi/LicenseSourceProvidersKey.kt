package io.github.zenhelix.dependanger.features.license.spi

import io.github.zenhelix.dependanger.core.pipeline.ProcessingContextKey

public val LicenseSourceProvidersKey: ProcessingContextKey<List<LicenseSourceProvider>> =
    ProcessingContextKey("licenseSourceProviders")
