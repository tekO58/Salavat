// Salavat web uygulamasi. Android surumundeki (app/) ekranlarin ve kurallarin
// tarayici karsiligi; kurulum gerektirmez. Ekranlar: pano, bolum listesi,
// kayit sayfasi, yapistir ve ekle, tespih. Veri bu tarayicida (localStorage).
'use strict';

/* ================================================================ veri */

const SECTIONS = [
  { id: 'salavat', name: 'Salavat', icon: '📿', color: '#E8B34A', target: 100, hint: 'Sayaçlı · Arapça, okunuş, anlam', builtin: true },
  { id: 'zikir', name: 'Zikirler', icon: '✨', color: '#72C98A', target: 100, hint: 'Sayaçlı · kısa metin', builtin: true },
  { id: 'dua', name: 'Dualar', icon: '🤲', color: '#7FB2F0', target: 0, hint: 'Okuma sayfası · sayaç isteğe bağlı', builtin: true }
];
const SECTION_ICONS = ['🌙', '✨', '🌹', '🕋', '🕌', '📖', '📿', '🤲', '🌟', '🌸', '🌿', '💧', '💚', '🕯️', '🌅', '🍃'];
const SECTION_COLORS = ['#E39BD0', '#2DD4BF', '#F0A94B', '#A78BFA', '#E8746A', '#5B8DEF'];
// Tespih materyalleri ve cizimi rosary.js'te (Rosary.MATERIALS).
const MATERIALS = Rosary.MATERIALS;
/** Eski kayitlardaki (bead: 'kuka' ...) renk adindan materyal. */
const LEGACY_BEAD = { kuka: 'KUKA', kehribar: 'KEHRIBAR', siyah: 'SIYAH', yesil: 'YESIL', lacivert: 'LACIVERT', beyaz: 'BEYAZ' };
const materialOf = (t) => MATERIALS.find((m) => m.id === (t.material || LEGACY_BEAD[t.bead])) || MATERIALS[0];
const materialTitle = (m) => (m.detail ? `${m.label} (${m.detail})` : m.label);
const EVERY_PRESETS = [0, 1, 10, 33, 99];
const MAX_TARGET = 50000;
const KEY = 'salavat.v1';

let builtin = [];
let state = load();

function load() {
  const empty = { tasbih: {}, days: {}, hist: {}, last: null, entries: [], sections: [], fav: [], goal: 300, ar: 26, tx: 16, ctext: 1, open: {}, idle: 30000 };
  try {
    const raw = localStorage.getItem(KEY);
    return raw ? Object.assign(empty, JSON.parse(raw)) : empty;
  } catch (e) { return empty; }
}
function save() {
  try { localStorage.setItem(KEY, JSON.stringify(state)); } catch (e) { toast('Kaydedilemedi: tarayıcı depolaması kapalı olabilir'); }
}

const sections = () => SECTIONS.concat(state.sections);
const sectionOf = (id) => sections().find((s) => s.id === id);
const entries = () => builtin.concat(state.entries);
const entryOf = (id) => entries().find((e) => e.id === id);
const hasCounter = (e) => e.target > 0;

function dayKey(d = new Date()) {
  const p = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
}
const todayTotal = () => state.days[dayKey()] || 0;
const todayOf = (id) => (state.hist[id] || {})[dayKey()] || 0;

/** Ust uste gun: bugun okunmadiysa dunden geriye (Android: Library.streak). */
function streak() {
  const d = new Date();
  if (!state.days[dayKey(d)]) d.setDate(d.getDate() - 1);
  let n = 0;
  while (state.days[dayKey(d)] > 0) { n++; d.setDate(d.getDate() - 1); }
  return n;
}

function newId() { return 'u' + Date.now().toString(36) + Math.floor(Math.random() * 1000).toString(36); }

/** Arama: buyuk/kucuk, sapkali harf ve Arapca hareke farki yok sayilir. */
function normalize(s) {
  const map = { 'â': 'a', 'á': 'a', 'à': 'a', 'î': 'i', 'í': 'i', 'ı': 'i', 'û': 'u', 'ú': 'u', 'ğ': 'g', 'ş': 's', 'ç': 'c', 'ö': 'o', 'ü': 'u' };
  let out = '';
  for (const ch of (s || '').toLocaleLowerCase('tr')) {
    const c = ch.charCodeAt(0);
    if ("'’`ʿʾ-".includes(ch) || (c >= 0x064B && c <= 0x065F) || c === 0x0670 || c === 0x0640) continue;
    out += map[ch] || ch;
  }
  return out;
}
function matches(e, q) {
  const n = normalize(q.trim());
  if (!n) return true;
  return [e.name, e.aka, e.reading, e.meaning, e.arabic, e.attribution].some((f) => normalize(f).includes(n));
}

/* ------------------------------------------------------------- tespih */

/** Kaydin sayaci; yoksa kaydin okuma sayisiyla (sayacsizda 33) olusur. */
function tasbihFor(e) {
  if (!state.tasbih[e.id]) {
    state.tasbih[e.id] = { target: hasCounter(e) ? e.target : 33, onTarget: 'stop', every: 33, sound: false, finish: true, material: 'KUKA' };
  }
  // Eski kayitlarda sure alanlari yok: tamamlanir.
  state.tasbih[e.id] = TasbihCore.normalizeTasbih(state.tasbih[e.id]);
  save();
  return state.tasbih[e.id];
}
const isDone = TasbihCore.isDone;
/** Iki dokunus arasi bu sureyi asarsa sure duraklar. */
const idleMs = () => (TasbihCore.IDLE_OPTIONS.includes(state.idle) ? state.idle : TasbihCore.DEFAULT_IDLE_MS);

function addHist(id, delta) {
  const day = dayKey();
  state.days[day] = Math.max(0, (state.days[day] || 0) + delta);
  state.hist[id] = state.hist[id] || {};
  state.hist[id][day] = Math.max(0, (state.hist[id][day] || 0) + delta);
}

/** Tek dokunus: 'blocked' | 'completed' | 'milestone' | 'counted' (Android: TasbihLogic.tap). */
function tap(id) {
  const ev = TasbihCore.tap(state.tasbih[id], Date.now(), idleMs());
  if (ev === 'blocked') return ev;
  state.last = id;
  addHist(id, +1);
  save();
  return ev;
}

function undo(id) {
  if (!TasbihCore.undo(state.tasbih[id])) return false;
  addHist(id, -1);
  save();
  return true;
}

/* -------------------------------------------------------- geri bildirim */

const vib = (p) => { try { navigator.vibrate && navigator.vibrate(p); } catch (e) {} };
let audio = null;
function beep(finish) {
  try {
    audio = audio || new (window.AudioContext || window.webkitAudioContext)();
    const o = audio.createOscillator(), g = audio.createGain();
    o.frequency.value = finish ? 880 : 1200;
    g.gain.value = 0.08;
    o.connect(g); g.connect(audio.destination);
    o.start(); o.stop(audio.currentTime + (finish ? 0.22 : 0.035));
  } catch (e) {}
}

