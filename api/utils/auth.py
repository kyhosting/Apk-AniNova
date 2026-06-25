import os
import jwt
import bcrypt
import logging
import random
import string
from datetime import datetime, timedelta, timezone
from functools import wraps
from typing import Optional, Dict, Any

from .email import send_otp_email

from flask import request, jsonify, g, redirect
from . import db

logger = logging.getLogger(__name__)

JWT_SECRET = os.environ.get("JWT_SECRET", "aninova-jwt-secret-2025-change-me")
JWT_EXPIRE_DAYS = 30

AVATARS = [
    '<i class="fa-solid fa-dragon"></i>',
    '<i class="fa-solid fa-cat"></i>',
    '<i class="fa-solid fa-dog"></i>',
    '<i class="fa-solid fa-crow"></i>',
    '<i class="fa-solid fa-hippo"></i>',
    '<i class="fa-solid fa-horse"></i>',
    '<i class="fa-solid fa-frog"></i>',
    '<i class="fa-solid fa-otter"></i>',
    '<i class="fa-solid fa-feather-pointed"></i>',
    '<i class="fa-solid fa-paw"></i>',
    '<i class="fa-solid fa-shield-halved"></i>',
    '<i class="fa-solid fa-bolt"></i>',
    '<i class="fa-solid fa-star"></i>',
    '<i class="fa-solid fa-moon"></i>',
    '<i class="fa-solid fa-fire"></i>',
]


def hash_password(password: str) -> str:
    return bcrypt.hashpw(password.encode(), bcrypt.gensalt()).decode()


def check_password(password: str, hashed: str) -> bool:
    return bcrypt.checkpw(password.encode(), hashed.encode())


def create_token(user_id: int, role: str) -> str:
    payload = {
        "sub": str(user_id),   # JWT spec requires sub to be a string
        "role": role,
        "iat": datetime.now(timezone.utc),
        "exp": datetime.now(timezone.utc) + timedelta(days=JWT_EXPIRE_DAYS),
    }
    return jwt.encode(payload, JWT_SECRET, algorithm="HS256")


def decode_token(token: str) -> Optional[Dict[str, Any]]:
    try:
        return jwt.decode(token, JWT_SECRET, algorithms=["HS256"])
    except Exception:
        # Fallback: tolerate old tokens that stored sub as int
        try:
            return jwt.decode(
                token, JWT_SECRET, algorithms=["HS256"],
                options={"verify_sub": False},
            )
        except Exception:
            return None


def get_current_user() -> Optional[Dict[str, Any]]:
    token = None
    auth = request.headers.get("Authorization", "")
    if auth.startswith("Bearer "):
        token = auth[7:]
    if not token:
        token = request.cookies.get("token")
    if not token:
        return None
    payload = decode_token(token)
    if not payload:
        return None
    try:
        uid = int(payload["sub"])  # sub may be str (new) or int (legacy)
    except (KeyError, TypeError, ValueError):
        return None
    rows = db.query(
        "SELECT id, username, email, role, avatar, created_at, last_login FROM users WHERE id = ? AND is_active = 1",
        (uid,)
    )
    return rows[0] if rows else None


def login_required(f):
    @wraps(f)
    def decorated(*args, **kwargs):
        user = get_current_user()
        if not user:
            if request.path.startswith("/v1/") or request.is_json:
                return jsonify({"status": "error", "message": "Login diperlukan"}), 401
            return redirect("/login?next=" + request.path)
        g.user = user
        return f(*args, **kwargs)
    return decorated


def admin_required(f):
    @wraps(f)
    def decorated(*args, **kwargs):
        user = get_current_user()
        if not user:
            return jsonify({"status": "error", "message": "Login diperlukan"}), 401
        if user.get("role") != "admin":
            return jsonify({"status": "error", "message": "Akses admin diperlukan"}), 403
        g.user = user
        return f(*args, **kwargs)
    return decorated


def register_user(username: str, email: str, password: str) -> Dict[str, Any]:
    username = username.strip()
    email = email.strip().lower()
    if len(username) < 3 or len(username) > 30:
        return {"error": "Username harus 3-30 karakter"}
    if not username.replace("_", "").replace("-", "").isalnum():
        return {"error": "Username hanya boleh huruf, angka, _ dan -"}
    if len(password) < 6:
        return {"error": "Password minimal 6 karakter"}
    if "@" not in email or "." not in email:
        return {"error": "Email tidak valid"}
    existing = db.query(
        "SELECT id FROM users WHERE username = ? OR email = ?",
        (username.lower(), email)
    )
    if existing:
        return {"error": "Username atau email sudah digunakan"}
    avatar = random.choice(AVATARS)
    hashed = hash_password(password)
    user = db.execute(
        "INSERT INTO users (username, email, password_hash, avatar) VALUES (?, ?, ?, ?) RETURNING id, username, email, role, avatar",
        (username.lower(), email, hashed, avatar)
    )
    if not user:
        return {"error": "Gagal membuat akun"}
    token = create_token(user["id"], user["role"])
    return {"user": user, "token": token}


