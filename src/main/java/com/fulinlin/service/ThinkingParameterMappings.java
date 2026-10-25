package com.fulinlin.service;

import com.fulinlin.model.LlmProfile;
import com.fulinlin.model.enums.LlmProvider;
import com.fulinlin.model.enums.ThinkingLevel;
import com.fulinlin.model.enums.ThinkingParameterPreset;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Preset request fragments and validation for per-model reasoning parameter mappings.
 *
 * <p>A mapping is a JSON object whose keys are {@link ThinkingLevel} names and whose values are
 * request-body fragments. For example:
 *
 * <pre>
 * {
 *   "LOW": {"reasoning_effort": "low"},
 *   "HIGH": {"reasoning_effort": "high"}
 * }
 * </pre>
 */
public final class ThinkingParameterMappings {

    private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String BUDGET_PLACEHOLDER = "$thinking_budget";
    private static final int MAX_MAPPING_LENGTH = 65536;
    private static final int MAX_JSON_DEPTH = 12;
    private static final Set<String> RESERVED_REQUEST_FIELDS = Collections.unmodifiableSet(new java.util.HashSet<>(
            Arrays.asList(
                    "model", "messages", "system", "input", "instructions", "stream", "temperature",
                    "max_tokens", "max_completion_tokens", "max_output_tokens",
                    "tools", "tool_choice", "functions", "function_call", "response_format", "api_key"
            )
    ));

    private ThinkingParameterMappings() {
    }

    @NotNull
    public static String template(@NotNull ThinkingParameterPreset preset) {
        JsonObject mapping = new JsonObject();
        switch (preset) {
            case OPENAI_EFFORT:
                addEffort(mapping, "reasoning_effort", false);
                break;
            case NESTED_EFFORT:
                addEffort(mapping, "reasoning", true);
                break;
            case THINKING_OBJECT:
                addThinkingToggle(mapping, "thinking");
                break;
            case ENABLE_THINKING:
                mapping.add("DISABLED", parameter("enable_thinking", false));
                mapping.add("ENABLED", parameter("enable_thinking", true));
                break;
            case ANTHROPIC_BUDGET:
                mapping.add("DISABLED", thinkingObject("disabled"));
                for (ThinkingLevel level : gradedLevels()) {
                    JsonObject params = thinkingObject("enabled");
                    params.getAsJsonObject("thinking").addProperty("budget_tokens", BUDGET_PLACEHOLDER);
                    mapping.add(level.name(), params);
                }
                break;
            case ANTHROPIC_ADAPTIVE:
                for (ThinkingLevel level : Arrays.asList(ThinkingLevel.LOW, ThinkingLevel.MEDIUM, ThinkingLevel.HIGH)) {
                    JsonObject params = thinkingObject("adaptive");
                    JsonObject outputConfig = new JsonObject();
                    outputConfig.addProperty("effort", level.getEffortValue());
                    params.add("output_config", outputConfig);
                    mapping.add(level.name(), params);
                }
                break;
            case CUSTOM:
            case AUTO:
            case MODEL_DEFAULT:
            default:
                break;
        }
        return PRETTY_GSON.toJson(mapping);
    }

    @NotNull
    public static List<ThinkingLevel> availableLevels(@NotNull ThinkingParameterPreset preset,
                                                      @Nullable String mappingText) {
        List<ThinkingLevel> levels = new ArrayList<>();
        levels.add(ThinkingLevel.DEFAULT);
        if (preset == ThinkingParameterPreset.AUTO) {
            levels.add(ThinkingLevel.DISABLED);
            levels.add(ThinkingLevel.LOW);
            levels.add(ThinkingLevel.MEDIUM);
            levels.add(ThinkingLevel.HIGH);
            levels.add(ThinkingLevel.MAX);
            return levels;
        }
        if (preset == ThinkingParameterPreset.MODEL_DEFAULT) {
            return levels;
        }
        String effectiveMapping = effectiveMapping(preset, mappingText);
        if (validate(preset, effectiveMapping) != null) {
            return levels;
        }
        JsonObject mapping = parse(effectiveMapping);
        if (mapping == null) {
            return levels;
        }
        for (ThinkingLevel level : ThinkingLevel.values()) {
            if (level == ThinkingLevel.DEFAULT) {
                continue;
            }
            JsonElement value = mapping.get(level.name());
            if (value != null && value.isJsonObject() && !value.getAsJsonObject().entrySet().isEmpty()) {
                levels.add(level);
            }
        }
        return levels;
    }

    @NotNull
    public static List<ThinkingLevel> availableLevels(@NotNull LlmProfile profile) {
        ThinkingParameterPreset preset = ThinkingParameterPreset.fromNullable(profile.getThinkingParameterPreset());
        List<ThinkingLevel> levels = availableLevels(preset, profile.getThinkingParameterMapping());
        if (preset == ThinkingParameterPreset.AUTO || preset == ThinkingParameterPreset.MODEL_DEFAULT) {
            return levels;
        }
        levels.removeIf(level -> level != ThinkingLevel.DEFAULT && resolve(profile, level) == null);
        return levels;
    }

