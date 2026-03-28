package io.github.zenhelix.dependanger.maven.pom.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class XmlUtilsTest {

    @Nested
    inner class `escapeXml` {

        @Test
        fun `replaces all 5 special characters`() {
            val input = """<tag attr="val" & 'quote'>"""

            val result = input.escapeXml()

            assertThat(result).isEqualTo("&lt;tag attr=&quot;val&quot; &amp; &apos;quote&apos;&gt;")
        }

        @Test
        fun `empty string returns empty string`() {
            val result = "".escapeXml()

            assertThat(result).isEmpty()
        }

        @Test
        fun `string with no special chars returns same string`() {
            val input = "hello world 123"

            val result = input.escapeXml()

            assertThat(result).isEqualTo("hello world 123")
        }
    }

    @Nested
    inner class `escapeXmlComment` {

        @Test
        fun `replaces double dashes with spaced dashes`() {
            val result = "some--comment".escapeXmlComment()

            assertThat(result).isEqualTo("some- -comment")
        }

        @Test
        fun `triple dashes are partially escaped`() {
            // Known limitation: single-pass replace("--", "- -") leaves trailing "--" in "---"
            val result = "---".escapeXmlComment()

            assertThat(result).isEqualTo("- --")
        }

        @Test
        fun `no dashes returns same string`() {
            val result = "safe comment".escapeXmlComment()

            assertThat(result).isEqualTo("safe comment")
        }
    }
}
