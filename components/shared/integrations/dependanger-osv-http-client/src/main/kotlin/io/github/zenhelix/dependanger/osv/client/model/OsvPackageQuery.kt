package io.github.zenhelix.dependanger.osv.client.model

public data class OsvPackageQuery(
    val group: String,
    val artifact: String,
    val version: String,
)
