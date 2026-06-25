'use strict';

if (!document.getElementById('app-page')) {
  // not on app page
} else {
(function () {

if (!Auth.isLoggedIn()) {
  window.location.href = '/login?next=' + encodeURIComponent(location.pathname + location.search);
}

const user = Auth.user;
const $ = id => document.getElementById(id);
const content = $('main-content');

// ── Page Transition (top bar only) ────────────────────────────────
const pageLoader = $('page-loader');
function showPageLoader() {
  if (!pageLoader) return;
  const bar = pageLoader.querySelector('.loader-bar');
  if (bar) bar.style.width = '0%';
  pageLoader.classList.add('active');
  requestAnimationFrame(() => requestAnimationFrame(() => {
    if (bar) bar.style.width = '70%';
  }));
}
function hidePageLoader() {
  if (!pageLoader) return;
  const bar = pageLoader.querySelector('.loader-bar');
  if (bar) bar.style.width = '100%';
  setTimeout(() => {
    pageLoader.classList.remove('active');
    setTimeout(() => { if (bar) bar.style.width = '0%'; }, 150);
  }, 250);
}

// ── Helpers ────────────────────────────────────────────────────────
function relTime(dateStr) {
  if (!dateStr) return '';
  let d;
  const mdy = dateStr.match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})$/);
  if (mdy) d = new Date(+mdy[3], +mdy[1]-1, +mdy[2]);
  else d = new Date(dateStr.replace(' ', 'T'));
  if (isNaN(d.getTime())) return dateStr;
  const diff = Date.now() - d.getTime();
  const s = Math.floor(diff/1000);
  if (s < 60)  return 'Baru saja';
  const m = Math.floor(s/60);
  if (m < 60)  return m + ' menit lalu';
  const h = Math.floor(m/60);
  if (h < 24)  return h + ' jam lalu';
  const day = Math.floor(h/24);
  if (day < 7) return day + ' hari lalu';
  const wk = Math.floor(day/7);
  if (wk < 5)  return wk + ' minggu lalu';
  const mo = Math.floor(day/30.5);
  if (mo < 12) return mo + ' bulan lalu';
  return Math.floor(day/365) + ' tahun lalu';
}

function fmtNum(n) {
  n = +n || 0;
  if (n >= 1e6) return (n/1e6).toFixed(1) + 'jt';
  if (n >= 1e3) return (n/1e3).toFixed(1) + 'rb';
  return n.toString();
}

function escHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')
    .replace(/"/g,'&quot;').replace(/\n/g,'<br>');
}

const QUAL_MAP = {
  mobile:'144p', lowest:'240p', low:'360p', sd:'480p',
  hd:'720p', full:'1080p (FHD)', quad:'1440p (2K)', ultra:'2160p (4K)'
};
function fmtQuality(q) {
  if (!q) return '';
  const lq = q.toLowerCase().trim();
  return QUAL_MAP[lq] || q.toUpperCase();
}

