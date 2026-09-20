#!/usr/bin/env python3
"""
BERIL — 2026-2027 Egitim Yili Muefredati (v2, genisletilmis)
Kaynak: beril-mufredat-2026-2027.pdf (v2) — 20 Eylul 2026 - ~20 Haziran 2027, 38 hafta (Hafta 0-37).
v1'den fark: gun bazli detay artik SADECE Donem 1'de degil, TUM YIL boyunca var
(Subat-Haziran icin de gercek gunluk mikro moduller, ayni PDF'in kendisinden).
Her gorev metninin basina gercek takvim tarihi eklenir (orn. "21 Eylul · ...").

Bu script app/src/main/assets/curriculum.json dosyasini komple yeniden uretir.
"""
import json
from datetime import date, timedelta

OUT_PATH = "app/src/main/assets/curriculum.json"

MONTH_TR = {1: "Ocak", 2: "Şubat", 3: "Mart", 4: "Nisan", 5: "Mayıs", 6: "Haziran",
            7: "Temmuz", 8: "Ağustos", 9: "Eylül", 10: "Ekim", 11: "Kasım", 12: "Aralık"}
DAY_OFFSET = {"Pzt": 0, "Sal": 1, "Çar": 2, "Per": 3, "Cum": 4, "Cmt": 5, "Paz": 6}
WEEKDAY_TR = {0: "Pzt", 1: "Sal", 2: "Çar", 3: "Per", 4: "Cum", 5: "Cmt", 6: "Paz"}

WEEK0_DATE = date(2026, 9, 20)   # Hafta 0 tek gunluk kurulum gunu (Pazar)
WEEK1_START = date(2026, 9, 21)  # Hafta 1 Pazartesi
# Yariyil tatili (~25 Ocak - 8 Subat 2027) Hafta 18 ile Hafta 19 arasinda haftalik
# akisi kesiyor — Hafta 19 numaralandirmada ardisik olsa da takvimde ardisik degil.
# Kaynak: "Hafta 19 — 9-14 Subat (kisa, donem basi — Sali basliyor)".
WEEK19_MONDAY = date(2027, 2, 8)


def week_start(n: int) -> date:
    if n == 0:
        return WEEK0_DATE
    if n <= 18:
        return WEEK1_START + timedelta(weeks=n - 1)
    return WEEK19_MONDAY + timedelta(weeks=n - 19)


def fmt(d: date) -> str:
    return f"{d.day} {MONTH_TR[d.month]}"


def dated(n: int, day_code: str, text: str) -> str:
    if n == 0:
        d = WEEK0_DATE
    else:
        d = week_start(n) + timedelta(days=DAY_OFFSET[day_code])
        got = WEEKDAY_TR[d.weekday()]
        assert got == day_code, f"Hafta {n} {day_code} gun kodu tutmuyor (hesaplanan: {got}, tarih: {d})"
    return f"{fmt(d)} · {text}"


def dated_range(n: int, day_from: str, day_to: str, text: str) -> str:
    ws = week_start(n)
    d1 = ws + timedelta(days=DAY_OFFSET[day_from])
    d2 = ws + timedelta(days=DAY_OFFSET[day_to])
    return f"{fmt(d1)}-{fmt(d2)} · {text}"


# ── Konu/gorev turu kisaltmalari (JSON'da s= subject kodu, k= kind kodu) ──
SUBJECTS = [
    {"c": "sys", "n": "Sistem Kurulumu", "e": "🗂️", "col": "#7A6A55"},
    {"c": "uwc", "n": "UWC Başvurusu", "e": "🌍", "col": "#6F2A36"},
    {"c": "fel", "n": "Felsefe Kulübü", "e": "🦉", "col": "#8073A5"},
    {"c": "p1", "n": "P1 — Hodgkin-Huxley", "e": "🧠", "col": "#6E2430"},
    {"c": "p2", "n": "P2 — Hopfield Network", "e": "🕸️", "col": "#524870"},
    {"c": "okul", "n": "Okul Dersleri", "e": "📚", "col": "#9B7D4B"},
    {"c": "jp", "n": "Japonca", "e": "🌸", "col": "#B99E6E"},
    {"c": "bb", "n": "Brain Bee", "e": "🥼", "col": "#5F5483"},
    {"c": "sat", "n": "SAT", "e": "🎓", "col": "#9C92B9"},
    {"c": "apcalc", "n": "AP Kalkülüs", "e": "📐", "col": "#79344B"},
    {"c": "apchem", "n": "AP Kimya", "e": "🧪", "col": "#A44666"},
    {"c": "apbio", "n": "AP Biyoloji", "e": "🧬", "col": "#8F633D"},
    {"c": "apexam", "n": "AP Sınav Haftası", "e": "📝", "col": "#66314A"},
    {"c": "yazproj", "n": "Yaz Projesi (P3?)", "e": "☀️", "col": "#7C8A4E"},
    {"c": "ozdeg", "n": "Öz-değerlendirme", "e": "🪞", "col": "#6D5F95"},
    {"c": "serbest", "n": "Serbest / Telafi", "e": "🌿", "col": "#B0A47E"},
]

KINDS = [
    {"c": "study", "n": "Öğren", "e": "📖"},
    {"c": "produce", "n": "Üret", "e": "🛠️"},
    {"c": "review", "n": "Tekrar", "e": "🔁"},
    {"c": "test", "n": "Sınav/Deneme", "e": "📋"},
    {"c": "reflect", "n": "Değerlendir", "e": "🪞"},
    {"c": "explain", "n": "Anlat/Sun", "e": "🎙️"},
    {"c": "rest", "n": "Dinlen/Telafi", "e": "🌿"},
]

