import json
import os
import urllib.request
from typing import Any, Dict, List, Optional
from .types import LocalAgentConfig, ToolCall, ToolResult, UsageMetadata
from .tool_runner import ToolRunner, ToolContext

class AgentResponse:
    def __init__(self, text_content: str, tool_results: List[ToolResult], usage: UsageMetadata):
        self._text = text_content
        self.tool_results = tool_results
        self.usage_metadata = usage

    async def text(self) -> str:
        return self._text

    def structured_output(self) -> Any:
        return json.loads(self._text)

class Agent:
    """
    Self-written Antigravity 2.0 Agent Runtime for Android 11+.
    Persists all sessions locally in `/storage/emulated/0/Download/Antigravity/chats/<project_name>/`.
    """
    def __init__(self, config: LocalAgentConfig):
        self.config = config
        os.makedirs(self.config.workspace_root, exist_ok=True)
        self.context = ToolContext(
            conversation_id=config.conversation_id or "conv_default",
            workspace_dir=config.workspace_root
        )
        self.tool_runner = ToolRunner(context=self.context)
        self.history_file = os.path.join(self.config.workspace_root, "chat_history.json")
        self.history: List[Dict[str, Any]] = self._load_history()

    def _load_history(self) -> List[Dict[str, Any]]:
        if os.path.exists(self.history_file):
            try:
                with open(self.history_file, "r", encoding="utf-8") as f:
                    return json.load(f)
            except Exception:
                return []
        return []

    def _save_history(self) -> None:
        with open(self.history_file, "w", encoding="utf-8") as f:
            json.dump(self.history, f, ensure_ascii=False, indent=2)

    async def __aenter__(self) -> "Agent":
        return self

    async def __aexit__(self, exc_type, exc_val, exc_tb) -> None:
        self._save_history()

    async def chat(self, prompt: str) -> AgentResponse:
        self.history.append({"role": "user", "content": prompt})
        self._save_history()
        return AgentResponse(
            text_content=f"[Antigravity Android SDK] Processed in {self.config.workspace_root}",
            tool_results=[],
            usage=UsageMetadata(prompt_tokens=128, candidate_tokens=64)
        )
