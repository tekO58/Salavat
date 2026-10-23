// Akilli yapistir: internetten kopyalanan metni ad, Arapca, okunus, anlam ve
// kaynak alanlarina ayirir. Android'deki PasteParser.kt ile ayni kurallar:
//  1. "Okunuşu:", "Anlamı:", "Meali:", "Kaynak:" gibi etiketler sonraki satirlari o alana yollar.
//  2. Harflerinin cogu Arapca olan satir her zaman Arapca alanina gider.
//  3. Baglanti her zaman kaynaktir; cumle icindeki "site.com" yalnizca etiketsiz satirda.
//  4. Etiketsiz Latin satir: sapkali harfler ve tireler okunusa, Turkce bag kelimeleri anlama.
//  5. Ilk satir kisaysa ve "salât", "duası", "zikir" gibi bir kelime iceriyorsa kaydin adidir.
(function (root) {
  const LABELS = [
    ['türkçe okunuşu', 'reading'], ['türkçe okunuş', 'reading'], ['latin harfleriyle', 'reading'],
    ['okunuşu', 'reading'], ['okunuş', 'reading'], ['okunusu', 'reading'], ['okunus', 'reading'],
    ['türkçe anlamı', 'meaning'], ['türkçe meali', 'meaning'], ['türkçe tercümesi', 'meaning'],
    ['anlamı', 'meaning'], ['anlami', 'meaning'], ['anlam', 'meaning'], ['meali', 'meaning'],
    ['meal', 'meaning'], ['manası', 'meaning'], ['mânâsı', 'meaning'], ['tercümesi', 'meaning'],
    ['arapça yazılışı', 'arabic'], ['arapça metni', 'arabic'], ['arapçası', 'arabic'], ['arapça', 'arabic'],
    ['kaynakça', 'source'], ['kaynak', 'source'], ['nispet', 'source'],
    ['adı', 'name'], ['başlık', 'name']
  ];
  const TITLE_WORDS = ['salât', 'salat', 'salavat', 'salevat', 'salâvat', 'duası', 'dua', 'zikri', 'zikir', 'tesbih',
    'virdi', 'esmâ', 'esma', 'tevhid', 'tevhîd', 'istiğfar', 'istiğfâr', 'suresi', 'sûresi', 'âyeti', 'ayeti'];
  const MEANING_WORDS = new Set(['bir', 'için', 'olan', 'eyle', 'ki', 'bizi', 'beni', 'bize', 'bana', 'ey', "allah'ım",
    'allahım', 'rabbim', 'rabbimiz', 'efendimiz', 'senin', 'sana', 'senden', 'bütün', 'olarak', 'gibi', 'şüphesiz',
    'öyle', 'onun', 'ona', 'onu', 'kendisi', 'her', 've', 'çok', 'yüce', 'olsun', 'et', 'ver', 'kıl', 'bu', 'şey']);
  const DOMAIN = /\b[\w-]+\.(com|org|net|tr|info|edu)\b/i;

  const lower = (s) => s.toLocaleLowerCase('tr');

  function isArabicChar(c) {
    const n = c.charCodeAt(0);
    return (n >= 0x0600 && n <= 0x06FF) || (n >= 0x0750 && n <= 0x077F) || (n >= 0x08A0 && n <= 0x08FF) ||
      (n >= 0xFB50 && n <= 0xFDFF) || (n >= 0xFE70 && n <= 0xFEFF);
  }

  function arabicRatio(line) {
    const letters = [...line].filter((c) => /\p{L}/u.test(c) || isArabicChar(c));
    if (!letters.length) return 0;
    return letters.filter(isArabicChar).length / letters.length;
  }

  function cleanLine(line) {
    return line.trim().replace(/^[•\-–—*>"“”«» ]+/, '').replace(/^\d+[.)]\s+/, '').trim();
  }

  function splitLabel(line) {
    const l = lower(line);
    for (const [label, field] of LABELS) {
      if (!l.startsWith(label)) continue;
      const rest = line.substring(label.length).trimStart();
      if (!rest) return [field, ''];
      if (/^[:\-–]/.test(rest)) return [field, rest.substring(1).trim()];
    }
    return null;
  }

  const isLink = (line) => { const l = lower(line); return l.includes('http://') || l.includes('https://') || l.startsWith('www.'); };
  const looksLikeSource = (line) => { const l = lower(line); return l.includes('http') || l.includes('www.') || DOMAIN.test(l); };

  function words(line) {
    return lower(line).split(/\s+/).map((w) => w.replace(/^[,.;:!?()"“”]+|[,.;:!?()"“”]+$/g, '')).filter(Boolean);
  }

  function looksLikeTitle(line) {
    if (line.length > 45) return false;
    if (/[.,;!]$/.test(line.trimEnd())) return false;
    const w = words(line);
    if (w.length > 6) return false;
    return w.some((x) => TITLE_WORDS.some((t) => x.startsWith(t)));
  }

  function isReading(line) {
    const w = words(line);
    if (!w.length) return false;
    const readingMarks = [...line].filter((c) => 'âîûÂÎÛ'.includes(c)).length + [...line].filter((c) => c === '-').length;
    const meaningMarks = w.filter((x) => MEANING_WORDS.has(x)).length;
    return readingMarks > meaningMarks;
  }

  function parse(text) {
    let name = '';
    const arabic = [], reading = [], meaning = [], source = [];
    let mode = null, first = true;
    const lines = text.split(/\r?\n/).map(cleanLine).filter(Boolean);
    lines.forEach((raw, index) => {
      let line = raw;
      const label = splitLabel(line);
      if (label) {
        mode = label[0];
        line = label[1];
        if (!line) { first = false; return; }
      }
      if (arabicRatio(line) > 0.5) arabic.push(line);
      else if (isLink(line) || (looksLikeSource(line) && mode !== 'meaning' && mode !== 'reading')) source.push(line);
      else if (mode === 'name') { name = name ? name + ' ' + line : line; mode = null; }
      else if (mode === 'reading') reading.push(line);
      else if (mode === 'meaning') meaning.push(line);
      else if (mode === 'source') source.push(line);
      else if (first && !name && index < lines.length - 1 && looksLikeTitle(line)) name = line;
      else if (isReading(line)) reading.push(line);
      else meaning.push(line);
      first = false;
    });
    return {
      name: name.trim().replace(/:$/, ''),
      arabic: arabic.join(' '),
      reading: reading.join(' '),
      meaning: meaning.join(' '),
      source: source.join(' · ')
    };
  }

  const isEmpty = (r) => !r.name && !r.arabic && !r.reading && !r.meaning && !r.source;

  function autoName(r) {
    const base = r.reading || r.meaning || '';
    return base.split(/\s+/).filter(Boolean).slice(0, 4).join(' ').replace(/[,.;:!]+$/, '').substring(0, 40);
  }

  const api = { parse, isEmpty, autoName, isArabicChar, arabicRatio };
  if (typeof module !== 'undefined') module.exports = api;
  root.PasteParser = api;
})(typeof window !== 'undefined' ? window : globalThis);
