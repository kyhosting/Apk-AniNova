import os
import sqlite3
import logging
from typing import Optional, List, Dict, Any

logger = logging.getLogger(__name__)

DB_PATH = os.path.join(os.path.dirname(__file__), "..", "..", "anichin.db")


def get_conn() -> sqlite3.Connection:
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA journal_mode=WAL")
    conn.execute("PRAGMA foreign_keys=ON")
    return conn


def init_db():
    sql = """
    CREATE TABLE IF NOT EXISTS users (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        username TEXT UNIQUE NOT NULL,
        email TEXT UNIQUE NOT NULL,
        password_hash TEXT NOT NULL,
        role TEXT NOT NULL DEFAULT 'user',
        avatar TEXT NOT NULL DEFAULT '<i class="fa-solid fa-dragon"></i>',
        created_at TEXT DEFAULT (datetime('now')),
        last_login TEXT,
        is_active INTEGER DEFAULT 1
    );
    CREATE TABLE IF NOT EXISTS watchlist (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
        anime_slug TEXT NOT NULL,
        anime_title TEXT,
        anime_thumbnail TEXT,
        added_at TEXT DEFAULT (datetime('now')),
        UNIQUE(user_id, anime_slug)
    );
    CREATE TABLE IF NOT EXISTS watch_history (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
        anime_slug TEXT NOT NULL,
        episode_slug TEXT NOT NULL,
        episode_number TEXT,
        anime_title TEXT,
        episode_title TEXT,
        thumbnail TEXT,
        watched_at TEXT DEFAULT (datetime('now')),
        UNIQUE(user_id, episode_slug)
    );
    CREATE INDEX IF NOT EXISTS idx_watchlist_user ON watchlist(user_id);
    CREATE INDEX IF NOT EXISTS idx_history_user ON watch_history(user_id);
    CREATE INDEX IF NOT EXISTS idx_history_watched ON watch_history(watched_at DESC);
    CREATE TABLE IF NOT EXISTS otp_requests (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        email TEXT NOT NULL,
        username TEXT NOT NULL,
        password_hash TEXT NOT NULL,
        otp_code TEXT NOT NULL,
        expires_at TEXT NOT NULL,
        created_at TEXT DEFAULT (datetime('now'))
    );
    CREATE INDEX IF NOT EXISTS idx_otp_email ON otp_requests(email);
    CREATE TABLE IF NOT EXISTS comments (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        anime_slug TEXT NOT NULL,
        user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
        content TEXT NOT NULL,
        created_at TEXT DEFAULT (datetime('now', 'localtime'))
    );
    CREATE INDEX IF NOT EXISTS idx_comments_slug ON comments(anime_slug);
    CREATE TABLE IF NOT EXISTS views (
        slug TEXT PRIMARY KEY,
        count INTEGER DEFAULT 1,
        updated_at TEXT DEFAULT (datetime('now', 'localtime'))
    );
    CREATE TABLE IF NOT EXISTS likes (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
        slug TEXT NOT NULL,
        created_at TEXT DEFAULT (datetime('now', 'localtime')),
        UNIQUE(user_id, slug)
    );
    CREATE INDEX IF NOT EXISTS idx_likes_slug ON likes(slug);
    CREATE TABLE IF NOT EXISTS notifications (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
        type TEXT NOT NULL DEFAULT 'new_episode',
        title TEXT NOT NULL,
        body TEXT,
        slug TEXT,
        thumbnail TEXT,
        is_read INTEGER DEFAULT 0,
        created_at TEXT DEFAULT (datetime('now', 'localtime'))
    );
    CREATE INDEX IF NOT EXISTS idx_notif_user ON notifications(user_id, is_read);
    CREATE TABLE IF NOT EXISTS notified_episodes (
        slug TEXT PRIMARY KEY,
        notified_at TEXT DEFAULT (datetime('now', 'localtime'))
    );
    """
    try:
        conn = get_conn()
        conn.executescript(sql)
        conn.commit()
        conn.close()
        logger.info("SQLite schema initialized")
    except Exception as e:
        logger.error(f"DB init error: {e}")
        raise


def query(sql: str, params=()) -> List[Dict[str, Any]]:
    try:
        conn = get_conn()
        cur = conn.execute(sql, params)
        rows = [dict(r) for r in cur.fetchall()]
        conn.close()
        return rows
    except Exception as e:
        logger.error(f"DB query error: {e}")
        return []


def execute(sql: str, params=()) -> Optional[Dict[str, Any]]:
    try:
        conn = get_conn()
        cur = conn.execute(sql, params)
        row = cur.fetchone()
        conn.commit()
        conn.close()
        return dict(row) if row else None
    except Exception as e:
        logger.error(f"DB execute error: {e}")
        return None


def execute_no_return(sql: str, params=()) -> bool:
    try:
        conn = get_conn()
        conn.execute(sql, params)
        conn.commit()
        conn.close()
        return True
    except Exception as e:
        logger.error(f"DB execute error: {e}")
        return False