let wakeLock = null;
async function keepAwake(on) {
  try {
    if (on && 'wakeLock' in navigator && !wakeLock) wakeLock = await navigator.wakeLock.request('screen');
    if (!on && wakeLock) { await wakeLock.release(); wakeLock = null; }
  } catch (e) { wakeLock = null; }
}

/* ================================================================ arayuz */

/** h('div.card.row', {onclick}, ...cocuklar) — metinler textContent olarak girer. */
function h(tag, props, ...kids) {
  const [name, ...classes] = tag.split('.');
  const el = document.createElement(name || 'div');
  if (classes.length) el.className = classes.join(' ');
  if (props) {
    for (const [k, v] of Object.entries(props)) {
      if (v == null || v === false) continue;
      if (k.startsWith('on')) el.addEventListener(k.substring(2), v);
      else if (k === 'style' && typeof v === 'object') Object.assign(el.style, v);
      else if (k === 'text') el.textContent = v;
      else el.setAttribute(k, v === true ? '' : v);
    }
  }
  for (const kid of kids.flat()) {
    if (kid == null || kid === false) continue;
    el.append(kid instanceof Node ? kid : document.createTextNode(String(kid)));
  }
  return el;
}
const num = (n) => Number(n || 0).toLocaleString('tr-TR');

/*
 * Yonlendirme. Normalde adres cubugundaki #/... kullanilir. Gomulu modda
 * (window.SALAVAT_EMBED: tek dosyalik claude.ai sayfasi) adres degistirilemez;
 * sayfa gecmisi bellekte tutulur.
 */
const EMBED = !!window.SALAVAT_EMBED;
// Buyuk harfli basliklarda Turkce İ/I (pencereler de sayfanin her yerinde Turkce sayilsin).
document.documentElement.lang = 'tr';
const stack = ['#/'];
const route = () => (EMBED ? stack[stack.length - 1] : location.hash || '#/');
function go(hash) {
  if (EMBED) { stack.push(hash); render(); } else location.hash = hash;
}
function replaceRoute(hash, rerender) {
  if (EMBED) { stack[stack.length - 1] = hash; if (rerender) render(); }
  else if (rerender) location.replace(hash);
  else history.replaceState(null, '', hash);
}
function back() {
  if (EMBED) { if (stack.length > 1) stack.pop(); render(); }
  else if (history.length > 1) history.back();
  else go('#/');
}
const app = document.getElementById('app');

function mix(hex, base, ratio) {
  const a = parseInt(hex.slice(1), 16), b = parseInt(base.slice(1), 16);
  const ch = (s) => Math.round(((a >> s) & 255) * ratio + ((b >> s) & 255) * (1 - ratio));
  return `rgb(${ch(16)},${ch(8)},${ch(0)})`;
}

function chip(label, on, color, onclick) {
  color = color || '#E8B34A';
  return h('button.chip' + (on ? '.on' : ''), {
    onclick,
    style: on ? { background: color, borderColor: color } : { borderColor: mix(color, '#151D32', 0.45) }
  }, label);
}

function toast(msg) {
  document.querySelectorAll('.toast').forEach((t) => t.remove());
  const t = h('div.toast', null, msg);
  document.body.append(t);
  setTimeout(() => t.remove(), 2200);
}

function overlay(content, bottom) {
  const o = h('div.overlay' + (bottom ? '.bottom' : ''), { onclick: (ev) => { if (ev.target === o) o.remove(); } }, content);
  document.body.append(o);
  return o;
}

/** Secenek listesi. items: [{label, fn, on}] */
function choose(title, items) {
  let o;
  const opts = h('div.opts', null, items.map((it) => h('button' + (it.on ? '.on' : ''), { onclick: () => { o.remove(); it.fn(); } }, it.label)));
  o = overlay(h('div.dialog', null, h('h2', null, title), opts,
    h('div.actions', null, h('button.cancel', { onclick: () => o.remove() }, 'Vazgeç'))));
}

function confirmDlg(title, msg, okLabel, fn) {
  let o;
  o = overlay(h('div.dialog', null, h('h2', null, title), h('p.sec', null, msg),
    h('div.actions', null,
      h('button.cancel', { onclick: () => o.remove() }, 'Vazgeç'),
      h('button.danger', { onclick: () => { o.remove(); fn(); } }, okLabel))));
}

function askNumber(title, msg, current, onValue) {
  let o;
  const input = h('input.field', { type: 'number', inputmode: 'numeric', min: 1, max: MAX_TARGET, value: current || '', placeholder: `1 – ${num(MAX_TARGET)}` });
  const ok = () => {
    const v = parseInt(input.value, 10);
    if (!(v >= 1 && v <= MAX_TARGET)) { toast(`1 ile ${num(MAX_TARGET)} arasında bir sayı gir`); return; }
    o.remove(); onValue(v);
  };
  input.addEventListener('keydown', (ev) => { if (ev.key === 'Enter') ok(); });
  o = overlay(h('div.dialog', null, h('h2', null, title), msg ? h('p.sec.mb8', null, msg) : null, input,
    h('div.actions', null, h('button.cancel', { onclick: () => o.remove() }, 'Vazgeç'), h('button.ok', { onclick: ok }, 'Tamam'))));
  setTimeout(() => input.focus(), 50);
}

/** Yeni bolum: ad, simge, renk. */
function askNewSection(onCreated) {
  let icon = SECTION_ICONS[0], color = SECTION_COLORS[0], o;
  const name = h('input.field', { placeholder: 'Bölüm adı (ör. Esmâ-ül Hüsnâ)', maxlength: 30 });
  const icons = h('div.icons'), swatches = h('div.swatches');
  const paint = () => {
    icons.replaceChildren(...SECTION_ICONS.map((ic) => h('button', {
      onclick: () => { icon = ic; paint(); },
      style: ic === icon ? { borderColor: color, background: mix(color, '#151D32', 0.2) } : null
    }, ic)));
    swatches.replaceChildren(...SECTION_COLORS.map((c) => h('button.swatch' + (c === color ? '.on' : ''), { style: { background: c }, onclick: () => { color = c; paint(); } })));
  };
  paint();
  const create = () => {
    const title = name.value.trim();
    if (!title) { toast('Bölüme bir ad ver'); return; }
    const s = { id: newId(), name: title, icon, color, target: 33, hint: 'Kendi bölümün' };
    state.sections.push(s); save();
    o.remove(); onCreated(s);
  };
  o = overlay(h('div.dialog', null, h('h2', null, 'Yeni bölüm'), name,
    h('div.label.mt12.mb8', null, 'Simge'), icons, h('div.label.mt12.mb8', null, 'Renk'), swatches,
    h('div.actions', null, h('button.cancel', { onclick: () => o.remove() }, 'Vazgeç'), h('button.ok', { onclick: create }, 'Oluştur'))));
  setTimeout(() => name.focus(), 50);
}

