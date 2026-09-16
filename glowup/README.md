# Glow-Up

"Şimdi ne yapsam?" sorusuna anlık, kategorize edilmiş bir cevap veren, 2 tuşlu
(D-pad + onay) düşük özellikli bir folder-style Android telefon için tasarlanan
uygulama. Ayrıntılı plan için proje kök dizinindeki geliştirme planına bakın.

## Durum: Aşama 1 — Mimari ve Temel Karar Seti

- [x] Native Kotlin, **View sistemi** (Jetpack Compose değil) + `KeyEvent`/D-pad
      focus navigasyon iskeleti (`ui/nav/DpadFocusHelper.kt`)
- [x] Room veritabanı: `Kategori`, `Gorev`, `Oturum`, `TekrarKarti`, `IlerlemeKaydi`
      (`data/model`, `data/db`)
- [x] JSON tabanlı görev kütüphanesi formatı (`assets/gorevler.json`) + yükleyici
      (`data/GorevKutuphanesiYukleyici.kt`) — yeni görev eklemek JSON'a satır
      eklemekten ibaret
- [x] Tema sistemi: "Genç Araştırmacı" paleti (`res/values/colors.xml`,
      `themes.xml`) — kirli bordo-kızıl (ana vurgu) + soluk mor-eflatun (ikincil) +
      kirli haki/bej (nötr zemin) + kırık beyaz (kart yüzeyi); başlıklarda serif,
      gövdede sans-serif, istatistiklerde monospace
- [x] "Tez klasörü" kart görünümü + D-pad odak göstergesi (`kart_focus_selector.xml`)
- [x] Kategori listesini gösteren ilk `MainActivity` prototipi

## Bilinçli olarak esinlenilen ama karakter içermeyen noktalar

`kaomoji` reposundaki `claude/makise-kurisu-kaomoji-theme-i09nyt` branch'inin
VOIDLAB temasından **mimari desen** olarak esinlenildi (Room + JSON görev/müfredat
kütüphanesi + View/Compose ayrımı + palet-seviyesinde karakter esintisi + "hiçbir
ekranda isim/yüz/replik yok" ilkesi). Renk paleti, motifler ve ton tamamen farklı
ve özgün üretildi — plandaki 2. bölümde tarif edilen "Genç Araştırmacı" estetiği.

## Durum: Aşama 2 — "Şimdi Ne Yapsam" Motoru ve Görev Kütüphanesi

- [x] 2 girdi: süre (5/15/30/60 dk) ve enerji/mod (yüksek/düşük × sosyal/yalnız) —
      `OneriActivity`, D-pad ile seçilebilir liste halinde
- [x] Öneri algoritması (`engine/OneriMotoru.kt`): süre + enerji/mod + kategoriye göre
      filtreleme; ağırlıklandırılmış rastgele seçim — skor = ağırlık × çeşitlilik
      çarpanı (görev en son ne kadar süre önce yapıldıysa çarpan o kadar yüksek)
- [x] Görev havuzu genişletildi: fiziksel 16, sosyal 15, bilişsel 16, entelektüel 16,
      yaratıcı (opsiyonel 5. alan) 12 — toplam 75 görev, `assets/gorevler.json`
- [x] "Başka öner": gösterilen görevin ağırlığını azaltır (taban sınırla), motor
      zamanla aynı öneriyi daha az öne çıkarır — basit ağırlıklandırma öğrenmesi
- [x] Ana ekrandan genel "Şimdi Ne Yapsam" girişi ve kategoriye özel giriş — ikisi de
      aynı motoru, kategoriId ile veya kategorisiz çağırıyor

### Ton kararı

Plan taslağındaki tsundere/iğneleyici ton **kullanılmadı**. Tüm arayüz metinleri
(görev açıklamaları, düğme etiketleri, durum mesajları) resmi, akademik ve net bir
dille yazıldı; mizahi/ironik ifadeler yok.

## Sırada (Aşama 3)

Bilimsel takip katmanı: SM-2 aralıklı tekrar, streak/tutarlılık takibi, istatistik
ekranı, her görev sonunda "ne öğrendin" retrieval practice girişi.

## Not

Bu ortamda Google/AGP Maven depolarına ağ erişimi kısıtlı olduğu için modül bu
oturumda `gradlew` ile derlenip doğrulanamadı. Kod, kaomoji `:app` modülüyle aynı
Gradle/AGP/Kotlin sürümlerini kullanır; gerçek derleme GitHub Actions'ta (Aşama 5)
ya da Android Studio'su olan bir makinede doğrulanmalı.
