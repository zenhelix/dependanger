package io.github.zenhelix.dependanger.maven.pom.writer

import io.github.zenhelix.dependanger.maven.pom.model.PomDependency
import io.github.zenhelix.dependanger.maven.pom.model.PomProject
import io.github.zenhelix.dependanger.maven.pom.util.MavenConstants
import io.github.zenhelix.dependanger.maven.pom.xml.XmlConfig
import io.github.zenhelix.dependanger.maven.pom.xml.XmlElement
import io.github.zenhelix.dependanger.maven.pom.xml.xml

public class PomWriter(
    private val config: PomWriterConfig = PomWriterConfig(),
) {

    public fun write(
        project: PomProject,
        dependencyComments: Map<Int, String> = emptyMap(),
    ): String {
        val xmlConfig = XmlConfig(config.prettyPrint, config.indent)

        return xml(xmlConfig) {
            if (config.includeXmlDeclaration) {
                xmlDeclaration()
            }

            element(
                "project", attrs = linkedMapOf(
                    "xmlns" to MavenConstants.MAVEN_POM_NS,
                    "xmlns:xsi" to MavenConstants.MAVEN_XSI_NS,
                    "xsi:schemaLocation" to MavenConstants.MAVEN_XSD_LOCATION,
                )
            ) {
                element("modelVersion") { text(project.modelVersion) }
                element("groupId") { text(project.coordinates.groupId) }
                element("artifactId") { text(project.coordinates.artifactId) }
                element("version") { text(project.coordinates.version) }
                element("packaging") { text(project.packaging) }

                project.name?.takeIf { it.isNotBlank() }?.let {
                    element("name") { text(it) }
                }

                project.description?.takeIf { it.isNotBlank() }?.let {
                    element("description") { text(it) }
                }

                project.parent?.let { parent ->
                    blankLine()
                    element("parent") {
                        element("groupId") { text(parent.coordinates.groupId) }
                        element("artifactId") { text(parent.coordinates.artifactId) }
                        element("version") { text(parent.coordinates.version) }
                        parent.relativePath?.let {
                            element("relativePath") { text(it) }
                        }
                    }
                }

                if (project.properties.entries.isNotEmpty()) {
                    blankLine()
                    element("properties") {
                        for ((key, value) in project.properties.entries) {
                            element(key) { text(value) }
                        }
                    }
                }

                project.dependencyManagement?.let { dm ->
                    if (dm.dependencies.isNotEmpty()) {
                        blankLine()
                        element("dependencyManagement") {
                            element("dependencies") {
                                for ((index, dep) in dm.dependencies.withIndex()) {
                                    dependencyComments[index]?.let { comment(it) }
                                    writeDependency(dep)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun XmlElement.writeDependency(dep: PomDependency) {
        element("dependency") {
            element("groupId") { text(dep.groupId) }
            element("artifactId") { text(dep.artifactId) }

            dep.version?.let {
                element("version") { text(it) }
            }

            dep.type?.let {
                element("type") { text(it) }
            }

            dep.scope?.let {
                element("scope") { text(it) }
            }

            if (dep.optional) {
                element("optional") { text("true") }
            }
        }
    }
}
