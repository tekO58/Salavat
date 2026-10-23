css='''
*{box-sizing:border-box;margin:0;padding:0}
body{background:#0b0f1d;font-family:Roboto,"Noto Sans","DejaVu Sans",sans-serif;padding:24px;color:#ddd}
h1{color:#E8B34A;font-size:24px;margin-bottom:4px}
.sub{color:#AEB5CF;font-size:14px;margin-bottom:18px}
.wrap{display:flex;gap:24px;align-items:flex-start}
.cap{color:#AEB5CF;font-size:13px;text-align:center;margin-top:8px;width:390px}
.phone{width:390px;height:844px;background:#12172B;border-radius:34px;overflow:hidden;position:relative;border:6px solid #000;color:#F2EFE6;display:flex;flex-direction:column}
.status{height:26px;font-size:12px;color:#AEB5CF;padding:6px 22px;display:flex;justify-content:space-between;flex:none}
.card{background:#151D32;border:1px solid #26314D;border-radius:16px}
.muted{color:#8790AD}.sec{color:#AEB5CF}.acc{color:#E8B34A}
.lbl{font-size:11.5px;font-weight:700;color:#8790AD;letter-spacing:.3px}
.ar{direction:rtl;font-family:"Noto Naskh Arabic","DejaVu Sans",serif}
.legend{width:330px;font-size:14px;line-height:1.5}
.legend h3{color:#E8B34A;font-size:15px;margin:12px 0 4px}
.legend li{margin:0 0 4px 18px}
.field{background:#0E1526;border:1px solid #26314D;border-radius:12px;padding:10px 12px;font-size:14px;margin-top:4px}
.chip{display:inline-block;padding:7px 13px;border-radius:18px;font-size:13.5px;border:1px solid #26314D;background:#151D32;margin:0 6px 6px 0}
.chip.on{background:#E8B34A;color:#111522;border-color:#E8B34A;font-weight:700}
.btn{background:#E8B34A;color:#111522;border-radius:16px;text-align:center;padding:13px;font-weight:700;font-size:16px}
'''
C={'s':'#E8B34A','z':'#72C98A','d':'#7FB2F0','e':'#E39BD0'}
def phone(inner,cap): return f'<div><div class="phone"><div class="status"><span>21:40</span><span>▮▮▮ 87%</span></div>{inner}</div><div class="cap">{cap}</div></div>'
def page(fn,title,sub,phones,legend):
    open(fn,'w').write(f'<html><head><meta charset="utf-8"><style>{css}</style></head><body><h1>{title}</h1><div class="sub">{sub}</div><div class="wrap">{"".join(phones)}<div class="legend">{legend}</div></div></body></html>')

items=[('s','Salât-ı Münciye','اللَّهُمَّ صَلِّ عَلَى سَيِّدِنَا مُحَمَّدٍ','23 / 100'),
 ('z','Sübhânallâhi ve bi-hamdihî','سُبْحَانَ اللّٰهِ وَبِحَمْدِهِ','0 / 100'),
 ('d','Rabbenâ âtinâ','رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً','Okuma'),
 ('s','Salât-ı Tefrîciyye','اللَّهُمَّ صَلِّ صَلَاةً كَامِلَةً','1.250 / 4.444'),
 ('z','Kelime-i Tevhîd','لَا إِلٰهَ إِلَّا اللّٰهُ','312 / 1.000'),
 ('d','Seyyidü\'l-istiğfâr','اللَّهُمَّ أَنْتَ رَبِّي','Okuma'),
 ('z','Estağfirullah','أَسْتَغْفِرُ اللّٰهَ','—'),
]

