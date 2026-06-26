import logging
import os
import sys
import time
import uuid
from datetime import datetime
from typing import Text, Dict, Any, Tuple

import flask.cli
from flask import Flask, jsonify, request, g, render_template
from flask_cors import CORS
try:
    from flask_compress import Compress
    _has_compress = True
except Exception:
    _has_compress = False
try:
    from flasgger import Swagger
    _has_swagger = True
except Exception:
    _has_swagger = False
from flask_limiter import Limiter
from flask_limiter.util import get_remote_address

from api import Main
from api.utils.cache import cache
from api.utils.parsing import update_runtime_cookies, get_cookie_status
from api.utils.response import (
    success, error as err_response,
    get_stats, DEVELOPER, API_VERSION,
)
from api.utils import db as db_utils
from api.utils.auth import (
    get_current_user, login_required, admin_required,
    register_user, login_user, send_otp, verify_otp_and_register,
)

# Suppress Flask's built-in "* Serving Flask app" / "* Debug mode: on" click output
flask.cli.show_server_banner = lambda *a, **kw: None

# True only in the reloader child — prevents duplicate output on first spawn
_IS_CHILD = os.environ.get("WERKZEUG_RUN_MAIN") == "true"

# ── COLOR PALETTE (256-color, soft & professional) ───────────────────
def _c(n: int) -> str:
    return f"\033[38;5;{n}m"

class C:
    RESET   = "\033[0m"
    BOLD    = "\033[1m"
    DIM     = "\033[2m"

    # structural chrome
    BORDER  = _c(60)    # muted slate-blue  — box borders
    TITLE   = _c(183)   # soft lavender     — banner title
    LABEL   = _c(242)   # medium-dark gray  — dim labels
    MUTED   = _c(238)   # dark gray         — timestamps / ids
    TEXT    = _c(252)   # light gray        — general text

    # log levels
    INFO    = _c(114)   # sage green
    WARN    = _c(179)   # warm amber
    ERROR   = _c(167)   # dusty rose-red
    CRIT    = _c(197)   # vivid red (reserved for critical only)
    DEBUG   = _c(73)    # muted teal

    # http methods
    M_GET    = _c(75)   # sky blue
    M_POST   = _c(114)  # sage green
    M_PUT    = _c(179)  # warm amber
    M_DELETE = _c(167)  # dusty rose
    M_PATCH  = _c(140)  # soft purple

    # status codes
    S_2XX   = _c(114)   # sage green
    S_3XX   = _c(73)    # muted teal
    S_4XX   = _c(179)   # warm amber
    S_5XX   = _c(167)   # dusty rose

    # latency
    MS_OK   = _c(114)   # sage green   < 500 ms
    MS_SLOW = _c(179)   # warm amber   500–2000 ms
    MS_BAD  = _c(167)   # dusty rose   > 2000 ms

    # extras
    VERSION = _c(110)   # steel blue
    PORT    = _c(179)   # warm amber
    DOCS    = _c(68)    # muted cornflower


class ColoredFormatter(logging.Formatter):
    LEVEL_MAP = {
        "DEBUG":    (C.DEBUG,  "DBG"),
        "INFO":     (C.INFO,   "INF"),
        "WARNING":  (C.WARN,   "WRN"),
        "ERROR":    (C.ERROR,  "ERR"),
        "CRITICAL": (C.CRIT,   "CRT"),
    }
    NAME_COLOR = {
        "__main__": _c(140),
        "werkzeug":  C.MUTED,
        "api":       _c(73),
    }

    def format(self, record):
        color, tag = self.LEVEL_MAP.get(record.levelname, (C.TEXT, record.levelname[:3]))
        level_tag = f"{color}{C.BOLD}{tag}{C.RESET}"

        name = record.name.split(".")[-1]
        name_col = self.NAME_COLOR.get(record.name, C.VERSION)
        name_tag = f"{name_col}{name:<10}{C.RESET}"

        ts = datetime.fromtimestamp(record.created).strftime("%H:%M:%S")
        time_tag = f"{C.MUTED}{ts}{C.RESET}"

        sep = f"{C.LABEL}│{C.RESET}"

        return f"  {time_tag}  {sep}  {level_tag}  {sep}  {name_tag}  {C.TEXT}{record.getMessage()}{C.RESET}"


def _build_logger():
    root = logging.getLogger()
    root.setLevel(logging.INFO)
    root.handlers.clear()

    console = logging.StreamHandler(sys.stdout)
    console.setFormatter(ColoredFormatter())
    # Parent process (pre-reload): mute all console output to avoid duplicate noise
    console.setLevel(logging.INFO if _IS_CHILD else logging.CRITICAL)

    file_handler = logging.FileHandler("aninova_api.log")
    file_handler.setFormatter(logging.Formatter(
        "%(asctime)s  %(levelname)-8s  %(name)-20s  %(message)s"
    ))

    root.addHandler(console)
    root.addHandler(file_handler)

    # Silence werkzeug's "* Debugger is active!" noise
    logging.getLogger("werkzeug").setLevel(logging.ERROR)

_build_logger()
logger = logging.getLogger(__name__)


def _print_banner():
    if not _IS_CHILD:
        return
    w   = 52
    br  = C.BORDER
    R   = C.RESET

    top   = f"  {br}╭{'─' * w}╮{R}"
    bot   = f"  {br}╰{'─' * w}╯{R}"
    div   = f"  {br}├{'─' * w}┤{R}"
    pipe  = f"{br}│{R}"

    def line(inner: str = "") -> str:
        pad = w - len(inner)
        return f"  {pipe}{inner}{' ' * max(pad, 0)}{pipe}"

    def row(label: str, value: str, val_col: str = C.TEXT) -> str:
        lbl = f"  {C.LABEL}{label:<12}{R}"
        val = f"{val_col}{value}{R}"
        raw_len = 2 + 12 + len(value)
        pad = w - raw_len
        return f"  {pipe}{lbl}{val}{' ' * max(pad, 0)}{pipe}"

    title_str  = "AniNova  API"
    title_pad  = (w - len(title_str)) // 2
    title_line = f"  {pipe}{' ' * title_pad}{C.TITLE}{C.BOLD}{title_str}{R}{' ' * (w - title_pad - len(title_str))}{pipe}"

    print(f"\n{top}")
    print(line())
    print(title_line)
    print(line())
    print(div)
    print(row("Developer",  DEVELOPER,                                            C.TEXT))
    print(row("Version",    f"v{API_VERSION}",                                    C.VERSION))
    print(row("Started",    datetime.now().strftime("%Y-%m-%d  %H:%M:%S"),        C.TEXT))
    print(row("Port",       "5000",                                                C.PORT))
    print(row("Frontend",   "http://localhost:5000",                               C.DOCS))
    print(row("Docs",       "http://localhost:5000/docs",                          C.DOCS))
    print(line())
    print(bot)
    print()

_print_banner()

app = Flask(__name__)
app.secret_key = os.environ.get("SECRET_KEY", "aninova-flask-secret-2025")
main = Main()
START_TIME = time.time()

CORS(app)
if _has_compress:
    Compress(app)

limiter = Limiter(
    get_remote_address,
    app=app,
    default_limits=["300 per day", "60 per hour"],
    storage_uri="memory://",
)

