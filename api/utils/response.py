from flask import jsonify, request, g
import uuid
import time
from typing import Any, Optional, Tuple

DEVELOPER = "Kicen Xensai"
API_VERSION = "1.0.0"

ERROR_CODES = {
    400: "BAD_REQUEST",
    401: "UNAUTHORIZED",
    403: "FORBIDDEN",
    404: "NOT_FOUND",
    429: "RATE_LIMIT_EXCEEDED",
    500: "INTERNAL_SERVER_ERROR",
}

# Global stats counter
_stats = {
    "total_requests": 0,
    "cache_hits": 0,
    "cache_misses": 0,
    "endpoints": {},
    "errors": 0,
}


def get_stats() -> dict:
    return dict(_stats)


def increment_stat(key: str, endpoint: Optional[str] = None) -> None:
    _stats[key] = _stats.get(key, 0) + 1
    if endpoint:
        _stats["endpoints"][endpoint] = _stats["endpoints"].get(endpoint, 0) + 1


def _build_meta(cache_hit: bool = False) -> dict:
    exec_ms = round((time.time() - getattr(g, "start_time", time.time())) * 1000, 2)
    return {
        "request_id": getattr(g, "request_id", str(uuid.uuid4())),
        "execution_time_ms": exec_ms,
        "cache_hit": cache_hit,
        "developer": DEVELOPER,
        "version": API_VERSION,
    }


def _filter_fields(data: Any, fields: Optional[str]) -> Any:
    if not fields or not isinstance(data, dict):
        return data
    keys = {f.strip() for f in fields.split(",")}
    return {k: v for k, v in data.items() if k in keys}


def success(
    data: Any,
    message: Optional[str] = None,
    status_code: int = 200,
    cache_hit: bool = False,
) -> Tuple:
    fields = request.args.get("fields") if request else None
    filtered = _filter_fields(data, fields)
    increment_stat("total_requests", request.endpoint if request else None)
    if cache_hit:
        increment_stat("cache_hits")
    else:
        increment_stat("cache_misses")
    return jsonify({
        "status": "success",
        "message": message,
        "data": filtered,
        "meta": _build_meta(cache_hit),
    }), status_code


def error(
    message: str,
    status_code: int = 400,
    data: Any = None,
) -> Tuple:
    increment_stat("errors")
    increment_stat("total_requests", request.endpoint if request else None)
    return jsonify({
        "status": "error",
        "error_code": ERROR_CODES.get(status_code, "UNKNOWN_ERROR"),
        "message": message,
        "data": data,
        "meta": _build_meta(False),
    }), status_code