# ===== A: Alt sekmeler
rowsA=''.join(f'''<div class="card" style="display:flex;align-items:center;padding:12px 8px 12px 14px;margin-bottom:9px">
<div style="flex:1;min-width:0"><div style="font-size:16px;font-weight:700">{n}</div><div class="sec" style="font-size:12.5px;margin-top:2px">{c}</div></div>
<div class="acc" style="width:40px;text-align:center;font-size:18px">▶</div></div>''' for k,n,a,c in items if k=='s')
rowsA+=''.join(f'''<div class="card" style="display:flex;align-items:center;padding:12px 8px 12px 14px;margin-bottom:9px">
<div style="flex:1"><div style="font-size:16px;font-weight:700">{n}</div><div class="sec" style="font-size:12.5px;margin-top:2px">Hedef 100</div></div>
<div class="acc" style="width:40px;text-align:center;font-size:18px">▶</div></div>''' for n in ['Salât-ı Fâtih','Salât-ı Kâmile','Salât-ı Ümmiyye','Salât-ı Azîmiyye','Salât-ı İbrâhîmiyye'])
tab=lambda i,l,on: f'<div style="flex:1;text-align:center;font-size:11.5px;{"color:#E8B34A;font-weight:700" if on else "color:#8790AD"}"><div style="font-size:20px">{i}</div>{l}</div>'
A1=f'''<div style="padding:10px 16px 0;display:flex;align-items:center"><div style="flex:1;font-size:24px;font-weight:700">Salavat</div><div style="font-size:20px;margin-right:6px">🔍</div></div>
<div style="padding:4px 16px 8px" class="sec">8 kayıt · bugün 23</div>
<div style="flex:1;overflow:hidden;padding:0 16px">{rowsA}</div>
<div style="position:absolute;right:20px;bottom:86px;width:58px;height:58px;border-radius:29px;background:#E8B34A;color:#111522;font-size:32px;display:flex;align-items:center;justify-content:center;box-shadow:0 6px 16px #0008">+</div>
<div style="display:flex;padding:8px 6px 14px;background:#0E1526;border-top:1px solid #26314D;flex:none">{tab("📿","Salavat",1)}{tab("✦","Zikir",0)}{tab("🤲","Dua",0)}{tab("★","Favori",0)}{tab("⋯","Diğer",0)}</div>'''
A2=f'''<div style="padding:10px 16px;display:flex;align-items:center"><div style="font-size:28px;width:34px">✕</div><div style="flex:1;font-size:19px;font-weight:700">Yeni ekle</div><div class="acc" style="font-weight:700">Kaydet</div></div>
<div style="flex:1;overflow:hidden;padding:0 16px">
<div class="lbl">BÖLÜM</div><div style="margin:6px 0 10px"><span class="chip on">Salavat</span><span class="chip">Zikir</span><span class="chip">Dua</span><span class="chip">+ Yeni bölüm</span></div>
<div class="lbl">AD</div><div class="field">Salât-ı Nûrâniyye</div>
<div class="lbl" style="margin-top:12px">ARAPÇA <span style="font-weight:400">(isteğe bağlı)</span></div><div class="field ar" style="height:78px;color:#8790AD;text-align:right">Arapça metni buraya yapıştır…</div>
<div class="lbl" style="margin-top:12px">OKUNUŞ</div><div class="field" style="height:64px">Allâhümme salli alâ seyyidinâ Muhammedin…</div>
<div class="lbl" style="margin-top:12px">ANLAM</div><div class="field" style="height:64px">Allah'ım! Efendimiz Muhammed'e…</div>
<div class="lbl" style="margin-top:12px">KAYNAK / NİSPET</div><div class="field">site adı, kitap, kime nispet</div>
<div class="lbl" style="margin-top:12px">OKUMA SAYISI</div><div style="margin-top:6px"><span class="chip">Yok (sadece oku)</span><span class="chip">33</span><span class="chip on">100</span><span class="chip">Özel</span></div>
</div>'''
LA='''<h3>Fikir</h3>Uygulamanın altında sabit sekmeler: Salavat · Zikir · Dua · Favori · Diğer. Her bölüm ayrı liste. Sağ altta yuvarlak <b>+</b> düğmesi, açık olan bölüme ekler.
<h3>Ekleme</h3>Tek sayfalık form. Arapça kutusu isteğe bağlı. Okuma sayısını "Yok" seçersen tespihsiz okuma sayfası olur (dualar için).
<h3>Artısı</h3><ul><li>Android'de en alışık olunan düzen</li><li>Bölümler arasında tek dokunuş</li><li>Yapması en hızlı ve en sade seçenek</li></ul>
<h3>Eksisi</h3><ul><li>Alt çubuğa en fazla 5 bölüm sığar</li><li>Yeni bölüm eklemek "Diğer"e düşer</li></ul>'''
page('A.html','Seçenek A · Alt sekmeler','Klasik, sade, hızlı',[phone(A1,'Ana ekran'),phone(A2,'+ Yeni ekle (form)')],LA)

