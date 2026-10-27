from dataclasses import dataclass, field
from enum import Enum
from typing import Any, Callable, Dict, List, Optional

class ThinkingLevel(str, Enum):
    MINIMAL = "MINIMAL"
    LOW = "LOW"
    MEDIUM = "MEDIUM"
    HIGH = "HIGH"

@dataclass
class GenerationConfig:
    thinking_level: ThinkingLevel = ThinkingLevel.HIGH
    temperature: float = 0.2
    max_output_tokens: int = 8192

class BuiltinTools:
    ALL = [
        "run_command", "view_file", "write_to_file", "edit_file",
        "list_dir", "search_files", "spawn_subagent", "create_artifact",
        "termux_exec", "phone_control"
    ]
    @staticmethod
    def read_only() -> List[str]:
        return ["view_file", "list_dir", "search_files"]

    @staticmethod
    def all_tools(include_phone: bool = False) -> List[str]:
        tools = list(BuiltinTools.ALL)
        if not include_phone and "phone_control" in tools:
            tools.remove("phone_control")
        return tools

@dataclass
class CapabilitiesConfig:
    enabled_tools: Optional[List[str]] = None
    disabled_tools: List[str] = field(default_factory=list)

@dataclass
class ToolCall:
    name: str
    args: Dict[str, Any]
    call_id: str = ""

@dataclass
class ToolResult:
    name: str
    result: Any
    is_error: bool = False
    call_id: str = ""

@dataclass
class UsageMetadata:
    prompt_tokens: int = 0
    candidate_tokens: int = 0
    cached_tokens: int = 0
    thinking_tokens: int = 0

@dataclass
class LocalAgentConfig:
    project_name: str = "default-project"
    workspace_root: str = "/storage/emulated/0/Download/Antigravity/chats/default-project"
    model: str = "gemini-2.5-pro"
    api_key: Optional[str] = None
    oauth_access_token: Optional[str] = None
    vertex: bool = False
    project: Optional[str] = None
    location: str = "us-central1"
    system_instructions: str = ""
    max_steps: int = 100
    auto_disable_step_limit: bool = False
    phone_control_enabled: bool = False
    allow_all_dangerously: bool = False
    agy_unlocker_proxy: Optional[str] = "http://127.0.0.1:53129"
    capabilities: CapabilitiesConfig = field(default_factory=CapabilitiesConfig)
    policies: List[Callable] = field(default_factory=list)
    skills_paths: List[str] = field(default_factory=list)
    conversation_id: Optional[str] = None
