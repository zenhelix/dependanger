package io.github.zenhelix.dependanger.core.dsl

import io.github.zenhelix.dependanger.core.model.JdkConstraints
import io.github.zenhelix.dependanger.core.model.KotlinConstraints
import io.github.zenhelix.dependanger.core.model.Requirements

@DependangerDslMarker
public class RequiresDsl {
    public var jdk: JdkConstraints? = null
    public var kotlin: KotlinConstraints? = null

    public fun jdk(block: JdkConstraintsDsl.() -> Unit) {
        val dsl = JdkConstraintsDsl().apply(block)
        jdk = JdkConstraints(min = dsl.min, max = dsl.max)
    }

    public fun kotlin(block: KotlinConstraintsDsl.() -> Unit) {
        val dsl = KotlinConstraintsDsl().apply(block)
        kotlin = KotlinConstraints(min = dsl.min, max = dsl.max)
    }

    public fun toRequirements(): Requirements = Requirements(jdk = jdk, kotlin = kotlin)
}

@DependangerDslMarker
public class JdkConstraintsDsl {
    public var min: Int? = null
    public var max: Int? = null
}

@DependangerDslMarker
public class KotlinConstraintsDsl {
    public var min: String? = null
    public var max: String? = null
}
