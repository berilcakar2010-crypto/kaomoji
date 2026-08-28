#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
(≧▽≦) — curriculum generator v3
Tüm okul yılı: 26 Ağustos 2026 – Haziran 2027 (43 hafta).
v2'den farkları:
  1. "sch" (10. Sınıf, tek blok) kaldırıldı → Validebağ'ın zorunlu dersleri
     (Maarif Model 10. sınıf) TEK TEK sekmelere bölündü, her biri kendi
     üretim formatına ve checkpoint sıklığına sahip (bkz. SCHOOL_SUBJECTS).
  2. P1 (Destek Eğitim Odası resmi projesi) kapsamı büyütüldü: gerçek nöron
     morfolojisi + elektrofizyoloji verisiyle çok bölmeli kablo-teorisi
     modeli, stokastik iyon kanalı dinamiği, iki sinaptik bağlı nöron,
     senkronizasyon analizi, gerçek veriyle doğrulama. Nihan Alp / renk
     illüzyonu hattı tamamen kaldırıldı.
  3. Yeni "self" dersi: haftalık öz değerlendirme + video günlüğü (kind=reflect).
  4. Japonca artık HER HAFTA sabit, kısa (20 dk) bir tekrar görevi (kind=review) —
     yeni ünite yok, sadece kelime tekrarı.
  5. 6 faz, HH projesinin aylık milestone tablosuna hizalı (bkz. PHASE_META).
  6. Okul dersi ünite başlıkları artık MEB'in resmi Türkiye Yüzyılı Maarif Modeli
     10. sınıf programından (tymm.meb.gov.tr) alınmış gerçek ünite/tema adları
     (bkz. UNIT_TITLES). Ünite sırası her ders için kendi cadence'ine göre
     ilerler ve liste bitince başa sarar — okulun kendi yıllık planındaki
     sıralama farklıysa UNIT_TITLES'taki listeyi buna göre düzenle.

