package io.github.zenhelix.dependanger.api

import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.github.zenhelix.dependanger.effective.model.EffectiveMetadata

public sealed class DependangerResult {
    public abstract val diagnostics: Diagnostics

    /** Pipeline completed without errors. Safe for artifact generation. */
    public data class Success(
        val effective: EffectiveMetadata,
        override val diagnostics: Diagnostics,
    ) : DependangerResult()

    /** Pipeline completed but diagnostics contain errors (e.g. denied licenses, security vulnerabilities). */
    public data class CompletedWithErrors(
        val effective: EffectiveMetadata,
        override val diagnostics: Diagnostics,
    ) : DependangerResult()

    /** Pipeline failed to produce effective metadata. */
    public data class Failure(
        override val diagnostics: Diagnostics,
    ) : DependangerResult()

    public val isSuccess: Boolean get() = this is Success

    public fun effectiveOrNull(): EffectiveMetadata? = when (this) {
        is Success -> effective
        is CompletedWithErrors -> effective
        is Failure -> null
    }
}
