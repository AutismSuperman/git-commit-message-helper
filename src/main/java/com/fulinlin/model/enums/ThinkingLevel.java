package com.fulinlin.model.enums;

import com.fulinlin.localization.PluginBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * How much internal reasoning a profile asks the model to perform. The user picks a level and the
 * protocol-specific parameter mapping in the service layer translates it into the fields each API
 * format understands.
 */
public enum ThinkingLevel {

    /**
     * Send no thinking parameter at all and let the provider apply its own default.
     */
    DEFAULT("setting.llm.thinking.level.default", null),

    /**
     * Ask the model to answer without a reasoning phase where the protocol supports it.
     */
    DISABLED("setting.llm.thinking.level.disabled", "disabled"),

    LOW("setting.llm.thinking.level.low", "low"),
    MEDIUM("setting.llm.thinking.level.medium", "medium"),
    HIGH("setting.llm.thinking.level.high", "high"),
    MAX("setting.llm.thinking.level.max", "max");

    private final String displayKey;
    private final String effortValue;

    ThinkingLevel(String displayKey, String effortValue) {
        this.displayKey = displayKey;
        this.effortValue = effortValue;
    }

    /**
     * Effort identifier understood by the OpenAI-style {@code reasoning_effort} and
     * {@code output_config.effort} knobs; null for levels that have no effort equivalent.
     */
    @Nullable
    public String getEffortValue() {
        return effortValue;
    }

    /**
     * True when the user picked a level other than {@link #DEFAULT}, meaning thinking parameters
     * should be sent. This includes {@link #DISABLED}, which asks for reasoning to be switched
     * off explicitly.
     */
    public boolean isSpecified() {
        return this != DEFAULT;
    }

    public boolean isDisabled() {
        return this == DISABLED;
    }

    /**
     * True when the profile actually asks the model to spend effort reasoning, i.e. a graded
     * level was chosen. Used for hints about slower first output.
     */
    public boolean isReasoningRequested() {
        return this != DEFAULT && this != DISABLED;
    }

    @NotNull
    public String getDisplayName() {
        return PluginBundle.get(displayKey);
    }

    @NotNull
    public static ThinkingLevel defaultLevel() {
        return DEFAULT;
    }

    @NotNull
    public static ThinkingLevel fromNullable(@Nullable ThinkingLevel level) {
        return level == null ? defaultLevel() : level;
    }
}
