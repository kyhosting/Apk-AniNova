'use strict';

(function () {
  if (!document.getElementById('landing-page')) return;

  if (Auth.isLoggedIn()) {
    document.querySelectorAll('.nav-logged-out').forEach(el => el.classList.add('hidden'));
    document.querySelectorAll('.nav-logged-in').forEach(el => el.classList.remove('hidden'));
  }

  // ── Canvas Particle System ────────────────────────────────────────
  const canvas = document.getElementById('bg-canvas');
  if (canvas) {
    const ctx = canvas.getContext('2d');
    let W, H, particles = [], animId;

    function resize() {
      W = canvas.width  = window.innerWidth;
      H = canvas.height = window.innerHeight;
    }
    resize();
    window.addEventListener('resize', resize);

    const COLORS = ['rgba(0,229,255,', 'rgba(191,95,255,', 'rgba(229,9,20,', 'rgba(255,255,255,'];
    const WORDS  = ['AniNova','DONGHUA','ANIME','ACTION','FANTASY','SUB ID','HD','WATCH'];

    class Particle {
      constructor() { this.reset(true); }
      reset(init) {
        this.x    = Math.random() * W;
        this.y    = init ? Math.random() * H : H + 20;
        this.size = Math.random() * 2.5 + .5;
        this.vx   = (Math.random() - .5) * .4;
        this.vy   = -(Math.random() * .8 + .2);
        this.col  = COLORS[Math.floor(Math.random() * COLORS.length)];
        this.alpha= Math.random() * .5 + .1;
        this.life = 1;
        this.decay= Math.random() * .003 + .001;
        this.isText = Math.random() < .04;
        if (this.isText) {
          this.word = WORDS[Math.floor(Math.random() * WORDS.length)];
          this.fontSize = Math.random() * 8 + 8;
        }
      }
      update() {
        this.x += this.vx;
        this.y += this.vy;
        this.life -= this.decay;
        if (this.life <= 0 || this.y < -30) this.reset(false);
      }
      draw() {
        ctx.save();
        ctx.globalAlpha = this.life * this.alpha;
        ctx.fillStyle = this.col + this.alpha + ')';
        if (this.isText) {
          ctx.font = `${this.fontSize}px Inter, sans-serif`;
          ctx.fillStyle = this.col + (this.life * .3) + ')';
          ctx.fillText(this.word, this.x, this.y);
        } else {
          ctx.beginPath();
          ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2);
          ctx.fill();
        }
        ctx.restore();
      }
    }

    for (let i = 0; i < 120; i++) particles.push(new Particle());

    // Connection lines between nearby particles
    function drawConnections() {
      const maxDist = 100;
      for (let i = 0; i < particles.length; i++) {
        for (let j = i + 1; j < particles.length; j++) {
          const dx = particles[i].x - particles[j].x;
          const dy = particles[i].y - particles[j].y;
          const d  = Math.sqrt(dx*dx + dy*dy);
          if (d < maxDist) {
            ctx.save();
            ctx.globalAlpha = (1 - d/maxDist) * .08;
            ctx.strokeStyle = 'rgba(0,229,255,1)';
            ctx.lineWidth = .5;
            ctx.beginPath();
            ctx.moveTo(particles[i].x, particles[i].y);
            ctx.lineTo(particles[j].x, particles[j].y);
            ctx.stroke();
            ctx.restore();
          }
        }
      }
    }

    function loop() {
      ctx.clearRect(0, 0, W, H);
      drawConnections();
      particles.forEach(p => { p.update(); p.draw(); });
      animId = requestAnimationFrame(loop);
    }
    loop();
  }

  // ── Load popular content ──────────────────────────────────────────
  async function loadPopular() {
    const grid = document.getElementById('popular-grid');
    if (!grid) return;
    try {
      const res = await API.trending();
      const items = res.data?.results || [];
      if (!items.length) {
        grid.innerHTML = `<div style="grid-column:1/-1;text-align:center;padding:60px 20px;color:#555">
          <i class="fa-solid fa-film" style="font-size:2.5rem;color:#333;display:block;margin-bottom:12px"></i>
          <p>Konten tidak tersedia saat ini</p></div>`;
        return;
      }
      grid.innerHTML = items.slice(0, 8).map(item => `
        <div class="popular-card" onclick="handleCardClick('${item.slug}')">
          <div class="popular-card-img">
            <img src="${item.thumbnail || ''}" alt="${item.title}" loading="lazy" onerror="this.src='/static/img/placeholder.jpg'">
            <div class="popular-card-overlay">
              <div class="lock-icon"><i class="fa-solid fa-lock"></i></div>
              <div class="lock-text">Login untuk menonton</div>
            </div>
          </div>
          <div class="popular-card-info">
            <div class="popular-card-badge">${item.type || 'DONGHUA'}</div>
            <div class="popular-card-title">${item.title}</div>
            <div class="popular-card-meta">${item.status || ''}</div>
          </div>
        </div>`).join('');
    } catch {}
  }

  function handleCardClick(slug) {
    if (Auth.isLoggedIn()) {
      window.location.href = '/app/anime/' + slug;
    } else {
      window.location.href = '/login';
    }
  }
  window.handleCardClick = handleCardClick;

  async function loadStats() {
    try {
      const res = await fetch('/v1/stats').then(r => r.json());
      const d = res.data || {};
      const el = document.getElementById('stat-anime');
      if (el && d.total_anime) el.textContent = d.total_anime.toLocaleString() + '+';
      const el2 = document.getElementById('stat-ep');
      if (el2 && d.total_episodes) el2.textContent = d.total_episodes.toLocaleString() + '+';
    } catch {}
  }

  // Smooth scroll
  document.querySelectorAll('a[href^="#"]').forEach(a => {
    a.addEventListener('click', e => {
      e.preventDefault();
      const target = document.querySelector(a.getAttribute('href'));
      if (target) target.scrollIntoView({ behavior: 'smooth' });
    });
  });

  // Neon navbar scroll effect
  const navbar = document.getElementById('navbar');
  window.addEventListener('scroll', () => {
    navbar?.classList.toggle('scrolled', window.scrollY > 50);
  });

  // Mobile nav
  const hamburger = document.getElementById('hamburger');
  const mobileMenu = document.getElementById('mobile-menu');
  hamburger?.addEventListener('click', () => mobileMenu?.classList.toggle('open'));

  loadPopular();
  loadStats();
})();
