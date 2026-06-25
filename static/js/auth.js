'use strict';

const Auth = {
  get user()  { try { return JSON.parse(localStorage.getItem('acuser') || 'null'); } catch { return null; } },
  get token() { return localStorage.getItem('actoken') || ''; },
  isLoggedIn() { return !!(this.token && this.user); },

  set(user, token) {
    localStorage.setItem('acuser', JSON.stringify(user));
    localStorage.setItem('actoken', token);
  },
  clear() {
    localStorage.removeItem('acuser');
    localStorage.removeItem('actoken');
  },
  guard() {
    if (!this.isLoggedIn()) {
      window.location.href = '/login?next=' + encodeURIComponent(location.pathname);
      return false;
    }
    return true;
  }
};

// ── Auth page logic ───────────────────────────────────────────────────
(function () {
  if (!document.getElementById('auth-page')) return;

  const params = new URLSearchParams(location.search);
  const nextUrl = params.get('next') || '/app';

  if (Auth.isLoggedIn()) { window.location.href = nextUrl; return; }

  let mode = location.pathname.includes('register') || params.get('mode') === 'register' ? 'register' : 'login';
  let step = 1;

  const loginSection    = document.getElementById('section-login');
  const registerSection = document.getElementById('section-register');
  const otpSection      = document.getElementById('section-otp');

  function showMode(m) {
    mode = m;
    loginSection.classList.toggle('hidden', mode !== 'login');
    registerSection.classList.toggle('hidden', mode !== 'register');
    otpSection.classList.add('hidden');
    clearErrors();
    if (mode === 'register') {
      history.replaceState({}, '', '/register');
    } else {
      history.replaceState({}, '', '/login');
    }
  }

  document.querySelectorAll('[data-mode]').forEach(el => {
    el.addEventListener('click', () => showMode(el.dataset.mode));
  });

  showMode(mode);

  function setError(id, msg) {
    const el = document.getElementById(id);
    if (el) { el.textContent = msg; el.classList.toggle('hidden', !msg); }
  }
  function clearErrors() {
    document.querySelectorAll('.form-error').forEach(el => { el.textContent = ''; el.classList.add('hidden'); });
  }
  function setLoading(btn, loading) {
    btn.disabled = loading;
    btn.dataset.orig = btn.dataset.orig || btn.textContent;
    btn.textContent = loading ? 'Memuat...' : btn.dataset.orig;
  }

  // LOGIN
  document.getElementById('btn-login').addEventListener('click', async () => {
    clearErrors();
    const login = document.getElementById('login-input').value.trim();
    const pass  = document.getElementById('login-pass').value;
    if (!login) return setError('err-login', 'Email atau username wajib diisi');
    if (!pass)  return setError('err-login', 'Password wajib diisi');
    const btn = document.getElementById('btn-login');
    setLoading(btn, true);
    try {
      const res = await API.login(login, pass);
      if (res.status === 'ok') {
        Auth.set(res.user, res.token);
        showToast('Selamat datang, ' + res.user.username + '!', 'success');
        setTimeout(() => window.location.href = nextUrl, 800);
      } else {
        setError('err-login', res.message || 'Login gagal');
      }
    } catch { setError('err-login', 'Koneksi gagal'); }
    setLoading(btn, false);
  });

  // REGISTER step 1 — kirim OTP
  document.getElementById('btn-send-otp').addEventListener('click', async () => {
    clearErrors();
    const uname = document.getElementById('reg-username').value.trim();
    const email = document.getElementById('reg-email').value.trim();
    const pass  = document.getElementById('reg-pass').value;
    const pass2 = document.getElementById('reg-pass2').value;
    if (!uname) return setError('err-reg', 'Username wajib diisi');
    if (!email) return setError('err-reg', 'Email wajib diisi');
    if (pass.length < 6) return setError('err-reg', 'Password minimal 6 karakter');
    if (pass !== pass2) return setError('err-reg', 'Password tidak sama');
    const btn = document.getElementById('btn-send-otp');
    setLoading(btn, true);
    try {
      const res = await API.sendOtp(email, uname, pass);
      if (res.status === 'ok' || res.dev_otp) {
        registerSection.classList.add('hidden');
        otpSection.classList.remove('hidden');
        document.getElementById('otp-email-hint').textContent = email;
        
      } else {
        setError('err-reg', res.message || 'Gagal mengirim OTP');
      }
    } catch { setError('err-reg', 'Koneksi gagal'); }
    setLoading(btn, false);
  });

  // REGISTER step 2 — verifikasi OTP
  document.getElementById('btn-verify-otp').addEventListener('click', async () => {
    clearErrors();
    const email = document.getElementById('reg-email').value.trim();
    const otp   = document.getElementById('otp-input').value.trim();
    if (otp.length !== 6) return setError('err-otp', 'Kode OTP harus 6 digit');
    const btn = document.getElementById('btn-verify-otp');
    setLoading(btn, true);
    try {
      const res = await API.register(email, otp);
      if (res.status === 'ok') {
        Auth.set(res.user, res.token);
        showToast('Akun berhasil dibuat! Selamat datang <i class="fa-solid fa-party-horn"></i>', 'success');
        setTimeout(() => window.location.href = nextUrl, 900);
      } else {
        setError('err-otp', res.message || 'OTP salah');
      }
    } catch { setError('err-otp', 'Koneksi gagal'); }
    setLoading(btn, false);
  });

  document.getElementById('btn-back-otp').addEventListener('click', () => {
    otpSection.classList.add('hidden');
    registerSection.classList.remove('hidden');
  });

  // Toggle password visibility
  document.querySelectorAll('[data-toggle-pass]').forEach(btn => {
    btn.addEventListener('click', () => {
      const inp = document.getElementById(btn.dataset.togglePass);
      if (!inp) return;
      inp.type = inp.type === 'password' ? 'text' : 'password';
      const icon = btn.querySelector('i');
      if (icon) icon.className = inp.type === 'password' ? 'fa-solid fa-eye' : 'fa-solid fa-eye-slash';
    });
  });

  // Enter key
  document.getElementById('login-pass').addEventListener('keydown', e => {
    if (e.key === 'Enter') document.getElementById('btn-login').click();
  });
})();

function showToast(msg, type = 'info') {
  let container = document.getElementById('toast-container');
  if (!container) {
    container = document.createElement('div');
    container.id = 'toast-container';
    document.body.appendChild(container);
  }
  const toast = document.createElement('div');
  toast.className = 'toast toast-' + type;
  toast.innerHTML = msg;
  container.appendChild(toast);
  requestAnimationFrame(() => toast.classList.add('show'));
  setTimeout(() => {
    toast.classList.remove('show');
    setTimeout(() => toast.remove(), 300);
  }, 3500);
}