swagger_config = {
    "headers": [],
    "specs": [{"endpoint": "apispec", "route": "/apispec.json", "rule_filter": lambda rule: True, "model_filter": lambda tag: True}],
    "static_url_path": "/flasgger_static",
    "swagger_ui": True,
    "specs_route": "/docs",
}
swagger_template = {
    "swagger": "2.0",
    "info": {
        "title": "AniNova API",
        "description": f"REST API scraping anime/donghua dari AniNova.\n\n**Developer:** {DEVELOPER}\n\n**Version:** {API_VERSION}",
        "version": API_VERSION,
        "contact": {"name": DEVELOPER},
    },
    "basePath": "/",
    "schemes": ["https", "http"],
    "tags": [
        {"name": "General", "description": "Status & statistik API"},
        {"name": "Home", "description": "Konten utama, latest, popular, trending"},
        {"name": "Anime", "description": "Info & daftar anime"},
        {"name": "Episode", "description": "Detail episode & navigasi"},
        {"name": "Video", "description": "Sumber video multi-server"},
        {"name": "Search", "description": "Pencarian anime"},
        {"name": "Genre", "description": "Genre anime"},
    ],
}
if _has_swagger:
    Swagger(app, config=swagger_config, template=swagger_template)


# ── MIDDLEWARE ──────────────────────────────────────────────────────
@app.before_request
def before_request():
    g.start_time = time.time()
    g.request_id = str(uuid.uuid4())[:8]


@app.after_request
def after_request(response):
    response.headers["X-Request-ID"] = getattr(g, "request_id", "-")
    response.headers["X-Developer"] = DEVELOPER
    response.headers["X-API-Version"] = API_VERSION
    exec_ms = round((time.time() - getattr(g, "start_time", time.time())) * 1000, 2)
    response.headers["X-Execution-Time-Ms"] = str(exec_ms)

    status = response.status_code
    if status < 300:
        status_color = C.S_2XX
    elif status < 400:
        status_color = C.S_3XX
    elif status < 500:
        status_color = C.S_4XX
    else:
        status_color = C.S_5XX

    method = request.method
    method_colors = {
        "GET":    C.M_GET,
        "POST":   C.M_POST,
        "PUT":    C.M_PUT,
        "DELETE": C.M_DELETE,
        "PATCH":  C.M_PATCH,
    }
    method_color = method_colors.get(method, C.TEXT)

    ms_color = C.MS_OK if exec_ms < 500 else C.MS_SLOW if exec_ms < 2000 else C.MS_BAD
    rid = getattr(g, "request_id", "--------")
    sep = f"{C.LABEL}│{C.RESET}"

    path = request.path
    if not path.startswith("/flasgger") and not path.startswith("/apispec"):
        print(
            f"  {C.MUTED}{datetime.now().strftime('%H:%M:%S')}{C.RESET}"
            f"  {sep}"
            f"  {method_color}{C.BOLD}{method:<7}{C.RESET}"
            f"  {sep}"
            f"  {status_color}{C.BOLD}{status}{C.RESET}"
            f"  {C.TEXT}{path}{C.RESET}"
            f"  {ms_color}{exec_ms}ms{C.RESET}"
            f"  {C.MUTED}#{rid}{C.RESET}"
        )

    return response


# ── INIT DATABASE ────────────────────────────────────────────────────
try:
    db_utils.init_db()
except Exception as _db_err:
    logger.error(f"DB init failed: {_db_err}")


# ── ENSURE ADMIN ACCOUNT ─────────────────────────────────────────────
def ensure_admin():
    """Create admin account if it does not exist."""
    try:
        from api.utils.auth import hash_password
        existing = db_utils.query(
            "SELECT id FROM users WHERE email = ?",
            ("kikimodesad8@gmail.com",)
        )
        if not existing:
            hashed = hash_password("@KIKIASU28")
            db_utils.execute_no_return(
                "INSERT INTO users (username, email, password_hash, role, avatar) VALUES (?, ?, ?, ?, ?)",
                ("KikiAdmin", "kikimodesad8@gmail.com", hashed, "admin",
                 '<i class="fa-solid fa-crown"></i>')
            )
            logger.info("Admin account created: kikimodesad8@gmail.com")
        else:
            # Ensure role is admin and password is correct
            hashed = hash_password("@KIKIASU28")
            db_utils.execute_no_return(
                "UPDATE users SET role = 'admin', username = 'KikiAdmin', password_hash = ? WHERE email = ?",
                (hashed, "kikimodesad8@gmail.com",)
            )
    except Exception as e:
        logger.error(f"ensure_admin failed: {e}")


ensure_admin()


# ── BACKGROUND: CEK EPISODE BARU ─────────────────────────────────────
import threading

def _episode_checker_loop():
    import time as _time
    _time.sleep(30)
    while True:
        try:
            result = main.get_latest(1)
            episodes = result.get("results", [])[:15]
            for ep in episodes:
                slug = ep.get("slug") or ep.get("episode_slug")
                if not slug:
                    continue
                existing = db_utils.query(
                    "SELECT slug FROM notified_episodes WHERE slug = ?", (slug,)
                )
                if existing:
                    continue
                db_utils.execute_no_return(
                    "INSERT OR IGNORE INTO notified_episodes (slug) VALUES (?)", (slug,)
                )
                title = ep.get("title") or ep.get("name") or "Episode Baru"
                thumb = ep.get("thumbnail") or ep.get("image") or ""
                ep_info = ep.get("episode") or ep.get("episode_number") or ""
                body = f"Episode {ep_info} sudah tersedia!" if ep_info else "Sudah tersedia!"
                users = db_utils.query("SELECT id FROM users WHERE is_active = 1")
                for u in users:
                    db_utils.execute_no_return(
                        """INSERT INTO notifications (user_id, type, title, body, slug, thumbnail)
                           VALUES (?, 'new_episode', ?, ?, ?, ?)""",
                        (u["id"], title, body, slug, thumb)
                    )
                logger.info(f"Notif dibuat untuk episode baru: {slug}")
        except Exception as e:
            logger.error(f"Episode checker error: {e}")
        _time.sleep(300)


_checker_thread = threading.Thread(target=_episode_checker_loop, daemon=True, name="ep-checker")
_checker_thread.start()


# ── ANTI-SCRAPING ────────────────────────────────────────────────────
_BLOCKED_UA = [
    'python-requests', 'python-urllib', 'curl/', 'wget/', 'scrapy',
    'httpie', 'go-http-client', 'java/', 'libwww', 'lwp-trivial',
    'mechanize', 'phantomjs', 'headless', 'selenium', 'playwright',
    'puppeteer', 'axios/', 'node-fetch', 'got/', 'superagent',
    'insomnia', 'postman', 'httpx', 'aiohttp', 'okhttp', 'apachehttpclient',
]

_NON_CHROME_UA = [
    'firefox/', 'fxios/',
    'opr/', 'opera/',
    'samsungbrowser/',
    'yabrowser/',
    'ucbrowser/',
    'vivaldi/',
    'brave/',
    'duckduckgo/',
    'miuibrowser/',
    'edg/', 'edge/',
]

_BLOCK_HTML = """<!DOCTYPE html>
<html><head><meta charset="UTF-8"><title>NOT FOUND</title>
<style>
*{margin:0;padding:0;box-sizing:border-box}
body{background:#000;color:#fff;display:flex;align-items:center;justify-content:center;min-height:100vh;font-family:'Courier New',monospace;flex-direction:column;gap:16px;text-align:center;padding:20px}
.title{font-size:1.6rem;font-weight:bold;color:#e50914;letter-spacing:4px}
.msg{font-size:1rem;letter-spacing:2px}
.sub{color:#444;font-size:.8rem;letter-spacing:3px}
</style>
<link rel="stylesheet" href="/static/css/fa.all.min.css">
</head><body>
<i class="fa-solid fa-ban" style="font-size:5rem;color:#e50914"></i>
<div class="title">NOT FOUND</div>
<div class="msg">JANGAN CURI, MENCOBA DEBUGGING</div>
<div class="sub">SUPPORT BY KICEN XENSAI</div>
</body></html>"""


