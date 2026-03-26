package io.github.zenhelix.dependanger.features.license

import io.github.zenhelix.dependanger.features.license.model.LicenseCategory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class LicenseCategorizationTest {

    @Nested
    inner class `normalizing known SPDX identifiers` {

        @Test
        fun `returns exact SPDX ID when already canonical`() {
            assertThat(SpdxLicenseMapper.normalize("MIT")).isEqualTo("MIT")
            assertThat(SpdxLicenseMapper.normalize("Apache-2.0")).isEqualTo("Apache-2.0")
            assertThat(SpdxLicenseMapper.normalize("GPL-3.0-only")).isEqualTo("GPL-3.0-only")
        }

        @Test
        fun `trims whitespace before matching`() {
            assertThat(SpdxLicenseMapper.normalize("  MIT  ")).isEqualTo("MIT")
        }
    }

    @Nested
    inner class `normalizing common aliases` {

        @Test
        fun `maps Apache License 2_0 to Apache-2_0`() {
            assertThat(SpdxLicenseMapper.normalize("Apache License 2.0")).isEqualTo("Apache-2.0")
        }

        @Test
        fun `maps The Apache Software License Version 2_0`() {
            assertThat(SpdxLicenseMapper.normalize("The Apache Software License, Version 2.0")).isEqualTo("Apache-2.0")
        }

        @Test
        fun `maps The MIT License to MIT`() {
            assertThat(SpdxLicenseMapper.normalize("The MIT License")).isEqualTo("MIT")
        }

        @Test
        fun `maps MIT License to MIT`() {
            assertThat(SpdxLicenseMapper.normalize("MIT License")).isEqualTo("MIT")
        }

        @Test
        fun `maps BSD 3-Clause License to BSD-3-Clause`() {
            assertThat(SpdxLicenseMapper.normalize("BSD 3-Clause License")).isEqualTo("BSD-3-Clause")
        }

        @Test
        fun `maps GPL aliases to canonical SPDX IDs`() {
            assertThat(SpdxLicenseMapper.normalize("GPL v3")).isEqualTo("GPL-3.0-only")
            assertThat(SpdxLicenseMapper.normalize("GPLv2")).isEqualTo("GPL-2.0-only")
        }

        @Test
        fun `maps LGPL aliases to canonical SPDX IDs`() {
            assertThat(SpdxLicenseMapper.normalize("LGPL-2.1")).isEqualTo("LGPL-2.1-only")
            assertThat(SpdxLicenseMapper.normalize("LGPL 3.0")).isEqualTo("LGPL-3.0-only")
        }

        @Test
        fun `maps Eclipse aliases to canonical SPDX IDs`() {
            assertThat(SpdxLicenseMapper.normalize("EPL-2.0")).isEqualTo("EPL-2.0")
            assertThat(SpdxLicenseMapper.normalize("Eclipse Public License 2.0")).isEqualTo("EPL-2.0")
        }

        @Test
        fun `maps public domain aliases`() {
            assertThat(SpdxLicenseMapper.normalize("The Unlicense")).isEqualTo("Unlicense")
            assertThat(SpdxLicenseMapper.normalize("CC0 1.0 Universal")).isEqualTo("CC0-1.0")
            assertThat(SpdxLicenseMapper.normalize("Public Domain")).isEqualTo("CC0-1.0")
        }
    }

    @Nested
    inner class `case-insensitive alias matching` {

        @Test
        fun `matches aliases regardless of case`() {
            assertThat(SpdxLicenseMapper.normalize("the mit license")).isEqualTo("MIT")
            assertThat(SpdxLicenseMapper.normalize("THE MIT LICENSE")).isEqualTo("MIT")
            assertThat(SpdxLicenseMapper.normalize("apache license 2.0")).isEqualTo("Apache-2.0")
        }
    }

    @Nested
    inner class `pattern matching for unrecognized names` {

        @Test
        fun `matches strings containing apache and 2 to Apache-2_0`() {
            assertThat(SpdxLicenseMapper.normalize("Some Apache v2 License")).isEqualTo("Apache-2.0")
        }

        @Test
        fun `matches strings containing mit to MIT`() {
            assertThat(SpdxLicenseMapper.normalize("Custom MIT-style License")).isEqualTo("MIT")
        }

        @Test
        fun `matches strings containing bsd and 3 to BSD-3-Clause`() {
            assertThat(SpdxLicenseMapper.normalize("Modified BSD 3 License")).isEqualTo("BSD-3-Clause")
        }

        @Test
        fun `matches strings containing bsd and 2 to BSD-2-Clause`() {
            assertThat(SpdxLicenseMapper.normalize("FreeBSD 2-clause style")).isEqualTo("BSD-2-Clause")
        }

        @Test
        fun `matches generic bsd to BSD-3-Clause`() {
            assertThat(SpdxLicenseMapper.normalize("Some BSD License")).isEqualTo("BSD-3-Clause")
        }

        @Test
        fun `matches strings containing gpl and 3 to GPL-3_0-only`() {
            assertThat(SpdxLicenseMapper.normalize("Custom GPL version 3")).isEqualTo("GPL-3.0-only")
        }

        @Test
        fun `matches strings containing eclipse and 2 to EPL-2_0`() {
            assertThat(SpdxLicenseMapper.normalize("Eclipse License v2")).isEqualTo("EPL-2.0")
        }

        @Test
        fun `matches public domain and cc0 patterns`() {
            assertThat(SpdxLicenseMapper.normalize("Released to public domain")).isEqualTo("CC0-1.0")
            assertThat(SpdxLicenseMapper.normalize("CC0 variant")).isEqualTo("CC0-1.0")
        }

        @Test
        fun `returns null for completely unknown license name`() {
            assertThat(SpdxLicenseMapper.normalize("Some Proprietary License XYZ")).isNull()
        }
    }

    @Nested
    inner class `categorizing licenses` {

        @Test
        fun `categorizes permissive licenses correctly`() {
            assertThat(SpdxLicenseMapper.categorize("MIT")).isEqualTo(LicenseCategory.PERMISSIVE)
            assertThat(SpdxLicenseMapper.categorize("Apache-2.0")).isEqualTo(LicenseCategory.PERMISSIVE)
            assertThat(SpdxLicenseMapper.categorize("BSD-3-Clause")).isEqualTo(LicenseCategory.PERMISSIVE)
            assertThat(SpdxLicenseMapper.categorize("ISC")).isEqualTo(LicenseCategory.PERMISSIVE)
        }

        @Test
        fun `categorizes strong copyleft licenses correctly`() {
            assertThat(SpdxLicenseMapper.categorize("GPL-3.0-only")).isEqualTo(LicenseCategory.STRONG_COPYLEFT)
            assertThat(SpdxLicenseMapper.categorize("GPL-2.0-only")).isEqualTo(LicenseCategory.STRONG_COPYLEFT)
            assertThat(SpdxLicenseMapper.categorize("AGPL-3.0-only")).isEqualTo(LicenseCategory.STRONG_COPYLEFT)
        }

        @Test
        fun `categorizes weak copyleft licenses correctly`() {
            assertThat(SpdxLicenseMapper.categorize("LGPL-2.1-only")).isEqualTo(LicenseCategory.WEAK_COPYLEFT)
            assertThat(SpdxLicenseMapper.categorize("MPL-2.0")).isEqualTo(LicenseCategory.WEAK_COPYLEFT)
            assertThat(SpdxLicenseMapper.categorize("EPL-2.0")).isEqualTo(LicenseCategory.WEAK_COPYLEFT)
            assertThat(SpdxLicenseMapper.categorize("CDDL-1.0")).isEqualTo(LicenseCategory.WEAK_COPYLEFT)
        }

        @Test
        fun `categorizes public domain licenses correctly`() {
            assertThat(SpdxLicenseMapper.categorize("Unlicense")).isEqualTo(LicenseCategory.PUBLIC_DOMAIN)
            assertThat(SpdxLicenseMapper.categorize("CC0-1.0")).isEqualTo(LicenseCategory.PUBLIC_DOMAIN)
            assertThat(SpdxLicenseMapper.categorize("WTFPL")).isEqualTo(LicenseCategory.PUBLIC_DOMAIN)
        }

        @Test
        fun `categorizes unknown SPDX ID as PROPRIETARY`() {
            assertThat(SpdxLicenseMapper.categorize("Some-Custom-1.0")).isEqualTo(LicenseCategory.PROPRIETARY)
        }

        @Test
        fun `categorizes as PROPRIETARY when no spdxId but has licenseName`() {
            assertThat(SpdxLicenseMapper.categorize(null, "Some Custom License")).isEqualTo(LicenseCategory.PROPRIETARY)
        }

        @Test
        fun `categorizes as UNKNOWN when neither spdxId nor licenseName provided`() {
            assertThat(SpdxLicenseMapper.categorize(null, null)).isEqualTo(LicenseCategory.UNKNOWN)
        }

        @Test
        fun `spdxId takes priority over licenseName for categorization`() {
            assertThat(SpdxLicenseMapper.categorize("MIT", "Some random name")).isEqualTo(LicenseCategory.PERMISSIVE)
        }
    }
}
