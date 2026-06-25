'use strict';

const API = {
  base: '/v1',

  _token() {
    return localStorage.getItem('actoken') || '';
  },

  _headers() {
    const h = { 'Content-Type': 'application/json' };
    const t = this._token();
    if (t) h['Authorization'] = 'Bearer ' + t;
    return h;
  },

  async _req(method, path, body) {
    try {
      const opts = { method, headers: this._headers() };
      if (body) opts.body = JSON.stringify(body);
      const res = await fetch(this.base + path, opts);
      const json = await res.json();
      return { ok: res.ok, status: res.status, data: json.data ?? json };
    } catch (e) {
      return { ok: false, status: 0, data: { message: 'Koneksi gagal' } };
    }
  },

  get(path) { return this._req('GET', path); },
  post(path, body) { return this._req('POST', path, body); },
  del(path, body) { return this._req('DELETE', path, body); },

  // Auth
  sendOtp(email, username, password) {
    return fetch('/v1/auth/send-otp', {
      method: 'POST',
      headers: this._headers(),
      body: JSON.stringify({ email, username, password })
    }).then(r => r.json());
  },
  register(email, otp_code) {
    return fetch('/v1/auth/register', {
      method: 'POST',
      headers: this._headers(),
      body: JSON.stringify({ email, otp_code })
    }).then(r => r.json());
  },
  login(login, password) {
    return fetch('/v1/auth/login', {
      method: 'POST',
      headers: this._headers(),
      body: JSON.stringify({ login, password })
    }).then(r => r.json());
  },
  logout() { return fetch('/v1/auth/logout', { method: 'POST', headers: this._headers() }).then(r => r.json()); },
  me() { return this.get('/auth/me'); },

  // Content
  home()           { return this.get('/home'); },
  latest(page=1)   { return this.get('/latest?page=' + page); },
  popular(page=1)  { return this.get('/popular?page=' + page); },
  trending(page=1) { return this.get('/trending?page=' + page); },
  ongoing(page=1)  { return this.get('/ongoing?page=' + page); },
  completed(page=1){ return this.get('/completed?page=' + page); },
  random()         { return this.get('/random'); },

  animeList(params={}) {
    const q = new URLSearchParams(params).toString();
    return this.get('/anime' + (q ? '?' + q : ''));
  },
  animeDetail(slug) { return this.get('/info/' + slug); },
  episode(slug)     { return this.get('/episode/' + slug); },
  epNav(slug)       { return this.get('/episode/' + slug + '/navigation'); },
  videoSource(slug) {
    return fetch('/video-source/' + slug, { headers: this._headers() }).then(r => r.json());
  },
  search(q)         { return this.get('/search/' + encodeURIComponent(q)); },
  genres()          { return this.get('/genres'); },
  genre(slug, p=1)  { return this.get('/genre/' + slug + '?page=' + p); },

  // User
  getWatchlist()              { return this.get('/user/watchlist'); },
  addWatchlist(slug)          { return this.post('/user/watchlist', { anime_slug: slug }); },
  removeWatchlist(slug)       { return this.del('/user/watchlist', { anime_slug: slug }); },
  getHistory()                { return this.get('/user/history'); },
  addHistory(ep, anime)       { return this.post('/user/history', { episode_slug: ep, anime_slug: anime }); },
  changePassword(oldPw, newPw){ return this.post('/user/change-password', { old_password: oldPw, new_password: newPw }); },
  updateAvatar(avatar)        { return this.post('/user/avatar', { avatar }); },

  // Comments
  getComments(animeSlug)       { return this.get('/comments/' + animeSlug); },
  addComment(animeSlug, text)  { return this.post('/comments/' + animeSlug, { content: text }); },
  deleteComment(id)            { return this.del('/comments/' + id); },

  // Views
  addView(slug)  { return this.post('/views/' + slug); },
  getViews(slug) { return this.get('/views/' + slug); },

  // Likes
  getLikes(slug)  { return this.get('/likes/' + slug); },
  like(slug)      { return this.post('/likes/' + slug); },
  unlike(slug)    { return this.del('/likes/' + slug); },

  // Notifications
  getNotifications()     { return this.get('/notifications'); },
  markNotifRead()        { return this.post('/notifications/read'); },

  // Type-filtered lists
  donghua(page=1) { return this.get('/donghua?page=' + page); },
  movie(page=1)   { return this.get('/movie?page=' + page); },
};