# ===== B: Raflar
tile=lambda k,icon,n,cnt,today: f'''<div class="card" style="flex:1;padding:14px;border-color:{C[k]}55;background:linear-gradient(160deg,{C[k]}22,#151D32 60%)">
<div style="font-size:26px">{icon}</div><div style="font-size:17px;font-weight:700;margin-top:8px">{n}</div><div class="sec" style="font-size:12.5px">{cnt} kayıt</div><div style="font-size:12.5px;margin-top:6px;color:{C[k]};font-weight:700">Bugün {today}</div></div>'''
B1=f'''<div style="padding:10px 16px 0;display:flex;align-items:center"><div style="flex:1"><div class="muted" style="font-size:13px">Cuma · 23 Ekim</div><div style="font-size:24px;font-weight:700">Esselâmü aleyküm</div></div><div style="font-size:20px;margin-right:12px">🔍</div><div style="width:40px;height:40px;border-radius:20px;background:#E8B34A;color:#111522;font-size:26px;display:flex;align-items:center;justify-content:center">+</div></div>
<div style="padding:12px 16px 0;flex:1;overflow:hidden">
<div class="card" style="padding:14px;display:flex;align-items:center;margin-bottom:12px">
 <svg width="64" height="64"><circle cx="32" cy="32" r="26" fill="none" stroke="#26314D" stroke-width="7"/><circle cx="32" cy="32" r="26" fill="none" stroke="#E8B34A" stroke-width="7" stroke-dasharray="110 200" transform="rotate(-90 32 32)" stroke-linecap="round"/></svg>
 <div style="margin-left:14px;flex:1"><div class="lbl">BUGÜN</div><div style="font-size:22px;font-weight:700">335 <span class="sec" style="font-size:14px;font-weight:400">/ 500 günlük hedef</span></div><div class="sec" style="font-size:12.5px">🔥 6 gün üst üste</div></div></div>
<div class="card" style="padding:12px 14px;display:flex;align-items:center;margin-bottom:14px;border-color:#E8B34A88"><div style="flex:1"><div class="lbl">KALDIĞIN YER</div><div style="font-size:16px;font-weight:700">Salât-ı Münciye · 23/100</div></div><div class="acc" style="font-weight:700">Devam ▶</div></div>
<div style="display:flex;gap:10px;margin-bottom:10px">{tile('s','📿','Salavat',8,23)}{tile('z','✦','Zikirler',5,312)}</div>
<div style="display:flex;gap:10px;margin-bottom:14px">{tile('d','🤲','Dualar',12,0)}{tile('e','✧','Esmâ-ül Hüsnâ',99,0)}</div>
<div class="lbl" style="margin-bottom:8px">FAVORİLER</div>
<div style="display:flex;gap:8px;overflow:hidden"><span class="chip">★ Münciye</span><span class="chip">★ Tevhîd</span><span class="chip">★ Rabbenâ âtinâ</span></div>
</div>'''
B2=f'''<div style="flex:1;background:#0b0f1d88;position:relative">
<div style="padding:10px 16px;opacity:.25">{''}<div style="font-size:24px;font-weight:700">Esselâmü aleyküm</div></div>
<div style="position:absolute;left:0;right:0;bottom:0;background:#1A2440;border-radius:24px 24px 0 0;padding:10px 16px 20px">
<div style="width:40px;height:4px;background:#333A55;border-radius:2px;margin:0 auto 12px"></div>
<div style="font-size:18px;font-weight:700;margin-bottom:12px">Ne ekliyorsun?</div>
''' + ''.join(f'''<div class="card" style="display:flex;align-items:center;padding:12px 14px;margin-bottom:8px"><div style="font-size:22px;width:36px">{i}</div><div style="flex:1"><div style="font-weight:700">{t}</div><div class="sec" style="font-size:12.5px">{d}</div></div><div class="muted">›</div></div>''' for i,t,d in [("📿","Salavat","Sayaçlı · Arapça, okunuş, anlam"),("✦","Zikir","Sayaçlı · kısa metin"),("🤲","Dua","Okuma sayfası · sayaç isteğe bağlı"),("✧","Esmâ-ül Hüsnâ","İsim + anlam"),("＋","Yeni bölüm oluştur","Ad, simge ve renk seç")]) + '''
<div class="lbl" style="margin:10px 0 6px">YA DA</div>
<div class="card" style="padding:12px 14px;display:flex;align-items:center;border-style:dashed"><div style="font-size:20px;width:36px">📋</div><div style="flex:1"><b>Panodan yapıştır</b><div class="sec" style="font-size:12.5px">Kopyaladığın metni otomatik ayırır</div></div></div>
</div></div>'''
LB='''<h3>Fikir</h3>Ana ekran bir <b>pano</b>: üstte bugünkü toplam ve günlük hedef halkası, "kaldığın yer", altında büyük renkli bölüm kutuları (Salavat, Zikirler, Dualar, Esmâ…). Kutuya dokun, bölüm listesi açılır.
<h3>Ekleme</h3>Üstteki <b>+</b> alttan bir panel açar: "Ne ekliyorsun?" Bölümü seçince ona uygun form gelir. Buradan <b>yeni bölüm</b> de oluşturulur (ad, simge, renk).
<h3>Artısı</h3><ul><li>Bölüm sayısı sınırsız, her biri kendi renginde</li><li>Günlük hedef ve seri motive eder</li><li>En "uygulama gibi" görünen seçenek</li></ul>
<h3>Eksisi</h3><ul><li>Okumaya başlamak bir dokunuş daha uzun</li><li>Yapması A'dan biraz daha uzun</li></ul>'''
page('B.html','Seçenek B · Pano ve bölüm kutuları','Görsel, motive edici, bölüm sayısı serbest',[phone(B1,'Ana ekran (pano)'),phone(B2,'+ → "Ne ekliyorsun?" paneli')],LB)