# ── Haftalar: (hafta_no, tema_or_None, [(gun, konu, metin, dakika, tur), ...]) ──
# tema sadece ayin ilk haftasinda dolu (faz baslik/kicker'i icin).
WEEKS = [
(0, "Eylül · Kurulum ve Keşif", [
    ("Paz", "sys", "CollaNote'ta haftalık şablon sayfası oluştur; P1 için kaynak listesi başlat (NeuroMorpho.org, Allen Brain Atlas)", 45, "produce"),
    ("Paz", "uwc", "Türkiye Ulusal Komitesi'nden kesin son başvuru tarihini teyit et", 30, "study"),
    ("Paz", "ozdeg", "Video günlüğü #0 (hedefler, motivasyon)", 15, "reflect"),
]),
(1, None, [
    ("Pzt", "uwc", "\"Neden UWC, neden ben\" beyin fırtınası — serbest yazım", 60, "produce"),
    ("Pzt", "okul", "Haftanın konu planını çıkar, ödevleri listele", 30, "study"),
    ("Pzt", "apcalc", "Türev kurallarını (kendi sitenden) tekrar et", 30, "review"),
    ("Sal", "p1", "Cable theory teorik özeti + tek-kompartmanlı denklemi kağıtta türet", 75, "study"),
    ("Sal", "sat", "Reading & Writing diagnostic — 1. bölüm", 30, "test"),
    ("Sal", "bb", "Nöron/sinaps temelleri — okuma 1", 15, "study"),
    ("Çar", "okul", "Haftanın konusunu (fizik/biyoloji) pekiştir", 60, "review"),
    ("Çar", "apchem", "Madde ve ölçüm, birim dönüşümleri", 45, "study"),
    ("Çar", "jp", "N5 kelime seti 1", 15, "study"),
    ("Per", "p1", "Python'da HH denklemlerini (Na, K, sızıntı) kodlamaya başla", 75, "produce"),
    ("Per", "sat", "Reading & Writing diagnostic — 2. bölüm", 30, "test"),
    ("Per", "bb", "Nöron/sinaps temelleri — okuma 2", 20, "study"),
    ("Cum", "uwc", "Felsefe kulübü ilk toplantı materyali (okuma + tartışma soruları)", 60, "study"),
    ("Cum", "apcalc", "İlgili oranlar (related rates) — problem seti 1", 45, "review"),
    ("Cum", "okul", "Haftanın ödevlerini bitir", 20, "review"),
    ("Cmt", "sat", "Math diagnostic — tam bölüm", 90, "test"),
    ("Cmt", "apchem", "Atom altı parçacıklar, izotoplar", 45, "study"),
    ("Paz", "ozdeg", "Video günlüğü #1 + checkpoint", 30, "reflect"),
    ("Paz", "serbest", "Telafi/dinlenme", 90, "rest"),
]),
(2, None, [
    ("Pzt", "uwc", "Deneme taslağı — giriş paragrafı", 60, "produce"),
    ("Pzt", "okul", "Konu takibi, quiz hazırlığı", 30, "study"),
    ("Pzt", "apcalc", "İlgili oranlar — problem seti 2", 30, "review"),
    ("Sal", "p1", "HH modelini çalıştır, tek aksiyon potansiyeli simülasyonu + grafik", 75, "produce"),
    ("Sal", "sat", "Math diagnostic sonuçlarını analiz et, zayıf alan listesi çıkar", 30, "review"),
    ("Sal", "bb", "Aksiyon potansiyeli mekanizması — okuma 1", 15, "study"),
    ("Çar", "okul", "Haftalık konu tekrarı + kendi kendine mini quiz", 60, "review"),
    ("Çar", "apchem", "Elektron konfigürasyonu, kuantum sayıları", 45, "study"),
    ("Çar", "jp", "N5 kelime seti 2", 15, "study"),
    ("Per", "p1", "NeuroMorpho'dan morfoloji örneği indir, model parametreleriyle karşılaştır", 75, "study"),
    ("Per", "sat", "R&W zayıf alan review", 30, "review"),
    ("Per", "bb", "Aksiyon potansiyeli mekanizması — okuma 2", 20, "study"),
    ("Cum", "uwc", "Deneme taslağı — 2. paragraf", 60, "produce"),
    ("Cum", "apchem", "Periyodik eğilimler (iyonlaşma enerjisi, elektronegatiflik)", 45, "study"),
    ("Cum", "okul", "Haftanın ödevlerini bitir", 20, "review"),
    ("Cmt", "sat", "Tam pratik bölüm (R&W)", 90, "test"),
    ("Cmt", "apcalc", "Optimizasyon problemleri — giriş", 45, "study"),
    ("Paz", "ozdeg", "Video günlüğü #2 + checkpoint", 30, "reflect"),
    ("Paz", "serbest", "Telafi/dinlenme", 90, "rest"),
]),
(3, "Ekim · Derinleşme", [
    ("Pzt", "uwc", "Deneme v1'i baştan sona oku, geri bildirim için paylaşım planla", 60, "produce"),
    ("Pzt", "okul", "Konu takibi", 30, "study"),
    ("Pzt", "apcalc", "Optimizasyon problemleri — devam", 30, "review"),
    ("Sal", "p1", "Çok-kompartmanlı yapıya geçiş: dendrit-soma-akson segmentasyonu tasarımı", 75, "produce"),
    ("Sal", "sat", "Zayıf alan #1 üzerine odaklı çalışma", 30, "review"),
    ("Sal", "bb", "İyon kanalları — okuma 1", 15, "study"),
    ("Çar", "okul", "Konu tekrarı", 60, "review"),
    ("Çar", "apchem", "İyonik ve kovalent bağlar", 45, "study"),
    ("Çar", "jp", "N5 kelime seti 3", 15, "study"),
    ("Per", "p1", "Çok-kompartman modelini kodla, tek kompartmanla karşılaştırmalı test", 75, "produce"),
    ("Per", "sat", "Zayıf alan #2 üzerine odaklı çalışma", 30, "review"),
    ("Per", "bb", "İyon kanalları — okuma 2", 20, "study"),
    ("Cum", "uwc", "Felsefe kulübü 2. toplantı + notlarını dosyala", 60, "study"),
    ("Cum", "apcalc", "Eğri çizimi (1. ve 2. türev testleri)", 45, "review"),
    ("Cum", "okul", "Haftanın ödevlerini bitir", 20, "review"),
    ("Cmt", "sat", "Tam pratik bölüm (Math)", 90, "test"),
    ("Cmt", "apchem", "Molekül geometrisi (VSEPR)", 45, "study"),
    ("Paz", "ozdeg", "Video günlüğü #3 + checkpoint", 30, "reflect"),
    ("Paz", "serbest", "Telafi/dinlenme", 90, "rest"),
]),
(4, None, [
    ("Pzt", "uwc", "Geri bildirimlere göre deneme v2 revizyonu", 60, "produce"),
    ("Pzt", "okul", "Konu takibi", 30, "study"),
    ("Pzt", "apcalc", "Limit ve süreklilik (ileri)", 30, "study"),
    ("Sal", "p1", "Allen Brain Atlas'tan elektrofizyoloji verisi çek, parametreleri kalibre et", 75, "produce"),
    ("Sal", "sat", "Tam pratik bölüm (R&W, hızlanma amaçlı)", 30, "test"),
    ("Sal", "bb", "Sinaptik iletim — okuma 1", 15, "study"),
    ("Çar", "okul", "Konu tekrarı", 60, "review"),
    ("Çar", "apchem", "Mol kavramı, stokiyometri temelleri", 45, "study"),
    ("Çar", "jp", "N5 kelime seti 4", 15, "study"),
    ("Per", "p1", "Kalibrasyon sonuçlarını doğrula, sapmaları not al", 75, "review"),
    ("Per", "sat", "Tam pratik bölüm (Math, hızlanma amaçlı)", 30, "test"),
    ("Per", "bb", "Sinaptik iletim — okuma 2", 20, "study"),
    ("Cum", "uwc", "Felsefe kulübü 3. toplantı hazırlığı", 60, "study"),
    ("Cum", "apchem", "Kimyasal reaksiyon denklemleri, denkleştirme", 45, "study"),
    ("Cum", "okul", "Haftanın ödevlerini bitir", 20, "review"),
    ("Cmt", "sat", "Tam bölüm pratik (dönüşümlü)", 90, "test"),
    ("Cmt", "apcalc", "İntegral kavramına giriş (Riemann toplamları)", 45, "study"),
    ("Paz", "ozdeg", "Video günlüğü #4 + checkpoint", 30, "reflect"),
    ("Paz", "serbest", "Telafi/dinlenme", 90, "rest"),
]),
(5, None, [
    ("Pzt", "uwc", "Başvuru formunun geri kalan bölümleri (aktiviteler, referanslar)", 60, "produce"),
    ("Pzt", "okul", "Ara sınavlar yaklaşıyorsa buna ağırlık ver", 30, "review"),
    ("Pzt", "apcalc", "Belirsiz integral, temel kurallar", 30, "study"),
    ("Sal", "p1", "Kalibre edilmiş modelle uzun-süreli simülasyon, kararlılık testi", 75, "produce"),
    ("Sal", "sat", "Zayıf alan review (güncel)", 30, "review"),
    ("Sal", "bb", "Nöroanatomi — lob ve bölgeler, okuma 1", 15, "study"),
    ("Çar", "okul", "Konu tekrarı — ara sınav hazırlığı", 60, "review"),
    ("Çar", "apchem", "Gazlar — ideal gaz yasası", 45, "study"),
    ("Çar", "jp", "N5 kelime seti 5", 15, "study"),
    ("Per", "p1", "Sonuçları grafikle özetle, ilk taslak metin (yöntem bölümü)", 75, "produce"),
    ("Per", "sat", "Zayıf alan review (güncel)", 30, "review"),
    ("Per", "bb", "Nöroanatomi — lob ve bölgeler, okuma 2", 20, "study"),
    ("Cum", "uwc", "Felsefe kulübü 3. toplantı", 60, "study"),
    ("Cum", "apchem", "Çözeltiler, molarite", 45, "study"),
    ("Cum", "okul", "Haftanın ödevlerini bitir", 20, "review"),
    ("Cmt", "sat", "Tam deneme (tek oturum, R&W+Math)", 100, "test"),
    ("Cmt", "apcalc", "Analizin Temel Teoremi (FTC) — giriş", 45, "study"),
    ("Paz", "ozdeg", "Video günlüğü #5 + checkpoint", 30, "reflect"),
    ("Paz", "serbest", "Telafi/dinlenme", 90, "rest"),
]),
(6, None, [
    ("Pzt", "uwc", "Deneme v2'yi son haline getir", 60, "produce"),
    ("Pzt", "okul", "Konu takibi", 30, "study"),
    ("Pzt", "apcalc", "u-substitüsyon — giriş", 30, "study"),
    ("Sal", "p1", "Stokastik iyon kanalı gürültüsü için literatür okuması (Markov modeli)", 75, "study"),
    ("Sal", "sat", "Tam pratik bölüm", 30, "test"),
    ("Sal", "bb", "Duyu sistemleri giriş — okuma 1", 15, "study"),
    ("Çar", "okul", "Konu tekrarı / ara sınav hazırlığı", 60, "review"),
    ("Çar", "apchem", "Termokimya — entalpi", 45, "study"),
    ("Çar", "jp", "N5 tekrar (set 1-5)", 15, "review"),
    ("Per", "p1", "Stokastik gürültü modülünü koda ekleme (ilk deneme)", 75, "produce"),
    ("Per", "sat", "Zayıf alan review", 30, "review"),
    ("Per", "bb", "Duyu sistemleri giriş — okuma 2", 20, "study"),
    ("Cum", "uwc", "Felsefe kulübü 4. toplantı", 60, "study"),
    ("Cum", "apchem", "Termokimya — devam, örnekler", 45, "study"),
    ("Cum", "okul", "Haftanın ödevlerini bitir", 20, "review"),
    ("Cmt", "sat", "Zayıf alan review + kısa pratik", 90, "review"),
    ("Cmt", "apcalc", "u-substitüsyon — problem seti", 45, "review"),
    ("Paz", "ozdeg", "Video günlüğü #6 + checkpoint + Ekim ayı retrosu", 75, "reflect"),
    ("Paz", "serbest", "Telafi/dinlenme", 45, "rest"),
]),
(7, "Kasım · Tamamlama ve Sıkılaştırma", [
    ("Pzt", "uwc", "Başvuruyu gözden geçir ve gönder (gerçek son tarihe göre kayabilir)", 90, "produce"),
    ("Pzt", "apcalc", "u-substitüsyon — devam", 30, "review"),
    ("Sal", "p1", "Stokastik kanal gürültüsünü ikinci nörona da uygula", 75, "produce"),
    ("Sal", "sat", "Tam pratik bölüm", 30, "test"),
    ("Sal", "bb", "İşitme sistemi — okuma", 15, "study"),
    ("Çar", "okul", "Konu tekrarı", 60, "review"),
    ("Çar", "apchem", "Kimyasal kinetik — giriş", 45, "study"),
    ("Çar", "jp", "N5 kelime seti 6", 15, "study"),
    ("Per", "p1", "İki nöronu sinaptik olarak bağlama — bağlantı mimarisi tasarımı", 75, "produce"),
    ("Per", "sat", "Tam pratik bölüm", 30, "test"),
    ("Per", "bb", "Görme sistemi — okuma", 20, "study"),
    ("Cum", "fel", "Kulüp 5. toplantı (UWC gönderildi, artık kulübün bağımsız hedefi)", 60, "study"),
    ("Cum", "apcalc", "Alanlar (eğriler arası alan) — giriş", 45, "study"),
    ("Cum", "okul", "Haftanın ödevlerini bitir", 20, "review"),
    ("Cmt", "sat", "Tam deneme sınavı (tek oturum)", 100, "test"),
    ("Cmt", "apchem", "Denge (Le Chatelier) — giriş", 45, "study"),
    ("Paz", "ozdeg", "Video günlüğü #7 + checkpoint", 30, "reflect"),
    ("Paz", "serbest", "Telafi/dinlenme", 90, "rest"),
]),
(8, None, [
    ("Pzt", "uwc", "Mülakat olasılığına karşı hazırlık notları", 45, "study"),
    ("Pzt", "okul", "Sınav odaklı yoğun tekrar", 60, "review"),
    ("Sal", "okul", "Sınav odaklı yoğun tekrar", 60, "review"),
    ("Sal", "sat", "Hafif: 1 bölüm pratik", 30, "review"),
    ("Çar", "okul", "Sınav odaklı yoğun tekrar", 75, "review"),
    ("Çar", "jp", "N5 seti (bu hafta esnek, atlanabilir)", 15, "study"),
    ("Per", "okul", "Sınav odaklı yoğun tekrar", 60, "review"),
    ("Per", "p1", "Bu hafta hafif: sinaptik bağlantıyı kodla", 45, "produce"),
    ("Cum", "fel", "Kulüp toplantısı (kısa tutulabilir)", 30, "study"),
    ("Cum", "bb", "Hafıza sistemleri — okuma", 20, "study"),
    ("Cmt", "sat", "Hafif: 1 bölüm pratik", 60, "review"),
    ("Cmt", "apchem", "Hafif: kendi seçtiğin zayıf konu (Kimya/Kalkülüs dönüşümlü)", 45, "review"),
    ("Paz", "ozdeg", "Video günlüğü #8 + checkpoint (sınav haftası nasıl geçti?)", 30, "reflect"),
    ("Paz", "serbest", "Telafi/dinlenme", 60, "rest"),
]),
(9, None, [
    ("Pzt", "uwc", "Kulüp için yeni dönem içeriği planlama", 60, "study"),
    ("Pzt", "okul", "Konu takibi", 30, "study"),
    ("Pzt", "apcalc", "Alanlar — problem seti", 30, "review"),
    ("Sal", "p1", "İki nöron simülasyonunda faz kilitleme/senkronizasyon ölçümü — ilk analiz", 75, "produce"),
    ("Sal", "sat", "Tam pratik bölüm", 30, "test"),
    ("Sal", "bb", "Nörolojik hastalıklara giriş — okuma 1", 15, "study"),
    ("Çar", "okul", "Konu tekrarı", 60, "review"),
    ("Çar", "apchem", "Denge — Le Chatelier, devam", 45, "study"),
    ("Çar", "jp", "N5 kelime seti 7", 15, "study"),
    ("Per", "p1", "Cross-correlation analizi, parametre taraması", 75, "produce"),
    ("Per", "sat", "Tam pratik bölüm", 30, "test"),
    ("Per", "bb", "Nörolojik hastalıklara giriş — okuma 2", 20, "study"),
    ("Cum", "fel", "Kulüp toplantısı", 60, "study"),
    ("Cum", "apcalc", "Analizin Temel Teoremi — problem seti", 45, "review"),
    ("Cum", "okul", "Haftanın ödevlerini bitir", 20, "review"),
    ("Cmt", "sat", "Tam pratik test #2", 100, "test"),
    ("Cmt", "apchem", "Kimyasal kinetik — problem seti", 45, "review"),
    ("Paz", "ozdeg", "Video günlüğü #9 + checkpoint", 30, "reflect"),
    ("Paz", "serbest", "Telafi/dinlenme", 90, "rest"),
]),
(10, None, [
    ("Pzt", "uwc", "Kulüp çıktısını yazılı hale getir (portföy için)", 60, "produce"),
    ("Pzt", "okul", "Konu takibi", 30, "study"),
    ("Pzt", "apcalc", "Genel tekrar — türev+integral karışık problemler", 30, "review"),
    ("Sal", "p1", "Senkronizasyon sonuçlarını grafikle özetle, sonuçlar bölümü taslağı", 75, "produce"),
    ("Sal", "sat", "Zayıf alan review (Kasım deneme sonucuna göre)", 30, "review"),
    ("Sal", "bb", "Bilişsel nörobilim giriş — okuma 1", 15, "study"),
    ("Çar", "okul", "Konu tekrarı", 60, "review"),
    ("Çar", "apchem", "Genel tekrar — bağlar+stokiyometri karışık problemler", 45, "review"),
    ("Çar", "jp", "N5 kelime seti 8", 15, "study"),
    ("Per", "p1", "Rapor/yazım — giriş ve yöntem bölümlerini birleştir", 75, "produce"),
    ("Per", "sat", "Zayıf alan review", 30, "review"),
    ("Per", "bb", "Bilişsel nörobilim giriş — okuma 2", 20, "study"),
    ("Cum", "fel", "Kulüp toplantısı", 60, "study"),
    ("Cum", "apchem", "Genel tekrar — devam", 45, "review"),
    ("Cum", "okul", "Haftanın ödevlerini bitir", 20, "review"),
    ("Cmt", "sat", "Tam deneme sınavı (tek oturum)", 100, "test"),
    ("Cmt", "apcalc", "Genel tekrar — devam", 45, "review"),
    ("Paz", "ozdeg", "Video günlüğü #10 + checkpoint + Kasım ayı retrosu", 75, "reflect"),
    ("Paz", "serbest", "Telafi/dinlenme", 45, "rest"),
]),
(11, "Aralık · Bitirme, Toparlama, Dinlenme", [
    ("Pzt", "uwc", "Kulüp toplantısı + dönem sonu özet raporu başlangıcı", 60, "study"),
    ("Pzt", "okul", "Konu takibi", 30, "study"),
    ("Pzt", "apcalc", "Hacim (disk yöntemi) — giriş", 30, "study"),
    ("Sal", "p1", "Raporun tartışma/sonuç bölümü taslağı", 75, "produce"),
    ("Sal", "sat", "Tam pratik bölüm", 30, "test"),
    ("Sal", "bb", "Genel tekrar seti 1", 15, "review"),
    ("Çar", "okul", "Konu tekrarı", 60, "review"),
    ("Çar", "apchem", "Asit-baz kimyası — giriş", 45, "study"),
    ("Çar", "jp", "N5 kelime seti 9", 15, "study"),
    ("Per", "p1", "Destek Eğitim Odası'na sunum için slayt/görsel hazırlığı", 75, "produce"),
    ("Per", "sat", "Tam pratik bölüm", 30, "test"),
    ("Per", "bb", "Genel tekrar seti 2", 20, "review"),
    ("Cum", "fel", "Kulüp toplantısı", 60, "study"),
    ("Cum", "apchem", "Asit-baz — pH hesapları", 45, "review"),
    ("Cum", "okul", "Haftanın ödevlerini bitir", 20, "review"),
    ("Cmt", "sat", "Tam pratik test #3", 100, "test"),
    ("Cmt", "apcalc", "Hacim — disk/shell problem seti", 45, "review"),
    ("Paz", "ozdeg", "Video günlüğü #11 + checkpoint", 30, "reflect"),
    ("Paz", "serbest", "Telafi/dinlenme", 90, "rest"),
]),
(12, None, [
    ("Pzt", "uwc", "Kulüp özet raporunu bitir", 60, "produce"),
    ("Pzt", "okul", "Konu takibi", 30, "study"),
    ("Pzt", "apcalc", "Diferansiyel denklemlere giriş", 30, "study"),
    ("Sal", "p1", "Rapor/sunum revizyonu — geri bildirim al", 75, "review"),
    ("Sal", "sat", "Tam pratik bölüm", 30, "test"),
    ("Sal", "bb", "Genel tekrar seti 3", 15, "review"),
    ("Çar", "okul", "Konu tekrarı", 60, "review"),
    ("Çar", "apchem", "Genel tekrar — asit-baz+denge karışık", 45, "review"),
    ("Çar", "jp", "N5 kelime seti 10", 15, "study"),
    ("Per", "p1", "Geri bildirime göre son düzeltmeler", 75, "produce"),
    ("Per", "sat", "Zayıf alan son review", 30, "review"),
    ("Per", "bb", "Genel tekrar seti 4", 20, "review"),
    ("Cum", "fel", "Kulüp toplantısı", 60, "study"),
    ("Cum", "apcalc", "Genel tekrar — tüm dönem karışık problemler", 45, "review"),
    ("Cum", "okul", "Haftanın ödevlerini bitir", 20, "review"),
    ("Cmt", "sat", "Tam deneme sınavı (tek oturum)", 100, "test"),
    ("Cmt", "apchem", "Genel tekrar — tüm dönem karışık problemler", 45, "review"),
    ("Paz", "ozdeg", "Video günlüğü #12 + checkpoint", 30, "reflect"),
    ("Paz", "serbest", "Telafi/dinlenme", 90, "rest"),
]),
(13, None, [
    ("Pzt", "p1", "Sunum/rapor final teslim (Destek Eğitim Odası)", 90, "produce"),
    ("Pzt", "okul", "Sınav tekrarı", 30, "review"),
    ("Sal", "okul", "Sınav tekrarı", 75, "review"),
    ("Sal", "sat", "Hafif: 1 bölüm pratik", 30, "review"),
    ("Çar", "okul", "Sınav tekrarı", 75, "review"),
    ("Çar", "jp", "N5 genel tekrar", 15, "review"),
    ("Per", "okul", "Sınav tekrarı", 75, "review"),
    ("Per", "apchem", "Hafif: termokimya tekrar", 30, "review"),
    ("Cum", "okul", "Sınav tekrarı", 60, "review"),
    ("Cum", "apcalc", "Hafif: FTC tekrar", 30, "review"),
    ("Cmt", "serbest", "Bu hafta okul öncelikli — kaçan görevleri buraya taşı", 60, "rest"),
    ("Paz", "ozdeg", "Video günlüğü #13 + checkpoint", 30, "reflect"),
    ("Paz", "serbest", "Dinlenme", 60, "rest"),
]),
(14, None, [
    ("Pzt", "serbest", "Mülakat çağrısı geldiyse hazırlık; gelmediyse tam dinlenme (0-45 dk)", 20, "rest"),
    ("Sal", "bb", "Ocak dönemine hazırlık: konu haritası çıkar", 30, "study"),
    ("Çar", "serbest", "İstersen tamamen boş bırak", 0, "rest"),
    ("Per", "p2", "\"İleri adımlar\" notu — Ocak'ta P2'ye nasıl başlanacak", 45, "study"),
    ("Cum", "apcalc", "Hafif: zayıf konu seçip 1 tekrar", 30, "review"),
    ("Cmt", "apchem", "Hafif: zayıf konu seçip 1 tekrar", 30, "review"),
    ("Paz", "ozdeg", "Video günlüğü #14 + checkpoint", 30, "reflect"),
]),
(15, None, [
    ("Pzt", "serbest", "Dinlenme", 0, "rest"),
    ("Sal", "apbio", "Ocak'ta başlayacak rotasyon için Campbell'dan hücre biyolojisi göz gezdirme", 30, "study"),
    ("Çar", "serbest", "Tamamen boş", 0, "rest"),
    ("Per", "ozdeg", "Yıl ortası mini-check: video günlüğü #15 + checkpoint", 45, "reflect"),
]),
(16, "Ocak · Dönem 1 Kapanışı + AP Biyoloji'nin Devreye Girişi", [
    ("Pzt", "uwc", "Kulübün ikinci yarıyıl planını taslakla", 60, "study"),
    ("Pzt", "okul", "Konu takibi", 30, "study"),
    ("Pzt", "apcalc", "Diferansiyel denklemler — problem seti", 30, "review"),
    ("Sal", "p2", "P1'i kapalı dosya olarak arşivle; P2 (Hopfield network) için literatür taraması", 75, "study"),
    ("Sal", "sat", "Tam pratik bölüm", 30, "test"),
    ("Sal", "bb", "Genel tekrar seti 5", 15, "review"),
    ("Çar", "okul", "Konu tekrarı", 60, "review"),
    ("Çar", "apchem", "Genel tekrar — dönem 1 kapsamlı", 45, "review"),
    ("Çar", "jp", "N5 kelime seti 11", 15, "study"),
    ("Per", "p2", "Hopfield network'ün matematiksel temeli (enerji fonksiyonu, ağırlık matrisi)", 75, "study"),
    ("Per", "sat", "Tam pratik bölüm", 30, "test"),
    ("Per", "bb", "Genel tekrar seti 6", 20, "review"),
    ("Cum", "fel", "Kulüp toplantısı", 60, "study"),
    ("Cum", "apbio", "Hücre biyolojisi — zar yapısı, taşınma mekanizmaları (Campbell)", 45, "study"),
    ("Cum", "okul", "Haftanın ödevlerini bitir", 20, "review"),
    ("Cmt", "sat", "Tam deneme sınavı #4", 100, "test"),
    ("Cmt", "apbio", "Hücre biyolojisi — devam, organeller", 45, "study"),
    ("Paz", "ozdeg", "Video günlüğü #16 + checkpoint", 30, "reflect"),
    ("Paz", "serbest", "Telafi/dinlenme", 90, "rest"),
]),
(17, None, [
    ("Pzt", "uwc", "Kulüp toplantısı", 45, "study"),
    ("Pzt", "okul", "Sınav tekrarı", 45, "review"),
    ("Sal", "p2", "Hopfield network'ü Python'da kodlamaya başla (küçük örnek desenle)", 75, "produce"),
    ("Sal", "okul", "Sınav tekrarı", 30, "review"),
    ("Çar", "okul", "Sınav tekrarı — yoğun", 75, "review"),
    ("Çar", "jp", "N5 kelime seti 12", 15, "study"),
    ("Per", "okul", "Sınav tekrarı — yoğun", 75, "review"),
    ("Per", "bb", "Genel tekrar seti 7", 20, "review"),
    ("Cum", "okul", "Sınav tekrarı", 60, "review"),
    ("Cum", "apbio", "Hücre solunumu — giriş", 30, "study"),
    ("Cmt", "sat", "Zayıf alan son review + kısa pratik", 90, "review"),
    ("Cmt", "apcalc", "Haftalık dönüşüm: zayıf konu tekrarı (Kalkülüs/Kimya)", 30, "review"),
    ("Paz", "ozdeg", "Video günlüğü #17 + checkpoint", 30, "reflect"),
    ("Paz", "serbest", "Telafi/dinlenme", 60, "rest"),
]),
(18, None, [
    ("Pzt", "okul", "Sınav tekrarı", 90, "review"),
    ("Sal", "okul", "Sınav tekrarı", 90, "review"),
    ("Çar", "okul", "Sınav tekrarı", 90, "review"),
    ("Çar", "jp", "N5 genel tekrar (1-12)", 15, "review"),
    ("Per", "okul", "Sınav tekrarı", 75, "review"),
    ("Cum", "serbest", "Bu hafta tamamen okul öncelikli", 30, "rest"),
    ("Cmt", "serbest", "Sınavlar bitince dinlen", 0, "rest"),
    ("Paz", "ozdeg", "(karne sonrası) Dönem 1 büyük retrospektifi + Dönem 2'yi bu detayla yeniden yazma oturumu", 120, "reflect"),
]),
(19, "Şubat", [
    ("Sal", "uwc", "Kulüp dönem başı toplantısı — yeni tema, yeni üyeler", 45, "study"),
    ("Sal", "p2", "Hopfield network kodlamasına devam (öğrenme kuralı — Hebbian)", 75, "produce"),
    ("Sal", "bb", "Round hazırlığı — genel tekrar seti 8", 20, "review"),
    ("Çar", "okul", "Konu tekrarı", 60, "review"),
    ("Çar", "apchem", "Elektrokimya — galvanik hücreler, hücre potansiyeli", 45, "study"),
    ("Çar", "jp", "N5 kelime seti 13", 15, "study"),
    ("Per", "p2", "Kapasite testi — kaç desen hatasız hatırlanıyor", 75, "review"),
    ("Per", "sat", "Mart sınavına hazırlık — zayıf alan review", 30, "review"),
    ("Per", "bb", "Round hazırlığı — genel tekrar seti 9", 20, "review"),
    ("Cum", "uwc", "Kulüp toplantısı", 60, "study"),
    ("Cum", "apbio", "Genetik giriş — Mendel kalıtım kuralları", 45, "study"),
    ("Cum", "okul", "Haftanın ödevlerini bitir", 20, "review"),
    ("Cmt", "sat", "Tam deneme sınavı", 100, "test"),
    ("Cmt", "apcalc", "Seriler/diziler — yakınsaklık kavramına giriş", 45, "study"),
    ("Paz", "ozdeg", "Video günlüğü #19 + checkpoint", 30, "reflect"),
    ("Paz", "serbest", "Telafi/dinlenme", 90, "rest"),
]),
(20, None, [
    ("Pzt", "uwc", "Kulüp toplantısı", 60, "study"),
    ("Pzt", "okul", "Konu takibi", 30, "study"),
    ("Pzt", "apcalc", "Yakınsaklık testleri (oran testi, karşılaştırma testi)", 30, "study"),
    ("Sal", "p2", "Gürültülü/eksik desenlerle geri çağırma testi", 75, "review"),
    ("Sal", "sat", "Tam pratik bölüm", 30, "test"),
    ("Sal", "bb", "Round hazırlığı — genel tekrar seti 10", 15, "review"),
    ("Çar", "okul", "Konu tekrarı", 60, "review"),
    ("Çar", "apchem", "Elektrokimya — pil hesapları, Faraday yasası", 45, "study"),
    ("Çar", "jp", "N5 kelime seti 14", 15, "study"),
    ("Per", "p2", "Test sonuçlarının analizi, grafik özet", 75, "review"),
    ("Per", "sat", "Tam pratik bölüm", 30, "test"),
    ("Per", "bb", "Round hazırlığı — genel tekrar seti 11", 20, "review"),
    ("Cum", "fel", "Kulüp toplantısı", 60, "study"),
    ("Cum", "apbio", "Genetik — kalıtım problemleri, Punnett kareleri", 45, "review"),
    ("Cum", "okul", "Haftanın ödevlerini bitir", 20, "review"),
    ("Cmt", "sat", "Tam deneme sınavı", 100, "test"),
    ("Cmt", "apchem", "Elektrokimya problem seti", 45, "review"),
    ("Paz", "ozdeg", "Video günlüğü #20 + checkpoint", 30, "reflect"),
    ("Paz", "serbest", "Telafi/dinlenme", 90, "rest"),
]),
(21, None, [
    ("Pzt", "uwc", "Kulüp toplantısı", 60, "study"),
    ("Pzt", "okul", "Konu takibi", 30, "study"),
    ("Pzt", "apcalc", "Taylor/Maclaurin serilerine giriş", 30, "study"),
    ("Sal", "p2", "Rapor taslağı — giriş ve yöntem", 75, "produce"),
    ("Sal", "sat", "Tam pratik bölüm", 30, "test"),
    ("Sal", "bb", "Round günü / son hazırlık", 20, "review"),
    ("Çar", "okul", "Konu tekrarı", 60, "review"),
    ("Çar", "apchem", "Nükleer kimya — bozunma türleri, yarı ömür", 45, "study"),
    ("Çar", "jp", "N5 kelime seti 15", 15, "study"),
    ("Per", "p2", "Rapor — sonuçlar bölümü", 75, "produce"),
    ("Per", "sat", "Tam pratik bölüm", 30, "test"),
    ("Per", "apbio", "(Brain Bee bittiyse buradan itibaren) hücre bölünmesi — mitoz/mayoz", 20, "study"),
    ("Cum", "fel", "Kulüp toplantısı", 60, "study"),
    ("Cum", "apbio", "Moleküler genetik — DNA replikasyonu", 45, "study"),
    ("Cum", "okul", "Haftanın ödevlerini bitir", 20, "review"),
    ("Cmt", "sat", "Tam deneme — Mart sınavına son prova", 100, "test"),
    ("Cmt", "apcalc", "Seri yakınsaklık — problem seti", 45, "review"),
    ("Paz", "ozdeg", "Video günlüğü #21 + checkpoint + Şubat ayı retrosu", 75, "reflect"),
    ("Paz", "serbest", "Telafi/dinlenme", 45, "rest"),
]),
(22, "Mart", [
    ("Pzt", "uwc", "Kulüp toplantısı", 60, "study"),
    ("Pzt", "okul", "Konu takibi", 30, "study"),
    ("Pzt", "apcalc", "Parametrik denklemler — türev, eğim", 30, "study"),
    ("Sal", "p2", "Rapor — tartışma bölümü", 75, "produce"),
    ("Sal", "sat", "Sınav haftası — hafif review", 30, "review"),
    ("Sal", "apbio", "Gen ifadesi — transkripsiyon", 20, "study"),
    ("Çar", "okul", "Konu tekrarı", 60, "review"),
    ("Çar", "apchem", "Organik kimya — fonksiyonel gruplar, isimlendirme", 45, "study"),
    ("Çar", "jp", "N5 kelime seti 16", 15, "study"),
    ("Per", "p2", "Sunum için slayt/görsel hazırlığı", 75, "produce"),
    ("Per", "sat", "Hafif review (sınav bu hafta)", 30, "review"),
    ("Per", "apbio", "Gen ifadesi — translasyon", 20, "study"),
    ("Cum", "fel", "Kulüp toplantısı", 60, "study"),
    ("Cum", "apbio", "Gen regülasyonu — operonlar (lac operon)", 45, "study"),
    ("Cum", "okul", "Haftanın ödevlerini bitir", 20, "review"),
    ("Cmt", "sat", "Gerçek sınav günü (varsayılan tarih — kendi kayıt tarihine göre kayabilir)", 0, "test"),
    ("Cmt", "apcalc", "Polar koordinatlar — alan, türev", 45, "study"),
    ("Paz", "ozdeg", "Video günlüğü #22 + checkpoint (sınav sonrası dinlenme öncelikli)", 30, "reflect"),
    ("Paz", "serbest", "Dinlenme (sınav sonrası)", 90, "rest"),
]),
(23, None, [
    ("Pzt", "uwc", "Kulüp toplantısı", 60, "study"),
    ("Pzt", "okul", "Konu takibi", 30, "study"),
    ("Pzt", "apcalc", "Vektör değerli fonksiyonlar — hız/ivme", 30, "study"),
    ("Sal", "p2", "Rapor son hâli — giriş/yöntem/sonuç birleştirme", 75, "produce"),
    ("Sal", "apchem", "Termodinamik — entropi", 30, "study"),
    ("Sal", "apbio", "Evrim — doğal seçilim mekanizmaları", 20, "study"),
    ("Çar", "okul", "Konu tekrarı", 60, "review"),
    ("Çar", "apchem", "Termodinamik — Gibbs serbest enerji", 45, "study"),
    ("Çar", "jp", "N5 kelime seti 17", 15, "study"),
    ("Per", "p2", "Danışman/öğretmenden geri bildirim al", 75, "review"),
    ("Per", "apchem", "Termodinamik problem seti", 30, "review"),
    ("Per", "apbio", "Evrim — popülasyon genetiği (Hardy-Weinberg)", 20, "study"),
    ("Cum", "fel", "Kulüp toplantısı", 60, "study"),
    ("Cum", "apbio", "Evrim — tür oluşumu (speciation)", 45, "study"),
    ("Cum", "okul", "Haftanın ödevlerini bitir", 20, "review"),
    ("Cmt", "apcalc", "Problem seti — parametrik/polar/vektör karışık", 60, "review"),
    ("Cmt", "apchem", "Problem seti — termodinamik karışık", 30, "review"),
    ("Paz", "ozdeg", "Video günlüğü #23 + checkpoint", 30, "reflect"),
    ("Paz", "serbest", "Telafi/dinlenme", 90, "rest"),
]),
(24, None, [
    ("Pzt", "uwc", "Kulüp toplantısı", 60, "study"),
    ("Pzt", "okul", "Konu takibi", 30, "study"),
    ("Pzt", "apcalc", "Euler yöntemi, eğim alanları", 30, "study"),
    ("Sal", "p2", "Geri bildirime göre revizyon", 75, "produce"),
    ("Sal", "apchem", "Asit-baz — tampon çözeltiler (buffer)", 30, "study"),
    ("Sal", "apbio", "Ekoloji giriş — popülasyon ekolojisi", 20, "study"),
    ("Çar", "okul", "Konu tekrarı", 60, "review"),
    ("Çar", "apchem", "Asit-baz — titrasyon eğrileri", 45, "study"),
    ("Çar", "jp", "N5 kelime seti 18", 15, "study"),
    ("Per", "p2", "Rapor/sunum final teslim (Destek Eğitim Odası)", 75, "produce"),
    ("Per", "apchem", "Titrasyon problem seti", 30, "review"),
    ("Per", "apbio", "Ekoloji — topluluk etkileşimleri (predasyon, rekabet)", 20, "study"),
    ("Cum", "fel", "Kulüp toplantısı", 60, "study"),
    ("Cum", "apbio", "Ekoloji — ekosistem, biyojeokimyasal döngüler", 45, "study"),
    ("Cum", "okul", "Haftanın ödevlerini bitir", 20, "review"),
    ("Cmt", "apcalc", "Lojistik büyüme modelleri", 45, "study"),
    ("Cmt", "apbio", "Genel tekrar — genetik+hücre bölünmesi", 45, "review"),
    ("Paz", "ozdeg", "Video günlüğü #24 + checkpoint + Mart ayı retrosu", 75, "reflect"),
    ("Paz", "serbest", "Telafi/dinlenme", 45, "rest"),
]),
(25, None, [
    ("Pzt", "uwc", "Kulüp toplantısı", 60, "study"),
    ("Pzt", "okul", "Konu takibi — ikinci ara sınavlar yaklaşıyorsa ağırlık ver", 30, "review"),
    ("Pzt", "apcalc", "Genel tekrar — AB konuları (türev/integral) cumulative", 30, "review"),
    ("Sal", "yazproj", "P2 sonrası ne yapılacak (P3?) — literatür/ilgi taraması", 75, "study"),
    ("Sal", "apchem", "Kinetik — hız yasaları, tepkime mekanizmaları", 30, "study"),
    ("Sal", "apbio", "Ekoloji — enerji akışı, besin ağları", 20, "study"),
    ("Çar", "okul", "Konu tekrarı", 60, "review"),
    ("Çar", "apchem", "Denge — Ksp, çözünürlük dengeleri", 45, "study"),
    ("Çar", "jp", "N5 kelime seti 19", 15, "study"),
    ("Per", "yazproj", "Proje fikrini somutlaştır, kapsam belirle", 75, "produce"),
    ("Per", "apchem", "Ksp problem seti", 30, "review"),
    ("Per", "apbio", "Biyoteknoloji — PCR, jel elektroforez, CRISPR temelleri", 20, "study"),
    ("Cum", "fel", "Kulüp toplantısı", 60, "study"),
    ("Cum", "apbio", "Genel tekrar — evrim+ekoloji cumulative", 45, "review"),
    ("Cum", "okul", "Haftanın ödevlerini bitir", 20, "review"),
    ("Cmt", "apcalc", "Tam pratik sınav — MCQ+FRQ (BC formatında)", 90, "test"),
    ("Cmt", "apchem", "Genel tekrar — bağlar+gazlar+çözeltiler cumulative", 45, "review"),
    ("Paz", "ozdeg", "Video günlüğü #25 + checkpoint", 30, "reflect"),
    ("Paz", "serbest", "Telafi/dinlenme", 90, "rest"),
]),
(26, "Nisan · AP Sınavlarına Geri Sayım", [
    ("Pzt", "uwc", "Kulüp toplantısı", 60, "study"),
    ("Pzt", "okul", "Konu takibi", 30, "study"),
    ("Pzt", "apcalc", "Tam pratik sınav sonuçlarını analiz et, zayıf alan listesi", 30, "review"),
    ("Sal", "yazproj", "Proje planını yaz (hedef, üretim adımları)", 75, "produce"),
    ("Sal", "apchem", "Genel tekrar — termodinamik+kinetik+denge cumulative", 30, "review"),
    ("Sal", "apbio", "Genel tekrar — gen ifadesi+biyoteknoloji cumulative", 20, "review"),
    ("Çar", "okul", "Konu tekrarı / ikinci ara sınav hazırlığı", 60, "review"),
    ("Çar", "apchem", "Tam pratik sınav — MCQ+FRQ", 60, "test"),
    ("Çar", "jp", "N5 kelime seti 20", 15, "study"),
    ("Per", "yazproj", "İlk teknik adım (literatür/kod iskeleti)", 60, "produce"),
    ("Per", "apchem", "Pratik sınav sonuçlarını analiz et", 30, "review"),
    ("Per", "apbio", "Tam pratik sınav — MCQ+FRQ", 45, "test"),
    ("Cum", "fel", "Kulüp toplantısı", 60, "study"),
    ("Cum", "apbio", "Pratik sınav sonuçlarını analiz et, zayıf alan listesi", 45, "review"),
    ("Cum", "okul", "Haftanın ödevlerini bitir", 20, "review"),
    ("Cmt", "apcalc", "Zayıf alan #1 üzerine odaklı çalışma", 60, "review"),
    ("Cmt", "apchem", "Zayıf alan #1 üzerine odaklı çalışma", 45, "review"),
    ("Paz", "ozdeg", "Video günlüğü #26 + checkpoint", 30, "reflect"),
    ("Paz", "serbest", "Telafi/dinlenme", 90, "rest"),
]),
(27, None, [
    ("Pzt", "uwc", "Kulüp toplantısı", 60, "study"),
    ("Pzt", "okul", "Ara sınav hazırlığı", 30, "review"),
    ("Pzt", "apcalc", "Zayıf alan #2", 30, "review"),
    ("Sal", "yazproj", "Devam", 60, "produce"),
    ("Sal", "apchem", "Zayıf alan #2", 30, "review"),
    ("Sal", "apbio", "Zayıf alan #1", 20, "review"),
    ("Çar", "okul", "Ara sınav hazırlığı — yoğun", 75, "review"),
    ("Çar", "apchem", "FRQ (açık uçlu soru) pratik seti 1", 45, "review"),
    ("Çar", "jp", "N5 genel tekrar (11-20)", 15, "review"),
    ("Per", "okul", "Ara sınav hazırlığı — yoğun", 60, "review"),
    ("Per", "apchem", "FRQ pratik seti 2", 30, "review"),
    ("Per", "apbio", "Zayıf alan #2", 20, "review"),
    ("Cum", "fel", "Kulüp toplantısı", 45, "study"),
    ("Cum", "apbio", "FRQ (açık uçlu soru) pratik seti 1", 45, "review"),
    ("Cum", "okul", "Ara sınav hazırlığı", 30, "review"),
    ("Cmt", "apcalc", "FRQ pratik seti 1", 60, "review"),
    ("Cmt", "apbio", "FRQ pratik seti 2", 45, "review"),
    ("Paz", "ozdeg", "Video günlüğü #27 + checkpoint", 30, "reflect"),
    ("Paz", "serbest", "Telafi/dinlenme", 90, "rest"),
]),
(28, None, [
    ("Pzt", "okul", "Sınav tekrarı", 75, "review"),
    ("Pzt", "apcalc", "Hafif: FRQ pratik seti 2", 45, "review"),
    ("Sal", "okul", "Sınav tekrarı", 75, "review"),
    ("Sal", "apchem", "Hafif: FRQ pratik seti 3", 30, "review"),
    ("Çar", "okul", "Sınav tekrarı", 75, "review"),
    ("Çar", "jp", "N5 tekrar", 15, "review"),
    ("Per", "okul", "Sınav tekrarı", 60, "review"),
    ("Per", "apbio", "Hafif: FRQ pratik seti 3", 30, "review"),
    ("Cum", "uwc", "Kulüp toplantısı (kısa)", 30, "study"),
    ("Cum", "okul", "Sınav tekrarı", 45, "review"),
    ("Cmt", "serbest", "Bu hafta okul öncelikli — kaçan görevleri buraya taşı", 60, "rest"),
    ("Paz", "ozdeg", "Video günlüğü #28 + checkpoint", 30, "reflect"),
    ("Paz", "serbest", "Telafi/dinlenme", 60, "rest"),
]),
(29, None, [
    ("Pzt", "uwc", "Kulüp toplantısı", 45, "study"),
    ("Pzt", "okul", "Konu takibi", 30, "study"),
    ("Pzt", "apcalc", "Tam pratik sınav #2 — MCQ+FRQ", 60, "test"),
    ("Sal", "yazproj", "Devam", 60, "produce"),
    ("Sal", "apchem", "Tam pratik sınav #2 — MCQ+FRQ", 60, "test"),
    ("Çar", "okul", "Konu tekrarı", 60, "review"),
    ("Çar", "apchem", "Pratik sınav #2 sonuçlarını analiz et", 30, "review"),
    ("Çar", "jp", "N5 tekrar", 15, "review"),
    ("Per", "yazproj", "Devam", 45, "produce"),
    ("Per", "apbio", "Tam pratik sınav #2 — MCQ+FRQ", 60, "test"),
    ("Cum", "fel", "Kulüp toplantısı", 45, "study"),
    ("Cum", "apbio", "Pratik sınav #2 sonuçlarını analiz et", 30, "review"),
    ("Cum", "okul", "Haftanın ödevlerini bitir", 20, "review"),
    ("Cmt", "apcalc", "Pratik sınav #2 sonuçlarını analiz et + zayıf alan", 60, "review"),
    ("Cmt", "apchem", "Zayıf alan review", 45, "review"),
    ("Paz", "ozdeg", "Video günlüğü #29 + checkpoint + Nisan ayı retrosu", 75, "reflect"),
    ("Paz", "serbest", "Telafi/dinlenme", 45, "rest"),
]),
(30, "Mayıs · AP Sınavları ve Kapanış", [
    ("Pzt", "apcalc", "Kapsamlı son tekrar — formül listesi + hızlı problem turu", 60, "review"),
    ("Pzt", "okul", "Konu takibi", 30, "study"),
    ("Sal", "apchem", "Kapsamlı son tekrar — formül listesi + hızlı problem turu", 60, "review"),
    ("Sal", "uwc", "Kulüp toplantısı", 45, "study"),
    ("Çar", "apbio", "Kapsamlı son tekrar — 8 ünite hızlı tur", 60, "review"),
    ("Çar", "okul", "Konu tekrarı", 45, "review"),
    ("Çar", "jp", "N5 tekrar", 15, "review"),
    ("Per", "apcalc", "Son FRQ pratik seti", 45, "review"),
    ("Per", "apchem", "Son FRQ pratik seti", 45, "review"),
    ("Cum", "apbio", "Son FRQ pratik seti", 45, "review"),
    ("Cum", "fel", "Kulüp toplantısı (kısa)", 30, "study"),
    ("Cmt", "serbest", "Hafif — dinlenme, erken yatma düzeni", 30, "rest"),
    ("Paz", "ozdeg", "Video günlüğü #30 + checkpoint (sınav haftasına moral hazırlığı)", 30, "reflect"),
]),
(31, None, [
    ("Cmt", "serbest", "Dinlenme — bir haftadır yoğun sınav modundaydı", 0, "rest"),
    ("Paz", "ozdeg", "Video günlüğü #31 + checkpoint", 30, "reflect"),
]),
(32, None, [
    ("Pzt", "uwc", "Kulüp toplantısı", 60, "study"),
    ("Pzt", "okul", "Konu takibi (AP'ler bitince tüm enerji okula kayabilir)", 45, "review"),
    ("Sal", "yazproj", "Devam", 90, "produce"),
    ("Sal", "okul", "Konu tekrarı", 30, "review"),
    ("Çar", "okul", "Konu tekrarı", 75, "review"),
    ("Çar", "jp", "N5 tekrar", 15, "review"),
    ("Per", "yazproj", "Devam", 90, "produce"),
    ("Per", "okul", "Konu tekrarı", 30, "review"),
    ("Cum", "fel", "Kulüp toplantısı — dönem sonu özet raporuna başla", 60, "study"),
    ("Cum", "okul", "Haftanın ödevlerini bitir", 30, "review"),
    ("Cmt", "okul", "Yıl sonu sınavlarına hazırlık başlangıcı", 60, "review"),
    ("Cmt", "yazproj", "Devam", 45, "produce"),
    ("Paz", "ozdeg", "Video günlüğü #32 + checkpoint", 30, "reflect"),
    ("Paz", "serbest", "Telafi/dinlenme", 90, "rest"),
]),
(33, None, [
    ("Pzt", "uwc", "Kulüp toplantısı", 60, "study"),
    ("Pzt", "okul", "Yıl sonu sınav hazırlığı", 45, "review"),
    ("Sal", "yazproj", "Devam", 90, "produce"),
    ("Sal", "okul", "Konu tekrarı", 30, "review"),
    ("Çar", "okul", "Yıl sonu sınav hazırlığı", 75, "review"),
    ("Çar", "jp", "N5 tekrar", 15, "review"),
    ("Per", "yazproj", "Devam", 90, "produce"),
    ("Per", "okul", "Konu tekrarı", 30, "review"),
    ("Cum", "fel", "Kulüp dönem sonu özet raporu — devam", 60, "study"),
    ("Cum", "okul", "Haftanın ödevlerini bitir", 30, "review"),
    ("Cmt", "okul", "Yıl sonu sınav hazırlığı", 60, "review"),
    ("Cmt", "yazproj", "Devam", 45, "produce"),
    ("Paz", "ozdeg", "Video günlüğü #33 + checkpoint", 30, "reflect"),
    ("Paz", "serbest", "Telafi/dinlenme", 90, "rest"),
]),
(34, None, [
    ("Pzt", "okul", "Yıl sonu sınav hazırlığı", 75, "review"),
    ("Pzt", "uwc", "Kulüp toplantısı", 45, "study"),
    ("Sal", "okul", "Yıl sonu sınav hazırlığı", 75, "review"),
    ("Sal", "yazproj", "Devam", 45, "produce"),
    ("Çar", "okul", "Yıl sonu sınav hazırlığı", 75, "review"),
    ("Çar", "jp", "N5 tekrar", 15, "review"),
    ("Per", "okul", "Yıl sonu sınav hazırlığı", 75, "review"),
    ("Per", "yazproj", "Devam", 45, "produce"),
    ("Cum", "fel", "Kulüp dönem sonu özet raporunu bitir", 60, "study"),
    ("Cum", "okul", "Haftanın ödevlerini bitir", 30, "review"),
    ("Cmt", "okul", "Yıl sonu sınav hazırlığı", 60, "review"),
    ("Paz", "ozdeg", "Video günlüğü #34 + checkpoint + Mayıs ayı retrosu", 75, "reflect"),
    ("Paz", "serbest", "Telafi/dinlenme", 45, "rest"),
]),
(35, "Haziran · Yıl Sonu Kapanışı", [
    ("Pzt", "okul", "Yıl sonu sınavları (varsa bu hafta başlıyor)", 90, "test"),
    ("Sal", "okul", "Sınav", 90, "test"),
    ("Çar", "okul", "Sınav", 90, "test"),
    ("Çar", "jp", "N5 genel tekrar (tüm setler)", 15, "review"),
    ("Per", "okul", "Sınav", 75, "test"),
    ("Cum", "serbest", "Sınav yoğunluğuna göre esnek", 45, "rest"),
    ("Cmt", "yazproj", "Devam (sınavlar izin verirse)", 45, "produce"),
    ("Paz", "ozdeg", "Video günlüğü #35 + checkpoint", 30, "reflect"),
    ("Paz", "serbest", "Dinlenme", 60, "rest"),
]),
(36, None, [
    ("Pzt", "okul", "Sınav / son değerlendirmeler", 60, "test"),
    ("Sal", "yazproj", "Devam", 90, "produce"),
    ("Çar", "okul", "Karne öncesi son toparlanma", 45, "review"),
    ("Çar", "jp", "N5 genel tekrar", 15, "review"),
    ("Per", "yazproj", "Devam", 90, "produce"),
    ("Cum", "fel", "Kulüp — yıl sonu kapanış toplantısı", 60, "study"),
    ("Cmt", "serbest", "Dinlenme", 30, "rest"),
    ("Paz", "ozdeg", "Video günlüğü #36 + checkpoint", 30, "reflect"),
    ("Paz", "serbest", "Dinlenme", 60, "rest"),
]),
(37, None, [
    ("Pzt", "serbest", "Karne/yıl sonu — dinlenme", 0, "rest"),
    ("Sal", "serbest", "Tamamen boş", 0, "rest"),
    ("Çar", "ozdeg", "Yıl sonu büyük retrospektif: tüm yılın video günlüklerinden seçmeler izle, 5 alanda 1-10 puanlama, en büyük 5 ders çıkarımı, yaz planı + gelecek yıl (11. sınıf / UWC ise yurt dışı) için kararlar", 120, "reflect"),
]),
]

