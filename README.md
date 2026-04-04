# Git Commit Message Helper

[English](./README.md) | [简体中文](./README.zh-CN.md)

Git Commit Message Helper is an IntelliJ Platform plugin for writing cleaner, more consistent commit messages without leaving the IDE.

It combines a structured commit editor, customizable Conventional Commit style templates, and OpenAI-compatible LLM assistance into one workflow. You can create commit messages manually, generate them from selected changes, reformat existing drafts, and tune the final output to match your team's conventions.

This project started as an enhanced version of [git-commit-template](https://plugins.jetbrains.com/plugin/9861-git-commit-template) and has evolved into a more configurable commit authoring assistant.

## What It Does

- Adds commit actions directly to the IntelliJ commit message panel.
- Helps compose commit messages with structured fields such as `type`, `scope`, `subject`, `body`, `BREAKING CHANGE`, `Closes`, and `skip ci`.
- Supports customizable commit templates powered by Apache Velocity.
- Lets you manage allowed commit types and their descriptions.
- Can generate a commit message from the selected git changes with an LLM.
- Can rewrite an existing commit message to match the configured template.
- Can parse an existing commit message back into structured fields with Smart Echo when opening the manual editor.
- Supports optional skip-CI presets and configurable field visibility.
- Includes localized resources for English, Chinese, Japanese, and Korean.

## Main Actions

The plugin contributes three actions to the VCS commit message area:

- `Generate Commit Message`: generates a message from the currently selected changes.
- `Format Commit Message`: rewrites the current commit message to fit the configured template.
- `Create Commit Message`: opens the structured editor for manual commit composition.

Each action can be individually shown or hidden from the plugin settings.

## Default Commit Style

Out of the box, the plugin uses a Conventional Commit style template similar to:

```text
type(scope): subject

body

BREAKING CHANGE: changes

Closes issue

[skip ci]
```

The default commit types are:

`feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`, `revert`

Everything here is configurable, including the template text, visible fields, type list, display style, and skip-CI presets.

## Installation

Install from the JetBrains Marketplace inside your IDE:

`File` -> `Settings` -> `Plugins` -> `Marketplace` -> `Git Commit Message Helper`

## Usage

![operation.gif](https://raw.githubusercontent.com/AutismSuperman/git-commit-message-helper/master/doc/image/operation.gif)

Use the action buttons in the commit panel to generate, format, or manually build a commit message. The structured dialog is useful when you want strict control over each field, while the LLM actions help when you want a faster draft based on the selected diff.

## Configuration

Open the plugin settings from:

`File` -> `Settings` -> `GitCommitMessageHelper`

You can configure:

- Type display mode and how many type options are shown inline
- Which commit fields are hidden in the editor
- Skip-CI presets and defaults
- LLM base URL, API key, model, temperature, response language, and Smart Echo
- Visibility of the three commit actions

### General Settings

![settings-0.png](https://raw.githubusercontent.com/AutismSuperman/git-commit-message-helper/master/doc/image/settings-0.png)

### Commit Template

The commit template is powered by Apache Velocity, so you can fully customize how the final commit message is rendered.

![settings-1.png](https://raw.githubusercontent.com/AutismSuperman/git-commit-message-helper/master/doc/image/settings-1.png)

### Commit Types

You can edit the allowed commit types and their descriptions to match your team's workflow.

![settings-2.png](https://raw.githubusercontent.com/AutismSuperman/git-commit-message-helper/master/doc/image/settings-2.png)

## LLM Compatibility

The current AI integration uses an OpenAI-compatible Chat Completions API.

Your configured `Base URL` can be either:

- a full endpoint ending with `/chat/completions`
- or a server base URL such as `https://api.openai.com/v1`, in which case the plugin automatically appends `/chat/completions`

The request uses:

- `Authorization: Bearer <API Key>`
- JSON fields including `model`, `temperature`, `stream`, and `messages`

This makes the plugin compatible with services that expose an OpenAI-style chat completions interface.

### Smart Echo

When Smart Echo is enabled and the commit panel already contains text, the manual commit dialog can ask the LLM to parse the current message back into structured fields like `type`, `scope`, `subject`, `body`, `changes`, `closes`, and `skipCi`. This is useful when you want to refine an existing draft in the structured editor instead of starting over.

## Development

- Built with Java 11
- Uses the Gradle IntelliJ Plugin
- Targets IntelliJ Platform `2020.3+`

Useful commands:

```bash
./gradlew runIde
./gradlew buildPlugin
```

## License

Licensed under the [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0).

## Credits

- [git-commit-template](https://github.com/MobileTribe/commit-template-idea-plugin)
- [CodeMaker](https://github.com/x-hansong/CodeMaker)
- [leetcode-editor](https://github.com/shuzijun/leetcode-editor)
