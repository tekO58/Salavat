# Salavat

Salavat, zikir ve dua kütüphanesi ve tespih.

- **Web uygulaması** (`web/`, şu an geliştirilen): kurulumsuz HTML/CSS/JS,
  GitHub Pages'te yayınlanır (`.github/workflows/pages.yml`). Telefonda adresi
  açıp "Ana ekrana ekle" ile uygulama gibi kullanılır, internetsiz de açılır.
  Kullanıcının verisi o tarayıcıda saklanır (localStorage). Ayırıcı testi:
  `node web/test/parser.test.js`.
- **Android** (`app/`, Kotlin, donduruldu): web'de tasarım oturunca son kez
  güncellenip APK çıkarılacak.

Aşağıdaki ekran ve veri açıklamaları iki sürüm için de geçerli.

### APK aşamasında yapılacaklar

Web'de eklenip Android'e henüz taşınmayanlar:

- Arapça / Okunuş / Anlam / Kaynak kartları açılır kapanır (seçim hatırlanır)
- Tespih ekranında metin penceresi: üçü alt alta, açılır kapanır
- Okuma modu (⧉ Küçült): tespih köşede yuvarlak sayaca döner, metin ekranı kaplar
- Yeni bölüm için renkli emoji simgeler; Zikirler simgesi ✨
- Tespih: büyütülmüş halka (0.35 / 0.45)
- **Vird rutini**: Sabah / Öğle / İkindi / Akşam / Yatsı listeleri, sırayla okuma,
  bitince sıradakine geçiş, sayaçsız kalemde "Okudum"; kayıt sayfasında 📅 Virde ekle
- UX düzeltmeleri: Arapça · Okunuş · Türkçe adları her yerde aynı; tespih ekranı tek ekrana
  sığar (Ses/Titreşim yuvarlak düğmede, TUR kartından hedef); kayıt başlığında Aa,
  alt çubukta 📅 Virde ekle; boş vakte hazır namaz tesbihatı; dokunma alanları en az 40 px
- **Renkler**: koyu "Gece zümrüdü" (varsayılan) ve açık "Gündüz" teması, panoda ☀️/🌙 ile
  değişir; +1, sekmeler ve tespih halkası vurgu renginde, boncuklar materyalden;
  uygulama simgesi de yeni renklerde (yeşil hilal)
- **Kayıt paneli**: listede kayda dokununca alttan panel; Arapça / Okunuş / Türkçe ile oku
  (sayfa yalnız o metin açık gelir), Tespihle oku, Virde ekle, Favori. Listedeki önizleme
  son seçilen dilde
- **Yedek**: panonun altında "Yedekle / Geri yükle"; bütün veri tek JSON dosyasına
  (telefonda paylaş menüsü: Drive, WhatsApp, Dosyalar), dosyadan geri yükleme; 7 günden
  eski yedekte uyarı rengi
- **Okuma sayfası** (mushaf): kâğıt zemin, Arapça / Okunuş / Anlam çipleri, sayfa
  sayfa çevirme, sürüklenebilir yuvarlak sayaç, Aa yazı boyutu
- **Diğer uygulamaların üstünde tespih balonu** (bildirimden +1 ile): yalnız
  Android'de mümkün; `workplanner-app`'teki `TasbihFloatService` ve
  `TasbihBubbleView` taşınacak

Tespih görünümü (10 materyal), süre / kalan tahmini ve gerçekçi tespih Android'de
zaten var (workplanner'dan kopyalanan `Tasbih*.kt`).

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
  çipleri (Tümü, ★, bölümler), satırda Arapçanın başı.
- **Kayıt sayfası** – Arapça, okunuş, anlam, kaynak; ☆ favori; A− / A+;
  kullanıcının eklediği kayıtta ⋮ Düzenle / Sil; okuma sayısı ve "Tespihle oku".
- **Tespih** – `workplanner-app`'teki tespih sayacının aynısı (33'lük tespih,
  aşağı kaydırarak / dokunarak / +1 ile sayma, titreşim, ses, süre ve hız,
  geri al, sıfırla, geçmiş, 10 tespih görünümü). Üstte kaydın metni
  (dokununca Arapça → okunuş → anlam).

Tespih dosyaları (`Tasbih*.kt`) workplanner-app'ten kopyalandı; plan, yüzen
balon ve tespih listesi alınmadı. Orada bir düzeltme yapılırsa buraya da
taşınmalı.

## Veri

- Hazır kayıtlar: `app/src/main/assets/salavat.json`.
- Kullanıcının eklediği kayıtlar, bölümler, favoriler ve günlük hedef:
  telefonda `kutuphane` ayar dosyası (`LibraryStore`).
- Sayaçlar ve günlük geçmiş: `tesbihlerim` (`TasbihStore`), kaydın id'siyle.

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

Her push'ta GitHub Actions debug APK üretir (Actions → son koşu → `salavat-apk`).
Birim testleri her 10 koşuda bir ya da "Run workflow"da "Birim testlerini
çalıştır" işaretlenince çalışır. APK depodaki sabit test anahtarıyla
(`app/debug.keystore`) imzalı; yeni APK telefondakinin üzerine kurulur.
Mağaza yayını için ayrı, gizli bir anahtar gerekecek.
