package io.github.zenhelix.dependanger.core.dsl

import io.github.zenhelix.dependanger.core.model.ProcessingPreset

@DependangerDslMarker
public class ProcessingDsl {
    public var preset: ProcessingPreset = ProcessingPreset.DEFAULT
    private val _disabledProcessors: MutableList<String> = mutableListOf()
    public val disabledProcessors: List<String> get() = _disabledProcessors.toList()

    public fun disableProcessor(id: String) {
        _disabledProcessors.add(id)
    }
}
