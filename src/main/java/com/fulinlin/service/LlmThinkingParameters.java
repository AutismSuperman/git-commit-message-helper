package com.fulinlin.service;

import com.fulinlin.model.LlmProfile;
import com.fulinlin.model.enums.LlmProvider;
import com.fulinlin.model.enums.ThinkingLevel;
import com.fulinlin.model.enums.ThinkingParameterPreset;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Translates a profile's thinking level into the request fields each API protocol understands.
 *
 * <p>The mapping mirrors the rule tables ZCode ships for its built-in providers: Anthropic
 * Messages profiles use the {@code thinking} object (plus {@code output_config.effort} where the
 * gateway supports it), OpenAI Chat Completions profiles use the parameter family the endpoint is
 * known to accept ({@code enable_thinking}, {@code thinking}, {@code reasoning_effort}, or
 * {@code reasoning.effort}), and OpenAI Responses profiles use {@code reasoning.effort}.
 *
 * <p>Unknown gateways get the standard OpenAI parameter for their protocol, so a level always
 * produces a defensible request; when a provider rejects the extra fields the clients retry
 * without them and remember that for the model.
 */
final class LlmThinkingParameters {

    private static final int MIN_THINKING_BUDGET = 1024;
    private static final int THINKING_OUTPUT_RESERVE = 256;

    /**
     * Gateways and model families that expose a graded effort knob through the Anthropic Messages
     * protocol. Older Anthropic-compatible endpoints and official Anthropic only accept the
     * classic enabled/disabled thinking switch.
     */
    private static final String[] ANTHROPIC_EFFORT_HINTS = {
            "glm-5.2", "glm-5.3", "qwen3.8", "kimi-k3", "deepseek-v4", "deepseek-flash",
            "x-preview", "ox-alpha"
    };

    private static final String[] ANTHROPIC_ADAPTIVE_HINTS = {
            "claude-opus-4-8", "claude-opus-5", "claude-sonnet-5", "fable-5", "mythos-5"
    };

    private static final String[] QWEN_HINTS = {"qwen", "dashscope", "aliyuncs", "alibabacloud"};

    private static final String[] THINKING_OBJECT_HINTS = {
            "mimo", "xiaomimimo", "token-plan", "zhipu", "bigmodel", "glm",
            "moonshot", "kimi", "doubao", "volces", "deepseek"
    };

    private static final String[] REASONING_OBJECT_HINTS = {"openrouter", "minimax"};

    /**
     * OpenAI Responses models whose only reasoning efforts are {@code none} and {@code high};
     * graded levels collapse to high instead of failing the request.
     */
    private static final String[] RESPONSES_HIGH_ONLY_HINTS = {"kimi-k2.7-code", "qwen3.6", "qwen3.7"};

    private LlmThinkingParameters() {
    }

    static boolean isRequested(@NotNull LlmProfile profile) {
        ThinkingLevel level = ThinkingLevel.fromNullable(profile.getThinkingLevel());
        if (!level.isSpecified()) {
            return false;
        }
        ThinkingParameterPreset preset = ThinkingParameterPreset.fromNullable(profile.getThinkingParameterPreset());
        if (preset == ThinkingParameterPreset.AUTO) {
            return level != ThinkingLevel.ENABLED;
        }
        JsonObject mapping = ThinkingParameterMappings.resolve(profile, level);
        return mapping != null && !mapping.entrySet().isEmpty();
    }

    static void apply(@NotNull JsonObject requestBody, @NotNull LlmProfile profile) {
        ThinkingLevel level = ThinkingLevel.fromNullable(profile.getThinkingLevel());
        if (!level.isSpecified()) {
            return;
        }
        ThinkingParameterPreset preset = ThinkingParameterPreset.fromNullable(profile.getThinkingParameterPreset());
        if (preset != ThinkingParameterPreset.AUTO) {
            JsonObject parameters = ThinkingParameterMappings.resolve(profile, level);
            if (parameters != null) {
                for (java.util.Map.Entry<String, com.google.gson.JsonElement> entry : parameters.entrySet()) {
                    requestBody.add(entry.getKey(), entry.getValue().deepCopy());
                }
            }
            return;
        }
        if (level == ThinkingLevel.ENABLED) {
            return;
        }
        switch (LlmProvider.fromNullable(profile.getProvider())) {
            case ANTHROPIC:
                applyAnthropicMessages(requestBody, profile, level);
                break;
            case OPENAI_RESPONSES:
                applyOpenAiResponses(requestBody, profile, level);
                break;
            default:
                applyChatCompletions(requestBody, profile, level);
                break;
        }
    }

