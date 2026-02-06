package io.github.zenhelix.dependanger.effective.model

import kotlinx.serialization.Serializable

@Serializable
public data class UpdateAvailableInfo(
    val alias: String,
    val group: String,
    val artifact: String,
    val currentVersion: String,
    val latestVersion: String,
    val updateType: UpdateType,
)
