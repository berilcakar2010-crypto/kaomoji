# Lab

Kişisel akademik işletim sistemi — bir bilgi grafiği üzerine kurulu.

> **"Daha çok çalışma. Sırada ne olduğunu bil."**

Bir üretkenlik uygulaması değil. Pomodoro yok, seri (streak) yok, puan yok, geri sayım yok.
Müfredat bir takvim değil — kavramların önkoşul/bağlantı ilişkileriyle birbirine bağlı olduğu
bir **bilgi grafiği**. Öğrenme "soru-önce" ilkesiyle ilerir: önce kendi tahminini/denemeni
yazarsın, sonra sadece ihtiyacın olan kadarı açığa çıkar.

---

## 📱 APK Nasıl Alınır

### Yol 1 — GitHub Actions (önerilen, bilgisayara hiçbir şey kurmadan)

1. Repoya git → **Actions** sekmesi → **APK Derle** iş akışı otomatik başlar
   (başlamazsa **Run workflow** butonuna bas).
2. 3–6 dakika sonra iş biter. İşe tıkla → sayfanın altındaki **Artifacts** bölümünden
   **`kaomoji-debug-apk`** dosyasını indir → zip'ten çıkar → telefona at → kur.

> Telefonda "bilinmeyen kaynaklardan yükleme" iznini vermen gerekebilir.

### Yol 2 — Sürüm etiketiyle otomatik Release

```bash
git tag v1.0
git push origin v1.0
```

APK otomatik olarak repo'nun **Releases** sayfasına yüklenir.

### Yol 3 — Kendi bilgisayarında

Android Studio ile klasörü aç, Gradle senkronizasyonunu bekle, `Run` bas.
Komut satırından: `gradle :app:assembleDebug` (Gradle 8.9+, JDK 17).

---

## 🧠 Ana Komut Yüzeyi

Açılışta tek bir ekran: "şu an önemli olan" — yaklaşan veya hedef tarihi geçmiş her şey,
hızlı yakalama kutusu, kavram listesi, son eklenenler. Dashboard değil, bir soruya cevap:
**şimdi ne yapmalıyım?**

| | Bölüm | Ne yapar |
|---|---|---|
| 🧠 | **Kavramlar** | Önkoşul/bağlantı ilişkileriyle birbirine bağlı bilgi grafiği; her kavram bir öğrenme oturumuna açılır |
| 🪞 | **Değerlendir** | Gerçek sayılar önce, AI yorumu ayrı ve etiketli bir blokta sonra |
| ⚠️ | **Hata Defteri** | Soru / neden yanlış yaptım / doğru yaklaşım — tekrarlayan örüntüleri fark eder, suçlamaz |
| 🃏 | **Tekrar Kartları** | SM-2 aralıklı tekrar algoritması |
| ⚗️ | **Projeler** | En önemli alan: SIRADAKİ EYLEM, görev listesi değil |
| 📋 | **Sınavlar** | Kapsam + hazırlık durumu; tarihli bir sınav otomatik olarak ana ekranın "önemli olan" listesine girer |
| 🔎 | **Ara** | Tüm bilgi grafiğinde genel arama |
| 📁 | **Verim** | Tüm verinin dışa/içe aktarımı (SAF, düz JSON — sunucu yok, hesap yok) |

Tam teknik durum, neyin gerçekten bittiği ve neyin hâlâ eksik olduğunun dürüst listesi:
[`LAB_2.0_ARCHITECTURE.md`](./LAB_2.0_ARCHITECTURE.md).

---

## 📚 Müfredat İçe Aktarma

Uygulama kendi müfredatını yazmaz — bir dış **Curriculum Contract**'a uyan JSON paketini
içe aktarır (kavramlar, önkoşullar, bağlantılar, bağlamlar). Hem bu sözleşmeye tam uyan
paketler hem de daha zengin dış şemalar (özel bir adaptörle) desteklenir.

---

## 🤖 Yapay Zeka — Sınırlı Yetkiler

AI hiçbir zaman merkezde değil ve hiçbir zaman sessizce bir şeyi değiştirmez:

