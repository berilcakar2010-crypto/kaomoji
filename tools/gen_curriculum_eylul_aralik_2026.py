#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
(≧▽≦) — Eylül-Aralık 2026 müfredatı üretici.

Kaynak: beril-mufredat-eylul-aralik-2026.pdf (16 haftalık, 18 Eylül - 31 Aralık
2026 kapsamlı kişisel program) elle bu şemaya dönüştürüldü — AI üretimi değil,
kaynak belgenin birebir yapılandırılmış transkripsiyonu (Groq/Gemini API
kotası yetersiz olduğu için CurriculumGenScreen'deki AI akışı yerine
doğrudan bu script kullanıldı).

Şema CurriculumLoader.kt / Models.kt ile birebir aynı (kısaltılmış anahtarlar).
"""
import json, os

SUBJECTS = {
    "uwc":     ("UWC Başvurusu",        "🌍", "#6F2A36"),
    "fel":     ("Felsefe Kulübü",       "🦉", "#8073A5"),
    "p1":      ("P1 — Hodgkin-Huxley",  "🧠", "#6E2430"),
    "okul":    ("Okul Dersleri",        "📚", "#9B7D4B"),
    "jp":      ("Japonca",              "🌸", "#B99E6E"),
    "bb":      ("Brain Bee",            "🥼", "#5F5483"),
    "sat":     ("SAT",                  "🎓", "#9C92B9"),
    "apcalc":  ("AP Kalkülüs",          "📐", "#79344B"),
    "apchem":  ("AP Kimya",             "🧪", "#A44666"),
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

# Her hafta: (id, title, kicker, [(subject, text, minutes, kind), ...])
WEEKS = [
("h0", "Kurulum Haftası", "18-20 Eylül", [
    ("okul", "Bu müfredatı işle, haftalık şablon sayfası oluştur; P1 için kaynak listesi başlat (NeuroMorpho.org hesabı, Allen Brain Atlas erişimi)", 60, "produce"),
    ("uwc", "Türkiye Ulusal Komitesi sitesinden kesin son başvuru tarihini teyit et, gerekli belge listesini çıkar, takvimi buna göre not al", 75, "study"),
    ("fel", "Dönem teması ve ilk toplantı gündemi taslağı", 45, "produce"),
    ("ozdeg", "Video günlüğü #0 (hedefler, motivasyon)", 30, "reflect"),
]),
("h1", "Hafta 1", "21-27 Eylül", [
    ("uwc", "“Neden UWC, neden ben” beyin fırtınası — serbest yazım, düzenleme yok", 60, "produce"),
    ("p1", "Cable theory teorik özeti (ders notları) + tek-kompartmanlı denklemi kağıt üzerinde türet", 75, "study"),
    ("okul", "Haftanın konusunu (fizik/biyoloji) pekiştir", 45, "review"),
    ("jp", "N5 kelime seti 1", 15, "review"),
    ("p1", "Python'da HH denklemlerini (Na, K, sızıntı akımı) kodlamaya başla", 75, "produce"),
    ("fel", "Felsefe kulübü ilk toplantı materyali (okuma + tartışma soruları)", 45, "study"),
    ("bb", "Nöron/sinaps temelleri okuma", 15, "study"),
    ("sat", "SAT diagnostic — Reading & Writing bölümü", 100, "test"),
    ("ozdeg", "Video günlüğü #1 + checkpoint", 30, "reflect"),
]),
("h2", "Hafta 2", "28 Eylül – 4 Ekim", [
    ("uwc", "Deneme taslağı — giriş paragrafı", 60, "produce"),
    ("p1", "HH modelini çalıştır, tek aksiyon potansiyeli simülasyonu + grafik çıktısı", 75, "produce"),
    ("okul", "Haftalık konu tekrarı + kendi kendine mini quiz", 45, "review"),
    ("jp", "N5 kelime seti 2", 15, "review"),
    ("p1", "NeuroMorpho'dan gerçekçi bir morfoloji örneği indir, model parametreleriyle karşılaştır (literatür notu)", 75, "study"),
    ("uwc", "Deneme taslağı — 2. paragraf", 45, "produce"),
    ("bb", "Aksiyon potansiyeli mekanizması", 15, "study"),
    ("sat", "SAT diagnostic — Math bölümü + sonuç analizi, zayıf alan listesi", 100, "test"),
    ("ozdeg", "Video günlüğü #2 + checkpoint", 30, "reflect"),
]),
("h3", "Hafta 3", "5-11 Ekim", [
    ("uwc", "Deneme v1'i baştan sona oku, geri bildirim için bir öğretmenle/güvendiğin biriyle paylaşım planla", 60, "review"),
    ("p1", "Çok-kompartmanlı yapıya geçiş: dendrit-soma-akson segmentasyonu tasarımı", 75, "study"),
    ("okul", "Konu tekrarı", 45, "review"),
    ("jp", "N5 kelime seti 3", 15, "review"),
    ("p1", "Çok-kompartman modelini kodla, tek kompartmanla karşılaştırmalı test", 75, "produce"),
    ("fel", "Felsefe kulübü 2. toplantı + notlarını dosyala (başvuruda liderlik kanıtı olacak)", 45, "explain"),
    ("bb", "İyon kanalları", 15, "study"),
    ("apcalc", "Türev konusunu kendi yaptığın siteden tekrar et + ilgili oranlar (related rates) problemleri", 100, "review"),
    ("ozdeg", "Video günlüğü #3 + checkpoint", 30, "reflect"),
]),
("h4", "Hafta 4", "12-18 Ekim", [
    ("uwc", "Geri bildirimlere göre deneme v2 revizyonu", 60, "produce"),
    ("p1", "Allen Brain Atlas'tan elektrofizyoloji verisi çek, model parametrelerini kalibre et", 75, "produce"),
    ("okul", "Konu tekrarı", 45, "review"),
    ("jp", "N5 kelime seti 4", 15, "review"),
    ("p1", "Kalibrasyon sonuçlarını doğrula, sapmaları not al", 75, "study"),
    ("fel", "Felsefe kulübü 3. toplantı hazırlığı", 45, "study"),
    ("bb", "Sinaptik iletim", 15, "study"),
    ("apchem", "Atom yapısı ve periyodik eğilimler — giriş", 100, "study"),
    ("ozdeg", "Video günlüğü #4 + checkpoint", 30, "reflect"),
]),
("h5", "Hafta 5", "19-25 Ekim", [
    ("uwc", "Başvuru formunun geri kalan bölümleri (aktiviteler, referanslar için kime soracağını netleştir)", 60, "produce"),
    ("p1", "Kalibre edilmiş modelle uzun-süreli simülasyon, kararlılık testi", 75, "produce"),
    ("okul", "Konu tekrarı — ilk ara sınavlar yaklaşıyorsa buna ağırlık ver", 45, "review"),
    ("jp", "N5 kelime seti 5", 15, "review"),
    ("p1", "Sonuçları grafikle özetle, ilk taslak metin (yöntem bölümü)", 75, "produce"),
    ("fel", "Felsefe kulübü 3. toplantı", 45, "explain"),
    ("bb", "Nöroanatomi — lob ve bölgeler", 15, "study"),
    ("sat", "Tam bölüm pratik testi (R&W veya Math, dönüşümlü)", 110, "test"),
    ("ozdeg", "Video günlüğü #5 + checkpoint", 30, "reflect"),
]),
("h6", "Hafta 6", "26 Ekim – 1 Kasım", [
    ("uwc", "Deneme v2'yi son haline getir, referans mektubu isteme (öğretmene mesaj taslağı)", 60, "produce"),
    ("p1", "Stokastik iyon kanalı gürültüsü için literatür okuması (Markov kanal modeli)", 75, "study"),
    ("okul", "Konu tekrarı / varsa ara sınav hazırlığı", 45, "review"),
    ("jp", "N5 tekrar (set 1-5)", 15, "review"),
    ("p1", "Stokastik gürültü modülünü koda ekleme (ilk deneme)", 75, "produce"),
    ("fel", "Felsefe kulübü 4. toplantı", 45, "explain"),
    ("bb", "Duyu sistemleri giriş", 15, "study"),
    ("apcalc", "İntegral kavramına giriş (Riemann toplamları)", 100, "study"),
    ("ozdeg", "Video günlüğü #6 + checkpoint + Ekim ayı retrosu", 75, "reflect"),
]),
("h7", "Hafta 7", "2-8 Kasım", [
    ("uwc", "Başvuruyu gözden geçir ve gönder (tarih Hafta 0'da teyit edilen son tarihe göre kayabilir)", 90, "produce"),
    ("p1", "Stokastik kanal gürültüsünü ikinci nörona da uygula", 75, "produce"),
    ("okul", "Konu tekrarı", 45, "review"),
    ("jp", "N5 kelime seti 6", 15, "review"),
    ("p1", "İki nöronu sinaptik olarak bağlama — bağlantı mimarisi tasarımı", 75, "study"),
    ("fel", "Kulüp 5. toplantı (artık UWC gönderildiği için tema: kulübün kendi bağımsız hedefi)", 45, "explain"),
    ("bb", "İşitme/görme sistemleri", 15, "study"),
    ("sat", "Tam deneme sınavı (tek oturum, mümkünse)", 130, "test"),
    ("ozdeg", "Video günlüğü #7 + checkpoint", 30, "reflect"),
]),
("h8", "Hafta 8 (sınav haftası olasılığı)", "9-15 Kasım", [
    ("uwc", "Mülakat olasılığına karşı hazırlık notları (kendi hikayeni 3 dk'da anlatma pratiği)", 60, "produce"),
    ("p1", "Sinaptik bağlantıyı kodla, ilk ortak simülasyon testi", 75, "produce"),
    ("okul", "Sınav odaklı yoğun tekrar (bu hafta yoğun/ara sınav olasılığı)", 60, "review"),
    ("jp", "Bu hafta esnek/atlanabilir", 15, "review"),
    ("okul", "Sınav odaklı yoğun tekrar (P1 bu hafta hafif)", 60, "review"),
    ("fel", "Kulüp toplantısı (okul sınav haftasıysa kısa tutulabilir)", 45, "explain"),
    ("bb", "Hafıza sistemleri", 15, "study"),
    ("sat", "Bu hafta okula öncelik verildiği için hafif — 1 bölüm pratik", 60, "test"),
    ("ozdeg", "Video günlüğü #8 + checkpoint (sınav haftası nasıl geçti?)", 30, "reflect"),
]),
("h9", "Hafta 9", "16-22 Kasım", [
    ("uwc", "Kulüp için yeni dönem içeriği planlama (UWC sonrası bağımsız hedef)", 60, "produce"),
    ("p1", "İki nöron simülasyonunda faz kilitleme / senkronizasyon ölçümü — ilk analiz", 75, "produce"),
    ("okul", "Konu tekrarı", 45, "review"),
    ("jp", "N5 kelime seti 7", 15, "review"),
    ("p1", "Cross-correlation analizi, parametre taraması (bağlantı gücü değiştikçe senkronizasyon nasıl değişiyor)", 75, "produce"),
    ("fel", "Kulüp toplantısı", 45, "explain"),
    ("bb", "Nörolojik hastalıklara giriş", 15, "study"),
    ("apcalc", "Belirli integral, Analizin Temel Teoremi", 100, "study"),
    ("ozdeg", "Video günlüğü #9 + checkpoint", 30, "reflect"),
]),
("h10", "Hafta 10", "23-29 Kasım", [
    ("uwc", "Kulüp çıktısını yazılı hale getir (rapor/blog — portföy için)", 60, "produce"),
    ("p1", "Senkronizasyon sonuçlarını grafikle özetle, sonuçlar bölümü taslağı", 75, "produce"),
    ("okul", "Konu tekrarı", 45, "review"),
    ("jp", "N5 kelime seti 8", 15, "review"),
    ("p1", "Rapor/yazım — giriş ve yöntem bölümlerini birleştir", 75, "produce"),
    ("fel", "Kulüp toplantısı", 45, "explain"),
    ("bb", "Bilişsel nörobilim giriş", 15, "study"),
    ("sat", "Tam pratik test #2", 130, "test"),
    ("ozdeg", "Video günlüğü #10 + checkpoint + Kasım ayı retrosu", 75, "reflect"),
]),
("h11", "Hafta 11", "30 Kasım – 6 Aralık", [
    ("uwc", "Kulüp toplantısı + dönem sonu kulüp özet raporu başlangıcı", 60, "explain"),
    ("p1", "Raporun tartışma/sonuç bölümü taslağı", 75, "produce"),
    ("okul", "Konu tekrarı", 45, "review"),
    ("jp", "N5 kelime seti 9", 15, "review"),
    ("p1", "Destek Eğitim Odası'na sunum için slayt/görsel hazırlığı", 75, "produce"),
    ("fel", "Kulüp toplantısı", 45, "explain"),
    ("bb", "Genel tekrar seti 1", 15, "review"),
    ("apchem", "Kimyasal bağlar, moleküler geometri", 90, "study"),
    ("ozdeg", "Video günlüğü #11 + checkpoint", 30, "reflect"),
]),
("h12", "Hafta 12", "7-13 Aralık", [
    ("uwc", "Kulüp özet raporunu bitir", 60, "produce"),
    ("p1", "Rapor/sunum revizyonu — geri bildirim al (danışman/öğretmen)", 75, "review"),
    ("okul", "Konu tekrarı", 45, "review"),
    ("jp", "N5 kelime seti 10", 15, "review"),
    ("p1", "Geri bildirime göre son düzeltmeler", 75, "produce"),
    ("fel", "Kulüp toplantısı", 45, "explain"),
    ("bb", "Genel tekrar seti 2", 15, "review"),
    ("sat", "Tam pratik test #3", 130, "test"),
    ("ozdeg", "Video günlüğü #12 + checkpoint", 30, "reflect"),
]),
("h13", "Hafta 13 (dönem sonu sınav haftası)", "14-20 Aralık", [
    ("p1", "Sunum/rapor final teslim (Destek Eğitim Odası)", 90, "produce"),
    ("okul", "Dönem sonu sınavlarına (varsa) yoğunlaşma", 75, "review"),
    ("okul", "Sınav tekrarı", 45, "review"),
    ("jp", "N5 genel tekrar", 15, "review"),
    ("okul", "Sınav tekrarı", 75, "review"),
    ("apchem", "Termokimya — final sınavına göre hafif tekrar", 60, "review"),
    ("serbest", "Bu hafta okul öncelikli — kaçan modülleri buraya taşı", 60, "review"),
    ("ozdeg", "Video günlüğü #13 + checkpoint", 30, "reflect"),
]),
("h14", "Hafta 14 (yarıyıl tatili başlangıcı olası)", "21-27 Aralık", [
    ("uwc", "Mülakat çağrısı geldiyse hazırlık; gelmediyse tam dinlenme", 45, "review"),
    ("bb", "Ocak dönemine hazırlık — konu haritası çıkar (hangi başlıklar eksik)", 45, "study"),
    ("serbest", "İstersen tamamen boş bırak", 0, "review"),
    ("p1", "Rapor sonrası “ileri adımlar” notu (Ocak'ta nereden devam edilecek)", 45, "produce"),
    ("serbest", "İstersen tamamen boş bırak", 0, "review"),
    ("apcalc", "Hafif — kendi seçtiğin zayıf konudan 1 tekrar (istersen atla)", 45, "review"),
    ("ozdeg", "Video günlüğü #14 + checkpoint", 30, "reflect"),
]),
("h15", "Hafta 15 (dönem sonu)", "28-31 Aralık", [
    ("serbest", "Ocak-Haziran için müfredat ihtiyacını değerlendir (P1 sonrası ne? P2 Hopfield network'e mi geçilecek?)", 45, "reflect"),
    ("ozdeg", "Dönem sonu büyük retrospektif: 4 aylık video günlüklerinden seçmeler izle, 5 alanda 1-10 puanlama, en büyük 3 ders çıkarımı, Ocak müfredatı için karar", 105, "reflect"),
]),
]

PHASES = [
    ("eylul", "Eylül · Kurulum ve Keşif", "18-30 Eylül 2026", "Sistemi oturt, UWC yönünü netleştir, P1'in teorik temelini at.", 0, 3),
    ("ekim", "Ekim · Derinleşme", "1-31 Ekim 2026", "P1'i çok-kompartmanlı hale getir, UWC dosyasını bitir, SAT zayıf alanlarını sistemli çalış, felsefe kulübünü düzenli işlet.", 3, 7),
    ("kasim", "Kasım · Tamamlama ve Sıkılaştırma", "1-30 Kasım 2026", "UWC'yi gönder, P1'de stokastik gürültü + iki-nöron senkronizasyonuna geç, okulun yoğun (muhtemel ara sınav) haftasını yönet.", 7, 11),
    ("aralik", "Aralık · Bitirme, Toparlama, Dinlenme", "1-31 Aralık 2026", "P1 raporunu/sunumunu bitir, dönem sonu değerlendirmesi yap, tatile makul bir bitiş çizgisiyle gir.", 11, 16),
]

UNIT_BRIDGES = {
    "h1": ["Kablo Teorisi Köprüsü"],
    "h6": ["Stokastik Süreçler ↔ Kanal Gürültüsü"],
    "h9": ["Dinamik Sistemler ↔ Senkronizasyon"],
}

def build_unit(w):
    uid, title, kicker, tasks = w
    return {
        "id": uid,
        "title": title,
        "kicker": kicker,
        "br": UNIT_BRIDGES.get(uid, []),
        "tasks": [
            {"i": f"{uid}-t{idx+1:02d}", "t": text, "s": subj, "k": kind, "m": minutes}
            for idx, (subj, text, minutes, kind) in enumerate(tasks)
        ],
    }

phases = []
for pid, name, sub, goal, start, end in PHASES:
    units = [build_unit(w) for w in WEEKS[start:end]]
    hours = round(sum(t["m"] for u in units for t in u["tasks"]) / 60)
    phases.append({"id": pid, "name": name, "sub": sub, "goal": goal, "hours": hours, "units": units})

milestones_note = (
    "Eylül sonu: UWC deneme taslağı v1 tamam, P1 tek-kompartman model çalışıyor, SAT zayıf alanlar netleşti. "
    "Ekim sonu: UWC dosyası tamamlanmaya yakın, P1 çok-kompartmanlı+kalibre model çalışıyor, felsefe kulübü 4 toplantı geride. "
    "Kasım sonu: UWC başvurusu gönderildi, P1'de iki nöron senkronizasyon analizi tamam, SAT'ta en az 2 tam deneme birikti. "
    "Aralık sonu: P1 raporu teslim edildi, UWC süreci tamamlandı, felsefe kulübü dönem özeti yazıldı, SAT'ta 3 tam deneme."
)
phases[-1]["units"][-1]["note"] = milestones_note

data = {
    "version": 1,
    "title": "Beril · Eylül–Aralık 2026 Müfredatı",
    "phases": phases,
    "projects": [
        {
            "id": "P1", "name": "Hodgkin-Huxley Çok-Kompartmanlı Nöron Modeli", "emoji": "🧠",
            "goal": "Gerçek nöron morfolojisi (NeuroMorpho.org) ve elektrofizyoloji verisiyle (Allen Brain Atlas) kablo-teorisi tabanlı, stokastik iyon kanalı gürültülü, iki sinaptik bağlı nöron simülasyonu — Destek Eğitim Odası resmi projesi.",
            "phases": {
                "eylul": "Teorik temel: cable theory, tek-kompartmanlı HH modeli",
                "ekim": "Çok-kompartmanlı model + gerçek veriyle kalibrasyon",
                "kasim": "Stokastik gürültü + iki nöron sinaptik bağlantı + senkronizasyon analizi",
                "aralik": "Rapor/sunum yazımı ve final teslimi",
            },
            "topics": ["Cable theory", "Hodgkin-Huxley denklemleri", "Çok-kompartmanlı modelleme",
                       "Stokastik iyon kanalı gürültüsü (Markov modeli)", "Sinaptik bağlantı", "Senkronizasyon analizi"],
            "next": "Cable theory teorik özetini çıkar ve tek-kompartmanlı denklemi kağıt üzerinde türet.",
            "ms": ["Tek-kompartman model çalışıyor", "Çok-kompartmanlı + kalibre model çalışıyor",
                   "İki nöron senkronizasyon analizi tamam", "Rapor/sunum final teslimi"],
        },
        {
            "id": "P3", "name": "UWC Başvurusu ve Felsefe Kulübü", "emoji": "🌍",
            "goal": "UWC başvurusu birincil öncelik; felsefe kulübü, başvuru sonrası da bağımsız bir hedefle sürdürülen bir topluluk katkısı kanıtı.",
            "phases": {
                "eylul": "Son başvuru tarihini teyit et, deneme taslağı v1",
                "ekim": "Deneme v2, başvuru formu, referans süreci",
                "kasim": "Başvuruyu gönder, mülakat hazırlığı",
                "aralik": "Kulüp dönem özet raporu",
            },
            "topics": ["Başvuru denemesi", "Başvuru formu", "Referans mektupları", "Mülakat hazırlığı", "Felsefe kulübü toplantıları"],
            "next": "Türkiye Ulusal Komitesi sitesinden kesin son başvuru tarihini teyit et.",
            "ms": ["Deneme taslağı v1 tamam", "Başvuru formu tamam", "Başvuru gönderildi", "Kulüp dönem özeti yazıldı"],
        },
    ],
    "assessments": [],
    "bridges": [
        {"n": "Kablo Teorisi Köprüsü", "e": "🔗", "d": "PDE + devre teorisi + gerçek nöron morfolojisi (P1'in matematiksel temeli).", "t": ["Cable theory", "P1"]},
        {"n": "Stokastik Süreçler ↔ Kanal Gürültüsü", "e": "🔗", "d": "Markov modeli = iyon kanalının açılıp kapanması.", "t": ["Markov modeli", "İyon kanalı gürültüsü", "P1"]},
        {"n": "Dinamik Sistemler ↔ Senkronizasyon", "e": "🔗", "d": "Salınım + diferansiyel denklem + iki nöron senkronu.", "t": ["Faz kilitleme", "Senkronizasyon", "P1"]},
    ],
    "resources": [
        {"s": "p1", "n": "NeuroMorpho.org", "u": "Gerçek nöron morfolojisi verisi"},
        {"s": "p1", "n": "Allen Brain Atlas", "u": "Elektrofizyoloji verisi"},
        {"s": "sat", "n": "SAT resmi deneme sınavları", "u": "Diagnostic ve tam pratik testler"},
        {"s": "apcalc", "n": "Kendi türev/trigonometri/polinom siteleri", "u": "AP Kalkülüs tekrarı"},
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
print("total hours =", sum(t["m"] for p in phases for u in p["units"] for t in u["tasks"]) / 60)
