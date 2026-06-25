from dotenv import load_dotenv
from .utils.info import Info
from .utils.video import Video
from .utils.episode import Episode
from .utils.home import Home
from .utils.search import Search
from .utils.genre import Genres
from .utils.anime import Anime
import logging
import random as _random
from typing import Dict, List, Optional, Any, Union

load_dotenv()
logger = logging.getLogger(__name__)


class Main:
    def __init__(self) -> None:
        logger.info("Initialized Main API handler")

    def get_info(self, slug: str) -> Dict[str, Any]:
        try:
            return Info(slug).to_json()
        except Exception as e:
            logger.error(f"Error getting info for {slug}: {e}")
            return {"result": None, "error": str(e)}

    def get_video_source(self, slug: str) -> Union[Dict[str, Any], bool]:
        try:
            return Video(slug).get_details()
        except Exception as e:
            logger.error(f"Error getting video source for {slug}: {e}")
            return False

    def get_episode(self, slug: str) -> Dict[str, Any]:
        try:
            return Episode(slug).to_json()
        except Exception as e:
            logger.error(f"Error getting episode for {slug}: {e}")
            return {"result": None, "error": str(e)}

    def get_home(self, page: int = 1) -> Dict[str, Any]:
        try:
            return Home(page).get_details()
        except Exception as e:
            logger.error(f"Error getting home page {page}: {e}")
            return {"results": [], "page": page, "total": 0, "error": str(e)}

    def get_latest(self, page: int = 1) -> Dict[str, Any]:
        try:
            home_data = Home(page).get_details()
            results = home_data.get("results", [])
            latest = next(
                (s["cards"] for s in results if "terbaru" in s.get("section", "").lower()),
                results[0]["cards"] if results else [],
            )
            return {"results": latest, "page": page, "total": len(latest)}
        except Exception as e:
            logger.error(f"Error getting latest: {e}")
            return {"results": [], "page": page, "total": 0, "error": str(e)}

    def get_popular(self, page: int = 1) -> Dict[str, Any]:
        try:
            home_data = Home(page).get_details()
            results = home_data.get("results", [])
            popular = next(
                (s["cards"] for s in results if "populer" in s.get("section", "").lower() or "popular" in s.get("section", "").lower()),
                results[0]["cards"] if results else [],
            )
            return {"results": popular, "page": page, "total": len(popular)}
        except Exception as e:
            logger.error(f"Error getting popular: {e}")
            return {"results": [], "page": page, "total": 0, "error": str(e)}

    def get_trending(self, page: int = 1) -> Dict[str, Any]:
        try:
            home_data = Home(page).get_details()
            results = home_data.get("results", [])
            trending: List[Dict[str, Any]] = []
            for section in results:
                trending.extend(section.get("cards", []))
            seen = set()
            unique: List[Dict[str, Any]] = []
            for item in trending:
                slug = item.get("slug")
                if slug and slug not in seen:
                    seen.add(slug)
                    unique.append(item)
            return {"results": unique[:20], "page": page, "total": min(len(unique), 20)}
        except Exception as e:
            logger.error(f"Error getting trending: {e}")
            return {"results": [], "page": page, "total": 0, "error": str(e)}

    def get_random(self) -> Dict[str, Any]:
        try:
            anime_data = Anime().get_details()
            items = anime_data.get("results", [])
            if not items:
                return {"result": None, "error": "No anime found"}
            picked = _random.choice(items)
            return {"result": picked}
        except Exception as e:
            logger.error(f"Error getting random anime: {e}")
            return {"result": None, "error": str(e)}

    def get_ongoing(self, page: int = 1) -> Dict[str, Any]:
        try:
            data = Anime().get_details(params={"status": "ongoing", "page": str(page)})
            results = data.get("results", [])
            return {"results": results, "page": page, "total": len(results)}
        except Exception as e:
            logger.error(f"Error getting ongoing: {e}")
            return {"results": [], "page": page, "total": 0, "error": str(e)}

    def get_completed(self, page: int = 1) -> Dict[str, Any]:
        try:
            data = Anime().get_details(params={"status": "completed", "page": str(page)})
            results = data.get("results", [])
            return {"results": results, "page": page, "total": len(results)}
        except Exception as e:
            logger.error(f"Error getting completed: {e}")
            return {"results": [], "page": page, "total": 0, "error": str(e)}

    def get_episode_navigation(self, slug: str) -> Dict[str, Any]:
        try:
            episode_data = Episode(slug).to_json()
            if not episode_data or not episode_data.get("result"):
                return {"next": None, "previous": None, "current": slug, "error": "Episode not found"}
            episodes: List[Dict[str, Any]] = episode_data["result"].get("episode", [])
            current_index = next(
                (i for i, ep in enumerate(episodes) if ep.get("slug") == slug), None
            )
            if current_index is None:
                return {"next": None, "previous": None, "current": slug}
            next_ep = episodes[current_index - 1] if current_index > 0 else None
            prev_ep = episodes[current_index + 1] if current_index < len(episodes) - 1 else None
            return {
                "current": slug,
                "next": {"slug": next_ep["slug"], "episode": next_ep.get("episode"), "name": next_ep.get("name")} if next_ep else None,
                "previous": {"slug": prev_ep["slug"], "episode": prev_ep.get("episode"), "name": prev_ep.get("name")} if prev_ep else None,
            }
        except Exception as e:
            logger.error(f"Error getting episode navigation for {slug}: {e}")
            return {"next": None, "previous": None, "current": slug, "error": str(e)}

    def search(self, query: str) -> Dict[str, Any]:
        try:
            return Search(query).get_details()
        except Exception as e:
            logger.error(f"Error searching for {query}: {e}")
            return {"results": [], "query": query, "total": 0, "error": str(e)}

    def genres(self, genre: Optional[str] = None, page: int = 1) -> Dict[str, Any]:
        try:
            genres_handler = Genres()
            if not genre:
                return genres_handler.list_genre()
            return genres_handler.get_genre(genre, page)
        except Exception as e:
            if genre:
                return {"results": [], "slug": genre, "page": page, "total": 0, "error": str(e)}
            return {"genres": [], "total": 0, "error": str(e)}

    def anime(self, **kwargs: Any) -> Dict[str, Any]:
        try:
            return Anime().get_details(**kwargs)
        except Exception as e:
            logger.error(f"Error getting anime list: {e}")
            return {"results": [], "total": 0, "error": str(e)}
