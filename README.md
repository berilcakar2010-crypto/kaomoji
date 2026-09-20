# (≧▽≦)

Kişisel akademik işletim sistemi.

> **"Daha çok çalışma. Sırada ne olduğunu bil."**
> *Küçük ölçümler, kesin sonuçlar.*

Bu bir üretkenlik uygulaması değil. Pomodoro yok, seri (streak) yok, puan yok, geri sayım yok.
Tek bir soruya cevap verir: **şimdi ne yapmalıyım?**

---

## 📱 APK Nasıl Alınır

### Yol 1 — GitHub Actions (önerilen, bilgisayara hiçbir şey kurmadan)

1. Bu klasörü GitHub'da yeni bir repoya yükle:

```bash
cd kaomoji
git init
git add .
git commit -m "(≧▽≦) ilk sürüm"
git branch -M main
git remote add origin https://github.com/KULLANICI_ADIN/kaomoji.git
git push -u origin main
```

2. GitHub'da repona git → **Actions** sekmesi → **APK Derle** iş akışı otomatik başlar
   (başlamazsa **Run workflow** butonuna bas).

3. 3–6 dakika sonra iş biter. İşe tıkla → sayfanın altındaki **Artifacts** bölümünden
   **`kaomoji-debug-apk`** dosyasını indir → zip'ten çıkar → telefona at → kur.

> Telefonda "bilinmeyen kaynaklardan yükleme" iznini vermen gerekebilir.

### Yol 2 — Sürüm etiketiyle otomatik Release

```bash
git tag v1.0
git push origin v1.0
```

APK otomatik olarak repo'nun **Releases** sayfasına yüklenir — telefondan doğrudan indirilebilir.

### Yol 3 — Kendi bilgisayarında

Android Studio ile klasörü aç, Gradle senkronizasyonunu bekle, `Run` bas.
Komut satırından: `gradle assembleDebug` (Gradle 8.9+, JDK 17).

---

## 🌍 Uygulama Yapısı

Beş alan. Onlarca sekme yok — tek bir küçük dünya.

| | Alan | Ne yapar |
|---|---|---|
| 🔬 | **Laboratuvar** | Ana ekran. Bugünün küçük görevi, deney aşaması, devam et, son anlatım, projeler, köprüler |
| 📚 | **Müfredat** | 10 faz, 38 birim (haftalık), 566 görev (20 Eylül 2026–20 Haziran 2027). Her mikro modülün başında gerçek takvim tarihi |
| 📥 | **Brain Inbox** | Hızlı yakalama. Düzenleme sonra |
| ⚗️ | **Projeler** | Beş üretim projesi. En önemli alan: SIRADAKİ EYLEM |
| 🎒 | **Çanta** | Anlatımlar, sınavlar, hata defteri, değerlendirme, depolama, kaynaklar |

---

## ⏳ Zamansız Müfredat, Tarihli Görevler

Fazlar arası sıra hâlâ zamansız — bir birimin görevleri bitince sıradaki açılır,
geç kalmak ya da bir günü kaçırmak diye bir şey yok. Ama artık **tüm yıl boyunca**
(sadece Eylül-Aralık değil) her günlük mikro modülün metni gerçek takvim
tarihiyle başlıyor (ör. **"21 Eylül · Cable theory teorik özeti..."**,
**"9 Şubat · Hopfield network kodlamasına devam..."**) — gerçek 2026-2027 okul
yılı programından (20 Eylül 2026 – 20 Haziran 2027) elle çıkarıldı.

```
Eylül · Kurulum ve Keşif                    3 birim   ~30 saat
Ekim · Derinleşme                            4 birim   ~58 saat
Kasım · Tamamlama ve Sıkılaştırma            4 birim   ~54 saat
Aralık · Bitirme, Toparlama, Dinlenme        5 birim   ~44 saat
Ocak · Dönem 1 Kapanışı + AP Biyoloji Girişi 3 birim   ~34 saat
Şubat                                        3 birim   ~42 saat
Mart                                         4 birim   ~55 saat
Nisan · AP Sınavlarına Geri Sayım            4 birim   ~52 saat
Mayıs · AP Sınavları ve Kapanış              5 birim   ~46 saat
Haziran · Yıl Sonu Kapanışı                  3 birim   ~19 saat
```