# Hafta 31'in Pzt-Cum "değişken" AP sınav bloğu ayrı ekleniyor (tek gün değil, aralık)
WEEK31_EXAM_TEXT = ("apexam",
    "AP sınavları: Kimya, Kalkülüs BC, Biyoloji (hangi gün hangi ders College Board "
    "takviminden gelir) — sınav olmayan günlerde hafif tekrar (30-45 dk) dışında görev yok",
    0, "test")


def build_phases():
    # ay etiketlerinin gectigi hafta index'lerine gore fazlara bol
    phase_defs = []
    current = None
    for n, theme, days in WEEKS:
        if theme is not None:
            if current:
                phase_defs.append(current)
            name = theme
            pid = name.split(" ")[0].lower().replace("ı", "i").replace("ş", "s").replace("ç", "c").replace("ğ", "g").replace("ü", "u").replace("ö", "o")
            current = {"id": pid, "name": name, "week_range": [n, n]}
        else:
            current["week_range"][1] = n
    if current:
        phase_defs.append(current)

    units_by_week = {}
    for n, theme, days in WEEKS:
        tasks = []
        for i, (day, subj, text, minutes, kind) in enumerate(days):
            task_id = f"w{n}-{i}"
            tasks.append({"i": task_id, "t": dated(n, day, text), "s": subj, "k": kind, "m": minutes})
        if n == 31:
            subj, text, minutes, kind = WEEK31_EXAM_TEXT
            tasks.insert(0, {"i": "w31-exam", "t": dated_range(31, "Pzt", "Cum", text), "s": subj, "k": kind, "m": minutes})
        ws = week_start(n)
        if n == 0:
            date_range = fmt(WEEK0_DATE)
        else:
            last_off = max(DAY_OFFSET[d[0]] for d in days) if days else 6
            date_range = f"{fmt(ws)} – {fmt(ws + timedelta(days=last_off))}"
        units_by_week[n] = {
            "id": f"hafta{n}",
            "title": f"Hafta {n}",
            "kicker": date_range,
            "tasks": tasks,
        }

    phases = []
    for pd in phase_defs:
        start_w, end_w = pd["week_range"]
        units = [units_by_week[w] for w in range(start_w, end_w + 1)]
        total_hours = sum(t["m"] for u in units for t in u["tasks"]) / 60.0
        phases.append({
            "id": pd["id"],
            "name": pd["name"],
            "sub": f"Hafta {start_w}-{end_w} · ~{total_hours:.0f} saat",
            "goal": pd["name"],
            "hours": round(total_hours, 1),
            "units": units,
        })
    return phases


