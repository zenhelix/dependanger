package io.github.zenhelix.dependanger.core.dsl

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Property delegate that tracks whether a value was explicitly set.
 *
 * Used in preset DSL classes to distinguish "not configured" from "explicitly set to default".
 * When [isSet] is `false`, the field retains its default and should not override target settings.
 * When [isSet] is `true`, the field was explicitly assigned and should be applied during merge.
 */
public class Trackable<T>(default: T) : ReadWriteProperty<Any?, T> {
    public var isSet: Boolean = false
        private set

    private var value: T = default

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = value

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
        isSet = true
    }
}