/** "Ne ekliyorsun?" alt paneli (B). */
function addSheet() {
  let o;
  const opt = (icon, title, hint, fn, dashed) => h('button.card.opt' + (dashed ? '.dashed' : ''), { onclick: () => { o.remove(); fn(); } },
    h('span.ic', null, icon), h('div.grow', null, h('div', { style: { fontWeight: 700 } }, title), h('div.sec.ellipsis', { style: { fontSize: '12.5px' } }, hint)),
    h('span.muted', null, '›'));
  o = overlay(h('div.sheet', null, h('div.grip'), h('h2', { style: { fontSize: '18px', marginBottom: '12px' } }, 'Ne ekliyorsun?'),
    sections().map((s) => opt(s.icon, s.name, s.hint, () => go('#/add?section=' + s.id))),
    opt('＋', 'Yeni bölüm oluştur', 'Ad, simge ve renk seç', () => askNewSection((s) => go('#/add?section=' + s.id))),
    h('div.label.mt8.mb8', null, 'Ya da'),
    opt('📋', 'Panodan yapıştır', 'Kopyaladığın metni otomatik ayırır', () => go('#/add?paste=1'), true)), true);
}

/**
 * Acilir kapanir kart: basliga dokununca kapanir/acilir; secim (key) butun
 * kayitlarda hatirlanir. plain: kart cercevesi yok (metin penceresinin icinde).
 */
function foldCard(key, label, body, plain) {
  const el = h(plain ? 'div.fold.plain' : 'div.card.text-card.fold');
  const chev = h('span.chev');
  const head = h('button.fold-head', { onclick: () => { state.open[key] = state.open[key] === false; save(); paintFold(); } },
    h('span.label', null, label), chev);
  const paintFold = () => {
    const open = state.open[key] !== false;
    el.classList.toggle('closed', !open);
    head.setAttribute('aria-expanded', String(open));
    chev.textContent = open ? '▾' : '▸';
  };
  el.append(head, h('div.fold-body', null, body));
  paintFold();
  return el;
}

/* --------------------------------------------------------------- pano */

function ring(progress, color) {
  const r = 26, c = 2 * Math.PI * r;
  const svg = `<svg width="64" height="64" viewBox="0 0 64 64"><circle cx="32" cy="32" r="${r}" fill="none" stroke="#26314D" stroke-width="7"/>` +
    `<circle cx="32" cy="32" r="${r}" fill="none" stroke="${color}" stroke-width="7" stroke-linecap="round" stroke-dasharray="${(c * Math.min(1, progress)).toFixed(1)} ${c.toFixed(1)}" transform="rotate(-90 32 32)"/></svg>`;
  const el = h('div', { style: { flex: 'none', width: '64px', height: '64px' } });
  el.innerHTML = svg;
  return el;
}

function home() {
  const today = todayTotal(), goal = state.goal, st = streak();
  const date = new Date().toLocaleDateString('tr-TR', { weekday: 'long', day: 'numeric', month: 'long' });
  const last = state.last && entryOf(state.last), lt = last && state.tasbih[last.id];
  const all = entries();

  const tiles = sections().map((s) => {
    const items = all.filter((e) => e.section === s.id);
    const td = items.reduce((n, e) => n + todayOf(e.id), 0);
    return h('button.tile', {
      onclick: () => go('#/s/' + s.id),
      style: { background: `linear-gradient(160deg, ${mix(s.color, '#151D32', 0.16)}, #151D32 60%)`, borderColor: mix(s.color, '#151D32', 0.35) }
    }, h('span.ic', null, s.icon), h('span.nm', null, s.name), h('span.sec', { style: { fontSize: '12.5px' } }, `${items.length} kayıt`),
      h('span', { style: { fontSize: '12.5px', marginTop: '4px', color: s.color, fontWeight: 700 } }, 'Bugün ' + num(td)));
  });
  tiles.push(h('button.tile.new', { onclick: () => askNewSection((s) => go('#/s/' + s.id)) }, h('span', { style: { fontSize: '24px' } }, '＋'), 'Yeni bölüm'));

  const favs = state.fav.map(entryOf).filter(Boolean);

  app.replaceChildren(h('div.page', null,
    h('div.hello', null,
      h('div.grow', null, h('div.muted', { style: { fontSize: '13px' } }, date.charAt(0).toLocaleUpperCase('tr') + date.slice(1)), h('h1', null, 'Esselâmü aleyküm')),
      h('button.icon-btn', { onclick: () => go('#/s/_all'), 'aria-label': 'Ara' }, '🔍'),
      h('button.plus', { onclick: addSheet, 'aria-label': 'Ekle' }, '+')),
    h('div.scroll', null,
      h('button.card.today', {
        style: { width: '100%', textAlign: 'left' },
        onclick: () => askNumber('Günlük hedef', 'Bütün bölümlerde günde kaç okuma?', goal, (v) => { state.goal = v; save(); render(); })
      }, ring(today / goal, today >= goal ? '#7FBF8C' : '#E8B34A'),
        h('div.grow', null, h('div.label', null, 'Bugün'),
          h('div', null, h('span.big', null, num(today)), h('span.sec', null, `  / ${num(goal)} günlük hedef`)),
          h('div.sec', { style: { fontSize: '12.5px' } },
            today >= goal ? '✅ Bugünün hedefi tamam' + (st > 1 ? `  ·  🔥 ${st} gün` : '') : st > 0 ? `🔥 ${st} gün üst üste` : 'Hedefi değiştirmek için dokun'))),
      lt && lt.count > 0 && !isDone(lt) ? h('button.card.resume', { style: { width: '100%', textAlign: 'left' }, onclick: () => go('#/c/' + last.id) },
        h('div.grow', null, h('div.label', null, 'Kaldığın yer'), h('div.ellipsis', { style: { fontWeight: 700, fontSize: '16px' } }, `${last.name} · ${num(lt.count)}/${num(lt.target)}`)),
        h('span.acc', { style: { color: 'var(--accent)', fontWeight: 700 } }, 'Devam ▶')) : null,
      h('div.tiles', null, tiles),
      h('div.label.mb8', null, 'Favoriler'),
      favs.length ? h('div.chips', null, favs.map((e) => chip('★ ' + e.name, false, (sectionOf(e.section) || {}).color, () => go('#/e/' + e.id))))
        : h('p.muted', { style: { fontSize: '13px' } }, 'Bir kaydın sayfasında ☆ işaretine dokun, buraya gelsin.'),
      h('div.footer', null, 'Web sürümü · veriler bu tarayıcıda saklanır'))));
}

