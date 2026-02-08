package io.github.zenhelix.dependanger.features.license

import io.github.zenhelix.dependanger.features.license.model.LicenseResult

public class LicenseResolver {
    public suspend fun resolve(group: String, artifact: String, version: String): List<LicenseResult> = TODO()
}
