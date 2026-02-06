package io.github.zenhelix.dependanger.core.dsl

import io.github.zenhelix.dependanger.core.model.DeprecationInfo

@DependangerDslMarker
public class DeprecationDsl {
    public var replacedBy: String? = null
    public var message: String? = null
    public var since: String? = null
    public var removalVersion: String? = null

    public fun toDeprecationInfo(): DeprecationInfo = DeprecationInfo(
        replacedBy = replacedBy,
        message = message,
        since = since,
        removalVersion = removalVersion,
    )
}
