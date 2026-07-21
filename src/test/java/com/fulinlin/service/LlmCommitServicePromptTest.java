package com.fulinlin.service;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LlmCommitServicePromptTest {

    @Test
    public void emptyCustomPromptKeepsBuiltInPromptUnchanged() {
        assertEquals("built-in", LlmCommitService.buildSystemPrompt("built-in", "  "));
    }

    @Test
    public void customPromptIsAddedAsConstrainedPreference() {
        String result = LlmCommitService.buildSystemPrompt("built-in", "Write the body in Chinese.");

        assertTrue(result.startsWith("built-in"));
        assertTrue(result.contains("Persistent User Preferences:\nWrite the body in Chinese."));
        assertTrue(result.contains("do not conflict with the required JSON shape"));
    }
}
