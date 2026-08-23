#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
(≧▽≦) — curriculum generator v2
7 aylık, hesaplamalı nörobilim hedefine göre şekillenmiş müfredat.
26 Ağustos 2026 – Mart 2027.

TIMELESS: tarih yok, birim bitince sıradaki açılır. 1 hafta = 1 birim.
Bu script app/src/main/assets/curriculum.json'ı ÜRETİR — schema önceki
sürümle birebir aynı (Models.kt / CurriculumLoader.kt DEĞİŞMEDİ).
"""
import json, os

# ── 1. Haftalık konu yol haritaları (30 hafta) ──────────────────────
physics = (
    ["Ch1 Ölçme (Measurement)", "Ch2 Doğrusal Hareket", "Ch3 Vektörler"] +
    ["Ch4 2B/3B Hareket", "Ch5 Kuvvet ve Hareket I", "Ch6 Kuvvet ve Hareket II",
     "Ch7 Kinetik Enerji ve İş", "Ch8 Potansiyel Enerji ve Enerji Korunumu",
     "Ch9 Kütle Merkezi ve Momentum", "Ch10 Dönme", "Ch11 Yuvarlanma, Tork, Açısal Momentum",
     "Ch12 Denge ve Esneklik", "Ch13 Kütleçekimi", "Ch13 Kütleçekimi — tekrar & problem seti"] +
    ["Ch14 Akışkanlar", "Ch15 Salınımlar", "Ch15 Salınımlar — tekrar & problem seti",
     "Ch16 Dalgalar I", "Ch16 Dalgalar I — tekrar", "Ch17 Dalgalar II",
     "Ch17 Dalgalar II — tekrar", "Genel tekrar (Ch1-13 mekanik)", "Sınav sonrası toparlama"] +
    ["Ch18 Sıcaklık ve Isı", "Ch19 Gazların Kinetik Teorisi", "Ch20 Entropi ve 2. Yasa",
     "Ch21 Elektrik Yükü", "Ch22 Elektrik Alanlar", "Genel tekrar (Ch14-22)",
     "P2 portföy toparlama"]
)
math = (
    ["Ch1 Fonksiyonlar ve Modeller (tekrar)", "Ch2 Limit ve Türev — başlangıç", "Ch2 Limit ve Türev — bitiş"] +
    ["Ch3 Türev Alma Kuralları (1/3)", "Ch3 Türev Alma Kuralları (2/3)", "Ch3 Türev Alma Kuralları (3/3)",
     "Ch4 Türevin Uygulamaları (1/3)", "Ch4 Türevin Uygulamaları (2/3)", "Ch4 Türevin Uygulamaları (3/3)",
     "Ch5 İntegraller (1/2)", "Ch5 İntegraller (2/2)", "Ch5 tekrar & problem seti",
     "Ch6 İntegral Uygulamaları (1/2)", "Ch6 İntegral Uygulamaları (2/2)"] +
    ["Ch6 tekrar", "Ch7 İntegral Teknikleri — giriş",
     "Ch7 İntegral Teknikleri — tekrar", "Genel tekrar (Ch2-6)", "Genel tekrar (Ch2-6)",
     "Ch9 Diferansiyel Denklemler — giriş", "Ch9 Diferansiyel Denklemler — tekrar",
     "Genel tekrar (integral teknikleri)", "Sınav sonrası toparlama"] +
    ["Ch9 Diferansiyel Denklemler — devam", "Strang Lineer Cebir Ch1 (vektörler/matrisler)",
     "Strang Lineer Cebir Ch2 (Ax=b çözümü)", "Strang Ch5 — özdeğerler (dinamik sistemler için)",
     "Stewart Ch13 Vektör Fonksiyonları", "Genel tekrar (diferansiyel denklem + lineer cebir)",
     "Portföy toparlama"]
)
bio = (
    ["Campbell Ch2 Yaşamın Kimyasal Bağlamı", "Campbell Ch3 Su ve Yaşamın Uygunluğu",
     "Python: değişkenler, döngüler, fonksiyonlar"] +
    ["Campbell Ch6 Hücreye Bir Tur", "Campbell Ch7 Hücre Zarı ve Taşınım (nöron potansiyeli temeli)",
     "Campbell Ch8 Metabolizmaya Giriş", "Python: OOP temelleri",
     "Campbell Ch48 Nöronlar, Sinapslar, Sinyalleşme (1/3)", "Campbell Ch48 (2/3)", "Campbell Ch48 (3/3) + quiz",
     "Campbell Ch49 Sinir Sistemleri (1/2)", "Campbell Ch49 (2/2)",
     "Python: sayısal yöntemler (Euler, Runge-Kutta girişi)", "P1 proje iskeleti (HH model kurulumu)"] +
    ["Kandel Ch1 Nöron biyolojisine giriş", "Kandel Ch2 Sinir hücreleri ve davranış",
     "Tekrar & konsolidasyon (Campbell+Kandel)", "Kandel Ch3 Sinaptik iletim temelleri",
     "Tekrar & konsolidasyon", "Genel tekrar (biyoloji temelleri)", "Genel tekrar (nöron biyolojisi)",
     "Python: Hodgkin-Huxley denklemlerini kodlamaya başlama", "Sınav sonrası toparlama"] +
    ["Kandel Ch7-8 İyon kanalları ve aksiyon potansiyeli", "HH modelini Python'da tamamlama",
     "Kandel Ch9 Aksiyon potansiyeli yayılımı", "Dinamik sistemler + faz uzayı görselleştirme",
     "P1 projesini bitirme ve dokümantasyon", "Kandel Ch21 Algının yapısal doğası (opsiyonel derinleşme)",
     "P1 son teslim + portföy toparlama"]
)
assert len(physics) == len(math) == len(bio) == 30

def phase_of(wk):
    if wk <= 3: return "p1"
    if wk <= 14: return "p2"
    if wk <= 23: return "p3"
    return "p4"

PHASE_META = {
    "p1": {"name": "Faz 1 · Kuruluş", "sub": "26 Ağu – 15 Eyl", "hours": 105,
           "goal": "Sıfırdan başlıyoruz: temel kavramlar, Python temelleri, biyolojinin kimyasal zemini."},
    "p2": {"name": "Faz 2 · Derinleşme", "sub": "16 Eyl – 30 Kas", "hours": 198,
           "goal": "Fizik ve matematik güçleniyor, hücre biyolojisinden nöron sinyalleşmesine geçiliyor, kulüp kuruluyor."},
    "p3": {"name": "Faz 3 · Konsolidasyon", "sub": "1 Ara – 31 Oca", "hours": 122,
           "goal": "Sınav dönemi — tempo bilerek düşük, Kandel ile nöron biyolojisi derinleşiyor, HH modeli kodlanmaya başlıyor."},
    "p4": {"name": "Faz 4 · Sentez ve Üretim", "sub": "1 Şub – Mart sonu", "hours": 126,
           "goal": "HH simülatörü bitiyor, dinamik sistemler devreye giriyor, UWC başvurusu ve portföy toparlanıyor."},
}

SRC = {
    "phys": "Halliday, Fundamentals of Physics (EN)",
    "math": "Stewart Calculus / Strang Lineer Cebir / MEB",
    "bio": "Campbell Biology / Kandel Principles of Neural Science (6.bs)",
    "py": "Python (proje kodu)",
    "jp": "Genki I-II / Minna no Nihongo",
    "de": "Menschen A1-A2",
    "sch": "Okul müfredatı (MEB)",
    "prod": "P1/P2/P3 üretim",
}

# ── 2. Haftalık gün şablonları (subject, dakika) ────────────────────
template_faz1 = [
    ("phys", 60), ("math", 60), ("bio", 60), ("math", 60), ("bio", 60), ("jp", 60),
    ("phys", 60), ("bio", 60), ("sch", 60), ("math", 60), ("phys", 60), ("prod", 60),
    ("bio", 60), ("jp", 60), ("de", 60),
    ("phys", 90), ("math", 90), ("prod", 90),
    ("bio", 90), ("sch", 60), ("prod", 60),
]
template_faz2 = [
    ("phys", 60), ("math", 60),
    ("bio", 60), ("math", 60),
    ("phys", 60), ("bio", 60),
    ("math", 60), ("jp", 60),
    ("sch", 60), ("de", 60),
    ("phys", 120), ("bio", 120),
    ("math", 120), ("prod", 120),
]
template_faz3 = [
    ("phys", 45), ("math", 45),
    ("bio", 45), ("sch", 45),
    ("math", 45), ("jp", 45),
    ("phys", 45), ("bio", 45),
    ("sch", 45), ("de", 45),
    ("math", 90), ("bio", 90),
    ("phys", 90), ("prod", 90),
]
template_faz4 = [
    ("phys", 60), ("math", 60),
    ("bio", 60), ("math", 60),
    ("phys", 60), ("bio", 60),
    ("math", 60), ("jp", 60),
    ("sch", 60), ("prod", 60),
    ("bio", 120), ("prod", 120),
    ("phys", 120), ("math", 120),
]
TEMPLATES = {"p1": template_faz1, "p2": template_faz2, "p3": template_faz3, "p4": template_faz4}

TOPIC_LIST = {"phys": physics, "math": math, "bio": bio}

def topic_for(subj, wk_idx, phid):
    if subj in TOPIC_LIST:
        return TOPIC_LIST[subj][wk_idx]
    if subj == "sch":
        return "Okul programına göre haftalık ödev/sınav takibi"
    if subj == "jp":
        return {"p1": "Hiragana/Katakana pekiştirme", "p2": "Genki I Ch1-6 (temel gramer)",
                "p3": "Genki I Ch7-12", "p4": "Genki II başlangıç + N3 kelime tekrarı"}[phid]
    if subj == "de":
        return "A1-A2 devam — kelime/gramer tekrarı"
    if subj == "prod":
        return "P1 (HH simülasyonu) / P2 (fizik portföyü) / P3 (UWC & felsefe kulübü) — dönüşümlü"
    return ""

KIND = {"phys": "problem", "math": "problem", "bio": "read", "py": "code",
        "sch": "study", "jp": "study", "de": "study", "prod": "produce"}

def task_text(subj, topic):
    if subj == "phys":
        return "Fizik — %s: konu anlatımı + örnek problemler + bölüm sonu 8-10 soru" % topic
    if subj == "math":
        return "Matematik — %s: teori + 10-12 alıştırma + 2 türetim" % topic
    if subj == "bio":
        return "Biyoloji/Nörobilim — %s: oku, özetle, 5-6 kavrama sorusu" % topic
    if subj == "sch":
        return topic
    if subj == "jp":
        return "Japonca — %s" % topic
    if subj == "de":
        return "Almanca — %s" % topic
    if subj == "prod":
        return topic
    return topic

# ── 3. Birimleri üret: 1 hafta = 1 birim ────────────────────────────
units_by_phase = {"p1": [], "p2": [], "p3": [], "p4": []}

for wk in range(1, 31):
    idx = wk - 1
    phid = phase_of(wk)
    tmpl = TEMPLATES[phid]
    tasks = []
    for i, (subj, mins) in enumerate(tmpl):
        topic = topic_for(subj, idx, phid)
        tasks.append({
            "i": "p%s-u%02d-t%02d" % (phid[1], wk, i + 1),
            "s": subj, "k": KIND[subj], "t": task_text(subj, topic), "m": mins,
        })
    title = "Hafta %d · %s" % (wk, physics[idx].split(" — ")[0])
    kicker = "%s · %s" % (PHASE_META[phid]["name"], math[idx].split(" (")[0])
    note = "Kaynaklar: " + "; ".join(sorted({SRC[s] for s, _ in tmpl if s in SRC}))
    bridges = []
    if idx >= 13 and any(s == "bio" and "Python" in topic_for("bio", idx, phid) or "HH" in topic_for("bio", idx, phid) for s in ["bio"]):
        bridges = ["Hesaplamalı Nörobilim Köprüsü"]
    unit = {
        "id": "u%02d" % wk,
        "title": title,
        "kicker": kicker,
        "tasks": tasks,
        "note": note,
    }
    if bridges:
        unit["br"] = bridges
    units_by_phase[phid].append(unit)

phases = []
for phid in ["p1", "p2", "p3", "p4"]:
    m = PHASE_META[phid]
    phases.append({
        "id": phid, "name": m["name"], "sub": m["sub"], "goal": m["goal"],
        "hours": m["hours"], "units": units_by_phase[phid],
    })

# ── 4. Üretim omurgası ───────────────────────────────────────────────
projects = [
    {"id": "P1", "name": "Hodgkin-Huxley Nöron Simülatörü", "emoji": "🧠",
     "goal": "Tek nöronun elektriksel ateşlemesini Python'da diferansiyel denklemlerle simüle et.",
     "phases": {
         "p1": "Python temelleri, membran taşınımını kavramsal olarak anlama",
         "p2": "Nöron sinyalleşmesi teorisi + sayısal yöntemler + proje iskeleti",
         "p3": "HH denklemlerini koda dökmeye başlama",
         "p4": "Modeli tamamlama, faz uzayı görselleştirme, dokümantasyon"},
     "topics": ["Python", "Diferansiyel Denklem", "Biyoloji", "Nörobilim", "Dinamik Sistemler"],
     "next": "Python'da bir nöronun membran potansiyelini zamana göre çizen ilk taslağı yaz",
     "ms": ["Python OOP iskeleti hazır", "İyon kanalı denklemleri koda döküldü",
            "İlk aksiyon potansiyeli grafiği üretildi", "Faz uzayı görselleştirmesi eklendi",
            "README + dokümantasyon tamam, GitHub'da yayınlandı"]},
    {"id": "P2", "name": "Fizik & Olimpiyat Problem Portföyü", "emoji": "🍎",
     "goal": "Halliday + olimpiyat seviyesi problemlerden seçilmiş bir çözüm/türetim defteri.",
     "phases": {
         "p1": "Mekanik temel problemleri",
         "p2": "Enerji, momentum, dönme problemleri",
         "p3": "Salınım ve dalga problemleri",
         "p4": "Temel elektromanyetizma problemleri, portföy derlemesi"},
     "topics": ["Mekanik", "Enerji", "Salınım", "Dalgalar", "Elektromanyetizma"],
     "next": "İlk 5 mekanik problemini seç ve çözüm defterini aç",
     "ms": ["Faz 1 problemleri tamam", "Faz 2 problemleri tamam",
            "Faz 3 problemleri tamam", "Faz 4 problemleri + portföy PDF"]},
    {"id": "P3", "name": "UWC Başvurusu & Felsefe Kulübü Üretimi", "emoji": "🍀",
     "goal": "Felsefe kulübünü sürdür, UWC başvurusunu somut çıktılarla besle.",
     "phases": {
         "p1": "Kurucu çekirdek toplama, idareyle görüşme",
         "p2": "Kulüp ilk oturumları, düzenli ritim",
         "p3": "Kulüp devam, UWC başvuru araştırması",
         "p4": "UWC essay yazımı, aktivite listesi, başvuru toparlama"},
     "topics": ["Felsefe Kulübü", "UWC Başvurusu", "Akademik Yazım"],
     "next": "Kurucu çekirdek için 4-6 kişiyle konuş",
     "ms": ["Kulüp resmen kuruldu", "İlk 4 oturum tamamlandı",
            "UWC başvuru takvimi çıkarıldı", "Essay taslağı yazıldı"]},
]

# ── 5. Sınavlar / köprüler / kaynaklar / sözlükler ──────────────────
assessments = [
    {"id": "a1", "name": "Faz 1 Genel", "scope": "Mekanik giriş + limit/türev + hücre kimyası",
     "hours": 2, "phase": "p1", "unit": "u03"},
    {"id": "a2", "name": "Faz 2 Genel", "scope": "Newton yasaları-enerji-momentum + türev/integral + Campbell Ch6-49",
     "hours": 3, "phase": "p2", "unit": "u14"},
    {"id": "a3", "name": "Faz 3 Genel", "scope": "Salınım-dalga + integral teknikleri/DD + Kandel Ch1-3",
     "hours": 3, "phase": "p3", "unit": "u23"},
    {"id": "a4", "name": "Genel Deneme", "scope": "Fizik+matematik tam kapsam + HH modeli sunumu",
     "hours": 4, "phase": "p4", "unit": "u30"},
]

bridges = [
    {"n": "Hesaplamalı Nörobilim Köprüsü", "e": "🧠",
     "d": "Hodgkin-Huxley denklemleri diferansiyel denklem + devre teorisi + nöron biyolojisinin kesişimi. P1 projesi bu köprüyü somutlaştırıyor.",
     "t": ["Diferansiyel Denklem", "Python", "Nörobilim"]},
    {"n": "Hücre Zarı ↔ Elektrik Devresi", "e": "🔋",
     "d": "Nöron zarı bir kapasitör-direnç devresi gibi modellenir. Campbell Ch7 ve gelecekteki devre konuları aynı matematiği paylaşır.",
     "t": ["Biyoloji", "Elektromanyetizma"]},
    {"n": "Dinamik Sistemler ↔ Faz Uzayı", "e": "🌀",
     "d": "Salınım (fizik), diferansiyel denklem (matematik) ve nöron ateşleme döngüleri (biyoloji) aynı faz uzayı dilinde anlatılır.",
     "t": ["Salınım", "Diferansiyel Denklem", "Nörobilim"]},
    {"n": "İstatistiksel Mekanik ↔ Nöron Popülasyonları", "e": "🔥",
     "d": "Büyük nöron popülasyonlarının kolektif davranışı, parçacık sistemlerindeki gibi istatistiksel mekanik araçlarıyla incelenebilir.",
     "t": ["Termodinamik", "Nörobilim"]},
    {"n": "Kalkülüs ↔ Felsefe", "e": "♾️",
     "d": "Zenon paradoksları ve limit kavramı — felsefe kulübünün ilk oturum konusu.",
     "t": ["Limit", "Felsefe"]},
]

resources = [
    {"s": "phys", "n": "Halliday, Resnick & Walker — Fundamentals of Physics", "u": "ANA KAYNAK, İngilizce"},
    {"s": "math", "n": "Stewart — Calculus: Early Transcendentals", "u": "Ana kitap"},
    {"s": "math", "n": "Strang — Introduction to Linear Algebra", "u": "Faz 4, dinamik sistemler için"},
    {"s": "bio", "n": "Campbell Biology", "u": "Temel biyoloji, Ch2-3, 6-8, 48-49"},
    {"s": "bio", "n": "Kandel — Principles of Neural Science (6. baskı)", "u": "Nörobilim derinleşme, Faz 3-4"},
    {"s": "py", "n": "Computational Physics — Mark Newman", "u": "Sayısal yöntemler, HH modeli için"},
    {"s": "jp", "n": "Genki I + II", "u": "Ana kitap"},
    {"s": "de", "n": "Menschen A1/A2", "u": "Ana kitap"},
]

subjects = [
    {"c": "math", "n": "Matematik", "e": "📐", "col": "#7BB661"},
    {"c": "phys", "n": "Fizik", "e": "🍎", "col": "#C8402F"},
    {"c": "py", "n": "Python", "e": "🐍", "col": "#3F7A57"},
    {"c": "bio", "n": "Biyoloji + Nörobilim", "e": "🧠", "col": "#5E9C4F"},
    {"c": "jp", "n": "Japonca", "e": "🌸", "col": "#E58FA6"},
    {"c": "de", "n": "Almanca", "e": "🥨", "col": "#B98A3E"},
    {"c": "sch", "n": "10. Sınıf", "e": "🏫", "col": "#8899AA"},
    {"c": "uwc", "n": "UWC", "e": "🍀", "col": "#4E9160"},
    {"c": "prod", "n": "Üretim", "e": "🌱", "col": "#D08A2E"},
]

kinds = [
    {"c": "study", "n": "Öğren", "e": "📚"},
    {"c": "problem", "n": "Problem", "e": "✏️"},
    {"c": "code", "n": "Kod", "e": "⌨️"},
    {"c": "explain", "n": "Anlat", "e": "🎙️"},
    {"c": "write", "n": "Yaz", "e": "✍️"},
    {"c": "read", "n": "Oku", "e": "📖"},
    {"c": "review", "n": "Tekrar", "e": "🔁"},
    {"c": "produce", "n": "Üret", "e": "🌱"},
    {"c": "test", "n": "Sınav", "e": "📋"},
]

data = {
    "version": 2,
    "title": "Beril · 7 Aylık Müfredat (Hesaplamalı Nörobilim)",
    "phases": phases,
    "projects": projects,
    "assessments": assessments,
    "bridges": bridges,
    "resources": resources,
    "subjects": subjects,
    "kinds": kinds,
}

out = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets", "curriculum.json")
out = os.path.normpath(out)
os.makedirs(os.path.dirname(out), exist_ok=True)
with open(out, "w", encoding="utf-8") as f:
    json.dump(data, f, ensure_ascii=False, separators=(",", ":"))

nu = sum(len(p["units"]) for p in phases)
nt = sum(len(u["tasks"]) for p in phases for u in p["units"])
print("wrote %s" % out)
print("phases=%d units=%d tasks=%d" % (len(phases), nu, nt))
print("total hours (units) = %.1f" % (sum(t["m"] for p in phases for u in p["units"] for t in u["tasks"]) / 60))
