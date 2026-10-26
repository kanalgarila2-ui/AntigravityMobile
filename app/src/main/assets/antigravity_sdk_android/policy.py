from typing import Any, Callable, Dict, Optional
from .types import ToolCall

def allow_all() -> Callable[[ToolCall], bool]:
    def _rule(call: ToolCall) -> bool:
        return True
    _rule.__policy_type__ = "allow_all"
    return _rule

def allow(pattern: str, when: Optional[Callable[[Dict[str, Any]], bool]] = None) -> Callable[[ToolCall], Optional[bool]]:
    def _rule(call: ToolCall) -> Optional[bool]:
        if pattern == "*" or call.name == pattern:
            if when is None or when(call.args):
                return True
        return None
    return _rule

def deny(pattern: str, when: Optional[Callable[[Dict[str, Any]], bool]] = None) -> Callable[[ToolCall], Optional[bool]]:
    def _rule(call: ToolCall) -> Optional[bool]:
        if pattern == "*" or call.name == pattern:
            if when is None or when(call.args):
                return False
        return None
    return _rule

def ask_user(pattern: str, handler: Callable[[ToolCall], bool]) -> Callable[[ToolCall], Optional[bool]]:
    def _rule(call: ToolCall) -> Optional[bool]:
        if pattern == "*" or call.name == pattern:
            return handler(call)
        return None
    return _rule
