package com.fulinlin.service;

import com.fulinlin.model.LlmProfile;
import com.fulinlin.model.LlmSettings;
import com.fulinlin.model.enums.LlmProvider;
import com.fulinlin.model.enums.ThinkingLevel;
import com.fulinlin.model.enums.ThinkingParameterPreset;
import com.fulinlin.storage.GitCommitMessageHelperSettings;
import com.google.gson.JsonObject;
import org.junit.After;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class ThinkingParameterMappingsTest {

    @After
    public void clearCache() {
        LlmCapabilityCache.clearForTests();
    }

    @Test
    public void editedBudgetTemplateStillResolvesItsPlaceholder() {
        LlmProfile profile = profile(ThinkingParameterPreset.CUSTOM, ThinkingLevel.HIGH,
                ThinkingParameterMappings.template(ThinkingParameterPreset.ANTHROPIC_BUDGET));
        profile.setProvider(LlmProvider.ANTHROPIC);
        JsonObject parameters = ThinkingParameterMappings.resolve(profile, ThinkingLevel.HIGH);

        assertNotNull(parameters);
        assertTrue(parameters.getAsJsonObject("thinking").get("budget_tokens").getAsJsonPrimitive().isNumber());
        assertEquals(3072, parameters.getAsJsonObject("thinking").get("budget_tokens").getAsInt());
    }

    @Test
    public void budgetTemplateNeverSendsAnInvalidBudgetWhenResponseLimitIsTooSmall() {
        LlmProfile profile = profile(ThinkingParameterPreset.ANTHROPIC_BUDGET, ThinkingLevel.HIGH,
                ThinkingParameterMappings.template(ThinkingParameterPreset.ANTHROPIC_BUDGET));
        profile.setProvider(LlmProvider.ANTHROPIC);
        profile.setMaxResponseTokens(256);
        profile.setThinkingLevel(ThinkingLevel.DEFAULT);
        JsonObject request = AnthropicLlmProviderClient.createRequestBody(
                profile, new LlmSettings(), "system", "user", false);

        assertFalse(request.has("thinking"));
        assertFalse(LlmThinkingParameters.isRequested(profile));
        assertNull("an unavailable optional level does not prevent saving the profile",
                ThinkingParameterMappings.validate(profile));
        assertEquals(
                Arrays.asList(ThinkingLevel.DEFAULT, ThinkingLevel.DISABLED),
                ThinkingParameterMappings.availableLevels(profile)
        );

        profile.setThinkingLevel(ThinkingLevel.HIGH);
        assertNotNull("the selected level must fit the configured response budget",
                ThinkingParameterMappings.validate(profile));
    }

    @Test
    public void correctingAMappingDoesNotReuseItsRejectedParameterCache() {
        LlmProfile profile = profile(ThinkingParameterPreset.CUSTOM, ThinkingLevel.HIGH,
                "{\"HIGH\":{\"reasoning_effort\":\"max\"}}");
        LlmCapabilityCache.markThinkingParametersUnsupported(profile);
        assertTrue(LlmCapabilityCache.shouldSkipThinkingParameters(profile));

        profile.setThinkingParameterMapping("{\"HIGH\":{\"reasoning_effort\":\"high\"}}");
        assertFalse(LlmCapabilityCache.shouldSkipThinkingParameters(profile));
    }

    @Test
    public void rejectedEffortDoesNotDisableAnotherEffortForTheSameModel() {
        LlmProfile profile = profile(ThinkingParameterPreset.OPENAI_EFFORT, ThinkingLevel.HIGH,
                ThinkingParameterMappings.template(ThinkingParameterPreset.OPENAI_EFFORT));
        LlmCapabilityCache.markThinkingParametersUnsupported(profile);
        profile.setThinkingLevel(ThinkingLevel.LOW);

        assertFalse(LlmCapabilityCache.shouldSkipThinkingParameters(profile));
    }

    @Test
    public void invalidJsonCannotBeSentAsReasoningParameters() {
        for (String invalid : Arrays.asList(
                "{HIGH:{reasoning_effort:'high'}}",
                "{\"HIGH\":{\"reasoning_effort\":\"high\"},}",
                "{\"HIGH\":{\"reasoning_effort\":\"high\"}} trailing",
                "{\"HIGH\":{\"reasoning_effort\":\"low\"},\"HIGH\":{\"reasoning_effort\":\"high\"}}",
                "{\"HIGH\":{\"reasoning_effort\":null}}"
        )) {
            assertNotNull(invalid, ThinkingParameterMappings.validate(ThinkingParameterPreset.CUSTOM, invalid));
            LlmProfile profile = profile(ThinkingParameterPreset.CUSTOM, ThinkingLevel.HIGH, invalid);
            assertFalse(invalid, LlmThinkingParameters.isRequested(profile));
        }
    }

    @Test
    public void invalidMappingCannotOverrideTheActualModelOrInput() {
        LlmProfile profile = profile(ThinkingParameterPreset.CUSTOM, ThinkingLevel.HIGH,
                "{\"HIGH\":{\"model\":\"other-model\",\"input\":\"other-input\"}}");
        profile.setProvider(LlmProvider.OPENAI_RESPONSES);
        JsonObject request = OpenAiResponsesLlmProviderClient.createRequestBody(
                profile, new LlmSettings(), "system", "user", false);

        assertEquals("custom-model", request.get("model").getAsString());
        assertEquals("user", request.get("input").getAsString());
        assertFalse(LlmThinkingParameters.isRequested(profile));
    }

    @Test
    public void customOffAndHighestEffortAreSentVerbatimForEachOpenAiProtocol() {
        LlmProfile profile = profile(ThinkingParameterPreset.CUSTOM, ThinkingLevel.DISABLED,
                "{\"DISABLED\":{\"reasoning_effort\":\"none\"},\"MAX\":{\"reasoning_effort\":\"xhigh\"}}");
        JsonObject off = OpenAiCompatibleLlmProviderClient.createRequestBody(
                profile, new LlmSettings(), "system", "user", false);
        assertEquals("none", off.get("reasoning_effort").getAsString());
        profile.setThinkingLevel(ThinkingLevel.MAX);
        JsonObject max = OpenAiCompatibleLlmProviderClient.createRequestBody(
                profile, new LlmSettings(), "system", "user", false);
        assertEquals("xhigh", max.get("reasoning_effort").getAsString());

        profile.setProvider(LlmProvider.OPENAI_RESPONSES);
        profile.setThinkingParameterMapping(
                "{\"DISABLED\":{\"reasoning\":{\"effort\":\"none\"}},\"MAX\":{\"reasoning\":{\"effort\":\"xhigh\"}}}");
        JsonObject response = OpenAiResponsesLlmProviderClient.createRequestBody(
                profile, new LlmSettings(), "system", "user", true);
        assertEquals("xhigh", response.getAsJsonObject("reasoning").get("effort").getAsString());
        assertTrue(response.get("stream").getAsBoolean());
    }

    @Test
    public void unmappedOffDoesNotSilentlyBecomeLowEffort() {
        LlmProfile profile = profile(ThinkingParameterPreset.OPENAI_EFFORT, ThinkingLevel.DISABLED,
                ThinkingParameterMappings.template(ThinkingParameterPreset.OPENAI_EFFORT));
        JsonObject request = OpenAiCompatibleLlmProviderClient.createRequestBody(
                profile, new LlmSettings(), "system", "user", false);

        assertFalse(request.has("reasoning_effort"));
        assertFalse(ThinkingParameterMappings.availableLevels(
                ThinkingParameterPreset.OPENAI_EFFORT, profile.getThinkingParameterMapping()
        ).contains(ThinkingLevel.DISABLED));
    }

    @Test
    public void newProfilesNeverInferReasoningParametersFromTheModelName() {
        LlmProfile profile = GitCommitMessageHelperSettings.createDefaultLlmProfile();
        profile.setModel("qwen-gpt-5-kimi");
        profile.setThinkingLevel(ThinkingLevel.HIGH);
        JsonObject request = OpenAiCompatibleLlmProviderClient.createRequestBody(
                profile, new LlmSettings(), "system", "user", false);

        assertEquals(ThinkingParameterPreset.MODEL_DEFAULT, profile.getThinkingParameterPreset());
        assertFalse(LlmThinkingParameters.isRequested(profile));
        assertFalse(request.has("enable_thinking"));
        assertFalse(request.has("reasoning_effort"));
    }

    @Test
    public void profilePersistenceRoundTripRetainsMappingAndSelection() {
        LlmProfile profile = profile(ThinkingParameterPreset.CUSTOM, ThinkingLevel.MAX,
                "{\"MAX\":{\"reasoning_effort\":\"xhigh\"}}");
        org.jdom.Element xml = com.intellij.util.xmlb.XmlSerializer.serialize(profile);
        LlmProfile restored = com.intellij.util.xmlb.XmlSerializer.deserialize(xml, LlmProfile.class);

        assertEquals(profile, restored);
        assertEquals(ThinkingParameterPreset.CUSTOM, restored.getThinkingParameterPreset());
        assertEquals(profile.getThinkingParameterMapping(), restored.getThinkingParameterMapping());
        assertEquals("xhigh", ThinkingParameterMappings.resolve(restored, ThinkingLevel.MAX)
                .get("reasoning_effort").getAsString());
    }

    @Test
    public void defaultPresetPersistsDespiteLegacyConstructorDefault() {
        LlmProfile profile = GitCommitMessageHelperSettings.createDefaultLlmProfile();
        org.jdom.Element xml = com.intellij.util.xmlb.XmlSerializer.serialize(profile);
        LlmProfile restored = com.intellij.util.xmlb.XmlSerializer.deserialize(xml, LlmProfile.class);

        assertEquals(ThinkingParameterPreset.MODEL_DEFAULT, restored.getThinkingParameterPreset());
    }

    @Test
    public void enabledOnlyMappingSendsAdaptiveThinkingWithoutOfferingOff() {
        for (LlmProvider provider : Arrays.asList(LlmProvider.OPENAI_COMPATIBLE, LlmProvider.ANTHROPIC)) {
            LlmProfile profile = profile(ThinkingParameterPreset.CUSTOM, ThinkingLevel.ENABLED,
                    "{\"ENABLED\":{\"thinking\":{\"type\":\"adaptive\"}}}");
            profile.setProvider(provider);
            profile.setModel("MiniMax-M3.1-Flash");

            assertNull(ThinkingParameterMappings.validate(profile));
            assertEquals(Arrays.asList(ThinkingLevel.DEFAULT, ThinkingLevel.ENABLED),
                    ThinkingParameterMappings.availableLevels(profile));
            JsonObject request = provider == LlmProvider.ANTHROPIC
                    ? AnthropicLlmProviderClient.createRequestBody(profile, new LlmSettings(), "system", "user", false)
                    : OpenAiCompatibleLlmProviderClient.createRequestBody(profile, new LlmSettings(), "system", "user", false);
            assertEquals("adaptive", request.getAsJsonObject("thinking").get("type").getAsString());
            assertFalse(request.has("reasoning"));
            assertFalse(request.has("reasoning_effort"));
        }
    }

    @Test
    public void minimaxAlwaysOnProfilesCanUseModelDefaultWithoutAnOffParameter() {
        for (LlmProvider provider : Arrays.asList(LlmProvider.OPENAI_COMPATIBLE, LlmProvider.ANTHROPIC)) {
            LlmProfile profile = GitCommitMessageHelperSettings.createDefaultLlmProfile();
            profile.setProvider(provider);
            profile.setModel("MiniMax-M3.1-Flash");
            JsonObject request = provider == LlmProvider.ANTHROPIC
                    ? AnthropicLlmProviderClient.createRequestBody(profile, new LlmSettings(), "system", "user", false)
                    : OpenAiCompatibleLlmProviderClient.createRequestBody(profile, new LlmSettings(), "system", "user", false);

            assertEquals(Arrays.asList(ThinkingLevel.DEFAULT), ThinkingParameterMappings.availableLevels(profile));
            assertFalse(request.has("thinking"));
            assertFalse(request.has("reasoning"));
            assertFalse(request.has("reasoning_effort"));
        }
    }

    @Test
    public void minimaxDepthMappingsUseTheFieldForTheSelectedProtocol() {
        for (LlmProvider provider : Arrays.asList(LlmProvider.OPENAI_COMPATIBLE, LlmProvider.ANTHROPIC)) {
            String mapping = provider == LlmProvider.ANTHROPIC
                    ? "{\"LOW\":{\"output_config\":{\"effort\":\"low\"}},\"MAX\":{\"output_config\":{\"effort\":\"max\"}}}"
                    : "{\"LOW\":{\"reasoning_effort\":\"low\"},\"MAX\":{\"reasoning_effort\":\"max\"}}";
            LlmProfile profile = profile(ThinkingParameterPreset.CUSTOM, ThinkingLevel.MAX, mapping);
            profile.setProvider(provider);
            profile.setModel("MiniMax-M3.1-Flash");

            assertEquals(Arrays.asList(ThinkingLevel.DEFAULT, ThinkingLevel.LOW, ThinkingLevel.MAX),
                    ThinkingParameterMappings.availableLevels(profile));
            JsonObject request = provider == LlmProvider.ANTHROPIC
                    ? AnthropicLlmProviderClient.createRequestBody(profile, new LlmSettings(), "system", "user", false)
                    : OpenAiCompatibleLlmProviderClient.createRequestBody(profile, new LlmSettings(), "system", "user", false);
            String effort = provider == LlmProvider.ANTHROPIC
                    ? request.getAsJsonObject("output_config").get("effort").getAsString()
                    : request.get("reasoning_effort").getAsString();
            assertEquals("max", effort);
            assertFalse(request.has("thinking"));
            assertFalse(request.has("reasoning"));
        }
    }

    private static LlmProfile profile(ThinkingParameterPreset preset, ThinkingLevel level, String mapping) {
        LlmProfile profile = new LlmProfile();
        profile.setBaseUrl("https://custom.example/v1");
        profile.setModel("custom-model");
        profile.setThinkingParameterPreset(preset);
        profile.setThinkingParameterMapping(mapping);
        profile.setThinkingLevel(level);
        return profile;
    }
}
