#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
(≧▽≦) — 2026-2027 eğitim yılı müfredatı üretici (tam yıl).

Kaynak: beril-mufredat-2026-2027.pdf — 20 Eylül 2026 - Haziran 2027 kapsamlı
kişisel program. AI üretimi değil, kaynak belgenin elle CurriculumLoader
şemasına dönüştürülmüş hali (Groq/Gemini API kotası yetersiz).

Dönem 1 (Eylül-Ocak, Hafta 0-18): belgede gün gün / hafta hafta detaylı —
her mikro-modülün metni gerçek takvim tarihiyle ("21 Eylül" gibi) başlıyor.
Dönem 2 (Şubat-Haziran): belgenin kendisi de sadece aylık hedef iskeleti
veriyor ("bu tablo bir pusula, günlük program değil, Dönem 1 sonu
retrospektifinde [23 Ocak civarı] gün-gün yeniden yazılacak") — bu yüzden
Dönem 2 görevleri kasıtlı olarak gün bazlı değil, ay adıyla etiketlendi.
Var olmayan bir günü uydurmak yerine kaynağın kendi dürüstlüğü korundu.
"""
import json, os

SUBJECTS = {
    "uwc":     ("UWC Başvurusu",        "🌍", "#6F2A36"),
    "fel":     ("Felsefe Kulübü",       "🦉", "#8073A5"),
    "p1":      ("P1 — Hodgkin-Huxley",  "🧠", "#6E2430"),
    "p2":      ("P2 — Hopfield Network", "🕸️", "#524870"),
    "okul":    ("Okul Dersleri",        "📚", "#9B7D4B"),
    "jp":      ("Japonca",              "🌸", "#B99E6E"),
    "bb":      ("Brain Bee",            "🥼", "#5F5483"),
    "sat":     ("SAT",                  "🎓", "#9C92B9"),
    "apcalc":  ("AP Kalkülüs",          "📐", "#79344B"),
    "apchem":  ("AP Kimya",             "🧪", "#A44666"),
    "apbio":   ("AP Biyoloji",          "🧬", "#8F633D"),
    "ozdeg":   ("Öz-değerlendirme",     "🪞", "#6D5F95"),
    "serbest": ("Serbest / Telafi",     "🌿", "#B0A47E"),
}

KINDS = {
    "study":   ("Öğren",        "📖"),
    "produce": ("Üret",         "🛠️"),
    "review":  ("Tekrar",       "🔁"),
    "test":    ("Sınav/Deneme", "📋"),
    "reflect": ("Değerlendir",  "🪞"),
    "explain": ("Anlat/Sun",    "🎙️"),
}

# ── Dönem 1 takvimi: her haftanın başlangıç günü (ay sırasıyla artan gün sayacı) ──
MONTHS = ["Eylül", "Ekim", "Kasım", "Aralık", "Ocak"]
MONTH_DAYS = {"Eylül": 30, "Ekim": 31, "Kasım": 30, "Aralık": 31, "Ocak": 31}

def add_days(day, month, n):
    """(gün, ay) çiftine n gün ekler, ay sınırlarını MONTHS sırasına göre aşar."""
    idx = MONTHS.index(month)
    d = day + n
    while d > MONTH_DAYS[MONTHS[idx]]:
        d -= MONTH_DAYS[MONTHS[idx]]
        idx += 1
    return d, MONTHS[idx]

def dated(start, offset, text):
    """Görev metnine gerçek takvim tarihini ('21 Eylül' gibi) önek olarak ekler."""
    d, m = add_days(start[0], start[1], offset)
    return f"{d} {m} · {text}"

# Her hafta: (id, title, start=(gün,ay), gün_sayısı, [(subject, text, minutes, kind), ...])
# tasks listesindeki sıra Pzt,Sal,Çar,Per,Cum,Cmt,Paz'a karşılık gelir (offset 0..6);
# kısa haftalarda (Hafta 0, Hafta 15) daha az gün vardır.
WEEKS = [
("h0", "Kurulum Günü", (20, "Eylül"), [
    (0, "uwc", "Bu müfredatı işle, haftalık şablon sayfası oluştur; P1 için kaynak listesi başlat (NeuroMorpho.org, Allen Brain Atlas); UWC Türkiye Ulusal Komitesi'nden kesin son başvuru tarihini teyit et", 60, "produce"),
    (0, "ozdeg", "Video günlüğü #0 (hedefler, motivasyon)", 30, "reflect"),
]),
("h1", "Hafta 1", (21, "Eylül"), [
    (0, "uwc", "“Neden UWC, neden ben” beyin fırtınası — serbest yazım, düzenleme yok", 60, "produce"),
    (1, "p1", "Cable theory teorik özeti (ders notları) + tek-kompartmanlı denklemi kağıt üzerinde türet", 75, "study"),
    (2, "okul", "Haftanın konusunu (fizik/biyoloji) pekiştir", 45, "review"),
    (2, "jp", "N5 kelime seti 1", 15, "review"),
    (3, "p1", "Python'da HH denklemlerini (Na, K, sızıntı akımı) kodlamaya başla", 75, "produce"),
    (4, "fel", "Felsefe kulübü ilk toplantı materyali (okuma + tartışma soruları)", 45, "study"),
    (4, "bb", "Nöron/sinaps temelleri okuma", 15, "study"),
    (5, "sat", "SAT diagnostic — Reading & Writing bölümü", 100, "test"),
    (6, "ozdeg", "Video günlüğü #1 + checkpoint", 30, "reflect"),
]),
("h2", "Hafta 2", (28, "Eylül"), [
    (0, "uwc", "Deneme taslağı — giriş paragrafı", 60, "produce"),
    (1, "p1", "HH modelini çalıştır, tek aksiyon potansiyeli simülasyonu + grafik çıktısı", 75, "produce"),
    (2, "okul", "Haftalık konu tekrarı + kendi kendine mini quiz", 45, "review"),
    (2, "jp", "N5 kelime seti 2", 15, "review"),
    (3, "p1", "NeuroMorpho'dan gerçekçi bir morfoloji örneği indir, model parametreleriyle karşılaştır (literatür notu)", 75, "study"),
    (4, "uwc", "Deneme taslağı — 2. paragraf", 45, "produce"),
    (4, "bb", "Aksiyon potansiyeli mekanizması", 15, "study"),
    (5, "sat", "SAT diagnostic — Math bölümü + sonuç analizi, zayıf alan listesi", 100, "test"),
    (6, "ozdeg", "Video günlüğü #2 + checkpoint", 30, "reflect"),
]),
("h3", "Hafta 3", (5, "Ekim"), [
    (0, "uwc", "Deneme v1'i baştan sona oku, geri bildirim için bir öğretmenle/güvendiğin biriyle paylaşım planla", 60, "review"),
    (1, "p1", "Çok-kompartmanlı yapıya geçiş: dendrit-soma-akson segmentasyonu tasarımı", 75, "study"),
    (2, "okul", "Konu tekrarı", 45, "review"),
    (2, "jp", "N5 kelime seti 3", 15, "review"),
    (3, "p1", "Çok-kompartman modelini kodla, tek kompartmanla karşılaştırmalı test", 75, "produce"),
    (4, "fel", "Felsefe kulübü 2. toplantı + notlarını dosyala (başvuruda liderlik kanıtı olacak)", 45, "explain"),
    (4, "bb", "İyon kanalları", 15, "study"),
    (5, "apcalc", "Türev konusunu kendi yaptığın siteden tekrar et + ilgili oranlar (related rates) problemleri", 100, "review"),
    (6, "ozdeg", "Video günlüğü #3 + checkpoint", 30, "reflect"),
]),
("h4", "Hafta 4", (12, "Ekim"), [
    (0, "uwc", "Geri bildirimlere göre deneme v2 revizyonu", 60, "produce"),
    (1, "p1", "Allen Brain Atlas'tan elektrofizyoloji verisi çek, model parametrelerini kalibre et", 75, "produce"),
    (2, "okul", "Konu tekrarı", 45, "review"),
    (2, "jp", "N5 kelime seti 4", 15, "review"),
    (3, "p1", "Kalibrasyon sonuçlarını doğrula, sapmaları not al", 75, "study"),
    (4, "fel", "Felsefe kulübü 3. toplantı hazırlığı", 45, "study"),
    (4, "bb", "Sinaptik iletim", 15, "study"),
    (5, "apchem", "Atom yapısı ve periyodik eğilimler — giriş", 100, "study"),
    (6, "ozdeg", "Video günlüğü #4 + checkpoint", 30, "reflect"),
]),
("h5", "Hafta 5", (19, "Ekim"), [
    (0, "uwc", "Başvuru formunun geri kalan bölümleri (aktiviteler, referanslar için kime soracağını netleştir)", 60, "produce"),
    (1, "p1", "Kalibre edilmiş modelle uzun-süreli simülasyon, kararlılık testi", 75, "produce"),
    (2, "okul", "Konu tekrarı — ilk ara sınavlar yaklaşıyorsa buna ağırlık ver", 45, "review"),
    (2, "jp", "N5 kelime seti 5", 15, "review"),
    (3, "p1", "Sonuçları grafikle özetle, ilk taslak metin (yöntem bölümü)", 75, "produce"),
    (4, "fel", "Felsefe kulübü 3. toplantı", 45, "explain"),
    (4, "bb", "Nöroanatomi — lob ve bölgeler", 15, "study"),
    (5, "sat", "Tam bölüm pratik testi (R&W veya Math, dönüşümlü)", 110, "test"),
    (6, "ozdeg", "Video günlüğü #5 + checkpoint", 30, "reflect"),
]),
("h6", "Hafta 6", (26, "Ekim"), [
    (0, "uwc", "Deneme v2'yi son haline getir, referans mektubu isteme (öğretmene mesaj taslağı)", 60, "produce"),
    (1, "p1", "Stokastik iyon kanalı gürültüsü için literatür okuması (Markov kanal modeli)", 75, "study"),
    (2, "okul", "Konu tekrarı / varsa ara sınav hazırlığı", 45, "review"),
    (2, "jp", "N5 tekrar (set 1-5)", 15, "review"),
    (3, "p1", "Stokastik gürültü modülünü koda ekleme (ilk deneme)", 75, "produce"),
    (4, "fel", "Felsefe kulübü 4. toplantı", 45, "explain"),
    (4, "bb", "Duyu sistemleri giriş", 15, "study"),
    (5, "apcalc", "İntegral kavramına giriş (Riemann toplamları)", 100, "study"),
    (6, "ozdeg", "Video günlüğü #6 + checkpoint + Ekim ayı retrosu", 75, "reflect"),
]),
("h7", "Hafta 7", (2, "Kasım"), [
    (0, "uwc", "Başvuruyu gözden geçir ve gönder (tarih Hafta 0'da teyit edilen son tarihe göre kayabilir)", 90, "produce"),
    (1, "p1", "Stokastik kanal gürültüsünü ikinci nörona da uygula", 75, "produce"),
    (2, "okul", "Konu tekrarı", 45, "review"),
    (2, "jp", "N5 kelime seti 6", 15, "review"),
    (3, "p1", "İki nöronu sinaptik olarak bağlama — bağlantı mimarisi tasarımı", 75, "study"),
    (4, "fel", "Kulüp 5. toplantı (artık UWC gönderildiği için tema: kulübün kendi bağımsız hedefi)", 45, "explain"),
    (4, "bb", "İşitme/görme sistemleri", 15, "study"),
    (5, "sat", "Tam deneme sınavı (tek oturum, mümkünse)", 130, "test"),
    (6, "ozdeg", "Video günlüğü #7 + checkpoint", 30, "reflect"),
]),
("h8", "Hafta 8 (sınav haftası olasılığı)", (9, "Kasım"), [
    (0, "uwc", "Mülakat olasılığına karşı hazırlık notları (kendi hikayeni 3 dk'da anlatma pratiği)", 60, "produce"),
    (1, "p1", "Sinaptik bağlantıyı kodla, ilk ortak simülasyon testi", 75, "produce"),
    (2, "okul", "Sınav odaklı yoğun tekrar (bu hafta yoğun/ara sınav olasılığı)", 60, "review"),
    (2, "jp", "Bu hafta esnek/atlanabilir", 15, "review"),
    (3, "okul", "Sınav odaklı yoğun tekrar (P1 bu hafta hafif)", 60, "review"),
    (4, "fel", "Kulüp toplantısı (okul sınav haftasıysa kısa tutulabilir)", 45, "explain"),
    (4, "bb", "Hafıza sistemleri", 15, "study"),
    (5, "sat", "Bu hafta okula öncelik verildiği için hafif — 1 bölüm pratik", 60, "test"),
    (6, "ozdeg", "Video günlüğü #8 + checkpoint (sınav haftası nasıl geçti?)", 30, "reflect"),
]),
("h9", "Hafta 9", (16, "Kasım"), [
    (0, "uwc", "Kulüp için yeni dönem içeriği planlama (UWC sonrası bağımsız hedef)", 60, "produce"),
    (1, "p1", "İki nöron simülasyonunda faz kilitleme / senkronizasyon ölçümü — ilk analiz", 75, "produce"),
    (2, "okul", "Konu tekrarı", 45, "review"),
    (2, "jp", "N5 kelime seti 7", 15, "review"),
    (3, "p1", "Cross-correlation analizi, parametre taraması (bağlantı gücü değiştikçe senkronizasyon nasıl değişiyor)", 75, "produce"),
    (4, "fel", "Kulüp toplantısı", 45, "explain"),
    (4, "bb", "Nörolojik hastalıklara giriş", 15, "study"),
    (5, "apcalc", "Belirli integral, Analizin Temel Teoremi", 100, "study"),
    (6, "ozdeg", "Video günlüğü #9 + checkpoint", 30, "reflect"),
]),
("h10", "Hafta 10", (23, "Kasım"), [
    (0, "uwc", "Kulüp çıktısını yazılı hale getir (rapor/blog — portföy için)", 60, "produce"),
    (1, "p1", "Senkronizasyon sonuçlarını grafikle özetle, sonuçlar bölümü taslağı", 75, "produce"),
    (2, "okul", "Konu tekrarı", 45, "review"),
    (2, "jp", "N5 kelime seti 8", 15, "review"),
    (3, "p1", "Rapor/yazım — giriş ve yöntem bölümlerini birleştir", 75, "produce"),
    (4, "fel", "Kulüp toplantısı", 45, "explain"),
    (4, "bb", "Bilişsel nörobilim giriş", 15, "study"),
    (5, "sat", "Tam pratik test #2", 130, "test"),
    (6, "ozdeg", "Video günlüğü #10 + checkpoint + Kasım ayı retrosu", 75, "reflect"),
]),
("h11", "Hafta 11", (30, "Kasım"), [
    (0, "uwc", "Kulüp toplantısı + dönem sonu kulüp özet raporu başlangıcı", 60, "explain"),
    (1, "p1", "Raporun tartışma/sonuç bölümü taslağı", 75, "produce"),
    (2, "okul", "Konu tekrarı", 45, "review"),
    (2, "jp", "N5 kelime seti 9", 15, "review"),
    (3, "p1", "Destek Eğitim Odası'na sunum için slayt/görsel hazırlığı", 75, "produce"),
    (4, "fel", "Kulüp toplantısı", 45, "explain"),
    (4, "bb", "Genel tekrar seti 1", 15, "review"),
    (5, "apchem", "Kimyasal bağlar, moleküler geometri", 90, "study"),
    (6, "ozdeg", "Video günlüğü #11 + checkpoint", 30, "reflect"),
]),
("h12", "Hafta 12", (7, "Aralık"), [
    (0, "uwc", "Kulüp özet raporunu bitir", 60, "produce"),
    (1, "p1", "Rapor/sunum revizyonu — geri bildirim al (danışman/öğretmen)", 75, "review"),
    (2, "okul", "Konu tekrarı", 45, "review"),
    (2, "jp", "N5 kelime seti 10", 15, "review"),
    (3, "p1", "Geri bildirime göre son düzeltmeler", 75, "produce"),
    (4, "fel", "Kulüp toplantısı", 45, "explain"),
    (4, "bb", "Genel tekrar seti 2", 15, "review"),
    (5, "sat", "Tam pratik test #3", 130, "test"),
    (6, "ozdeg", "Video günlüğü #12 + checkpoint", 30, "reflect"),
]),
("h13", "Hafta 13 (dönem sonu sınav haftası)", (14, "Aralık"), [
    (0, "p1", "Sunum/rapor final teslim (Destek Eğitim Odası)", 90, "produce"),
    (1, "okul", "Dönem sonu sınavlarına (varsa) yoğunlaşma", 75, "review"),
    (2, "okul", "Sınav tekrarı", 45, "review"),
    (2, "jp", "N5 genel tekrar", 15, "review"),
    (3, "okul", "Sınav tekrarı", 75, "review"),
    (4, "apchem", "Termokimya — final sınavına göre hafif tekrar", 60, "review"),
    (5, "serbest", "Bu hafta okul öncelikli — kaçan modülleri buraya taşı", 60, "review"),
    (6, "ozdeg", "Video günlüğü #13 + checkpoint", 30, "reflect"),
]),
("h14", "Hafta 14 (yarıyıl tatili başlangıcı olası)", (21, "Aralık"), [
    (0, "uwc", "Mülakat çağrısı geldiyse hazırlık; gelmediyse tam dinlenme", 45, "review"),
    (1, "bb", "Ocak dönemine hazırlık — konu haritası çıkar (hangi başlıklar eksik)", 45, "study"),
    (2, "serbest", "İstersen tamamen boş bırak", 0, "review"),
    (3, "p1", "Rapor sonrası “ileri adımlar” notu (Ocak'ta nereden devam edilecek)", 45, "produce"),
    (4, "serbest", "İstersen tamamen boş bırak", 0, "review"),
    (5, "apcalc", "Hafif — kendi seçtiğin zayıf konudan 1 tekrar (istersen atla)", 45, "review"),
    (6, "ozdeg", "Video günlüğü #14 + checkpoint", 30, "reflect"),
]),
("h15", "Hafta 15 (kısa hafta, yılbaşı arası)", (28, "Aralık"), [
    (0, "serbest", "Dinlenme — istersen tamamen boş bırak", 0, "review"),
    (1, "apbio", "Ocak'ta başlayacak Biyoloji rotasyonu için Campbell'dan hücre biyolojisi bölümünü göz gezdir (yük değil, ısınma)", 30, "study"),
    (2, "serbest", "—", 0, "review"),
    (3, "ozdeg", "Yıl ortası mini-check: video günlüğü #15 + checkpoint + kısa bir bakış (bu 3 ay nasıl geçti?)", 45, "reflect"),
]),
("h16", "Hafta 16", (4, "Ocak"), [
    (0, "uwc", "Kulübün ikinci yarıyıl planını taslakla (yeni üyeler, yeni tema)", 60, "produce"),
    (1, "p2", "P1'i kapalı dosya olarak arşivle; P2 (Hopfield network / ilişkisel bellek modeli) için literatür taraması başlat", 75, "study"),
    (2, "okul", "Konu tekrarı", 45, "review"),
    (2, "jp", "N5 set 11", 15, "review"),
    (3, "p2", "Hopfield network'ün matematiksel temeli (enerji fonksiyonu, ağırlık matrisi) — kağıt üzerinde", 75, "study"),
    (4, "fel", "Kulüp toplantısı", 45, "explain"),
    (4, "bb", "Genel tekrar seti 3", 15, "review"),
    (5, "apbio", "Hücre biyolojisi — zar yapısı, taşınma mekanizmaları (Campbell)", 90, "study"),
    (6, "ozdeg", "Video günlüğü #16 + checkpoint", 30, "reflect"),
]),
("h17", "Hafta 17", (11, "Ocak"), [
    (0, "uwc", "Kulüp toplantısı", 45, "explain"),
    (1, "p2", "Hopfield network'ü Python'da kodlamaya başla (küçük örnek desenle)", 75, "produce"),
    (2, "okul", "Yoğun tekrar (dönem sonu sınavlarına yaklaşılıyor)", 45, "review"),
    (2, "jp", "N5 set 12", 15, "review"),
    (3, "okul", "Yoğun tekrar (P2 bu hafta hafif)", 60, "review"),
    (4, "bb", "Genel tekrar seti 4 (Felsefe kulübü bu hafta sınav yoğunluğundan kısa/atlanabilir)", 20, "review"),
    (5, "sat", "Tam pratik test #4 — dönem sonu genel ölçüm", 130, "test"),
    (6, "ozdeg", "Video günlüğü #17 + checkpoint", 30, "reflect"),
]),
("h18", "Hafta 18 (dönem sonu sınavları + karne ~23 Ocak)", (18, "Ocak"), [
    (0, "okul", "Sınav tekrarı", 75, "review"),
    (1, "okul", "Sınav tekrarı", 75, "review"),
    (2, "okul", "Sınav tekrarı", 45, "review"),
    (2, "jp", "N5 genel tekrar (1-12)", 0, "review"),
    (3, "okul", "Sınav tekrarı", 60, "review"),
    (4, "serbest", "Bu hafta tamamen okul öncelikli", 30, "review"),
    (5, "serbest", "Sınavlar bitince dinlen", 0, "review"),
    (6, "ozdeg", "Dönem 1 büyük retrospektifi (bkz. Öz-değerlendirme Sistemi) + Dönem 2'yi bu retrospektife göre detaylandırma oturumu (karne sonrası)", 105, "reflect"),
]),
]

def build_dated_task(uid, idx, start, offset, subj, text, minutes, kind):
    return {
        "i": f"{uid}-t{idx+1:02d}",
        "t": dated(start, offset, text),
        "s": subj, "k": kind, "m": minutes,
    }

UNIT_BRIDGES = {
    "h1": ["Kablo Teorisi Köprüsü"],
    "h6": ["Stokastik Süreçler ↔ Kanal Gürültüsü"],
    "h9": ["Dinamik Sistemler ↔ Senkronizasyon"],
    "h16": ["Hopfield Ağı ↔ Transformer Attention"],
}

def build_unit(w):
    uid, title, start, tasks = w
    d, m = start
    kicker = f"{d} {m}" if len(tasks) <= 2 or uid == "h0" else f"{d} {m} ile başlayan hafta"
    return {
        "id": uid,
        "title": title,
        "kicker": kicker,
        "br": UNIT_BRIDGES.get(uid, []),
        "tasks": [
            build_dated_task(uid, idx, start, offset, subj, text, minutes, kind)
            for idx, (offset, subj, text, minutes, kind) in enumerate(tasks)
        ],
    }

PHASES = [
    ("eylul", "Eylül · Kurulum ve Keşif", "20-30 Eylül 2026", "Sistemi oturt, UWC yönünü netleştir, P1'in teorik temelini at.", 0, 3),
    ("ekim", "Ekim · Derinleşme", "1-31 Ekim 2026", "P1'i çok-kompartmanlı hale getir, UWC dosyasını bitir, SAT zayıf alanlarını sistemli çalış, felsefe kulübünü düzenli işlet.", 3, 7),
    ("kasim", "Kasım · Tamamlama ve Sıkılaştırma", "1-30 Kasım 2026", "UWC'yi gönder, P1'de stokastik gürültü + iki-nöron senkronizasyonuna geç, okulun yoğun (muhtemel ara sınav) haftasını yönet.", 7, 11),
    ("aralik", "Aralık · Bitirme, Toparlama, Dinlenme", "1-31 Aralık 2026", "P1 raporunu/sunumunu bitir, dönem sonu değerlendirmesi yap, tatile makul bir bitiş çizgisiyle gir.", 11, 16),
    ("ocak", "Ocak · Dönem 1 Kapanışı + AP Biyoloji'nin Devreye Girişi", "1-24 Ocak 2027", "Okula dönüş, dönem sonu sınavları, Cumartesi rotasyonuna AP Biyoloji'yi ekle, Dönem 1'i sağlam kapat.", 16, 19),
]

phases = []
for pid, name, sub, goal, start, end in PHASES:
    units = [build_unit(w) for w in WEEKS[start:end]]
    hours = round(sum(t["m"] for u in units for t in u["tasks"]) / 60)
    phases.append({"id": pid, "name": name, "sub": sub, "goal": goal, "hours": hours, "units": units})

phases[-1]["units"][-1]["note"] = (
    "Dönem 1 sonu milestone (23 Ocak civarı): Karne alındı · P1 tamamen kapandı, "
    "P2'ye (Hopfield network) geçiş yapıldı · AP Biyoloji rotasyona girdi · "
    "SAT'ta 4 tam deneme birikti."
)

# ── Dönem 2 — Şubat-Haziran 2027: iskelet (kaynak belge de gün-gün değil,
# yalnızca aylık hedef veriyor; Dönem 1 sonu retrospektifinde [~23 Ocak]
# aynı gün-gün detay seviyesiyle yeniden yazılacak — bkz. not alanları) ──
DONEM2_UNITS = [
    ("s1", "Şubat", "~9 Şubat 2027'den itibaren (yarıyıl tatili sonrası)", [
        ("p2", "Model kodlanması tamamlanır, kapasite testleri (kaç desen hatasız hatırlanıyor)", "produce"),
        ("uwc", "Kulüp yeni dönem temasıyla devam; UWC sonucu/mülakat süreci varsa buraya oturur", "explain"),
        ("okul", "Yeni dönem konularına adaptasyon", "review"),
        ("sat", "AP'lerde 3 dersin de orta seviye konuları paralel ilerler; SAT'ta zayıf alan review", "review"),
        ("bb", "Bölgesel/ulusal round takvimi netleşirse yoğunlaşma başlar", "study"),
    ]),
    ("s2", "Mart", "Şubat sonrası", [
        ("p2", "Gürültülü/eksik desenlerle geri çağırma testleri, sonuçların analizi", "produce"),
        ("uwc", "Kulüp devam", "explain"),
        ("okul", "Konu tekrarı", "review"),
        ("sat", "AP'ler ileri konulara geçer; SAT tam deneme #5", "test"),
        ("bb", "Round'a göre yoğun prova", "study"),
    ]),
    ("s3", "Nisan", "Mart sonrası", [
        ("p2", "Rapor/yazım — P1 formatına benzer bir sunum hazırlığı", "produce"),
        ("uwc", "Kulüp devam + varsa UWC yerleştirme süreci netleşir", "explain"),
        ("okul", "Konu tekrarı + varsa ikinci ara sınavlara hazırlık", "review"),
        ("sat", "AP sınavları yaklaşıyor — üç dersin de yoğun tekrar moduna geçmesi (Mayıs'ın ilk iki haftasına göre geri sayım)", "review"),
        ("bb", "Round sonrası veya devam eden hazırlık", "study"),
    ]),
    ("s4", "Mayıs", "Nisan sonrası — AP sınavları Mayıs'ın ilk iki haftasında (kesin tarih College Board'dan)", [
        ("p2", "P2 final teslim (Destek Eğitim Odası)", "produce"),
        ("uwc", "Kulüp dönem sonu özeti", "explain"),
        ("okul", "Konu tekrarı", "review"),
        ("sat", "AP Kimya, AP Kalkülüs, AP Biyoloji sınavları (tarihler netleşince Cumartesi/hafta içi bloklar buna göre kayacak); SAT varsa son tam deneme", "test"),
    ]),
    ("s5", "Haziran", "Mayıs sonrası", [
        ("p2", "Yaz için sıradaki proje (P3 sonrası yeni bir üretim projesi mi, yoksa bir üniversite/yaz programı mı — bu karar Dönem 1 sonu retrospektifinde netleşir)", "reflect"),
        ("uwc", "Yıl sonu kulüp raporu", "explain"),
        ("okul", "Yıl sonu sınavları", "test"),
        ("sat", "Sınavlar bitti — dinlenme + sonuç değerlendirme", "reflect"),
        ("bb", "Yıl sonu değerlendirme", "reflect"),
    ]),
]

donem2_units = []
for uid, ay, kicker, items in DONEM2_UNITS:
    tasks = [
        {"i": f"{uid}-t{idx+1:02d}", "t": f"({ay}) {text}", "s": subj, "k": kind, "m": 0}
        for idx, (subj, text, kind) in enumerate(items)
    ]
    donem2_units.append({
        "id": uid, "title": ay, "kicker": kicker,
        "note": (
            "Bu birim bir pusula, günlük program değil — kaynak belge de aylık hedeften daha "
            "ayrıntılı bir şey vermiyor. Dönem 1 sonu retrospektifinde (~23 Ocak 2027) UWC "
            "sonucu, AP sınav tarihleri ve Brain Bee takvimi netleştikten sonra, Eylül-Ocak "
            "bloğuyla aynı gün-gün detay seviyesinde yeniden yazılacak."
        ),
        "tasks": tasks,
    })

phases.append({
    "id": "donem2",
    "name": "Dönem 2 · Şubat-Haziran 2027 (iskelet)",
    "sub": "~9 Şubat - Haziran 2027",
    "goal": "Haftalık ritim aynı kalır (bkz. Hızlı Referans), Cumartesi rotasyonu artık 4'lü döner: SAT / AP Kalkülüs / AP Kimya / AP Biyoloji. Bu faz Dönem 1 sonu retrospektifinde gün-gün yeniden yazılacak.",
    "hours": 0,
    "units": donem2_units,
})

data = {
    "version": 1,
    "title": "Beril · 2026-2027 Eğitim Yılı Müfredatı",
    "phases": phases,
    "projects": [
        {
            "id": "P1", "name": "Hodgkin-Huxley Çok-Kompartmanlı Nöron Modeli", "emoji": "🧠",
            "goal": "Gerçek nöron morfolojisi (NeuroMorpho.org) ve elektrofizyoloji verisiyle (Allen Brain Atlas) kablo-teorisi tabanlı, stokastik iyon kanalı gürültülü, iki sinaptik bağlı nöron simülasyonu — Destek Eğitim Odası resmi projesi. Ocak'ta (Hafta 16) kapanıp arşivlenir, yerini P2 alır.",
            "phases": {
                "eylul": "Teorik temel: cable theory, tek-kompartmanlı HH modeli",
                "ekim": "Çok-kompartmanlı model + gerçek veriyle kalibrasyon",
                "kasim": "Stokastik gürültü + iki nöron sinaptik bağlantı + senkronizasyon analizi",
                "aralik": "Rapor/sunum yazımı ve final teslimi",
                "ocak": "Arşivleme ve P2'ye geçiş",
            },
            "topics": ["Cable theory", "Hodgkin-Huxley denklemleri", "Çok-kompartmanlı modelleme",
                       "Stokastik iyon kanalı gürültüsü (Markov modeli)", "Sinaptik bağlantı", "Senkronizasyon analizi"],
            "next": "Cable theory teorik özetini çıkar ve tek-kompartmanlı denklemi kağıt üzerinde türet.",
            "ms": ["Tek-kompartman model çalışıyor", "Çok-kompartmanlı + kalibre model çalışıyor",
                   "İki nöron senkronizasyon analizi tamam", "Rapor/sunum final teslimi", "Arşivlendi, P2'ye geçildi"],
        },
        {
            "id": "P2", "name": "Hopfield Network ile İlişkisel Bellek Modellemesi", "emoji": "🕸️",
            "goal": "Klasik Hopfield ağı (enerji fonksiyonu, ağırlık matrisi, depolama kapasitesi), gürültülü/eksik desenlerle geri çağırma testleri, Mayıs'ta final teslimi (Destek Eğitim Odası).",
            "phases": {
                "ocak": "Literatür taraması + matematiksel temel + ilk Python kodu",
                "donem2": "Kapasite testleri (Şubat), geri çağırma analizi (Mart), rapor hazırlığı (Nisan), final teslim (Mayıs)",
            },
            "topics": ["Enerji fonksiyonu", "Ağırlık matrisi", "Depolama kapasitesi", "Gürültülü/eksik desenle geri çağırma"],
            "next": "P1'i kapalı dosya olarak arşivle, Hopfield network için literatür taramasına başla.",
            "ms": ["Matematiksel temel çıkarıldı", "Python'da küçük örnek desenle kodlama başladı",
                   "Model kodlanması tamamlandı, kapasite testleri yapıldı", "Final teslim (Mayıs)"],
        },
        {
            "id": "P3", "name": "UWC Başvurusu ve Felsefe Kulübü", "emoji": "🌍",
            "goal": "UWC başvurusu birincil öncelik; felsefe kulübü, başvuru sonrası da bağımsız bir hedefle sürdürülen bir topluluk katkısı kanıtı.",
            "phases": {
                "eylul": "Son başvuru tarihini teyit et, deneme taslağı v1",
                "ekim": "Deneme v2, başvuru formu, referans süreci",
                "kasim": "Başvuruyu gönder, mülakat hazırlığı",
                "aralik": "Kulüp dönem özet raporu",
                "ocak": "İkinci yarıyıl kulüp planı",
                "donem2": "UWC sonucu/mülakat/yerleştirme süreci netleşir, kulüp yıl sonu raporu",
            },
            "topics": ["Başvuru denemesi", "Başvuru formu", "Referans mektupları", "Mülakat hazırlığı", "Felsefe kulübü toplantıları"],
            "next": "Türkiye Ulusal Komitesi sitesinden kesin son başvuru tarihini teyit et.",
            "ms": ["Deneme taslağı v1 tamam", "Başvuru formu tamam", "Başvuru gönderildi", "Kulüp dönem özeti yazıldı",
                   "İkinci yarıyıl planı taslaklandı", "Yıl sonu kulüp raporu"],
        },
    ],
    "assessments": [],
    "bridges": [
        {"n": "Kablo Teorisi Köprüsü", "e": "🔗", "d": "PDE + devre teorisi + gerçek nöron morfolojisi (P1'in matematiksel temeli).", "t": ["Cable theory", "P1"]},
        {"n": "Stokastik Süreçler ↔ Kanal Gürültüsü", "e": "🔗", "d": "Markov modeli = iyon kanalının açılıp kapanması.", "t": ["Markov modeli", "İyon kanalı gürültüsü", "P1"]},
        {"n": "Dinamik Sistemler ↔ Senkronizasyon", "e": "🔗", "d": "Salınım + diferansiyel denklem + iki nöron senkronu.", "t": ["Faz kilitleme", "Senkronizasyon", "P1"]},
        {"n": "Hopfield Ağı ↔ Transformer Attention", "e": "🔗", "d": "İstatistiksel mekanik kökenli attractor network, güncel yapay zeka mimarileriyle bağlantısı (opsiyonel ileri konu).", "t": ["Hopfield network", "Attention", "P2"]},
    ],
    "resources": [
        {"s": "p1", "n": "NeuroMorpho.org", "u": "Gerçek nöron morfolojisi verisi"},
        {"s": "p1", "n": "Allen Brain Atlas", "u": "Elektrofizyoloji verisi"},
        {"s": "sat", "n": "SAT resmi deneme sınavları", "u": "Diagnostic ve tam pratik testler"},
        {"s": "apcalc", "n": "Kendi türev/trigonometri/polinom siteleri", "u": "AP Kalkülüs tekrarı"},
        {"s": "apbio", "n": "Campbell Biology", "u": "AP Biyoloji ders kitabı"},
        {"s": "jp", "n": "N5 kelime setleri", "u": "Haftalık Japonca kelime tekrarı"},
    ],
    "subjects": [{"c": c, "n": n, "e": e, "col": col} for c, (n, e, col) in SUBJECTS.items()],
    "kinds": [{"c": c, "n": n, "e": e} for c, (n, e) in KINDS.items()],
}

out = os.path.normpath(os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets", "curriculum.json"))
os.makedirs(os.path.dirname(out), exist_ok=True)
with open(out, "w", encoding="utf-8") as f:
    json.dump(data, f, ensure_ascii=False, separators=(",", ":"))

nu = sum(len(p["units"]) for p in phases)
nt = sum(len(u["tasks"]) for p in phases for u in p["units"])
print("wrote", out)
print("phases=%d units=%d tasks=%d" % (len(phases), nu, nt))
print("Dönem 1 toplam saat =", sum(t["m"] for p in phases[:-1] for u in p["units"] for t in u["tasks"]) / 60)
