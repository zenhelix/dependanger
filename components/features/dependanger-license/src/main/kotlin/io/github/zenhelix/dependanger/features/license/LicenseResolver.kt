package io.github.zenhelix.dependanger.features.license

import io.github.zenhelix.dependanger.effective.model.LicenseResult

public class LicenseResolver {
    public suspend fun resolve(group: String, artifact: String, version: String): LicenseResult = TODO()
}