PROJECTS = [
    {"id": "P1", "name": "P1 — Hodgkin-Huxley Çok-Kompartmanlı Nöron Modeli", "emoji": "🧠",
     "goal": "Gerçek morfoloji (NeuroMorpho.org) + gerçek elektrofizyoloji (Allen Brain Atlas) verisiyle "
             "kablo-teorisi tabanlı, stokastik iyon kanalı gürültülü, iki sinaptik bağlı nöron simülasyonu.",
     "topics": ["Cable theory", "Hodgkin-Huxley", "Stokastik kanal gürültüsü", "Senkronizasyon"],
     "next": "NeuroMorpho.org'dan bir piramidal nöron morfolojisi seç ve kablo denkleminin sayısal iskeletini kur.",
     "ms": ["Tek-kompartman model çalışıyor", "Çok-kompartmanlı + kalibre model çalışıyor",
            "İki nöron senkronizasyon analizi tamam", "Rapor/sunum Destek Eğitim Odası'na teslim edildi"]},
    {"id": "P2", "name": "P2 — Hopfield Network ile İlişkisel Bellek Modellemesi", "emoji": "🕸️",
     "goal": "Klasik Hopfield ağı (depolama kapasitesi, enerji manzarası, sahte durumlar), biyolojik bellek "
             "fenomenleriyle karşılaştırma, Hebbian öğrenme kuralı, gürültülü desenlerle geri çağırma testi, "
             "opsiyonel Transformer-attention bağlantısı.",
     "topics": ["Hopfield network", "Hebbian öğrenme", "Attractor network", "Attention"],
     "next": "Hopfield network'ün matematiksel temelini (enerji fonksiyonu, ağırlık matrisi) çıkar.",
     "ms": ["Kodlama tamam (küçük örnek desenle)", "Kapasite/gürültü testleri tamam",
            "Rapor/sunum Destek Eğitim Odası'na teslim edildi"]},
    {"id": "P3", "name": "UWC Başvurusu & Felsefe Kulübü", "emoji": "🍀",
     "goal": "UWC başvurusu birincil öncelik (Kasım'da gönderim); felsefe kulübü (analitik felsefe + bilim "
             "felsefesi) tüm yıl boyunca topluluk katkısı kanıtı olarak sürdürülüyor, yıl sonunda yazılı arşiv.",
     "topics": ["UWC başvurusu", "Felsefe kulübü"],
     "next": "\"Neden UWC, neden ben\" beyin fırtınası ile deneme taslağına başla.",
     "ms": ["Başvuru gönderildi (Kasım)", "Kulüp dönem sonu özet raporu (Aralık, Mayıs)",
            "Yıl sonu yazılı arşiv tamam"]},
    {"id": "P3-yaz", "name": "Yaz Projesi (olası P3-2)", "emoji": "☀️",
     "goal": "P2 tamamlandıktan sonra (Mart-Nisan) planlanmaya başlanan, Mayıs-Haziran'da yürütülen üçüncü "
             "üretim projesi — kapsamı Mart'ta netleşiyor.",
     "topics": [],
     "next": "P2 sonrası ne yapılacağına dair literatür/ilgi taraması yap.",
     "ms": ["Proje fikri netleşti, kapsam belirlendi", "Proje planı yazıldı", "İlk teknik adım atıldı"]},
]