def _is_blocked():
    ua = request.headers.get('User-Agent', '').lower()
    if not ua:
        return True
    if any(b in ua for b in _BLOCKED_UA):
        return True
    if any(b in ua for b in _NON_CHROME_UA):
        return True
    if 'chrome/' not in ua and 'crios/' not in ua:
        return True
    return False


@app.before_request
def block_non_chrome():
    skip_paths = ('/docs', '/apispec', '/flasgger', '/static', '/robots.txt', '/favicon', '/v1/')
    if any(request.path.startswith(p) for p in skip_paths):
        return
    if _is_blocked():
        return _BLOCK_HTML, 403, {'Content-Type': 'text/html'}


# ── PAGE ROUTES ───────────────────────────────────────────────────────
@app.get('/')
def page_landing():
    return render_template('index.html')


@app.get('/login')
@app.get('/register')
@app.get('/daftar')
def page_auth():
    return render_template('auth.html')


@app.get('/app')
@app.get('/app/<path:subpath>')
def page_app(subpath=''):
    return render_template('app.html')


# ── AUTH API ────────────────────────────────────────────────────────
@app.post("/v1/auth/send-otp")
@limiter.limit("5 per minute")
def api_send_otp():
    """
    Kirim kode OTP ke email untuk pendaftaran
    ---
    tags: [Auth]
    parameters:
      - in: body
        name: body
        schema:
          type: object
          required: [email, username, password]
          properties:
            email: {type: string}
            username: {type: string}
            password: {type: string}
    responses:
      200:
        description: OTP dikirim
      400:
        description: Validasi gagal
    """
    data = request.get_json(silent=True) or {}
    result = send_otp(
        data.get("email", ""),
        data.get("username", ""),
        data.get("password", ""),
    )
    if "error" in result:
        return jsonify({"status": "error", "message": result["error"]}), 400
    resp_data = {"status": "ok", "message": "Kode OTP dikirim ke email kamu"}
    if "dev_otp" in result:
        resp_data["dev_otp"] = result["dev_otp"]
        resp_data["message"] = result.get("message", "Dev mode: OTP ada di response")
    return jsonify(resp_data), 200


@app.post("/v1/auth/register")
@limiter.limit("5 per minute")
def api_register():
    """
    Daftar akun baru (memerlukan OTP dari /v1/auth/send-otp)
    ---
    tags: [Auth]
    parameters:
      - in: body
        name: body
        schema:
          type: object
          required: [email, otp_code]
          properties:
            email: {type: string}
            otp_code: {type: string}
    responses:
      201:
        description: Akun berhasil dibuat
      400:
        description: OTP salah atau kedaluwarsa
    """
    data = request.get_json(silent=True) or {}
    otp_code = data.get("otp_code", "").strip()
    email = data.get("email", "").strip()
    if not otp_code or not email:
        return jsonify({"status": "error", "message": "email dan otp_code diperlukan"}), 400
    result = verify_otp_and_register(email, otp_code)
    if "error" in result:
        return jsonify({"status": "error", "message": result["error"]}), 400
    resp = jsonify({"status": "ok", "user": result["user"], "token": result["token"]})
    resp.set_cookie("token", result["token"], httponly=True, samesite="Lax", max_age=86400 * 30)
    return resp, 201


@app.post("/v1/auth/login")
@limiter.limit("10 per minute")
def api_login():
    data = request.get_json(silent=True) or {}
    result = login_user(data.get("login", ""), data.get("password", ""))
    if "error" in result:
        return jsonify({"status": "error", "message": result["error"]}), 401
    resp = jsonify({"status": "ok", "user": result["user"], "token": result["token"]})
    resp.set_cookie("token", result["token"], httponly=True, samesite="Lax", max_age=86400 * 30)
    return resp, 200


@app.post("/v1/auth/logout")
def api_logout():
    resp = jsonify({"status": "ok", "message": "Berhasil logout"})
    resp.delete_cookie("token")
    return resp, 200


@app.get("/v1/auth/me")
def api_me():
    user = get_current_user()
    if not user:
        return jsonify({"status": "error", "message": "Tidak terautentikasi"}), 401
    return jsonify({"status": "ok", "data": user}), 200


# ── USER: WATCHLIST ─────────────────────────────────────────────────
@app.get("/v1/user/watchlist")
def api_get_watchlist():
    user = get_current_user()
    if not user:
        return jsonify({"status": "error", "message": "Login diperlukan"}), 401
    rows = db_utils.query(
        "SELECT * FROM watchlist WHERE user_id = ? ORDER BY added_at DESC",
        (user["id"],),
    )
    return jsonify({"status": "ok", "data": {"watchlist": rows}}), 200


@app.post("/v1/user/watchlist")
def api_add_watchlist():
    user = get_current_user()
    if not user:
        return jsonify({"status": "error", "message": "Login diperlukan"}), 401
    data = request.get_json(silent=True) or {}
    slug = data.get("anime_slug", "").strip()
    if not slug:
        return jsonify({"status": "error", "message": "anime_slug diperlukan"}), 400
    db_utils.execute_no_return(
        "INSERT OR IGNORE INTO watchlist (user_id, anime_slug, anime_title, anime_thumbnail) VALUES (?, ?, ?, ?)",
        (user["id"], slug, data.get("anime_title", ""), data.get("anime_thumbnail", "")),
    )
    return jsonify({"status": "ok", "message": "Ditambahkan ke watchlist"}), 200


@app.delete("/v1/user/watchlist")
def api_remove_watchlist():
    user = get_current_user()
    if not user:
        return jsonify({"status": "error", "message": "Login diperlukan"}), 401
    data = request.get_json(silent=True) or {}
    slug = data.get("anime_slug", "").strip()
    if not slug:
        return jsonify({"status": "error", "message": "anime_slug diperlukan"}), 400
    db_utils.execute_no_return(
        "DELETE FROM watchlist WHERE user_id = ? AND anime_slug = ?",
        (user["id"], slug),
    )
    return jsonify({"status": "ok", "message": "Dihapus dari watchlist"}), 200


# ── USER: HISTORY ───────────────────────────────────────────────────
@app.get("/v1/user/history")
def api_get_history():
    user = get_current_user()
    if not user:
        return jsonify({"status": "error", "message": "Login diperlukan"}), 401
    rows = db_utils.query(
        "SELECT * FROM watch_history WHERE user_id = ? ORDER BY watched_at DESC LIMIT 100",
        (user["id"],),
    )
    return jsonify({"status": "ok", "data": {"history": rows}}), 200


@app.post("/v1/user/history")
def api_add_history():
    user = get_current_user()
    if not user:
        return jsonify({"status": "error", "message": "Login diperlukan"}), 401
    data = request.get_json(silent=True) or {}
    ep_slug = data.get("episode_slug", "").strip()
    anime_slug = data.get("anime_slug", "").strip()
    if not ep_slug:
        return jsonify({"status": "error", "message": "episode_slug diperlukan"}), 400
    db_utils.execute_no_return(
        """INSERT INTO watch_history
           (user_id, anime_slug, episode_slug, episode_number, anime_title, episode_title, thumbnail, watched_at)
           VALUES (?, ?, ?, ?, ?, ?, ?, datetime('now'))
           ON CONFLICT(user_id, episode_slug) DO UPDATE SET watched_at = datetime('now')""",
        (
            user["id"], anime_slug, ep_slug,
            data.get("episode_number", ""),
            data.get("anime_title", ""),
            data.get("episode_title", ""),
            data.get("thumbnail", ""),
        ),
    )
    return jsonify({"status": "ok", "message": "Riwayat disimpan"}), 200