/* -------------------------------------------------------- bolum listesi */

function subLine(e, s, t) {
  const parts = [s ? s.name : ''];
  parts.push(t && t.total > 0 ? `${num(t.count)} / ${num(t.target)}` : hasCounter(e) ? 'Hedef ' + num(e.target) : 'Okuma');
  if (!e.arabic) parts.push('Arapça bekliyor');
  return parts.filter(Boolean).join(' · ');
}

function sectionPage(filter) {
  let query = '';
  const list = h('div');
  const titleEl = h('div.title');
  const chipsEl = h('div.chips.mt12');
  const search = h('input.search', { type: 'search', placeholder: '🔍  Ara: ad, Arapça ya da Türkçe…' });
  search.addEventListener('input', () => { query = search.value; renderList(); });

  function renderTop() {
    const s = sectionOf(filter);
    titleEl.textContent = filter === '_all' ? 'Tümü' : filter === '_fav' ? 'Favoriler' : s ? `${s.icon}  ${s.name}` : 'Tümü';
    chipsEl.replaceChildren(
      chip('Tümü', filter === '_all', null, () => { filter = '_all'; replaceRoute('#/s/_all'); renderTop(); }),
      chip('★', filter === '_fav', null, () => { filter = '_fav'; replaceRoute('#/s/_fav'); renderTop(); }),
      ...sections().map((x) => chip(x.name, filter === x.id, x.color, () => { filter = x.id; replaceRoute('#/s/' + x.id); renderTop(); })));
    renderList();
  }

  function renderList() {
    const items = entries().filter((e) => (filter === '_all' || (filter === '_fav' ? state.fav.includes(e.id) : e.section === filter)) && matches(e, query));
    if (!items.length) {
      list.replaceChildren(h('p.empty', null, query.trim() ? `“${query}” için sonuç yok.`
        : filter === '_fav' ? 'Henüz favori yok. Bir kaydın sayfasında ☆ işaretine dokun.'
          : 'Bu bölümde henüz kayıt yok.\nAşağıdaki + Ekle ile internetten bulduğun metni yapıştır.'));
      return;
    }
    list.replaceChildren(...items.map((e) => {
      const s = sectionOf(e.section);
      return h('button.list-row', { onclick: () => go('#/e/' + e.id) },
        h('span.strip', { style: { background: s ? s.color : '#E8B34A' } }),
        h('div.grow', null, h('div.nm', null, e.name), h('div.sub' + (e.arabic ? '.sec' : '.warn'), null, subLine(e, s, state.tasbih[e.id]))),
        e.arabic ? h('div.ar', null, e.arabic) : null);
    }));
  }

  app.replaceChildren(h('div.page', null,
    h('div.head', null, h('button.icon-btn.back', { onclick: back }, '‹'), titleEl),
    h('div', { style: { padding: '4px 16px 6px' } }, search, chipsEl),
    h('div.scroll', { style: { paddingBottom: '96px' } }, list),
    h('button.fab', { onclick: () => (filter.startsWith('_') ? addSheet() : go('#/add?section=' + filter)) }, '＋ Ekle')));
  renderTop();
}

/* -------------------------------------------------------- kayit sayfasi */

function entryPage(id) {
  const e = entryOf(id);
  if (!e) { toast('Kayıt bulunamadı'); go('#/'); return; }
  const s = sectionOf(e.section);
  const star = h('button.icon-btn');
  const paintStar = () => { const on = state.fav.includes(e.id); star.textContent = on ? '★' : '☆'; star.style.color = on ? 'var(--accent)' : ''; };
  star.addEventListener('click', () => {
    const on = !state.fav.includes(e.id);
    state.fav = on ? state.fav.concat(e.id) : state.fav.filter((x) => x !== e.id);
    save(); paintStar(); toast(on ? 'Favorilere eklendi' : 'Favorilerden çıkarıldı');
  });
  paintStar();
  const size = (d) => {
    state.ar = Math.min(44, Math.max(12, state.ar + d * 2));
    state.tx = Math.min(30, Math.max(12, state.tx + d * 1.5));
    save(); applySizes();
  };

  const card = (label, body) => h('div.card.text-card', null, h('div.label', null, label), body);
  const fold = (key, label, body) => foldCard(key, label, body);
  const origin = [e.attribution && 'Nispet: ' + e.attribution, e.source && 'Kaynak: ' + e.source, e.note].filter(Boolean).join('\n\n');

  const stats = h('div.sec.ellipsis', { style: { fontSize: '13px' } });
  const targets = h('div.targets');
  function paint() {
    const t = state.tasbih[e.id];
    const target = t ? t.target : e.target;
    stats.textContent = t && t.total > 0 ? `Bugün ${num(todayOf(e.id))}  ·  Toplam ${num(t.total)}  ·  Şu an ${num(t.count)} / ${num(t.target)}`
      : hasCounter(e) ? 'Okuma sayısını seç ve başla' : 'Okuma sayfası · sayaç isteğe bağlı';
    const presets = [...new Set([e.target, 33, 100, 1000].filter((v) => v > 0))];
    const custom = target > 0 && !presets.includes(target);
    const setTarget = (v) => { const tt = tasbihFor(e); tt.target = v; tt.count = Math.min(tt.count, v); save(); paint(); };
    targets.replaceChildren(...presets.map((v) => chip(num(v), v === target, null, () => setTarget(v))),
      chip(custom ? num(target) : 'Özel…', custom, null, () => askNumber('Okuma sayısı', 'Kaç kere okunacak?', target > 0 ? target : '', setTarget)));
  }

  const menu = () => choose(e.name, [
    { label: '✏️  Düzenle', fn: () => go(`#/add?edit=${e.id}`) },
    { label: '🗑️  Sil', fn: () => confirmDlg('Kaydı sil', `"${e.name}" ve sayacı silinsin mi? Bu geri alınamaz. Günlük toplamlar kalır.`, 'Sil', () => {
      state.entries = state.entries.filter((x) => x.id !== e.id);
      state.fav = state.fav.filter((x) => x !== e.id);
      delete state.tasbih[e.id];
      if (state.last === e.id) state.last = null;
      save(); toast('Silindi'); back();
    }) }
  ]);

  app.replaceChildren(h('div.page', null,
    h('div.head', null, h('button.icon-btn.back', { onclick: back }, '‹'), h('div.title', null, e.name), star,
      h('button.icon-btn', { onclick: () => size(-1), style: { fontSize: '16px' }, 'aria-label': 'Yazıyı küçült' }, 'A−'),
      h('button.icon-btn', { onclick: () => size(+1), style: { fontSize: '18px' }, 'aria-label': 'Yazıyı büyüt' }, 'A+'),
      e.user ? h('button.icon-btn', { onclick: menu }, '⋮') : null),
    h('div.scroll', null,
      h('div.mb8', { style: { fontSize: '13.5px', color: s ? s.color : 'var(--muted)' } }, [s && `${s.icon} ${s.name}`, e.aka].filter(Boolean).join('  ·  ')),
      e.arabic ? fold('arabic', 'Arapça', h('div.ar.arabic-text', null, e.arabic))
        : fold('arabic', 'Arapça', h('div.warn', null, 'Arapça bekliyor' + (e.user ? ' · bulunca ⋮ → Düzenle ile ekle.' : '.'))),
      e.reading ? fold('reading', 'Okunuşu', h('div.reading-text', null, e.reading)) : null,
      e.meaning ? fold('meaning', 'Anlamı', h('div.meaning-text.sec', null, e.meaning)) : null,
      origin ? fold('source', 'Kaynak', h('div.sec', { style: { fontSize: '13.5px', whiteSpace: 'pre-line' } }, origin)) : null,
      !e.verified && !e.user ? h('p.warn', { style: { fontSize: '12.5px' } }, '⚠ Bu metin henüz ikinci bir kaynakla harf harf karşılaştırılmadı.') : null),
    h('div.bottom-bar', null, stats, targets,
      h('button.primary', { onclick: () => { tasbihFor(e); go('#/c/' + e.id); }, style: { background: s ? s.color : 'var(--accent)' } }, '📿  Tespihle oku'))));
  paint();
}

