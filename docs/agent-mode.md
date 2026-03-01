# Agent Mode — Technical Reference

## Overview

Agent Mode is an opt-in feature (toggled per-session in the Chat UI) that routes user goals through a multi-step ReAct reasoning loop instead of a single direct LLM call. The core module is `:agent-core`.

---

## Module Boundaries

```
:app
 └── :agent-core          ← orchestration, Koog wiring, tool definitions
      ├── :edge-ai-core   ← IAgentOrchestrator interface, EngineOrchestrator, MemoryMonitor
      └── :doc-library    ← IRagEngine (semantic search)
```

`:edge-ai-core` defines the public contract (`IAgentOrchestrator`) so that `:app` never depends directly on Koog.

---

## Key Classes

| Class | Module | Role |
|---|---|---|
| `IAgentOrchestrator` | `:edge-ai-core` | Interface: `runAgent()`, `agentSteps: SharedFlow`, `isReady()` |
| `EduMateAgentOrchestrator` | `:agent-core` | Koog `AIAgent` wrapper; memory-gated loop |
| `LocalEnginePromptExecutor` | `:agent-core` | Koog `PromptExecutor` backed by `EngineOrchestrator` |
| `RagSearchTool` | `:agent-core` | Koog `SimpleTool` — semantic search via `IRagEngine` |
| `MaterialSummaryTool` | `:agent-core` | Koog `SimpleTool` — RAG retrieval + summarization |
| `WorksheetTool` | `:agent-core` | Koog `SimpleTool` — RAG retrieval + worksheet generation |

---

## Agent Loop (ReAct)

```
User goal
   │
   ▼
AIAgent (Koog 0.6.3)
   │
   ├─► LocalEnginePromptExecutor.execute()
   │        │  builds plain-text prompt with tool schema
   │        │  calls EngineOrchestrator.generateComplete()
   │        └─► parses response:
   │               JSON  → Message.Tool.Call  (tool invocation)
   │               text  → Message.Assistant  (final answer)
   │
   ├─► [if tool call] ToolRegistry executes the matched SimpleTool
   │        result fed back into next iteration
   │
   └─► repeat until text response OR maxIterations exhausted
```

### Tool Call Format

The model is expected to emit a single-line JSON:

```json
{"name": "tool_name", "arguments": {"param": "value"}}
```

`LocalEnginePromptExecutor.tryParseToolCall()` scans for the first `{`, extracts the balanced JSON object, and resolves the tool name from `name`, `tool`, or falling back to nothing.

---

## Memory Gating

`EduMateAgentOrchestrator.resolveMaxIterations()` queries `MemoryMonitor` before each `agent.run()`:

| RAM Pressure | `maxIterations` |
|---|---|
| `NORMAL` | 5 |
| `MODERATE` | 3 |
| `CRITICAL` | 2 |

Rationale: each iteration accumulates context tokens. On a Nothing 3A (~8 GB), with a ~900 MB model loaded, MODERATE pressure kicks in around 1.5 GB free.

---

## Step Streaming

`IAgentOrchestrator.agentSteps: SharedFlow<AgentStep>` emits during `runAgent()`:

```kotlin
sealed class AgentStep {
    data class ToolCalling(val toolName: String, val args: String)
    data class ToolResult(val toolName: String, val result: String)
    data class Thinking(val text: String)
}
```

`ChatViewModel` collects this flow and appends step cards to `ChatUiState.messages` so the UI shows live progress.

---

## LLM Model Configuration

- **Primary (recommended):** Gemma 3n E2B — larger context comprehension, VLM support for scanned docs.
- **Lightweight alternative:** LFM2-1.2B-Tool-GGUF (Q4_K_M, ~900 MB) — explicit tool-call fine-tune, lower RAM.

`EngineOrchestrator` holds a single active model; agent mode uses whatever is currently loaded. No second model is spun up concurrently.

The Koog `LLModel` descriptor used:

```kotlin
LLModel(
    provider = LocalLLMProvider,   // sentinel LLMProvider("local", "Local Engine")
    id = "local-engine",
    capabilities = [Tools, Temperature],
    contextLength = 8192,
    maxOutputTokens = 512
)
```

---

## UI Integration

Toggle lives in the `ChatScreen` `TopAppBar` (`Switch` + `Psychology` icon).

When agent mode is enabled:
- `AgentModeBanner` slides in (green = model ready, amber = not ready).
- `QuickActionChips` switches to agent-oriented prompts.
- `InputBar` placeholder changes to "Describe your study goal…".
- `ChatViewModel.sendAgentMessage()` is called instead of the direct RAG path.
- Step cards (`ToolCalling`, `ToolResult`, `Thinking`) render inline in the message list.

`ChatUiState.isModelReady` is set by `agentOrchestrator.isReady()` on toggle, which checks `EngineOrchestrator.isInferenceReady()`.

---

## Dependencies

- **JetBrains Koog** `0.6.3` (`ai.koog:koog-agents`) — Apache 2.0
- **Kotlin Serialization** — tool argument `@Serializable` data classes
- Packaging exclusions required in both `:app` and `:agent-core` due to Netty/Apache transitive conflicts from Koog's gRPC dependencies:
  ```
  META-INF/INDEX.LIST
  META-INF/io.netty.versions.properties
  META-INF/DEPENDENCIES
  ```