    @Nullable
    static JsonObject resolve(@NotNull LlmProfile profile, @NotNull ThinkingLevel level) {
        ThinkingParameterPreset preset = ThinkingParameterPreset.fromNullable(profile.getThinkingParameterPreset());
        if (preset == ThinkingParameterPreset.AUTO || preset == ThinkingParameterPreset.MODEL_DEFAULT
                || level == ThinkingLevel.DEFAULT) {
            return null;
        }
        String mappingText = effectiveMapping(preset, profile.getThinkingParameterMapping());
        if (validate(preset, mappingText) != null) {
            return null;
        }
        JsonObject mapping = parse(mappingText);
        if (mapping == null) {
            return null;
        }
        JsonElement configured = mapping.get(level.name());
        if (configured == null || !configured.isJsonObject()) {
            return null;
        }
        JsonObject params = configured.getAsJsonObject().deepCopy();
        boolean strictAnthropicBudget = preset == ThinkingParameterPreset.ANTHROPIC_BUDGET
                || LlmProvider.fromNullable(profile.getProvider()) == LlmProvider.ANTHROPIC
                || containsThinkingBudgetPlaceholder(params);
        if (resolveBudget(profile, level, params, strictAnthropicBudget) != null) {
            return null;
        }
        return params;
    }

    @Nullable
    public static String validate(@NotNull LlmProfile profile) {
        ThinkingParameterPreset preset = ThinkingParameterPreset.fromNullable(profile.getThinkingParameterPreset());
        String text = effectiveMapping(preset, profile.getThinkingParameterMapping());
        String error = validate(preset, text);
        if (error != null || preset == ThinkingParameterPreset.AUTO || preset == ThinkingParameterPreset.MODEL_DEFAULT) {
            return error;
        }
        ThinkingLevel level = ThinkingLevel.fromNullable(profile.getThinkingLevel());
        if (level.isSpecified() && hasMapping(preset, text, level) && resolve(profile, level) == null) {
            return "The selected thinking level has an invalid or unavailable token budget.";
        }
        return null;
    }

    @Nullable
    public static String validate(@NotNull ThinkingParameterPreset preset, @Nullable String mappingText) {
        if (preset == ThinkingParameterPreset.AUTO || preset == ThinkingParameterPreset.MODEL_DEFAULT) {
            return null;
        }
        JsonObject mapping = parse(mappingText);
        if (mapping == null) {
            return "Mapping must be a JSON object keyed by thinking level.";
        }
        for (java.util.Map.Entry<String, JsonElement> entry : mapping.entrySet()) {
            ThinkingLevel level;
            try {
                level = ThinkingLevel.valueOf(entry.getKey());
            } catch (IllegalArgumentException ex) {
                return "Unknown thinking level: " + entry.getKey();
            }
            if (level == ThinkingLevel.DEFAULT) {
                return "DEFAULT is implicit and cannot be mapped.";
            }
            JsonElement value = entry.getValue();
            if (value == null || !value.isJsonObject() || value.getAsJsonObject().entrySet().isEmpty()) {
                return entry.getKey() + " must map to a non-empty JSON object.";
            }
            if (containsJsonNull(value)) {
                return entry.getKey() + " cannot contain null parameter values.";
            }
            for (String field : value.getAsJsonObject().keySet()) {
                if (RESERVED_REQUEST_FIELDS.contains(field)) {
                    return "The mapping cannot override the request field: " + field;
                }
            }
            if (hasInvalidPlaceholder(value, "")) {
                return "The $thinking_budget placeholder is only valid at thinking.budget_tokens.";
            }
        }
        return null;
    }

    @NotNull
    private static String effectiveMapping(@NotNull ThinkingParameterPreset preset, @Nullable String mappingText) {
        return mappingText == null ? template(preset) : mappingText;
    }

    @Nullable
    private static String resolveBudget(@NotNull LlmProfile profile, @NotNull ThinkingLevel level,
                                         @NotNull JsonObject params, boolean strictAnthropicBudget) {
        JsonElement thinking = params.get("thinking");
        if (thinking == null || !thinking.isJsonObject()) {
            return null;
        }
        JsonObject thinkingObject = thinking.getAsJsonObject();
        JsonElement budget = thinkingObject.get("budget_tokens");
        if (budget == null) {
            return null;
        }
        if (budget.isJsonPrimitive() && budget.getAsJsonPrimitive().isString()
                && BUDGET_PLACEHOLDER.equals(budget.getAsString())) {
            thinkingObject.addProperty("budget_tokens", LlmThinkingParameters.resolveThinkingBudget(profile, level));
            budget = thinkingObject.get("budget_tokens");
        }
        try {
            if (!budget.isJsonPrimitive() || !budget.getAsJsonPrimitive().isNumber()) {
                throw new ArithmeticException();
            }
            int value = budget.getAsBigDecimal().intValueExact();
            if (strictAnthropicBudget
                    && (value < 1024 || value >= profile.resolveMaxResponseTokens())) {
                return "thinking.budget_tokens must be at least 1024 and less than max response tokens.";
            }
            if (!strictAnthropicBudget && value < 0) {
                return "thinking.budget_tokens cannot be negative.";
            }
        } catch (ArithmeticException | NumberFormatException ex) {
            return "thinking.budget_tokens must be an integer.";
        }
        return null;
    }

