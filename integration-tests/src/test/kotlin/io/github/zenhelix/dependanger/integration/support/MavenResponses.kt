package io.github.zenhelix.dependanger.integration.support

data class PomDep(
    val group: String,
    val artifact: String,
    val version: String,
    val scope: String? = null,
)

data class PomParent(
    val group: String,
    val artifact: String,
    val version: String,
)

object MavenResponses {

    fun mavenMetadataXml(group: String, artifact: String, versions: List<String>): String = buildString {
        appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        appendLine("<metadata>")
        appendLine("  <groupId>$group</groupId>")
        appendLine("  <artifactId>$artifact</artifactId>")
        appendLine("  <versioning>")
        appendLine("    <versions>")
        for (v in versions) {
            appendLine("      <version>$v</version>")
        }
        appendLine("    </versions>")
        appendLine("  </versioning>")
        append("</metadata>")
    }

    fun pomXml(
        group: String,
        artifact: String,
        version: String,
        dependencies: List<PomDep> = emptyList(),
        parent: PomParent? = null,
    ): String = buildString {
        appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        appendLine("""<project xmlns="http://maven.apache.org/POM/4.0.0">""")
        appendLine("  <modelVersion>4.0.0</modelVersion>")
        if (parent != null) {
            appendLine("  <parent>")
            appendLine("    <groupId>${parent.group}</groupId>")
            appendLine("    <artifactId>${parent.artifact}</artifactId>")
            appendLine("    <version>${parent.version}</version>")
            appendLine("  </parent>")
        }
        appendLine("  <groupId>$group</groupId>")
        appendLine("  <artifactId>$artifact</artifactId>")
        appendLine("  <version>$version</version>")
        if (dependencies.isNotEmpty()) {
            appendLine("  <dependencies>")
            for (dep in dependencies) {
                appendLine("    <dependency>")
                appendLine("      <groupId>${dep.group}</groupId>")
                appendLine("      <artifactId>${dep.artifact}</artifactId>")
                appendLine("      <version>${dep.version}</version>")
                if (dep.scope != null) {
                    appendLine("      <scope>${dep.scope}</scope>")
                }
                appendLine("    </dependency>")
            }
            appendLine("  </dependencies>")
        }
        append("</project>")
    }

    fun bomPomXml(
        group: String,
        artifact: String,
        version: String,
        managed: List<PomDep>,
    ): String = buildString {
        appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        appendLine("""<project xmlns="http://maven.apache.org/POM/4.0.0">""")
        appendLine("  <modelVersion>4.0.0</modelVersion>")
        appendLine("  <groupId>$group</groupId>")
        appendLine("  <artifactId>$artifact</artifactId>")
        appendLine("  <version>$version</version>")
        appendLine("  <packaging>pom</packaging>")
        if (managed.isNotEmpty()) {
            appendLine("  <dependencyManagement>")
            appendLine("    <dependencies>")
            for (dep in managed) {
                appendLine("      <dependency>")
                appendLine("        <groupId>${dep.group}</groupId>")
                appendLine("        <artifactId>${dep.artifact}</artifactId>")
                appendLine("        <version>${dep.version}</version>")
                if (dep.scope != null) {
                    appendLine("        <scope>${dep.scope}</scope>")
                }
                appendLine("      </dependency>")
            }
            appendLine("    </dependencies>")
            appendLine("  </dependencyManagement>")
        }
        append("</project>")
    }
}
