package io.github.zenhelix.dependanger.effective.processor

import io.github.zenhelix.dependanger.core.model.filter.TagFilter

internal fun passesTagFilter(tags: Set<String>, filter: TagFilter): Boolean {
    val passesIncludes = if (filter.includes.isEmpty()) true
    else filter.includes.any { include ->
        val anyOfOk = include.anyOf.isEmpty() || tags.any { it in include.anyOf }
        val allOfOk = include.allOf.isEmpty() || tags.containsAll(include.allOf)
        anyOfOk && allOfOk
    }

    val passesExcludes = filter.excludes.all { exclude ->
        exclude.anyOf.isEmpty() || tags.none { it in exclude.anyOf }
    }

    return passesIncludes && passesExcludes
}
