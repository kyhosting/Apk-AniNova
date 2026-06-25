'use strict';
(function () {
  if (sessionStorage.getItem('an_splash_done')) return;
  sessionStorage.setItem('an_splash_done', '1');

  const TOTAL_MS = 18000;

  const STEPS = [
    { icon: 'fa-solid fa-bolt',           color: '#facc15', text: 'Menginisialisasi AniNova...',        sub: 'Mempersiapkan sistem core' },
    { icon: 'fa-solid fa-database',       color: '#a855f7', text: 'Memuat database anime & donghua...', sub: 'Ribuan judul sedang diproses' },
    { icon: 'fa-solid fa-clapperboard',   color: '#6366f1', text: 'Menyiapkan engine streaming...',     sub: 'Konfigurasi video player' },
    { icon: 'fa-solid fa-satellite-dish', color: '#22d3ee', text: 'Menghubungkan ke server CDN...',     sub: 'Memilih server terbaik untuk kamu' },
    { icon: 'fa-solid fa-film',           color: '#f472b6', text: 'Optimasi kualitas video...',         sub: 'Konfigurasi auto-playback' },
    { icon: 'fa-solid fa-shield-halved',  color: '#4ade80', text: 'Memverifikasi keamanan...',          sub: 'Enkripsi data pengguna' },
    { icon: 'fa-solid fa-rocket',         color: '#e879f9', text: 'Launching AniNova...',               sub: 'Selamat datang, siap untuk nonton!' },
  ];

  const el = document.createElement('div');
  el.id = 'splash-screen';
  el.innerHTML = `
    <div class="sp-orb sp-orb1"></div>
    <div class="sp-orb sp-orb2"></div>
    <div class="sp-orb sp-orb3"></div>

    <div class="sp-center">
      <div class="sp-logo-wrap">
        <img src="/static/img/logo.svg" class="sp-logo-img" alt="AniNova">
        <div class="sp-brand neon-logo">AniNova</div>
        <div class="sp-tagline">Platform Streaming Anime &amp; Donghua #1</div>
      </div>

      <div class="sp-step-box">
        <div class="sp-step-icon" id="sp-icon"><i class="fa-solid fa-bolt sp-fa-icon"></i></div>
        <div class="sp-step-text" id="sp-text">Memulai sistem...</div>
        <div class="sp-step-sub"  id="sp-sub">Mohon tunggu sebentar</div>
      </div>

      <div class="sp-progress-wrap">
        <div class="sp-prog-track"><div class="sp-prog-fill" id="sp-fill"></div></div>
        <div class="sp-prog-pct" id="sp-pct">0%</div>
      </div>

      <div class="sp-dots">
        <span></span><span></span><span></span>
      </div>
    </div>

  `;

  document.documentElement.style.overflow = 'hidden';
  document.body.prepend(el);

  const fillEl  = document.getElementById('sp-fill');
  const pctEl   = document.getElementById('sp-pct');
  const iconEl  = document.getElementById('sp-icon');
  const textEl  = document.getElementById('sp-text');
  const subEl   = document.getElementById('sp-sub');

  const stepDur = TOTAL_MS / STEPS.length;
  let startTime = null;
  let stepIdx   = -1;
  let raf       = null;

  function setStep(i) {
    const s = STEPS[Math.min(i, STEPS.length - 1)];
    if (!s) return;
    iconEl.style.opacity = '0';
    textEl.style.opacity = '0';
    subEl.style.opacity  = '0';
    setTimeout(() => {
      iconEl.innerHTML = `<i class="${s.icon} sp-fa-icon" style="color:${s.color};filter:drop-shadow(0 0 10px ${s.color}88)"></i>`;
      textEl.textContent = s.text;
      subEl.textContent  = s.sub;
      iconEl.style.opacity = '1';
      textEl.style.opacity = '1';
      subEl.style.opacity  = '1';
    }, 180);
  }

  function closeSplash() {
    cancelAnimationFrame(raf);
    el.style.transition = 'opacity .6s ease';
    el.style.opacity    = '0';
    setTimeout(() => {
      el.remove();
      document.documentElement.style.overflow = '';
    }, 650);
  }

  window.skipSplash = closeSplash;

  function tick(ts) {
    if (!startTime) startTime = ts;
    const elapsed = ts - startTime;
    const pct = Math.min(100, Math.round((elapsed / TOTAL_MS) * 100));

    fillEl.style.width = pct + '%';
    pctEl.textContent  = pct + '%';

    const cur = Math.floor(elapsed / stepDur);
    if (cur !== stepIdx && cur < STEPS.length) { stepIdx = cur; setStep(stepIdx); }

    if (elapsed < TOTAL_MS) {
      raf = requestAnimationFrame(tick);
    } else {
      setStep(STEPS.length - 1);
      fillEl.style.width = '100%';
      pctEl.textContent  = '100%';
      setTimeout(closeSplash, 800);
    }
  }

  setStep(0);
  raf = requestAnimationFrame(tick);
})();
