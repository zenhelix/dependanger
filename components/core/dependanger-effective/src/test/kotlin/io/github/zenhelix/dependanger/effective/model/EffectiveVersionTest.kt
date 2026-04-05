package io.github.zenhelix.dependanger.effective.model

import io.github.zenhelix.dependanger.core.model.VersionReference.VersionRange
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class EffectiveVersionTest {

    @Nested
    inner class RangeVariant {
        @Test
        fun `Range isResolved returns false`() {
            val range = EffectiveVersion.Range(VersionRange.Simple("[1.0,2.0)"))
            assertThat(range.isResolved).isFalse()
        }

        @Test
        fun `Range valueOrNull returns null`() {
            val range = EffectiveVersion.Range(VersionRange.Simple("[1.0,2.0)"))
            assertThat(range.valueOrNull).isNull()
        }

        @Test
        fun `Range resolvedOrNull returns null`() {
            val range = EffectiveVersion.Range(VersionRange.Simple("[1.0,2.0)"))
            assertThat(range.resolvedOrNull).isNull()
        }

        @Test
        fun `Range rangeOrNull returns the range`() {
            val versionRange = VersionRange.Simple("[1.0,2.0)")
            val range = EffectiveVersion.Range(versionRange)
            assertThat(range.rangeOrNull).isEqualTo(versionRange)
        }

        @Test
        fun `Rich range rangeOrNull returns the range`() {
            val versionRange = VersionRange.Rich(require = "1.0", strictly = null, prefer = "1.5", reject = emptyList())
            val range = EffectiveVersion.Range(versionRange)
            assertThat(range.rangeOrNull).isEqualTo(versionRange)
        }
    }

    @Nested
    inner class InlineVariant {
        @Test
        fun `Inline isResolved returns false`() {
            val inline = EffectiveVersion.Inline("1.2.3")
            assertThat(inline.isResolved).isFalse()
        }

        @Test
        fun `Inline valueOrNull returns the value`() {
            val inline = EffectiveVersion.Inline("1.2.3")
            assertThat(inline.valueOrNull).isEqualTo("1.2.3")
        }

        @Test
        fun `Inline resolvedOrNull returns null`() {
            val inline = EffectiveVersion.Inline("1.2.3")
            assertThat(inline.resolvedOrNull).isNull()
        }

        @Test
        fun `Inline rangeOrNull returns null`() {
            val inline = EffectiveVersion.Inline("1.2.3")
            assertThat(inline.rangeOrNull).isNull()
        }
    }

    @Nested
    inner class RangeOrNullOnOtherVariants {
        @Test
        fun `Resolved rangeOrNull returns null`() {
            val resolved = EffectiveVersion.Resolved(
                ResolvedVersion(alias = "v", value = "1.0", source = VersionSource.DECLARED, originalRef = null)
            )
            assertThat(resolved.rangeOrNull).isNull()
        }

        @Test
        fun `None rangeOrNull returns null`() {
            assertThat(EffectiveVersion.None.rangeOrNull).isNull()
        }

        @Test
        fun `Unresolved rangeOrNull returns null`() {
            assertThat(EffectiveVersion.Unresolved("ref").rangeOrNull).isNull()
        }

        @Test
        fun `Inline rangeOrNull returns null`() {
            assertThat(EffectiveVersion.Inline("1.0").rangeOrNull).isNull()
        }
    }
}