# ── USER: CHANGE PASSWORD ───────────────────────────────────────────
@app.post("/v1/user/change-password")
def api_change_password():
    from api.utils.auth import check_password, hash_password
    user = get_current_user()
    if not user:
        return jsonify({"status": "error", "message": "Login diperlukan"}), 401
    data = request.get_json(silent=True) or {}
    old_pw = data.get("old_password", "")
    new_pw = data.get("new_password", "")
    if not old_pw or not new_pw:
        return jsonify({"status": "error", "message": "Password lama dan baru diperlukan"}), 400
    if len(new_pw) < 6:
        return jsonify({"status": "error", "message": "Password baru minimal 6 karakter"}), 400
    row = db_utils.query("SELECT password_hash FROM users WHERE id = ?", (user["id"],))
    if not row:
        return jsonify({"status": "error", "message": "User tidak ditemukan"}), 404
    if not check_password(old_pw, row[0]["password_hash"]):
        return jsonify({"status": "error", "message": "Password lama salah"}), 400
    new_hash = hash_password(new_pw)
    db_utils.execute_no_return("UPDATE users SET password_hash = ? WHERE id = ?", (new_hash, user["id"]))
    return jsonify({"status": "ok", "message": "Password berhasil diubah"}), 200


# ── USER: UPDATE AVATAR ─────────────────────────────────────────────
@app.post("/v1/user/avatar")
def api_update_avatar():
    user = get_current_user()
    if not user:
        return jsonify({"status": "error", "message": "Login diperlukan"}), 401
    data = request.get_json(silent=True) or {}
    avatar = data.get("avatar", "")
    if not avatar:
        return jsonify({"status": "error", "message": "Avatar diperlukan"}), 400
    db_utils.execute_no_return("UPDATE users SET avatar = ? WHERE id = ?", (avatar, user["id"]))
    return jsonify({"status": "ok", "message": "Avatar berhasil diubah"}), 200


# ── ADMIN API ───────────────────────────────────────────────────────
@app.get("/v1/admin/stats")
@admin_required
def api_admin_stats():
    stats = {
        "total_users": (db_utils.query("SELECT COUNT(*) AS c FROM users") or [{}])[0].get("c", 0),
        "active_users": (db_utils.query("SELECT COUNT(*) AS c FROM users WHERE is_active = 1") or [{}])[0].get("c", 0),
        "total_watchlist": (db_utils.query("SELECT COUNT(*) AS c FROM watchlist") or [{}])[0].get("c", 0),
        "total_history": (db_utils.query("SELECT COUNT(*) AS c FROM watch_history") or [{}])[0].get("c", 0),
    }
    return jsonify({"status": "ok", "data": stats}), 200


@app.get("/v1/admin/users")
@admin_required
def api_admin_users():
    users = db_utils.query(
        "SELECT id, username, email, role, avatar, is_active, created_at, last_login FROM users ORDER BY created_at DESC"
    )
    return jsonify({"status": "ok", "data": {"users": users}}), 200


@app.post("/v1/admin/users/<int:uid>/toggle")
@admin_required
def api_admin_toggle_user(uid: int):
    row = db_utils.query("SELECT is_active FROM users WHERE id = ?", (uid,))
    if not row:
        return jsonify({"status": "error", "message": "User tidak ditemukan"}), 404
    new_state = 0 if row[0]["is_active"] else 1
    db_utils.execute_no_return("UPDATE users SET is_active = ? WHERE id = ?", (new_state, uid))
    return jsonify({"status": "ok", "message": f"User {'diaktifkan' if new_state else 'dinonaktifkan'}"}), 200


@app.post("/v1/admin/users/<int:uid>/role")
@admin_required
def api_admin_change_role(uid: int):
    data = request.get_json(silent=True) or {}
    role = data.get("role", "user")
    if role not in ("user", "admin"):
        return jsonify({"status": "error", "message": "Role tidak valid"}), 400
    db_utils.execute_no_return("UPDATE users SET role = ? WHERE id = ?", (role, uid))
    return jsonify({"status": "ok", "message": f"Role diubah ke {role}"}), 200


@app.delete("/v1/admin/cache")
@admin_required
def api_admin_clear_cache():
    size_before = cache.size()
    cache.clear()
    return jsonify({"status": "ok", "message": f"Cache dibersihkan ({size_before} entri dihapus)"}), 200


@app.get("/v1/admin/cookies")
@admin_required
def api_admin_cookies_status():
    """
    Status cookies scraper saat ini
    ---
    tags: [Admin]
    responses:
      200:
        description: Status cookies
    """
    status = get_cookie_status()
    return jsonify({"status": "ok", "data": status}), 200


@app.post("/v1/admin/cookies")
@admin_required
def api_admin_cookies_update():
    """
    Update cookies scraper tanpa restart server
    ---
    tags: [Admin]
    parameters:
      - in: body
        name: body
        schema:
          type: object
          required: [site_cookies]
          properties:
            site_cookies:
              type: string
              description: Full cookie string (semua cookie kecuali cf_clearance)
            cf_clearance:
              type: string
              description: Nilai cf_clearance saja (opsional)
    responses:
      200:
        description: Cookies berhasil diperbarui
      400:
        description: site_cookies diperlukan
    """
    data = request.get_json(silent=True) or {}
    site_cookies = data.get("site_cookies", "").strip()
    cf_clearance = data.get("cf_clearance", "").strip()

    if not site_cookies:
        return jsonify({"status": "error", "message": "site_cookies diperlukan"}), 400

    update_runtime_cookies(site_cookies, cf_clearance)
    cache.clear()  # clear cache so next requests use fresh cookies
    logger.info("Admin updated runtime cookies and cleared cache")

    return jsonify({
        "status": "ok",
        "message": "Cookies diperbarui. Cache dibersihkan. Request berikutnya akan pakai cookies baru.",
        "data": get_cookie_status(),
    }), 200


# ── GENERAL ─────────────────────────────────────────────────────────
@app.get("/v1/health")
@limiter.exempt
def health_check():
    """
    Status server — uptime, cache, versi
    ---
    tags: [General]
    responses:
      200:
        description: Server berjalan normal
    """
    uptime_sec = int(time.time() - START_TIME)
    h, rem = divmod(uptime_sec, 3600)
    m, s = divmod(rem, 60)
    return success({
        "status": "ok",
        "uptime": f"{h}h {m}m {s}s",
        "uptime_seconds": uptime_sec,
        "started_at": datetime.fromtimestamp(START_TIME).isoformat(),
        "cache": cache.stats(),
        "developer": DEVELOPER,
        "version": API_VERSION,
    })


@app.get("/v1/stats")
@limiter.exempt
def api_stats():
    """
    Statistik penggunaan API
    ---
    tags: [General]
    responses:
      200:
        description: Statistik request, cache hit/miss, per-endpoint
    """
    stats = get_stats()
    return success({
        "requests": {
            "total": stats.get("total_requests", 0),
            "errors": stats.get("errors", 0),
        },
        "cache": {
            "hits": stats.get("cache_hits", 0),
            "misses": stats.get("cache_misses", 0),
            "hit_rate": (
                round(stats.get("cache_hits", 0) / max(stats.get("total_requests", 1), 1) * 100, 1)
            ),
            "live_keys": cache.size(),
        },
        "top_endpoints": dict(
            sorted(stats.get("endpoints", {}).items(), key=lambda x: x[1], reverse=True)[:10]
        ),
    })


