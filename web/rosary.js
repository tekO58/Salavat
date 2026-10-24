// Tespih cizimi (SVG). workplanner-app'teki TasbihMaterial / TasbihBeadPainter /
// TasbihRosaryView'un web karsiligi: materyaller, boncuk katmanlari (golge, kure,
// isik gecirgenligi, doku, yansima), ara halkalar, nisaneler, puskullu imame ve
// sagda sabit cekme boslugundan akan 33'luk tespih. Dizilis hesabi tasbih.js'te.
(function (root) {
  /*
   * Materyaller: workplanner'daki ile ayni adlar ve renkler. light/base/dark
   * govde tonlari, gloss parlaklik (0 mat..1 cam), glow isik gecirgenligi,
   * texture yuzey dokusu, metal ara parcalar, tassel puskul, accent halka,
   * +1 dugmesi ve yazilarin rengi.
   */
  const METALS = { gold: ['#FFF1B8', '#C99A3A', '#5E4210'], silver: ['#FFFFFF', '#A7ADB8', '#3B4049'], bronze: ['#F3C79A', '#9A6434', '#3E2410'] };
  const MATERIALS = [
    { id: 'KUKA', label: 'Kuka', detail: 'doğal', desc: 'Doğal ahşap dokusu, mat ve sıcak.', light: '#B88458', base: '#6E4326', dark: '#2E1A0D', gloss: 0.18, glow: 0, texture: 'wood', metal: 'bronze', tassel: '#8A5A33', accent: '#C98E5A' },
    { id: 'KEHRIBAR', label: 'Kehribar', detail: 'bal rengi', desc: 'Parlak, yarı saydam; ışığı içine alır.', light: '#FFE08A', base: '#E39A1E', dark: '#7A3E05', gloss: 0.85, glow: 0.75, texture: 'cloud', metal: 'gold', tassel: '#C7861E', accent: '#F2A93B' },
    { id: 'TURUNCU_KEHRIBAR', label: 'Turuncu kehribar', detail: 'koyu bal', desc: 'Sıcak turuncu, yarı saydam ve parlak.', light: '#FFB27A', base: '#E0561C', dark: '#6E1E05', gloss: 0.85, glow: 0.65, texture: 'cloud', metal: 'gold', tassel: '#C4521E', accent: '#F07A2A' },
    { id: 'YESIL', label: 'Yeşil akik', detail: 'doğal taş', desc: 'Doğal taş görünümü, parlak ve derin renk.', light: '#7FD9A0', base: '#1E7A45', dark: '#0A2E19', gloss: 0.75, glow: 0.2, texture: 'veins', metal: 'silver', tassel: '#1F6B40', accent: '#2FBF71' },
    { id: 'SIYAH', label: 'Siyah', detail: 'oniks', desc: 'Derin siyah, ayna gibi parlak.', light: '#5A5F6B', base: '#15161A', dark: '#000000', gloss: 1, glow: 0, texture: 'none', metal: 'silver', tassel: '#26282E', accent: '#A9B1C2' },
    { id: 'LACIVERT', label: 'Lacivert', detail: 'parlak', desc: 'Koyu mavi, cam gibi parlak.', light: '#7FA4FF', base: '#1C3FA8', dark: '#07153F', gloss: 0.9, glow: 0.35, texture: 'none', metal: 'silver', tassel: '#1E3A8A', accent: '#5B84F0' },
    { id: 'MOR', label: 'Mor', detail: 'ametist', desc: 'Ametist moru, yarı saydam kristal.', light: '#D6B4FF', base: '#7A3FC4', dark: '#2A0E4F', gloss: 0.85, glow: 0.5, texture: 'cloud', metal: 'silver', tassel: '#6A35A8', accent: '#B08AF5' },
    { id: 'KIRMIZI', label: 'Kırmızı', detail: 'akik', desc: 'Kırmızı akik, parlak ve derin.', light: '#FF9A8A', base: '#B5161E', dark: '#45050A', gloss: 0.8, glow: 0.4, texture: 'cloud', metal: 'gold', tassel: '#9E1B22', accent: '#E8535A' },
    { id: 'BEYAZ', label: 'Beyaz / krem', detail: 'howlit', desc: 'Kırık beyaz taş, ince gri damarlı.', light: '#FFFFFF', base: '#E6E1D6', dark: '#8E887C', gloss: 0.45, glow: 0, texture: 'veins', metal: 'gold', tassel: '#CFC8B8', accent: '#EDE7DA', veinColor: '#7D776B' },
    { id: 'TURKUAZ', label: 'Turkuaz', detail: 'doğal taş', desc: 'Doğal turkuaz, koyu damarlı, yarı mat.', light: '#9BF0EA', base: '#1AA6A0', dark: '#064A48', gloss: 0.4, glow: 0, texture: 'matrix', metal: 'silver', tassel: '#148C87', accent: '#2EC4C0' }
  ];

  /** Her materyal hep ayni dokuyu alsin diye tohumlu rastgele (mulberry32). */
  function rng(seed) {
    let a = seed >>> 0;
    return () => {
      a = (a + 0x6D2B79F5) >>> 0;
      let t = a;
      t = Math.imul(t ^ (t >>> 15), t | 1);
      t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
      return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
    };
  }

  function mixHex(color, base, ratio) {
    const a = parseInt(color.slice(1), 16), b = parseInt(base.slice(1), 16);
    const ch = (s) => Math.round(((a >> s) & 255) * ratio + ((b >> s) & 255) * (1 - ratio));
    return '#' + [16, 8, 0].map((s) => ch(s).toString(16).padStart(2, '0')).join('');
  }

  const f = (n) => n.toFixed(2);
  let uid = 0;

  /**
   * Materyalin parca cizimleri (<defs>): boncuk (r=10, merkez 0,0), ara halka,
   * nisane ve imame. Donen: { defs, bead, spacer, sep, ima } kimlikleri.
   */
  function defsFor(m) {
    const u = 'r' + (++uid);
    const metal = METALS[m.metal] || METALS.silver;
    const r = 10, rand = rng(MATERIALS.indexOf(m) * 31 + 7);
    const metalLinear = (id) => `<linearGradient id="${id}" x1="0" x2="1" y1="0" y2="0"><stop offset="0" stop-color="${metal[2]}"/><stop offset=".35" stop-color="${metal[0]}"/><stop offset=".6" stop-color="${metal[1]}"/><stop offset="1" stop-color="${metal[2]}"/></linearGradient>`;

    // Doku: yalnizca boncugun icine (clipPath).
    let texture = '';
    if (m.texture === 'wood') {
      for (let i = -4; i <= 4; i++) {
        const ox = i * r * 0.24 + (rand() - 0.5) * r * 0.1;
        const op = (45 + rand() * 40) / 255;
        // Ahsap lifleri: boncugun ekseni boyunca hafif egri yaylar.
        texture += `<path d="M${f(ox - 0.1 * r)} ${f(-r * 1.1)} Q${f(ox + 0.35 * r)} 0 ${f(ox - 0.1 * r)} ${f(r * 1.1)}" fill="none" stroke="${m.dark}" stroke-opacity="${f(op)}" stroke-width=".7"/>`;
      }
    } else if (m.texture === 'cloud') {
      for (let i = 0; i < 3; i++) {
        const cx = (rand() - 0.5) * r, cy = (rand() - 0.5) * r;
        texture += `<circle cx="${f(cx)}" cy="${f(cy)}" r="${f(r * 0.55)}" fill="url(#${u}cl)"/>`;
      }
    } else if (m.texture === 'veins') {
      const color = m.veinColor || m.light;
      for (let k = 0; k < 4; k++) {
        let px = -r + rand() * r * 0.4, py = -r + rand() * r * 2;
        let d = `M${f(px)} ${f(py)}`;
        for (let j = 0; j < 4; j++) {
          const nx = px + r * (0.35 + rand() * 0.3), ny = py + (rand() - 0.5) * r * 0.7;
          d += ` Q${f((px + nx) / 2)} ${f(py + (rand() - 0.5) * r * 0.5)} ${f(nx)} ${f(ny)}`;
          px = nx; py = ny;
        }
        texture += `<path d="${d}" fill="none" stroke="${color}" stroke-opacity="${f((55 + rand() * 45) / 255)}" stroke-width=".5"/>`;
      }
    } else if (m.texture === 'matrix') {
      for (let k = 0; k < 6; k++) {
        const sx = (rand() - 0.5) * r * 2, sy = (rand() - 0.5) * r * 2;
        texture += `<path d="M${f(sx)} ${f(sy)} L${f(sx + (rand() - 0.5) * r * 0.9)} ${f(sy + (rand() - 0.5) * r * 0.9)} L${f(sx + (rand() - 0.5) * r * 1.2)} ${f(sy + (rand() - 0.5) * r * 1.2)}" fill="none" stroke="#3A2A18" stroke-opacity=".43" stroke-width=".6"/>`;
      }
    }

    const lightStop = m.gloss < 0.3 ? 0.05 : 0;
    const glowColor = mixHex(m.light, m.base, 0.7);
    const hr = r * (0.55 - m.gloss * 0.22);

    // Imame: genislik W, boy H (yerel birim); ust orta nokta ipin baglandigi yer.
    const W = 58, H = 68, icx = W / 2;
    const capH = H * 0.07, bodyH = H * 0.30, bodyW = W * 0.3, neckH = H * 0.06;
    const cap = (y, w, h) => `<rect x="${f(icx - w / 2)}" y="${f(y)}" width="${f(w)}" height="${f(h)}" rx="${f(h * 0.4)}" fill="url(#${u}ml)"/>`;
    let y = 0, ima = cap(y, W * 0.2, capH);
    y += capH;
    ima += `<rect x="${f(icx - bodyW / 2)}" y="${f(y)}" width="${f(bodyW)}" height="${f(bodyH)}" rx="${f(bodyW / 2)}" fill="url(#${u}il)"/>`;
    if (m.gloss > 0.3) ima += `<rect x="${f(icx - bodyW / 2 + bodyW * 0.22)}" y="${f(y + bodyH * 0.12)}" width="${f(bodyW * 0.16)}" height="${f(bodyH * 0.68)}" rx="${f(bodyW * 0.08)}" fill="#fff" fill-opacity="${f(m.gloss * 150 / 255)}"/>`;
    y += bodyH;
    ima += cap(y, W * 0.24, neckH);
    y += neckH;
    // Puskul: ince ipler, ucu hafif dagilan.
    const tasselTop = y, tasselH = H - y - 1, trand = rng(MATERIALS.indexOf(m) * 7 + W);
    for (let i = 0; i < 26; i++) {
      const fr = i / 25 - 0.5;
      const endX = icx + fr * W * 0.55 + (trand() - 0.5) * W * 0.04;
      const endY = tasselTop + tasselH * (0.9 + trand() * 0.1);
      const color = mixHex(m.tassel, '#000000', 0.55 + trand() * 0.45);
      ima += `<path d="M${f(icx + fr * W * 0.12)} ${f(tasselTop)} Q${f(icx + fr * W * 0.2)} ${f(tasselTop + tasselH * 0.5)} ${f(endX)} ${f(endY)}" fill="none" stroke="${color}" stroke-width="1.05" stroke-linecap="round"/>`;
    }
    ima += cap(tasselTop - 1, W * 0.16, H * 0.035);

    const defs = `
      <radialGradient id="${u}sh" cx="1.2" cy="2" r="11.5" gradientUnits="userSpaceOnUse"><stop offset=".6" stop-color="#000" stop-opacity=".4"/><stop offset="1" stop-color="#000" stop-opacity="0"/></radialGradient>
      <radialGradient id="${u}bd" cx="-3.2" cy="-3.8" r="14.5" gradientUnits="userSpaceOnUse"><stop offset="${lightStop}" stop-color="${m.light}"/><stop offset=".5" stop-color="${m.base}"/><stop offset="1" stop-color="${m.dark}"/></radialGradient>
      <radialGradient id="${u}gl" cx="2.8" cy="3.4" r="7.5" gradientUnits="userSpaceOnUse"><stop offset="0" stop-color="${glowColor}" stop-opacity="${f(m.glow * 200 / 255)}"/><stop offset="1" stop-color="${glowColor}" stop-opacity="0"/></radialGradient>
      <radialGradient id="${u}ed" cx="0" cy="0" r="10" gradientUnits="userSpaceOnUse"><stop offset=".72" stop-color="${m.dark}" stop-opacity="0"/><stop offset="1" stop-color="${m.dark}" stop-opacity="${f(m.glow * 150 / 255)}"/></radialGradient>
      <radialGradient id="${u}cl"><stop offset="0" stop-color="${m.dark}" stop-opacity=".24"/><stop offset="1" stop-color="${m.dark}" stop-opacity="0"/></radialGradient>
      <radialGradient id="${u}sp" cx="-3.6" cy="-4.2" r="${f(hr)}" gradientUnits="userSpaceOnUse"><stop offset="${m.gloss > 0.6 ? 0.25 : 0}" stop-color="#fff" stop-opacity="${f((70 + m.gloss * 185) / 255)}"/><stop offset="1" stop-color="#fff" stop-opacity="0"/></radialGradient>
      <radialGradient id="${u}mr" cx="-3" cy="-4" r="13" gradientUnits="userSpaceOnUse"><stop offset="0" stop-color="${metal[0]}"/><stop offset=".5" stop-color="${metal[1]}"/><stop offset="1" stop-color="${metal[2]}"/></radialGradient>
      ${metalLinear(u + 'ml')}
      <linearGradient id="${u}il" x1="0" x2="1" y1="0" y2="0"><stop offset="0" stop-color="${m.dark}"/><stop offset=".3" stop-color="${m.light}"/><stop offset=".6" stop-color="${m.base}"/><stop offset="1" stop-color="${m.dark}"/></linearGradient>
      <clipPath id="${u}cp"><circle r="9.8"/></clipPath>
      <symbol id="${u}bead" viewBox="-12 -12 24 24" overflow="visible">
        <circle cx="1.2" cy="2" r="11.5" fill="url(#${u}sh)"/>
        <circle r="10" fill="url(#${u}bd)"/>
        ${m.glow > 0 ? `<circle r="10" fill="url(#${u}gl)"/><circle r="10" fill="url(#${u}ed)"/>` : ''}
        ${texture ? `<g clip-path="url(#${u}cp)">${texture}</g>` : ''}
        <circle cx="-3.6" cy="-4.2" r="${f(hr)}" fill="url(#${u}sp)"/>
        ${m.gloss > 0.4 ? `<path d="M${f(7.8 * Math.cos(Math.PI / 6))} ${f(7.8 * Math.sin(Math.PI / 6))} A7.8 7.8 0 0 1 0 7.8" fill="none" stroke="#fff" stroke-opacity="${f(m.gloss * 70 / 255)}" stroke-width="1" stroke-linecap="round"/>` : ''}
      </symbol>
      <symbol id="${u}spc" viewBox="-10 -10 20 20"><circle r="10" fill="url(#${u}mr)"/></symbol>
      <symbol id="${u}sep" viewBox="0 0 10 21">
        <rect x="0" y="0" width="10" height="21" rx="3.5" fill="url(#${u}ml)"/>
        ${[1, 2, 3].map((k) => `<line x1="1" x2="9" y1="${f(21 * k / 4)}" y2="${f(21 * k / 4)}" stroke="${metal[2]}" stroke-opacity=".43" stroke-width=".5"/>`).join('')}
      </symbol>
      <symbol id="${u}ima" viewBox="0 0 ${W} ${H}">${ima}</symbol>`;
    return { defs, bead: `#${u}bead`, spacer: `#${u}spc`, sep: `#${u}sep`, ima: `#${u}ima`, imaW: W, imaH: H };
  }

  /** Kucuk tespih simgesi (gorunum secicisi): halka seklinde 18 boncuk, altta imame. */
  function preview(m) {
    const d = defsFor(m);
    const s = 100, cx = 50, cy = s * 0.36, r = s * 0.27, n = 18, step = 360 / (n + 1);
    const dia = 2 * r * Math.sin((step / 2) * Math.PI / 180) * 0.95, size = dia * 1.2;
    let out = '';
    for (let i = 0; i < n; i++) {
      // Altta imamenin baglandigi yerde bir boncukluk bosluk.
      const a = (90 + step * (i + 1)) * Math.PI / 180;
      out += `<use href="${d.bead}" x="${f(cx + r * Math.cos(a) - size / 2)}" y="${f(cy + r * Math.sin(a) - size / 2)}" width="${f(size)}" height="${f(size)}"/>`;
    }
    const iw = s * 0.2, ih = s * 0.34;
    out += `<use href="${d.ima}" x="${f(cx - iw / 2)}" y="${f(cy + r - dia * 0.2)}" width="${f(iw)}" height="${f(ih)}"/>`;
    return `<svg viewBox="0 0 100 100" aria-hidden="true"><defs>${d.defs}</defs>${out}</svg>`;
  }

  /**
   * Sayma ekranindaki tespih. Asagi kaydirma parmagi takip eder: siradaki
   * boncuk parmakla birlikte bosluktan asagi iner; yeterince inince onPull
   * (sayi +1). Kisa dokunus da bir boncuk ceker.
   * Donen: { el, setPulled(k, animate), setCenter(count, target, name, nameColor), setProgress(p) }.
   */
  function create(m, onPull, opts) {
    const L = root.TasbihCore.Rosary;
    const S = 340, cx = 170, cy = 170, ringR = S * 0.30, progR = S * 0.40;
    const unitDeg = 360 / L.TOTAL;
    const beadD = 2 * ringR * Math.sin((unitDeg / 2) * Math.PI / 180) * 0.94;
    // Bosluk sagda, yataydan biraz asagida; bosluk sonu buradan baslar.
    const gapEndDeg = 18 + L.GAP_W * unitDeg / 2;
    const pullPx = (opts && opts.pullPx) || 56;
    const d = defsFor(m);
    const ns = 'http://www.w3.org/2000/svg';
    const wrap = document.createElement('div');
    wrap.className = 'rosary';
    const circ = 2 * Math.PI * progR;
    wrap.innerHTML = `<svg viewBox="0 0 ${S} ${S}" role="img" aria-label="Tespih">
      <defs>${d.defs}</defs>
      <circle cx="${cx}" cy="${cy}" r="${progR}" fill="none" stroke="#26314D" stroke-width="${f(S * 0.022)}"/>
      <circle class="arc" cx="${cx}" cy="${cy}" r="${progR}" fill="none" stroke="${m.accent}" stroke-width="${f(S * 0.022)}" stroke-linecap="round" transform="rotate(-90 ${cx} ${cy})" stroke-dasharray="0 ${f(circ)}"/>
      <circle cx="${cx}" cy="${cy}" r="${f(ringR)}" fill="none" stroke="${mixHex(METALS[m.metal][1], '#12172B', 0.5)}" stroke-width="${f(Math.max(1, S * 0.004))}"/>
      <g class="beads"></g>
      <text class="cnt" x="${cx}" fill="#F2EFE6" font-weight="700" text-anchor="middle"></text>
      <text class="tgt" x="${cx}" y="${f(cy + S * 0.09)}" fill="#AEB5CF" font-size="${f(S * 0.05)}" text-anchor="middle"></text>
      <text class="nm" x="${cx}" y="${f(cy + S * 0.165)}" font-weight="700" text-anchor="middle"></text>
    </svg>`;
    const g = wrap.querySelector('.beads'), arc = wrap.querySelector('.arc');
    const cnt = wrap.querySelector('.cnt'), tgt = wrap.querySelector('.tgt'), nm = wrap.querySelector('.nm');

    let shown = 0, phase = 0, anim = null, hasState = false;

    const pt = (deg, rad) => [cx + rad * Math.cos(deg * Math.PI / 180), cy + rad * Math.sin(deg * Math.PI / 180)];
    const useBead = (deg, scale = 1, out = 0) => {
      const [x, y] = pt(deg, ringR + out), size = beadD * 1.2 * scale;
      return `<use href="${d.bead}" x="${f(x - size / 2)}" y="${f(y - size / 2)}" width="${f(size)}" height="${f(size)}"/>`;
    };

    function draw() {
      const items = L.positions(shown, phase);
      const moving = L.movingBead(shown);
      const lift = Math.sin(Math.PI * phase);
      let out = '';
      // Ara halkalar: yan yana duran iki boncugun arasinda.
      const sd = beadD * 0.28;
      for (let i = 0; i < items.length - 1; i++) {
        const [a, ua] = items[i], [b, ub] = items[i + 1];
        if (!a.isBead || !b.isBead || a.bead === moving || b.bead === moving || Math.abs(ua - ub) > 1.5) continue;
        const [x, y] = pt(gapEndDeg + ((ua + ub) / 2) * unitDeg, ringR);
        out += `<use href="${d.spacer}" x="${f(x - sd / 2)}" y="${f(y - sd / 2)}" width="${f(sd)}" height="${f(sd)}"/>`;
      }
      let movingDeg = 0, imameDeg = null;
      for (const [e, u] of items) {
        const deg = gapEndDeg + u * unitDeg;
        if (e.isImame) { imameDeg = deg; continue; }
        if (e.isBead) {
          if (e.bead === moving && phase > 0) { movingDeg = deg; continue; }
          out += useBead(deg);
        } else {
          // Nisane ipe dik durur: uzun ekseni yaricap yonune cevrilir.
          const [x, y] = pt(deg, ringR), w = beadD * 0.5, h = beadD * 1.05;
          out += `<use href="${d.sep}" x="${f(x - w / 2)}" y="${f(y - h / 2)}" width="${f(w)}" height="${f(h)}" transform="rotate(${f(deg - 90)} ${f(x)} ${f(y)})"/>`;
        }
      }
      // Imame ve puskul: ipin parcasi, halkayla doner; puskul disa bakar.
      if (imameDeg != null) {
        const [x, y] = pt(imameDeg, ringR), iw = S * 0.17, ih = S * 0.20;
        out += `<use href="${d.ima}" x="${f(x - iw / 2)}" y="${f(y - beadD * 0.15)}" width="${f(iw)}" height="${f(ih)}" transform="rotate(${f(imameDeg - 90)} ${f(x)} ${f(y)})"/>`;
      }
      if (phase > 0) {
        // Cekilen boncuk: one ve disa dogru kalkar, etrafinda isik.
        const scale = 1 + 0.28 * lift, o = ringR * 0.07 * lift;
        out += useBead(movingDeg, scale, o);
        const [x, y] = pt(movingDeg, ringR + o);
        out += `<circle cx="${f(x)}" cy="${f(y)}" r="${f(beadD * 0.62 * scale)}" fill="none" stroke="${m.accent}" stroke-opacity="${f(0.86 * lift)}" stroke-width="${f(S * 0.008)}"/>`;
      } else {
        // Bekarken siradaki boncuk hafifce isaretli.
        const next = items.filter((p) => p[0].isBead).pop();
        const [x, y] = pt(gapEndDeg + next[1] * unitDeg, ringR);
        out += `<circle cx="${f(x)}" cy="${f(y)}" r="${f(beadD * 0.62)}" fill="none" stroke="${m.accent}" stroke-opacity=".43" stroke-width="${f(S * 0.005)}"/>`;
      }
      g.innerHTML = out;
    }

    // ---------------------------------------------------------- canlandirma

    let pendingEnd = null;
    function animate(from, to, onEnd) {
      const dur = Math.max(60, 230 * Math.abs(to - from));
      const start = performance.now();
      pendingEnd = onEnd;
      const step = (now) => {
        const x = Math.min(1, (now - start) / dur);
        const eased = 1 - Math.pow(1 - x, 2.8); // DecelerateInterpolator(1.4)
        phase = from + (to - from) * eased;
        draw();
        if (x < 1) anim = requestAnimationFrame(step);
        else { anim = null; pendingEnd = null; if (onEnd) onEnd(); draw(); }
      };
      anim = requestAnimationFrame(step);
    }
    /** Yarida kalan canlandirmayi hemen bitirir (hizli arka arkaya cekis). */
    function finish() {
      if (anim != null) {
        cancelAnimationFrame(anim);
        anim = null;
        const end = pendingEnd;
        pendingEnd = null;
        if (end) end();
      }
    }

    /** k cekilmis boncuga getirir; bir ileri ceker gibi, bir geri geri alir gibi canlanir. */
    function setPulled(k, animated) {
      const target = ((k % L.BEADS) + L.BEADS) % L.BEADS;
      const reduce = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
      if (!hasState || !animated || reduce) { finish(); shown = target; phase = 0; hasState = true; draw(); return; }
      finish();
      const next = (shown + 1) % L.BEADS, prev = (shown + L.BEADS - 1) % L.BEADS;
      if (target === shown) { if (phase !== 0) animate(phase, 0, null); }
      else if (target === next) animate(phase, 1, () => { shown = next; phase = 0; });
      else if (target === prev) { shown = prev; phase = 1; animate(1, 0, null); }
      else { shown = target; phase = 0; draw(); }
    }

    // ---------------------------------------------------------------- dokunus

    let downX = 0, downY = 0, dragging = false, moved = false, pulledInGesture = false, active = false;
    const slop = 8;
    wrap.addEventListener('pointerdown', (ev) => {
      finish();
      active = true; downX = ev.clientX; downY = ev.clientY;
      dragging = false; moved = false; pulledInGesture = false;
      try { wrap.setPointerCapture(ev.pointerId); } catch (e) {}
    });
    wrap.addEventListener('pointermove', (ev) => {
      if (!active) return;
      const dy = ev.clientY - downY, dx = ev.clientX - downX;
      if (!dragging && !moved && (Math.abs(dy) > slop || Math.abs(dx) > slop)) {
        // Yalnizca asagi dogru hareket ceker; yana ya da yukari kaydirma saymaz.
        if (dy > 0 && Math.abs(dy) > Math.abs(dx)) dragging = true; else moved = true;
      }
      if (dragging && !pulledInGesture) {
        finish();
        const t = Math.min(1, Math.max(0, dy / pullPx));
        if (t >= 1) {
          // Tam cekildi: say. Bir cekiste en fazla bir sayi.
          phase = 0.999; pulledInGesture = true; onPull();
        } else { phase = t; draw(); }
      }
    });
    wrap.addEventListener('pointerup', () => {
      if (!active) return;
      active = false;
      if (dragging && !pulledInGesture) {
        if (phase >= 0.45) { pulledInGesture = true; onPull(); }
        else animate(phase, 0, null);
      } else if (!pulledInGesture && !moved) onPull();
      dragging = false;
    });
    wrap.addEventListener('pointercancel', () => {
      if (active && dragging && !pulledInGesture && phase > 0) animate(phase, 0, null);
      active = false; dragging = false;
    });

    function fit(text, size, maxW, el) {
      el.setAttribute('font-size', f(size));
      el.textContent = text;
      try {
        const w = el.getComputedTextLength();
        if (w > maxW) el.setAttribute('font-size', f(size * maxW / w));
      } catch (e) {}
    }

    function setCenter(count, target, name, nameColor) {
      const size = S * 0.17;
      fit(count, size, ringR * 1.5, cnt);
      cnt.setAttribute('y', f(cy + parseFloat(cnt.getAttribute('font-size')) * 0.2));
      tgt.textContent = target;
      nm.setAttribute('fill', nameColor);
      fit(name, S * 0.048, ringR * 1.45, nm);
    }

    function setProgress(p) {
      arc.setAttribute('stroke-dasharray', `${f(circ * Math.min(1, Math.max(0, p)))} ${f(circ)}`);
    }

    return { el: wrap, setPulled, setCenter, setProgress };
  }

  const api = { METALS, MATERIALS, preview, create };
  root.Rosary = api;
})(typeof window !== 'undefined' ? window : globalThis);