def login_user(login: str, password: str) -> Dict[str, Any]:
    login = login.strip().lower()
    rows = db.query(
        "SELECT * FROM users WHERE (username = ? OR email = ?) AND is_active = 1",
        (login, login)
    )
    if not rows:
        return {"error": "Username/email atau password salah"}
    user = rows[0]
    if not check_password(password, user["password_hash"]):
        return {"error": "Username/email atau password salah"}
    db.execute_no_return(
        "UPDATE users SET last_login = datetime('now') WHERE id = ?", (user["id"],)
    )
    token = create_token(user["id"], user["role"])
    return {
        "user": {
            "id": user["id"],
            "username": user["username"],
            "email": user["email"],
            "role": user["role"],
            "avatar": user["avatar"],
        },
        "token": token
    }


def send_otp(email: str, username: str, password: str) -> Dict[str, Any]:
    email = email.strip().lower()
    username = username.strip()

    if len(username) < 3 or len(username) > 30:
        return {"error": "Username harus 3-30 karakter"}
    if not username.replace("_", "").replace("-", "").isalnum():
        return {"error": "Username hanya boleh huruf, angka, _ dan -"}
    if len(password) < 6:
        return {"error": "Password minimal 6 karakter"}
    if "@" not in email or "." not in email:
        return {"error": "Email tidak valid"}

    existing = db.query(
        "SELECT id FROM users WHERE username = ? OR email = ?",
        (username.lower(), email),
    )
    if existing:
        return {"error": "Username atau email sudah digunakan"}

    otp = "".join(random.choices(string.digits, k=6))
    password_hash = hash_password(password)
    expires_at = (datetime.now(timezone.utc) + timedelta(minutes=10)).isoformat()

    db.execute_no_return("DELETE FROM otp_requests WHERE email = ?", (email,))
    db.execute_no_return(
        "INSERT INTO otp_requests (email, username, password_hash, otp_code, expires_at) VALUES (?, ?, ?, ?, ?)",
        (email, username.lower(), password_hash, otp, expires_at),
    )

    sent = send_otp_email(email, username, otp)
    if not sent:
        if not os.environ.get("GMAIL_USER"):
            return {
                "dev_otp": otp,
                "message": f"GMAIL tidak dikonfigurasi (dev mode). Kode OTP kamu: {otp}",
            }
        return {"error": "Gagal mengirim email. Coba lagi atau periksa konfigurasi Gmail."}

    return {"sent": True}


def verify_otp_and_register(email: str, otp_code: str) -> Dict[str, Any]:
    email = email.strip().lower()

    rows = db.query(
        "SELECT * FROM otp_requests WHERE email = ? ORDER BY created_at DESC LIMIT 1",
        (email,),
    )
    if not rows:
        return {"error": "Tidak ada permintaan OTP untuk email ini. Minta kode baru."}

    row = rows[0]

    try:
        exp_str = row["expires_at"].replace("Z", "+00:00")
        expires_at = datetime.fromisoformat(exp_str)
        if expires_at.tzinfo is None:
            expires_at = expires_at.replace(tzinfo=timezone.utc)
    except Exception:
        return {"error": "Data OTP tidak valid. Minta kode baru."}

    if datetime.now(timezone.utc) > expires_at:
        db.execute_no_return("DELETE FROM otp_requests WHERE email = ?", (email,))
        return {"error": "Kode OTP sudah kedaluwarsa. Minta kode baru."}

    if row["otp_code"] != otp_code.strip():
        return {"error": "Kode OTP salah. Periksa email kamu."}

    db.execute_no_return("DELETE FROM otp_requests WHERE email = ?", (email,))

    avatar = random.choice(AVATARS)
    user = db.execute(
        "INSERT INTO users (username, email, password_hash, avatar) VALUES (?, ?, ?, ?) RETURNING id, username, email, role, avatar",
        (row["username"], email, row["password_hash"], avatar),
    )
    if not user:
        return {"error": "Gagal membuat akun. Coba lagi."}

    token = create_token(user["id"], user["role"])
    return {"user": user, "token": token}
