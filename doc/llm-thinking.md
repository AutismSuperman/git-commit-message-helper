# Thinking Configuration

Each profile selects an API format, a parameter preset, and a thinking level.
`Model default` sends no thinking override. It is not an instruction to disable reasoning.
Automatic compatibility is a legacy heuristic, not a model capability detector.
Use an explicit mapping when the model or gateway has different rules.

## Models That Always Think

If the model always reasons without an override, choose the Model default preset.
The selector then contains only Model default.

For an explicit on-only switch, select Custom JSON mapping and use:

```json
{
  "ENABLED": {
    "thinking": {
      "type": "adaptive"
    }
  }
}
```

Choose Enabled to send that fragment. The selector contains only Model default
and Enabled; there is no Off or invented effort level.
This example applies only to APIs that accept `thinking.type=adaptive`.
Other APIs may require a different fragment, such as `{"enable_thinking": true}`.

Mappings describe request parameters, not a universal capability policy.
The plugin cannot force an always-on model to stop reasoning, discover an upstream
model behind an alias, or guarantee that a provider honors a parameter.
If a mapped field is explicitly rejected, compatibility fallback retries without
the thinking overrides and caches that mapping's rejection. The model then uses
its own defaults, which can still include reasoning.

## MiniMax M3 and M3.1 Flash

Verified against the official documentation on 2026-10-25:

| Model | Chat Completions, no override | Anthropic Messages, no override | Can turn thinking off? |
| --- | --- | --- | --- |
| MiniMax-M3 | On | Off | Yes |
| MiniMax-M3.1-Flash | On | On | No |

Use the Chat Completions API format with a base URL such as
`https://api.minimax.io/v1`, or the Anthropic Messages format with
`https://api.minimax.io/anthropic`. Third-party gateways may differ.

For MiniMax-M3, an explicit toggle mapping for either protocol is:

```json
{
  "DISABLED": {
    "thinking": {
      "type": "disabled"
    }
  },
  "ENABLED": {
    "thinking": {
      "type": "adaptive"
    }
  }
}
```

Do not use `thinking.type=enabled` for these MiniMax endpoints.
For MiniMax-M3.1-Flash, use Model default or the on-only mapping above.
Do not configure Off or an effort value of `none`; the official API rejects them.
MiniMax M2.x also always thinks, but its supported protocols and treatment of
unsupported fields differ from the M3 family.

### Adjustable Depth on M3.1 Flash

MiniMax-M3.1-Flash supports `low`, `medium`, `high`, `xhigh`, and `max`.
The Chat Completions field is `reasoning_effort`; the Anthropic Messages field is
`output_config.effort`. MiniMax-M3 does not expose this depth control.

For Chat Completions, this mapping exposes Low, Medium, High, and Max without Off:

```json
{
  "LOW": {"reasoning_effort": "low"},
  "MEDIUM": {"reasoning_effort": "medium"},
  "HIGH": {"reasoning_effort": "high"},
  "MAX": {"reasoning_effort": "max"}
}
```

For Anthropic Messages, use:

```json
{
  "LOW": {"output_config": {"effort": "low"}},
  "MEDIUM": {"output_config": {"effort": "medium"}},
  "HIGH": {"output_config": {"effort": "high"}},
  "MAX": {"output_config": {"effort": "max"}}
}
```

There is no separate Xhigh UI level. To request it, map an existing level to
`"xhigh"` instead, for example `"MAX": {"reasoning_effort": "xhigh"}`.
The custom mapping is sent verbatim; OpenAI's GPT-5.2-specific maximum-effort
translation does not change MiniMax mappings.

The plugin uses visible answer text for commit messages, not separate
`reasoning_content` or Anthropic thinking blocks. Thinking tokens still consume
the response token limit, so increase that limit if the provider returns no
visible answer after exhausting the budget.

## Sources

- [MiniMax OpenAI-compatible API](https://platform.minimax.io/docs/api-reference/text-openai-api)
- [MiniMax Anthropic-compatible API](https://platform.minimax.io/docs/api-reference/text-anthropic-api)
- [MiniMax M3.1 Flash FAQ](https://agent.minimax.io/tools/m3.1-flash)
