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

## Sırada (Aşama 2)

"Şimdi ne yapsam" öneri motoru: süre + enerji/mod girdisi, kategori/süre/çeşitlilik
bazlı seçim algoritması, her kategoride 15-20 görevlik gerçek görev havuzu,
"başka öner" tuşu.

## Not

Bu ortamda Google/AGP Maven depolarına ağ erişimi kısıtlı olduğu için modül bu
oturumda `gradlew` ile derlenip doğrulanamadı. Kod, kaomoji `:app` modülüyle aynı
Gradle/AGP/Kotlin sürümlerini kullanır; gerçek derleme GitHub Actions'ta (Aşama 5)
ya da Android Studio'su olan bir makinede doğrulanmalı.