// ── Router ─────────────────────────────────────────────────────────
const Router = {
  routes: [],
  add(pattern, handler) { this.routes.push({ pattern, handler }); },
  match(path) {
    for (const r of this.routes) {
      const keys = [];
      const regexStr = r.pattern
        .replace(/:[^/]+/g, m => { keys.push(m.slice(1)); return '([^/]+)'; })
        .replace(/\//g, '\\/');
      const regex = new RegExp('^' + regexStr + '$');
      const m = path.match(regex);
      if (m) {
        const params = {};
        keys.forEach((k, i) => params[k] = decodeURIComponent(m[i+1]));
        return { handler: r.handler, params };
      }
    }
    return null;
  },
  navigate(path, push = true) {
    if (push) history.pushState({}, '', path);
    // Always close notification panel before navigating
    const panel = document.getElementById('notif-panel');
    const overlay = document.getElementById('notif-overlay');
    if (panel) panel.classList.remove('open');
    if (overlay) overlay.classList.remove('open');
    showPageLoader();
    const m = this.match(path);
    setActiveNav(path);
    if (m) m.handler(m.params);
    else renderNotFound();
  },
  init() {
    window.addEventListener('popstate', () => this.navigate(location.pathname, false));
    document.addEventListener('click', e => {
      const a = e.target.closest('[data-link]');
      if (a) { e.preventDefault(); this.navigate(a.dataset.link); }
    });
    this.navigate(location.pathname, false);
  }
};

function setActiveNav(path) {
  document.querySelectorAll('.nav-item[data-path]').forEach(el => {
    const ep = el.dataset.path;
    const isActive = path === ep || (ep !== '/app' && path.startsWith(ep));
    el.classList.toggle('active', isActive);
  });
}

function go(path) { Router.navigate(path); }
window.go = go;

// ── User Setup ─────────────────────────────────────────────────────
(function setupUser() {
  const uname = $('sidebar-username');
  if (uname && user) uname.textContent = user.username;
  const av = $('sidebar-avatar');
  if (av && user) av.innerHTML = user.avatar || '<i class="fa-solid fa-dragon"></i>';
  const avb = $('bottomnav-avatar');
  if (avb && user) avb.innerHTML = user.avatar || '<i class="fa-solid fa-dragon"></i>';
  const role = $('sidebar-role');
  if (role && user) role.innerHTML = user.role === 'admin'
    ? '<i class="fa-solid fa-crown" style="color:#facc15"></i> Admin'
    : '<i class="fa-solid fa-circle-user"></i> Member';

  // Show admin nav items for admin users
  if (user?.role === 'admin') {
    document.querySelectorAll('.admin-only').forEach(el => el.classList.remove('hidden'));
  }
})();

// ── Loading ────────────────────────────────────────────────────────
function loading() {
  content.innerHTML = `<div class="page-loading"><div class="spinner neon-spinner"></div></div>`;
}

function showToastGlobal(msg, type = 'info') {
  let c = $('toast-container');
  if (!c) { c = document.createElement('div'); c.id = 'toast-container'; document.body.appendChild(c); }
  const t = document.createElement('div');
  t.className = 'toast toast-' + type;
  t.innerHTML = msg;
  c.appendChild(t);
  requestAnimationFrame(() => t.classList.add('show'));
  setTimeout(() => { t.classList.remove('show'); setTimeout(() => t.remove(), 300); }, 3500);
}

function animatePageIn() {
  const el = document.getElementById('main-content');
  if (!el) return;
  el.style.animation = 'none';
  el.offsetHeight;
  el.style.animation = '';
  el.classList.remove('page-enter');
  void el.offsetWidth;
  el.classList.add('page-enter');
}

function afterRender() {
  hidePageLoader();
  animatePageIn();
}

// ── Notification System ────────────────────────────────────────────
const NOTIF_KEY   = 'ac_notifs';
const LAST_EP_KEY = 'ac_last_ep';
let notifList = [];
let notifPollTimer = null;

function loadStoredNotifs() {
  try { notifList = JSON.parse(localStorage.getItem(NOTIF_KEY) || '[]'); } catch { notifList = []; }
}
function saveNotifs() {
  try { localStorage.setItem(NOTIF_KEY, JSON.stringify(notifList.slice(0, 50))); } catch {}
}
function updateNotifBadge() {
  const unread = notifList.filter(n => !n.read).length;
  [$('notif-badge'), $('notif-badge-bottom')].forEach(el => {
    if (!el) return;
    el.textContent = unread;
    el.classList.toggle('hidden', unread === 0);
  });
}
function renderNotifPanel() {
  const list = $('notif-list');
  if (!list) return;
  if (!notifList.length) {
    list.innerHTML = `<div class="notif-empty"><i class="fa-regular fa-bell-slash"></i><p>Belum ada notifikasi</p></div>`;
    return;
  }
  list.innerHTML = notifList.map(n => `
    <div class="notif-item ${n.read ? '' : 'unread'}" onclick="openNotif('${n.slug}','${n.id}')">
      ${n.thumbnail ? `<img class="notif-item-icon" src="${n.thumbnail}" alt="" onerror="this.style.display='none'">` : `<div class="notif-item-icon" style="display:flex;align-items:center;justify-content:center;color:var(--neon)"><i class="fa-solid fa-bell"></i></div>`}
      <div class="notif-item-body">
        <div class="notif-item-title">${escHtml(n.title)}</div>
        <div class="notif-item-sub">${escHtml(n.body)}</div>
        <div class="notif-item-time">${n.time || ''}</div>
      </div>
    </div>`).join('');
}

window.openNotif = function(slug, id) {
  const n = notifList.find(x => x.id === id);
  if (n) n.read = true;
  saveNotifs();
  updateNotifBadge();
  if (slug && slug !== 'undefined') {
    closeNotifPanel();
    go('/app/watch/' + slug);
  }
};

window.clearNotifs = function() {
  notifList = [];
  saveNotifs();
  updateNotifBadge();
  renderNotifPanel();
};

function addNotif(n) {
  const id = n.slug + '_' + Date.now();
  notifList.unshift({ ...n, id, read: false, time: 'Baru saja' });
  saveNotifs();
  updateNotifBadge();
  renderNotifPanel();
  // Browser push notification
  if (Notification.permission === 'granted') {
    try {
      new Notification('AniNova — Episode Baru!', {
        body: n.body || n.title,
        icon: n.thumbnail || '/static/img/icon.png',
        tag: n.slug,
        silent: false
      });
    } catch {}
  }
}

async function pollLatestEpisodes() {
  try {
    const res = await API.latest(1);
    const items = res.data?.results || [];
    if (!items.length) return;
    const latest = items[0];
    const slug = latest.slug || '';
    const lastSlug = localStorage.getItem(LAST_EP_KEY);
    if (lastSlug && slug && slug !== lastSlug) {
      const newEps = [];
      for (const ep of items) {
        if (ep.slug === lastSlug) break;
        newEps.push(ep);
      }
      if (newEps.length > 0) {
        const ep = newEps[0];
        addNotif({ slug: ep.slug, title: ep.name || ep.title || 'Episode Baru', body: `Episode baru tersedia: ${ep.name || ep.slug}`, thumbnail: ep.thumbnail || null });
        showToastGlobal(`<i class="fa-solid fa-bell"></i> Episode baru: <strong>${ep.name || ep.slug}</strong>`, 'success');
      }
    }
    if (slug) localStorage.setItem(LAST_EP_KEY, slug);
  } catch {}
}

async function pollServerNotifications() {
  try {
    const res = await API.getNotifications();
    if (!res.ok) return;
    const srvNotifs = res.data?.notifications || [];
    const srvUnread = res.data?.unread || 0;
    if (!srvNotifs.length) return;
    let changed = false;
    for (const sn of srvNotifs) {
      const key = 'srv_' + sn.id;
      if (!notifList.find(n => n.id === key)) {
        notifList.unshift({ id: key, slug: sn.slug || '', title: sn.title, body: sn.body, thumbnail: sn.thumbnail, read: !!sn.is_read, time: relTime(sn.created_at) });
        changed = true;
        if (!sn.is_read && srvUnread > 0 && 'Notification' in window && Notification.permission === 'granted') {
          try { new Notification('AniNova — ' + sn.title, { body: sn.body || '', icon: sn.thumbnail || '/static/img/icon.png', tag: key, silent: false }); } catch {}
        }
      }
    }
    if (changed) {
      notifList = notifList.slice(0, 50);
      saveNotifs();
      updateNotifBadge();
      renderNotifPanel();
      if (srvUnread > 0) showToastGlobal(`<i class="fa-solid fa-bell"></i> ${srvUnread} notifikasi baru!`, 'success');
    }
  } catch {}
}

function startNotifPolling() {
  if (!localStorage.getItem(LAST_EP_KEY)) {
    API.latest(1).then(res => {
      const items = res.data?.results || [];
      if (items[0]?.slug) localStorage.setItem(LAST_EP_KEY, items[0].slug);
    }).catch(() => {});
  }
  clearInterval(notifPollTimer);
  pollServerNotifications();
  notifPollTimer = setInterval(() => {
    pollLatestEpisodes();
    pollServerNotifications();
  }, 60000);
}

function requestNotifPermission() {
  if ('Notification' in window && Notification.permission === 'default') {
    Notification.requestPermission();
  }
}

function toggleNotifPanel() {
  const panel = $('notif-panel');
  const overlay = $('notif-overlay');
  if (!panel) return;
  const isOpen = panel.classList.contains('open');
  if (isOpen) { closeNotifPanel(); return; }
  notifList.forEach(n => n.read = true);
  saveNotifs();
  updateNotifBadge();
  renderNotifPanel();
  panel.classList.add('open');
  if (overlay) overlay.classList.add('open');
}
function closeNotifPanel() {
  const panel = $('notif-panel');
  const overlay = $('notif-overlay');
  panel?.classList.remove('open');
  overlay?.classList.remove('open');
}
window.toggleNotifPanel = toggleNotifPanel;

// Init notifications
loadStoredNotifs();
updateNotifBadge();
renderNotifPanel();
startNotifPolling();
requestNotifPermission();

// ── Card helpers ────────────────────────────────────────────────────
function animeCard(item) {
  const slug = item.slug || item.anime_slug || '';
  return `
    <div class="anime-card" data-link="/app/anime/${slug}">
      <div class="anime-card-img">
        <img src="${item.thumbnail || '/static/img/placeholder.jpg'}" alt="${item.title||''}" loading="lazy" onerror="this.src='/static/img/placeholder.jpg'">
        ${item.type ? `<div class="anime-card-badge">${item.type}</div>` : ''}
        ${item.status ? `<div class="anime-card-status ${item.status.toLowerCase()}">${item.status}</div>` : ''}
      </div>
      <div class="anime-card-info">
        <div class="anime-card-title">${item.title||''}</div>
        ${item.headline && item.headline !== item.title ? `<div class="anime-card-sub">${item.headline}</div>` : ''}
      </div>
    </div>`;
}

function episodeCard(ep) {
  return `
    <div class="ep-card" data-link="/app/watch/${ep.slug}">
      <div class="ep-card-img">
        <img src="${ep.thumbnail||'/static/img/placeholder.jpg'}" alt="${ep.name||''}" loading="lazy" onerror="this.src='/static/img/placeholder.jpg'">
        <div class="ep-play-icon"><i class="fa-solid fa-circle-play"></i></div>
      </div>
      <div class="ep-card-info">
        <div class="ep-card-title">${ep.name||''}</div>
        <div class="ep-card-date">${ep.date ? relTime(ep.date) : ''}</div>
      </div>
    </div>`;
}

// ══════════════════════════════════════════════════════════════════
// PAGE: HOME  — WeTV-style streaming app layout
// ══════════════════════════════════════════════════════════════════
function skCards(n, ranked) {
  return Array.from({length: n}, (_,i) => `
    <div class="sk-card">
      <div class="sk sk-card-img"></div>
      <div class="sk sk-card-lbl"></div>
      <div class="sk sk-card-lbl2"></div>
    </div>`).join('');
}
function homeSkeleton() {
  const uchar = (user?.username || 'U').charAt(0).toUpperCase();
  return `
    <div class="an-home">
      <div class="an-welcome-hd">
        <div class="an-avatar-circle">${uchar}</div>
        <div class="an-welcome-info">
          <div class="an-welcome-txt">Welcome</div>
          <div class="sk" style="height:14px;width:110px;border-radius:7px;margin-top:4px"></div>
        </div>
        <div class="an-hd-actions">
          <button class="an-hd-btn"><i class="fa-solid fa-bell"></i></button>
          <button class="an-hd-btn"><i class="fa-solid fa-magnifying-glass"></i></button>
        </div>
      </div>
      <div class="sk sk-banner"></div>
      <div style="padding:16px 16px 8px"><div class="sk" style="height:14px;width:130px;border-radius:7px"></div></div>
      <div style="display:flex;gap:10px;padding:4px 16px;margin-bottom:12px">
        ${Array.from({length:5},()=>'<div class="sk" style="height:28px;width:80px;border-radius:100px"></div>').join('')}
      </div>
      <div style="padding:0 16px 8px"><div class="sk" style="height:14px;width:160px;border-radius:7px"></div></div>
      <div style="display:flex;gap:12px;padding:0 16px;overflow:hidden;margin-bottom:16px">
        ${Array.from({length:3},()=>'<div class="sk" style="flex-shrink:0;width:200px;height:112px;border-radius:12px"></div>').join('')}
      </div>
      <div style="padding:0 16px 8px"><div class="sk" style="height:14px;width:140px;border-radius:7px"></div></div>
      <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:8px;padding:0 16px">
        ${Array.from({length:6},()=>'<div class="sk" style="aspect-ratio:3/4;border-radius:10px"></div>').join('')}
      </div>
    </div>`;
}

async function renderHome() {
  content.innerHTML = homeSkeleton();
  try {
    const [homeRes, histRes, latestRes] = await Promise.all([
      API.home(),
      API.getHistory().catch(() => ({ data: { results: [] } })),
      API.latest(1).catch(() => ({ data: { results: [] } }))
    ]);
    const sections   = homeRes.data?.results || homeRes.results || [];
    const histItems  = histRes.data?.results || histRes.data?.history || [];
    const latestEps  = latestRes.data?.results || [];

    function cardLink(card) {
      const s = card.slug || '';
      return s.includes('-episode-') ? `/app/watch/${s}` : `/app/anime/${s}`;
    }
    function fmtSection(name) {
      const map = {
        latest:'Episode Terbaru', terbaru:'Episode Terbaru',
        popular:'AniNova Hot', populer:'AniNova Hot',
        trending:'Trending', completed:'Sudah Tamat', complete:'Sudah Tamat'
      };
      const k = (name||'').toLowerCase().replace(/_/g,'');
      return map[k] || name.replace(/_/g,' ').replace(/\b\w/g,c=>c.toUpperCase());
    }
    function sectionIcon(name) {
      const n = (name||'').toLowerCase();
      if (n.includes('terbaru')||n.includes('latest')) return '<i class="fa-solid fa-bolt-lightning" style="color:var(--neon)"></i>';
      if (n.includes('trending')) return '<i class="fa-solid fa-arrow-trend-up" style="color:#ff7b00"></i>';
      if (n.includes('populer')||n.includes('popular')) return '<i class="fa-solid fa-fire" style="color:#ff7b00"></i>';
      if (n.includes('complete')||n.includes('tamat')) return '<i class="fa-solid fa-circle-check" style="color:#22c55e"></i>';
      return '<i class="fa-solid fa-star" style="color:#f5c518"></i>';
    }

    // Hero cards from first section
    const heroCards = [];
    for (const sec of sections) {
      if (sec.cards?.length > 0) { heroCards.push(...sec.cards.slice(0,6)); break; }
    }

    // ── Hero carousel ───────────────────────────────────────────────
    const bannerHtml = heroCards.length ? `
      <div class="an-banner" id="an-banner">
        <div class="an-banner-track" id="an-banner-track">
          ${heroCards.map((card) => `
            <div class="an-slide" data-link="${cardLink(card)}" style="background-image:url('${card.thumbnail||''}')">
              <div class="an-slide-overlay"></div>
              <div class="an-slide-info">
                <div class="an-slide-tags">
                  ${card.type?`<span class="an-slide-badge">${card.type}</span>`:''}
                  ${card.status?`<span class="an-slide-badge an-slide-badge-status">${card.status}</span>`:''}
                </div>
                <h2 class="an-slide-title">${card.title||''}</h2>
                ${card.headline&&card.headline!==card.title?`<p class="an-slide-sub">${card.headline.slice(0,90)}${card.headline.length>90?'...':''}</p>`:''}
                <div class="an-slide-btns">
                  <button class="an-btn-play" onclick="event.stopPropagation();go('${cardLink(card)}')">
                    <i class="fa-solid fa-play"></i> Tonton
                  </button>
                  <button class="an-btn-add" onclick="event.stopPropagation();go('/app/anime/${card.slug||card.anime_slug||''}')">
                    <i class="fa-solid fa-circle-info"></i> Detail
                  </button>
                </div>
              </div>
            </div>`).join('')}
        </div>
        <div class="an-banner-dots" id="an-banner-dots">
          ${heroCards.map((_,i)=>`<span class="an-dot${i===0?' active':''}" data-idx="${i}"></span>`).join('')}
        </div>
        ${heroCards.length>1?`
        <button class="an-banner-arr an-banner-prev" id="an-banner-prev"><i class="fa-solid fa-chevron-left"></i></button>
        <button class="an-banner-arr an-banner-next" id="an-banner-next"><i class="fa-solid fa-chevron-right"></i></button>`:''}
      </div>` : '';

    // ── Pengumuman (announcement) ───────────────────────────────────
    const annHtml = `
      <div class="an-ann-section">
        <h3 class="an-section-title">Pengumuman</h3>
        <div class="an-ann-card">
          <i class="fa-solid fa-bullhorn an-ann-icon"></i>
          <p class="an-ann-text">Kami berupaya memberikan yang terbaik untuk Anda. Terimakasih atas dukungannya <i class="fa-solid fa-heart" style="color:#e50914"></i></p>
        </div>
      </div>`;

    // ── Semua Genre chips ───────────────────────────────────────────
    const genres = ['Donghua','Anime','Movie','Action','Fantasy','Martial Arts','Adventure','Romance','Comedy','Thriller'];
    const genreHtml = `
      <div class="an-genre-section">
        <h3 class="an-section-title">Semua Genre</h3>
        <div class="an-genre-scroll" id="an-genre-scroll">
          ${genres.map((g,i)=>`<button class="an-genre-chip${i===0?' active':''}" data-genre="${g}">${g}</button>`).join('')}
        </div>
      </div>`;

    // ── Lanjutkan Nonton (history) ──────────────────────────────────
    const histHtml = histItems.length ? `
      <div class="an-cont-section">
        <div class="an-section-hd">
          <span class="an-section-title-sm">Lanjutkan Nonton</span>
          <span class="an-see-all" data-link="/app/profile">Lihat History <i class="fa-solid fa-chevron-right"></i></span>
        </div>
        <div class="an-hist-scroll">
          ${histItems.slice(0,6).map(h => `
            <div class="an-hist-card" data-link="/app/watch/${h.episode_slug||h.slug||''}">
              <div class="an-hist-img">
                <img src="${h.thumbnail||h.episode_thumbnail||'/static/img/placeholder.jpg'}" loading="lazy" onerror="this.src='/static/img/placeholder.jpg'">
                <div class="an-hist-overlay">
                  <div class="an-hist-ep-label">${h.episode_name||h.name||''}</div>
                  <div class="an-hist-title">${h.anime_title||h.title||''}</div>
                </div>
                <div class="an-hist-play"><i class="fa-solid fa-play"></i></div>
              </div>
            </div>`).join('')}
        </div>
      </div>` : '';

    // ── Episode Terbaru 3-col grid ──────────────────────────────────
    const terbaruCards = latestEps.length ? latestEps : (
      sections.find(s=>(s.section||'').toLowerCase().includes('terbaru'))?.cards || []
    );
    const terbaruHtml = terbaruCards.length ? `
      <div class="an-terbaru-section">
        <div class="an-section-hd">
          <span class="an-section-title-sm"><i class="fa-solid fa-bolt-lightning" style="color:var(--neon)"></i> Episode Terbaru</span>
          <span class="an-see-all" data-link="/app/browse">Lihat Semua <i class="fa-solid fa-chevron-right"></i></span>
        </div>
        <div class="an-terbaru-grid">
          ${terbaruCards.slice(0,9).map(card => {
            const link = cardLink(card);
            const isNew = true;
            return `
              <div class="an-tb-card" data-link="${link}">
                <div class="an-tb-img">
                  <img src="${card.thumbnail||'/static/img/placeholder.jpg'}" loading="lazy" onerror="this.src='/static/img/placeholder.jpg'">
                  ${isNew?'<div class="an-tb-new">New</div>':''}
                  ${card.rating?`<div class="an-tb-rating"><i class="fa-solid fa-star"></i>${card.rating}</div>`:''}
                </div>
                ${card.eps?`<div class="an-tb-eps">${card.eps} Eps</div>`:''}
                <div class="an-tb-title">${card.title||''}</div>
              </div>`;
          }).join('')}
        </div>
      </div>` : '';

    // ── Section rows (from home API, skip first which is in banner) ─
    const rowsHtml = sections.map((sec, idx) => {
      const ranked = idx === 0;
      return `
        <div class="an-row" data-section="${(sec.section||'').toLowerCase()}">
          <div class="an-row-hd">
            <span class="an-row-title">${sectionIcon(sec.section)} ${fmtSection(sec.section)}</span>
            <span class="an-row-more" data-link="/app/browse">Lainnya <i class="fa-solid fa-chevron-right"></i></span>
          </div>
          <div class="an-scroll" id="an-row-${idx}">
            ${(sec.cards||[]).map((card,ci)=>`
              <div class="an-card${ranked?' an-card-rank':''}" data-link="${cardLink(card)}">
                ${ranked?`<div class="an-rank">${ci+1}</div>`:''}
                <div class="an-card-img">
                  <img src="${card.thumbnail||'/static/img/placeholder.jpg'}" alt="${card.title||''}" loading="lazy" onerror="this.src='/static/img/placeholder.jpg'">
                  ${card.type?`<span class="an-card-type">${card.type}</span>`:''}
                  ${card.eps?`<span class="an-card-ep">${card.eps}ep</span>`:''}
                  <div class="an-card-play-ov"><i class="fa-solid fa-play"></i></div>
                </div>
                <div class="an-card-name">${card.title||''}</div>
              </div>`).join('')}
          </div>
        </div>`;
    }).join('');

    // ── Username first char for avatar ─────────────────────────────
    const uname  = user?.username || user?.email || 'U';
    const uchar  = uname.charAt(0).toUpperCase();
    const unread = document.querySelector('.notif-badge')?.textContent || '';

    content.innerHTML = `
      <div class="an-home">
        <!-- Welcome Header -->
        <div class="an-welcome-hd">
          <div class="an-avatar-circle" onclick="go('/app/profile')">${uchar}</div>
          <div class="an-welcome-info">
            <div class="an-welcome-txt">Welcome</div>
            <div class="an-welcome-name">${escHtml(uname)}</div>
          </div>
          <div class="an-hd-actions">
            <button class="an-hd-btn" onclick="toggleNotifPanel()" title="Notifikasi">
              <i class="fa-solid fa-bell"></i>
              <span class="an-notif-dot" id="an-notif-dot" style="display:none"></span>
            </button>
            <button class="an-hd-btn" id="an-search-toggle" title="Cari">
              <i class="fa-solid fa-magnifying-glass"></i>
            </button>
          </div>
        </div>

        <!-- Search bar -->
        <div class="an-search-bar" id="an-search-bar" style="display:none">
          <div class="an-searchbox">
            <i class="fa-solid fa-magnifying-glass"></i>
            <input type="text" id="home-q" class="an-q" placeholder="Cari anime atau donghua..." autocomplete="off">
            <button class="an-close-search" id="an-close-search"><i class="fa-solid fa-xmark"></i></button>
          </div>
        </div>
        <div id="home-search-results" class="an-search-results" style="display:none"></div>

        <div id="an-main">
          ${bannerHtml}
          ${annHtml}
          ${genreHtml}
          ${histHtml}
          ${terbaruHtml}
          <div class="an-rows" id="an-rows">${rowsHtml||''}</div>
        </div>
      </div>`;

    // update notif dot
    const dot = $('an-notif-dot');
    if (dot) {
      const badge = document.querySelector('.notif-badge');
      if (badge && badge.style.display !== 'none' && badge.textContent !== '0') dot.style.display = '';
    }

    // ── Search toggle ───────────────────────────────────────────────
    $('an-search-toggle')?.addEventListener('click', () => {
      const bar = $('an-search-bar');
      if (bar) { bar.style.display=''; $('home-q')?.focus(); }
    });
    $('an-close-search')?.addEventListener('click', () => {
      const bar=$('an-search-bar'), sr=$('home-search-results'), main=$('an-main');
      if (bar) bar.style.display='none';
      if ($('home-q')) $('home-q').value='';
      if (sr) sr.style.display='none';
      if (main) main.style.display='';
    });
    let _timer;
    $('home-q')?.addEventListener('input', () => {
      clearTimeout(_timer);
      const q=$('home-q')?.value.trim(), sr=$('home-search-results'), main=$('an-main');
      if (!q) { if(sr) sr.style.display='none'; if(main) main.style.display=''; return; }
      _timer = setTimeout(async () => {
        try {
          if(sr){sr.innerHTML=`<div class="page-loading"><div class="spinner neon-spinner"></div></div>`;sr.style.display='';}
          if(main) main.style.display='none';
          const res=await API.search(q); const items=res.data?.results||[];
          if(sr) sr.innerHTML=items.length
            ?`<div style="padding:16px"><div class="card-grid">${items.map(animeCard).join('')}</div></div>`
            :`<div class="empty-state"><i class="fa-solid fa-magnifying-glass-minus"></i><p>Tidak ditemukan: "<strong>${escHtml(q)}</strong>"</p></div>`;
        } catch {}
      }, 400);
    });
    $('home-q')?.addEventListener('keydown', e=>{ if(e.key==='Escape') $('an-close-search')?.click(); });

    // ── Genre chips ─────────────────────────────────────────────────
    document.querySelectorAll('.an-genre-chip').forEach(btn => {
      btn.addEventListener('click', () => {
        document.querySelectorAll('.an-genre-chip').forEach(b=>b.classList.remove('active'));
        btn.classList.add('active');
        const g = btn.dataset.genre;
        if (g === 'Donghua') go('/app/donghua');
        else if (g === 'Movie') go('/app/movie');
        else if (g === 'Anime') go('/app/browse?type=Anime');
        else go(`/app/browse?q=${encodeURIComponent(g)}`);
      });
    });

    // ── See-all links ───────────────────────────────────────────────
    document.querySelectorAll('.an-see-all[data-link], .an-row-more[data-link]').forEach(el => {
      el.addEventListener('click', () => go(el.dataset.link));
    });

    // ── Click cards ─────────────────────────────────────────────────
    document.querySelectorAll('.an-card, .an-hist-card, .an-tb-card, .an-slide').forEach(el => {
      el.addEventListener('click', () => { const l=el.dataset.link; if(l) go(l); });
    });

    // ── Banner carousel ─────────────────────────────────────────────
    if (heroCards.length > 1) {
      let cur = 0;
      const track = $('an-banner-track');
      function goSlide(n) {
        cur = (n + heroCards.length) % heroCards.length;
        if (track) track.style.transform = `translateX(-${cur*100}%)`;
        document.querySelectorAll('.an-dot').forEach((d,i)=>d.classList.toggle('active',i===cur));
      }
      $('an-banner-prev')?.addEventListener('click', e=>{ e.stopPropagation(); goSlide(cur-1); });
      $('an-banner-next')?.addEventListener('click', e=>{ e.stopPropagation(); goSlide(cur+1); });
      document.querySelectorAll('.an-dot').forEach(d=>d.addEventListener('click',()=>goSlide(+d.dataset.idx)));
      let auto = setInterval(()=>goSlide(cur+1), 5000);
      $('an-banner')?.addEventListener('mouseenter',()=>clearInterval(auto));
      $('an-banner')?.addEventListener('mouseleave',()=>{ auto=setInterval(()=>goSlide(cur+1),5000); });
    }

    afterRender();
  } catch(err) {
    content.innerHTML=`<div class="error-page"><p>Gagal memuat konten. <button class="btn btn-sm" onclick="renderHome()">Coba lagi</button></p></div>`;
    hidePageLoader();
  }
}

window.scrollRow = function(id, dir) {
  const el = $(id);
  if (el) el.scrollBy({ left: dir * 320, behavior: 'smooth' });
};

// ══════════════════════════════════════════════════════════════════
// PAGE: BROWSE
// ══════════════════════════════════════════════════════════════════
let browseState = { q: '', genre: '', type: '', status: '', page: 1, loading: false };

async function renderBrowse() {
  const _urlp = new URLSearchParams(location.search);
  browseState = { q: _urlp.get('q')||'', genre: '', type: _urlp.get('type')||'', status: _urlp.get('status')||'', page: 1, loading: false };
  content.innerHTML = `
    <div class="browse-page">
      <div class="browse-topbar">
        <div class="browse-search-wrap">
          <i class="fa-solid fa-magnifying-glass browse-search-icon"></i>
          <input type="text" id="search-input" class="browse-input"
            placeholder="Cari donghua atau anime..." value="${browseState.q}" autocomplete="off">
          ${browseState.q ? `<button class="browse-clear" id="btn-clear-q"><i class="fa-solid fa-xmark"></i></button>` : ''}
        </div>
        <div class="browse-filters">
          <select id="filter-type" class="browse-select">
            <option value="">Semua Tipe</option>
            <option value="Donghua">Donghua</option>
            <option value="Anime">Anime</option>
            <option value="ONA">ONA</option>
          </select>
          <select id="filter-status" class="browse-select">
            <option value="">Semua Status</option>
            <option value="Ongoing">Ongoing</option>
            <option value="Completed">Completed</option>
          </select>
        </div>
      </div>
      <div id="browse-results" class="card-grid browse-grid"></div>
      <div id="browse-pagination" class="browse-pages"></div>
    </div>`;

  if (browseState.q && $('search-input')) $('search-input').value = browseState.q;
  if (browseState.type && $('filter-type')) $('filter-type').value = browseState.type;
  if (browseState.status && $('filter-status')) $('filter-status').value = browseState.status;

  $('filter-type').addEventListener('change', e => { browseState.type = e.target.value; browseState.page = 1; loadBrowse(); });
  $('filter-status').addEventListener('change', e => { browseState.status = e.target.value; browseState.page = 1; loadBrowse(); });

  let searchTimer;
  $('search-input').addEventListener('input', e => {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(() => { browseState.q = e.target.value.trim(); browseState.page = 1; loadBrowse(); }, 450);
  });
  $('search-input').addEventListener('keydown', e => { if (e.key === 'Enter') { clearTimeout(searchTimer); browseState.q = $('search-input').value.trim(); browseState.page = 1; loadBrowse(); } });

  loadBrowse();
  afterRender();
}

function doSearch() {
  browseState.q = $('search-input')?.value.trim() || '';
  browseState.page = 1;
  loadBrowse();
}

async function loadBrowse() {
  const resultsEl = $('browse-results');
  const pageEl    = $('browse-pagination');
  if (!resultsEl) return;
  browseState.loading = true;
  resultsEl.innerHTML = Array.from({length:16}, () => '<div class="skeleton-card"></div>').join('');

  try {
    let data, total = 0, pages = 1;
    if (browseState.q) {
      const res = await API.search(browseState.q);
      data = res.data?.results || [];
      total = data.length;
    } else {
      const params = { page: browseState.page, limit: 28 };
      if (browseState.type)   params.type = browseState.type;
      if (browseState.status) params.status = browseState.status;
      const res = await API.animeList(params);
      data = res.data?.results || [];
      total = res.data?.total || 0; pages = res.data?.total_pages || 1;
    }

    resultsEl.innerHTML = data.length
      ? data.map(animeCard).join('')
      : `<div class="empty-state" style="grid-column:1/-1"><i class="fa-solid fa-magnifying-glass-minus"></i><p>Tidak ada hasil</p></div>`;
    resultsEl.querySelectorAll('.anime-card').forEach(c => c.addEventListener('click', () => go(c.dataset.link)));

    if (pageEl) {
      pageEl.innerHTML = pages > 1 ? buildPagination(browseState.page, pages) : '';
      pageEl.querySelectorAll('[data-page]').forEach(btn => {
        btn.addEventListener('click', () => { browseState.page = +btn.dataset.page; loadBrowse(); content.scrollTo({top:0,behavior:'smooth'}); });
      });
    }
  } catch { if(resultsEl) resultsEl.innerHTML = `<div class="empty-state" style="grid-column:1/-1"><p>Gagal memuat</p></div>`; }
  browseState.loading = false;
}

function buildPagination(cur, total) {
  const pages = [];
  if (cur > 1) pages.push(`<button class="page-btn" data-page="${cur-1}">‹ Prev</button>`);
  const start = Math.max(1, cur - 2), end = Math.min(total, cur + 2);
  for (let i = start; i <= end; i++) {
    pages.push(`<button class="page-btn ${i===cur?'active':''}" data-page="${i}">${i}</button>`);
  }
  if (cur < total) pages.push(`<button class="page-btn" data-page="${cur+1}">Next ›</button>`);
  return pages.join('');
}

// ══════════════════════════════════════════════════════════════════
// PAGE: ANIME DETAIL
// ══════════════════════════════════════════════════════════════════
async function renderAnime({ slug }) {
  loading();
  try {
    const res = await API.animeDetail(slug);
    const d = res.data?.result || res.data || {};
    if (!d.name) { renderNotFound(); return; }

    const eps        = d.episode || [];
    const lastEp     = eps[0];       // newest
    const firstEp    = eps[eps.length - 1]; // oldest
    const animeType  = d.type || d.tipe || '';
    const animeStatus= d.status || '';
    const synParas   = d.sinopsis?.paragraphs || [];
    const synopsis   = synParas.filter(Boolean).join('\n\n');
    const genreList  = d.genre || [];

    // Find last watched episode from history (optional)
    let continueEp = lastEp;

    // Tags row 1: rating, type, status, year, release
    const tags1 = [
      d.rating ? `<span class="ani-tag-pill ani-tag-rating"><i class="fa-solid fa-star"></i> ${d.rating}</span>` : '',
      animeType ? `<span class="ani-tag-pill">${animeType}</span>` : '',
      animeStatus ? `<span class="ani-tag-pill">${animeStatus}</span>` : '',
      d.dirilis||d.released ? `<span class="ani-tag-pill">${d.dirilis||d.released}</span>` : '',
    ].filter(Boolean).join('');

    // Tags row 2: eps count, duration, studio
    const tags2 = [
      eps.length ? `<span class="ani-tag-pill">Eps ${eps.length}</span>` : '',
      d.durasi||d.duration ? `<span class="ani-tag-pill">${d.durasi||d.duration}</span>` : '',
      d.studio ? `<span class="ani-tag-pill">${d.studio}</span>` : '',
    ].filter(Boolean).join('');

    // Ep number for continue button
    let continueNum = '';
    if (continueEp) {
      const rawN = String(continueEp.episode||'');
      const nm = rawN.match(/\d+/);
      continueNum = nm ? nm[0] : '1';
    }

    // Build episode list (as row items like reference)
    let sortAsc = false;
    const epListHtml = eps.map((ep, i) => {
      const rawN = String(ep.episode||'');
      const nm = rawN.match(/\d+/);
      const num = nm ? +nm[0] : (eps.length - i);
      return `<div class="ani-ep-row" data-link="/app/watch/${ep.slug}">
        <div class="ani-ep-row-left">
          <i class="fa-solid fa-play ani-ep-row-icon"></i>
          <span class="ani-ep-row-num">Episode ${num}</span>
        </div>
        <span class="ani-ep-row-date">${relTime(ep.date)}</span>
      </div>`;
    }).join('');

    // Related series (if any)
    const related = d.related || d.related_series || [];

    content.innerHTML = `
      <div class="page ani-detail-v2">

        <!-- Full-width hero -->
        <div class="ani-hero-wrap">
          <img class="ani-hero-bg-img" src="${d.thumbnail||'/static/img/placeholder.jpg'}" onerror="this.src='/static/img/placeholder.jpg'" alt="${d.name}">
          <div class="ani-hero-grad"></div>
          <button class="ani-back-btn" onclick="history.go(-1)"><i class="fa-solid fa-arrow-left"></i></button>
          ${d.jadwal ? `<div class="ani-sched-badge"><i class="fa-regular fa-calendar"></i> ${d.jadwal}</div>` : ''}
        </div>

        <!-- Body -->
        <div class="ani-body">
          <h1 class="ani-v2-title">${escHtml(d.name)}</h1>
          ${d.judul_lain||d.alt_title ? `<div class="ani-v2-alt">${escHtml(d.judul_lain||d.alt_title)}</div>` : ''}

          <!-- Tags row 1 -->
          ${tags1 ? `<div class="ani-tags-row">${tags1}</div>` : ''}
          <!-- Tags row 2 -->
          ${tags2 ? `<div class="ani-tags-row">${tags2}</div>` : ''}

          <!-- Action buttons -->
          <div class="ani-v2-actions">
            ${continueEp ? `<button class="ani-btn-play-v2" onclick="go('/app/watch/${continueEp.slug}')">
              <i class="fa-solid fa-play"></i> ${continueNum ? `Lanjut Eps ${continueNum}` : 'Mulai Tonton'}
            </button>` : ''}
            <button class="ani-btn-wl-v2" id="btn-watchlist" onclick="toggleWatchlist('${slug}')">
              <i class="fa-regular fa-bookmark"></i> <span id="wl-label">Simpan</span>
            </button>
          </div>

          <!-- Genre tags -->
          ${genreList.length ? `
          <div class="ani-genre-tags-row">
            ${genreList.map(g=>`<span class="ani-genre-chip-sm">${g}</span>`).join('')}
          </div>` : ''}

          <!-- Synopsis -->
          ${synopsis ? `
          <div class="ani-syn-wrap">
            <h2 class="ani-sub-heading">Sinopsis</h2>
            <p class="ani-syn-txt" id="ani-syn">${escHtml(synopsis)}</p>
            <button class="ani-syn-more" id="btn-syn-more">Selengkapnya <i class="fa-solid fa-chevron-down"></i></button>
          </div>` : ''}

          <!-- Related series -->
          ${related.length ? `
          <div class="ani-related-wrap">
            <h2 class="ani-sub-heading">Related Series</h2>
            <div class="ani-related-scroll">
              ${related.map(r=>`
                <div class="ani-rel-card" data-link="/app/anime/${r.slug||''}">
                  <img src="${r.thumbnail||'/static/img/placeholder.jpg'}" onerror="this.src='/static/img/placeholder.jpg'" loading="lazy">
                  ${r.rating?`<div class="ani-rel-rating"><i class="fa-solid fa-star"></i>${r.rating}</div>`:''}
                  <div class="ani-rel-title">${escHtml(r.title||r.name||'')}</div>
                </div>`).join('')}
            </div>
          </div>` : ''}

          <!-- Episode section -->
          <div class="ani-eps-section">
            <div class="ani-eps-hd">
              <span class="ani-sub-heading">${eps.length} Episode</span>
              <div class="ani-eps-tools">
                <button class="ani-eps-tool-btn" id="btn-ep-sort" title="Urutkan"><i class="fa-solid fa-arrow-down-wide-short"></i></button>
              </div>
            </div>
            <div class="ani-ep-rows" id="ep-rows">${epListHtml}</div>
          </div>

          <!-- Comments -->
          <div class="ani-comments-section">
            <h2 class="ani-sub-heading"><i class="fa-solid fa-comments" style="color:var(--neon)"></i> Komentar</h2>
            <div class="ani-comment-input-row">
              <div class="ani-comment-av">${user?.username?.charAt(0).toUpperCase()||'<i class="fa-solid fa-dragon"></i>'}</div>
              <div class="ani-comment-input-wrap">
                <textarea id="comment-input" class="ani-comment-input" placeholder="Tambah komentar..." rows="2" maxlength="500"></textarea>
                <button class="ani-comment-send" id="btn-send-comment" onclick="sendComment('${slug}')">
                  <i class="fa-solid fa-paper-plane"></i>
                </button>
              </div>
            </div>
            <div id="comments-list" class="ani-comment-list">
              <div class="page-loading"><div class="spinner neon-spinner"></div></div>
            </div>
          </div>
        </div>
      </div>`;

    // Episodes click
    document.querySelectorAll('.ani-ep-row[data-link]').forEach(el => el.addEventListener('click', ()=>go(el.dataset.link)));
    document.querySelectorAll('.ani-rel-card[data-link]').forEach(el => el.addEventListener('click', ()=>go(el.dataset.link)));

    // Synopsis expand
    const syn = $('ani-syn');
    const synBtn = $('btn-syn-more');
    if (syn && synBtn) {
      if (syn.scrollHeight <= 90) synBtn.style.display = 'none';
      synBtn.addEventListener('click', () => {
        syn.classList.toggle('expanded');
        synBtn.innerHTML = syn.classList.contains('expanded')
          ? 'Tutup <i class="fa-solid fa-chevron-up"></i>'
          : 'Selengkapnya <i class="fa-solid fa-chevron-down"></i>';
      });
    }

    // Sort episodes
    $('btn-ep-sort')?.addEventListener('click', () => {
      sortAsc = !sortAsc;
      const rows = $('ep-rows');
      if (!rows) return;
      [...rows.querySelectorAll('.ani-ep-row')].reverse().forEach(el => rows.appendChild(el));
    });

    checkWatchlist(slug);
    loadComments(slug);
    afterRender();
  } catch {
    content.innerHTML = `<div class="error-page"><p>Gagal memuat. <button class="btn btn-sm" onclick="history.go(-1)">Kembali</button></p></div>`;
    hidePageLoader();
  }
}

async function checkWatchlist(slug) {
  try {
    const res = await API.getWatchlist();
    const list = res.data?.results || [];
    updateWatchlistBtn(list.some(w => w.anime_slug === slug));
  } catch {}
}
function updateWatchlistBtn(inList) {
  const btn = $('btn-watchlist');
  if (!btn) return;
  btn.classList.toggle('active', inList);
  btn.title = inList ? 'Hapus dari watchlist' : 'Tambah ke watchlist';
  const label = $('wl-label');
  if (label) {
    label.textContent = inList ? 'Tersimpan' : 'Simpan';
    btn.querySelector('i').className = inList ? 'fa-solid fa-bookmark' : 'fa-regular fa-bookmark';
  } else {
    btn.innerHTML = inList ? '<i class="fa-solid fa-bookmark"></i>' : '<i class="fa-regular fa-bookmark"></i>';
  }
}
async function toggleWatchlist(slug) {
  try {
    const res = await API.getWatchlist();
    const inList = (res.data?.results||[]).some(w => w.anime_slug === slug);
    if (inList) { await API.removeWatchlist(slug); showToastGlobal('Dihapus dari watchlist','info'); updateWatchlistBtn(false); }
    else { await API.addWatchlist(slug); showToastGlobal('Ditambahkan ke watchlist','success'); updateWatchlistBtn(true); }
  } catch { showToastGlobal('Gagal update watchlist','error'); }
}
window.toggleWatchlist = toggleWatchlist;

// ── Comments ───────────────────────────────────────────────────────
async function loadComments(animeSlug) {
  const el = $('comments-list');
  if (!el) return;
  try {
    const res = await API.getComments(animeSlug);
    const comments = res.data?.comments || [];
    if (!comments.length) {
      el.innerHTML = `<div class="empty-state" style="padding:20px 0"><p style="color:var(--text3)">Belum ada komentar. Jadilah yang pertama!</p></div>`;
      return;
    }
    el.innerHTML = comments.map(c => `
      <div class="comment-item" id="cmt-${c.id}">
        <div class="comment-item-av">${c.avatar||'<i class="fa-solid fa-dragon"></i>'}</div>
        <div class="comment-item-body">
          <div class="comment-item-header">
            <span class="comment-item-user">${escHtml(c.username)}</span>
            <span class="comment-item-time">${relTime(c.created_at)}</span>
            ${(user?.role==='admin'||user?.id===c.user_id) ? `<button class="comment-del-btn" onclick="deleteComment(${c.id},'${animeSlug}')"><i class="fa-solid fa-trash"></i></button>` : ''}
          </div>
          <div class="comment-item-text">${escHtml(c.content)}</div>
        </div>
      </div>`).join('');
  } catch {
    el.innerHTML = `<div style="padding:16px;color:var(--text3);text-align:center">Gagal memuat komentar</div>`;
  }
}

window.sendComment = async function(animeSlug) {
  const input = $('comment-input');
  const txt = input?.value.trim();
  if (!txt) return showToastGlobal('Tulis komentar dulu','error');
  const btn = $('btn-send-comment');
  if (btn) { btn.disabled = true; btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i>'; }
  try {
    const res = await API.addComment(animeSlug, txt);
    const ok = res.status === 200 || res.ok || res.data?.status === 'ok';
    if (ok) { if(input) input.value = ''; showToastGlobal('Komentar dikirim!','success'); loadComments(animeSlug); }
    else showToastGlobal(res.data?.message||'Gagal kirim komentar','error');
  } catch { showToastGlobal('Gagal mengirim komentar','error'); }
  if (btn) { btn.disabled = false; btn.innerHTML = '<i class="fa-solid fa-paper-plane"></i> Kirim'; }
};

window.deleteComment = async function(id, animeSlug) {
  try {
    await API.deleteComment(id);
    document.getElementById('cmt-'+id)?.remove();
    showToastGlobal('Komentar dihapus','info');
    const el = $('comments-list');
    if (el && !el.querySelector('.comment-item')) {
      el.innerHTML = `<div class="empty-state" style="padding:20px 0"><p style="color:var(--text3)">Belum ada komentar.</p></div>`;
    }
  } catch { showToastGlobal('Gagal hapus komentar','error'); }
};

// ══════════════════════════════════════════════════════════════════
// PAGE: WATCH  — redesigned like reference app
// ══════════════════════════════════════════════════════════════════
async function renderWatch({ slug }) {
  loading();
  try {
    const [epRes, vidRes] = await Promise.all([API.episode(slug), API.videoSource(slug)]);
    const ep  = epRes.data?.result || {};
    const eps = ep.episode || [];
    const vid = vidRes.result || vidRes || {};

    const rawServers = vid.servers || [];
    const servers = [...rawServers].sort((a, b) =>
      (b.direct_urls?.length > 0 ? 1 : 0) - (a.direct_urls?.length > 0 ? 1 : 0)
    );

    const animeTitle = ep.name || slug;
    const epNum      = slug.match(/episode-(\d+)/)?.[1] || '';
    const animeSlug  = ep.root || slug.match(/^(.+?)-episode-/)?.[1] || '';
    const thumbnail  = ep.thumbnail || '/static/img/placeholder.jpg';

    if (animeSlug) API.addHistory(slug, animeSlug).catch(() => {});
    API.addView(slug).then(res => {
      const cnt = res.data?.count || 0;
      const el = $('wv-views');
      if (el) el.textContent = fmtNum(cnt) + ' ditonton';
    }).catch(() => {});

    // Episode number bubbles (show 20 around current)
    const curIdx = eps.findIndex(e => e.slug === slug || ('/'+e.slug) === slug);
    const epBubbles = eps.map((e, i) => {
      const rawN = String(e.episode||'');
      const nm = rawN.match(/\d+/);
      const num = nm ? +nm[0] : (eps.length - i);
      const isCur = i === curIdx;
      return `<button class="wv-ep-bubble${isCur?' active':''}" data-link="/app/watch/${e.slug}">${num}</button>`;
    }).join('');

    content.innerHTML = `
      <div class="page watch-v2-page">

        <!-- Player -->
        <div class="wv-player-wrap" id="player-wrapper">
          <div class="player-loading"><div class="spinner neon-spinner"></div><p>Memuat video...</p></div>
        </div>

        <!-- Info bar -->
        <div class="wv-info-bar">
          <button class="wv-back-btn" onclick="history.go(-1)"><i class="fa-solid fa-arrow-left"></i></button>
          <img class="wv-thumb" src="${thumbnail}" onerror="this.src='/static/img/placeholder.jpg'" loading="lazy">
          <div class="wv-info-txt">
            <div class="wv-anime-title">${escHtml(animeTitle)}</div>
            <div class="wv-ep-info">
              ${epNum ? `Episode ${epNum} · ` : ''}<span id="wv-views">0 ditonton</span>
            </div>
          </div>
        </div>

        <!-- Action buttons row -->
        <div class="wv-actions-row">
          <button class="wv-action-btn" id="wv-like-btn" title="Suka">
            <i class="fa-regular fa-thumbs-up"></i>
            <span>Suka</span>
          </button>
          <button class="wv-action-btn" id="wv-qual-btn" title="Kualitas">
            <i class="fa-solid fa-sliders"></i>
            <span id="wv-qual-label">Kualitas</span>
          </button>
          <button class="wv-action-btn" id="wv-dl-btn" title="Download">
            <i class="fa-solid fa-download"></i>
            <span>Download</span>
          </button>
          <button class="wv-action-btn" id="wv-share-btn" title="Bagikan">
            <i class="fa-solid fa-share-nodes"></i>
            <span>Bagikan</span>
          </button>
          <button class="wv-action-btn" onclick="go('/app/anime/${animeSlug||''}')" title="Detail Anime">
            <i class="fa-solid fa-circle-info"></i>
            <span>Detail</span>
          </button>
        </div>

        <!-- Episode list -->
        <div class="wv-ep-section">
          <div class="wv-ep-hd">
            <span class="wv-ep-hd-title">Episode List <span class="wv-ep-count">${eps.length} Ep</span></span>
            <button class="wv-see-all" onclick="showEpSheet()">Lihat Semua <i class="fa-solid fa-chevron-right"></i></button>
          </div>
          <div class="wv-ep-scroll" id="wv-ep-scroll">${epBubbles}</div>
        </div>

        <!-- Nav prev/next -->
        <div class="wv-nav-row">
          <button class="wv-nav-btn" id="btn-prev" style="display:none"><i class="fa-solid fa-backward-step"></i> Sebelumnya</button>
          <button class="wv-nav-btn wv-nav-next" id="btn-next" style="display:none">Selanjutnya <i class="fa-solid fa-forward-step"></i></button>
        </div>

        <!-- Comments -->
        <div class="wv-comments">
          <div class="wv-comments-hd">
            <span class="wv-comments-title">Komentar</span>
            <select class="wv-sort-sel"><option>Terpopuler</option><option>Terbaru</option></select>
          </div>
          <div class="wv-comment-input-row">
            <div class="wv-comment-av">${user?.username?.charAt(0).toUpperCase()||'?'}</div>
            <input class="wv-comment-input" id="comment-input" placeholder="Tambah Komentar..." maxlength="500">
            <button class="wv-comment-send" id="btn-send-comment" onclick="sendComment('${animeSlug||slug}')">
              <i class="fa-solid fa-paper-plane"></i>
            </button>
          </div>
          <div id="comments-list" class="ani-comment-list">
            <div class="page-loading"><div class="spinner neon-spinner"></div></div>
          </div>
        </div>
      </div>`;

    // Episode bubbles click
    document.querySelectorAll('.wv-ep-bubble').forEach(btn => {
      btn.addEventListener('click', () => { const l = btn.dataset.link; if(l) go(l); });
    });
    // Scroll active bubble into view
    setTimeout(() => {
      const active = document.querySelector('.wv-ep-bubble.active');
      active?.scrollIntoView({ inline: 'center', behavior: 'smooth' });
    }, 300);

    // Share button
    $('wv-share-btn')?.addEventListener('click', () => {
      if (navigator.share) {
        navigator.share({ title: animeTitle, url: location.href }).catch(()=>{});
      } else {
        navigator.clipboard?.writeText(location.href).then(()=>showToastGlobal('Link disalin!','success')).catch(()=>{});
      }
    });

    // Full episode sheet
    window.showEpSheet = () => {
      const sheet = document.createElement('div');
      sheet.className = 'psmenu-overlay';
      const gridHtml = eps.map((e, i) => {
        const rawN = String(e.episode||'');
        const nm = rawN.match(/\d+/);
        const num = nm ? +nm[0] : (eps.length - i);
        const isCur = i === curIdx;
        return `<button class="ep-sheet-btn${isCur?' ep-sheet-cur':''}" data-link="/app/watch/${e.slug}">${num}</button>`;
      }).join('');
      sheet.innerHTML = `<div class="psmenu-sheet ep-sheet">
        <div class="psmenu-handle"></div>
        <div class="psmenu-header">
          <span class="psmenu-title">Semua Episode (${eps.length})</span>
          <button class="psmenu-close"><i class="fa-solid fa-xmark"></i></button>
        </div>
        <div class="ep-sheet-grid">${gridHtml}</div>
      </div>`;
      sheet.querySelector('.psmenu-close')?.addEventListener('click', ()=>sheet.remove());
      sheet.addEventListener('click', e=>{ if(e.target===sheet) sheet.remove(); });
      sheet.querySelectorAll('.ep-sheet-btn').forEach(btn => {
        btn.addEventListener('click', ()=>{ sheet.remove(); go(btn.dataset.link); });
      });
      document.body.appendChild(sheet);
      requestAnimationFrame(()=>sheet.classList.add('open'));
      setTimeout(()=>{ sheet.querySelector('.ep-sheet-cur')?.scrollIntoView({block:'center',behavior:'smooth'}); }, 350);
    };

    let currentIdx = 0, currentQuals = [], currentQualIdx = 0, autoRetryTimer = null;

    function addPlayerBtns(wrapper) {
      wrapper.querySelector('.player-btns')?.remove();
      const div = document.createElement('div');
      div.className = 'player-btns';

      // Only show quality button (no server selector)
      if (currentQuals.length > 1) {
        const qualBtn = document.createElement('button');
        qualBtn.className = 'player-settings-btn';
        qualBtn.title = 'Kualitas';
        qualBtn.innerHTML = `<span class="player-qual-badge">${fmtQuality(currentQuals[currentQualIdx]?.quality)}</span><i class="fa-solid fa-sliders"></i>`;
        qualBtn.addEventListener('click', () => {
          const sheet = document.createElement('div');
          sheet.className = 'psmenu-overlay';
          const itemsHtml = currentQuals.map((q,i) => `
            <div class="psmenu-item${i===currentQualIdx?' active':''}" data-qi="${i}">
              <div class="psmenu-item-l">
                <span class="psmenu-chk">${i===currentQualIdx?'<i class="fa-solid fa-check"></i>':''}</span>
                <span class="psmenu-label">${fmtQuality(q.quality)}</span>
              </div>
            </div>`).join('');
          sheet.innerHTML = `<div class="psmenu-sheet"><div class="psmenu-handle"></div>
            <div class="psmenu-header"><span class="psmenu-title">Kualitas Video</span><button class="psmenu-close"><i class="fa-solid fa-xmark"></i></button></div>
            <div class="psmenu-section">${itemsHtml}</div></div>`;
          sheet.querySelector('.psmenu-close')?.addEventListener('click', () => sheet.remove());
          sheet.addEventListener('click', e => { if(e.target===sheet) sheet.remove(); });
          sheet.querySelectorAll('[data-qi]').forEach(el => {
            el.addEventListener('click', () => {
              const qi = +el.dataset.qi;
              currentQualIdx = qi;
              const v = $('native-video');
              if (v) { const t=v.currentTime,was=!v.paused; v.src=currentQuals[qi].url; v.currentTime=t; if(was)v.play(); }
              const b = div.querySelector('.player-qual-badge');
              if (b) b.textContent = fmtQuality(currentQuals[qi].quality);
              sheet.remove();
            });
          });
          document.body.appendChild(sheet);
          requestAnimationFrame(() => sheet.classList.add('open'));
        });
        div.appendChild(qualBtn);
      }

      // Fullscreen button
      const fsBtn = document.createElement('button');
      fsBtn.className = 'player-fs-btn';
      fsBtn.innerHTML = '<i class="fa-solid fa-expand"></i>';
      fsBtn.title = 'Layar Penuh';
      fsBtn.addEventListener('click', async () => {
        try {
          if (!document.fullscreenElement) {
            await (wrapper.requestFullscreen?.() || wrapper.webkitRequestFullscreen?.());
            screen.orientation?.lock?.('landscape').catch(() => {});
          } else {
            await (document.exitFullscreen?.() || document.webkitExitFullscreen?.());
            screen.orientation?.unlock?.();
          }
        } catch {}
      });
      div.appendChild(fsBtn);
      wrapper.appendChild(div);
    }

    function loadServer(idx) {
      const wrapper = $('player-wrapper');
      if (!wrapper) return;
      clearTimeout(autoRetryTimer);

      if (idx >= servers.length) {
        wrapper.innerHTML = `<div class="player-error"><i class="fa-solid fa-circle-exclamation"></i><p>Video tidak tersedia dari semua server.<br>Coba lagi nanti.</p></div>`;
        return;
      }

      currentIdx = idx;
      const s = servers[idx];

      if (s.direct_urls?.length > 0) {
        currentQuals = [...s.direct_urls];
        currentQualIdx = 0;
        const lbl = $('wv-qual-label');
        if (lbl) lbl.textContent = fmtQuality(currentQuals[0].quality);
        $('wv-qual-btn')?.classList.add('has-quality');
        $('wv-dl-btn')?.classList.add('has-quality');
        wrapper.innerHTML = `
          <video id="native-video" controls autoplay playsinline preload="auto"
            src="${currentQuals[0].url}"
            poster="${s.thumbnail||''}"
            style="width:100%;height:100%;background:#000;display:block">
          </video>`;
        $('native-video')?.addEventListener('error', () => { if(currentIdx===idx) loadServer(idx+1); });
        addPlayerBtns(wrapper);
        return;
      }

      const url = s.embed_url || s.url;
      if (!url) { loadServer(idx+1); return; }

      currentQuals = []; currentQualIdx = 0;
      wrapper.innerHTML = '';
      const iframe = document.createElement('iframe');
      iframe.src = url;
      iframe.frameBorder = '0';
      iframe.setAttribute('allow', 'autoplay; fullscreen; encrypted-media; picture-in-picture');
      iframe.setAttribute('scrolling', 'no');
      iframe.setAttribute('allowfullscreen', '');
      Object.assign(iframe.style, { position:'absolute', inset:'0', width:'100%', height:'100%', border:'0' });
      wrapper.appendChild(iframe);
      addPlayerBtns(wrapper);

      autoRetryTimer = setTimeout(() => {
        if (currentIdx === idx && idx < servers.length - 1) loadServer(idx+1);
      }, 14000);
      iframe.addEventListener('load', () => clearTimeout(autoRetryTimer));
    }

    const onFsChange = () => {
      const icon = document.querySelector('.player-fs-btn i');
      if (icon) icon.className = document.fullscreenElement ? 'fa-solid fa-compress' : 'fa-solid fa-expand';
      if (!document.fullscreenElement) screen.orientation?.unlock?.();
    };
    document.addEventListener('fullscreenchange', onFsChange);
    document.addEventListener('webkitfullscreenchange', onFsChange);

    const onOrient = () => {
      const isLand = screen.orientation
        ? screen.orientation.type.includes('landscape')
        : Math.abs(+(window.orientation||0)) === 90;
      const wrapper = $('player-wrapper');
      if (!wrapper) return;
      if (isLand && !document.fullscreenElement) wrapper.requestFullscreen?.() || wrapper.webkitRequestFullscreen?.();
      else if (!isLand && document.fullscreenElement) document.exitFullscreen?.() || document.webkitExitFullscreen?.();
    };
    screen.orientation?.addEventListener('change', onOrient);
    window.addEventListener('orientationchange', onOrient);

    if (servers.length) loadServer(0);
    else {
      const w = $('player-wrapper');
      if (w) w.innerHTML = `<div class="player-error"><i class="fa-solid fa-circle-exclamation"></i><p>Tidak ada server video tersedia</p></div>`;
    }

    function openQualitySheet() {
      if (!currentQuals.length) {
        showToastGlobal('Kualitas tidak tersedia untuk server ini','info');
        return;
      }
      const sheet = document.createElement('div');
      sheet.className = 'psmenu-overlay';
      const qualHtml = currentQuals.map((q,i) => `
        <div class="psmenu-item${i===currentQualIdx?' active':''}" data-qi="${i}">
          <div class="psmenu-item-l">
            <span class="psmenu-chk">${i===currentQualIdx?'<i class="fa-solid fa-check"></i>':''}</span>
            <span class="psmenu-label">${fmtQuality(q.quality)}</span>
          </div>
          ${i===0?'<span class="psmenu-badge">Terbaik</span>':''}
        </div>`).join('');
      sheet.innerHTML = `<div class="psmenu-sheet">
        <div class="psmenu-handle"></div>
        <div class="psmenu-header">
          <span class="psmenu-title">Pilih Kualitas</span>
          <button class="psmenu-close"><i class="fa-solid fa-xmark"></i></button>
        </div>
        <div class="psmenu-section">${qualHtml}</div>
      </div>`;
      sheet.querySelector('.psmenu-close')?.addEventListener('click', ()=>sheet.remove());
      sheet.addEventListener('click', e=>{ if(e.target===sheet) sheet.remove(); });
      sheet.querySelectorAll('[data-qi]').forEach(el => {
        el.addEventListener('click', () => {
          const qi = +el.dataset.qi; currentQualIdx = qi;
          const v = $('native-video');
          if (v) { const t=v.currentTime, was=!v.paused; v.src=currentQuals[qi].url; v.currentTime=t; if(was)v.play(); }
          const lbl = $('wv-qual-label'); if(lbl) lbl.textContent = fmtQuality(currentQuals[qi].quality);
          sheet.remove();
        });
      });
      document.body.appendChild(sheet);
      requestAnimationFrame(()=>sheet.classList.add('open'));
    }

    $('wv-qual-btn')?.addEventListener('click', openQualitySheet);

    $('wv-dl-btn')?.addEventListener('click', () => {
      if (!currentQuals.length) { showToastGlobal('Download tidak tersedia untuk server ini','info'); return; }
      const url = currentQuals[currentQualIdx]?.url;
      if (url) { const a=document.createElement('a'); a.href=url; a.download=''; a.target='_blank'; a.click(); }
      else showToastGlobal('URL download tidak ditemukan','error');
    });

    API.epNav(slug).then(navRes => {
      const nav = navRes.data || {};
      const prevBtn = $('btn-prev'), nextBtn = $('btn-next');
      if (prevBtn && nav.prev) { prevBtn.style.display = ''; prevBtn.addEventListener('click', () => go('/app/watch/'+nav.prev.slug)); }
      if (nextBtn && nav.next) { nextBtn.style.display = ''; nextBtn.addEventListener('click', () => go('/app/watch/'+nav.next.slug)); }
    }).catch(() => {});

    // ── Like button ─────────────────────────────────────────────────
    const likeSlug = animeSlug || slug;
    let _userLiked = false;
    const likeBtn = $('wv-like-btn');
    function _updateLikeUI(count, liked) {
      _userLiked = liked;
      if (!likeBtn) return;
      const icon = likeBtn.querySelector('i');
      const span = likeBtn.querySelector('span');
      if (icon) icon.className = liked ? 'fa-solid fa-thumbs-up' : 'fa-regular fa-thumbs-up';
      if (span) span.textContent = count > 0 ? `Suka (${fmtNum(count)})` : 'Suka';
      likeBtn.classList.toggle('liked', liked);
    }
    API.getLikes(likeSlug).then(res => {
      const d = res.data || {};
      _updateLikeUI(d.count || 0, !!d.user_liked);
    }).catch(() => {});

    likeBtn?.addEventListener('click', async () => {
      try {
        const res = _userLiked ? await API.unlike(likeSlug) : await API.like(likeSlug);
        if (res.status === 401) { showToastGlobal('Login untuk menyukai', 'info'); return; }
        const d = res.data || {};
        _updateLikeUI(d.count || 0, !!d.user_liked);
        showToastGlobal(_userLiked ? 'Ditambahkan ke suka!' : 'Dihapus dari suka', 'success');
      } catch { showToastGlobal('Gagal mengubah status suka', 'error'); }
    });

    afterRender();
  } catch {
    content.innerHTML = `<div class="error-page"><p>Gagal memuat video. <button class="btn btn-sm" onclick="history.go(-1)">Kembali</button></p></div>`;
    hidePageLoader();
  }
}

// ══════════════════════════════════════════════════════════════════
// PAGE: WATCHLIST
// ══════════════════════════════════════════════════════════════════
async function renderWatchlist() {
  loading();
  try {
    const res = await API.getWatchlist();
    const list = res.data?.results || [];
    content.innerHTML = `
      <div class="page">
        <div class="page-heading">
          <h1><i class="fa-solid fa-bookmark" style="color:var(--neon)"></i> Watchlist Saya</h1>
          <p class="page-heading-sub">${list.length} anime tersimpan</p>
        </div>
        <div id="wl-grid" class="card-grid">
          ${list.length
            ? list.map(w => animeCard({ slug: w.anime_slug, title: w.anime_title||w.title, thumbnail: w.anime_thumbnail||w.thumbnail, type: w.type })).join('')
            : `<div class="empty-state" style="grid-column:1/-1">
                <i class="fa-regular fa-bookmark" style="font-size:3rem;display:block;margin-bottom:12px"></i>
                <p>Watchlist kamu masih kosong.</p>
                <button class="btn btn-primary btn-neon" style="margin-top:16px" onclick="go('/app')">
                  <i class="fa-solid fa-house"></i> Ke Beranda
                </button>
              </div>`}
        </div>
      </div>`;
    afterRender();
  } catch {
    content.innerHTML = `<div class="error-page"><p>Gagal memuat watchlist.</p></div>`;
    hidePageLoader();
  }
}

// ══════════════════════════════════════════════════════════════════
// PAGE: ADMIN PANEL
// ══════════════════════════════════════════════════════════════════
async function renderAdmin() {
  if (user?.role !== 'admin') { renderNotFound(); return; }
  loading();
  try {
    const [statsRes, usersRes] = await Promise.all([
      API.get('/admin/stats'), API.get('/admin/users')
    ]);
    const stats = statsRes.data || {};
    const users = usersRes.data?.users || [];

    content.innerHTML = `
      <div class="admin-page">
        <div class="admin-header">
          <h1><i class="fa-solid fa-crown"></i> Admin Panel</h1>
          <p style="color:var(--text3);margin-top:6px;font-size:.9rem">Selamat datang, ${user.username}</p>
        </div>
        <div class="admin-stats-grid">
          <div class="admin-stat-card">
            <div class="admin-stat-num">${stats.total_users ?? '—'}</div>
            <div class="admin-stat-lbl">Total Users</div>
          </div>
          <div class="admin-stat-card">
            <div class="admin-stat-num">${stats.active_users ?? '—'}</div>
            <div class="admin-stat-lbl">Active Users</div>
          </div>
          <div class="admin-stat-card">
            <div class="admin-stat-num">${stats.total_comments ?? '—'}</div>
            <div class="admin-stat-lbl">Total Komentar</div>
          </div>
          <div class="admin-stat-card">
            <div class="admin-stat-num">${stats.total_history ?? '—'}</div>
            <div class="admin-stat-lbl">Total History</div>
          </div>
        </div>
        <div class="admin-section">
          <h2 class="admin-section-title"><i class="fa-solid fa-users"></i> Daftar Pengguna</h2>
          <div style="overflow-x:auto">
          <table class="admin-table">
            <thead>
              <tr>
                <th>ID</th><th>Username</th><th>Email</th><th>Role</th><th>Status</th><th>Bergabung</th><th>Aksi</th>
              </tr>
            </thead>
            <tbody>
              ${users.map(u => `
                <tr>
                  <td>${u.id}</td>
                  <td style="font-weight:600">${escHtml(u.username)}</td>
                  <td style="color:var(--text2)">${escHtml(u.email)}</td>
                  <td><span class="admin-badge ${u.role}">${u.role}</span></td>
                  <td><span class="admin-badge ${u.is_active?'active':'banned'}">${u.is_active?'Aktif':'Banned'}</span></td>
                  <td style="color:var(--text3);font-size:.8rem">${u.created_at ? new Date(u.created_at).toLocaleDateString('id-ID') : '—'}</td>
                  <td>
                    ${u.id !== user.id ? `
                      <button class="admin-action-btn" onclick="adminToggleUser(${u.id})" title="${u.is_active?'Ban':'Aktifkan'}">
                        ${u.is_active ? '<i class="fa-solid fa-ban"></i>' : '<i class="fa-solid fa-check"></i>'}
                      </button>
                      <button class="admin-action-btn" onclick="adminChangeRole(${u.id},'${u.role==='admin'?'user':'admin'}')" title="Ganti Role" style="margin-left:4px">
                        ${u.role==='admin' ? '<i class="fa-solid fa-user-minus"></i>' : '<i class="fa-solid fa-user-shield"></i>'}
                      </button>
                    ` : '<span style="color:var(--text3);font-size:.75rem">—</span>'}
                  </td>
                </tr>`).join('')}
            </tbody>
          </table>
          </div>
        </div>

        <div class="admin-section" style="margin-top:20px">
          <h2 class="admin-section-title"><i class="fa-solid fa-sliders"></i> Pengaturan</h2>
          <div style="display:flex;gap:12px;flex-wrap:wrap">
            <button class="btn btn-outline" onclick="adminClearCache()"><i class="fa-solid fa-trash-can"></i> Clear Cache</button>
            <button class="btn btn-outline" onclick="go('/app')"><i class="fa-solid fa-house"></i> Kembali ke Beranda</button>
          </div>
        </div>
      </div>`;
    afterRender();
  } catch {
    content.innerHTML = `<div class="error-page"><p>Gagal memuat admin panel. <button class="btn btn-sm" onclick="go('/app')">Kembali</button></p></div>`;
    hidePageLoader();
  }
}

window.adminToggleUser = async function(uid) {
  try {
    const res = await API.post('/admin/users/' + uid + '/toggle', {});
    showToastGlobal(res.data?.message || 'Status diubah', 'success');
    renderAdmin();
  } catch { showToastGlobal('Gagal mengubah status','error'); }
};
window.adminChangeRole = async function(uid, role) {
  try {
    const res = await API.post('/admin/users/' + uid + '/role', { role });
    showToastGlobal(res.data?.message || 'Role diubah', 'success');
    renderAdmin();
  } catch { showToastGlobal('Gagal mengubah role','error'); }
};
window.adminClearCache = async function() {
  try {
    await API.del('/admin/cache');
    showToastGlobal('Cache berhasil dibersihkan!','success');
  } catch { showToastGlobal('Gagal clear cache','error'); }
};

// ══════════════════════════════════════════════════════════════════
// PAGE: PROFILE — YouTube+Netflix "You" style
// ══════════════════════════════════════════════════════════════════
const FA_AVATARS = [
  '<i class="fa-solid fa-dragon"></i>','<i class="fa-solid fa-cat"></i>',
  '<i class="fa-solid fa-dog"></i>','<i class="fa-solid fa-crow"></i>',
  '<i class="fa-solid fa-hippo"></i>','<i class="fa-solid fa-horse"></i>',
  '<i class="fa-solid fa-frog"></i>','<i class="fa-solid fa-otter"></i>',
  '<i class="fa-solid fa-feather-pointed"></i>','<i class="fa-solid fa-paw"></i>',
  '<i class="fa-solid fa-shield-halved"></i>','<i class="fa-solid fa-bolt"></i>',
  '<i class="fa-solid fa-star"></i>','<i class="fa-solid fa-moon"></i>',
  '<i class="fa-solid fa-fire"></i>',
];

async function renderProfile() {
  loading();
  const u = Auth.user || {};
  const joinDate = u.created_at ? new Date(u.created_at).toLocaleDateString('id-ID', { year:'numeric', month:'long' }) : '';

  // Load counts
  let histCount = 0, wlCount = 0;
  try {
    const [h, w] = await Promise.all([API.getHistory(), API.getWatchlist()]);
    histCount = (h.data?.results || h.data?.history || []).length;
    wlCount   = (w.data?.results || []).length;
  } catch {}

  content.innerHTML = `
    <div class="profile-page">
      <div class="profile-hero">
        <div class="profile-banner"></div>
        <div class="profile-hero-body">
          <div class="profile-avatar-wrap">
            <div class="profile-avatar-big" id="prof-avatar">${u.avatar||'<i class="fa-solid fa-dragon"></i>'}</div>
            <button class="avatar-change-btn" id="btn-change-avatar" title="Ganti avatar"><i class="fa-solid fa-pen"></i></button>
          </div>
          <div class="profile-info-main">
            <div class="profile-username">${u.username||''}</div>
            <div class="profile-email"><i class="fa-solid fa-envelope"></i> ${u.email||''}</div>
            <div class="profile-badges-row">
              <span class="profile-role ${u.role==='admin'?'admin':''}">
                ${u.role==='admin' ? '<i class="fa-solid fa-crown"></i> Admin' : '<i class="fa-solid fa-user"></i> Member'}
              </span>
              ${joinDate ? `<span class="profile-join"><i class="fa-solid fa-calendar-check"></i> Bergabung ${joinDate}</span>` : ''}
            </div>
          </div>
        </div>
      </div>

      <div class="profile-stats-row">
        <div class="profile-stat">
          <div class="profile-stat-num">${histCount}</div>
          <div class="profile-stat-lbl">Ditonton</div>
        </div>
        <div class="profile-stat">
          <div class="profile-stat-num">${wlCount}</div>
          <div class="profile-stat-lbl">Watchlist</div>
        </div>
        <div class="profile-stat">
          <div class="profile-stat-num">${u.role==='admin'?'<i class="fa-solid fa-crown" style="color:#facc15;font-size:1.4rem"></i>':'<i class="fa-solid fa-star" style="color:#facc15;font-size:1.4rem"></i>'}</div>
          <div class="profile-stat-lbl">Status</div>
        </div>
      </div>

      <div class="avatar-picker" id="avatar-picker" style="display:none">
        <div class="avatar-picker-title">Pilih Avatar</div>
        <div class="avatar-picker-grid" id="avatar-picker-grid">
          ${FA_AVATARS.map((av,i) => `
            <button class="avatar-option ${av===u.avatar?'selected':''}" data-avatar="${av.replace(/"/g,'&quot;')}" title="Avatar ${i+1}">
              ${av}
            </button>`).join('')}
        </div>
      </div>

      <div class="profile-tabs">
        <button class="tab-btn active" data-tab="history"><i class="fa-solid fa-clock-rotate-left"></i> Riwayat</button>
        <button class="tab-btn" data-tab="watchlist"><i class="fa-solid fa-bookmark"></i> Watchlist</button>
        <button class="tab-btn" data-tab="settings"><i class="fa-solid fa-gear"></i> Pengaturan</button>
      </div>

      <div id="tab-history" class="tab-content">
        <div id="hist-list" class="ep-grid"><div class="page-loading"><div class="spinner neon-spinner"></div></div></div>
      </div>
      <div id="tab-watchlist" class="tab-content hidden">
        <div id="wl-list" class="card-grid"><div class="page-loading"><div class="spinner neon-spinner"></div></div></div>
      </div>
      <div id="tab-settings" class="tab-content hidden">
        <div class="settings-section">
          <div class="settings-card">
            <h3 class="settings-card-title"><i class="fa-solid fa-bell" style="color:var(--neon)"></i> Notifikasi</h3>
            <p class="settings-card-desc">Aktifkan notifikasi browser untuk mendapat info episode baru.</p>
            <button class="btn btn-outline" onclick="adminRequestNotif()">
              <i class="fa-solid fa-bell"></i> Aktifkan Notifikasi
            </button>
          </div>
          <div class="settings-card">
            <h3 class="settings-card-title"><i class="fa-solid fa-lock" style="color:var(--neon)"></i> Ganti Kata Sandi</h3>
            <div class="form-group">
              <label class="form-label">Password Lama</label>
              <div class="form-input-wrap">
                <input type="password" class="form-input" id="set-old-pw" placeholder="Password lama">
                <button class="toggle-pass" onclick="togglePwVis('set-old-pw',this)" type="button"><i class="fa-solid fa-eye"></i></button>
              </div>
            </div>
            <div class="form-group">
              <label class="form-label">Password Baru</label>
              <div class="form-input-wrap">
                <input type="password" class="form-input" id="set-new-pw" placeholder="Min. 6 karakter">
                <button class="toggle-pass" onclick="togglePwVis('set-new-pw',this)" type="button"><i class="fa-solid fa-eye"></i></button>
              </div>
            </div>
            <div class="form-group">
              <label class="form-label">Konfirmasi Password Baru</label>
              <div class="form-input-wrap">
                <input type="password" class="form-input" id="set-confirm-pw" placeholder="Ulangi password baru">
                <button class="toggle-pass" onclick="togglePwVis('set-confirm-pw',this)" type="button"><i class="fa-solid fa-eye"></i></button>
              </div>
            </div>
            <div id="set-pw-err" class="form-error" style="display:none"></div>
            <button class="btn btn-primary btn-neon" id="btn-save-pw" onclick="doChangePassword()">
              <i class="fa-solid fa-floppy-disk"></i> Simpan Password
            </button>
          </div>
          <div class="settings-card danger-zone">
            <h3 class="settings-card-title"><i class="fa-solid fa-right-from-bracket"></i> Sesi</h3>
            <p class="settings-card-desc">Keluar dari akun di perangkat ini.</p>
            <button class="btn btn-danger" onclick="doLogout()">
              <i class="fa-solid fa-right-from-bracket"></i> Keluar
            </button>
          </div>
        </div>
      </div>
    </div>`;

  // Tab switching
  document.querySelectorAll('.tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      document.querySelectorAll('.tab-content').forEach(c => c.classList.add('hidden'));
      $('tab-'+btn.dataset.tab)?.classList.remove('hidden');
    });
  });

  // Avatar picker
  $('btn-change-avatar')?.addEventListener('click', () => {
    const picker = $('avatar-picker');
    if (picker) picker.style.display = picker.style.display === 'none' ? 'block' : 'none';
  });
  $('avatar-picker-grid')?.addEventListener('click', async e => {
    const btn = e.target.closest('.avatar-option');
    if (!btn) return;
    const newAvatar = btn.dataset.avatar.replace(/&quot;/g,'"');
    try {
      await API.updateAvatar(newAvatar);
      const u = Auth.user;
      if (u) { u.avatar = newAvatar; Auth.set(u, localStorage.getItem('actoken')); }
      $('prof-avatar').innerHTML = newAvatar;
      $('sidebar-avatar') && ($('sidebar-avatar').innerHTML = newAvatar);
      $('bottomnav-avatar') && ($('bottomnav-avatar').innerHTML = newAvatar);
      $('avatar-picker').style.display = 'none';
      document.querySelectorAll('.avatar-option').forEach(b => b.classList.toggle('selected', b.dataset.avatar.replace(/&quot;/g,'"') === newAvatar));
      showToastGlobal('Avatar berhasil diubah!', 'success');
    } catch { showToastGlobal('Gagal ubah avatar','error'); }
  });

  // Load history
  API.getHistory().then(res => {
    const list = res.data?.results || res.data?.history || [];
    const el = $('hist-list');
    if (!el) return;
    el.innerHTML = list.length
      ? list.map(h => episodeCard({ slug: h.episode_slug, name: h.episode_name||h.episode_title||h.episode_slug, thumbnail: h.thumbnail, date: h.watched_at })).join('')
      : `<div class="empty-state"><i class="fa-solid fa-clock-rotate-left" style="font-size:2.5rem;display:block;margin-bottom:10px"></i><p>Belum ada riwayat tontonan</p></div>`;
  }).catch(() => {});

  // Load watchlist tab
  API.getWatchlist().then(res => {
    const list = res.data?.results || [];
    const el = $('wl-list');
    if (!el) return;
    el.innerHTML = list.length
      ? list.map(w => animeCard({ slug: w.anime_slug, title: w.anime_title||w.title, thumbnail: w.anime_thumbnail||w.thumbnail, type: w.type })).join('')
      : `<div class="empty-state" style="grid-column:1/-1"><i class="fa-regular fa-bookmark" style="font-size:2.5rem;display:block;margin-bottom:10px"></i><p>Watchlist kosong</p></div>`;
  }).catch(() => {});

  afterRender();
}