@app.get("/v1/cache/clear")
@limiter.limit("5 per hour")
def clear_cache():
    """
    Bersihkan semua cache
    ---
    tags: [General]
    responses:
      200:
        description: Cache berhasil dibersihkan
    """
    cache.clear()
    return success({"cleared": True}, message="Cache berhasil dibersihkan")


@app.get("/v1/postman")
@limiter.exempt
def postman_collection():
    """
    Download Postman Collection JSON
    ---
    tags: [General]
    produces: [application/json]
    responses:
      200:
        description: Postman collection untuk semua endpoint
    """
    base = "{{base_url}}"
    collection = {
        "info": {
            "name": "AniNova API",
            "description": f"Collection by {DEVELOPER} — v{API_VERSION}",
            "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
        },
        "variable": [{"key": "base_url", "value": "https://your-domain.replit.app"}],
        "item": [
            {"name": "Health Check", "request": {"method": "GET", "url": f"{base}/v1/health"}},
            {"name": "API Stats", "request": {"method": "GET", "url": f"{base}/v1/stats"}},
            {"name": "Home (v1)", "request": {"method": "GET", "url": {"raw": f"{base}/v1/home?page=1", "query": [{"key": "page", "value": "1"}]}}},
            {"name": "Latest Episodes", "request": {"method": "GET", "url": f"{base}/v1/latest"}},
            {"name": "Popular", "request": {"method": "GET", "url": f"{base}/v1/popular"}},
            {"name": "Trending", "request": {"method": "GET", "url": f"{base}/v1/trending"}},
            {"name": "Random Anime", "request": {"method": "GET", "url": f"{base}/v1/random"}},
            {"name": "Ongoing Anime", "request": {"method": "GET", "url": f"{base}/v1/ongoing"}},
            {"name": "Completed Anime", "request": {"method": "GET", "url": f"{base}/v1/completed"}},
            {"name": "Search", "request": {"method": "GET", "url": f"{base}/v1/search/perfect world"}},
            {"name": "Genres List", "request": {"method": "GET", "url": f"{base}/v1/genres"}},
            {"name": "Genre Detail", "request": {"method": "GET", "url": f"{base}/v1/genre/action"}},
            {"name": "Anime List", "request": {"method": "GET", "url": f"{base}/v1/anime"}},
            {"name": "Anime Info", "request": {"method": "GET", "url": f"{base}/v1/info/perfect-world"}},
            {"name": "Episode Detail", "request": {"method": "GET", "url": f"{base}/v1/episode/perfect-world-episode-273-subtitle-indonesia"}},
            {"name": "Episode Navigation", "request": {"method": "GET", "url": f"{base}/v1/episode/perfect-world-episode-273-subtitle-indonesia/navigation"}},
            {"name": "Video Source", "request": {"method": "GET", "url": f"{base}/v1/video-source/perfect-world-episode-273-subtitle-indonesia"}},
        ],
    }
    return jsonify(collection)


# ── HOME / LATEST / POPULAR / TRENDING / RANDOM / ONGOING / COMPLETED ──
@app.get("/v1/home")
@limiter.limit("60 per minute")
def v1_home():
    """
    Halaman utama — semua section
    ---
    tags: [Home]
    parameters:
      - {name: page, in: query, type: integer, required: false}
    responses:
      200:
        description: Semua section halaman utama
    """
    try:
        page = request.args.get("page")
        if page and not page.isdigit():
            return err_response("Page harus berupa angka", 400)
        page_num = int(page) if page else 1
        cache_key = f"home:{page_num}"
        cached = cache.get(cache_key)
        if cached:
            return success(cached, cache_hit=True)
        result = main.get_home(page_num)
        cache.set(cache_key, result, ttl=600)
        return success(result)
    except Exception as e:
        logger.error(f"v1_home: {e}")
        return err_response(str(e), 500)


@app.get("/v1/latest")
@limiter.limit("60 per minute")
def get_latest():
    """
    Episode terbaru
    ---
    tags: [Home]
    parameters:
      - {name: page, in: query, type: integer, required: false}
    responses:
      200:
        description: Daftar episode terbaru
    """
    try:
        page = request.args.get("page")
        if page and not page.isdigit():
            return err_response("Page harus berupa angka", 400)
        page_num = int(page) if page else 1
        cache_key = f"latest:{page_num}"
        cached = cache.get(cache_key)
        if cached:
            return success(cached, cache_hit=True)
        result = main.get_latest(page_num)
        cache.set(cache_key, result, ttl=300)
        return success(result)
    except Exception as e:
        logger.error(f"get_latest: {e}")
        return err_response(str(e), 500)


@app.get("/v1/popular")
@limiter.limit("60 per minute")
def get_popular():
    """
    Anime terpopuler
    ---
    tags: [Home]
    parameters:
      - {name: page, in: query, type: integer, required: false}
    responses:
      200:
        description: Daftar anime terpopuler
    """
    try:
        page = request.args.get("page")
        if page and not page.isdigit():
            return err_response("Page harus berupa angka", 400)
        page_num = int(page) if page else 1
        cache_key = f"popular:{page_num}"
        cached = cache.get(cache_key)
        if cached:
            return success(cached, cache_hit=True)
        result = main.get_popular(page_num)
        cache.set(cache_key, result, ttl=600)
        return success(result)
    except Exception as e:
        logger.error(f"get_popular: {e}")
        return err_response(str(e), 500)


@app.get("/v1/trending")
@limiter.limit("60 per minute")
def get_trending():
    """
    Anime trending — gabungan semua section
    ---
    tags: [Home]
    parameters:
      - {name: page, in: query, type: integer, required: false}
    responses:
      200:
        description: Daftar anime trending (maks 20)
    """
    try:
        page = request.args.get("page")
        if page and not page.isdigit():
            return err_response("Page harus berupa angka", 400)
        page_num = int(page) if page else 1
        cache_key = f"trending:{page_num}"
        cached = cache.get(cache_key)
        if cached:
            return success(cached, cache_hit=True)
        result = main.get_trending(page_num)
        cache.set(cache_key, result, ttl=600)
        return success(result)
    except Exception as e:
        logger.error(f"get_trending: {e}")
        return err_response(str(e), 500)


@app.get("/v1/random")
@limiter.limit("30 per minute")
def get_random():
    """
    Anime acak
    ---
    tags: [Home]
    responses:
      200:
        description: Satu anime yang dipilih secara acak
    """
    try:
        result = main.get_random()
        if not result.get("result"):
            return err_response("Tidak ada anime ditemukan", 404)
        return success(result)
    except Exception as e:
        logger.error(f"get_random: {e}")
        return err_response(str(e), 500)


@app.get("/v1/ongoing")
@limiter.limit("30 per minute")
def get_ongoing():
    """
    Anime ongoing
    ---
    tags: [Anime]
    parameters:
      - {name: page, in: query, type: integer, required: false}
    responses:
      200:
        description: Daftar anime yang masih ongoing
    """
    try:
        page = request.args.get("page")
        if page and not page.isdigit():
            return err_response("Page harus berupa angka", 400)
        page_num = int(page) if page else 1
        cache_key = f"ongoing:{page_num}"
        cached = cache.get(cache_key)
        if cached:
            return success(cached, cache_hit=True)
        result = main.get_ongoing(page_num)
        cache.set(cache_key, result, ttl=1800)
        return success(result)
    except Exception as e:
        logger.error(f"get_ongoing: {e}")
        return err_response(str(e), 500)