Schema, v2 ile birebir aynı (Models.kt / CurriculumLoader.kt DEĞİŞMEDİ).
"""
import json, os

WEEKS = 43  # 26 Ağu 2026 – ~13 Haz 2027

# ── 1. Ray A: ileri düzey üretim — haftalık konu yol haritaları ─────
# v2'nin 30 haftalık listesi + 13 hafta uzatma (Şub-Haz): elektromanyetizma
# devamı/optik, çok değişkenli kalkülüs, sinkron/sistem nörobilimi.
physics = (
    ["Ch1 Ölçme (Measurement)", "Ch2 Doğrusal Hareket", "Ch3 Vektörler",
     "Ch4 2B/3B Hareket", "Ch5 Kuvvet ve Hareket I", "Ch6 Kuvvet ve Hareket II",
     "Ch7 Kinetik Enerji ve İş", "Ch8 Potansiyel Enerji ve Enerji Korunumu",
     "Ch9 Kütle Merkezi ve Momentum", "Ch10 Dönme", "Ch11 Yuvarlanma, Tork, Açısal Momentum",
     "Ch12 Denge ve Esneklik", "Ch13 Kütleçekimi", "Ch13 Kütleçekimi — tekrar & problem seti",
     "Ch14 Akışkanlar", "Ch15 Salınımlar", "Ch15 Salınımlar — tekrar & problem seti",
     "Ch16 Dalgalar I", "Ch16 Dalgalar I — tekrar", "Ch17 Dalgalar II",
     "Ch17 Dalgalar II — tekrar", "Genel tekrar (Ch1-13 mekanik)", "Sınav sonrası toparlama",
     "Ch18 Sıcaklık ve Isı", "Ch19 Gazların Kinetik Teorisi", "Ch20 Entropi ve 2. Yasa",
     "Ch21 Elektrik Yükü", "Ch22 Elektrik Alanlar", "Genel tekrar (Ch14-22)",
     "P2 portföy toparlama (Faz 3)"] +
    ["Ch23 Gauss Yasası", "Ch24 Elektrik Potansiyeli", "Ch25 Kapasitans",
     "Ch26 Akım ve Direnç", "Ch27 Devreler", "Ch28 Manyetik Alanlar",
     "Ch29 Akım Kaynaklı Manyetik Alan", "Genel tekrar (elektromanyetizma)",
     "Ch33 Elektromanyetik Dalgalar", "Ch34 Görüntüler (Geometrik Optik)",
     "Ch35 Girişim", "Genel tekrar (optik)", "P2 portföy final derlemesi"]
)
math = (
    ["Ch1 Fonksiyonlar ve Modeller (tekrar)", "Ch2 Limit ve Türev — başlangıç", "Ch2 Limit ve Türev — bitiş",
     "Ch3 Türev Alma Kuralları (1/3)", "Ch3 Türev Alma Kuralları (2/3)", "Ch3 Türev Alma Kuralları (3/3)",
     "Ch4 Türevin Uygulamaları (1/3)", "Ch4 Türevin Uygulamaları (2/3)", "Ch4 Türevin Uygulamaları (3/3)",
     "Ch5 İntegraller (1/2)", "Ch5 İntegraller (2/2)", "Ch5 tekrar & problem seti",
     "Ch6 İntegral Uygulamaları (1/2)", "Ch6 İntegral Uygulamaları (2/2)",
     "Ch6 tekrar", "Ch7 İntegral Teknikleri — giriş",
     "Ch7 İntegral Teknikleri — tekrar", "Genel tekrar (Ch2-6)", "Genel tekrar (Ch2-6)",
     "Ch9 Diferansiyel Denklemler — giriş", "Ch9 Diferansiyel Denklemler — tekrar",
     "Genel tekrar (integral teknikleri)", "Sınav sonrası toparlama",
     "Ch9 Diferansiyel Denklemler — devam", "Strang Lineer Cebir Ch1 (vektörler/matrisler)",
     "Strang Lineer Cebir Ch2 (Ax=b çözümü)", "Strang Ch5 — özdeğerler (dinamik sistemler için)",
     "Stewart Ch13 Vektör Fonksiyonları", "Genel tekrar (diferansiyel denklem + lineer cebir)",
     "Faz 3 portföy toparlama"] +
    ["Stewart Ch14 Çok Değişkenli Fonksiyonlar (1/2)", "Stewart Ch14 Çok Değişkenli Fonksiyonlar (2/2)",
     "Stewart Ch15 Çoklu İntegraller (1/2)", "Stewart Ch15 Çoklu İntegraller (2/2)",
     "Kısmi diferansiyel denklemlere giriş (kablo denklemi için)", "PDE — sayısal çözüm yöntemleri",
     "Strang Ch6 — pozitif tanımlı matrisler", "Genel tekrar (çok değişkenli kalkülüs)",
     "Olasılık/istatistik temelleri (kanal gürültüsü analizi için)",
     "Stokastik süreçlere giriş (Poisson süreci)", "Faz 4 tekrar",
     "Genel tekrar (PDE + stokastik)", "Final portföy derlemesi"]
)
bio = (
    ["Campbell Ch2 Yaşamın Kimyasal Bağlamı", "Campbell Ch3 Su ve Yaşamın Uygunluğu",
     "Python: değişkenler, döngüler, fonksiyonlar",
     "Campbell Ch6 Hücreye Bir Tur", "Campbell Ch7 Hücre Zarı ve Taşınım (nöron potansiyeli temeli)",
     "Campbell Ch8 Metabolizmaya Giriş", "Python: OOP temelleri",
     "Campbell Ch48 Nöronlar, Sinapslar, Sinyalleşme (1/3)", "Campbell Ch48 (2/3)", "Campbell Ch48 (3/3) + quiz",
     "Campbell Ch49 Sinir Sistemleri (1/2)", "Campbell Ch49 (2/2)",
     "Python: sayısal yöntemler (Euler, Runge-Kutta girişi)", "P1 proje iskeleti (HH model kurulumu)",
     "Kandel Ch1 Nöron biyolojisine giriş", "Kandel Ch2 Sinir hücreleri ve davranış",
     "Tekrar & konsolidasyon (Campbell+Kandel)", "Kandel Ch3 Sinaptik iletim temelleri",
     "Tekrar & konsolidasyon", "Genel tekrar (biyoloji temelleri)", "Genel tekrar (nöron biyolojisi)",
     "Python: Hodgkin-Huxley denklemlerini kodlamaya başlama", "Sınav sonrası toparlama",
     "Kandel Ch7-8 İyon kanalları ve aksiyon potansiyeli", "Kablo teorisi: nöron morfolojisini modele bağlama",
     "NeuroMorpho.org'dan morfoloji dosyası seçimi ve ön işleme",
     "Allen Brain Atlas'tan elektrofizyoloji verisi çekme",
     "Çok bölmeli (multi-compartment) model — ilk parametre eşleştirme",
     "Kandel Ch9 Aksiyon potansiyeli yayılımı", "Faz 3 raporu: tek nöron modeli + veriyle karşılaştırma"] +
    ["Stokastik iyon kanalı teorisi (Markov modeli)", "Kanal gürültüsünü modele entegre etme",
     "İki nöron: sinaptik bağlantı mimarisi (elektriksel/kimyasal sinaps)",
     "Sinaptik ağırlık ve gecikme parametreleri", "Kanal gürültüsü — spike zamanlaması taraması (1/2)",
     "Kanal gürültüsü — spike zamanlaması taraması (2/2)",
     "Ateşleme davranışı analizi (rate coding vs. temporal coding)",
     "Genel tekrar (stokastik kanal + sinaptik bağlanma)",
     "Senkronizasyon analizi — faz kilitlenmesi/faz kayması",
     "Gerçek veriyle nihai karşılaştırma, biyolojik gerçeklik değerlendirmesi",
     "P1 final rapor yazımı", "P1 final rapor + görselleştirme",
     "BEP yıl sonu sunumu hazırlığı"]
)
assert len(physics) == len(math) == len(bio) == WEEKS, (len(physics), len(math), len(bio))

def phase_of(wk):
    if wk <= 9: return "p1"
    if wk <= 17: return "p2"
    if wk <= 21: return "p3"
    if wk <= 30: return "p4"
    if wk <= 39: return "p5"
    return "p6"

PHASE_META = {
    "p1": {"name": "Faz 1 · Kuruluş", "sub": "26 Ağu – Eki başı", "hours": 200,
           "goal": "Temel kavramlar, Python temelleri, kablo teorisi matematiği + gerçek nöron morfolojisi seçimi."},
    "p2": {"name": "Faz 2 · Çok Bölmeli Model", "sub": "Eki – Kas", "hours": 178,
           "goal": "Çok bölmeli (multi-compartment) HH modeli kuruluyor, Allen Brain Atlas verisiyle ilk parametre eşleştirme."},
    "p3": {"name": "Faz 3 · Güz Konsolidasyonu", "sub": "Ara – Oca", "hours": 84,
           "goal": "Sınav dönemi — tempo bilerek düşük. Tek nöron modeli + gerçek veriyle karşılaştırma raporu (BEP güz değerlendirmesi)."},
    "p4": {"name": "Faz 4 · Stokastik Genişleme", "sub": "Şub – Mar", "hours": 189,
           "goal": "Stokastik iyon kanalı gürültüsü ve iki sinaptik bağlı nöron modele ekleniyor."},
    "p5": {"name": "Faz 5 · Senkronizasyon ve Sentez", "sub": "Nis – May", "hours": 189,
           "goal": "Kanal gürültüsünün spike zamanlaması/senkronizasyon üzerindeki etkisi taranıyor, gerçek veriyle nihai karşılaştırma yapılıyor."},
    "p6": {"name": "Faz 6 · Final", "sub": "Haz", "hours": 63,
           "goal": "P1 final raporu + BEP yıl sonu sunumu + portföylerin (P2) kapanışı."},
}

# ── 2. RAY B — okul dersleri (Validebağ zorunlu dersler, Maarif Model 10. sınıf) ──
# Ünite adları MEB'in resmi Türkiye Yüzyılı Maarif Modeli programından alındı
# (tymm.meb.gov.tr/ogretim-programlari/<ders>/12 — "12" = 10. sınıf kodu).
# cadence: bu ders kaç haftada bir bir üretim görevi alıyor (markdown planındaki
# dönemlik/aylık checkpoint sıklığından türetildi); ünite listesi bittiğinde başa sarar.
UNIT_TITLES = {
    "msch": ["Geometrik Şekiller", "İstatistiksel Araştırma Süreci", "Sayılar",
             "Nicelikler ve Değişimler", "Sayma, Algoritma ve Bilişim",
             "Analitik İnceleme", "Veriden Olasılığa"],
    "psch": ["Kuvvet ve Hareket", "Enerji", "Elektrik", "Dalgalar"],
    "kim": ["Etkileşim (kimyasal tepkimeler, gazlar)", "Çeşitlilik (karışımlar, çözeltiler)",
            "Sürdürülebilirlik (çevre, atom ekonomisi)"],
    "bsch": ["Enerji", "Ekoloji"],
    "tde": ["Sözün Ezgisi", "Kelimelerin Ritmi", "Dünden Bugüne", "Nesillerin Mirası"],
    "tar": ["Türkistan'dan Türkiye'ye (1040-1299)", "Beylikten Devlete Osmanlı (1299-1453)",
            "Cihan Devleti Osmanlı (1453-1683)"],
    "cog": ["Coğrafyanın Doğası", "Mekânsal Bilgi Teknolojileri", "Doğal Sistemler ve Süreçler",
            "Beşerî Sistemler ve Süreçler", "Ekonomik Faaliyetler ve Etkileri",
            "Afetler ve Sürdürülebilir Çevre", "Bölgeler, Ülkeler ve Küresel Bağlantılar"],
    "fel": ["Felsefenin Doğası", "Felsefe, Mantık ve Argümantasyon", "Varlık Felsefesi",
            "Bilgi Felsefesi", "Ahlak Felsefesi", "Estetik ve Sanat Felsefesi",
            "Siyaset Felsefesi", "Din Felsefesi", "Bilim Felsefesi"],
    "din": ["İslam'da Varlık ve Bilgi", "Allah'ı Tanımak", "İslam'ın Evrensel Mesajları",
            "Din, Çevre ve Teknoloji", "İslam Düşüncesinde İtikadi-Siyasi ve Fıkhi Yorumlar"],
    "ing": ["School Life & Education", "Classroom Life & Learning", "Personal Life & Well-Being",
            "Family Life & Home", "Life in the Neighbourhood, City & Social Life",
            "Life in the World & Culture", "Life in Nature & Global Problems",
            "Life in the Universe & The Future"],  # B2.2 (hazırlık sonrası 10. sınıf)
}
SCHOOL_SUBJECTS = {
    "msch": {"name": "Matematik (okul)", "emoji": "📐", "col": "#9FCB8B", "cadence": 3,
             "kind": "write", "unit_label": "köprü notu",
             "text": "Matematik (okul) — {u}: MEB konusunu Ray A'daki ileri versiyona bağlayan kısa köprü notu"},
    "psch": {"name": "Fizik (okul)", "emoji": "🔺", "col": "#E08A73", "cadence": 4,
             "kind": "produce", "unit_label": "olimpiyat sorusu",
             "text": "Fizik (okul) — {u}: konuyu bir olimpiyat sorusuna dönüştür + çöz (P2 portföyüne ekle)"},
    "kim": {"name": "Kimya", "emoji": "🧪", "col": "#7FA6C9", "cadence": 4,
            "kind": "write", "unit_label": "vaka analizi",
            "text": "Kimya — {u}: gerçek dünya vakası analizi (tek sayfa)"},
    "bsch": {"name": "Biyoloji (okul)", "emoji": "🍃", "col": "#6FAE7C", "cadence": 6,
             "kind": "write", "unit_label": "P1 bağlantı notu",
             "text": "Biyoloji (okul) — {u}: konunun P1'in biyolojik gerçeklik bölümüne referans notu"},
    "tde": {"name": "Türk Dili ve Edebiyatı", "emoji": "📖", "col": "#C99A3E", "cadence": 2,
            "kind": "write", "unit_label": "metin tahlili",
            "text": "Türk Dili ve Edebiyatı — {u}: temasından okunan metin için 300-400 kelimelik özgün tahlil"},
    "tar": {"name": "Tarih", "emoji": "🏛️", "col": "#A9764E", "cadence": 6,
            "kind": "produce", "unit_label": "zaman çizelgesi",
            "text": "Tarih — {u}: zaman çizelgesi + UWC küresel vatandaşlık teması bağlantı notu"},
    "cog": {"name": "Coğrafya", "emoji": "🗺️", "col": "#5B8FA8", "cadence": 9,
            "kind": "produce", "unit_label": "görselleştirme",
            "text": "Coğrafya — {u}: konuyla ilgili harita/grafik görselleştirmesi"},
    "fel": {"name": "Felsefe", "emoji": "🦉", "col": "#8E7FB8", "cadence": 6,
            "kind": "write", "unit_label": "kulüp sorusu",
            "text": "Felsefe — {u}: felsefe kulübü için tartışma sorusu önerisi + kısa gerekçe"},
    "din": {"name": "Din Kültürü ve Ahlak Bilgisi", "emoji": "🕊️", "col": "#B8A98E", "cadence": 4,
            "kind": "review", "unit_label": "özet kart seti",
            "text": "Din Kültürü — {u}: kısa özet kart seti (flashcard formatında)"},
    "ing": {"name": "İngilizce", "emoji": "🇬🇧", "col": "#4E7FA8", "cadence": 1,
            "kind": "write", "unit_label": "yazılı",
            "text": "İngilizce — {u} temalı kısa yazılı üretim (essay/summary, B2.2→C1)"},
}
# Almanca (A1+) ve Beden/Sanat/Sağlık grubu ayrı ele alınıyor (aşağıda).

BAK_GROUP = {"name": "Beden/Sanat/Sağlık", "emoji": "🎨", "col": "#AAAAAA"}  # bakım grubu, üretim baskısı yok

def school_task_for(code, occurrence_n):
    d = SCHOOL_SUBJECTS[code]
    titles = UNIT_TITLES[code]
    unit_title = titles[(occurrence_n - 1) % len(titles)]
    return {"s": code, "k": d["kind"], "t": d["text"].format(u=unit_title), "m": 45}

# ── 3. Diğer sabit haftalık görevler ────────────────────────────────
def jp_task():
    return {"s": "jp", "k": "review", "t": "Japonca — haftalık kelime tekrarı (yeni ünite yok, sadece pekiştirme)", "m": 20}

def de_task(wk):
    # Almanca A1+ — haftada 10 kelime + 3 cümlelik günlük mikro-metin
    return {"s": "de", "k": "write", "t": "Almanca — 10 yeni kelime + 3 cümlelik günlük mikro-metin (A1+)", "m": 25}

def self_task():
    return {
        "s": "self", "k": "reflect",
        "t": "Haftalık öz değerlendirme (60 dk): 10 dk video günlüğü + 15 dk sayısal özet "
             "(checkpoint'ler tamam mı) + 15 dk tek engel analizi + 10 dk gelecek hafta 3 hedef "
             "+ arşivleme. Video izleme yok — sadece kayıt (ay sonunda toplu izleniyor).",
        "m": 60,
    }

def bak_task(wk):
    # sadece asgari takip, checkpoint baskısı yok — 4 haftada bir hafif hatırlatma
    return {"s": "bak", "k": "study", "t": "Beden Eğitimi / Görsel Sanatlar-Müzik / Sağlık Bilgisi — haftalık asgari takip", "m": 20}

def prod_task():
    return {"s": "prod", "k": "produce",
            "t": "P1 (destek eğitim odası — çok bölmeli HH modeli) / P2 (fizik portföyü) / P3 (UWC & felsefe kulübü) — dönüşümlü",
            "m": 60}

SRC = {
    "phys": "Halliday, Fundamentals of Physics (EN)",
    "math": "Stewart Calculus / Strang Lineer Cebir / MEB",
    "bio": "Campbell Biology / Kandel Principles of Neural Science (6.bs) / NeuroMorpho.org / Allen Brain Atlas",
    "py": "Python (proje kodu)",
    "jp": "Genki I-II / Minna no Nihongo (haftalık tekrar)",
    "de": "Menschen A1-A2",
    "prod": "P1/P2/P3 üretim",
}
for c, d in SCHOOL_SUBJECTS.items():
    SRC[c] = "Okul müfredatı (Maarif Model 10. sınıf, MEB)"
SRC["bak"] = "Okul müfredatı (MEB)"
SRC["self"] = "Haftalık öz değerlendirme protokolü"

# ── 4. Haftalık birimleri üret ──────────────────────────────────────
units_by_phase = {p: [] for p in PHASE_META}
school_occurrence = {c: 0 for c in SCHOOL_SUBJECTS}

for wk in range(1, WEEKS + 1):
    idx = wk - 1
    phid = phase_of(wk)
    tasks = []

    # Ray A — her hafta sabit blok
    tasks.append({"i": "u%02d-t01" % wk, "s": "phys", "k": "problem",
                   "t": "Fizik — %s: konu anlatımı + örnek problemler + bölüm sonu 8-10 soru" % physics[idx], "m": 60})
    tasks.append({"i": "u%02d-t02" % wk, "s": "math", "k": "problem",
                   "t": "Matematik — %s: teori + 10-12 alıştırma + 2 türetim" % math[idx], "m": 60})
    tasks.append({"i": "u%02d-t03" % wk, "s": "bio", "k": "read",
                   "t": "Biyoloji/Nörobilim — %s: oku, özetle, 5-6 kavrama sorusu" % bio[idx], "m": 60})
    p_t = prod_task(); p_t["i"] = "u%02d-t04" % wk
    tasks.append(p_t)

    # Ray B — rotasyonla dağıtılan okul dersleri (cadence'e göre bu hafta sırası gelenler)
    for code, d in SCHOOL_SUBJECTS.items():
        if wk % d["cadence"] == 0 or wk == 1:
            school_occurrence[code] += 1
            t = school_task_for(code, school_occurrence[code])
            t["i"] = "u%02d-t-%s" % (wk, code)
            tasks.append(t)

    # Almanca (haftalık), Japonca (haftalık, hafif), Beden/Sanat (4 haftada bir), Öz değerlendirme (haftalık)
    de_t = de_task(wk); de_t["i"] = "u%02d-t-de" % wk; tasks.append(de_t)
    jp_t = jp_task(); jp_t["i"] = "u%02d-t-jp" % wk; tasks.append(jp_t)
    if wk % 4 == 0:
        bak_t = bak_task(wk); bak_t["i"] = "u%02d-t-bak" % wk; tasks.append(bak_t)
    self_t = self_task(); self_t["i"] = "u%02d-t-self" % wk; tasks.append(self_t)

    title = "Hafta %d · %s" % (wk, physics[idx].split(" — ")[0].split(" (")[0])
    kicker = "%s · %s" % (PHASE_META[phid]["name"], math[idx].split(" (")[0])
    used_subjects = {t["s"] for t in tasks}
    note = "Kaynaklar: " + "; ".join(sorted({SRC[s] for s in used_subjects if s in SRC}))

    unit = {"id": "u%02d" % wk, "title": title, "kicker": kicker, "tasks": tasks, "note": note}
    units_by_phase[phid].append(unit)

phases = []
for phid in ["p1", "p2", "p3", "p4", "p5", "p6"]:
    m = PHASE_META[phid]
    phases.append({"id": phid, "name": m["name"], "sub": m["sub"], "goal": m["goal"],
                    "hours": m["hours"], "units": units_by_phase[phid]})

# ── 5. Üretim omurgası (P1 kapsamı büyütüldü) ───────────────────────
projects = [
    {"id": "P1", "name": "Destek Eğitim Odası — Çok Bölmeli HH Nöron Modeli", "emoji": "🧠",
     "goal": ("Gerçek bir nöronun morfolojik rekonstrüksiyonu (NeuroMorpho.org) ve elektrofizyolojik "
              "kayıtlarını (Allen Brain Atlas) kullanarak, kablo teorisi ve Hodgkin-Huxley iyon kanal "
              "dinamiklerine dayanan çok bölmeli bir nöron modeli kurmak; iyon kanallarının stokastik "
              "açılıp kapanmasını modele dahil etmek; iki sinaptik bağlı nöronda kanal gürültüsünün spike "
              "zamanlaması, ateşleme davranışı ve senkronizasyon üzerindeki etkisini araştırmak; sonuçları "
              "gerçek veriyle karşılaştırıp modelin biyolojik gerçekliğini değerlendirmek."),
     "phases": {
         "p1": "Kablo teorisi matematiği + morfoloji dosyası seçimi (NeuroMorpho.org)",
         "p2": "Çok bölmeli deterministik model + Allen Brain Atlas ile ilk parametre eşleştirme",
         "p3": "Güz raporu: tek nöron modeli + gerçek veriyle karşılaştırma (BEP güz değerlendirmesi)",
         "p4": "Stokastik iyon kanalı gürültüsü + iki nöronun sinaptik bağlanması",
         "p5": "Spike zamanlaması taraması + senkronizasyon analizi + nihai veri karşılaştırması",
         "p6": "Final rapor + BEP yıl sonu sunumu"},
     "topics": ["Kablo Teorisi", "Hodgkin-Huxley", "Stokastik Süreçler", "Senkronizasyon", "Python", "Nörobilim"],
     "next": "NeuroMorpho.org'dan bir piramidal nöron morfolojisi seç ve kablo denkleminin sayısal iskeletini kur",
     "ms": ["Morfoloji dosyası seçildi ve ön işlendi", "Çok bölmeli deterministik model çalışıyor",
            "Allen Brain Atlas verisiyle ilk parametre eşleştirmesi yapıldı",
            "Güz dönemi raporu tamamlandı (BEP)", "Stokastik kanal gürültüsü modele entegre edildi",
            "İki nöron sinaptik olarak bağlandı", "Senkronizasyon analizi tamamlandı",
            "Gerçek veriyle nihai karşılaştırma + biyolojik gerçeklik değerlendirmesi yazıldı",
            "Final rapor + BEP sunumu tamam"]},
    {"id": "P2", "name": "Fizik & Olimpiyat Problem Portföyü", "emoji": "🍎",
     "goal": "Halliday + okul fiziğinden dönüştürülen + olimpiyat seviyesi problemlerden bir çözüm/türetim defteri.",
     "phases": {"p1": "Mekanik temel problemleri", "p2": "Enerji, momentum, dönme problemleri",
                "p3": "Salınım ve dalga problemleri", "p4": "Elektromanyetizma problemleri",
                "p5": "Optik + portföy derlemesi", "p6": "Final portföy PDF"},
     "topics": ["Mekanik", "Enerji", "Salınım", "Dalgalar", "Elektromanyetizma", "Optik"],
     "next": "İlk 5 mekanik problemini seç ve çözüm defterini aç",
     "ms": ["Faz 1-2 problemleri tamam", "Faz 3 problemleri tamam", "Faz 4 problemleri tamam",
            "Faz 5 problemleri tamam", "Final portföy PDF hazır"]},
    {"id": "P3", "name": "UWC Başvurusu & Felsefe Kulübü Üretimi", "emoji": "🍀",
     "goal": "Felsefe kulübünü sürdür, UWC başvurusunu somut çıktılarla besle.",
     "phases": {"p1": "Kurucu çekirdek toplama, idareyle görüşme", "p2": "Kulüp ilk oturumları, düzenli ritim",
                "p3": "UWC başvuru araştırması", "p4": "UWC essay taslağı",
                "p5": "Başvuru teslimi", "p6": "Kulüp dönem kapanışı"},
     "topics": ["Felsefe Kulübü", "UWC Başvurusu", "Akademik Yazım"],
     "next": "Kurucu çekirdek için 4-6 kişiyle konuş",
     "ms": ["Kulüp resmen kuruldu", "İlk 4 oturum tamamlandı", "UWC başvuru takvimi çıkarıldı",
            "Essay taslağı yazıldı", "Başvuru teslim edildi", "Kulüp dönem kapanış etkinliği yapıldı"]},
]

assessments = [
    {"id": "a1", "name": "Faz 1-2 Genel", "scope": "Mekanik + limit/türev + kablo teorisi temelleri", "hours": 2, "phase": "p2", "unit": "u17"},
    {"id": "a2", "name": "Güz Dönemi Genel (BEP)", "scope": "Fizik+matematik+biyoloji + P1 güz raporu", "hours": 3, "phase": "p3", "unit": "u21"},
    {"id": "a3", "name": "Faz 4 Genel", "scope": "Elektromanyetizma + PDE/stokastik + stokastik kanal modeli", "hours": 3, "phase": "p4", "unit": "u30"},
    {"id": "a4", "name": "Bahar/Yıl Sonu Genel (BEP)", "scope": "Tam kapsam + P1 final sunumu", "hours": 4, "phase": "p6", "unit": "u43"},
]

bridges = [
    {"n": "Kablo Teorisi Köprüsü", "e": "🧠",
     "d": "Kısmi diferansiyel denklemler + devre teorisi + gerçek nöron morfolojisinin kesişimi. P1 bu köprüyü somutlaştırıyor.",
     "t": ["PDE", "Python", "Nörobilim"]},
    {"n": "Stokastik Süreçler ↔ Kanal Gürültüsü", "e": "🎲",
     "d": "Olasılık/istatistikteki stokastik süreç kavramları, iyon kanalı açılıp kapanmasının Markov modeliyle birebir örtüşüyor.",
     "t": ["Olasılık", "Nörobilim"]},
    {"n": "Dinamik Sistemler ↔ Senkronizasyon", "e": "🌀",
     "d": "Salınım (fizik), diferansiyel denklem (matematik) ve iki nöronun senkronizasyonu aynı faz uzayı dilinde anlatılır.",
     "t": ["Salınım", "Diferansiyel Denklem", "Nörobilim"]},
    {"n": "Kalkülüs ↔ Felsefe", "e": "♾️",
     "d": "Zenon paradoksları ve limit kavramı — felsefe kulübünün ilk oturum konusu.",
     "t": ["Limit", "Felsefe"]},
]

resources = [
    {"s": "phys", "n": "Halliday, Resnick & Walker — Fundamentals of Physics", "u": "ANA KAYNAK, İngilizce"},
    {"s": "math", "n": "Stewart — Calculus: Early Transcendentals", "u": "Ana kitap"},
    {"s": "math", "n": "Strang — Introduction to Linear Algebra", "u": "Faz 4-5, dinamik sistemler için"},
    {"s": "bio", "n": "Campbell Biology", "u": "Temel biyoloji, Ch2-3, 6-8, 48-49"},
    {"s": "bio", "n": "Kandel — Principles of Neural Science (6. baskı)", "u": "Nörobilim derinleşme"},
    {"s": "bio", "n": "NeuroMorpho.org", "u": "Gerçek nöron morfolojisi"},
    {"s": "bio", "n": "Allen Brain Atlas / Allen Brain Cell Types Database", "u": "Gerçek elektrofizyoloji verisi"},
    {"s": "py", "n": "Computational Physics — Mark Newman", "u": "Sayısal yöntemler, PDE/stokastik için"},
    {"s": "jp", "n": "Genki I + II", "u": "Haftalık tekrar kaynağı"},
    {"s": "de", "n": "Menschen A1/A2", "u": "Ana kitap"},
]

subjects = [
    {"c": "math", "n": "Matematik", "e": "📐", "col": "#7BB661"},
    {"c": "phys", "n": "Fizik", "e": "🍎", "col": "#C8402F"},
    {"c": "py", "n": "Python", "e": "🐍", "col": "#3F7A57"},
    {"c": "bio", "n": "Biyoloji + Nörobilim", "e": "🧠", "col": "#5E9C4F"},
    {"c": "jp", "n": "Japonca", "e": "🌸", "col": "#E58FA6"},
    {"c": "de", "n": "Almanca", "e": "🥨", "col": "#B98A3E"},
    {"c": "uwc", "n": "UWC", "e": "🍀", "col": "#4E9160"},
    {"c": "prod", "n": "Üretim", "e": "🌱", "col": "#D08A2E"},
    {"c": "self", "n": "Öz Değerlendirme", "e": "🪞", "col": "#9B8AA8"},
    {"c": "bak", "n": BAK_GROUP["name"], "e": BAK_GROUP["emoji"], "col": BAK_GROUP["col"]},
] + [{"c": c, "n": d["name"], "e": d["emoji"], "col": d["col"]} for c, d in SCHOOL_SUBJECTS.items()]

kinds = [
    {"c": "study", "n": "Öğren", "e": "📚"}, {"c": "problem", "n": "Problem", "e": "✏️"},
    {"c": "code", "n": "Kod", "e": "⌨️"}, {"c": "explain", "n": "Anlat", "e": "🎙️"},
    {"c": "write", "n": "Yaz", "e": "✍️"}, {"c": "read", "n": "Oku", "e": "📖"},
    {"c": "review", "n": "Tekrar", "e": "🔁"}, {"c": "produce", "n": "Üret", "e": "🌱"},
    {"c": "test", "n": "Sınav", "e": "📋"}, {"c": "reflect", "n": "Değerlendir", "e": "🪞"},
]

data = {
    "version": 3,
    "title": "Beril · 2026-2027 Bütünleşik Müfredat (Okul Yılı Sonuna Kadar)",
    "phases": phases, "projects": projects, "assessments": assessments,
    "bridges": bridges, "resources": resources, "subjects": subjects, "kinds": kinds,
}

out = os.path.normpath(os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets", "curriculum.json"))
os.makedirs(os.path.dirname(out), exist_ok=True)
with open(out, "w", encoding="utf-8") as f:
    json.dump(data, f, ensure_ascii=False, separators=(",", ":"))

nu = sum(len(p["units"]) for p in phases)
nt = sum(len(u["tasks"]) for p in phases for u in p["units"])
print("wrote %s" % out)
print("phases=%d units=%d tasks=%d" % (len(phases), nu, nt))
print("total hours (units) = %.1f" % (sum(t["m"] for p in phases for u in p["units"] for t in u["tasks"]) / 60))
