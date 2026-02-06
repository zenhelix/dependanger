package io.github.zenhelix.dependanger.core.dsl

import io.github.zenhelix.dependanger.core.model.AndroidConstraints
import io.github.zenhelix.dependanger.core.model.JdkConstraints
import io.github.zenhelix.dependanger.core.model.KmpTarget
import io.github.zenhelix.dependanger.core.model.KotlinConstraints
import io.github.zenhelix.dependanger.core.model.TargetPlatform

@DependangerDslMarker
public class TargetPlatformsDsl {
    public val platforms: MutableList<TargetPlatform> = mutableListOf()

    public fun platform(name: String, block: PlatformDsl.() -> Unit) {
        val dsl = PlatformDsl().apply(block)
        platforms.add(
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
    public val targets: MutableSet<KmpTarget> = mutableSetOf()

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
        targets.addAll(dsl.targets)
    }
}

@DependangerDslMarker
public class AndroidConstraintsDsl {
    public var minSdk: Int? = null
    public var targetSdk: Int? = null
}

@DependangerDslMarker
public class TargetsDsl {
    public val targets: MutableSet<KmpTarget> = mutableSetOf()

    public fun jvm() {
        targets.add(KmpTarget.JVM)
    }

    public fun js() {
        targets.add(KmpTarget.JS)
    }

    public fun android() {
        targets.add(KmpTarget.ANDROID)
    }

    public fun ios() {
        targets.add(KmpTarget.IOS)
    }

    public fun macos() {
        targets.add(KmpTarget.MACOS)
    }

    public fun linux() {
        targets.add(KmpTarget.LINUX)
    }

    public fun mingw() {
        targets.add(KmpTarget.MINGW)
    }

    public fun native() {
        targets.add(KmpTarget.NATIVE)
    }

    public fun wasmJs() {
        targets.add(KmpTarget.WASM_JS)
    }

    public fun wasmWasi() {
        targets.add(KmpTarget.WASM_WASI)
    }
}
