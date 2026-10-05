# (≧▽≦) — Uygulama Tanıtımı

Kişisel akademik işletim sistemi. Üretkenlik uygulaması değil — pomodoro, puan, seri (streak)
veya geri sayım yok. Tek soruya cevap verir: **şimdi ne yapmalıyım?**

## Tasarım — "Genç Araştırmacı" Estetiği

Glow-Up kardeş uygulamasıyla aynı tema: bordo (`#6E2430`) · lavanta (`#8A7CA8`) · haki
(`#C9BFA0`) · kirli beyaz (`#F5F0E6`) paleti, serif başlıklar + sans gövde metni +
istatistiklerde monospace. Süsleme bilgiyi dekore eder, ezmez; sade, akademik defter hissi
(aşırı animasyon, pastel SaaS, glassmorphism yok). D-pad / 2 tuşlu cihazlarda da kullanılabilir
— odaklanan öğenin etrafında ince bir çerçeve belirir (`Modifier.dpadFocusable`).

## Alanlar (ana gezinme)

| Alan | Ne yapar |
|---|---|
| 🔬 Laboratuvar | Ana ekran — bugünün küçük görevi, deney aşaması, son anlatım, projeler, köprüler |
| 📚 Müfredat | Fazlar → haftalık birimler → günlük görevler; manuel düzenlenebilir |
| 📥 Brain Inbox | Hızlı not yakalama, düzenleme sonraya kalır |
| ⚗️ Projeler | Üretim projeleri (P1/P2/P3 + yaz projesi), her kartta "sıradaki eylem" |
| 🎒 Çanta | Anlatımlar, sınavlar, hata defteri, kaynaklar, depolama, köprü grafiği, tekrar kartları, AI değerlendirme, istatistikler |

## Özellikler

- **Bugünün Küçük Görevi** — rastgele değil: mevcut birim, borçlu Feynman anlatımı, hata
  defteri birikintisi, tekrarlayan hata örüntüleri, bekleyen sınav, sessizleşen proje, taşan
  inbox, gecikmiş değerlendirme — bunlara bakıp tek bir anlamlı eylem seçer ve nedenini söyler.
- **Müfredat** — fazlar/birimler/görevler `assets/curriculum.json`'dan yüklenir; her günlük
  mikro modülün başında gerçek takvim tarihi var. Çanta → **Müfredatı Düzenle** ekranından
  birim/görev elle eklenip çıkarılabilir. Ya da bir `.md`/`.pdf` belge yükleyip AI (Groq/Gemini)
  ile sıfırdan müfredat ürettirilebilir.
- **Aralıklı Tekrar (SM-2)** — tekrar kartları SuperMemo-2 algoritmasıyla zamanlanır (kolaylık
  faktörü, tekrar aralığı); Çanta → Tekrar Kartları'nda "Tekrara başla" ile 4 kaliteli
  (Tekrar/Zor/İyi/Kolay) değerlendirme akışı. Kart elle de eklenebilir.
- **Anlatım Arşivi** — kayıt anında başlar (önce form doldurtmaz), durduktan sonra metadata
  sorar (ders/birim/proje/dil). Feynman kuralı gömülü: bir birim biterse İngilizce anlatım ister.
- **Hata Defteri** — her hata: soru / neden yanlış yaptım / doğru yaklaşım + kategori. Uygulama
  tekrarlayan örüntüleri fark eder ama suçlamaz.
- **AI Destekli Değerlendirme** — Çanta → **Durumu Değerlendir**: ilerleme %, seri, çözülen
  problem, hata örüntüleri, kart kolaylık ortalaması gibi verilerden bir özet çıkarıp Groq/Gemini
  API'sine gönderir, 150 kelimeyi aşmayan, veri odaklı Türkçe bir değerlendirme alır.
- **Zamana Bağlı İstatistikler** — Çanta → **İstatistikler**: gün/hafta/ay/yıl bazında çalışma
  dakikası ve tamamlanan görev grafiği, güncel seri, toplam görev, kart sayısı.
- **Köprü Grafiği** — disiplinlerarası bağlantılar (örn. Kablo Teorisi ↔ P1, Hopfield Ağı ↔
  Transformer Attention) birinci sınıf veri, süs değil.
- **Ana Ekran Widget'ı + Bildirim** — geniş/alçak nixie tüp görünümlü widget, bugünün görevini
  gösterir, ✓ ile tamamlanır, AÇ ile uygulamaya döner; kilit ekranı bildirimi aynı veriyle senkron.
- **Katlanabilir Cihaz Desteği** — kapalı ekranda tek satır görev, açık ekranda kalıcı yan menü +
  ikinci panel, ortada klasik alt sekme çubuğu.
- **Depolama** — uygulama verisi (ilerleme, notlar) ile kullanıcı dosyaları (ses, dışa aktarma,
  yedek) mimari olarak ayrı; SAF ile kullanıcı kendi klasörünü seçer.

## Araçlar / Teknik

- **Kotlin + Jetpack Compose + Material 3**, bağımlılık minimum (Room/Hilt/Navigation yok)
- Durum tek bir JSON dosyasında (`filesDir/state.json`); müfredat `assets/curriculum.json`
- Ses: `MediaRecorder` (AAC/MP4) + `MediaPlayer`
- AI: Groq veya Gemini — tamamen opsiyonel, kendi API anahtarını girmezsen uygulama asla
  internete çıkmaz (transkripsiyon, analiz, soru/müfredat üretimi, değerlendirme için kullanılır)
- Widget: Jetpack Glance (`MissionWidget`)
- minSdk 26 · targetSdk 34 · JDK 17

## Kullanım

APK'yı GitHub Actions (Releases sekmesi) üzerinden indirip kurman yeterli — hesap, sunucu veya
kurulum adımı yok. Klasörünü seçtikten sonra her şey cihazda kalır. Her gün Laboratuvar'ı aç,
önerilen görevi yap ya da "Başka" ile alternatifine geç; birim bitince otomatik sıradaki açılır.
