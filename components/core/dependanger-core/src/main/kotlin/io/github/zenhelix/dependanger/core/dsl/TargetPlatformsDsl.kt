package io.github.zenhelix.dependanger.core.dsl

import io.github.zenhelix.dependanger.core.model.AndroidConstraints
import io.github.zenhelix.dependanger.core.model.JdkConstraints
import io.github.zenhelix.dependanger.core.model.KmpTarget
import io.github.zenhelix.dependanger.core.model.KotlinConstraints
import io.github.zenhelix.dependanger.core.model.TargetPlatform

@DependangerDslMarker
public class TargetPlatformsDsl {
    private val _platforms: MutableList<TargetPlatform> = mutableListOf()
    public val platforms: List<TargetPlatform> get() = _platforms.toList()

    public fun platform(name: String, block: PlatformDsl.() -> Unit) {
        val dsl = PlatformDsl().apply(block)
        _platforms.add(
            TargetPlatform(
                name = name,
                jdkConstraints = dsl.jdkConstraints,
                androidConstraints = dsl.androidConstraints,
                kotlinConstraints = dsl.kotlinConstraints,
                supportedTargets = dsl.targets.toSet(),
            )
        )
    }
}

@DependangerDslMarker
public class PlatformDsl {
    public var jdkConstraints: JdkConstraints? = null
    public var androidConstraints: AndroidConstraints? = null
    public var kotlinConstraints: KotlinConstraints? = null

    private val _targets: MutableSet<KmpTarget> = mutableSetOf()
    public val targets: Set<KmpTarget> get() = _targets.toSet()

    public fun jdk(block: JdkConstraintsDsl.() -> Unit) {
        val dsl = JdkConstraintsDsl().apply(block)
        jdkConstraints = JdkConstraints(min = dsl.min, max = dsl.max)
    }

    public fun android(block: AndroidConstraintsDsl.() -> Unit) {
        val dsl = AndroidConstraintsDsl().apply(block)
        androidConstraints = AndroidConstraints(minSdk = dsl.minSdk, targetSdk = dsl.targetSdk)
    }

    public fun kotlin(block: KotlinConstraintsDsl.() -> Unit) {
        val dsl = KotlinConstraintsDsl().apply(block)
        kotlinConstraints = KotlinConstraints(min = dsl.min, max = dsl.max)
    }

    public fun targets(block: TargetsDsl.() -> Unit) {
        val dsl = TargetsDsl().apply(block)
        _targets.addAll(dsl.targets)
    }
}

@DependangerDslMarker
public class AndroidConstraintsDsl {
    public var minSdk: Int? = null
    public var targetSdk: Int? = null
}

@DependangerDslMarker
public class TargetsDsl {
    private val _targets: MutableSet<KmpTarget> = mutableSetOf()
    public val targets: Set<KmpTarget> get() = _targets.toSet()

    public fun jvm() {
        _targets.add(KmpTarget.JVM)
    }

    public fun js() {
        _targets.add(KmpTarget.JS)
    }

    public fun android() {
        _targets.add(KmpTarget.ANDROID)
    }

    public fun ios() {
        _targets.add(KmpTarget.IOS)
    }

    public fun macos() {
        _targets.add(KmpTarget.MACOS)
    }

    public fun linux() {
        _targets.add(KmpTarget.LINUX)
    }

    public fun mingw() {
        _targets.add(KmpTarget.MINGW)
    }

    public fun native() {
        _targets.add(KmpTarget.NATIVE)
    }

    public fun wasmJs() {
        _targets.add(KmpTarget.WASM_JS)
    }

    public fun wasmWasi() {
        _targets.add(KmpTarget.WASM_WASI)
    }
}