BRIDGES = [
    {"n": "Kablo Teorisi Köprüsü", "e": "🔗",
     "d": "PDE + devre teorisi + gerçek nöron morfolojisi (P1'in matematiksel temeli).",
     "t": ["Cable theory", "P1"]},
    {"n": "Stokastik Süreçler ↔ Kanal Gürültüsü", "e": "🔗",
     "d": "Markov modeli = iyon kanalının açılıp kapanması.",
     "t": ["Markov modeli", "İyon kanalı gürültüsü", "P1"]},
    {"n": "Dinamik Sistemler ↔ Senkronizasyon", "e": "🔗",
     "d": "Salınım + diferansiyel denklem + iki nöron senkronu.",
     "t": ["Faz kilitleme", "Senkronizasyon", "P1"]},
    {"n": "Hopfield Ağı ↔ Transformer Attention", "e": "🔗",
     "d": "İstatistiksel mekanik kökenli attractor network, güncel yapay zeka mimarileriyle bağlantısı (opsiyonel ileri konu).",
     "t": ["Hopfield network", "Attention", "P2"]},
]


def main():
    phases = build_phases()
    doc = {
        "subjects": SUBJECTS,
        "kinds": KINDS,
        "phases": phases,
        "projects": PROJECTS,
        "bridges": BRIDGES,
    }
    with open(OUT_PATH, "w", encoding="utf-8") as f:
        json.dump(doc, f, ensure_ascii=False, indent=2)

    n_units = sum(len(p["units"]) for p in phases)
    n_tasks = sum(len(u["tasks"]) for p in phases for u in p["units"])
    print(f"wrote {OUT_PATH}")
    print(f"phases={len(phases)} units={n_units} tasks={n_tasks}")
    total_hours = sum(p["hours"] for p in phases)
    print(f"Toplam saat (tum yil) = {total_hours:.1f}")


if __name__ == "__main__":
    main()
