# Git Commit Message Helper

[English](./README.md) | [简体中文](./README.zh-CN.md)

Git Commit Message Helper 是一个 IntelliJ Platform 插件，用来帮助你在 IDE 中更高效地编写规范、清晰且一致的 Git 提交信息。

它把结构化提交编辑器、可自定义的 Conventional Commit 风格模板，以及支持多 Provider 的 LLM 能力整合在一起。你既可以手动编写提交信息，也可以根据所选变更自动生成、对已有草稿进行格式化，并根据团队规范输出最终结果。

这个项目最初基于 [git-commit-template](https://plugins.jetbrains.com/plugin/9861-git-commit-template) 增强而来，当前已经演进为一个更完整、更灵活的提交信息辅助插件。

## JetBrains Marketplace

[![Git Commit Message Helper](https://raw.githubusercontent.com/AutismSuperman/git-commit-message-helper/master/doc/image/operation.png)](https://plugins.jetbrains.com/plugin/13477-git-commit-message-helper)

可直接从 [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/13477-git-commit-message-helper) 安装。

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

可以直接在 [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/13477-git-commit-message-helper) 中安装：

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
- LLM 的 Provider、Base URL、API Key、Model、Temperature、Response Language 和 Smart Echo
- 可复用提示词，以及全局默认和项目默认提示词
- 4 个提交动作的显示状态

### 通用设置

![settings-0.png](https://raw.githubusercontent.com/AutismSuperman/git-commit-message-helper/master/doc/image/settings-0.png)

### 提交模板

提交模板由 Apache Velocity 驱动，因此你可以完全控制最终提交信息的渲染格式。

![settings-1.png](https://raw.githubusercontent.com/AutismSuperman/git-commit-message-helper/master/doc/image/settings-1.png)

### 提交类型

你可以按照团队工作流自由调整允许使用的提交类型及其描述。

![settings-2.png](https://raw.githubusercontent.com/AutismSuperman/git-commit-message-helper/master/doc/image/settings-2.png)

### LLM 设置

你可以在专门的设置页中配置 LLM 的 Provider、接口地址、鉴权信息、模型、Temperature、返回语言以及 Smart Echo 等行为。

![settings-3.png](https://raw.githubusercontent.com/AutismSuperman/git-commit-message-helper/master/doc/image/settings-3.png)

### 提示词

提示词设置页支持创建、重命名、编辑和删除多个提示词，并可分别选择全局默认和当前项目默认提示词。项目提示词优先生效；项目没有有效选择时，自动使用全局默认提示词。内置的空 `Default` 提示词不可删除，用于保持未配置时的原有行为。

所选提示词会用于 LLM 提交信息生成和格式化，不会参与 Smart Echo 对已有提交信息的结构化解析。带附加信息的生成动作仍可为当前一次请求补充问题编号、Skip CI 或其他临时要求。

#### 生成请求中的提示词结构

生成提交信息时，请求大致由以下内容组成：

```text
System Prompt
  你是一名高级工程师和 Git 维护者。
  分析所选变更，识别主要意图，并填写提交模板字段。
  只返回要求的 JSON。

  Persistent User Preferences
  <当前项目提示词；没有项目提示词时使用全局提示词>

  自定义偏好不能覆盖 JSON 结构、提交模板、允许类型、Git diff
  以及插件要求的其他输出约束。

User Prompt
  - 内部分析要求
  - JSON 输出结构和字段规则
  - 本次附加信息（如果使用附加信息生成动作）
  - 允许使用的提交类型
  - 当前 Velocity 提交模板及预览
  - 文件列表、diff 统计和实际 Git diff
```

最终优先关系为：

1. 插件内置的 JSON、模板和合法类型约束
2. 当前项目默认提示词；未配置时使用全局默认提示词
3. 当前一次生成输入的附加信息
4. Git diff 和实际代码事实；提示词不能要求模型编造 diff 中不存在的变更

LLM 返回结构化字段 `type`、`scope`、`subject`、`body`、`changes`、`closes` 和 `skipCi`，插件再通过当前 Velocity 模板在本地渲染最终提交信息。

## LLM 兼容性

每个模型配置可以选择三种接口协议之一，同一个 Base URL 只要协议匹配就能接入：

| 接口协议 | 端点 | 请求要点 |
| --- | --- | --- |
| `Chat Completions` | `/chat/completions` | `model`、`messages`、`temperature`、`max_tokens` |
| `Anthropic Messages` | `/v1/messages` | `x-api-key` 与 `anthropic-version` 请求头、`system`、`max_tokens` |
| `Responses` | `/responses` | `instructions`、`input`、`max_output_tokens`、`reasoning.effort` |

任意协议下 `Base URL` 既可以填写完整接口地址，也可以填写 `https://api.openai.com/v1` 这样的基础地址，插件会自动补全对应路径。

### 思考等级

每个模型配置都有一个思考等级：`模型默认`、`关闭(最快)`、`低`、`中`、`高`、`最高`。插件会把所选等级翻译成当前接口协议支持的参数，你不需要记住各家厂商的字段名：

- **Chat Completions**：Qwen/DashScope 走 `enable_thinking`；智谱、Moonshot、豆包/火山、MiMo、DeepSeek 走 `thinking` 对象；OpenRouter 与 MiniMax 走 `reasoning.effort` 对象；OpenAI 推理模型、Gemini、Grok 及其他兼容网关走 `reasoning_effort`。OpenAI `o*`/`gpt-5` 系列使用 `max_completion_tokens`。
- **Anthropic Messages**：发送经典 `thinking` 对象，并按等级与模型最大响应 token 数推导思考预算；支持分级 effort 的网关则发送 `output_config.effort`。思考开启期间不会发送 temperature，因为 Anthropic 此时只接受默认值。
- **Responses**：发送 `reasoning.effort`，响应预算使用 `max_output_tokens`。

选择“模型默认”不会发送任何思考参数，保持服务商自身的默认行为。若服务商拒绝这些额外参数，请求会自动去掉它们重试一次，并记住该模型后续请求不再携带。

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
