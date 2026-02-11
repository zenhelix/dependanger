package io.github.zenhelix.dependanger.core.dsl

import io.github.zenhelix.dependanger.core.model.CompatibilityRule
import io.github.zenhelix.dependanger.core.model.Severity
import io.github.zenhelix.dependanger.core.model.VersionConstraintType

@DependangerDslMarker
public class CompatibilityDsl {
    private val _rules: MutableList<CompatibilityRule> = mutableListOf()
    public val rules: List<CompatibilityRule> get() = _rules.toList()

    public fun jdkRequirement(name: String, block: JdkRequirementDsl.() -> Unit) {
        val dsl = JdkRequirementDsl().apply(block)
        _rules.add(
            CompatibilityRule.JdkRequirement(
                name = name,
                matches = dsl.matches,
                minJdk = dsl.minJdk,
                maxJdk = dsl.maxJdk,
                severity = dsl.severity,
                message = dsl.message,
            )
        )
    }

    public fun mutualExclusion(name: String, block: MutualExclusionDsl.() -> Unit) {
        val dsl = MutualExclusionDsl().apply(block)
        _rules.add(
            CompatibilityRule.MutualExclusion(
                name = name,
                libraries = dsl.libraries.toList(),
                severity = dsl.severity,
                message = dsl.message,
            )
        )
    }

    public fun versionConstraint(name: String, block: VersionConstraintDsl.() -> Unit) {
        val dsl = VersionConstraintDsl().apply(block)
        _rules.add(
            CompatibilityRule.VersionConstraint(
                name = name,
                libraries = dsl.libraries.toList(),
                constraint = dsl.constraint,
                severity = dsl.severity,
                message = dsl.message,
            )
        )
    }

    public fun customRule(name: String, block: CustomRuleDsl.() -> Unit) {
        val dsl = CustomRuleDsl().apply(block)
        _rules.add(
            CompatibilityRule.CustomRule(
                name = name,
                ruleId = dsl.ruleId,
                parameters = dsl.parameters.toMap(),
                severity = dsl.severity,
                message = dsl.message,
            )
        )
    }
}

@DependangerDslMarker
public class JdkRequirementDsl {
    public var matches: String = ""
    public var minJdk: Int? = null
    public var maxJdk: Int? = null
    public var severity: Severity = Severity.ERROR
    public var message: String? = null
}

@DependangerDslMarker
public class MutualExclusionDsl {
    private val _libraries: MutableList<String> = mutableListOf()
    public val libraries: List<String> get() = _libraries.toList()
    public var severity: Severity = Severity.WARNING
    public var message: String? = null

    public fun libraries(vararg aliases: String) {
        _libraries.addAll(aliases)
    }
}

@DependangerDslMarker
public class VersionConstraintDsl {
    private val _libraries: MutableList<String> = mutableListOf()
    public val libraries: List<String> get() = _libraries.toList()
    public var constraint: VersionConstraintType = VersionConstraintType.SAME_VERSION
    public var severity: Severity = Severity.ERROR
    public var message: String? = null

    public fun libraries(vararg aliases: String) {
        _libraries.addAll(aliases)
    }
}

@DependangerDslMarker
public class CustomRuleDsl {
    public var ruleId: String = ""
    private val _parameters: MutableMap<String, String> = mutableMapOf()
    public val parameters: Map<String, String> get() = _parameters.toMap()
    public var severity: Severity = Severity.WARNING
    public var message: String? = null

    public fun parameter(key: String, value: String) {
        _parameters[key] = value
    }
}
