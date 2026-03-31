package io.github.zenhelix.dependanger.cli

import com.github.ajalt.clikt.core.ProgramResult
import io.github.zenhelix.dependanger.api.DependangerException
import io.github.zenhelix.dependanger.api.DependangerProcessingException
import io.github.zenhelix.dependanger.core.model.Diagnostics
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class ErrorHandlingExtendedTest {

    private val formatter = mockk<OutputFormatter>(relaxed = true)

    @Nested
    inner class `Successful execution` {

        @Test
        fun `block completes without throwing`() {
            assertDoesNotThrow {
                withErrorHandling(formatter) { /* no-op */ }
            }
        }

        @Test
        fun `block result is not intercepted`() {
            var executed = false
            withErrorHandling(formatter) { executed = true }
            assertThat(executed).isTrue()
        }
    }

    @Nested
    inner class `CliException handling` {

        @Test
        fun `FileNotFound produces ProgramResult with status 1`() {
            val result = assertThrows<ProgramResult> {
                withErrorHandling(formatter) {
                    throw CliException.FileNotFound("/missing.json")
                }
            }
            assertThat(result.statusCode).isEqualTo(1)
            verify { formatter.error("File not found: /missing.json") }
        }

        @Test
        fun `InvalidArgument produces ProgramResult with status 1`() {
            val result = assertThrows<ProgramResult> {
                withErrorHandling(formatter) {
                    throw CliException.InvalidArgument("bad argument")
                }
            }
            assertThat(result.statusCode).isEqualTo(1)
            verify { formatter.error("bad argument") }
        }

        @Test
        fun `ValidationFailed renders diagnostics and produces ProgramResult with status 1`() {
            val diagnostics = Diagnostics.error("E001", "something wrong", null, emptyMap())

            val result = assertThrows<ProgramResult> {
                withErrorHandling(formatter) {
                    throw CliException.ValidationFailed(diagnostics)
                }
            }
            assertThat(result.statusCode).isEqualTo(1)
            verify { formatter.renderDiagnostics(diagnostics) }
        }
    }

    @Nested
    inner class `DependangerException handling` {

        @Test
        fun `DependangerException produces ProgramResult with status 1`() {
            val result = assertThrows<ProgramResult> {
                withErrorHandling(formatter) {
                    throw DependangerException("processing failed", null)
                }
            }
            assertThat(result.statusCode).isEqualTo(1)
            verify { formatter.error("processing failed") }
        }

        @Test
        fun `DependangerException with cause prints cause message`() {
            val cause = RuntimeException("root cause")

            val result = assertThrows<ProgramResult> {
                withErrorHandling(formatter) {
                    throw DependangerProcessingException("processing failed", "resolve", cause)
                }
            }
            assertThat(result.statusCode).isEqualTo(1)
            verify { formatter.error("processing failed") }
            verify { formatter.error("Caused by: root cause") }
        }

        @Test
        fun `DependangerException without cause does not print caused-by`() {
            assertThrows<ProgramResult> {
                withErrorHandling(formatter) {
                    throw DependangerException("no cause", null)
                }
            }
            verify(exactly = 1) { formatter.error(any()) }
            verify { formatter.error("no cause") }
        }
    }

    @Nested
    inner class `Unexpected exception handling` {

        @Test
        fun `IllegalStateException produces ProgramResult with status 1`() {
            val result = assertThrows<ProgramResult> {
                withErrorHandling(formatter) {
                    throw IllegalStateException("illegal state")
                }
            }
            assertThat(result.statusCode).isEqualTo(1)
            verify { formatter.error("Unexpected error: illegal state") }
        }

        @Test
        fun `RuntimeException produces ProgramResult with status 1`() {
            val result = assertThrows<ProgramResult> {
                withErrorHandling(formatter) {
                    throw RuntimeException("runtime boom")
                }
            }
            assertThat(result.statusCode).isEqualTo(1)
            verify { formatter.error("Unexpected error: runtime boom") }
        }

        @Test
        fun `unexpected exception with cause prints cause message`() {
            val cause = IllegalArgumentException("bad arg")

            val result = assertThrows<ProgramResult> {
                withErrorHandling(formatter) {
                    throw RuntimeException("outer", cause)
                }
            }
            assertThat(result.statusCode).isEqualTo(1)
            verify { formatter.error("Unexpected error: outer") }
            verify { formatter.error("Caused by: bad arg") }
        }

        @Test
        fun `unexpected exception without cause does not print caused-by`() {
            assertThrows<ProgramResult> {
                withErrorHandling(formatter) {
                    throw RuntimeException("no cause")
                }
            }
            verify(exactly = 1) { formatter.error(any()) }
            verify { formatter.error("Unexpected error: no cause") }
        }

        @Test
        fun `exception with null message uses fallback`() {
            val result = assertThrows<ProgramResult> {
                withErrorHandling(formatter) {
                    throw object : Exception(null as String?) {}
                }
            }
            assertThat(result.statusCode).isEqualTo(1)
            verify { formatter.error("Unexpected error: Unknown error") }
        }
    }
}
