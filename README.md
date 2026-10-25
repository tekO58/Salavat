# Salavat

Salavat, zikir ve dua kütüphanesi ve tespih.

Tek kod: **web uygulaması** (`web/`, HTML/CSS/JS). İki yoldan kullanılır:

- **Web**: GitHub Pages'te yayınlanır (`.github/workflows/pages.yml`):
  https://teko58.github.io/Salavat/ — telefonda "Ana ekrana ekle" ile uygulama
  gibi açılır, internetsiz de çalışır. Veri o tarayıcıda (localStorage).
- **Android APK** (`app/`): aynı web uygulamasının kabuğu. Derlemede `web/`
  APK'nin içine kopyalanır (`assets/web/`) ve WebView'da açılır; görünüm ve
  özellikler birebir aynı. Android'e özgü olanlar Kotlin'de:
  - `MainActivity`: WebView, geri tuşu, yedek dosyasını kaydetme / seçme,
    durum çubuğu rengi, ekranı açık tutma
  - `WebBridge`: sayfaya `window.SalavatAndroid` köprüsü (web'de `NATIVE`):
    titreşim, pano, dosya, tema, balon
  - **Tespih balonu** (`TasbihFloatService`, `TasbihBubbleView`; workplanner-app'ten):
    tespih ekranında ⋮ → 🫧 Balon. Diğer uygulamaların üstünde küçük sayaç
    (dokun +1, sürükle taşı, uzun bas uygulamaya dön, X'e bırak kapat) ve
    bildirimde +1 / Geri al / Kapat. Balon dokunuşları zamanlarıyla birikir
    (`BubbleCounter`), uygulama öne gelince web sayacına aynı kuralla işlenir.

Testler: `node web/test/parser.test.js`, `node web/test/tasbih.test.js`,
`./gradlew testDebugUnitTest` (balon sayacı).

### Özellikler

- Arapça / Okunuş / Türkçe / Kaynak kartları açılır kapanır (seçim hatırlanır)
- Tespih ekranında metin penceresi: Arapça · Okunuş · Türkçe düğmeleri, açılır kapanır
- Okuma modu (⧉ Küçült): tespih köşede yuvarlak sayaca döner, metin ekranı kaplar
- Yeni bölüm için renkli emoji simgeler; Zikirler simgesi ✨
- Tespih: 10 materyal, gerçekçi halka (boşluk, imame), süre / kalan tahmini;
  +1 düğmesi yok, tespih alanının tamamına dokunmak ya da aşağı kaydırmak sayar
- **Vird rutini**: Sabah / Öğle / İkindi / Akşam / Yatsı listeleri, sırayla okuma,
  bitince sıradakine geçiş, sayaçsız kalemde "Okudum"; boş vakte hazır namaz tesbihatı
- **Renkler**: koyu "Gece zümrüdü" (varsayılan) ve açık "Gündüz" teması, panoda ☀️/🌙
- **Kayıt paneli**: listede kayda dokununca alttan panel; Arapça / Okunuş / Türkçe ile oku
  (sayfa yalnız o metin açık gelir), Tespihle oku, Virde ekle, Favori. Listedeki önizleme
  son seçilen dilde
- **Yedek**: panonun altında "Yedekle / Geri yükle"; bütün veri tek JSON dosyasına,
  dosyadan geri yükleme; 7 günden eski yedekte uyarı rengi
- **Okuma sayfası** (mushaf): kâğıt zemin, sayfa sayfa çevirme, sürüklenebilir
  yuvarlak sayaç, Aa yazı boyutu

## Ekranlar

Tasarım: "B + C" (görseller `docs/ui/`).

- **Pano (ana ekran)** – bugünkü toplam ve günlük hedef halkası (dokununca
  hedef değişir), üst üste gün serisi, kaldığın yer, renkli bölüm kutuları
  (Salavat, Zikirler, Dualar, kullanıcının açtıkları, + Yeni bölüm), favoriler.
- **+ "Ne ekliyorsun?"** – bölüm seç, yeni bölüm oluştur (ad, simge, renk) ya da
  panodan yapıştır.
- **Yapıştır ve ekle** – internetten kopyalanan metin yapıştırılır, uygulama
  Arapça / okunuş / anlam / kaynak olarak ayırır (`PasteParser`), kullanıcı
  düzeltir, bölüm ve okuma sayısını seçer. Arapça boş bırakılabilir: kayıt
  "Arapça bekliyor" etiketiyle durur. "Sayaçsız" kayıt yalnızca okuma sayfasıdır.
- **Bölüm listesi** – arama (şapka, büyük harf ve harekeyi yok sayar), filtre
  çipleri (Tümü, ★, bölümler), satırda seçili dilde metnin başı; dokununca kayıt paneli.
- **Kayıt sayfası** – Arapça, okunuş, Türkçe, kaynak; ☆ favori; Aa yazı boyutu;
  kullanıcının eklediği kayıtta ⋮ Düzenle / Sil; okuma sayısı, 📅 Virde ekle, "Tespihle oku".
- **Tespih** – `workplanner-app`'teki tespih sayacından (`web/tasbih.js`,
  `web/rosary.js`): aşağı kaydırarak / dokunarak sayma, titreşim, ses, süre ve hız,
  geri al, sıfırla, geçmiş, 10 tespih görünümü; Android'de 🫧 Balon.

Tespih balonu (`TasbihFloatService`, `TasbihBubbleView`) workplanner-app'ten
uyarlandı. Orada bir düzeltme yapılırsa buraya da taşınmalı.

## Veri

- Hazır kayıtlar: `app/src/main/assets/salavat.json` (web'e ve APK'ye kopyalanır).
- Kullanıcının bütün verisi (kayıtlar, bölümler, sayaçlar, günlük geçmiş, vird
  listeleri, favoriler, ayarlar) localStorage'da `salavat.v1` anahtarında; APK'de
  WebView'in kendi depolamasında. Yedek dosyası aynı veriyi taşır.
- Balonun henüz işlenmemiş dokunuşları: APK'de `tespih_balonu` ayar dosyası.

Hazır kayıt alanları:

| alan | açıklama |
|---|---|
| `id` | tekil, değişmez (sayaç bu id ile kaydedilir) |
| `section` | `salavat`, `zikir` ya da `dua` |
| `name`, `aka` | ad ve diğer adları |
| `arabic`, `reading`, `meaning` | harekeli Arapça, okunuş, anlam |
| `attribution`, `source` | nispet ve kaynak eser |
| `target` | okuma sayısı (ilk hedef); `0` = sayaçsız |
| `note` | ek not (isteğe bağlı) |
| `verified` | metin en az iki kaynakla harf harf karşılaştırıldıysa `true` |

`verified: false` olan hazır kaydın sayfasında "henüz karşılaştırılmadı" uyarısı çıkar.

## APK

Her push'ta GitHub Actions debug APK üretir (Actions → son koşu → `salavat-apk`);
web değişince de, çünkü APK web uygulamasını içinde taşır.
Birim testleri her 10 koşuda bir ya da "Run workflow"da "Birim testlerini
çalıştır" işaretlenince çalışır. APK depodaki sabit test anahtarıyla
(`app/debug.keystore`) imzalı; yeni APK telefondakinin üzerine kurulur.
Mağaza yayını için ayrı, gizli bir anahtar gerekecek.
