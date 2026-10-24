// Tespih sayma kurallari ve sure hesabi. workplanner-app'teki TasbihModel.kt
// (TasbihLogic, TasbihTime) ile ayni kurallar; DOM'a bagli degil, node ile test ediliyor.
//
// Sure dokunuslar arasindan hesaplanir: iki dokunus arasi bekleme siniri
// (idle) kadar ya da daha kisaysa sureye eklenir, daha uzunsa o ara
// "duraklama" sayilir. Boylece sayma birakilinca sure kendiliginden durur,
// ilk yeni dokunusta kaldigi yerden devam eder.
(function (root) {
  const DEFAULT_IDLE_MS = 30000;
  const IDLE_OPTIONS = [15000, 30000, 60000, 120000];
  const SPEED_WINDOW = 20; // hiz icin tutulan son aralik sayisi
  const SPEED_MIN = 10; // tahminin gosterilmesi icin gereken en az aralik

  const idleLabel = (ms) => (ms < 60000 ? `${ms / 1000} sn` : `${ms / 60000} dk`);

  /** Son dokunustan bu yana eklenecek sure; duraklamadaysa 0. */
  function gap(lastTapAt, now, idleMs) {
    if (!lastTapAt || now <= lastTapAt) return 0;
    const g = now - lastTapAt;
    return g <= idleMs ? g : 0;
  }

  const liveRoundMs = (t, now, idleMs) => (t.roundMs || 0) + gap(t.lastTapAt, now, idleMs);

  /** "1:48", "12:05", "1:02:33" */
  function format(ms) {
    const total = Math.floor((Math.max(0, ms) + 500) / 1000);
    const h = Math.floor(total / 3600), m = Math.floor((total % 3600) / 60), s = total % 60;
    const p = (n) => String(n).padStart(2, '0');
    return h > 0 ? `${h}:${p(m)}:${p(s)}` : `${m}:${p(s)}`;
  }

  /** Adet basina sure: "~1,7 sn". */
  function perBead(roundMs, target) {
    if (roundMs <= 0 || target <= 0) return '';
    return '~' + (roundMs / 1000 / target).toFixed(1).replace('.', ',') + ' sn';
  }

  const addInterval = (list, g) => (g <= 0 ? list : list.concat(g).slice(-SPEED_WINDOW));

  /** Tipik cekis araligi: en hizli ve en yavas %10 atilmis ortalama; az veride null. */
  function typicalIntervalMs(list) {
    if (!list || list.length < SPEED_MIN) return null;
    const sorted = list.slice().sort((a, b) => a - b);
    const cut = Math.floor(sorted.length / 10);
    const kept = sorted.slice(cut, sorted.length - cut);
    return Math.max(1, Math.floor(kept.reduce((a, b) => a + b, 0) / kept.length));
  }

  function perMinute(list) {
    const t = typicalIntervalMs(list);
    return t == null ? null : Math.max(1, Math.floor(60000 / t));
  }

  function etaMs(list, remaining) {
    if (remaining <= 0) return 0;
    const t = typicalIntervalMs(list);
    return t == null ? null : t * remaining;
  }

  /** "≈ 40 sn", "≈ 3,5 dk", "≈ 1 sa 12 dk" */
  function approx(ms) {
    const sec = Math.floor((Math.max(0, ms) + 500) / 1000);
    if (sec < 60) return '≈ ' + Math.max(5, Math.floor((sec + 4) / 5) * 5) + ' sn';
    if (sec < 600) {
      const halves = Math.floor((sec + 15) / 30), whole = Math.floor(halves / 2);
      return halves % 2 === 0 ? `≈ ${whole} dk` : `≈ ${whole},5 dk`;
    }
    const min = Math.floor((sec + 30) / 60);
    return min < 60 ? `≈ ${min} dk` : `≈ ${Math.floor(min / 60)} sa ${min % 60} dk`;
  }

  const averageRoundMs = (t) => (t.timedRounds > 0 ? Math.floor(t.timedMs / t.timedRounds) : 0);

  /** "Son tur 2:45 · ort. 2:52 · en hızlı 2:31"; tur yoksa bos. */
  function summary(t) {
    if (!(t.timedRounds > 0)) return '';
    const parts = ['Son tur ' + format(t.lastRoundMs)];
    if (t.timedRounds > 1) {
      parts.push('ort. ' + format(averageRoundMs(t)));
      parts.push('en hızlı ' + format(t.bestRoundMs));
    }
    return parts.join(' · ');
  }

  const isDone = (t) => t.onTarget === 'stop' && t.count >= t.target;

  /** Bos alanlari tamamlar (eski kayitlarda sure alanlari yok). */
  function normalizeTasbih(t) {
    return Object.assign({ count: 0, rounds: 0, total: 0, roundMs: 0, lastTapAt: 0, lastRoundMs: 0, bestRoundMs: 0, timedRounds: 0, timedMs: 0, intervals: [] }, t);
  }

  /**
   * Tek dokunus; t'yi yerinde degistirir.
   * Doner: 'blocked' | 'completed' | 'milestone' | 'counted'.
   */
  function tap(t, now, idleMs = DEFAULT_IDLE_MS) {
    if (isDone(t)) return 'blocked';
    const next = t.count + 1;
    const g = gap(t.lastTapAt, now, idleMs);
    const elapsed = (t.roundMs || 0) + g;
    t.total++;
    t.lastTapAt = now;
    t.roundMs = elapsed;
    t.intervals = addInterval(t.intervals || [], g);
    if (next >= t.target) {
      // Tur bitti: suresi kaydedilir, yeni tur sifirdan olculur.
      if (elapsed > 0) {
        t.lastRoundMs = elapsed;
        t.bestRoundMs = t.bestRoundMs > 0 ? Math.min(t.bestRoundMs, elapsed) : elapsed;
        t.timedRounds = (t.timedRounds || 0) + 1;
        t.timedMs = (t.timedMs || 0) + elapsed;
      }
      t.roundMs = 0;
      t.rounds++;
      t.count = t.onTarget === 'round' ? 0 : t.target;
      return 'completed';
    }
    t.count = next;
    return t.every > 0 && next % t.every === 0 ? 'milestone' : 'counted';
  }

  /** Son dokunusu geri alir; geri alinacak sey yoksa false. */
  function undo(t) {
    if (t.total <= 0) return false;
    if (t.onTarget === 'stop' && t.count >= t.target) { t.count = t.target - 1; t.rounds = Math.max(0, t.rounds - 1); }
    else if (t.count > 0) t.count--;
    else if (t.rounds > 0 && t.onTarget === 'round') { t.count = t.target - 1; t.rounds--; }
    else return false;
    t.total--;
    return true;
  }

  /** Turdaki sayi, tur ve bu turun suresi sifirlanir; toplam ve sure istatistigi kalir. */
  function resetCounter(t) { Object.assign(t, { count: 0, rounds: 0, roundMs: 0, lastTapAt: 0 }); }

  /** Toplam ve sure istatistigi de sifirlanir. */
  function resetAll(t) {
    Object.assign(t, { count: 0, rounds: 0, total: 0, roundMs: 0, lastTapAt: 0, lastRoundMs: 0, bestRoundMs: 0, timedRounds: 0, timedMs: 0, intervals: [] });
  }

  const api = {
    DEFAULT_IDLE_MS, IDLE_OPTIONS, SPEED_MIN, idleLabel, gap, liveRoundMs, format, perBead, typicalIntervalMs,
    perMinute, etaMs, approx, averageRoundMs, summary, isDone, normalizeTasbih, tap, undo, resetCounter, resetAll
  };
  if (typeof module !== 'undefined') module.exports = api;
  root.TasbihCore = api;
})(typeof window !== 'undefined' ? window : globalThis);
