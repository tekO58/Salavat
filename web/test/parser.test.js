// node web/test/parser.test.js — Android'deki PasteParserTest ile ayni ornekler.
const assert = require('assert');
const P = require('../parser.js');

const cases = {
  'etiketsiz blok'() {
    const r = P.parse([
      'اللَّهُمَّ صَلِّ عَلَى سَيِّدِنَا مُحَمَّدٍ صَلَاةً تُنْجِينَا',
      'Allâhümme salli alâ seyyidinâ Muhammedin salâten tüncînâ bihâ',
      "Allah'ım! Efendimiz Muhammed'e öyle bir salât eyle ki bizi kurtarasın.",
      'Kaynak: islamveihsan.com'
    ].join('\n'));
    assert(r.arabic.startsWith('اللَّهُمَّ'));
    assert(r.reading.startsWith('Allâhümme salli'));
    assert(r.meaning.startsWith("Allah'ım! Efendimiz"));
    assert.strictEqual(r.source, 'islamveihsan.com');
    assert.strictEqual(r.name, '');
  },
  'etiketli metin ve baslik'() {
    const r = P.parse([
      'Salat-ı Münciye Duası', '', 'Arapça:', 'اللَّهُمَّ صَلِّ عَلَى سَيِّدِنَا مُحَمَّدٍ',
      'Okunuşu: Allahümme salli ala seyyidina Muhammed', 'Anlamı:',
      "Allah'ım, Efendimiz Muhammed'e salat eyle.", 'https://example.com/munciye'
    ].join('\n'));
    assert.strictEqual(r.name, 'Salat-ı Münciye Duası');
    assert.strictEqual(r.arabic, 'اللَّهُمَّ صَلِّ عَلَى سَيِّدِنَا مُحَمَّدٍ');
    assert.strictEqual(r.reading, 'Allahümme salli ala seyyidina Muhammed');
    assert.strictEqual(r.meaning, "Allah'ım, Efendimiz Muhammed'e salat eyle.");
    assert.strictEqual(r.source, 'https://example.com/munciye');
  },
  'cok satirli arapca, madde isaretleri'() {
    const r = P.parse('• سُبْحَانَ اللّٰهِ\n• وَبِحَمْدِهِ\n- Sübhânallâhi ve bi-hamdihî');
    assert.strictEqual(r.arabic, 'سُبْحَانَ اللّٰهِ وَبِحَمْدِهِ');
    assert.strictEqual(r.reading, 'Sübhânallâhi ve bi-hamdihî');
  },
  'yalnizca okunus'() {
    const r = P.parse('Lâ ilâhe illallâhü vahdehû lâ şerîke leh');
    assert.strictEqual(r.arabic, '');
    assert(r.reading.startsWith('Lâ ilâhe'));
    assert.strictEqual(P.autoName(r), 'Lâ ilâhe illallâhü vahdehû');
  },
  'bos metin'() { assert(P.isEmpty(P.parse('  \n \n'))); }
};

let failed = 0;
for (const [name, fn] of Object.entries(cases)) {
  try { fn(); console.log('✓', name); } catch (e) { failed++; console.log('✗', name, '\n ', e.message); }
}
console.log(failed ? `${failed} başarısız` : 'hepsi geçti');
process.exit(failed ? 1 : 0);