    /**
     * Anthropic Messages: the classic {@code thinking} object is always accepted; graded effort
     * is added for gateways known to forward {@code output_config.effort}. Newer Claude families
     * require adaptive thinking; older models use an enabled object with a token budget.
     */
    private static void applyAnthropicMessages(@NotNull JsonObject requestBody,
                                               @NotNull LlmProfile profile,
                                               @NotNull ThinkingLevel level) {
        if (usesAnthropicAdaptiveThinking(profile)) {
            if (!level.isDisabled()) {
                requestBody.add("thinking", thinkingObject("adaptive"));
                requestBody.add("output_config", effortObject(level.getEffortValue()));
            }
            // Adaptive-thinking models do not expose a supported "off" value; omitting the
            // object avoids a 400 and lets the provider apply its documented default.
            return;
        }
        if (level.isDisabled()) {
            requestBody.add("thinking", thinkingObject("disabled"));
            return;
        }
        if (usesAnthropicEffort(profile)) {
            requestBody.add("thinking", thinkingObject("enabled"));
            requestBody.add("output_config", effortObject(level.getEffortValue()));
            return;
        }
        int budget = resolveThinkingBudget(profile, level);
        if (budget < MIN_THINKING_BUDGET) {
            // The response budget cannot fit a valid thinking budget (Anthropic requires at least
            // 1024 and strictly less than max_tokens). Keeping the request valid wins over asking
            // for thinking the API would reject.
            return;
        }
        JsonObject thinking = thinkingObject("enabled");
        thinking.addProperty("budget_tokens", budget);
        requestBody.add("thinking", thinking);
    }

    /**
     * OpenAI Chat Completions: parameter family depends on the endpoint, following the same
     * vendor grouping the plugin already used for reasoning compatibility.
     */
    private static void applyChatCompletions(@NotNull JsonObject requestBody,
                                             @NotNull LlmProfile profile,
                                             @NotNull ThinkingLevel level) {
        if (containsProfileText(profile, QWEN_HINTS)) {
            requestBody.addProperty("enable_thinking", !level.isDisabled());
            return;
        }
        if (containsProfileText(profile, REASONING_OBJECT_HINTS)) {
            requestBody.add("reasoning", effortObject(level.isDisabled() ? "none" : level.getEffortValue()));
            return;
        }
        if (containsProfileText(profile, THINKING_OBJECT_HINTS)) {
            requestBody.add("thinking", thinkingObject(level.isDisabled() ? "disabled" : "enabled"));
            return;
        }
        // OpenAI reasoning families, Gemini, Grok, and plain compatible gateways: the standard
        // effort field. Older reasoning models do not accept "none", so keep their lowest valid
        // effort; GPT-5.1+ explicitly support "none".
        String effort = level.isDisabled()
                ? (supportsNoneEffort(profile) ? "none" : "low")
                : (isOpenAiReasoningModel(profile) ? resolveOpenAiEffort(profile, level) : level.getEffortValue());
        requestBody.addProperty("reasoning_effort", effort);
    }

    /**
     * OpenAI Responses: reasoning effort is the only knob. Only models known to support
     * {@code none} receive it for the disabled level; older models omit the field instead of
     * sending a value they reject.
     */
    private static void applyOpenAiResponses(@NotNull JsonObject requestBody,
                                             @NotNull LlmProfile profile,
                                             @NotNull ThinkingLevel level) {
        if (level.isDisabled()) {
            if (supportsNoneEffort(profile)) {
                requestBody.add("reasoning", effortObject("none"));
            }
            return;
        }
        String effort = containsProfileText(profile, RESPONSES_HIGH_ONLY_HINTS)
                ? "high" : resolveOpenAiEffort(profile, level);
        requestBody.add("reasoning", effortObject(effort));
    }