# ===== C: tek akış + akıllı yapıştır
rowC=lambda k,n,a,c: f'''<div style="display:flex;align-items:center;padding:12px 4px;border-bottom:1px solid #26314D">
<div style="width:4px;height:42px;border-radius:2px;background:{C[k]};margin-right:12px"></div>
<div style="flex:1;min-width:0"><div style="font-size:15.5px;font-weight:700">{n}</div><div class="sec" style="font-size:12px">{ {"s":"Salavat","z":"Zikir","d":"Dua"}[k]} · {c}</div></div>
<div class="ar" style="width:130px;text-align:right;font-size:16px;color:#AEB5CF;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">{a}</div></div>'''
C1=f'''<div style="padding:10px 16px 0"><div style="font-size:24px;font-weight:700">Virdlerim</div>
<div class="field" style="margin-top:10px;color:#8790AD">🔍  Ara: ad, Arapça ya da Türkçe…</div>
<div style="margin-top:10px;white-space:nowrap;overflow:hidden"><span class="chip on">Tümü</span><span class="chip">★</span><span class="chip" style="border-color:#E8B34A">Salavat</span><span class="chip" style="border-color:#72C98A">Zikir</span><span class="chip" style="border-color:#7FB2F0">Dua</span></div></div>
<div style="padding:0 12px;flex:1;overflow:hidden">{''.join(rowC(*x) for x in items)}</div>
<div style="position:absolute;left:50%;transform:translateX(-50%);bottom:22px;background:#E8B34A;color:#111522;border-radius:28px;padding:14px 26px;font-weight:700;font-size:16px;box-shadow:0 6px 16px #0008">＋ Ekle</div>'''
C2=f'''<div style="padding:10px 16px;display:flex;align-items:center"><div style="font-size:28px;width:34px">‹</div><div style="flex:1;font-size:19px;font-weight:700">Yapıştır ve ekle</div></div>
<div style="flex:1;overflow:hidden;padding:0 16px">
<div class="lbl">1 · İNTERNETTEN KOPYALADIĞIN METİN</div>
<div class="field" style="font-size:13px;line-height:1.5;height:150px;overflow:hidden"><div class="ar" style="text-align:right;font-size:16px">اللَّهُمَّ صَلِّ عَلَى سَيِّدِنَا مُحَمَّدٍ صَلَاةً تُنْجِينَا</div>Allâhümme salli alâ seyyidinâ Muhammedin salâten tüncînâ…<br>Anlamı: Allah'ım! Efendimiz Muhammed'e öyle bir salât eyle ki…<br><span class="muted">Kaynak: islamveihsan.com</span></div>
<div class="lbl" style="margin-top:14px">2 · UYGULAMA AYIRDI <span style="font-weight:400;color:#72C98A">✓ otomatik</span></div>
<div class="card" style="padding:10px 12px;margin-top:6px;font-size:13px;line-height:1.45">
<div><span style="color:#72C98A;font-weight:700">ARAPÇA</span> <span class="ar" style="float:right">اللَّهُمَّ صَلِّ عَلَى…</span></div>
<div style="margin-top:6px"><span style="color:#72C98A;font-weight:700">OKUNUŞ</span>  Allâhümme salli alâ…</div>
<div style="margin-top:6px"><span style="color:#72C98A;font-weight:700">ANLAM</span>  Allah'ım! Efendimiz…</div>
<div style="margin-top:6px"><span style="color:#E8B34A;font-weight:700">KAYNAK</span>  islamveihsan.com</div>
<div class="muted" style="margin-top:6px">Yanlış ayrılan satırı dokunup taşı</div></div>
<div class="lbl" style="margin-top:14px">3 · BÖLÜM VE SAYI</div>
<div style="margin-top:6px"><span class="chip on">Salavat</span><span class="chip">Zikir</span><span class="chip">Dua</span></div>
<div><span class="chip">Sayaçsız</span><span class="chip">33</span><span class="chip on">100</span><span class="chip">Özel</span></div>
<div class="card" style="padding:10px 12px;margin-top:6px;font-size:13px;border-color:#7FB2F055"><span style="color:#7FB2F0;font-weight:700">Arapça yoksa:</span> Türkçe okunuştan Arapçasını Claude ile birlikte bulup sonra eklersin; kayıt "Arapça bekliyor" etiketiyle durur.</div>
</div>
<div style="padding:10px 16px 16px;flex:none"><div class="btn">Kaydet</div></div>'''
LC='''<h3>Fikir</h3>Hepsi <b>tek listede</b>: salavat, zikir, dua iç içe; üstte arama ve renkli filtre çipleri (Tümü, ★, Salavat, Zikir, Dua…). Her satırın sağında Arapçanın başı görünür, solda bölüm rengi şeridi.
<h3>Ekleme</h3><b>Akıllı yapıştır</b>: internetten kopyaladığın bloğu olduğu gibi yapıştırırsın. Uygulama Arapça harfli satırları, okunuşu, "Anlamı:" ile başlayanı ve kaynağı kendisi ayırır. Sen sadece kontrol edip bölümü seçersin.
<h3>Artısı</h3><ul><li>Senin "internetten bul, ekle" alışkanlığına en uygun</li><li>Arama her şeyi tek yerde bulur</li><li>Bölüm eklemek sadece yeni bir çip demek</li></ul>
<h3>Eksisi</h3><ul><li>Liste kalabalıklaşınca filtre şart</li><li>Ayırma bazen yanılır, düzeltme adımı gerekir</li></ul>'''
page('C.html','Seçenek C · Tek akış + akıllı yapıştır','Arama ve hızlı ekleme odaklı',[phone(C1,'Ana ekran (tek liste + filtre)'),phone(C2,'+ Ekle → yapıştır, otomatik ayır')],LC)
