# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Git Commit Message Helper is an IntelliJ IDEA plugin that helps generate structured Git commit messages following the conventional commits format. It provides a dialog for entering commit details and uses Apache Velocity templates to format the final message.

## Build Commands

```bash
# Build the plugin
./gradlew build

# Run IDE with the plugin installed (for testing)
./gradlew runIde

# Build plugin distribution (creates .zip in build/distributions/)
./gradlew buildPlugin

# Verify plugin
./gradlew verifyPlugin
```

## Architecture

### Package Structure (`com.fulinlin`)

- **action/** - Entry point via `CreateCommitAction`, triggered from the VCS commit dialog toolbar
- **storage/** - Persistent settings storage:
  - `GitCommitMessageHelperSettings` - Application-level settings (templates, type aliases)
  - `GitCommitMessageStorage` - Project-level storage for preserving commit draft state
- **model/** - Data models including `CommitTemplate`, `CentralSettings`, `DataSettings`, `TypeAlias`
- **ui/** - Swing-based UI components:
  - `commit/` - Commit dialog and panel for message creation
  - `central/` - Settings panel for display options
  - `setting/` - Template and type alias editor panels
- **utils/** - `VelocityUtils` handles template conversion using Apache Velocity
- **configurable/** - IntelliJ settings configurables for the plugin preferences
- **localization/** - i18n support via `PluginBundle` with properties files for EN, ZH, JA

### Template System

The plugin uses Apache Velocity for commit message templates. Available variables in templates:
- `type`, `scope`, `subject`, `body`, `changes`, `closes`, `skipCi` - commit fields
- `newline` - newline character
- `velocityTool` - utility methods

Default template location: `src/main/resources/includes/defaultTemplate.vm`

### Settings Persistence

Settings are persisted via IntelliJ's `PersistentStateComponent` mechanism:
- Application settings stored in `$APP_CONFIG$/GitCommitMessageHelperSettings-settings.xml`
- Settings include: commit template, type aliases (feat/fix/docs etc.), display style preferences

## Key Files

- `src/main/resources/META-INF/plugin.xml` - Plugin configuration, action registrations, service definitions
- `build.gradle` - Build configuration using gradle-intellij-plugin v1.7.0
- `gradle.properties` - Plugin version, IDE version (2020.3), and build parameters
- `src/main/resources/i18n/info*.properties` - Localization strings

## Development Notes

- Java 11 target compatibility
- Plugin compatible with IDE builds 203.392+ (2020.3+)
- UI forms use IntelliJ's GUI Designer (.form files paired with Java classes)
- No test infrastructure currently exists in this project