Her birim bir hafta (Hafta 0 – Hafta 37). Şubat başı ile Ocak sonu arasında
kaynağın kendi yarıyıl tatili boşluğu var (~25 Ocak – 8 Şubat) — haftalar
numara olarak ardışık ama takvimde değil, uygulama bunu doğru hesaplıyor.
AP sınav haftası (Hafta 31, 3-7 Mayıs) gibi bazı bloklarda hangi günün hangi
derse denk geleceği College Board takviminden geldiği için kaynağın kendisi
de "değişken" diyor — o tek görev tarih aralığı olarak yazılıyor, uydurma tek
gün değil.

Laboratuvar metaforu XP değil, gerçek durum: 🧊 ham numune → ⚗️ tepkimede → 🧪 stabilize → 🔬 analiz → 💠 sonuçlandı

---

## 🎯 Bugünün Küçük Görevi

Rastgele değil. Şunlara bakıp **tek bir anlamlı eylem** seçer ve **neden seçtiğini söyler**:

1. Mevcut birim ve sıradaki bitmemiş görev
2. Borçlu kalan Feynman kaydı (birim bitti ama anlatım yok)
3. Hata Defteri'nde biriken çözülmemiş sorular
4. Aynı derste tekrar eden hata örüntüleri
5. Bu birime bağlı, girilmemiş sınav
6. Sessizleşen projeler
7. Taşan Brain Inbox
8. Gecikmiş haftalık değerlendirme

Öğrenme "X dakika çalışmak" değildir. Uygulama şunları eşit görür:
**çözmek · anlatmak · okumak · yazmak · kodlamak · tekrar etmek · üretmek · bağlantı kurmak**

Görev bir emir değil — "Başka" butonu her zaman alternatif sunar.

---

## 🎙️ Anlatım Arşivi

Sıradan bir ses kaydedici değil. Kendi sesinle kurduğun sözlü bilgi arşivi.

- **Kayıt anında başlar.** Önce form doldurtmaz.
- Durdurduktan sonra sorar: ne anlattın, hangi ders, hangi birim, hangi proje, hangi dil
- "Sonra düzenle" ile metadata tamamen atlanabilir
- Feynman kuralı doğrudan gömülü: birim biterse uygulama İngilizce anlatmanı ister
- Kayıt kalıcı olarak o birime bağlanır — eski ve yeni anlatımlarını karşılaştırabilirsin
- Kaset etiketi estetiği, Spotify değil

---

## ⚠️ Hata Defteri

Müfredatın en yüksek getirili aracı. Her hata üç sütun:

1. **Soru**
2. **Neden yanlış yaptım** ← asıl değer burada
3. **Doğru yaklaşım**

Kategoriler: kavram eksiği · teknik hata · dikkatsizlik · cebir · hesap · soruyu yanlış anladım · yetersiz pratik

Uygulama örüntü arar ama suçlamaz:
> *"Dönme dinamiğinde 4 açık hata var — burada kapanmamış bir kavram olabilir."*

---

## ⚗️ Üretim Projeleri

Müfredat açıkça söylüyor: **dersler projelere hizmet eder, tersi değil.**