    private static boolean hasMapping(@NotNull ThinkingParameterPreset preset, @NotNull String mappingText,
                                      @NotNull ThinkingLevel level) {
        JsonObject mapping = parse(effectiveMapping(preset, mappingText));
        return mapping != null && mapping.has(level.name());
    }

    private static boolean containsThinkingBudgetPlaceholder(@NotNull JsonElement element) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            return BUDGET_PLACEHOLDER.equals(element.getAsString());
        }
        if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                if (containsThinkingBudgetPlaceholder(entry.getValue())) {
                    return true;
                }
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                if (containsThinkingBudgetPlaceholder(child)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean containsJsonNull(@NotNull JsonElement element) {
        if (element.isJsonNull()) {
            return true;
        }
        if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                if (containsJsonNull(entry.getValue())) {
                    return true;
                }
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                if (containsJsonNull(child)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasInvalidPlaceholder(@NotNull JsonElement element, @NotNull String path) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            String value = element.getAsString();
            return value.startsWith("$")
                    && !(BUDGET_PLACEHOLDER.equals(value) && "thinking.budget_tokens".equals(path));
        }
        if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                String childPath = path.isEmpty() ? entry.getKey() : path + "." + entry.getKey();
                if (hasInvalidPlaceholder(entry.getValue(), childPath)) {
                    return true;
                }
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                if (hasInvalidPlaceholder(child, path + "[]")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void addEffort(@NotNull JsonObject mapping,
                                  @NotNull String field,
                                  boolean nested) {
        for (ThinkingLevel level : Arrays.asList(ThinkingLevel.LOW, ThinkingLevel.MEDIUM, ThinkingLevel.HIGH)) {
            JsonObject params = new JsonObject();
            if (nested) {
                JsonObject reasoning = new JsonObject();
                reasoning.addProperty("effort", level.getEffortValue());
                params.add(field, reasoning);
            } else {
                params.addProperty(field, level.getEffortValue());
            }
            mapping.add(level.name(), params);
        }
    }

    private static void addThinkingToggle(@NotNull JsonObject mapping, @NotNull String field) {
        JsonObject disabled = new JsonObject();
        disabled.addProperty("type", "disabled");
        JsonObject enabled = new JsonObject();
        enabled.addProperty("type", "enabled");
        mapping.add("DISABLED", objectParameter(field, disabled));
        mapping.add("ENABLED", objectParameter(field, enabled));
    }

    @NotNull
    private static List<ThinkingLevel> gradedLevels() {
        return Arrays.asList(ThinkingLevel.LOW, ThinkingLevel.MEDIUM, ThinkingLevel.HIGH, ThinkingLevel.MAX);
    }

    @NotNull
    private static JsonObject thinkingObject(@NotNull String type) {
        JsonObject thinking = new JsonObject();
        thinking.addProperty("type", type);
        return objectParameter("thinking", thinking);
    }

    @NotNull
    private static JsonObject parameter(@NotNull String name, boolean value) {
        JsonObject params = new JsonObject();
        params.addProperty(name, value);
        return params;
    }

    @NotNull
    private static JsonObject objectParameter(@NotNull String name, @NotNull JsonObject value) {
        JsonObject params = new JsonObject();
        params.add(name, value);
        return params;
    }

    @Nullable
    private static JsonObject parse(@Nullable String text) {
        if (text == null || text.trim().isEmpty() || text.length() > MAX_MAPPING_LENGTH) {
            return null;
        }
        try (JsonReader reader = new JsonReader(new StringReader(text))) {
            reader.setLenient(false);
            JsonElement parsed = readJsonValue(reader, 0);
            return reader.peek() == JsonToken.END_DOCUMENT && parsed.isJsonObject()
                    ? parsed.getAsJsonObject() : null;
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }

    @NotNull
    private static JsonElement readJsonValue(@NotNull JsonReader reader, int depth) throws IOException {
        if (depth > MAX_JSON_DEPTH) {
            throw new IOException("Mapping nesting is too deep.");
        }
        switch (reader.peek()) {
            case BEGIN_OBJECT:
                JsonObject object = new JsonObject();
                reader.beginObject();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if (object.has(name)) {
                        throw new IOException("Duplicate mapping key: " + name);
                    }
                    object.add(name, readJsonValue(reader, depth + 1));
                }
                reader.endObject();
                return object;
            case BEGIN_ARRAY:
                JsonArray array = new JsonArray();
                reader.beginArray();
                while (reader.hasNext()) {
                    array.add(readJsonValue(reader, depth + 1));
                }
                reader.endArray();
                return array;
            case STRING:
                return new JsonPrimitive(reader.nextString());
            case NUMBER:
                return new JsonPrimitive(new BigDecimal(reader.nextString()));
            case BOOLEAN:
                return new JsonPrimitive(reader.nextBoolean());
            case NULL:
                reader.nextNull();
                return JsonNull.INSTANCE;
            default:
                throw new IOException("Expected a JSON value.");
        }
    }
}
