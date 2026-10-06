# Git Commit Message Helper

[English](./README.md) | [简体中文](./README.zh-CN.md)

Git Commit Message Helper 是一个 IntelliJ Platform 插件，把结构化提交编辑器、可自定义的 Conventional Commit 模板（Apache Velocity 驱动）和支持多种协议的 LLM 能力整合进提交面板，帮助你在 IDE 中写出规范、清晰、一致的提交信息。

![Git Commit Message Helper](https://raw.githubusercontent.com/AutismSuperman/git-commit-message-helper/master/doc/image/operation.png)

## 安装

从 [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/13477-git-commit-message-helper) 安装：

`File` → `Settings` → `Plugins` → `Marketplace` → `Git Commit Message Helper`

## 提交面板动作

插件会在 VCS 提交信息区域中加入四个动作，每个动作都可以在设置中单独控制是否显示。

| 动作 | 用途 |
| --- | --- |
| `Generate Commit Message` | 根据选中的变更通过 LLM 生成提交信息。 |
| `Generate Commit Message With Additional Context` | 先输入本次生成的附加要求（关闭的 issue、跳过 CI、Bug 说明等），再生成。 |
| `Format Commit Message` | 把当前草稿重写为符合模板的格式。 |
| `Create Commit Message` | 打开结构化编辑器，手动编写提交信息。 |

![结构化提交编辑器](https://raw.githubusercontent.com/AutismSuperman/git-commit-message-helper/master/doc/image/commit-dialog.png)

## 配置

打开 `File` → `Settings` → `Git 提交消息助手`，一切皆可配置：

- 提交模板（Apache Velocity），支持多模板档案与全局/项目默认
- 允许使用的提交类型及其描述
- 类型展示方式、隐藏字段、Skip CI 预设
- 可复用提示词档案，支持全局默认与项目默认
- 四个提交动作的显示状态

## LLM 兼容性

每个模型配置可以选择三种接口协议之一，同一个 Base URL 只要协议匹配就能接入：

| 接口协议 | 端点 | 适用 |
| --- | --- | --- |
| `Chat Completions` | `/chat/completions` | OpenAI 兼容端点（OpenAI、DeepSeek、Qwen、Moonshot、MiniMax 等） |
| `OpenAI Responses` | `/responses` | OpenAI Responses API 及兼容网关 |
| `Anthropic Messages` | `/v1/messages` | Claude 及 Anthropic 兼容网关 |

`Base URL` 既可以填写完整接口地址，也可以填写 `https://api.openai.com/v1` 这样的基础地址，插件会自动补全对应路径。

每个模型配置还有独立的**思考等级**（`模型默认`、`关闭`、`低`、`中`、`高`、`最高`）。插件会把等级翻译成当前协议支持的参数，如果服务商拒绝这些字段会自动去掉重试。

![LLM 设置](https://raw.githubusercontent.com/AutismSuperman/git-commit-message-helper/master/doc/image/settings-llm.png)

**Smart Echo**：当提交面板中已有提交信息时，结构化编辑器可以调用 LLM 把它解析回结构化字段（`type`、`scope`、`subject`、`body`、`changes`、`closes`、`skipCi`），在已有草稿上继续微调而不是从头填写。

## 开发

- Java 11，Gradle IntelliJ Plugin 构建，目标 IntelliJ Platform `2020.3+`

```bash
./gradlew buildPlugin
```

## License

项目采用 [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)。

## 致谢

- [git-commit-template](https://github.com/MobileTribe/commit-template-idea-plugin)
- [CodeMaker](https://github.com/x-hansong/CodeMaker)
- [leetcode-editor](https://github.com/shuzijun/leetcode-editor)
