package io.github.zenhelix.dependanger.effective.model

import kotlinx.serialization.Serializable

@Serializable
public data class UpdateAvailableInfo(
    val alias: String,
    val group: String,
    val artifact: String,
    val currentVersion: String,
    val latestVersion: String,
    val latestStable: String? = null,
    val latestAny: String? = null,
    val updateType: UpdateType,
    val repository: String? = null,
)