/* --------------------------------------------------- yapistir ve ekle */

function addPage(params) {
  const editing = params.get('edit') ? state.entries.find((x) => x.id === params.get('edit')) : null;
  let sectionId = editing ? editing.section : (sectionOf(params.get('section')) ? params.get('section') : 'salavat');
  let target = editing ? editing.target : null;

  const paste = h('textarea.field', { rows: 5, placeholder: 'İnternetten kopyaladığın metni buraya yapıştır: Arapça, okunuş, anlam, kaynak… Hepsi bir arada olabilir.' });
  const tag = h('span.ok', { style: { fontSize: '12.5px' } });
  const f = {
    name: h('input.field', { placeholder: 'Ad (boş bırakırsan okunuştan alınır)' }),
    arabic: h('textarea.field.ar', { rows: 2, placeholder: 'Arapça (yoksa boş bırak)', dir: 'rtl' }),
    reading: h('textarea.field', { rows: 2, placeholder: 'Türkçe okunuş' }),
    meaning: h('textarea.field', { rows: 2, placeholder: 'Türkçe anlam' }),
    source: h('input.field', { placeholder: 'Kaynak / nispet (site, kitap, kime nispet)' })
  };
  if (editing) {
    f.name.value = editing.name; f.arabic.value = editing.arabic || ''; f.reading.value = editing.reading || '';
    f.meaning.value = editing.meaning || ''; f.source.value = [editing.attribution, editing.source].filter(Boolean).join(' · ');
  }

  function split(text) {
    const r = PasteParser.parse(text);
    if (PasteParser.isEmpty(r)) { toast('Ayrılacak metin yok'); return; }
    for (const k of Object.keys(f)) if (r[k]) f[k].value = r[k];
    tag.textContent = '✓ ayrıldı: ' + [[r.arabic, 'Arapça'], [r.reading, 'okunuş'], [r.meaning, 'anlam'], [r.source, 'kaynak']].filter((x) => x[0]).map((x) => x[1]).join(', ');
  }
  async function fromClipboard() {
    try {
      const text = await navigator.clipboard.readText();
      if (!text.trim()) throw new Error('bos');
      paste.value = text; split(text);
    } catch (e) {
      toast('Panoya erişilemedi: metni kutuya uzun basıp yapıştır');
      paste.focus();
    }
  }
  // Kutuya yapistirinca kendiliginden ayir.
  paste.addEventListener('paste', () => setTimeout(() => split(paste.value), 0));

  const sectionChips = h('div.chips'), targetChips = h('div.chips.mt8');
  function paintChips() {
    sectionChips.replaceChildren(...sections().map((s) => chip(`${s.icon} ${s.name}`, s.id === sectionId, s.color, () => { sectionId = s.id; paintChips(); })),
      chip('＋ Yeni bölüm', false, null, () => askNewSection((s) => { sectionId = s.id; paintChips(); })));
    const s = sectionOf(sectionId) || SECTIONS[0];
    const cur = target != null ? target : s.target;
    const custom = cur > 0 && ![33, 100].includes(cur);
    targetChips.replaceChildren(chip('Sayaçsız', cur === 0, s.color, () => { target = 0; paintChips(); }),
      ...[33, 100].map((v) => chip(num(v), cur === v, s.color, () => { target = v; paintChips(); })),
      chip(custom ? num(cur) : 'Özel…', custom, s.color, () => askNumber('Okuma sayısı', 'Kaç kere okunacak?', cur > 0 ? cur : '', (v) => { target = v; paintChips(); })));
  }
  paintChips();

  function saveEntry() {
    const arabic = f.arabic.value.trim(), reading = f.reading.value.trim(), meaning = f.meaning.value.trim();
    if (!arabic && !reading && !meaning) { toast('Arapça, okunuş ya da anlamdan en az biri gerekli'); return; }
    const name = f.name.value.trim() || PasteParser.autoName({ reading, meaning }) || 'Adsız';
    const s = sectionOf(sectionId) || SECTIONS[0];
    const e = {
      id: editing ? editing.id : newId(), section: sectionId, name: name.substring(0, 60), aka: '',
      arabic, reading, meaning, attribution: '', source: f.source.value.trim(),
      target: target != null ? target : s.target, note: '', verified: false, user: true,
      createdAt: editing ? editing.createdAt : Date.now()
    };
    if (editing) state.entries = state.entries.map((x) => (x.id === e.id ? e : x));
    else state.entries.push(e);
    const t = state.tasbih[e.id];
    if (t && e.target > 0) { t.target = e.target; t.count = Math.min(t.count, e.target); }
    save();
    toast(editing ? 'Kaydedildi' : 'Eklendi');
    if (editing) back(); else replaceRoute('#/e/' + e.id, true);
  }

  const lbl = (text) => h('label.field-label', null, text);
  app.replaceChildren(h('div.page', null,
    h('div.head', null, h('button.icon-btn.back', { onclick: back }, '‹'), h('div.title', null, editing ? 'Düzenle' : 'Yapıştır ve ekle')),
    h('div.scroll', null,
      editing ? null : [h('div.label', null, '1 · İnternetten kopyaladığın metin'), paste,
        // Gomulu sayfada pano okunamiyor: kutuya uzun basip Yapistir (yapistirinca kendiliginden ayrilir).
        h('div.row.mt8', { style: { gap: '8px' } },
          EMBED ? h('p.muted.grow', { style: { fontSize: '12.5px' } }, 'Kutuya uzun bas → Yapıştır. Metin kendiliğinden ayrılır.')
            : h('button.secondary.grow', { onclick: fromClipboard }, '📋  Panodan yapıştır'),
          h('button.secondary.grow', { onclick: () => split(paste.value) }, 'Ayır  ↓'))],
      h('div.row', { style: { marginTop: editing ? '4px' : '18px' } }, h('div.label.grow', null, editing ? 'Metin' : '2 · Kontrol et, gerekirse düzelt'), tag),
      lbl('AD'), f.name, lbl('ARAPÇA'), f.arabic, lbl('OKUNUŞ'), f.reading, lbl('ANLAM'), f.meaning, lbl('KAYNAK'), f.source,
      h('p.muted.mt8', { style: { fontSize: '12.5px' } }, 'Arapçası yoksa boş bırak: kayıt "Arapça bekliyor" etiketiyle durur, bulunca Düzenle\'den eklersin.'),
      h('div.label', { style: { marginTop: '18px', marginBottom: '8px' } }, editing ? 'Bölüm ve okuma sayısı' : '3 · Bölüm ve okuma sayısı'),
      sectionChips, targetChips),
    h('div.bottom-bar', null, h('button.primary', { onclick: saveEntry, style: { marginTop: 0 } }, 'Kaydet'))));
  if (!editing && params.get('paste')) { if (EMBED) paste.focus(); else fromClipboard(); }
}

