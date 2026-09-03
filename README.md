# (≧▽≦)

Kişisel akademik işletim sistemi.

> **"Daha çok çalışma. Sırada ne olduğunu bil."**
> *Küçük adımlar, büyük ağaçlar.*

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
| 🍎 | **Bahçe** | Ana ekran. Bugünün küçük görevi, büyüme durumu, devam et, son anlatım, projeler, köprüler |
| 📚 | **Müfredat** | 4 faz, 67 birim, 657 görev. Zamansız akış |
| 🧺 | **Brain Inbox** | Hızlı yakalama. Düzenleme sonra |
| 🌱 | **Projeler** | Beş üretim projesi. En önemli alan: SIRADAKİ EYLEM |
| 🎒 | **Çanta** | Anlatımlar, sınavlar, hata defteri, değerlendirme, depolama, kaynaklar |

---

## ⏳ Zamansız Müfredat

Tarih yok. Bir birimin görevleri bitince **sıradaki açılır.**

- Geç kalmak diye bir şey yok
- Bir günü kaçırmak diye bir şey yok
- Hızlı gidersen önden gidersin, yavaş gidersen aynı yoldasın

```
Faz 1 · Temeller          31 birim   ~325 saat
Faz 2 · Derinleşme        12 birim   ~175 saat
Faz 3 · Elektromanyetizma 12 birim   ~175 saat
Faz 4 · Sentez ve Üretim  12 birim   ~180 saat
```

Bahçe metaforu XP değil, gerçek durum: 🌰 tohum → 🌱 filiz → 🪴 fidan → 🌳 ağaç → 🍎 meyve

---

## 🍀 Bugünün Küçük Görevi

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

## 🍂 Hata Defteri

Müfredatın en yüksek getirili aracı. Her hata üç sütun:

1. **Soru**
2. **Neden yanlış yaptım** ← asıl değer burada
3. **Doğru yaklaşım**

Kategoriler: kavram eksiği · teknik hata · dikkatsizlik · cebir · hesap · soruyu yanlış anladım · yetersiz pratik

Uygulama örüntü arar ama suçlamaz:
> *"Dönme dinamiğinde 4 açık hata var — burada kapanmamış bir kavram olabilir."*

---

## 🌱 Beş Üretim Projesi

Müfredat açıkça söylüyor: **dersler projelere hizmet eder, tersi değil.**

| | Proje | Ne |
|---|---|---|
| 🐍 | **fizik.py** | Kendi sayısal fizik kütüphanen — kinematikten ışın izlemeye |
| 🗺️ | **İnteraktif Fizik Atlası** | React + Plotly, parametre kaydırıcılı sayfalar |
| 📄 | **Kuantum Simülasyon Makalesi** | 4006 projesi → İngilizce akademik makale |
| 🇯🇵 | **kotoba** | Kendi Japonca SRS aracın (SM-2) |
| ✍️ | **Deneme Portfolyosu** | Ayda 2 deneme, hedef 16 yazı |

Her proje kartında en önemli alan: **SIRADAKİ EYLEM**.
"fizik.py üzerinde çalış" değil — *"Vector3D cross product metodunu yaz."*

---

## 🔗 Disiplinlerarası Köprüler

Birinci sınıf ilişkiler, süs değil:

```
Entropi ↔ Enformasyon          Boltzmann = Shannon
Bilim Felsefesi ↔ Halliday     Newton→Einstein bir paradigma kayması mı?
Kalkülüs ↔ Felsefe             Zenon ve limit
Edebiyat ↔ Akademik Yazım      argüman kurmak
Japonca ↔ Programlama          kotoba: Unicode, veritabanı, SRS
Olasılık ↔ Kuantum             ölçümün olasılıksal doğası
Lineer Cebir ↔ Kuantum         Hilbert uzayı, özdurum
Fourier ↔ Müzik ↔ Dalga        harmonik analiz
Vektör Analizi ↔ Elektrik Alan Gauss = diverjans teoremi
DD ↔ Salınım ↔ Devre           hepsi aynı denklem
```

Bunlar hem birim sayfalarında görünür hem görev seçimini etkiler.

---

## 📱 Katlanabilir Cihaz (ALT Mive Style Folder 2)

**Kapalı ekran (< 380dp)** — tek bir soru:

> (≧▽≦) — *ne yapıyoruz?*
> Devam et · Anlat · Yakala · Bahçe

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
- Müfredat `assets/curriculum.json` içinde, `tools/gen_curriculum.py` ile üretiliyor
- Ses: `MediaRecorder` (AAC/MP4) + `MediaPlayer`, SAF üzerinden dosya tanımlayıcı
- minSdk 26 · targetSdk 34 · JDK 17
- **Tamamen çevrimdışı.** Sunucu yok, hesap yok, internet izni bile yok.

### Müfredatı değiştirmek

```bash
python3 tools/gen_curriculum.py
```

`tools/gen_curriculum.py` içindeki `U(...)` ve `T(...)` çağrılarını düzenle, scripti çalıştır,
`app/src/main/assets/curriculum.json` yeniden üretilir. Sonra yeniden derle.

---

## 📋 MVP Kapsamı

Hepsi çalışıyor:

Bahçe · Müfredat · Bugünün Küçük Görevi · Brain Inbox · Projeler · Ses kaydı ·
Ses kütüphanesi · Oynatma · Müfredat↔Ses ilişkileri · Hata Defteri · Sınavlar ·
Haftalık değerlendirme · Kullanıcı seçimli klasör · Depolama yönetimi · Katlanabilir arayüz ·
Problem takibi · Kaynaklar · (≧▽≦)'ye Anlat

### Sonraya bırakılanlar

Yapay zekâ transkripsiyon · anlatım analizi · otomatik soru üretimi · Anki dışa aktarma ·
gelişmiş köprü grafiği · widget'lar · kilit ekranı entegrasyonu

Yapay zekâ hiçbir zaman merkeze konmayacak — **senin kendi düşüncen merkezde.**
Temel kayıt ve oynatma yapay zekâsız çalışır, hep öyle kalacak.
