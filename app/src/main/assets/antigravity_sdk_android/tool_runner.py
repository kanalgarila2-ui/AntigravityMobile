import asyncio
import inspect
import os
import shutil
import subprocess
from typing import Any, Callable, Dict, List, Optional
from .types import ToolCall, ToolResult

class ToolContext:
    def __init__(self, conversation_id: str, workspace_dir: str):
        self.conversation_id = conversation_id
        self.workspace_dir = workspace_dir
        self._state: Dict[str, Any] = {}

    def get_state(self, key: str, default: Any = None) -> Any:
        return self._state.get(key, default)

    def set_state(self, key: str, value: Any) -> None:
        self._state[key] = value

class ToolWithSchema:
    def __init__(self, func: Callable, name: str, description: str, parameters_schema: Dict[str, Any]):
        self.func = func
        self.name = name
        self.description = description
        self.parameters_schema = parameters_schema

class ToolRunner:
    def __init__(self, tools: Optional[List[Callable]] = None, context: Optional[ToolContext] = None):
        self._registry: Dict[str, Callable] = {}
        self.context = context
        if tools:
            for t in tools:
                self.register(t)

    def register(self, tool: Callable, name: Optional[str] = None) -> None:
        tool_name = name or getattr(tool, "name", getattr(tool, "__name__", "unnamed_tool"))
        self._registry[tool_name] = tool

    async def execute(self, tool_name: str, **kwargs) -> Any:
        if tool_name not in self._registry:
            raise ValueError(f"Tool '{tool_name}' is not registered in ToolRunner.")
        fn = self._registry[tool_name]
        target = fn.func if isinstance(fn, ToolWithSchema) else fn
        sig = inspect.signature(target)
        if "context" in sig.parameters and self.context is not None:
            kwargs["context"] = self.context
        if inspect.iscoroutinefunction(target):
            return await target(**kwargs)
        loop = asyncio.get_running_loop()
        return await loop.run_in_executor(None, lambda: target(**kwargs))

    async def process_tool_calls(self, tool_calls: List[ToolCall]) -> List[ToolResult]:
        results: List[ToolResult] = []
        for tc in tool_calls:
            try:
                out = await self.execute(tc.name, **tc.args)
                results.append(ToolResult(name=tc.name, result=out, is_error=False, call_id=tc.call_id))
            except Exception as exc:
                results.append(ToolResult(name=tc.name, result=str(exc), is_error=True, call_id=tc.call_id))
        return results
