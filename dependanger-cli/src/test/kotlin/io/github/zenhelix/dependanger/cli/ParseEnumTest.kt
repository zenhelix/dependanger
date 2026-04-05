package io.github.zenhelix.dependanger.cli

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ParseEnumTest {

    private enum class TestEnum { FOO, BAR_BAZ }

    @Test
    fun `parses exact match`() {
        assertThat(parseEnum<TestEnum>("FOO", "test")).isEqualTo(TestEnum.FOO)
    }

    @Test
    fun `parses case-insensitive`() {
        assertThat(parseEnum<TestEnum>("foo", "test")).isEqualTo(TestEnum.FOO)
        assertThat(parseEnum<TestEnum>("bar_baz", "test")).isEqualTo(TestEnum.BAR_BAZ)
    }

    @Test
    fun `throws CliException for unknown value`() {
        assertThatThrownBy { parseEnum<TestEnum>("UNKNOWN", "test enum") }
            .isInstanceOf(CliException.InvalidArgument::class.java)
            .hasMessageContaining("Unknown test enum 'UNKNOWN'")
            .hasMessageContaining("FOO")
            .hasMessageContaining("BAR_BAZ")
    }
}
