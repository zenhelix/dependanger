package io.github.zenhelix.dependanger.osv.client.internal

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class OsvBatchRequest(
    val queries: List<OsvQuery>,
)

@Serializable
internal data class OsvQuery(
    val version: String,
    @SerialName("package")
    val pkg: OsvPackage,
)

@Serializable
internal data class OsvPackage(
    val name: String,
    val ecosystem: String,
)

@Serializable
internal data class OsvBatchResponse(
    val results: List<OsvQueryResult>,
)

@Serializable
internal data class OsvQueryResult(
    val vulns: List<OsvVulnerability>? = null,
)

@Serializable
internal data class OsvVulnerability(
    val id: String,
    val aliases: List<String>? = null,
    val summary: String? = null,
    val severity: List<OsvSeverity>? = null,
    val affected: List<OsvAffected>? = null,
    val references: List<OsvReference>? = null,
)

@Serializable
internal data class OsvSeverity(
    val type: String,
    val score: String,
)

@Serializable
internal data class OsvAffected(
    val ranges: List<OsvRange>? = null,
)

@Serializable
internal data class OsvRange(
    val type: String,
    val events: List<OsvEvent>? = null,
)

@Serializable
internal data class OsvEvent(
    val introduced: String? = null,
    val fixed: String? = null,
)

@Serializable
internal data class OsvReference(
    val type: String? = null,
    val url: String? = null,
)
