package io.github.zenhelix.dependanger.features.resolver

import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory

public class PomXmlParser {

    public fun parseBomContent(pomXml: String): PomParseResult {
        val doc = parseXmlDocument(pomXml)

        val parent = extractParent(doc)

        val explicitProperties = extractProperties(doc)
        val syntheticProperties = buildSyntheticProperties(doc, parent)
        // synthetic + explicit: explicit overrides synthetic (as in Maven)
        val properties = syntheticProperties + explicitProperties

        val rawDependencies = extractDependencyManagement(doc)

        return PomParseResult(properties, parent, rawDependencies)
    }

    public fun resolveProperty(
        value: String,
        properties: Map<String, String>,
    ): String {
        val pattern = Regex("""\$\{([^}]+)\}""")
        return pattern.replace(value) { match ->
            val propName = match.groupValues[1]
            properties[propName]
                ?: throw IllegalStateException(
                    "Unresolved property '$propName' in BOM"
                )
        }
    }

    private fun parseXmlDocument(xml: String): Document {
        val factory = DocumentBuilderFactory.newInstance()
        // Disable external entities for security
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        val builder = factory.newDocumentBuilder()
        return builder.parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))
    }

    private fun extractParent(doc: Document): ParentPom? {
        val parentElements = doc.documentElement.getElementsByTagName("parent")
        if (parentElements.length == 0) return null

        val parentEl = parentElements.item(0) as? Element ?: return null
        // Only consider direct child <parent> of <project>
        if (parentEl.parentNode != doc.documentElement) return null

        val groupId = parentEl.getDirectChildText("groupId") ?: return null
        val artifactId = parentEl.getDirectChildText("artifactId") ?: return null
        val version = parentEl.getDirectChildText("version") ?: return null

        return ParentPom(groupId, artifactId, version)
    }

    private fun extractProperties(doc: Document): Map<String, String> {
        val props = mutableMapOf<String, String>()
        val propertiesElements = doc.documentElement.getElementsByTagName("properties")

        for (i in 0 until propertiesElements.length) {
            val propsEl = propertiesElements.item(i) as? Element ?: continue
            // Only consider direct child <properties> of <project>
            if (propsEl.parentNode != doc.documentElement) continue

            val children = propsEl.childNodes
            for (j in 0 until children.length) {
                val child = children.item(j) as? Element ?: continue
                props[child.tagName] = child.textContent.trim()
            }
        }
        return props
    }

    private fun buildSyntheticProperties(doc: Document, parent: ParentPom?): Map<String, String> {
        val props = mutableMapOf<String, String>()

        val groupId = doc.documentElement.getDirectChildText("groupId")
        val artifactId = doc.documentElement.getDirectChildText("artifactId")
        val version = doc.documentElement.getDirectChildText("version")

        groupId?.let { props["project.groupId"] = it }
        artifactId?.let { props["project.artifactId"] = it }
        version?.let { props["project.version"] = it }

        parent?.let {
            props["project.parent.groupId"] = it.group
            props["project.parent.artifactId"] = it.artifact
            props["project.parent.version"] = it.version
            // If project.groupId/version not set explicitly, inherit from parent (Maven standard)
            props.putIfAbsent("project.groupId", it.group)
            props.putIfAbsent("project.version", it.version)
        }

        return props
    }

    private fun extractDependencyManagement(doc: Document): List<RawBomDependency> {
        val dependencies = mutableListOf<RawBomDependency>()

        val dmElements = doc.documentElement.getElementsByTagName("dependencyManagement")
        for (i in 0 until dmElements.length) {
            val dmEl = dmElements.item(i) as? Element ?: continue
            // Only consider direct child <dependencyManagement> of <project>
            if (dmEl.parentNode != doc.documentElement) continue

            val depsElements = dmEl.getElementsByTagName("dependencies")
            for (j in 0 until depsElements.length) {
                val depsEl = depsElements.item(j) as? Element ?: continue
                // Only direct child <dependencies> of <dependencyManagement>
                if (depsEl.parentNode != dmEl) continue

                val depElements = depsEl.getElementsByTagName("dependency")
                for (k in 0 until depElements.length) {
                    val depEl = depElements.item(k) as? Element ?: continue
                    // Only direct child <dependency> of <dependencies>
                    if (depEl.parentNode != depsEl) continue

                    val groupId = depEl.getDirectChildText("groupId") ?: continue
                    val artifactId = depEl.getDirectChildText("artifactId") ?: continue
                    val depVersion = depEl.getDirectChildText("version") ?: continue
                    val scope = depEl.getDirectChildText("scope")
                    val type = depEl.getDirectChildText("type")

                    dependencies.add(
                        RawBomDependency(
                            group = groupId,
                            artifact = artifactId,
                            version = depVersion,
                            scope = scope,
                            type = type,
                        )
                    )
                }
            }
        }
        return dependencies
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