/* --------------------------------------------------------------- tespih */

/**
 * Tespih Gorunumu (workplanner'daki TasbihAppearancePicker): ustte buyuk
 * onizleme, altta 10 materyal; "Bu görünümü kullan" ile onPick.
 */
function pickMaterial(current, onPick) {
  let selected = current, o;
  const preview = h('div', { style: { width: '132px', height: '132px', flex: 'none' } });
  const infoName = h('div', { style: { fontSize: '19px', fontWeight: 700 } });
  const infoDesc = h('div.sec', { style: { fontSize: '14px', marginTop: '6px' } });
  const grid = h('div.mat-grid');
  const use = h('button.primary', { onclick: () => { o.remove(); onPick(selected); } }, 'Bu görünümü kullan');
  const paint = () => {
    preview.innerHTML = Rosary.preview(selected);
    infoName.textContent = materialTitle(selected);
    infoDesc.textContent = selected.desc;
    use.style.background = selected.accent;
    grid.replaceChildren(...MATERIALS.map((m) => {
      const on = m.id === selected.id;
      const icon = h('div.mat-icon');
      icon.innerHTML = Rosary.preview(m);
      return h('button.mat-tile' + (on ? '.on' : ''), {
        onclick: () => { selected = m; paint(); }, 'aria-label': materialTitle(m), 'aria-pressed': String(on),
        style: on ? { borderColor: m.accent, background: mix(m.accent, '#151D32', 0.12) } : null
      }, icon, h('div.mat-name', null, m.label), m.detail ? h('div.muted', { style: { fontSize: '11px' } }, `(${m.detail})`) : null);
    }));
  };
  paint();
  o = overlay(h('div.mat-sheet', null,
    h('div.head', null, h('button.icon-btn.back', { onclick: () => o.remove() }, '‹'), h('div.title', null, 'Tespih Görünümü')),
    h('div.scroll', null,
      h('div.card.row', { style: { padding: '12px 16px 12px 12px', gap: '14px' } }, preview, h('div.grow', null, infoName, infoDesc)),
      h('div', { style: { fontSize: '16px', fontWeight: 700, margin: '20px 0 10px' } }, 'Tespih Türü (Renk ve Materyal)'),
      grid),
    h('div', { style: { padding: '0 16px 16px' } }, use)), true);
}

let counterTicker = null;

/**
 * Okuma modundaki kucuk yuvarlak sayac (workplanner'daki balon gibi): ilerleme
 * halkasi, materyal renginde boncuklar, ortada sayi. Dokununca +1.
 */
function miniCounter(m, onTap) {
  const S = 100, c = 50, pr = 46, circ = 2 * Math.PI * pr;
  let dots = '';
  for (let i = 0; i < 12; i++) {
    const a = (i * 30 - 90) * Math.PI / 180;
    dots += `<circle cx="${(c + 36 * Math.cos(a)).toFixed(1)}" cy="${(c + 36 * Math.sin(a)).toFixed(1)}" r="4.2" fill="${m.base}" stroke="${m.light}" stroke-opacity=".5" stroke-width="1"/>`;
  }
  const el = h('button.mini', { onclick: onTap, 'aria-label': 'Say: +1' });
  el.innerHTML = `<svg viewBox="0 0 ${S} ${S}" aria-hidden="true">
    <circle cx="${c}" cy="${c}" r="${pr + 3}" fill="#151D32"/>
    <circle cx="${c}" cy="${c}" r="${pr}" fill="none" stroke="#26314D" stroke-width="5"/>
    <circle class="mini-arc" cx="${c}" cy="${c}" r="${pr}" fill="none" stroke="${m.accent}" stroke-width="5" stroke-linecap="round" transform="rotate(-90 ${c} ${c})" stroke-dasharray="0 ${circ.toFixed(1)}"/>
    ${dots}
    <text class="mini-cnt" x="${c}" y="${c + 6}" fill="#F2EFE6" font-size="22" font-weight="700" text-anchor="middle"></text>
    <text class="mini-tgt" x="${c}" y="${c + 21}" fill="#AEB5CF" font-size="10" text-anchor="middle"></text>
  </svg>`;
  const arc = el.querySelector('.mini-arc'), cnt = el.querySelector('.mini-cnt'), tgt = el.querySelector('.mini-tgt');
  return {
    el,
    set(count, target, done, animated) {
      cnt.textContent = num(count);
      cnt.setAttribute('font-size', String(count >= 10000 ? 16 : count >= 1000 ? 19 : 22));
      tgt.textContent = done ? 'Tamam ✓' : '/ ' + num(target);
      arc.setAttribute('stroke-dasharray', `${(circ * Math.min(1, count / target)).toFixed(1)} ${circ.toFixed(1)}`);
      if (animated) { el.classList.remove('pulse'); void el.offsetWidth; el.classList.add('pulse'); }
    }
  };
}