window.adminRequestNotif = function() {
  if (!('Notification' in window)) return showToastGlobal('Browser tidak mendukung notifikasi','error');
  if (Notification.permission === 'granted') return showToastGlobal('Notifikasi sudah aktif!','success');
  Notification.requestPermission().then(p => {
    showToastGlobal(p === 'granted' ? 'Notifikasi berhasil diaktifkan!' : 'Izin notifikasi ditolak', p === 'granted' ? 'success' : 'error');
  });
};

window.togglePwVis = function(id, btn) {
  const inp = $(id);
  if (!inp) return;
  const isHidden = inp.type === 'password';
  inp.type = isHidden ? 'text' : 'password';
  btn.innerHTML = isHidden ? '<i class="fa-solid fa-eye-slash"></i>' : '<i class="fa-solid fa-eye"></i>';
};

window.doChangePassword = async function() {
  const oldPw = $('set-old-pw')?.value.trim();
  const newPw = $('set-new-pw')?.value.trim();
  const confirmPw = $('set-confirm-pw')?.value.trim();
  const errEl = $('set-pw-err'), btn = $('btn-save-pw');
  function setErr(msg) { if(errEl) { errEl.textContent = msg; errEl.style.display = msg ? 'block' : 'none'; } }
  setErr('');
  if (!oldPw||!newPw||!confirmPw) return setErr('Semua field wajib diisi');
  if (newPw.length < 6) return setErr('Password baru minimal 6 karakter');
  if (newPw !== confirmPw) return setErr('Konfirmasi password tidak cocok');
  if (btn) { btn.disabled = true; btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Menyimpan...'; }
  try {
    const res = await API.changePassword(oldPw, newPw);
    if (res.ok === false || res.data?.status === 'error') setErr(res.data?.message || 'Gagal mengubah password');
    else {
      showToastGlobal('Password berhasil diubah!','success');
      $('set-old-pw').value = ''; $('set-new-pw').value = ''; $('set-confirm-pw').value = '';
    }
  } catch { setErr('Terjadi kesalahan. Coba lagi.'); }
  if (btn) { btn.disabled = false; btn.innerHTML = '<i class="fa-solid fa-floppy-disk"></i> Simpan Password'; }
};

async function doLogout() {
  await API.logout().catch(() => {});
  Auth.clear();
  showToastGlobal('Berhasil keluar','info');
  setTimeout(() => window.location.href = '/', 700);
}
window.doLogout = doLogout;

function renderNotFound() {
  content.innerHTML = `<div class="error-page"><i class="fa-solid fa-circle-exclamation" style="font-size:3rem;display:block;margin-bottom:16px;color:var(--neon)"></i><h2>Halaman tidak ditemukan</h2><button class="btn btn-primary btn-neon" style="margin-top:16px" onclick="go('/app')">Kembali ke Beranda</button></div>`;
  hidePageLoader();
}

// ══════════════════════════════════════════════════════════════════
// PAGE: DONGHUA
// ══════════════════════════════════════════════════════════════════
function _renderTypedList(title, icon, fetchFn, pageKey) {
  return async function(_routeParams) {
    const _qp = new URLSearchParams(location.search);
    const curPage = _qp.has('page') ? +_qp.get('page') : 1;
    loading();
    content.innerHTML = `
      <div class="browse-page">
        <div class="browse-topbar" style="padding:14px 16px 0">
          <button class="wv-back-btn" style="margin-right:8px" onclick="history.go(-1)"><i class="fa-solid fa-arrow-left"></i></button>
          <h2 style="font-size:1.1rem;font-weight:700;flex:1">${icon} ${title}</h2>
        </div>
        <div id="type-results" class="card-grid browse-grid" style="padding:14px 16px"></div>
        <div id="type-pagination" class="browse-pages"></div>
      </div>`;
    try {
      const res = await fetchFn(curPage);
      const items = res.data?.results || [];
      const total = res.data?.total || 0;
      const el = $('type-results');
      if (!el) return;
      el.innerHTML = items.length
        ? items.map(animeCard).join('')
        : `<div class="empty-state" style="grid-column:1/-1"><i class="fa-solid fa-circle-exclamation"></i><p>Tidak ada data</p></div>`;
      el.querySelectorAll('.anime-card').forEach(c => c.addEventListener('click', () => go(c.dataset.link)));
      const pg = $('type-pagination');
      if (pg && total > items.length) {
        const totalPages = Math.ceil(total / (items.length || 20));
        pg.innerHTML = curPage > 1
          ? `<button class="browse-page-btn" onclick="go('/app/${pageKey}?page=${curPage-1}')"><i class="fa-solid fa-chevron-left"></i> Sebelumnya</button>&nbsp;`
          : '';
        pg.innerHTML += `<span style="padding:0 8px;color:var(--text2)">Hal ${curPage}</span>`;
        pg.innerHTML += `<button class="browse-page-btn" onclick="go('/app/${pageKey}?page=${curPage+1}')">Selanjutnya <i class="fa-solid fa-chevron-right"></i></button>`;
      }
    } catch {
      const el = $('type-results');
      if (el) el.innerHTML = `<div class="empty-state" style="grid-column:1/-1"><p>Gagal memuat. <button class="btn btn-sm" onclick="go('/app/${pageKey}')">Coba lagi</button></p></div>`;
    }
    afterRender();
  };
}

const renderDonghua  = _renderTypedList('Donghua', '<i class="fa-solid fa-dragon" style="color:var(--neon)"></i>', p => API.donghua(p), 'donghua');
const renderMovie    = _renderTypedList('Movie', '<i class="fa-solid fa-film" style="color:#f5c518"></i>', p => API.movie(p), 'movie');
const renderOngoing  = _renderTypedList('Ongoing', '<i class="fa-solid fa-circle-play" style="color:#22c55e"></i>', p => API.ongoing(p), 'ongoing');
const renderCompleted = _renderTypedList('Completed', '<i class="fa-solid fa-circle-check" style="color:#a855f7"></i>', p => API.completed(p), 'completed');

// ── Register routes ────────────────────────────────────────────────
Router.add('/app',              renderHome);
Router.add('/app/anime/:slug',  renderAnime);
Router.add('/app/watch/:slug',  renderWatch);
Router.add('/app/browse',       renderBrowse);
Router.add('/app/watchlist',    renderWatchlist);
Router.add('/app/profile',      renderProfile);
Router.add('/app/admin',        renderAdmin);
Router.add('/app/donghua',      renderDonghua);
Router.add('/app/movie',        renderMovie);
Router.add('/app/ongoing',      renderOngoing);
Router.add('/app/completed',    renderCompleted);
Router.init();

$('btn-sidebar-logout')?.addEventListener('click', doLogout);
$('btn-bottom-logout')?.addEventListener('click', doLogout);

})();
}