@app.get("/v1/completed")
@limiter.limit("30 per minute")
def get_completed():
    """
    Anime completed
    ---
    tags: [Anime]
    parameters:
      - {name: page, in: query, type: integer, required: false}
    responses:
      200:
        description: Daftar anime yang sudah selesai
    """
    try:
        page = request.args.get("page")
        if page and not page.isdigit():
            return err_response("Page harus berupa angka", 400)
        page_num = int(page) if page else 1
        cache_key = f"completed:{page_num}"
        cached = cache.get(cache_key)
        if cached:
            return success(cached, cache_hit=True)
        result = main.get_completed(page_num)
        cache.set(cache_key, result, ttl=1800)
        return success(result)
    except Exception as e:
        logger.error(f"get_completed: {e}")
        return err_response(str(e), 500)


# ── SEARCH ──────────────────────────────────────────────────────────
@app.get("/search/<query>")
@limiter.limit("30 per minute")
def search(query: str):
    """
    Cari anime (legacy)
    ---
    tags: [Search]
    parameters:
      - {name: query, in: path, type: string, required: true}
    responses:
      200:
        description: Hasil pencarian
    """
    try:
        if not query or not query.strip():
            return jsonify(message="Query tidak boleh kosong"), 400
        cache_key = f"search:{query.strip().lower()}"
        cached = cache.get(cache_key)
        if cached:
            return jsonify(cached), 200
        result = main.search(query.strip())
        cache.set(cache_key, result, ttl=300)
        return jsonify(result), 200
    except Exception as e:
        logger.error(f"search: {e}")
        return jsonify(message=str(e)), 500


@app.get("/v1/search/<query>")
@limiter.limit("30 per minute")
def v1_search(query: str):
    """
    Cari anime/donghua (v1)
    ---
    tags: [Search]
    parameters:
      - {name: query, in: path, type: string, required: true}
    responses:
      200:
        description: Hasil pencarian
    """
    try:
        if not query or not query.strip():
            return err_response("Query tidak boleh kosong", 400)
        cache_key = f"search:{query.strip().lower()}"
        cached = cache.get(cache_key)
        if cached:
            return success(cached, cache_hit=True)
        result = main.search(query.strip())
        cache.set(cache_key, result, ttl=300)
        return success(result)
    except Exception as e:
        logger.error(f"v1_search: {e}")
        return err_response(str(e), 500)


# ── ANIME ───────────────────────────────────────────────────────────
@app.get("/anime")
@limiter.limit("30 per minute")
def anime():
    """
    Daftar anime (legacy)
    ---
    tags: [Anime]
    responses:
      200:
        description: Daftar anime
    """
    try:
        params_dict = dict(request.args)
        cache_key = f"anime:{sorted(params_dict.items())}"
        cached = cache.get(cache_key)
        if cached:
            return jsonify(cached), 200
        data = main.anime(params=params_dict)
        cache.set(cache_key, data, ttl=1800)
        return jsonify(data), 200
    except Exception as e:
        logger.error(f"anime: {e}")
        return jsonify(message=str(e)), 500


@app.get("/v1/anime")
@limiter.limit("30 per minute")
def v1_anime():
    """
    Daftar anime (v1)
    ---
    tags: [Anime]
    responses:
      200:
        description: Daftar anime
    """
    try:
        params_dict = dict(request.args)
        cache_key = f"anime:{sorted(params_dict.items())}"
        cached = cache.get(cache_key)
        if cached:
            return success(cached, cache_hit=True)
        data = main.anime(params=params_dict)
        cache.set(cache_key, data, ttl=1800)
        return success(data)
    except Exception as e:
        logger.error(f"v1_anime: {e}")
        return err_response(str(e), 500)


# ── GENRES ──────────────────────────────────────────────────────────
@app.get("/genres")
@limiter.limit("30 per minute")
def list_genres():
    """
    Daftar genre (legacy)
    ---
    tags: [Genre]
    responses:
      200:
        description: Semua genre
    """
    try:
        cached = cache.get("genres:all")
        if cached:
            return jsonify(cached), 200
        data = main.genres()
        cache.set("genres:all", data, ttl=3600)
        return jsonify(data), 200
    except Exception as e:
        return jsonify(message=str(e)), 500


@app.get("/v1/genres")
@limiter.limit("30 per minute")
def v1_list_genres():
    """
    Daftar semua genre (v1)
    ---
    tags: [Genre]
    responses:
      200:
        description: Semua genre
    """
    try:
        cached = cache.get("genres:all")
        if cached:
            return success(cached, cache_hit=True)
        data = main.genres()
        cache.set("genres:all", data, ttl=3600)
        return success(data)
    except Exception as e:
        return err_response(str(e), 500)


@app.get("/genre/<slug>")
@limiter.limit("30 per minute")
def get_genres(slug: str):
    """
    Anime berdasarkan genre (legacy)
    ---
    tags: [Genre]
    parameters:
      - {name: slug, in: path, type: string, required: true}
      - {name: page, in: query, type: integer, required: false}
    responses:
      200:
        description: Anime dalam genre
    """
    try:
        page = request.args.get("page")
        if page and not page.isdigit():
            return jsonify(message="Page harus berupa angka"), 400
        page_num = int(page) if page else 1
        cache_key = f"genre:{slug}:{page_num}"
        cached = cache.get(cache_key)
        if cached:
            return jsonify(cached), 200
        data = main.genres(slug.strip(), page_num)
        cache.set(cache_key, data, ttl=1800)
        return jsonify(data), 200
    except Exception as e:
        return jsonify(message=str(e)), 500


@app.get("/v1/genre/<slug>")
@limiter.limit("30 per minute")
def v1_get_genres(slug: str):
    """
    Anime berdasarkan genre (v1)
    ---
    tags: [Genre]
    parameters:
      - {name: slug, in: path, type: string, required: true}
      - {name: page, in: query, type: integer, required: false}
    responses:
      200:
        description: Anime dalam genre
    """
    try:
        page = request.args.get("page")
        if page and not page.isdigit():
            return err_response("Page harus berupa angka", 400)
        page_num = int(page) if page else 1
        cache_key = f"genre:{slug}:{page_num}"
        cached = cache.get(cache_key)
        if cached:
            return success(cached, cache_hit=True)
        data = main.genres(slug.strip(), page_num)
        cache.set(cache_key, data, ttl=1800)
        return success(data)
    except Exception as e:
        return err_response(str(e), 500)


# ── EPISODE ─────────────────────────────────────────────────────────
@app.get("/episode/<slug>")
@limiter.limit("30 per minute")
def get_episode(slug: Text):
    """
    Detail episode (legacy)
    ---
    tags: [Episode]
    parameters:
      - {name: slug, in: path, type: string, required: true}
    responses:
      200:
        description: Detail episode
      404:
        description: Episode tidak ditemukan
    """
    try:
        cache_key = f"episode:{slug}"
        cached = cache.get(cache_key)
        if cached:
            return jsonify(cached), 200
        data = main.get_episode(slug.strip())
        if data.get("result") is None and data.get("error"):
            return jsonify(message="Episode tidak ditemukan"), 404
        cache.set(cache_key, data, ttl=1800)
        return jsonify(data), 200
    except Exception as e:
        return jsonify(message=str(e)), 500


