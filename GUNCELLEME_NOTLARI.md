# Bu güncellemede eklenenler

## 1. Müfredat (7 aylık, hesaplamalı nörobilim)
`app/src/main/assets/curriculum.json` yeniden üretildi — 30 birim (1 hafta = 1 birim),
441 görev, ~514 saat. Üretici: `tools/gen_curriculum_v2.py`.

## 2. Ana ekran widget'ı + kilit ekranı bildirimi
- `widget/MissionWidget.kt` — Glance tabanlı ana ekran widget'ı, "bugünün küçük
  görevi"ni gösterir, dokunarak tamamlanabilir.
- `widget/MissionNotifier.kt` — kilit ekranında görünen kalıcı bildirim (Android artık
  gerçek kilit ekranı widget'ı desteklemiyor, bu en yakın eşdeğeri).
- Android 13+'ta ilk açılışta bildirim izni istenecek.

## 3. AI transkripsiyon + anlatım analizi
- `ai/GroqClient.kt` — Groq Whisper (ses → yazı) ve Llama 3.3 70B (analiz) çağrıları.
- Anlatımlarım ekranında her kayıt için "Transkribe et" ve "Anlatımı analiz et" butonları.
- **Groq API anahtarı gerekiyor** — console.groq.com'dan ücretsiz alınabiliyor
  (FocusLock'ta kullandığın anahtarla aynısı işe yarar). Kartlar (Anki) ekranından
  ya da ilk transkripsiyon denemesinde girip kaydedebilirsin.
- Not: anahtar şu an düz SharedPreferences'ta duruyor (FocusLock'taki gibi şifreli
  değil) — cihaz paylaşımlıysa aklında olsun.

## 4. Otomatik soru üretimi + Anki dışa aktarma
- Çanta → Kartlar (Anki) ekranı: açık birimden 8 soru-cevap kartı otomatik üretir
  (Groq/Llama), kartları listeler, dokunarak çevirebilirsin.
- "Anki'ye Aktar" → Exports klasörüne sekme-ayraçlı .txt dosyası yazar.
  Anki'de: Dosya → İçe Aktar → alan ayracı: Tab.

## 5. Gelişmiş köprü grafiği
- `ui/BridgeGraphScreen.kt` — curriculum.json'daki `bridges` verisinden çıkarılmış
  dairesel düğüm grafiği. Bir konuya dokunursan ilgili köprüler öne çıkar.
- Çanta → Köprü Grafiği.

---

## Derleme hakkında dürüst not

Bu değişiklikleri sandbox ortamında (Android SDK / Gradle dağıtım sunucusuna ağ
erişimi olmadan) yazdım — gerçek bir derleme çalıştıramadım. JSON şemasını elle
doğruladım, parantez/parantez dengesini kontrol ettim, ama özellikle:

- Glance widget API'sinin tam import yolları (sürümler arası değişebiliyor)
- OkHttp multipart/JSON çağrılarının uçtan uca davranışı

test edilmedi. GitHub Actions'ta derleme hatası çıkarsa logu yapıştır, hızlıca
düzeltirim — FocusLock'ta olduğu gibi muhtemelen 1-2 küçük tur gerekebilir.