function counterPage(id) {
  const e = entryOf(id);
  if (!e) { toast('Kayıt bulunamadı'); go('#/'); return; }
  const t = tasbihFor(e);
  state.last = id; save();
  // Halka, +1 dugmesi ve yazilarin rengi tespihin materyalinden (workplanner'daki gibi).
  const mat = materialOf(t);
  const color = mat.accent;

  const roundVal = h('div.val'), roundSub = h('div.sec', { style: { fontSize: '13px' } });
  const totalVal = h('div.val');
  // Sure: bu tur, kalan tahmini, hiz; altinda tamamlanan turlarin ozeti.
  const timeLine = h('div.sec', { style: { fontSize: '13px', textAlign: 'center', marginTop: '7px', whiteSpace: 'pre-line', fontVariantNumeric: 'tabular-nums' } });
  // Tespih: sagdaki bosluktan asagi kaydir ya da dokun, bir boncuk (rosary.js).
  // Okuma modu (⧉ Küçült): tespih kucuk yuvarlak sayaca doner, metin ekrani kaplar.
  const readMode = !!state.readMode;
  const rosary = readMode ? null : Rosary.create(mat, () => count());
  const stage = rosary ? h('div.stage', null, rosary.el) : null;
  const mini = readMode ? miniCounter(mat, () => count()) : null;
  const soundVal = h('div.sec', { style: { fontSize: '12.5px' } }), vibVal = h('div.sec', { style: { fontSize: '12.5px' } });
  const flash = h('div.flash', { style: { background: color } });
  /*
   * Metin penceresi: Arapca, okunus ve anlam alt alta, her biri acilir kapanir;
   * uzun metin pencerenin icinde kayar. Pencere de basligindan acilir kapanir.
   * Okuma modunda hep acik ve ekranin geri kalanini kaplar.
   */
  const readerBody = h('div.reader-body');
  const rdChev = h('span.chev');
  const rdHead = h('button.reader-head', { onclick: () => { state.open.counter = state.open.counter === false; save(); paintText(); } },
    h('span.label', null, 'Metin'), rdChev);
  const reader = h('div.card.reader', null, readMode ? null : rdHead, readerBody);
  function paintText() {
    const open = readMode || state.open.counter !== false;
    readerBody.hidden = !open;
    rdChev.textContent = open ? '▾' : '▸';
    rdHead.setAttribute('aria-expanded', String(open));
    const parts = [['arabic', 'Arapça', e.arabic, 'div.ar.arabic-text'], ['reading', 'Okunuşu', e.reading, 'div.reading-text'], ['meaning', 'Anlamı', e.meaning, 'div.meaning-text.sec']]
      .filter((x) => x[2]).map(([key, label, text, cls]) => foldCard(key, label, h(cls, null, text), true));
    readerBody.replaceChildren(...parts);
  }

  function paint(animated) {
    roundVal.textContent = num(t.rounds);
    roundSub.textContent = `${num(t.count)} / ${num(t.target)}`;
    totalVal.textContent = num(t.total);
    paintTime();
    if (rosary) {
      rosary.setProgress(t.count / t.target);
      rosary.setCenter(num(t.count), '/ ' + num(t.target), isDone(t) ? 'Tamamlandı ✓' : e.name, isDone(t) ? '#7FBF8C' : color);
      rosary.setPulled(t.total, animated);
    }
    if (mini) mini.set(t.count, t.target, isDone(t), animated);
    soundVal.textContent = t.sound ? 'Açık' : 'Kapalı';
    vibVal.textContent = t.every === 0 ? 'Kapalı' : t.every === 1 ? 'Her adette' : `Her ${t.every} adette`;
  }

  /**
   * "⏱ 1:48 · ≈ 3 dk kaldı · ~34/dk" ve altinda tamamlanan turlar. Sure son
   * dokunustan beri akar; bekleme sinirini gecince duraklar.
   */
  function paintTime() {
    const T = TasbihCore, now = Date.now(), idle = idleMs();
    const live = T.liveRoundMs(t, now, idle);
    const paused = t.lastTapAt > 0 && now - t.lastTapAt > idle;
    let eta = '';
    if (!isDone(t)) {
      const ms = T.etaMs(t.intervals, t.target - t.count);
      eta = ms == null ? ` · tahmin ${T.SPEED_MIN} çekişten sonra` : ` · ${T.approx(ms)} kaldı · ~${T.perMinute(t.intervals)}/dk`;
    }
    const first = isDone(t) && t.lastRoundMs > 0 ? `⏱ Tur ${T.format(t.lastRoundMs)} · adet başına ${T.perBead(t.lastRoundMs, t.target)}`
      : live <= 0 && t.count === 0 && !t.intervals.length ? '⏱ İlk çekişte süre başlar'
        : `⏱ ${T.format(live)}${paused ? ' · duraklatıldı' : ''}${eta}`;
    const summary = T.summary(t);
    timeLine.textContent = summary ? first + '\n' + summary : first;
  }

  function count() {
    const ev = tap(id);
    if (ev === 'blocked') {
      // Yarim cekilmis boncuk yerine geri donsun.
      if (rosary) rosary.setPulled(t.total, true);
      vib([30, 60, 30]); toast('Hedefe ulaşıldı. Yeniden başlamak için Sıfırla.'); return;
    }
    if (ev === 'completed') {
      if (t.finish) vib([0, 700, 250, 700]);
      if (t.sound) beep(true);
      flash.style.transition = 'none'; flash.style.opacity = '0.3';
      requestAnimationFrame(() => { flash.style.transition = ''; flash.style.opacity = '0'; });
      toast(t.onTarget === 'stop' ? 'Tamamlandı ✓' : 'Tur tamamlandı ✓');
    } else if (ev === 'milestone') { vib(t.every > 1 ? 60 : 25); if (t.sound) beep(false); }
    else if (t.sound) beep(false);
    paint(true);
  }

  const reset = () => choose('Sıfırla · ' + e.name, [
    { label: 'Sayacı ve turu sıfırla  (toplam kalır)', fn: () => { TasbihCore.resetCounter(t); save(); paint(); toast('Sayaç sıfırlandı'); } },
    { label: 'Toplamı da sıfırla', fn: () => confirmDlg('Toplam da sıfırlansın mı?', `"${e.name}" için sayaç, tur, toplam (${num(t.total)} adet) ve süre istatistiği sıfırlanacak. Günlük geçmiş kalır.`, 'Sıfırla',
      () => { TasbihCore.resetAll(t); save(); paint(); toast('Sıfırlandı'); }) }
  ]);
  const pickEvery = () => choose('Titreşim', EVERY_PRESETS.map((v) => ({
    label: v === 0 ? 'Kapalı' : v === 1 ? 'Her adette' : `Her ${v} adette`, on: v === t.every,
    fn: () => { t.every = v; save(); paint(); }
  })).concat([{ label: 'Özel…', fn: () => askNumber('Özel titreşim aralığı', 'Kaç adette bir titresin?', t.every > 1 ? t.every : '', (v) => { t.every = v; save(); paint(); }) }]));
  const menu = () => choose(e.name, [
    { label: `🎯  Hedef: ${num(t.target)}`, fn: () => askNumber('Hedef', 'Kaç kere okunacak?', t.target, (v) => { t.target = v; t.count = Math.min(t.count, v); save(); paint(); }) },
    { label: t.onTarget === 'stop' ? '🔁  Hedefte: dur  →  yeni tur başlat' : '⏹  Hedefte: yeni tur  →  dur', fn: () => { t.onTarget = t.onTarget === 'stop' ? 'round' : 'stop'; save(); paint(); } },
    { label: `🏁  Bitiş titreşimi: ${t.finish ? 'Açık' : 'Kapalı'}`, fn: () => { t.finish = !t.finish; save(); if (t.finish) vib([0, 700]); } },
    { label: `🎨  Tespih görünümü: ${mat.label}`, fn: () => pickMaterial(mat, (m) => { t.material = m.id; delete t.bead; save(); render(); }) },
    { label: `⏸  Süre duraklaması: ${TasbihCore.idleLabel(idleMs())} ara verince`, fn: () => choose('Süre ne zaman duraklasın?', TasbihCore.IDLE_OPTIONS.map((ms) => ({
      label: `${TasbihCore.idleLabel(ms)} dokunulmazsa`, on: ms === idleMs(), fn: () => { state.idle = ms; save(); paint(); }
    }))) },
    { label: '🔄  Sıfırla', fn: reset },
    { label: '📈  Geçmiş', fn: () => {
      const days = Object.entries(state.hist[id] || {}).filter((d) => d[1] > 0).sort((a, b) => b[0].localeCompare(a[0])).slice(0, 30);
      choose('📈 ' + e.name, days.length ? days.map(([d, n]) => ({ label: `${d}: ${num(n)}`, fn: () => {} })) : [{ label: 'Henüz kayıt yok', fn: () => {} }]);
    } }
  ]);

  const undoTap = () => { if (undo(id)) { vib(20); paint(true); } else toast('Geri alınacak bir şey yok'); };
  const toggleRead = () => { state.readMode = !readMode; save(); render(); };
  const header = h('div.row', { style: { height: '44px', flex: 'none' } },
    h('button.icon-btn.back', { onclick: back }, '‹'),
    h('div.grow.ellipsis', { style: { textAlign: 'center', fontSize: '20px', fontWeight: 700 } }, e.name),
    h('button.icon-btn', { onclick: toggleRead, 'aria-label': readMode ? 'Tespihe dön' : 'Küçült: okuma modu', title: readMode ? 'Tespihe dön' : 'Küçült' }, readMode ? '📿' : '⧉'),
    h('button.icon-btn', { onclick: menu }, '⋮'));

  if (readMode) {
    // Okuma modu: solda sayi ve sure, sagda yuvarlak sayac (dokun: +1); altta metin.
    app.replaceChildren(h('div.counter.read', null,
      header,
      h('div.read-top', null,
        h('div.card.grow.read-stats', null,
          h('div.label', null, 'Tur · Toplam'),
          h('div', null, roundVal, h('span.sec', null, ' tur  ·  '), totalVal, h('span.sec', null, ' adet')),
          roundSub, timeLine),
        h('div.mini-col', null, mini.el, h('button.mini-undo', { onclick: undoTap }, '↶ Geri al'))),
      reader,
      flash));
  } else {
    app.replaceChildren(h('div.counter', null,
      header,
      h('div.stats', null,
        h('div.card', null, h('div.label', null, 'Tur'), roundVal, roundSub),
        h('div.card', null, h('div.label', null, 'Toplam'), totalVal, h('div.sec', { style: { fontSize: '13px' } }, 'adet'))),
      timeLine,
      reader,
      stage,
      t.total < 5 ? h('p.muted', { style: { textAlign: 'center', fontSize: '12.5px', marginBottom: '8px' } }, 'Aşağı kaydır: bir boncuk çek  ·  dokunmak da sayar') : null,
      h('div.round-btns', null,
        h('button.round-btn', { onclick: undoTap }, '↶', h('small', null, 'Geri al')),
        h('button.round-btn', { onclick: reset }, '⟲', h('small', null, 'Sıfırla')),
        h('button.round-btn', { onclick: menu }, '⋮', h('small', null, 'Diğer'))),
      h('div.quick', null,
        h('button.card', { onclick: () => { t.sound = !t.sound; save(); paint(); if (t.sound) beep(false); } },
          h('span', { style: { fontSize: '17px' } }, '🔊'), h('div.grow', null, h('div', { style: { fontWeight: 700, fontSize: '14px' } }, 'Ses'), soundVal), h('span.muted', null, '›')),
        h('button.card', { onclick: pickEvery },
          h('span', { style: { fontSize: '17px' } }, '📳'), h('div.grow', null, h('div', { style: { fontWeight: 700, fontSize: '14px' } }, 'Titreşim'), vibVal), h('span.muted', null, '›'))),
      h('button.plus1', { onclick: count, style: { background: color, border: `1px solid ${mix(color, '#FFFFFF', 0.7)}` } },
        h('b', null, '+1'), h('span', { style: { fontSize: '12.5px' } }, 'Dokun, say')),
      flash));
  }
  paintText();
  paint();
  keepAwake(true);
  // Sure satiri saniyede bir tazelenir (ekran degisince render() durdurur).
  counterTicker = setInterval(paintTime, 1000);
}

