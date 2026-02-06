package io.github.zenhelix.dependanger.core.dsl

import io.github.zenhelix.dependanger.core.model.LicenseInfo

@DependangerDslMarker
public class LicenseDsl {
    public var id: String? = null
    public var url: String? = null

    public fun toLicenseInfo(): LicenseInfo = LicenseInfo(id = id, url = url)
}
