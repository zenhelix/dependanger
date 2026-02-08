package io.github.zenhelix.dependanger.effective.spi

import io.github.zenhelix.dependanger.core.model.CompatibilityRule
import io.github.zenhelix.dependanger.effective.model.CompatibilityIssue
import io.github.zenhelix.dependanger.effective.model.EffectiveLibrary
import io.github.zenhelix.dependanger.effective.pipeline.ProcessingContext

/**
 * SPI для обработки пользовательских правил совместимости.
 * Реализации обнаруживаются через ServiceLoader.
 */
public interface CustomRuleHandler {
    /** Уникальный идентификатор типа правила. */
    public val ruleType: String

    /**
     * Выполнить проверку правила и вернуть найденные проблемы.
     */
    public fun evaluate(
        rule: CompatibilityRule.CustomRule,
        libraries: Map<String, EffectiveLibrary>,
        context: ProcessingContext,
    ): List<CompatibilityIssue>
}
