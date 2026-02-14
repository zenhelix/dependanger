package io.github.zenhelix.dependanger.maven.pom.parser

import io.github.zenhelix.dependanger.maven.pom.model.PomCoordinates
import io.github.zenhelix.dependanger.maven.pom.model.PomDependency
import io.github.zenhelix.dependanger.maven.pom.model.PomDependencyManagement
import io.github.zenhelix.dependanger.maven.pom.model.PomLicense
import io.github.zenhelix.dependanger.maven.pom.model.PomParent
import io.github.zenhelix.dependanger.maven.pom.model.PomProject
import io.github.zenhelix.dependanger.maven.pom.model.PomProperties
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory

public object PomParser {

    public fun parse(pomXml: String): PomProject {
        try {
            val doc = parseXmlDocument(pomXml)
            return extractProject(doc)
        } catch (e: PomParseException) {
            throw e
        } catch (e: Exception) {
            throw PomParseException("Failed to parse POM XML", e)
        }
    }

    private fun parseXmlDocument(xml: String): Document {
        val factory = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        }
        val builder = factory.newDocumentBuilder()
        return builder.parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))
    }

    private fun extractProject(doc: Document): PomProject {
        val root = doc.documentElement

        val modelVersion = root.getDirectChildText("modelVersion") ?: "4.0.0"
        val groupId = root.getDirectChildText("groupId")
        val artifactId = root.getDirectChildText("artifactId")
            ?: throw PomParseException("Missing required element: artifactId")
        val version = root.getDirectChildText("version")
        val parent = extractParent(root)

        val effectiveGroupId = groupId ?: parent?.coordinates?.groupId
        ?: throw PomParseException("Missing required element: groupId (not in project or parent)")
        val effectiveVersion = version ?: parent?.coordinates?.version
        ?: throw PomParseException("Missing required element: version (not in project or parent)")

        val coordinates = PomCoordinates(effectiveGroupId, artifactId, effectiveVersion)
        val packaging = root.getDirectChildText("packaging") ?: "jar"
        val name = root.getDirectChildText("name")
        val description = root.getDirectChildText("description")
        val licenses = extractLicenses(root)
        val properties = extractProperties(root)
        val dependencyManagement = extractDependencyManagement(root)

        return PomProject(
            modelVersion = modelVersion,
            coordinates = coordinates,
            packaging = packaging,
            parent = parent,
            name = name,
            description = description,
            licenses = licenses,
            properties = properties,
            dependencyManagement = dependencyManagement,
        )
    }

    private fun extractParent(root: Element): PomParent? {
        val parentElements = root.getElementsByTagName("parent")
        if (parentElements.length == 0) return null

        val parentEl = parentElements.item(0) as? Element ?: return null
        if (parentEl.parentNode != root) return null

        val groupId = parentEl.getDirectChildText("groupId") ?: return null
        val artifactId = parentEl.getDirectChildText("artifactId") ?: return null
        val version = parentEl.getDirectChildText("version") ?: return null
        val relativePath = parentEl.getDirectChildText("relativePath")

        return PomParent(
            coordinates = PomCoordinates(groupId, artifactId, version),
            relativePath = relativePath,
        )
    }

    private fun extractLicenses(root: Element): List<PomLicense> {
        val licenses = mutableListOf<PomLicense>()
        val licensesElements = root.getElementsByTagName("licenses")

        for (i in 0 until licensesElements.length) {
            val licensesEl = licensesElements.item(i) as? Element ?: continue
            if (licensesEl.parentNode != root) continue

            val licenseElements = licensesEl.getElementsByTagName("license")
            for (j in 0 until licenseElements.length) {
                val licenseEl = licenseElements.item(j) as? Element ?: continue
                if (licenseEl.parentNode != licensesEl) continue

                licenses.add(
                    PomLicense(
                        name = licenseEl.getDirectChildText("name"),
                        url = licenseEl.getDirectChildText("url"),
                    )
                )
            }
        }

        return licenses
    }

    private fun extractProperties(root: Element): PomProperties {
        val props = mutableMapOf<String, String>()
        val propertiesElements = root.getElementsByTagName("properties")

        for (i in 0 until propertiesElements.length) {
            val propsEl = propertiesElements.item(i) as? Element ?: continue
            if (propsEl.parentNode != root) continue

            val children = propsEl.childNodes
            for (j in 0 until children.length) {
                val child = children.item(j) as? Element ?: continue
                props[child.tagName] = child.textContent.trim()
            }
        }

        return PomProperties(props)
    }

    private fun extractDependencyManagement(root: Element): PomDependencyManagement? {
        val dependencies = mutableListOf<PomDependency>()

        val dmElements = root.getElementsByTagName("dependencyManagement")
        for (i in 0 until dmElements.length) {
            val dmEl = dmElements.item(i) as? Element ?: continue
            if (dmEl.parentNode != root) continue

            val depsElements = dmEl.getElementsByTagName("dependencies")
            for (j in 0 until depsElements.length) {
                val depsEl = depsElements.item(j) as? Element ?: continue
                if (depsEl.parentNode != dmEl) continue

                val depElements = depsEl.getElementsByTagName("dependency")
                for (k in 0 until depElements.length) {
                    val depEl = depElements.item(k) as? Element ?: continue
                    if (depEl.parentNode != depsEl) continue

                    dependencies.add(extractDependency(depEl))
                }
            }
        }

        return if (dependencies.isEmpty()) null else PomDependencyManagement(dependencies)
    }

    private fun extractDependency(depEl: Element): PomDependency {
        val groupId = depEl.getDirectChildText("groupId")
            ?: throw PomParseException("Dependency missing groupId")
        val artifactId = depEl.getDirectChildText("artifactId")
            ?: throw PomParseException("Dependency missing artifactId")
        val version = depEl.getDirectChildText("version")
        val scope = depEl.getDirectChildText("scope")
        val type = depEl.getDirectChildText("type")
        val optional = depEl.getDirectChildText("optional")?.toBoolean() ?: false

        return PomDependency(groupId, artifactId, version, scope, type, optional)
    }

    private fun Element.getDirectChildText(tagName: String): String? {
        val children = childNodes
        for (i in 0 until children.length) {
            val child = children.item(i) as? Element ?: continue
            if (child.tagName == tagName) {
                return child.textContent.trim().ifEmpty { null }
            }
        }
        return null
    }
}
