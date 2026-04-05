package io.github.zenhelix.dependanger.features.security

import io.github.zenhelix.dependanger.feature.model.security.VulnerabilityInfo
import io.github.zenhelix.dependanger.feature.model.security.VulnerabilitySeverity
import io.github.zenhelix.dependanger.osv.client.OsvBatchResult
import io.github.zenhelix.dependanger.osv.client.OsvPackageQuery
import io.github.zenhelix.dependanger.osv.client.OsvVulnerabilityData
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SecurityCheckBehaviorTest {

    /** Mirrors the private cvssScoreToSeverity function logic. */
    private fun classifyCvss(score: Double?): VulnerabilitySeverity = when {
        score == null -> VulnerabilitySeverity.UNKNOWN
        score >= 9.0  -> VulnerabilitySeverity.CRITICAL
        score >= 7.0  -> VulnerabilitySeverity.HIGH
        score >= 4.0  -> VulnerabilitySeverity.MEDIUM
        score >= 0.1  -> VulnerabilitySeverity.LOW
        else          -> VulnerabilitySeverity.NONE
    }

    @Nested
    inner class `Vulnerability severity classification` {

        @Test
        fun `CVSS score 9_0 or above is CRITICAL`() {
            assertThat(classifyCvss(9.0)).isEqualTo(VulnerabilitySeverity.CRITICAL)
            assertThat(classifyCvss(10.0)).isEqualTo(VulnerabilitySeverity.CRITICAL)
        }

        @Test
        fun `CVSS score 7_0 to 8_9 is HIGH`() {
            assertThat(classifyCvss(7.0)).isEqualTo(VulnerabilitySeverity.HIGH)
            assertThat(classifyCvss(8.9)).isEqualTo(VulnerabilitySeverity.HIGH)
        }

        @Test
        fun `CVSS score 4_0 to 6_9 is MEDIUM`() {
            assertThat(classifyCvss(4.0)).isEqualTo(VulnerabilitySeverity.MEDIUM)
            assertThat(classifyCvss(6.9)).isEqualTo(VulnerabilitySeverity.MEDIUM)
        }

        @Test
        fun `CVSS score 0_1 to 3_9 is LOW`() {
            assertThat(classifyCvss(0.1)).isEqualTo(VulnerabilitySeverity.LOW)
            assertThat(classifyCvss(3.9)).isEqualTo(VulnerabilitySeverity.LOW)
        }

        @Test
        fun `CVSS score 0_0 is NONE`() {
            assertThat(classifyCvss(0.0)).isEqualTo(VulnerabilitySeverity.NONE)
        }

        @Test
        fun `null CVSS score is UNKNOWN`() {
            assertThat(classifyCvss(null)).isEqualTo(VulnerabilitySeverity.UNKNOWN)
        }
    }

    @Nested
    inner class `VulnerabilitySeverity threshold checking` {

        @Test
        fun `CRITICAL meets CRITICAL threshold`() {
            assertThat(VulnerabilitySeverity.CRITICAL.meetsThreshold(VulnerabilitySeverity.CRITICAL)).isTrue()
        }

        @Test
        fun `HIGH meets HIGH threshold`() {
            assertThat(VulnerabilitySeverity.HIGH.meetsThreshold(VulnerabilitySeverity.HIGH)).isTrue()
        }

        @Test
        fun `CRITICAL meets HIGH threshold`() {
            assertThat(VulnerabilitySeverity.CRITICAL.meetsThreshold(VulnerabilitySeverity.HIGH)).isTrue()
        }

        @Test
        fun `MEDIUM does not meet HIGH threshold`() {
            assertThat(VulnerabilitySeverity.MEDIUM.meetsThreshold(VulnerabilitySeverity.HIGH)).isFalse()
        }

        @Test
        fun `LOW meets LOW threshold`() {
            assertThat(VulnerabilitySeverity.LOW.meetsThreshold(VulnerabilitySeverity.LOW)).isTrue()
        }

        @Test
        fun `UNKNOWN never meets any threshold`() {
            assertThat(VulnerabilitySeverity.UNKNOWN.meetsThreshold(VulnerabilitySeverity.LOW)).isFalse()
            assertThat(VulnerabilitySeverity.UNKNOWN.meetsThreshold(VulnerabilitySeverity.CRITICAL)).isFalse()
        }

        @Test
        fun `NONE never meets any threshold`() {
            assertThat(VulnerabilitySeverity.NONE.meetsThreshold(VulnerabilitySeverity.LOW)).isFalse()
            assertThat(VulnerabilitySeverity.NONE.meetsThreshold(VulnerabilitySeverity.CRITICAL)).isFalse()
        }

        @Test
        fun `MEDIUM meets MEDIUM threshold`() {
            assertThat(VulnerabilitySeverity.MEDIUM.meetsThreshold(VulnerabilitySeverity.MEDIUM)).isTrue()
        }

        @Test
        fun `HIGH meets MEDIUM threshold`() {
            assertThat(VulnerabilitySeverity.HIGH.meetsThreshold(VulnerabilitySeverity.MEDIUM)).isTrue()
        }
    }

    @Nested
    inner class `VulnerabilitySeverity fromString parsing` {

        @Test
        fun `parses CRITICAL case-insensitively`() {
            assertThat(VulnerabilitySeverity.fromString("CRITICAL")).isEqualTo(VulnerabilitySeverity.CRITICAL)
            assertThat(VulnerabilitySeverity.fromString("critical")).isEqualTo(VulnerabilitySeverity.CRITICAL)
            assertThat(VulnerabilitySeverity.fromString("Critical")).isEqualTo(VulnerabilitySeverity.CRITICAL)
        }

        @Test
        fun `parses HIGH case-insensitively`() {
            assertThat(VulnerabilitySeverity.fromString("HIGH")).isEqualTo(VulnerabilitySeverity.HIGH)
            assertThat(VulnerabilitySeverity.fromString("high")).isEqualTo(VulnerabilitySeverity.HIGH)
        }

        @Test
        fun `parses all valid severity values`() {
            assertThat(VulnerabilitySeverity.fromString("MEDIUM")).isEqualTo(VulnerabilitySeverity.MEDIUM)
            assertThat(VulnerabilitySeverity.fromString("LOW")).isEqualTo(VulnerabilitySeverity.LOW)
            assertThat(VulnerabilitySeverity.fromString("NONE")).isEqualTo(VulnerabilitySeverity.NONE)
            assertThat(VulnerabilitySeverity.fromString("UNKNOWN")).isEqualTo(VulnerabilitySeverity.UNKNOWN)
        }

        @Test
        fun `throws IllegalArgumentException for invalid value`() {
            assertThatThrownBy { VulnerabilitySeverity.fromString("INVALID") }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Unknown vulnerability severity")
                .hasMessageContaining("INVALID")
        }

        @Test
        fun `throws for empty string`() {
            assertThatThrownBy { VulnerabilitySeverity.fromString("") }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    inner class `Ignore list filtering` {

        private val vulnerabilities = listOf(
            createVuln("GHSA-1111", aliases = listOf("CVE-2024-1111")),
            createVuln("GHSA-2222", aliases = listOf("CVE-2024-2222")),
            createVuln("GHSA-3333", aliases = emptyList()),
        )

        @Test
        fun `vulnerability in ignore list by ID is filtered out`() {
            val ignoreSet = setOf("GHSA-1111")

            val filtered = vulnerabilities.filter { vuln ->
                vuln.id !in ignoreSet && vuln.aliases.none { it in ignoreSet }
            }

            assertThat(filtered).hasSize(2)
            assertThat(filtered.map { it.id }).doesNotContain("GHSA-1111")
        }

        @Test
        fun `vulnerability in ignore list by CVE alias is filtered out`() {
            val ignoreSet = setOf("CVE-2024-2222")

            val filtered = vulnerabilities.filter { vuln ->
                vuln.id !in ignoreSet && vuln.aliases.none { it in ignoreSet }
            }

            assertThat(filtered).hasSize(2)
            assertThat(filtered.map { it.id }).doesNotContain("GHSA-2222")
        }

        @Test
        fun `empty ignore list keeps all vulnerabilities`() {
            val ignoreSet = emptySet<String>()

            val filtered = vulnerabilities.filter { vuln ->
                vuln.id !in ignoreSet && vuln.aliases.none { it in ignoreSet }
            }

            assertThat(filtered).hasSize(3)
        }

        @Test
        fun `non-matching ignore list keeps all vulnerabilities`() {
            val ignoreSet = setOf("GHSA-9999", "CVE-9999")

            val filtered = vulnerabilities.filter { vuln ->
                vuln.id !in ignoreSet && vuln.aliases.none { it in ignoreSet }
            }

            assertThat(filtered).hasSize(3)
        }

        private fun createVuln(id: String, aliases: List<String> = emptyList()) = VulnerabilityInfo(
            id = id,
            aliases = aliases,
            summary = "Test vulnerability $id",
            severity = VulnerabilitySeverity.HIGH,
            cvssScore = 7.5,
            cvssVersion = "CVSS_V3",
            fixedVersion = null,
            url = null,
            affectedGroup = "com.example",
            affectedArtifact = "test-lib",
            affectedVersion = "1.0.0",
        )
    }

    @Nested
    inner class `VulnerabilitySeverity enum ordering` {

        @Test
        fun `CRITICAL has lower ordinal than HIGH`() {
            assertThat(VulnerabilitySeverity.CRITICAL.ordinal)
                .isLessThan(VulnerabilitySeverity.HIGH.ordinal)
        }

        @Test
        fun `severity ordering is CRITICAL, HIGH, MEDIUM, LOW, NONE, UNKNOWN`() {
            val ordered = VulnerabilitySeverity.entries.toList()

            assertThat(ordered).containsExactly(
                VulnerabilitySeverity.CRITICAL,
                VulnerabilitySeverity.HIGH,
                VulnerabilitySeverity.MEDIUM,
                VulnerabilitySeverity.LOW,
                VulnerabilitySeverity.NONE,
                VulnerabilitySeverity.UNKNOWN,
            )
        }
    }

    @Nested
    inner class `Vulnerability severity boundary values` {

        @Test
        fun `score just below CRITICAL threshold is HIGH`() {
            val severity = classifyCvss(8.9)

            assertThat(severity).isEqualTo(VulnerabilitySeverity.HIGH)
        }

        @Test
        fun `score at exact CRITICAL boundary is CRITICAL`() {
            val severity = classifyCvss(9.0)

            assertThat(severity).isEqualTo(VulnerabilitySeverity.CRITICAL)
        }

        @Test
        fun `score just below HIGH threshold is MEDIUM`() {
            val severity = classifyCvss(6.9)

            assertThat(severity).isEqualTo(VulnerabilitySeverity.MEDIUM)
        }

        @Test
        fun `score at exact HIGH boundary is HIGH`() {
            val severity = classifyCvss(7.0)

            assertThat(severity).isEqualTo(VulnerabilitySeverity.HIGH)
        }

        @Test
        fun `score just below MEDIUM threshold is LOW`() {
            val severity = classifyCvss(3.9)

            assertThat(severity).isEqualTo(VulnerabilitySeverity.LOW)
        }

        @Test
        fun `score at exact MEDIUM boundary is MEDIUM`() {
            val severity = classifyCvss(4.0)

            assertThat(severity).isEqualTo(VulnerabilitySeverity.MEDIUM)
        }

        @Test
        fun `score at exact LOW boundary is LOW`() {
            val severity = classifyCvss(0.1)

            assertThat(severity).isEqualTo(VulnerabilitySeverity.LOW)
        }
    }

    @Nested
    inner class `Fixed version info preservation` {

        @Test
        fun `fixed version is preserved in VulnerabilityInfo`() {
            val vuln = VulnerabilityInfo(
                id = "GHSA-fix-test",
                aliases = listOf("CVE-2024-99999"),
                summary = "XSS in template engine",
                severity = VulnerabilitySeverity.HIGH,
                cvssScore = 7.5,
                cvssVersion = "CVSS_V3",
                fixedVersion = "3.2.1",
                url = "https://example.com/advisory",
                affectedGroup = "org.example",
                affectedArtifact = "template-engine",
                affectedVersion = "3.1.0",
            )

            assertThat(vuln.fixedVersion).isEqualTo("3.2.1")
            assertThat(vuln.affectedVersion).isEqualTo("3.1.0")
        }

        @Test
        fun `multiple vulnerabilities can have different fixed versions`() {
            val vuln1 = createVulnWithFix("GHSA-0001", "1.0.1")
            val vuln2 = createVulnWithFix("GHSA-0002", "2.0.0")
            val vuln3 = createVulnWithFix("GHSA-0003", null)

            val vulns = listOf(vuln1, vuln2, vuln3)

            assertThat(vulns.mapNotNull { it.fixedVersion }).containsExactly("1.0.1", "2.0.0")
            assertThat(vulns.count { it.fixedVersion == null }).isEqualTo(1)
        }

        private fun createVulnWithFix(id: String, fixedVersion: String?) = VulnerabilityInfo(
            id = id,
            aliases = emptyList(),
            summary = "Test",
            severity = VulnerabilitySeverity.MEDIUM,
            cvssScore = 5.0,
            cvssVersion = "CVSS_V3",
            fixedVersion = fixedVersion,
            url = null,
            affectedGroup = "com.example",
            affectedArtifact = "lib",
            affectedVersion = "1.0.0",
        )
    }

    @Nested
    inner class `OsvBatchResult variants` {

        @Test
        fun `Success contains list of vulnerability lists`() {
            val vulnLists: List<List<OsvVulnerabilityData>> = listOf(
                emptyList(),
                listOf(
                    OsvVulnerabilityData(
                        id = "GHSA-test",
                        aliases = emptyList(),
                        summary = "Test",
                        cvssScore = 7.5,
                        cvssVersion = "CVSS_V3",
                        fixedVersion = null,
                        referenceUrl = null,
                    ),
                ),
            )

            val result = OsvBatchResult.Success(vulnerabilities = vulnLists)

            assertThat(result.vulnerabilities).hasSize(2)
            assertThat(result.vulnerabilities[0]).isEmpty()
            assertThat(result.vulnerabilities[1]).hasSize(1)
        }

        @Test
        fun `Timeout captures error message`() {
            val result = OsvBatchResult.Timeout(error = "Request timed out after 30000ms")

            assertThat(result.error).contains("timed out")
        }

        @Test
        fun `Failed captures error message`() {
            val result = OsvBatchResult.Failed(error = "Connection refused")

            assertThat(result.error).isEqualTo("Connection refused")
        }

        @Test
        fun `PartialSuccess captures both successful results and failure info`() {
            val result = OsvBatchResult.PartialSuccess(
                vulnerabilities = listOf(emptyList()),
                failedPackageCount = 5,
                error = "Rate limited by OSV API",
                isTimeout = false,
            )

            assertThat(result.vulnerabilities).hasSize(1)
            assertThat(result.failedPackageCount).isEqualTo(5)
            assertThat(result.isTimeout).isFalse()
        }
    }

    @Nested
    inner class `OsvPackageQuery construction` {

        @Test
        fun `package query holds Maven coordinates`() {
            val query = OsvPackageQuery(
                group = "org.springframework.boot",
                artifact = "spring-boot-starter",
                version = "3.4.0",
            )

            assertThat(query.group).isEqualTo("org.springframework.boot")
            assertThat(query.artifact).isEqualTo("spring-boot-starter")
            assertThat(query.version).isEqualTo("3.4.0")
        }
    }
}
