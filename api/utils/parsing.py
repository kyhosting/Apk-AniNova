from bs4 import BeautifulSoup
from dotenv import load_dotenv
from os import getenv
import os
import time
from requests import Session, Response
import logging
from typing import Optional, Dict, Any

load_dotenv()

logger = logging.getLogger(__name__)

REQUEST_TIMEOUT = 15

# ── Runtime cookie override ──────────────────────────────────────────
# These are updated at runtime via the admin /v1/admin/cookies endpoint.
# They take priority over environment variables when set.
_runtime_site_cookies: Optional[str] = None
_runtime_cf_clearance: Optional[str] = None

# Timestamp of last Cloudflare challenge detection (Unix epoch)
_cf_challenge_detected_at: Optional[float] = None


def update_runtime_cookies(site_cookies: str, cf_clearance: str = "") -> None:
    """Update cookies at runtime without restarting the server."""
    global _runtime_site_cookies, _runtime_cf_clearance, _cf_challenge_detected_at
    _runtime_site_cookies = site_cookies.strip() if site_cookies else None
    _runtime_cf_clearance = cf_clearance.strip() if cf_clearance else None
    _cf_challenge_detected_at = None  # reset expiry flag on fresh cookies
    logger.info("Runtime cookies updated via admin endpoint")


def get_cookie_status() -> Dict[str, Any]:
    """Return current cookie source and CF-challenge state."""
    using_runtime = _runtime_site_cookies is not None
    return {
        "source": "runtime" if using_runtime else "environment",
        "has_site_cookies": bool(
            _runtime_site_cookies if using_runtime else os.getenv("SITE_COOKIES", "")
        ),
        "has_cf_clearance": bool(
            _runtime_cf_clearance if using_runtime
            else os.getenv("CF_CLEARANCE", "")
        ),
        "cf_challenge_detected_at": _cf_challenge_detected_at,
        "cf_challenge_detected": _cf_challenge_detected_at is not None,
        "cf_challenge_age_seconds": (
            round(time.time() - _cf_challenge_detected_at, 1)
            if _cf_challenge_detected_at else None
        ),
    }


def _build_cookie_header() -> str:
    """Build cookie header — runtime values take priority over env vars."""
    parts = []

    site_cookies = (
        _runtime_site_cookies
        if _runtime_site_cookies is not None
        else os.getenv("SITE_COOKIES", "")
    )
    if site_cookies:
        parts.append(site_cookies.strip())

    cf_clearance = (
        _runtime_cf_clearance
        if _runtime_cf_clearance is not None
        else os.getenv("CF_CLEARANCE", "")
    )
    if cf_clearance:
        parts.append(f"cf_clearance={cf_clearance}")

    return "; ".join(parts)


def _is_cloudflare_challenge(html: str) -> bool:
    """
    Check if the response is a Cloudflare challenge page.
    Only flags pages that are PURELY challenge pages, not normal pages
    that happen to include CF scripts in their footer.
    """
    hard_indicators = [
        "jschl-answer",
        "_cf_chl_opt",
        "cf-browser-verification",
        "Checking your browser before accessing",
        "Please enable JavaScript and cookies",
        "DDoS protection by Cloudflare",
        "Ray ID" and "Enable JavaScript",
    ]
    if any(ind in html for ind in hard_indicators):
        return True

    if "Just a moment" in html and len(html) < 15000:
        return True

    return False


class Parsing(Session):
    def __init__(self) -> None:
        super().__init__()
        self.url: str = os.getenv("HOST", "https://anichin.moe").rstrip("/")
        self.history_url: Optional[str] = None
        logger.info(f"Initialized Parsing session with URL: {self.url}")

    def __get_html(self, slug: str, **kwargs: Any) -> Optional[str]:
        """Get HTML content from the specified slug."""
        global _cf_challenge_detected_at
        try:
            if slug.startswith("http://") or slug.startswith("https://"):
                url = slug
            elif slug.startswith("/"):
                url = f"{self.url}{slug}"
            else:
                url = f"{self.url}/{slug}"

            headers: Dict[str, str] = {
                "User-Agent": getenv(
                    "USER_AGENT",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                ),
                "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
                "Accept-Language": "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7",
                "Accept-Encoding": "gzip, deflate, br",
                "Referer": self.url + "/",
                "Connection": "keep-alive",
                "Upgrade-Insecure-Requests": "1",
            }

            cookie_header = _build_cookie_header()
            if cookie_header:
                headers["Cookie"] = cookie_header

            if kwargs.get("headers"):
                headers.update(kwargs["headers"])
            kwargs["headers"] = headers

            if "timeout" not in kwargs:
                kwargs["timeout"] = REQUEST_TIMEOUT

            logger.debug(f"Making request to: {url}")
            response: Response = self.get(url, **kwargs)
            response.raise_for_status()

            html = response.text
            if _is_cloudflare_challenge(html):
                _cf_challenge_detected_at = time.time()
                logger.error(
                    f"Cloudflare challenge detected for {url}. "
                    "Update cookies via POST /v1/admin/cookies or "
                    "update SITE_COOKIES / CF_CLEARANCE in Secrets."
                )
                return None

            self.history_url = url
            logger.debug(f"Successfully fetched content from: {url}")
            return html

        except Exception as e:
            logger.error(f"Failed to fetch HTML from {slug}: {e}")
            return None

    def get_parsed_html(self, url: str, **kwargs: Any) -> Optional[BeautifulSoup]:
        """Get parsed HTML content using BeautifulSoup."""
        try:
            html_content = self.__get_html(url, **kwargs)
            if html_content:
                parsed = BeautifulSoup(html_content, "html.parser")
                logger.debug(f"Successfully parsed HTML content for: {url}")
                return parsed
            else:
                logger.warning(f"No HTML content to parse for: {url}")
                return None
        except Exception as e:
            logger.error(f"Failed to parse HTML for {url}: {e}")
            return None

    def parsing(self, data: str) -> Optional[BeautifulSoup]:
        """Parse HTML data using BeautifulSoup."""
        try:
            if not data:
                logger.warning("Empty data provided for parsing")
                return None

            parsed = BeautifulSoup(data, "html.parser")
            logger.debug("Successfully parsed provided HTML data")
            return parsed
        except Exception as e:
            logger.error(f"Failed to parse provided data: {e}")
            return None
