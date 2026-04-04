# Git Commit Message Helper

[English](./README.md) | [简体中文](./README.zh-CN.md)

Git Commit Message Helper 是一个 IntelliJ Platform 插件，用来帮助你在 IDE 中更高效地编写规范、清晰且一致的 Git 提交信息。

它把结构化提交编辑器、可自定义的 Conventional Commit 风格模板，以及兼容 OpenAI 接口的 LLM 能力整合在一起。你既可以手动编写提交信息，也可以根据所选变更自动生成、对已有草稿进行格式化，并根据团队规范输出最终结果。

这个项目最初基于 [git-commit-template](https://plugins.jetbrains.com/plugin/9861-git-commit-template) 增强而来，当前已经演进为一个更完整、更灵活的提交信息辅助插件。

## 功能概览

- 在 IntelliJ 的提交信息面板中直接提供提交辅助动作
- 通过结构化字段组织提交信息，包括 `type`、`scope`、`subject`、`body`、`BREAKING CHANGE`、`Closes` 和 `skip ci`
- 支持基于 Apache Velocity 的自定义提交模板
- 支持自定义可选提交类型及其描述
- 支持基于所选 Git 变更通过 LLM 生成提交信息
- 支持将已有提交信息按当前模板重新格式化
- 支持在打开手动编辑器时通过 Smart Echo 将已有提交信息回填为结构化字段
- 支持 Skip CI 预设项、默认值和字段显示控制
- 内置英文、中文、日文、韩文本地化资源

## 核心动作

插件会在 VCS 提交信息区域中提供 3 个动作：

- `Generate Commit Message`：根据当前选中的变更生成提交信息
- `Format Commit Message`：把当前提交信息重写为符合模板的格式
- `Create Commit Message`：打开结构化编辑窗口，手动编写提交信息

这 3 个动作都可以在插件设置中单独控制是否显示。

## 默认提交风格

插件默认采用类似 Conventional Commits 的模板，效果大致如下：

```text
type(scope): subject

body

BREAKING CHANGE: changes

Closes issue

[skip ci]
```

默认内置的提交类型包括：

`feat`、`fix`、`docs`、`style`、`refactor`、`perf`、`test`、`build`、`ci`、`chore`、`revert`

这些内容都可以根据团队规范进行调整，包括模板文本、字段显示、类型列表、展示方式以及 Skip CI 预设项。

## 安装

可以直接在 JetBrains Marketplace 中安装：

`File` -> `Settings` -> `Plugins` -> `Marketplace` -> `Git Commit Message Helper`

## 使用方式

![operation.gif](https://raw.githubusercontent.com/AutismSuperman/git-commit-message-helper/master/doc/image/operation.gif)

你可以直接在提交面板中使用这些动作按钮来生成、格式化或手动构建提交信息。结构化编辑窗口适合需要精细控制每个字段的场景，而 LLM 相关动作更适合根据当前 diff 快速得到一个初稿。

## 配置说明

插件设置入口：

`File` -> `Settings` -> `GitCommitMessageHelper`

你可以在这里配置：

- 类型展示模式，以及在界面中内联展示多少个类型
- 哪些提交字段在编辑器中隐藏
- Skip CI 预设项与默认值
- LLM 的 Base URL、API Key、Model、Temperature、Response Language 和 Smart Echo
- 3 个提交动作的显示状态

### 通用设置

![settings-0.png](https://raw.githubusercontent.com/AutismSuperman/git-commit-message-helper/master/doc/image/settings-0.png)

### 提交模板

提交模板由 Apache Velocity 驱动，因此你可以完全控制最终提交信息的渲染格式。

![settings-1.png](https://raw.githubusercontent.com/AutismSuperman/git-commit-message-helper/master/doc/image/settings-1.png)

### 提交类型

你可以按照团队工作流自由调整允许使用的提交类型及其描述。

![settings-2.png](https://raw.githubusercontent.com/AutismSuperman/git-commit-message-helper/master/doc/image/settings-2.png)

## LLM 兼容性

当前 AI 集成使用的是兼容 OpenAI 的 Chat Completions API。

配置中的 `Base URL` 可以是：

- 以 `/chat/completions` 结尾的完整接口地址
- 服务基础地址，例如 `https://api.openai.com/v1`，插件会自动补上 `/chat/completions`

请求会使用：

- `Authorization: Bearer <API Key>`
- 包含 `model`、`temperature`、`stream`、`messages` 等字段的 JSON 请求体

因此，只要服务提供的是 OpenAI 风格的 Chat Completions 接口，理论上都可以接入本插件。

### Smart Echo

当开启 Smart Echo，且提交面板中已经存在提交信息时，手动编辑窗口会调用 LLM，把当前提交信息解析回结构化字段，例如 `type`、`scope`、`subject`、`body`、`changes`、`closes` 和 `skipCi`。这样你就可以在已有草稿的基础上继续微调，而不是从头重新填写。

## 开发

- 基于 Java 11
- 使用 Gradle IntelliJ Plugin 构建
- 目标 IntelliJ Platform 版本为 `2020.3+`

常用命令：

```bash
./gradlew runIde
./gradlew buildPlugin
```

## License

项目采用 [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)。

## 致谢

- [git-commit-template](https://github.com/MobileTribe/commit-template-idea-plugin)
- [CodeMaker](https://github.com/x-hansong/CodeMaker)
- [leetcode-editor](https://github.com/shuzijun/leetcode-editor)
