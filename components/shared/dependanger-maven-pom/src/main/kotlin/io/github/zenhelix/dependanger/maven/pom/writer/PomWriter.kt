package io.github.zenhelix.dependanger.maven.pom.writer

import io.github.zenhelix.dependanger.maven.pom.model.PomDependency
import io.github.zenhelix.dependanger.maven.pom.model.PomProject
import io.github.zenhelix.dependanger.maven.pom.util.MavenConstants
import io.github.zenhelix.dependanger.maven.pom.util.escapeXml

public class PomWriter(
    private val config: PomWriterConfig = PomWriterConfig(),
) {

    public fun write(
        project: PomProject,
        beforeDependency: ((index: Int, dep: PomDependency) -> String?)? = null,
    ): String {
        val sb = StringBuilder()
        val nl = if (config.prettyPrint) "\n" else ""
        val indent = if (config.prettyPrint) config.indent else ""

        if (config.includeXmlDeclaration) {
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>$nl")
        }

        val attrSep = if (config.prettyPrint) "\n${indent.repeat(2)}" else " "
        sb.append("<project xmlns=\"${MavenConstants.MAVEN_POM_NS}\"")
        sb.append("${attrSep}xmlns:xsi=\"${MavenConstants.MAVEN_XSI_NS}\"")
        sb.append("${attrSep}xsi:schemaLocation=\"${MavenConstants.MAVEN_XSD_LOCATION}\">$nl")

        sb.append("$indent<modelVersion>${project.modelVersion.escapeXml()}</modelVersion>$nl")
        sb.append("$indent<groupId>${project.coordinates.groupId.escapeXml()}</groupId>$nl")
        sb.append("$indent<artifactId>${project.coordinates.artifactId.escapeXml()}</artifactId>$nl")
        sb.append("$indent<version>${project.coordinates.version.escapeXml()}</version>$nl")
        sb.append("$indent<packaging>${project.packaging.escapeXml()}</packaging>$nl")

        project.name?.takeIf { it.isNotBlank() }?.let {
            sb.append("$indent<name>${it.escapeXml()}</name>$nl")
        }
        project.description?.takeIf { it.isNotBlank() }?.let {
            sb.append("$indent<description>${it.escapeXml()}</description>$nl")
        }

        project.parent?.let { parent ->
            sb.append("${nl}$indent<parent>$nl")
            sb.append("$indent$indent<groupId>${parent.coordinates.groupId.escapeXml()}</groupId>$nl")
            sb.append("$indent$indent<artifactId>${parent.coordinates.artifactId.escapeXml()}</artifactId>$nl")
            sb.append("$indent$indent<version>${parent.coordinates.version.escapeXml()}</version>$nl")
            parent.relativePath?.let {
                sb.append("$indent$indent<relativePath>${it.escapeXml()}</relativePath>$nl")
            }
            sb.append("$indent</parent>$nl")
        }

        if (project.properties.entries.isNotEmpty()) {
            sb.append("${nl}$indent<properties>$nl")
            for ((key, value) in project.properties.entries) {
                sb.append("$indent$indent<$key>${value.escapeXml()}</$key>$nl")
            }
            sb.append("$indent</properties>$nl")
        }

        project.dependencyManagement?.let { dm ->
            if (dm.dependencies.isNotEmpty()) {
                sb.append("${nl}$indent<dependencyManagement>$nl")
                sb.append("$indent$indent<dependencies>$nl")

                for ((index, dep) in dm.dependencies.withIndex()) {
                    beforeDependency?.invoke(index, dep)?.let { sb.append(it) }
                    sb.append(writeDependency(dep, indent))
                }

                sb.append("$indent$indent</dependencies>$nl")
                sb.append("$indent</dependencyManagement>$nl")
            }
        }

        sb.append("</project>$nl")

        return sb.toString()
    }

    private fun writeDependency(dep: PomDependency, baseIndent: String): String {
        val nl = if (config.prettyPrint) "\n" else ""
        val indent3 = if (config.prettyPrint) baseIndent.repeat(3) else ""
        val indent4 = if (config.prettyPrint) baseIndent.repeat(4) else ""

        val sb = StringBuilder()
        sb.append("$indent3<dependency>$nl")
        sb.append("$indent4<groupId>${dep.groupId.escapeXml()}</groupId>$nl")
        sb.append("$indent4<artifactId>${dep.artifactId.escapeXml()}</artifactId>$nl")

        dep.version?.let {
            sb.append("$indent4<version>${it.escapeXml()}</version>$nl")
        }

        dep.type?.let {
            sb.append("$indent4<type>${it.escapeXml()}</type>$nl")
        }

        dep.scope?.let {
            sb.append("$indent4<scope>${it.escapeXml()}</scope>$nl")
        }

        if (dep.optional) {
            sb.append("$indent4<optional>true</optional>$nl")
        }

        sb.append("$indent3</dependency>$nl")
        return sb.toString()
    }
}
