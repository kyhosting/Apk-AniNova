from .parsing import Parsing
from dotenv import load_dotenv
import os
import re
import json
from base64 import b64decode
import logging
from html import unescape
from typing import Dict, List, Optional, Any, Union
from bs4 import BeautifulSoup

load_dotenv()

logger = logging.getLogger(__name__)


class Video(Parsing):
    def __init__(self, slug: str) -> None:
        super().__init__()
        self.slug: str = slug
        logger.info(f"Initialized Video scraper for slug: {slug}")

    def get_details(self) -> Union[Dict[str, Any], bool]:
        try:
            logger.info(f"Starting to fetch video details for slug: {self.slug}")
            data = self.get_parsed_html(self.slug)
            if not data:
                logger.error("Failed to get video page data")
                return False
            return self.__get_all_servers(data)
        except Exception as e:
            logger.error(f"Error in get_details for slug {self.slug}: {e}")
            return False

    def __get_all_servers(self, data: BeautifulSoup) -> Union[Dict[str, Any], bool]:
        try:
            video_select = data.find("select", {"name": "mirror"}) or data.find("select", {"class": "mirror"})
            if not video_select:
                logger.warning("Video select element not found")
                return False

            options = video_select.find_all("option")
            if not options:
                logger.warning("No video options found")
                return False

            servers = []
            for option in options:
                name = option.text.strip()
                value = option.get("value")
                if not value or name == "Pilih Server Video":
                    continue

                server_info = self.__process_server(name, value)
                if server_info:
                    servers.append(server_info)

            if not servers:
                logger.warning("No valid servers found")
                return False

            logger.info(f"Found {len(servers)} servers")
            return {"servers": servers}

        except Exception as e:
            logger.error(f"Error extracting servers: {e}")
            return False

    def __process_server(self, name: str, value: str) -> Optional[Dict[str, Any]]:
        try:
            decoded = b64decode(value).decode("utf-8")
            parsed = self.parsing(decoded)
            if not parsed:
                return None

            iframe = parsed.find("iframe")
            if not iframe or not iframe.get("src"):
                return None

            embed_url = iframe["src"]
            if embed_url.startswith("//"):
                embed_url = "https:" + embed_url

            server_data: Dict[str, Any] = {
                "name": name,
                "embed_url": embed_url,
                "direct_urls": [],
            }

            if "ok.ru" in embed_url:
                video_url = embed_url.replace("videoembed", "video")
                direct = self.__fetch_okru_videos(video_url)
                if direct:
                    server_data["embed_url"] = video_url
                    server_data["direct_urls"] = direct.get("direct_urls", [])
                    server_data["title"] = direct.get("title", "")
                    server_data["thumbnail"] = direct.get("thumbnail", "")

            return server_data

        except Exception as e:
            logger.error(f"Error processing server '{name}': {e}")
            return None

    def __fetch_okru_videos(self, video_url: str) -> Optional[Dict[str, Any]]:
        try:
            user_agent = os.getenv(
                "USER_AGENT",
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36",
            )
            headers = {
                "User-Agent": user_agent,
                "Referer": "https://ok.ru/",
            }

            response = self.get(video_url, headers=headers)
            if response.status_code != 200:
                logger.error(f"OK.ru request failed with status: {response.status_code}")
                return None

            match = re.search(r'data-options="({.*?})"', response.text)
            if not match:
                logger.error("data-options not found in OK.ru page")
                return None

            options_data = json.loads(unescape(match.group(1)))
            flashvars = options_data.get("flashvars", {})
            metadata_str = flashvars.get("metadata")
            if not metadata_str:
                logger.error("metadata not found in OK.ru flashvars")
                return None

            metadata = json.loads(metadata_str)
            movie = metadata.get("movie", {})
            videos = metadata.get("videos", [])

            if not videos:
                return None

            direct_urls = [
                {"quality": v.get("name", "unknown"), "url": v.get("url", "")}
                for v in videos
                if v.get("url")
            ]

            logger.info(f"Fetched {len(direct_urls)} quality levels from OK.ru")
            return {
                "title": movie.get("title", ""),
                "thumbnail": movie.get("poster", options_data.get("poster", "")),
                "direct_urls": direct_urls,
            }

        except Exception as e:
            logger.error(f"Error fetching OK.ru videos: {e}")
            return None


if __name__ == "__main__":
    logging.basicConfig(level=logging.DEBUG, format="%(asctime)s - %(name)s - %(levelname)s - %(message)s")
    video = Video("against-the-gods-episode-41-subtitle-indonesia")
    import json as j
    print(j.dumps(video.get_details(), indent=2))
