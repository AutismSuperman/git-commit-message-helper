package com.fulinlin.model.enums;

import com.fulinlin.localization.PluginBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum ThinkingParameterPreset {
    AUTO("setting.llm.thinking.preset.auto"),
    MODEL_DEFAULT("setting.llm.thinking.preset.default"),
    OPENAI_EFFORT("setting.llm.thinking.preset.openai.effort"),
    NESTED_EFFORT("setting.llm.thinking.preset.nested.effort"),
    THINKING_OBJECT("setting.llm.thinking.preset.thinking.object"),
    ENABLE_THINKING("setting.llm.thinking.preset.enable.thinking"),
    ANTHROPIC_BUDGET("setting.llm.thinking.preset.anthropic.budget"),
    ANTHROPIC_ADAPTIVE("setting.llm.thinking.preset.anthropic.adaptive"),
    CUSTOM("setting.llm.thinking.preset.custom");

    private final String displayKey;

    ThinkingParameterPreset(String displayKey) {
        this.displayKey = displayKey;
    }

    @NotNull
    public String getDisplayName() {
        return PluginBundle.get(displayKey);
    }

    @NotNull
    public static ThinkingParameterPreset fromNullable(@Nullable ThinkingParameterPreset preset) {
        return preset == null ? AUTO : preset;
    }
}
