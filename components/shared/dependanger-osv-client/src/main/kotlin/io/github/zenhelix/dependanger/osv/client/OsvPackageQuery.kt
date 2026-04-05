package io.github.zenhelix.dependanger.osv.client

public data class OsvPackageQuery(
    val group: String,
    val artifact: String,
    val version: String,
)
