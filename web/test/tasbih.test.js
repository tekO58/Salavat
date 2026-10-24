// node web/test/tasbih.test.js — workplanner-app'teki TasbihTimeTest ile ayni ornekler.
const assert = require('assert');
const T = require('../tasbih.js');
const idle = 30000;
const tasbih = (target, onTarget = 'stop') => T.normalizeTasbih({ target, onTarget, every: 33 });

const cases = {
  'sure dokunuslar arasindan birikir'() {
    const t = tasbih(10);
    T.tap(t, 1000, idle);
    assert.strictEqual(t.roundMs, 0);
    T.tap(t, 3000, idle);
    T.tap(t, 6000, idle);
    assert.strictEqual(t.roundMs, 5000);
  },
  'uzun ara duraklama sayilir, devam edince kaldigi yerden'() {
    const t = tasbih(10);
    T.tap(t, 1, idle);
    T.tap(t, 4001, idle);
    T.tap(t, 600000, idle);
    T.tap(t, 602000, idle);
    assert.strictEqual(t.roundMs, 6000);
  },
  'tur bitince son, en hizli ve ortalama kaydedilir'() {
    const t = tasbih(3, 'round');
    let now = 1;
    for (let i = 0; i < 3; i++) { T.tap(t, now, idle); now += 5000; }
    assert.strictEqual(t.lastRoundMs, 10000);
    assert.strictEqual(t.roundMs, 0);
    now = t.lastTapAt + 2000;
    for (let i = 0; i < 3; i++) { T.tap(t, now, idle); now += 2000; }
    assert.strictEqual(t.lastRoundMs, 6000);
    assert.strictEqual(t.bestRoundMs, 6000);
    assert.strictEqual(t.timedRounds, 2);
    assert.strictEqual(T.averageRoundMs(t), 8000);
  },
  'canli sure ve bicim'() {
    const t = Object.assign(tasbih(10), { roundMs: 60000, lastTapAt: 100000 });
    assert.strictEqual(T.liveRoundMs(t, 105000, idle), 65000);
    assert.strictEqual(T.liveRoundMs(t, 200000, idle), 60000);
    assert.strictEqual(T.format(65000), '1:05');
    assert.strictEqual(T.format(3753000), '1:02:33');
    assert.strictEqual(T.summary(tasbih(10)), '');
  },
  'hiz ve tahmin 10 cekisten sonra'() {
    const t = tasbih(100);
    let now = 1;
    for (let i = 0; i < 10; i++) { T.tap(t, now, idle); now += 2000; }
    assert.strictEqual(T.etaMs(t.intervals, 90), null); // 9 aralik: henuz yok
    T.tap(t, now, idle);
    assert.strictEqual(T.perMinute(t.intervals), 30);
    assert.strictEqual(T.etaMs(t.intervals, 89), 178000);
    assert.strictEqual(T.approx(178000), '≈ 3 dk');
    assert.strictEqual(T.approx(40000), '≈ 40 sn');
    assert.strictEqual(T.approx(210000), '≈ 3,5 dk');
  },
  'dur modunda hedefte kalir, geri al tamamlanmayi geri alir'() {
    const t = tasbih(2);
    T.tap(t, 1, idle);
    assert.strictEqual(T.tap(t, 2, idle), 'completed');
    assert.strictEqual(T.tap(t, 3, idle), 'blocked');
    assert(T.undo(t));
    assert.strictEqual(t.count, 1);
    assert.strictEqual(t.rounds, 0);
  }
};

// Dizilis: workplanner'daki TasbihRosaryLayoutTest ile ayni kurallar.
const L = T.Rosary;
Object.assign(cases, {
  'otuz uc boncuk, iki nisane, bir imame'() {
    assert.strictEqual(L.STRING.filter((e) => e.isBead).length, 33);
    assert.strictEqual(L.STRING.filter((e) => e.isImame).length, 1);
    assert.strictEqual(L.STRING.filter((e) => !e.isBead && !e.isImame).length, 2);
  },
  'cekis sonu bir sonraki durumun basiyla ayni (ziplama yok)'() {
    for (let k = 0; k < 33; k++) {
      const end = new Map(L.positions(k, 1).map(([e, u]) => [e.bead, u]));
      const next = new Map(L.positions(k + 1, 0).map(([e, u]) => [e.bead, u]));
      for (const [bead, u] of end) {
        const d = Math.abs(u - next.get(bead));
        assert(Math.min(d, L.TOTAL - d) < 1e-3, `k=${k} boncuk=${bead}`);
      }
    }
  },
  'siradaki boncuk sirayla cekilir ve 33te basa doner'() {
    for (let k = 0; k < 33; k++) assert.strictEqual(L.movingBead(k), (k % 33) + 1);
    assert.deepStrictEqual(L.order(0), L.order(33));
  },
  'durgun halde bosluk bos, boncuklar ust uste binmez'() {
    for (let k = 0; k < 33; k++) {
      const ps = L.positions(k, 0).map((p) => p[1]).sort((a, b) => a - b);
      assert(ps.every((u) => u < L.WIDTH));
      for (let i = 1; i < ps.length; i++) assert(ps[i] - ps[i - 1] >= L.SEP_W - 1e-3);
    }
  },
  'nisane ve imame boncukla birlikte gecer'() {
    assert.strictEqual(L.chunkSize(L.order(0)), 2);
    assert(L.order(0).slice(-2).some((e) => e.isImame));
    assert.strictEqual(L.chunkSize(L.order(11)), 2);
    assert.strictEqual(L.chunkSize(L.order(5)), 1);
  },
  'cekilen boncuk boslugu atlar'() {
    const before = L.positions(4, 0).find((p) => p[0].bead === 5)[1];
    const after = L.positions(4, 1).find((p) => p[0].bead === 5)[1];
    assert(before > L.WIDTH - 1);
    assert(after < 1);
  }
});

let failed = 0;
for (const [name, fn] of Object.entries(cases)) {
  try { fn(); console.log('✓', name); } catch (e) { failed++; console.log('✗', name, '\n ', e.message); }
}
console.log(failed ? `${failed} başarısız` : 'hepsi geçti');
process.exit(failed ? 1 : 0);