/* ---------------------------------------------------------- yonlendirme */

function applySizes() {
  document.documentElement.style.setProperty('--ar-size', state.ar + 'px');
  document.documentElement.style.setProperty('--tx-size', state.tx + 'px');
}

function render() {
  document.querySelectorAll('.overlay').forEach((o) => o.remove());
  clearInterval(counterTicker);
  const [path, query] = route().replace(/^#\/?/, '').split('?');
  const [page, arg] = path.split('/');
  const params = new URLSearchParams(query || '');
  if (page !== 'c') keepAwake(false);
  applySizes();
  window.scrollTo(0, 0);
  if (page === 's') sectionPage(arg ? decodeURIComponent(arg) : '_all');
  else if (page === 'e') entryPage(decodeURIComponent(arg || ''));
  else if (page === 'add') addPage(params);
  else if (page === 'c') counterPage(decodeURIComponent(arg || ''));
  else home();
}

if (!EMBED) window.addEventListener('hashchange', render);
document.addEventListener('visibilitychange', () => { if (document.visibilityState === 'visible' && route().startsWith('#/c/')) keepAwake(true); });

// Gomulu sayfada hazir kayitlar sayfanin icinde (SALAVAT_DATA), normalde salavat.json.
(window.SALAVAT_DATA ? Promise.resolve(window.SALAVAT_DATA) : fetch('salavat.json').then((r) => r.json())).then((data) => {
  builtin = data.filter((e) => e.id && e.name && (e.arabic || e.reading || e.meaning))
    .map((e) => Object.assign({ section: 'salavat', target: 100, aka: '', arabic: '', reading: '', meaning: '', attribution: '', source: '', note: '' }, e, { user: false }));
}).catch(() => toast('Hazır kayıtlar yüklenemedi')).finally(render);

if (!EMBED && 'serviceWorker' in navigator) navigator.serviceWorker.register('sw.js').catch(() => {});
