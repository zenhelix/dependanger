package io.github.zenhelix.dependanger.features.license

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SpdxExpressionParsingTest {

    @Nested
    inner class `single license identifier` {

        @Test
        fun `returns single-element list for a simple SPDX ID`() {
            val result = SpdxExpressionParser.parse("MIT")

            assertThat(result).containsExactly("MIT")
        }

        @Test
        fun `returns single-element list for a hyphenated SPDX ID`() {
            val result = SpdxExpressionParser.parse("Apache-2.0")

            assertThat(result).containsExactly("Apache-2.0")
        }
    }

    @Nested
    inner class `OR expressions` {

        @Test
        fun `splits two licenses joined by OR`() {
            val result = SpdxExpressionParser.parse("MIT OR Apache-2.0")

            assertThat(result).containsExactly("MIT", "Apache-2.0")
        }

        @Test
        fun `splits three licenses joined by OR`() {
            val result = SpdxExpressionParser.parse("MIT OR Apache-2.0 OR GPL-3.0-only")

            assertThat(result).containsExactly("MIT", "Apache-2.0", "GPL-3.0-only")
        }

        @Test
        fun `handles case-insensitive OR separator`() {
            val result = SpdxExpressionParser.parse("MIT or Apache-2.0")

            assertThat(result).containsExactly("MIT", "Apache-2.0")
        }

        @Test
        fun `handles mixed-case OR separator`() {
            val result = SpdxExpressionParser.parse("MIT Or Apache-2.0")

            assertThat(result).containsExactly("MIT", "Apache-2.0")
        }

        @Test
        fun `trims whitespace around license identifiers`() {
            val result = SpdxExpressionParser.parse("  MIT   OR   Apache-2.0  ")

            assertThat(result).containsExactly("MIT", "Apache-2.0")
        }
    }

    @Nested
    inner class `AND expressions` {

        @Test
        fun `does not split AND expression - treats as single license`() {
            val result = SpdxExpressionParser.parse("MIT AND Apache-2.0")

            assertThat(result).containsExactly("MIT AND Apache-2.0")
        }

        @Test
        fun `does not split compound AND expression`() {
            val result = SpdxExpressionParser.parse("LGPL-2.1-only AND BSD-3-Clause")

            assertThat(result).containsExactly("LGPL-2.1-only AND BSD-3-Clause")
        }
    }

    @Nested
    inner class `blank and empty input` {

        @Test
        fun `returns empty list for empty string`() {
            val result = SpdxExpressionParser.parse("")

            assertThat(result).isEmpty()
        }

        @Test
        fun `returns empty list for blank string`() {
            val result = SpdxExpressionParser.parse("   ")

            assertThat(result).isEmpty()
        }

        @Test
        fun `returns empty list for whitespace-only input`() {
            val result = SpdxExpressionParser.parse("\t\n ")

            assertThat(result).isEmpty()
        }
    }

    @Nested
    inner class `mixed OR and AND` {

        @Test
        fun `splits on OR but preserves AND within segments`() {
            val result = SpdxExpressionParser.parse("MIT AND Apache-2.0 OR GPL-3.0-only")

            assertThat(result).containsExactly("MIT AND Apache-2.0", "GPL-3.0-only")
        }
    }
}
