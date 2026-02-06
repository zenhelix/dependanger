package io.github.zenhelix.dependanger.core.dsl

@DependangerDslMarker
public class ProcessingDsl {
    public var preset: String = "DEFAULT"
    public var importBoms: Boolean = true
    public var checkUpdates: Boolean = false
    public var disabledProcessors: List<String> = emptyList()

    public var compatibilityEnabled: Boolean = false
    public var compatibilityTargetJdk: Int? = null
    public var compatibilityFailOnErrors: Boolean = true

    public fun analyzeCompatibility(block: ProcessingCompatibilityDsl.() -> Unit) {
        val dsl = ProcessingCompatibilityDsl().apply(block)
        compatibilityEnabled = dsl.enabled
        compatibilityTargetJdk = dsl.targetJdk
        compatibilityFailOnErrors = dsl.failOnErrors
    }

    public fun withBomImport() {
        importBoms = true
    }

    public fun withoutBomImport() {
        importBoms = false
    }

    public fun withUpdateCheck() {
        checkUpdates = true
    }

    public fun withoutUpdateCheck() {
        checkUpdates = false
    }

    public fun withSecurityCheck(): Unit = TODO()
    public fun withoutSecurityCheck(): Unit = TODO()
    public fun withLicenseCheck(): Unit = TODO()
    public fun withoutLicenseCheck(): Unit = TODO()
    public fun withCompatibilityAnalysis(): Unit = TODO()
    public fun withoutCompatibilityAnalysis(): Unit = TODO()
    public fun withTransitiveResolution(): Unit = TODO()
    public fun withoutTransitiveResolution(): Unit = TODO()
}

@DependangerDslMarker
public class ProcessingCompatibilityDsl {
    public var enabled: Boolean = true
    public var targetJdk: Int? = null
    public var failOnErrors: Boolean = true
}