@app.get("/v1/episode/<slug>")
@limiter.limit("30 per minute")
def v1_get_episode(slug: Text):
    """
    Detail episode (v1)
    ---
    tags: [Episode]
    parameters:
      - {name: slug, in: path, type: string, required: true}
    responses:
      200:
        description: Detail episode
      404:
        description: Episode tidak ditemukan
    """
    try:
        import re as _re
        cache_key = f"episode_v2:{slug}"
        cached = cache.get(cache_key)
        if cached:
            return success(cached, cache_hit=True)
        data = main.get_episode(slug.strip())
        result = data.get("result")
        if result is None:
            return err_response("Episode tidak ditemukan", 404)

        ep_match = _re.search(r'episode-(\d+)', slug)
        ep_num = ep_match.group(1) if ep_match else ""
        anime_title = result.get("name", "")

        players = []
        for p in (result.get("players") or []):
            if isinstance(p, dict) and p.get("name", "") != "Pilih Server Video":
                players.append({
                    "server": p.get("name", ""),
                    "url": p.get("url"),
                    "slug": slug,
                })

        transformed = {
            "title": f"{anime_title} Episode {ep_num}".strip() if ep_num else anime_title,
            "slug": slug,
            "anime_slug": result.get("root", ""),
            "anime_title": anime_title,
            "episode": ep_num,
            "players": players,
            "thumbnail": result.get("thumbnail"),
        }
        cache.set(cache_key, transformed, ttl=1800)
        return success(transformed)
    except Exception as e:
        return err_response(str(e), 500)


@app.get("/v1/episode/<slug>/navigation")
@limiter.limit("30 per minute")
def v1_episode_navigation(slug: Text):
    """
    Navigasi episode — next & previous
    ---
    tags: [Episode]
    parameters:
      - {name: slug, in: path, type: string, required: true}
    responses:
      200:
        description: Info next & previous episode
    """
    try:
        cache_key = f"nav:{slug}"
        cached = cache.get(cache_key)
        if cached:
            return success(cached, cache_hit=True)
        data = main.get_episode_navigation(slug.strip())
        cache.set(cache_key, data, ttl=1800)
        return success(data)
    except Exception as e:
        return err_response(str(e), 500)


# ── VIDEO SOURCE ─────────────────────────────────────────────────────
@app.get("/video-source/<slug>")
@limiter.limit("20 per minute")
def get_video(slug: Text):
    """
    Sumber video (legacy)
    ---
    tags: [Video]
    parameters:
      - {name: slug, in: path, type: string, required: true}
    responses:
      200:
        description: Data video
      404:
        description: Video tidak ditemukan
    """
    try:
        cache_key = f"video:{slug}"
        cached = cache.get(cache_key)
        if cached:
            return jsonify(cached), 200
        data = main.get_video_source(slug.strip())
        if not data:
            return jsonify(message="Sumber video tidak ditemukan"), 404
        cache.set(cache_key, data, ttl=300)
        return jsonify(data), 200
    except Exception as e:
        return jsonify(message=str(e)), 500


@app.get("/v1/video-source/<slug>")
@limiter.limit("20 per minute")
def v1_get_video(slug: Text):
    """
    Sumber video multi-server (v1)
    ---
    tags: [Video]
    parameters:
      - name: slug
        in: path
        type: string
        required: true
        description: Slug episode
    responses:
      200:
        description: Semua server — OK.ru dengan direct URL, server lain dengan embed URL
      404:
        description: Video tidak ditemukan
    """
    try:
        cache_key = f"videov2:{slug}"
        cached = cache.get(cache_key)
        if cached:
            return success(cached, cache_hit=True)
        raw = main.get_video_source(slug.strip())
        if not raw:
            return err_response("Sumber video tidak ditemukan", 404)

        servers = raw.get("servers", [])
        if not servers:
            return err_response("Tidak ada server video", 404)

        server = next(
            (s for s in servers if "ok.ru" in (s.get("embed_url") or "")),
            servers[0],
        )

        direct_urls = server.get("direct_urls", [])
        sources = [
            {"url": d["url"], "quality": d.get("quality", "Auto")}
            for d in direct_urls if d.get("url")
        ]
        best_url = sources[0]["url"] if sources else None
        embed = server.get("embed_url")

        transformed = {
            "server": server.get("name", ""),
            "url": embed,
            "direct_url": best_url,
            "quality": sources[0]["quality"] if sources else None,
            "sources": sources,
        }
        cache.set(cache_key, transformed, ttl=300)
        return success(transformed)
    except Exception as e:
        return err_response(str(e), 500)


# ── FAVICON & STATIC FALLBACK ───────────────────────────────────────
@app.get("/favicon.ico")
@limiter.exempt
def favicon():
    return "", 204


@app.get("/robots.txt")
@limiter.exempt
def robots_txt():
    return "User-agent: *\nDisallow: /v1/\n", 200, {"Content-Type": "text/plain"}


# ── LIKES ────────────────────────────────────────────────────────────
@app.get("/v1/likes/<slug>")
@limiter.limit("60 per minute")
def get_likes(slug: Text):
    try:
        row = db_utils.query("SELECT COUNT(*) AS c FROM likes WHERE slug = ?", (slug,))
        count = row[0]["c"] if row else 0
        user = get_current_user()
        user_liked = False
        if user:
            ul = db_utils.query("SELECT id FROM likes WHERE user_id = ? AND slug = ?", (user["id"], slug))
            user_liked = bool(ul)
        return success({"count": count, "user_liked": user_liked})
    except Exception as e:
        return err_response(str(e), 500)


@app.post("/v1/likes/<slug>")
@limiter.limit("30 per minute")
def add_like(slug: Text):
    user = get_current_user()
    if not user:
        return jsonify({"status": "error", "message": "Login diperlukan"}), 401
    try:
        db_utils.execute_no_return(
            "INSERT OR IGNORE INTO likes (user_id, slug) VALUES (?, ?)",
            (user["id"], slug)
        )
        row = db_utils.query("SELECT COUNT(*) AS c FROM likes WHERE slug = ?", (slug,))
        return success({"count": row[0]["c"] if row else 0, "user_liked": True})
    except Exception as e:
        return err_response(str(e), 500)


@app.delete("/v1/likes/<slug>")
@limiter.limit("30 per minute")
def remove_like(slug: Text):
    user = get_current_user()
    if not user:
        return jsonify({"status": "error", "message": "Login diperlukan"}), 401
    try:
        db_utils.execute_no_return(
            "DELETE FROM likes WHERE user_id = ? AND slug = ?",
            (user["id"], slug)
        )
        row = db_utils.query("SELECT COUNT(*) AS c FROM likes WHERE slug = ?", (slug,))
        return success({"count": row[0]["c"] if row else 0, "user_liked": False})
    except Exception as e:
        return err_response(str(e), 500)


# ── NOTIFICATIONS ─────────────────────────────────────────────────────
@app.get("/v1/notifications")
@limiter.limit("60 per minute")
def get_notifications():
    user = get_current_user()
    if not user:
        return jsonify({"status": "error", "message": "Login diperlukan"}), 401
    try:
        rows = db_utils.query(
            """SELECT id, type, title, body, slug, thumbnail, is_read, created_at
               FROM notifications WHERE user_id = ?
               ORDER BY created_at DESC LIMIT 50""",
            (user["id"],)
        )
        unread = sum(1 for r in rows if not r["is_read"])
        return success({"notifications": rows, "unread": unread, "total": len(rows)})
    except Exception as e:
        return err_response(str(e), 500)


