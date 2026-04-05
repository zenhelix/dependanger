package io.github.zenhelix.dependanger.effective.model

import io.github.zenhelix.dependanger.core.model.VersionReference.VersionRange
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the version state of a library or plugin in the processing pipeline.
 * Makes invalid states unrepresentable:
 * - [Resolved] — version is known (literal or resolved from reference/BOM)
 * - [Unresolved] — version reference waiting to be resolved by [VersionResolverProcessor]
 * - [Range] — version range or rich version constraint (not a concrete version)
 * - [None] — no version specified
 */
@Serializable
public sealed class EffectiveVersion {

    @Serializable
    @SerialName("resolved")
    public data class Resolved(val version: ResolvedVersion) : EffectiveVersion()

    @Serializable
    @SerialName("unresolved")
    public data class Unresolved(val refName: String) : EffectiveVersion()

    /**
     * An inline literal version that has not yet been promoted to a named version entry.
     * Created by [MetadataConversionProcessor] from [VersionReference.Literal],
     * then promoted to [Resolved] by [ExtractedVersionsProcessor].
     */
    @Serializable
    @SerialName("inline")
    public data class Inline(val value: String) : EffectiveVersion()

    @Serializable
    @SerialName("range")
    public data class Range(val range: VersionRange) : EffectiveVersion()

    @Serializable
    @SerialName("none")
    public data object None : EffectiveVersion()

    /** Returns [ResolvedVersion] if this is [Resolved], null otherwise. */
    public val resolvedOrNull: ResolvedVersion? get() = (this as? Resolved)?.version

    /** Returns the version string value if this is [Resolved] or [Inline], null otherwise. */
    public val valueOrNull: String?
        get() = when (this) {
            is Resolved -> version.value
            is Inline   -> value
            else        -> null
        }

    /**
     * True only when the version has been fully resolved into the version catalog (i.e. [Resolved]).
     * [Inline] versions carry a concrete value but are not yet promoted — use [valueOrNull] to check
     * for any concrete value regardless of catalog status.
     */
    public val isResolved: Boolean get() = this is Resolved

    /** Returns [VersionRange] if this is [Range], null otherwise. */
    public val rangeOrNull: VersionRange? get() = (this as? Range)?.range
}
