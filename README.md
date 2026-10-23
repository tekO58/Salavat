# Salavat

Salavat kütüphanesi ve tespih. Native Android (Kotlin, ekranlar koddan kurulur).

## Ne var

- **Kütüphane** – her salavat için Arapça metin, Türkçe okunuş, Türkçe anlam,
  kime nispet edildiği ve kaynak. Veriler `app/src/main/assets/salavat.json`.
- **Tespih** – `workplanner-app`'teki tespih sayacının aynısı (33'lük tespih,
  aşağı kaydırarak / dokunarak / +1 ile sayma, titreşim, ses, süre ve hız,
  geri al, sıfırla, geçmiş, 10 tespih görünümü). Her salavatın kendi sayacı var;
  nerede kaldığını hatırlar. Sayma ekranının üstünde salavatın metni durur
  (dokununca Arapça → okunuş → anlam).
- Yazı boyutu (A− / A+), günlük ve toplam sayı.

Tespih dosyaları (`Tasbih*.kt`) workplanner-app'ten kopyalandı; plan, yüzen
balon ve tespih listesi alınmadı. Orada bir düzeltme yapılırsa buraya da
taşınmalı.

## Salavat ekleme

`salavat.json`'a bir kayıt eklenir:

| alan | açıklama |
|---|---|
| `id` | tekil, değişmez (sayaç bu id ile kaydedilir) |
| `name`, `aka` | ad ve diğer adları |
| `arabic`, `reading`, `meaning` | harekeli Arapça, okunuş, anlam |
| `attribution`, `source` | nispet ve kaynak eser |
| `target` | gelenekteki okuma sayısı (ilk hedef) |
| `note` | ek not (isteğe bağlı) |
| `verified` | metin en az iki kaynakla harf harf karşılaştırıldıysa `true` |

`verified: false` olan salavatın sayfasında "henüz karşılaştırılmadı" uyarısı çıkar.

## APK

Her push'ta GitHub Actions testleri çalıştırır ve debug APK üretir
(Actions → son koşu → `salavat-apk`). APK depodaki sabit test anahtarıyla
(`app/debug.keystore`) imzalı; yeni APK telefondakinin üzerine kurulur.
Mağaza yayını için ayrı, gizli bir anahtar gerekecek.