@app.post("/v1/notifications/read")
@limiter.limit("30 per minute")
def mark_notifications_read():
    user = get_current_user()
    if not user:
        return jsonify({"status": "error", "message": "Login diperlukan"}), 401
    try:
        db_utils.execute_no_return(
            "UPDATE notifications SET is_read = 1 WHERE user_id = ?",
            (user["id"],)
        )
        return success({"message": "Semua notifikasi ditandai telah dibaca"})
    except Exception as e:
        return err_response(str(e), 500)


# ── DONGHUA / MOVIE / TYPE FILTER ────────────────────────────────────
@app.get("/v1/donghua")
@limiter.limit("30 per minute")
def get_donghua():
    try:
        page = int(request.args.get("page", 1))
        cache_key = f"donghua:{page}"
        cached = cache.get(cache_key)
        if cached:
            return success(cached, cache_hit=True)
        data = main.anime(params={"type": "Donghua", "page": str(page)})
        cache.set(cache_key, data, ttl=1800)
        return success(data)
    except Exception as e:
        return err_response(str(e), 500)


@app.get("/v1/movie")
@limiter.limit("30 per minute")
def get_movie():
    try:
        page = int(request.args.get("page", 1))
        cache_key = f"movie:{page}"
        cached = cache.get(cache_key)
        if cached:
            return success(cached, cache_hit=True)
        data = main.anime(params={"type": "Movie", "page": str(page)})
        cache.set(cache_key, data, ttl=1800)
        return success(data)
    except Exception as e:
        return err_response(str(e), 500)


# ── VIEWS (hit counter) ──────────────────────────────────────────────
@app.post("/v1/views/<slug>")
@limiter.limit("30 per minute")
def add_view(slug: Text):
    try:
        db_utils.execute_no_return(
            """INSERT INTO views (slug, count) VALUES (?, 1)
               ON CONFLICT(slug) DO UPDATE SET
                 count = count + 1,
                 updated_at = datetime('now', 'localtime')""",
            (slug,),
        )
        row = db_utils.query("SELECT count FROM views WHERE slug = ?", (slug,))
        cnt = row[0]["count"] if row else 1
        return success({"count": cnt})
    except Exception as e:
        return err_response(str(e), 500)


@app.get("/v1/views/<slug>")
@limiter.limit("60 per minute")
def get_view(slug: Text):
    try:
        row = db_utils.query("SELECT count FROM views WHERE slug = ?", (slug,))
        cnt = row[0]["count"] if row else 0
        return success({"count": cnt})
    except Exception as e:
        return err_response(str(e), 500)


# ── COMMENTS ─────────────────────────────────────────────────────────
@app.get("/v1/comments/<slug>")
@limiter.limit("30 per minute")
def get_comments(slug: Text):
    """
    Ambil komentar anime
    ---
    tags: [Comments]
    parameters:
      - {name: slug, in: path, type: string, required: true}
    responses:
      200:
        description: Daftar komentar
    """
    try:
        rows = db_utils.query(
            """SELECT c.id, c.content, c.created_at, c.user_id,
                      u.username, u.avatar
               FROM comments c
               JOIN users u ON c.user_id = u.id
               WHERE c.anime_slug = ?
               ORDER BY c.created_at DESC
               LIMIT 100""",
            (slug,),
        )
        return success({"comments": rows, "total": len(rows)})
    except Exception as e:
        return err_response(str(e), 500)


@app.post("/v1/comments/<slug>")
@limiter.limit("10 per minute")
def add_comment(slug: Text):
    """
    Tambah komentar
    ---
    tags: [Comments]
    parameters:
      - {name: slug, in: path, type: string, required: true}
    responses:
      200:
        description: Komentar berhasil ditambahkan
      401:
        description: Login diperlukan
    """
    user = get_current_user()
    if not user:
        return jsonify({"status": "error", "message": "Login diperlukan"}), 401
    data = request.get_json(silent=True) or {}
    content = data.get("content", "").strip()
    if not content:
        return jsonify({"status": "error", "message": "Komentar tidak boleh kosong"}), 400
    if len(content) > 500:
        return jsonify({"status": "error", "message": "Komentar terlalu panjang (max 500 karakter)"}), 400
    db_utils.execute_no_return(
        "INSERT INTO comments (anime_slug, user_id, content) VALUES (?, ?, ?)",
        (slug, user["id"], content),
    )
    return success({"message": "Komentar berhasil ditambahkan"})


@app.delete("/v1/comments/<int:comment_id>")
@limiter.limit("20 per minute")
def delete_comment(comment_id: int):
    """
    Hapus komentar
    ---
    tags: [Comments]
    parameters:
      - {name: comment_id, in: path, type: integer, required: true}
    responses:
      200:
        description: Komentar dihapus
      401:
        description: Login diperlukan
      403:
        description: Tidak diizinkan
    """
    user = get_current_user()
    if not user:
        return jsonify({"status": "error", "message": "Login diperlukan"}), 401
    if user.get("role") == "admin":
        db_utils.execute_no_return("DELETE FROM comments WHERE id = ?", (comment_id,))
    else:
        db_utils.execute_no_return(
            "DELETE FROM comments WHERE id = ? AND user_id = ?",
            (comment_id, user["id"]),
        )
    return success({"message": "Komentar dihapus"})


# ── ANIME INFO (catch-all) ───────────────────────────────────────────
@app.get("/v1/info/<slug>")
@limiter.limit("30 per minute")
def v1_get_info(slug: Text):
    """
    Info lengkap anime (v1)
    ---
    tags: [Anime]
    parameters:
      - {name: slug, in: path, type: string, required: true}
    responses:
      200:
        description: Info anime lengkap
      404:
        description: Anime tidak ditemukan
    """
    try:
        cache_key = f"info:{slug}"
        cached = cache.get(cache_key)
        if cached:
            return success(cached, cache_hit=True)
        data = main.get_info(slug.strip())
        if data.get("result") is None and data.get("error"):
            return err_response("Anime tidak ditemukan", 404)
        cache.set(cache_key, data, ttl=1800)
        return success(data)
    except Exception as e:
        return err_response(str(e), 500)


@app.get("/<slug>")
@limiter.limit("30 per minute")
def get_info(slug: Text):
    """
    Info anime (legacy catch-all)
    ---
    tags: [Anime]
    parameters:
      - {name: slug, in: path, type: string, required: true}
    responses:
      200:
        description: Info anime
      404:
        description: Tidak ditemukan
    """
    try:
        cache_key = f"info:{slug}"
        cached = cache.get(cache_key)
        if cached:
            return jsonify(cached), 200
        data = main.get_info(slug.strip())
        if data.get("result") is None and data.get("error"):
            return jsonify(message="Tidak ditemukan"), 404
        cache.set(cache_key, data, ttl=1800)
        return jsonify(data), 200
    except Exception as e:
        return jsonify(message=str(e)), 500


# ── ERROR HANDLERS ──────────────────────────────────────────────────
@app.errorhandler(404)
def not_found(e):
    return err_response("Endpoint tidak ditemukan", 404)


@app.errorhandler(429)
def rate_limit_exceeded(e):
    return err_response("Terlalu banyak request. Coba lagi nanti.", 429)


@app.errorhandler(500)
def internal_error(e):
    logger.error(f"500: {e}")
    return err_response("Internal server error", 500)


if __name__ == "__main__":
    app.run(debug=True, host="0.0.0.0", port=5000, use_reloader=True)
