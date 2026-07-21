package com.fulinlin.model;

import java.util.Objects;

public class PromptProfile {

    private String id;
    private String name;
    private String prompt;
    private Boolean defaultPrompt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public Boolean getDefaultPrompt() {
        return defaultPrompt;
    }

    public void setDefaultPrompt(Boolean defaultPrompt) {
        this.defaultPrompt = defaultPrompt;
    }

    public boolean isDefaultPrompt() {
        return Boolean.TRUE.equals(defaultPrompt);
    }

    @Override
    public String toString() {
        return name == null ? "" : name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PromptProfile)) return false;
        PromptProfile that = (PromptProfile) o;
        return Objects.equals(id, that.id)
                && Objects.equals(name, that.name)
                && Objects.equals(prompt, that.prompt)
                && Objects.equals(defaultPrompt, that.defaultPrompt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, prompt, defaultPrompt);
    }
}
