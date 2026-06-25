import time
import threading
import logging
from typing import Any, Optional, Dict, Tuple

logger = logging.getLogger(__name__)


class TTLCache:
    def __init__(self, default_ttl: int = 600) -> None:
        self._cache: Dict[str, Tuple[Any, float]] = {}
        self._lock = threading.Lock()
        self._default_ttl = default_ttl

    def get(self, key: str) -> Optional[Any]:
        with self._lock:
            if key in self._cache:
                value, expires_at = self._cache[key]
                if time.time() < expires_at:
                    logger.debug(f"Cache HIT: {key}")
                    return value
                else:
                    del self._cache[key]
                    logger.debug(f"Cache EXPIRED: {key}")
        return None

    def set(self, key: str, value: Any, ttl: Optional[int] = None) -> None:
        ttl = ttl if ttl is not None else self._default_ttl
        with self._lock:
            self._cache[key] = (value, time.time() + ttl)
        logger.debug(f"Cache SET: {key} (ttl={ttl}s)")

    def delete(self, key: str) -> None:
        with self._lock:
            self._cache.pop(key, None)

    def clear(self) -> None:
        with self._lock:
            self._cache.clear()
        logger.info("Cache cleared")

    def size(self) -> int:
        with self._lock:
            return len([k for k, (_, exp) in self._cache.items() if time.time() < exp])

    def stats(self) -> Dict[str, int]:
        with self._lock:
            now = time.time()
            total = len(self._cache)
            alive = sum(1 for _, (_, exp) in self._cache.items() if now < exp)
            return {"total_keys": total, "alive_keys": alive, "expired_keys": total - alive}


cache = TTLCache(default_ttl=600)