- **Açıkla** — bir kavramı, varsa kendi tahminini önce değerlendirerek açıklar
- **Değerlendir** — ilerleme özeti üretir, sayıların yerine geçmez
- **Not düzenle** — ham notları düzenler/özetler, kaynak uydurmaz
- **Yazım yardımı** — sonuç ayrı gösterilir, "bu metni kullan" demeden hiçbir alana yazılmaz
- **Plan öner** — sadece öneri; bu çağrı zincirinde bir zamanlamaya yazma yetkisi olan hiçbir kod yok

Tamamen opsiyonel — kendi Groq veya Gemini API anahtarını girersen çalışır, girmezsen
uygulama hiçbir zaman internete çıkmaz. Anahtarlar Android Keystore destekli
`EncryptedSharedPreferences` ile saklanır.

---

## 📁 Veri Sahipliği

Ürettiğin her şey (kavram, oturum, hata, kart, not, proje, sınav) tek bir düz JSON dosyasına
aktarılır/geri yüklenir (SAF — yeni izin yok, sunucu yok, hesap yok, vendor lock-in yok).

---

## 🎨 Görsel Kimlik

"Genç araştırmacı" estetiği — Glow-Up kardeş uygulamasıyla aynı palet: kirli bordo-kızıl
(ana vurgu) + soluk mor-eflatun (ikincil vurgu) + kirli haki (zemin) + kırık beyaz (kart
yüzeyi). Serif başlıklar + sans gövde metni + istatistiklerde monospace. Süsleme bilgiyi
dekore eder, ezmez. D-pad / 2 tuşlu cihazlarda da kullanılabilir.

Kaçınılanlar: anime/karakter referansları, pastel SaaS, kurumsal dashboard, aşırı
glassmorphism, jenerik Notion görünümü, steril Material.

---

## 📱 Katlanabilir / Tablet Desteği

Portre-tablet-öncelikli, üç kırılma noktası (`Lab2Breakpoint`): dar ekranlarda klasik akış,
geniş ekranlarda bir detay ekranı (graf/oturum/arama/değerlendirme) ana listeyi gizlemez —
ikisi yan yana durur, "nereden geldin" her zaman görünür kalır.

---

## 🗂️ Teknik

- **Kotlin + Jetpack Compose + Material 3**
- **Room** — bilgi nesnesi/ilişki/bağlam grafiği için yapılandırılmış yerel depolama
  (tek bir base entity + `kind` ayracı, ayrı first-class ilişki tablosu)
- **AI**: Groq veya Gemini, sağlayıcı-agnostik bir arayüz (`AIProvider`) arkasında;
  `AICapabilityGate` yapısal olarak hiçbir depo referansı taşımaz — bu, AI'nin bir
  zamanlamayı asla sessizce değiştiremeyeceğinin mimari garantisi
- **SM-2** aralıklı tekrar, epoch-day tabanlı
- Ana ekran widget'ı: Jetpack Glance — vadesi gelen kart sayısı + en yakın sınav/ödev
- minSdk 26 · targetSdk 34 · JDK 17
- **Temelde çevrimdışı.** Sunucu yok, hesap yok.

### Eski veriden göç

Bu uygulama daha önce sabit, takvime bağlı bir müfredat sistemiydi (`Store.kt`'ye bağlı
ekranlar). O sistem tamamen kaldırıldı ve yerini bu bilgi-grafiği mimarisi aldı. Eğer
cihazında o eski sürümden kalma veri varsa (hatalar, tekrar kartları, anlatımlar, Brain
Inbox notları, proje/sınav durumları), **Verim → Eski Verimi Kopyala** bu veriyi yeni
grafiğe bir kerelik, salt-okunur bir geçişle taşır. İnce taneli eski istatistikler
(`done`/`dailyLogs`/`problems`) kasıtlı olarak taşınmaz — bkz. `LAB_2.0_ARCHITECTURE.md`.

---

## 📋 Kapsam

Çalışan: bilgi grafiği (kavram/ilişki/bağlam), soru-önce öğrenme oturumları, dış müfredat
paketi içe aktarma, hata defteri, tekrar kartları (SM-2), projeler, sınavlar, AI destekli
açıklama/değerlendirme/not-düzenleme/yazım-yardımı/plan-önerisi (opsiyonel), genel arama,
veri dışa/içe aktarma, eski veri göçü, ana ekran widget'ı.

Sonraya bırakılanlar ve dürüst eksik listesi: [`LAB_2.0_ARCHITECTURE.md`](./LAB_2.0_ARCHITECTURE.md).