    @NotNull
    private static String resolveOpenAiEffort(@NotNull LlmProfile profile, @NotNull ThinkingLevel level) {
        if (level == ThinkingLevel.MAX) {
            return gpt5MinorVersion(profile) >= 2 ? "xhigh" : "high";
        }
        return level.getEffortValue();
    }

    private static boolean usesAnthropicEffort(@NotNull LlmProfile profile) {
        return containsProfileText(profile, ANTHROPIC_EFFORT_HINTS)
                && !containsProfileText(profile, "api.anthropic.com");
    }

    private static boolean usesAnthropicAdaptiveThinking(@NotNull LlmProfile profile) {
        return containsProfileText(profile, ANTHROPIC_ADAPTIVE_HINTS);
    }

    private static boolean supportsNoneEffort(@NotNull LlmProfile profile) {
        return gpt5MinorVersion(profile) >= 1;
    }

    private static int gpt5MinorVersion(@NotNull LlmProfile profile) {
        String model = normalize(profile.getModel());
        String prefix = "gpt-5.";
        if (!model.startsWith(prefix)) {
            return -1;
        }
        int start = prefix.length();
        int end = start;
        while (end < model.length() && Character.isDigit(model.charAt(end))) {
            end++;
        }
        if (end == start) {
            return -1;
        }
        try {
            return Integer.parseInt(model.substring(start, end));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    /**
     * Thinking budget for the Anthropic Messages protocol when the gateway does not support
     * graded effort. Higher levels spend a larger share of the configured response budget while
     * leaving room for the answer itself. Returns a value below {@link #MIN_THINKING_BUDGET} when
     * the configured response budget is too small to hold any valid budget.
     */
    static int resolveThinkingBudget(@NotNull LlmProfile profile, @NotNull ThinkingLevel level) {
        int maxResponseTokens = profile.resolveMaxResponseTokens();
        int share;
        switch (level) {
            case ENABLED:
            case LOW:
                share = 25;
                break;
            case MEDIUM:
                share = 50;
                break;
            case HIGH:
                share = 75;
                break;
            default:
                share = 90;
                break;
        }
        // Anthropic requires budget_tokens to be strictly less than max_tokens.
        int upperBound = maxResponseTokens - THINKING_OUTPUT_RESERVE;
        int budget = Math.max(MIN_THINKING_BUDGET, maxResponseTokens * share / 100);
        return Math.min(budget, upperBound);
    }

    /**
     * Models that reject {@code max_tokens} in favor of {@code max_completion_tokens}.
     * This is a model capability and must hold even when the thinking parameters are dropped
     * on retry.
     */
    static boolean needsCompletionTokenLimit(@NotNull LlmProfile profile) {
        return isOpenAiReasoningModel(profile) || containsProfileText(profile, "mimo", "xiaomimimo", "token-plan");
    }

    static boolean isOpenAiReasoningModel(@NotNull LlmProfile profile) {
        String model = normalize(profile.getModel());
        return model.startsWith("o1")
                || model.startsWith("o3")
                || model.startsWith("o4")
                || model.startsWith("o5")
                || model.startsWith("gpt-5");
    }

    private static boolean containsProfileText(@NotNull LlmProfile profile, @NotNull String... needles) {
        String text = normalize(profile.getBaseUrl()) + " " + normalize(profile.getModel());
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    private static JsonObject thinkingObject(@NotNull String type) {
        JsonObject thinking = new JsonObject();
        thinking.addProperty("type", type);
        return thinking;
    }

    @NotNull
    private static JsonObject effortObject(@NotNull String effort) {
        JsonObject object = new JsonObject();
        object.addProperty("effort", effort);
        return object;
    }

    @NotNull
    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
