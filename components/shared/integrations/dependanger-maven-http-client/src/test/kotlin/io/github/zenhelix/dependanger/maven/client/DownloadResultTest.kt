package io.github.zenhelix.dependanger.maven.client

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class DownloadResultTest {

    @Nested
    inner class `Success variant` {

        @Test
        fun `carries content`() {
            val result = DownloadResult.Success("<project>...</project>")
            assertThat(result.content).isEqualTo("<project>...</project>")
        }
    }

    @Nested
    inner class `NotFound variant` {

        @Test
        fun `is a singleton`() {
            assertThat(DownloadResult.NotFound).isSameAs(DownloadResult.NotFound)
        }
    }

    @Nested
    inner class `AuthRequired variant` {

        @Test
        fun `carries url and status code`() {
            val result = DownloadResult.AuthRequired("https://repo.example.com/foo.pom", 401)
            assertThat(result.url).isEqualTo("https://repo.example.com/foo.pom")
            assertThat(result.statusCode).isEqualTo(401)
        }
    }

    @Nested
    inner class `Failed variant` {

        @Test
        fun `carries error message`() {
            val result = DownloadResult.Failed("Connection timeout")
            assertThat(result.error).isEqualTo("Connection timeout")
        }
    }

    @Nested
    inner class `sealed interface exhaustiveness` {

        @Test
        fun `when-expression covers all variants`() {
            val results: List<DownloadResult> = listOf(
                DownloadResult.Success("content"),
                DownloadResult.NotFound,
                DownloadResult.AuthRequired("url", 403),
                DownloadResult.Failed("error"),
            )

            val descriptions = results.map { result ->
                when (result) {
                    is DownloadResult.Success      -> "success"
                    is DownloadResult.NotFound     -> "not-found"
                    is DownloadResult.AuthRequired -> "auth-required"
                    is DownloadResult.Failed       -> "failed"
                }
            }

            assertThat(descriptions).containsExactly("success", "not-found", "auth-required", "failed")
        }
    }
}