| | Proje | Ne |
|---|---|---|
| 🧠 | **P1 — Destek Eğitim Odası: Çok Bölmeli HH Nöron Modeli** | Gerçek morfoloji (NeuroMorpho.org) + gerçek elektrofizyoloji (Allen Brain Atlas) verisiyle kablo-teorisi tabanlı, stokastik iyon kanalı gürültülü, iki sinaptik bağlı nöron simülasyonu. Aralık'ta teslim, Ocak'ta arşivleniyor. |
| 🕸️ | **P2 — Hopfield Ağı ile İlişkisel Bellek Modellemesi** | Ocak'ta başlıyor (P1'in yerini alıyor), Mart'ta teslim: klasik Hopfield ağı (Hebbian öğrenme, kapasite/gürültü testleri, enerji manzarası), biyolojik bellek fenomenleriyle karşılaştırma, opsiyonel Transformer-attention bağlantısı. |
| 🍀 | **P3 — UWC Başvurusu & Felsefe Kulübü** | UWC başvurusu birincil öncelik (Kasım'da gönderim); felsefe kulübü (analitik felsefe + bilim felsefesi) tüm yıl boyunca topluluk katkısı kanıtı olarak sürdürülüyor. |
| ☀️ | **Yaz Projesi (olası P3-2)** | P2 bitince (Mart'tan itibaren) planlanmaya başlayan, Mayıs-Haziran'da yürütülen üçüncü üretim projesi — kapsamı henüz netleşmedi. |

Her proje kartında en önemli alan: **SIRADAKİ EYLEM**.
"P1 üzerinde çalış" değil — *"NeuroMorpho.org'dan bir piramidal nöron morfolojisi seç ve kablo denkleminin sayısal iskeletini kur."*

---

## 🔗 Disiplinlerarası Köprüler

Birinci sınıf ilişkiler, süs değil:

```
Kablo Teorisi Köprüsü                 PDE + devre teorisi + gerçek nöron morfolojisi (P1)
Stokastik Süreçler ↔ Kanal Gürültüsü  Markov modeli = iyon kanalı açılıp kapanması
Dinamik Sistemler ↔ Senkronizasyon    salınım + diferansiyel denklem + iki nöron senkronu
Hopfield Ağı ↔ Transformer Attention  attractor network ↔ güncel yapay zeka mimarileri (P2)
```

Bunlar hem birim sayfalarında görünür hem görev seçimini etkiler.

---

## 📱 Katlanabilir Cihaz (ALT Mive Style Folder 2)

**Kapalı ekran (< 380dp)** — tek bir soru:

> (≧▽≦) — *ne yapıyoruz?*
> Devam et · Anlat · Yakala · Lab

Bugünün görevi tek satır olarak görünür. Dashboard yok, utandırma yok.
Asla *"X dakika boşa harcadın"* demez.

**Açık ekran (≥ 640dp)** — kalıcı yan menü + ikinci panel.
Hangi ekranda olursan ol, bugünün görevi sağda durur.

**Arada (380–640dp)** — klasik alt sekme çubuğu.

---

## 📁 Depolama

Uygulama verisi ile kullanıcı dosyaları **mimari olarak ayrı**.

| Uygulama verisi (dahili) | Kullanıcı dosyaları (senin klasörün) |
|---|---|
| İlerleme, notlar, hatalar, metadata | Ses kayıtları, dışa aktarmalar, yedekler |

Klasörü sen seçersin (Android SAF). Uygulama şu yapıyı otomatik kurar:

```
(≧▽≦)/
├── Audio/
├── Transcripts/
├── Exports/
├── Backups/
├── Generated/
└── Projects/
```

Uygulamayı silip yeniden kursan bile aynı klasörü seçerek dosyalarına dönersin.
Klasör seçilmezse uygulama kendi harici klasörüne yazar — kayıt her zaman çalışır.

---

## 🎨 VOIDLAB

Void siyahı · elektrik moru · derin mor · sinyal kırmızısı · nixie kehribarı · kireç beyazı · loş lavanta

Karanlık laboratuvar defteri hissi: neredeyse siyah zemin, ince mor devre çizgileri (osiloskop ızgarası),
kesik çizgili sinyal çerçeveleri, monospace başlıklar, sade sans gövde metni.
Vurgular kırmızı (uyarı/hata) ve kehribar (nixie tüp parıltısı) — mor birincil, kırmızı ikincil.
Widget ve kilit ekranı bildirimi gerçek bir **nixie tüp göstergesi** gibi: cam koyu, çerçeve ince mor,
sinyal noktası kırmızı, metin kehribar parıltısında.

Süsleme bilgiyi **dekore eder**, ezmez. Hiyerarşi her zaman net kalır.

Kaçınılanlar: pastel SaaS, kurumsal dashboard, aşırı glassmorphism, jenerik Notion görünümü,
steril Material, çocuksu anaokulu estetiği, aşırı animasyon.

---

## 🗂️ Teknik

- **Kotlin + Jetpack Compose + Material 3**
- **Bağımlılık yok denecek kadar az** — Room yok, Hilt yok, Navigation kütüphanesi yok
- Durum tek bir JSON dosyasında (`filesDir/state.json`) — hızlı, taşınabilir, yedeklenebilir
- Müfredat `assets/curriculum.json` içinde. Aktif olan, `tools/gen_curriculum_2026_2027.py`
  ile üretilen tam 2026-2027 okul yılı müfredatı — **20 Eylül 2026'dan 20 Haziran 2027'ye
  kadar 38 hafta, hepsi gün bazlı gerçek takvim tarihli** (Şubat-Haziran artık iskelet değil,
  kaynağın kendisi de tüm yıl için gün bazlı program veriyor). Gerçek okul programından elle
  yapılandırılmış — AI üretimi değil. Script takvim aritmetiğini (ay geçişleri, ~25 Ocak-8
  Şubat yarıyıl tatili boşluğu) `datetime` ile hesaplıyor ve her haftanın gün kodunu
  (Pzt/Sal/…) gerçek haftanın günüyle çapraz doğruluyor (`assert`). Eski
  `gen_curriculum_2026_2027_donem1_v1.py` (yalnızca Dönem 1 detaylı, Dönem 2 aylık iskelet),
  `gen_curriculum_eylul_aralik_2026.py` (yalnızca Eylül-Aralık) ve daha önceki
  `gen_curriculum.py`/`v2`/`v3` sürümleri referans için repoda duruyor.
- Ses: `MediaRecorder` (AAC/MP4) + `MediaPlayer`, SAF üzerinden dosya tanımlayıcı
- minSdk 26 · targetSdk 34 · JDK 17
- **Temelde çevrimdışı.** Sunucu yok, hesap yok. AI özellikleri (transkripsiyon, analiz,
  soru/müfredat üretimi) opsiyonel — kendi Groq/Gemini API anahtarını girersen çalışır,
  girmezsen uygulama hiçbir zaman internete çıkmaz.

### Müfredatı değiştirmek

```bash
python3 tools/gen_curriculum_2026_2027.py
```

`tools/gen_curriculum_2026_2027.py` içindeki `WEEKS` listesini düzenle (her hafta bir
`(gün, konu_kodu, metin, dakika, tür)` listesi), scripti çalıştır,
`app/src/main/assets/curriculum.json` yeniden üretilir. Tarihler otomatik hesaplanır —
elle tarih yazmana gerek yok. Sonra yeniden derle.

Ya da hiç script'e dokunmadan: Çanta → **Müfredat Oluştur** ekranından bir `.md`/`.pdf`
belge yükleyip (Groq veya Gemini) API anahtarını girince, uygulama o belgeye uygun,
sıfırdan bir müfredat kurup mevcut varsayılanın yerine koyar.

---

## 📋 MVP Kapsamı

Hepsi çalışıyor:

Laboratuvar · Müfredat · Bugünün Küçük Görevi · Brain Inbox · Projeler · Ses kaydı ·
Ses kütüphanesi · Oynatma · Müfredat↔Ses ilişkileri · Hata Defteri · Sınavlar ·
Haftalık değerlendirme (video günlüğü dahil) · Kullanıcı seçimli klasör · Depolama yönetimi
ve başka bir yere yedekleme · Katlanabilir arayüz · Problem takibi · Kaynaklar · Kayda Anlat ·
Köprü grafiği · Ana ekran widget'ı + kilit ekranı bildirimi · AI transkripsiyon ve anlatım
analizi (Groq/Gemini) · Otomatik soru üretimi + Anki dışa aktarma · Belgeden (.md/.pdf)
özel müfredat üretme

### Sonraya bırakılanlar

Otomatik köprü çıkarımı (şu an elle tanımlı) · çoklu cihaz senkronizasyonu

Yapay zekâ hiçbir zaman merkeze konmayacak — **senin kendi düşüncen merkezde.**
Temel kayıt ve oynatma yapay zekâsız çalışır, hep öyle kalacak. AI özellikleri
(transkripsiyon, analiz, soru/müfredat üretimi) tamamen opsiyonel — kendi API
anahtarını girmezsen uygulama hiçbir zaman dışarı istek atmaz.
