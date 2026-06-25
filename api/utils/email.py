import os
import smtplib
import logging
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart

logger = logging.getLogger(__name__)

GMAIL_USER = os.environ.get("GMAIL_USER", "kicenxensai@gmail.com")
GMAIL_APP_PASSWORD = os.environ.get("GMAIL_APP_PASSWORD", "jxob uxgg cfqt nfsy")


def send_otp_email(to_email: str, username: str, otp_code: str) -> bool:
    if not GMAIL_USER or not GMAIL_APP_PASSWORD:
        logger.warning("Gmail credentials not set (GMAIL_USER / GMAIL_APP_PASSWORD). OTP not sent via email.")
        return False

    subject = f"Kode OTP AniNova — {otp_code}"
    html_body = f"""
<!DOCTYPE html>
<html lang="id">
<head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
<body style="margin:0;padding:0;background:#0a0a0a;font-family:Inter,Arial,sans-serif;">
  <table width="100%" cellpadding="0" cellspacing="0" style="background:#0a0a0a;min-height:100vh;">
    <tr><td align="center" style="padding:40px 16px;">
      <table width="480" cellpadding="0" cellspacing="0" style="background:#141414;border-radius:16px;border:1px solid rgba(255,255,255,0.08);overflow:hidden;max-width:100%;">
        <tr>
          <td style="padding:32px 36px 0;text-align:center;">
            <div style="font-size:28px;font-weight:900;color:#e50914;letter-spacing:-1px;">
            ANINOVA</div>
            <div style="font-size:12px;color:#666;margin-top:4px;letter-spacing:2px;text-transform:uppercase;">Anime & Donghua Platform</div>
          </td>
        </tr>
        <tr>
          <td style="padding:28px 36px 0;">
            <h2 style="color:#fff;font-size:20px;font-weight:600;margin:0 0 8px;">Verifikasi Akun Kamu</h2>
            <p style="color:#9ca3af;font-size:14px;line-height:1.6;margin:0;">
              Halo <strong style="color:#fff">{username}</strong>! Masukkan kode OTP berikut untuk menyelesaikan pendaftaran di AniNova.
            </p>
          </td>
        </tr>
        <tr>
          <td style="padding:28px 36px;">
            <div style="background:#1e1e1e;border:2px solid #e50914;border-radius:12px;padding:28px;text-align:center;">
              <div style="font-size:48px;font-weight:900;letter-spacing:16px;color:#e50914;font-family:monospace;">{otp_code}</div>
              <div style="font-size:12px;color:#666;margin-top:12px;">Kode berlaku selama <strong style="color:#9ca3af">10 menit</strong></div>
            </div>
          </td>
        </tr>
        <tr>
          <td style="padding:0 36px 28px;">
            <div style="background:#1a1a1a;border-radius:8px;padding:16px;border-left:3px solid #e50914;">
              <p style="color:#9ca3af;font-size:13px;margin:0;line-height:1.6;">
                Jangan bagikan kode ini ke siapapun. Tim AniNova tidak akan pernah meminta kode OTP kamu.
              </p>
            </div>
          </td>
        </tr>
        <tr>
          <td style="padding:20px 36px;border-top:1px solid rgba(255,255,255,0.06);text-align:center;">
            <p style="color:#555;font-size:12px;margin:0;">
              Jika kamu tidak mendaftar di AniNova, abaikan email ini.<br>
              &copy; 2026 AniNova. All rights reserved.
            </p>
          </td>
        </tr>
      </table>
    </td></tr>
  </table>
</body>
</html>
    """

    try:
        msg = MIMEMultipart("alternative")
        msg["Subject"] = subject
        msg["From"] = f"AniNova <{GMAIL_USER}>"
        msg["To"] = to_email
        msg.attach(MIMEText(html_body, "html", "utf-8"))

        with smtplib.SMTP_SSL("smtp.gmail.com", 465) as server:
            server.login(GMAIL_USER, GMAIL_APP_PASSWORD)
            server.sendmail(GMAIL_USER, to_email, msg.as_string())

        logger.info(f"OTP email sent to {to_email}")
        return True
    except smtplib.SMTPAuthenticationError:
        logger.error("Gmail auth failed — check GMAIL_USER and GMAIL_APP_PASSWORD (use App Password, not regular password)")
        return False
    except Exception as e:
        logger.error(f"Failed to send OTP email: {e}")
        return False
