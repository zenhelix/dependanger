package io.github.zenhelix.dependanger.maven.pom.xml

import io.github.zenhelix.dependanger.maven.pom.util.escapeXml
import io.github.zenhelix.dependanger.maven.pom.util.escapeXmlComment

@DslMarker
internal annotation class XmlDslMarker

internal data class XmlConfig(
    val prettyPrint: Boolean,
    val indent: String,
)

internal fun xml(config: XmlConfig, builder: XmlBuilder.() -> Unit): String {
    return XmlBuilder(config).apply(builder).render()
}

@XmlDslMarker
internal class XmlBuilder(private val config: XmlConfig) {
    private val nodes = mutableListOf<XmlNode>()

    internal fun xmlDeclaration(version: String = "1.0", encoding: String = "UTF-8") {
        nodes.add(XmlDeclaration(version, encoding))
    }

    internal fun element(name: String, attrs: Map<String, String> = emptyMap(), builder: XmlElement.() -> Unit = {}) {
        val element = XmlElement(name, attrs, config)
        element.builder()
        nodes.add(element)
    }

    internal fun render(): String {
        val sb = StringBuilder()
        for (node in nodes) {
            node.render(sb, 0, config)
        }
        return sb.toString()
    }
}

internal sealed interface XmlNode {
    fun render(sb: StringBuilder, depth: Int, config: XmlConfig)
}

internal class XmlDeclaration(
    private val version: String,
    private val encoding: String,
) : XmlNode {
    override fun render(sb: StringBuilder, depth: Int, config: XmlConfig) {
        val nl = if (config.prettyPrint) "\n" else ""
        sb.append("<?xml version=\"$version\" encoding=\"$encoding\"?>$nl")
    }
}

@XmlDslMarker
internal class XmlElement(
    private val name: String,
    private val attrs: Map<String, String>,
    private val config: XmlConfig,
) : XmlNode {
    private val children = mutableListOf<XmlNode>()

    internal fun element(name: String, attrs: Map<String, String> = emptyMap(), builder: XmlElement.() -> Unit = {}) {
        val element = XmlElement(name, attrs, config)
        element.builder()
        children.add(element)
    }

    internal fun text(content: String) {
        children.add(XmlText(content))
    }

    internal fun comment(text: String) {
        children.add(XmlComment(text))
    }

    internal fun blankLine() {
        children.add(XmlBlankLine())
    }

    override fun render(sb: StringBuilder, depth: Int, config: XmlConfig) {
        val nl = if (config.prettyPrint) "\n" else ""
        val indent = if (config.prettyPrint) config.indent.repeat(depth) else ""

        sb.append("$indent<$name")

        // Render attributes
        if (attrs.isNotEmpty()) {
            val attrList = attrs.entries.toList()
            if (attrList.size > 1 && config.prettyPrint) {
                // First attribute inline, subsequent on separate lines
                val (firstKey, firstValue) = attrList.first()
                sb.append(" $firstKey=\"${firstValue.escapeXml()}\"")
                val attrIndent = indent + config.indent.repeat(2)
                for (i in 1 until attrList.size) {
                    val (key, value) = attrList[i]
                    sb.append("\n$attrIndent$key=\"${value.escapeXml()}\"")
                }
            } else {
                for ((key, value) in attrList) {
                    sb.append(" $key=\"${value.escapeXml()}\"")
                }
            }
        }

        if (children.isEmpty()) {
            sb.append("/>$nl")
            return
        }

        // Check if the only child is a single text node — render inline
        val isSingleText = children.size == 1 && children[0] is XmlText
        if (isSingleText) {
            sb.append(">")
            children[0].render(sb, depth + 1, config)
            sb.append("</$name>$nl")
        } else {
            sb.append(">$nl")
            for (child in children) {
                child.render(sb, depth + 1, config)
            }
            sb.append("$indent</$name>$nl")
        }
    }
}

internal class XmlText(private val content: String) : XmlNode {
    override fun render(sb: StringBuilder, depth: Int, config: XmlConfig) {
        // When rendered inline (inside element), no indent/newline
        // When rendered as part of mixed content, with indent/newline
        sb.append(content.escapeXml())
    }
}

internal class XmlComment(private val text: String) : XmlNode {
    override fun render(sb: StringBuilder, depth: Int, config: XmlConfig) {
        val nl = if (config.prettyPrint) "\n" else ""
        val indent = if (config.prettyPrint) config.indent.repeat(depth) else ""
        sb.append("$indent<!-- ${text.escapeXmlComment()} -->$nl")
    }
}

internal class XmlBlankLine : XmlNode {
    override fun render(sb: StringBuilder, depth: Int, config: XmlConfig) {
        if (config.prettyPrint) sb.append("\n")
    }
}
