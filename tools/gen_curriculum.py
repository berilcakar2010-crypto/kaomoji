#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
(≧▽≦) — curriculum generator
Builds app/src/main/assets/curriculum.json from a compact declarative source.

The curriculum is TIMELESS: units unlock sequentially as the previous one is
completed. No dates, no streaks, no countdowns.
"""
import json, os, re

# subject codes: math phys py jp de lit phil chem bio hist geo eng uwc prod
# kind codes:    study problem code explain write read review produce test

units = []
_uc = [0]


def T(subject, kind, text, minutes=30):
    return {"s": subject, "k": kind, "t": text, "m": minutes}


def U(title, kicker, tasks, feynman=None, bridges=None, note=None):
    _uc[0] += 1
    uid = "u%03d" % _uc[0]
    for i, t in enumerate(tasks):
        t["i"] = "%s-t%02d" % (uid, i + 1)
    u = {"id": uid, "title": title, "kicker": kicker, "tasks": tasks}
    if feynman:
        u["fey"] = feynman
    if bridges:
        u["br"] = bridges
    if note:
        u["note"] = note
    units.append((uid, u))
    return uid


phases = []


def P(pid, name, sub, goal, hours, first_uid, last_uid):
    phases.append({
        "id": pid, "name": name, "sub": sub, "goal": goal, "hours": hours,
        "units": [u for (k, u) in units if first_uid <= k <= last_uid]
    })


# common recurring task builders
def UWC(text):
    return T("uwc", "write", text, 20)


def JP(text, m=45):
    return T("jp", "study", text, m)


def DE(text, m=45):
    return T("de", "study", text, m)


def CLOSE(topic):
    return T("eng", "explain", "Kapanış: %s konusunu kitaba bakmadan İngilizce anlat, kaydet" % topic, 20)


def ANKI(n=15):
    return T("eng", "review", "Bugünden en az %d Anki kartı üret ve destene ekle" % n, 15)


# ══════════════════════════════════════════════════════════════════
# FAZ 1 — TEMELLER (31 birim)
# ══════════════════════════════════════════════════════════════════

f1a = U("Limit ve Süreklilik", "Hafta 1 · Matematiksel Altyapı", [
    T("math", "study", "3Blue1Brown — Essence of Calculus Bl.1 ve Bl.7 izle", 35),
    T("math", "study", "Sezgisel limit tanımı, tek taraflı limitler, varlık koşulu", 40),
    T("math", "study", "ε-δ biçimsel tanımı — meydan okuma oyunu olarak kavra", 50),
    T("math", "study", "Limit kuralları + 7 belirsizlik türü (0/0, ∞/∞, ∞−∞, 0·∞, 1^∞, 0⁰, ∞⁰)", 40),
    T("math", "study", "Sıkıştırma teoremi + lim(sinx/x)=1 geometrik ispatını kendin türet", 45),
    T("math", "study", "e tanımı: lim(1+1/n)^n, bileşik faiz kökeni", 25),
    T("math", "study", "Sonsuzda limit, yatay/düşey/eğik asimptotlar", 30),
    T("math", "study", "Süreklilik üç koşulu, süreksizlik türleri, Ara Değer Teoremi", 40),
    T("math", "problem", "Stewart Bl.2 — 2.2/2.3/2.5/2.6 altbölümlerinden 15'er soru", 90),
    T("py", "code", "Python 3.12 + VS Code kur, venv oluştur ve etkinleştir", 30),
    T("py", "study", "Değişkenler, veri tipleri, tip dönüşümü, f-string, operatörler", 45),
    T("py", "study", "Kontrol akışı: if/elif/else, girinti kuralı", 30),
    T("py", "code", "20 temel alıştırma programını yaz", 60),
    T("py", "code", "limit_explorer.py — sin(x)/x yakınsaması + ikiye bölme (ADT) kök bulucu", 40),
    T("prod", "produce", "GitHub'da fizik-py reposunu aç, README yaz, ilk commit", 30),
    T("phil", "read", "Zenon paradoksları (dördü de) — limit kavramıyla çözümü", 35),
    T("phil", "write", "150 kelime: Akhilleus paradoksu ve limitin çözümü (P5 portfolyo)", 25),
    JP("Hiragana 46 karakter — kağıda yaz, dakuten/handakuten/yōon"),
    JP("Genki Bl.1: です, これ/それ/あれ, は ve の partikelleri, 30 kelime", 25),
    T("uwc", "read", "UWC nedir: Kurt Hahn, altı temel değer, seçim süreci", 10),
    UWC("Yansıma: 'hiç ulaşamadığın ama sürekli yaklaştığın' bir hedef var mı?"),
    T("uwc", "study", "Türkiye Ulusal Komitesi (tr.uwc.org) — başvuru takvimi ve aşamaları not al"),
    ANKI(),
    CLOSE("limit ve süreklilik"),
], feynman="Explain why the derivative needs the concept of a limit, in English, without the book.",
    bridges=["Kalkülüs ↔ Felsefe"])

U("Türev I — Kurallar", "Hafta 1 · Matematiksel Altyapı", [
    T("math", "study", "Türevin limit tanımı, teğet doğru, türevlenebilirlik ↔ süreklilik", 40),
    T("math", "study", "Çarpım ve bölüm kuralı — ikisini de ispatla", 40),
    T("math", "study", "Zincir kuralı — iç içe fonksiyonlarda çok tekrar", 45),
    T("math", "study", "Trigonometrik, üstel, logaritmik türevler; ters trig türevleri", 40),
    T("math", "study", "Kapalı türev (implicit), logaritmik türev, yüksek mertebe", 35),
    T("math", "problem", "50 türev sorusu — hız hedefi: 50 soru < 75 dakika", 80),
    T("py", "study", "Döngüler (for/while/range), break/continue, fonksiyonlar, kapsam", 50),
    T("py", "code", "Sayısal türev fonksiyonu yaz (ileri/geri/merkezi fark) ve hatayı karşılaştır", 40),
    T("prod", "produce", "fizik-py'ye numerical_derivative modülünü ekle ve commit et", 25),
    JP("Katakana + Genki Bl.1 tekrar, kelime 50'ye çıkar"),
    UWC("Bugün bir şeyi 'hızlı' yapmak ile 'iyi' yapmak arasında seçim yaptın mı? Hangisi?"),
    ANKI(),
    CLOSE("türevin limit tanımı"),
], feynman="Derive the product rule from the limit definition, in English.")

U("Türev II — Uygulamalar", "Hafta 1 · Matematiksel Altyapı", [
    T("math", "study", "İlişkili değişim oranları (related rates) — fizik için kritik", 45),
    T("math", "problem", "15 related-rates problemi çöz", 55),
    T("math", "study", "Doğrusallaştırma, diferansiyeller, L'Hôpital kuralı", 35),
    T("math", "study", "Rolle ve Ortalama Değer Teoremi — ispatlarıyla", 30),
    T("math", "study", "Artan/azalan, içbükeylik, büküm noktası, 1. ve 2. türev testi", 40),
    T("math", "problem", "20 optimizasyon problemi (geometrik, fiziksel, ekonomik)", 70),
    T("math", "study", "Tam eğri analizi ve çizimi + Newton-Raphson yöntemi", 30),
    T("py", "study", "Veri yapıları: liste, tuple, dict, set; dilimleme; comprehension", 50),
    T("py", "code", "Newton-Raphson kök bulucu yaz, ikiye bölme ile hız karşılaştır", 40),
    T("lit", "read", "Edebiyatın bilimle ilişkisi, metin türleri, edebî sanatlar", 45),
    JP("Genki Bl.2: sayılar, alışveriş, ここ/そこ/あそこ"),
    UWC("Perspektif alıştırması: bu yılki bir anlaşmazlığı iki gözden yaz"),
    ANKI(),
    CLOSE("optimizasyon problemi çözme stratejisi"),
])

U("İntegral I — Temel Teorem", "Hafta 1 · Matematiksel Altyapı", [
    T("math", "study", "Alan problemi, Riemann toplamı (sol/sağ/orta/üst/alt)", 40),
    T("math", "study", "Belirli integralin tanımı ve özellikleri", 30),
    T("math", "study", "Kalkülüsün Temel Teoremi I ve II — ikisini de ispatla", 55),
    T("math", "study", "Belirsiz integral, temel integral tablosunu ezberle", 30),
    T("math", "study", "Değişken değiştirme (u-substitution), sınır değiştirme, simetri", 40),
    T("math", "problem", "40 integral sorusu", 75),
    T("py", "study", "Dosya işlemleri, with bloğu, CSV/JSON, try/except", 45),
    T("py", "code", "Riemann toplamı + yamuk kuralı ile sayısal integral yaz", 40),
    T("prod", "produce", "fizik-py'ye numerical_integral ekle, commit", 20),
    DE("A1 tazeleme: Präsens, Artikel (der/die/das), Personalpronomen, Wortstellung"),
    UWC("Bugün öğrendiğin bir şeyi başka birine anlatabildin mi? Kime, nasıl?"),
    ANKI(),
    CLOSE("kalkülüsün temel teoremi"),
], feynman="Explain why differentiation and integration are inverse operations, in English.")

U("İntegral II — Teknikler", "Hafta 1 · Matematiksel Altyapı", [
    T("math", "study", "Kısmi integrasyon (LIATE), tekrarlı uygulama", 40),
    T("math", "study", "Trigonometrik integraller ve trigonometrik yerine koyma", 45),
    T("math", "study", "Basit kesirlere ayırma — tüm durumlar", 40),
    T("math", "study", "Genelleştirilmiş integral (improper) Tip 1 ve 2, yakınsaklık", 35),
    T("math", "study", "Sayısal integrasyon: yamuk, Simpson, hata sınırları", 25),
    T("math", "problem", "50 integral sorusu — bu blok ağır, mola ver", 95),
    T("py", "study", "OOP I: sınıf, __init__, metod, self, kapsülleme, property", 55),
    T("py", "code", "Vector2D sınıfı: toplama, çıkarma, nokta çarpım, büyüklük, açı", 45),
    JP("Genki Bl.3: fiil grupları, ます形, を/で/に partikelleri"),
    UWC("UWC 'farklılığın kutlanması' değeri — senin çevrende bu neye benziyor?"),
    ANKI(),
    CLOSE("kısmi integrasyonun neden çalıştığı"),
])

U("İntegral Uygulamaları + Seriler I", "Hafta 1 · Matematiksel Altyapı", [
    T("math", "study", "Eğri altı alan, iki eğri arası alan", 30),
    T("math", "study", "Dönel hacim: disk, halka, silindirik kabuk; kesit alanı yöntemi", 45),
    T("math", "study", "Yay uzunluğu, dönel yüzey alanı", 30),
    T("math", "study", "Fizik uygulamaları: iş, sıvı basıncı, kütle merkezi", 40),
    T("math", "study", "Diziler: yakınsaklık, monotonluk, sınırlılık", 30),
    T("math", "study", "Geometrik seri, teleskopik seri, n'inci terim testi, integral testi, p-serisi", 45),
    T("math", "problem", "35 soru (integral uygulamaları + seri testleri)", 70),
    T("py", "study", "OOP II: kalıtım, polimorfizm, super(), dunder metodlar", 50),
    T("py", "code", "Vector2D'yi dunder metodlarla yeniden yaz — v1+v2 çalışsın", 40),
    T("phil", "read", "Epistemoloji: bilginin kaynağı, rasyonalizm vs empirizm, Descartes ve Hume", 45),
    DE("Akkusativ, Possessivpronomen, Modalverben"),
    UWC("'Kişisel dürüstlük' — kimse bakmıyorken de aynı kişi olmak ne demek?"),
    ANKI(),
    CLOSE("dönel hacim yöntemleri arasındaki fark"),
])

U("Seriler II + Diferansiyel Denklem + SINAV", "Hafta 1 · Faz 1 ilk sınavı", [
    T("math", "study", "Karşılaştırma, limit karşılaştırma, alterne seri, oran, kök testleri", 45),
    T("math", "study", "Mutlak/koşullu yakınsaklık, kuvvet serileri, yakınsaklık yarıçapı", 40),
    T("math", "study", "Taylor ve Maclaurin serileri — fizikte sürekli kullanacaksın", 50),
    T("math", "study", "Ezberle: e^x, sinx, cosx, 1/(1−x), ln(1+x), (1+x)^k açılımları", 25),
    T("math", "study", "Taylor kalan terimi ve hata tahmini", 25),
    T("math", "study", "Ayrılabilir DD, yön alanları, Euler yöntemi, lojistik model, integral çarpanı", 55),
    T("py", "study", "Modüller, paketler, __name__, Git/GitHub akışı (commit, branch, push)", 45),
    T("prod", "produce", "fizik-py v0.1: Vector2D + sayısal yöntemler, README güncelle, push", 40),
    T("math", "test", "SINAV: Kalkülüs BC tam kapsam — 3 saat, süreli, kapalı kitap", 180),
    T("math", "review", "Sınav sonrası: her yanlışı Hata Defteri'ne 'neden' sütunuyla geçir", 45),
    UWC("Sınav sonrası yansıma: hata yapmak seni nasıl hissettiriyor? Dürüst yaz."),
    CLOSE("Taylor serisinin ne işe yaradığı"),
], feynman="Explain what a Taylor series does and why physicists love it, in English.")

# ── Hafta 2: Halliday Mekanik I ──
U("Halliday Bl.1–3: Ölçme, Doğrusal Hareket, Vektörler", "Hafta 2 · Mekanik", [
    T("phys", "study", "Bl.1 Measurement: SI, boyut analizi, mertebe tahmini, anlamlı rakam", 35),
    T("phys", "study", "Bl.2: konum, yer değiştirme, ortalama/anlık hız, ivme", 40),
    T("phys", "study", "Sabit ivme denklemlerini integral yoluyla TÜRET (ezberleme)", 40),
    T("phys", "study", "Serbest düşme + x-t/v-t/a-t grafikleri arası türev-integral ilişkisi", 35),
    T("phys", "study", "Bl.3: bileşenler, birim vektörler, nokta çarpım, vektörel çarpım", 50),
    T("phys", "problem", "Her bölümden 20 soru — toplam 60", 100),
    T("py", "code", "fizik-py: Vector3D sınıfı, dot ve cross metodları", 50),
    T("lit", "read", "Hikâye: Sait Faik ve Ömer Seyfettin — olay ve durum hikâyesi karşılaştırması", 45),
    JP("Genki Bl.4: あります/います, geçmiş zaman, sayaçlar"),
    UWC("Bir şeyi ezberlemek yerine türetmek — bu senin için ne değiştiriyor?"),
    ANKI(),
    CLOSE("nokta çarpım ile vektörel çarpımın fiziksel farkı"),
], bridges=["Kalkülüs ↔ Fizik"])

U("Halliday Bl.4: İki ve Üç Boyutta Hareket", "Hafta 2 · Mekanik", [
    T("phys", "study", "Konum, hız, ivme vektörleri — türev ilişkisi", 30),
    T("phys", "study", "Eğik atış tam analizi, yörünge denklemi", 45),
    T("phys", "study", "Eğik atış varyasyonları: farklı yükseklik, eğimli düzlem, hedef vurma", 40),
    T("phys", "study", "Düzgün dairesel hareket, merkezcil ivme türetimi", 35),
    T("phys", "study", "Göreli hareket — 1D ve 2D, referans çerçevesi dönüşümü", 35),
    T("phys", "problem", "40 soru, eğik atış ağırlıklı", 85),
    T("py", "code", "fizik-py: eğik atış çözücü sınıfı + matplotlib yörünge çizimi", 55),
    T("phil", "read", "Ontoloji: gerçeklik nedir, idealizm vs materyalizm, Platon'un idealar kuramı", 45),
    JP("Genki Bl.4 devamı, kelime 200'e çıkar"),
    UWC("Aynı olayı farklı çerçeveden görmek — fizikteki referans çerçevesi ile insan ilişkileri arasında bir benzerlik kurabilir misin?"),
    ANKI(),
    CLOSE("neden yatay ve dikey hareket bağımsızdır"),
], bridges=["Fizik ↔ Felsefe: referans çerçevesi"])

U("Halliday Bl.5: Kuvvet ve Hareket I", "Hafta 2 · Mekanik", [
    T("phys", "study", "Newton'un birinci yasası, eylemsizlik, eylemsiz referans çerçevesi", 30),
    T("phys", "study", "İkinci yasa — vektörel form ve bileşenlere ayırma", 35),
    T("phys", "study", "Kuvvet çeşitleri: ağırlık, normal, gerilme, yay (Hooke)", 30),
    T("phys", "study", "Üçüncü yasa — etki/tepki çiftleri ve yaygın hatalar", 30),
    T("phys", "problem", "30 farklı senaryo için serbest cisim diyagramı çiz", 60),
    T("phys", "problem", "45 soru: bağlı cisimler, makara, Atwood, asansör problemleri", 95),
    T("py", "study", "Test yazma: pytest temeli, birim test, assert", 40),
    T("py", "code", "fizik-py için test dosyası yaz, tüm testler geçsin", 40),
    DE("Perfekt, trennbare Verben, Imperativ"),
    UWC("Bir topluluğun senden sorumlu olması ne demek? Böyle bir topluluğun parçası mısın?"),
    ANKI(),
    CLOSE("serbest cisim diyagramı çizmenin adımları"),
], feynman="Explain why action-reaction pairs never cancel each other, in English.")

U("Halliday Bl.6: Kuvvet ve Hareket II", "Hafta 2 · Mekanik", [
    T("phys", "study", "Statik ve kinetik sürtünme, sürtünme katsayıları", 35),
    T("phys", "study", "Eğik düzlemde sürtünme, devrilme vs kayma", 30),
    T("phys", "study", "Sürüklenme kuvveti ve limit hız türetimi", 35),
    T("phys", "study", "Dairesel harekette net kuvvet: yatay/düşey döngü, bankalı viraj, konik sarkaç", 50),
    T("phys", "study", "Dönen referans çerçevesi — merkezkaç neden sahte kuvvet", 25),
    T("phys", "problem", "45 soru", 95),
    T("py", "code", "fizik-py: hava dirençli eğik atış (Euler), ideal durumla karşılaştırmalı grafik", 60),
    T("lit", "read", "Şiir I: koşuk/sagu/sav, halk şiiri nazım biçimleri, ölçü", 45),
    JP("Genki Bl.5: い-sıfat, な-sıfat, geçmiş zaman sıfat çekimi"),
    UWC("Bugün zorlandığın bir an: pes etmek yerine ne yaptın?"),
    ANKI(),
    CLOSE("limit hızın neden var olduğu"),
])

U("Halliday Bl.7–8: İş ve Enerji", "Hafta 2 · Mekanik", [
    T("phys", "study", "Kinetik enerji, iş tanımı (nokta çarpım), değişken kuvvetin işi (integral)", 45),
    T("phys", "study", "Yay ve yerçekimi işi, iş-enerji teoremi, güç", 35),
    T("phys", "study", "Korunumlu/korunumsuz kuvvet, yol bağımsızlığı", 30),
    T("phys", "study", "Potansiyel enerji fonksiyonu, F = −dU/dx", 35),
    T("phys", "study", "Potansiyel enerji eğrisi: dönüm noktaları, kararlı/kararsız denge", 40),
    T("phys", "study", "Sürtünmeli sistemlerde enerji, ısıya dönüşüm", 25),
    T("phys", "problem", "45 soru", 95),
    T("py", "code", "fizik-py: potansiyel kuyusu görselleştirici + enerji korunumu doğrulayıcı", 55),
    DE("Dativ giriş, Wechselpräpositionen"),
    UWC("'Şefkat ve hizmet' — yardım etmek ile birlikte çalışmak arasındaki fark ne?"),
    ANKI(),
    CLOSE("F = −dU/dx bağıntısının anlamı"),
], feynman="Explain the relationship between force and potential energy, in English.")

U("Halliday Bl.9: Kütle Merkezi ve Momentum", "Hafta 2 · Mekanik", [
    T("phys", "study", "Kütle merkezi: ayrık ve sürekli cisimler (integral hesabı)", 45),
    T("phys", "study", "KM hareketi, Newton'un 2. yasası KM için", 25),
    T("phys", "study", "Momentum, itme, itme-momentum teoremi, korunum koşulları", 35),
    T("phys", "study", "Çarpışmalar: elastik (1D/2D), inelastik, tam inelastik", 50),
    T("phys", "study", "Elastik çarpışma hız formüllerini türet, balistik sarkaç", 35),
    T("phys", "study", "Değişken kütleli sistem — roket denklemi (Tsiolkovsky) türetimi", 40),
    T("phys", "problem", "45 soru", 95),
    T("py", "code", "fizik-py: çarpışma simülatörü + momentum/enerji korunumu kontrolü", 55),
    T("phil", "read", "Etik: Aristoteles erdem etiği, Kant ödev etiği, faydacılık; bilim etiği", 45),
    JP("Genki Bl.6: て形 ve tüm kullanımları — Japoncanın omurgası"),
    UWC("Bilim etiği: bir araştırmacının en önemli sorumluluğu sence nedir?"),
    ANKI(),
    CLOSE("momentum korunumunun neden Newton'un 3. yasasından çıktığı"),
], bridges=["Fizik ↔ Felsefe: bilim etiği"])

U("Halliday Bl.10: Dönme + SINAV", "Hafta 2 · Faz 1 ikinci sınavı", [
    T("phys", "study", "Açısal konum/hız/ivme, sabit açısal ivme denklemleri", 35),
    T("phys", "study", "Doğrusal-açısal büyüklük ilişkileri", 20),
    T("phys", "study", "Eylemsizlik momenti: tanım, ayrık ve sürekli (integral) hesap", 50),
    T("phys", "study", "Standart cisimlerin I değerlerini TÜRET (çubuk, disk, küre, silindir, halka)", 45),
    T("phys", "study", "Paralel eksen teoremi — ispatıyla", 25),
    T("phys", "study", "Tork, dönme için 2. yasa, dönme kinetik enerjisi, iş ve güç", 40),
    T("phys", "problem", "40 soru", 85),
    T("phys", "test", "SINAV: Halliday Bl.1–10 + kalkülüs uygulamaları — 3 saat", 180),
    T("phys", "review", "Sınav yanlışlarını Hata Defteri'ne geçir, kategori ata", 40),
    JP("Japonca tekrar: Bl.1–6 gramer taraması"),
    UWC("İki sınav geçti. Kendi öğrenme yönteminde değiştirmek istediğin bir şey var mı?"),
    CLOSE("eylemsizlik momentinin fiziksel anlamı"),
], feynman="Explain what moment of inertia means physically, in English.")

# ── Hafta 3: Mekanik II + Dalgalar + Termodinamik ──
U("Halliday Bl.11: Yuvarlanma, Tork, Açısal Momentum", "Hafta 3 · Mekanik II", [
    T("phys", "study", "Yuvarlanma — kayma olmadan koşulu, kinetik enerji ayrışımı", 40),
    T("phys", "study", "Eğik düzlemde yuvarlanma, farklı cisimlerin yarışı, sürtünmenin rolü", 40),
    T("phys", "study", "Açısal momentum: parçacık ve katı cisim, korunumu", 45),
    T("phys", "study", "Buz patencisi, dönen tabure, topaç ve jiroskop presesyonu", 35),
    T("phys", "problem", "40 soru — Bl.10-11 en zor ikili, takılırsan Morin'e bak", 95),
    T("py", "study", "NumPy I: dizi oluşturma, indeksleme, broadcasting, vektörleştirme", 55),
    T("lit", "read", "Şiir II: Divan şiiri (gazel, kaside, mesnevi), aruz, mazmun; Fuzûlî ve Bâkî", 45),
    JP("Genki Bl.7: ~ている, sıfatların bağlanması"),
    UWC("Zor bir konuda takılmak — bunu başkasına nasıl anlatırdın?"),
    ANKI(),
    CLOSE("açısal momentum korunumu neden buz patencisini hızlandırır"),
])

U("Halliday Bl.12–13: Denge, Esneklik, Kütleçekim", "Hafta 3 · Mekanik II", [
    T("phys", "study", "Statik denge koşulları, merdiven/kiriş/konsol problemleri", 40),
    T("phys", "study", "Statik belirsiz yapılar, gerilme-şekil değiştirme, Young modülü", 30),
    T("phys", "study", "Newton evrensel çekim, süperpozisyon, kabuk teoremi", 40),
    T("phys", "study", "Dünya yüzeyinde ve içinde g, yerçekimi potansiyel enerjisi, kaçış hızı", 35),
    T("phys", "study", "Kepler'in üç yasası — 2. yasanın açısal momentumla ilişkisi", 40),
    T("phys", "study", "Uydu yörüngeleri, yörünge enerjisi, jeosenkron yörünge", 30),
    T("phys", "problem", "40 soru", 85),
    T("py", "study", "NumPy II: matris işlemleri, linalg, rastgele sayılar, istatistik", 50),
    DE("Präteritum (haben/sein/Modalverben), Komparativ/Superlativ"),
    UWC("Kepler verileri Tycho Brahe'nindi. Başkasının emeği üzerine inşa etmek — nasıl doğru yapılır?"),
    ANKI(),
    CLOSE("Kepler'in ikinci yasasının açısal momentumla bağlantısı"),
])

U("Halliday Bl.14–15: Akışkanlar ve Salınımlar", "Hafta 3 · Dalgalar", [
    T("phys", "study", "Yoğunluk, basınç, Pascal ilkesi, hidrolik pres", 30),
    T("phys", "study", "Arşimet ilkesi, yüzme koşulları, görünür ağırlık", 30),
    T("phys", "study", "Süreklilik denklemi, Bernoulli denklemini enerji korunumundan türet", 45),
    T("phys", "study", "Venturi, Torricelli, uçak kanadı", 25),
    T("phys", "study", "SHM'nin diferansiyel denklemi ve çözümü — kalkülüs burada devreye giriyor", 50),
    T("phys", "study", "Yay-kütle, basit/fiziksel/burulma sarkacı, SHM'de enerji", 40),
    T("phys", "study", "Sönümlü salınım (DD çözümü), zorlanmış salınım, rezonans", 45),
    T("phys", "problem", "40 soru", 85),
    T("py", "code", "Matplotlib mimarisi + fizik-py'ye SHM simülasyonu ekle", 60),
    T("phil", "read", "Estetik: güzellik nedir, taklit kuramı vs yaratıcılık kuramı", 40),
    JP("Genki Bl.8: 短形 (kısa form) — bu olmadan Japonca ilerlemez"),
    UWC("Rezonans: doğru frekansta küçük bir itiş büyük etki yaratır. Hayatta böyle bir 'doğru frekans' örneği?"),
    ANKI(),
    CLOSE("basit harmonik hareketin diferansiyel denklemi"),
], feynman="Explain why so many different systems all obey simple harmonic motion, in English.")

U("Halliday Bl.16–17: Dalgalar ve Ses", "Hafta 3 · Dalgalar", [
    T("phys", "study", "Dalga türleri, ilerleyen dalga fonksiyonu, dalga denklemi", 40),
    T("phys", "study", "Gerilmiş telde dalga hızını türet, dalgada enerji ve güç", 35),
    T("phys", "study", "Süperpozisyon, girişim, faz farkı", 35),
    T("phys", "study", "Duran dalgalar, düğüm/karın, rezonans modları, harmonikler", 40),
    T("phys", "study", "Ses dalgaları, şiddet ve desibel, borularda rezonans, vuru", 40),
    T("phys", "study", "Doppler etkisi — tüm durumlar; süpersonik hız, Mach konisi", 40),
    T("phys", "problem", "40 soru", 85),
    T("py", "code", "Dalga süperpozisyon animasyonu + vuru olayı görselleştirme", 60),
    T("lit", "read", "Destan ve efsane: Oğuz Kağan, Manas, Dede Korkut; destandan romana geçiş", 45),
    JP("Genki Bl.8 devamı, kelime 350'ye çıkar"),
    UWC("Girişim: iki dalga birleşince ya güçlenir ya söner. İnsan topluluklarında karşılığı?"),
    ANKI(),
    CLOSE("duran dalga nasıl oluşur"),
], bridges=["Fourier ↔ Müzik ↔ Dalga"])

U("Halliday Bl.18–19: Isı ve Kinetik Teori", "Hafta 3 · Termodinamik", [
    T("phys", "study", "Sıcaklık, ısıl denge, sıfırıncı yasa, ısıl genleşme, suyun anomalisi", 35),
    T("phys", "study", "Isı, öz ısı, faz değişimi, gizli ısı, ısınma eğrileri", 35),
    T("phys", "study", "Isı transferi: iletim (Fourier), taşınım, ışıma (Stefan-Boltzmann)", 30),
    T("phys", "study", "Termodinamiğin 1. yasası, P-V diyagramında iş (integral)", 40),
    T("phys", "study", "İzobarik/izokorik/izotermal/adyabatik — her biri için Q, W, ΔU", 45),
    T("phys", "study", "İdeal gaz yasası, basıncın moleküler kökenini türet, rms hız", 45),
    T("phys", "study", "Eşbölüşüm ilkesi, Cv/Cp, Mayer bağıntısı, Maxwell-Boltzmann dağılımı", 45),
    T("phys", "study", "Ortalama serbest yol, adyabatik süreç denklemini türet (PV^γ)", 35),
    T("phys", "problem", "40 soru", 85),
    T("py", "code", "Maxwell-Boltzmann dağılımı + ideal gaz parçacık simülatörü", 55),
    DE("Nebensätze (weil, dass, wenn), Reflexivverben"),
    UWC("Sıcaklık aslında istatistiksel bir kavram. 'Ortalama' bir topluluğu ne kadar iyi anlatır?"),
    ANKI(),
    CLOSE("sıcaklığın moleküler anlamı"),
])

U("Halliday Bl.20: Entropi + Enformasyon Köprüsü", "Hafta 3 · Köprü Günü", [
    T("phys", "study", "Tersinir/tersinmez süreçler, entropi tanımı ve değişim hesapları", 45),
    T("phys", "study", "İkinci yasa: Clausius ve Kelvin-Planck ifadeleri", 30),
    T("phys", "study", "Isı motorları, verim, Carnot çevrimi tam analizi", 45),
    T("phys", "study", "Otto ve Diesel çevrimleri, soğutucular ve etkinlik katsayısı", 30),
    T("phys", "study", "Entropinin istatistiksel yorumu: S = k ln W", 30),
    T("phys", "study", "KÖPRÜ: Shannon entropisi ile Boltzmann entropisi — aynı matematik", 45),
    T("py", "code", "Bir metnin Shannon entropisini hesaplayan Python programı yaz", 45),
    T("phil", "read", "Maxwell'in şeytanı, Landauer ilkesi, determinizm ve zamanın oku", 40),
    T("lit", "read", "Roman I: Tanzimat romanı, Halit Ziya, Ahmet Hamdi Tanpınar", 45),
    JP("Genki Bl.9"),
    UWC("Entropi artar ama yerel düzen kurulabilir. Bir insanın 'düzen kurması' neye mal olur?"),
    ANKI(),
    CLOSE("entropinin iki farklı tanımının neden aynı şey olduğu"),
], bridges=["Entropi ↔ Enformasyon", "Termodinamik ↔ Felsefe: zamanın oku"],
    feynman="Explain the connection between thermodynamic entropy and information entropy, in English.")

U("Fizik Sentez + Olimpiyat + SINAV", "Hafta 3 · Faz 1 üçüncü sınavı", [
    T("phys", "review", "Bl.1–20 kavram haritası çıkar — tek sayfa, kapalı kitap", 60),
    T("phys", "study", "Üç korunum yasasının (enerji, momentum, açısal momentum) birleşik kullanımı", 45),
    T("phys", "study", "Boyut analiziyle sonuç doğrulama tekniği", 25),
    T("phys", "problem", "Irodov mekanik bölümünden 15 zor soru", 110),
    T("phys", "problem", "IPhO geçmiş yıl: 2 tam problem", 90),
    T("phys", "problem", "URFODU/TÜBİTAK formatında 10 soru", 60),
    T("phys", "test", "SINAV: Halliday Bl.11–20 tam kapsam — 3 saat", 180),
    T("phys", "review", "Hata Defteri güncelle, tekrar eden hata kalıplarını işaretle", 40),
    UWC("Olimpiyat problemleri: yarışmak mı öğrenmek mi? Senin için hangisi ağır basıyor?"),
    CLOSE("üç korunum yasasının birlikte nasıl kullanıldığı"),
])

# ── Hafta 4: Python yoğunlaşma + 10. sınıf ──
U("Python: Sayısal Yöntemler", "Hafta 4 · Python Yoğunlaşma", [
    T("py", "study", "Sayısal türev: ileri/geri/merkezi fark, hata mertebeleri", 40),
    T("py", "study", "Sayısal integral: dikdörtgen, yamuk, Simpson; yakınsama hızı", 40),
    T("py", "study", "Kök bulma: ikiye bölme, Newton-Raphson, sekant", 35),
    T("py", "study", "ODE çözümü: Euler, geliştirilmiş Euler, RK4 — elle uygula", 60),
    T("py", "code", "scipy.integrate.solve_ivp kullanımı, kararlılık ve adım boyu", 40),
    T("py", "code", "Uygulama: tam sarkaç denklemi (küçük açı yaklaşımı olmadan)", 50),
    T("py", "code", "Uygulama: iki cisim problemi, yörünge çizimi", 50),
    T("math", "study", "İkinci mertebe sabit katsayılı lineer DD, karakteristik denklem", 50),
    T("phil", "read", "Bilim felsefesi: Popper yanlışlanabilirlik, Kuhn paradigma kayması", 45),
    T("phil", "write", "300 kelime: Newton→Einstein geçişi bir paradigma kayması mı? (P5)", 40),
    JP("Genki Bl.10"),
    UWC("Popper: bir iddia yanlışlanamıyorsa bilimsel değildir. Kendi hakkındaki bir inancını yanlışlayabilir misin?"),
    ANKI(),
    CLOSE("RK4'ün Euler'den neden daha iyi olduğu"),
], bridges=["Bilim Felsefesi ↔ Halliday", "Diferansiyel Denklem ↔ Salınım"])

U("Python: Veri Analizi I — Pandas", "Hafta 4 · Python Yoğunlaşma", [
    T("py", "study", "Pandas: Series, DataFrame, loc/iloc, veri okuma/yazma", 55),
    T("py", "study", "Filtreleme, sıralama, yeni sütun, groupby, pivot", 50),
    T("py", "study", "merge/join/concat, eksik veri stratejileri, temizleme iş akışı", 50),
    T("py", "code", "4006 kuantum simülasyon verilerini Pandas'a aktar ve temizle", 60),
    T("math", "study", "Olasılık: permütasyon, kombinasyon, binom, koşullu olasılık, Bayes", 60),
    T("lit", "read", "Roman II: Cumhuriyet dönemi, toplumcu gerçekçilik, Yaşar Kemal, Oğuz Atay", 45),
    DE("Genitiv, Konjunktiv II (würde)"),
    UWC("Veri temizleme: hangi veriyi atacağına karar vermek bir değer yargısı mı?"),
    ANKI(),
    CLOSE("Bayes teoreminin sezgisel anlamı"),
], bridges=["Olasılık ↔ Kuantum"])

U("Python: İstatistik ve Eğri Uydurma", "Hafta 4 · Python Yoğunlaşma", [
    T("py", "study", "Betimsel istatistik, dağılımlar (normal, binom, Poisson), scipy.stats", 50),
    T("py", "study", "Ölçüm belirsizliği ve hata yayılımı — deneysel fizik için kritik", 45),
    T("py", "study", "En küçük kareler, curve_fit, uyum iyiliği (R², ki-kare)", 50),
    T("py", "code", "Deneysel veriye model uydur, hata çubuklarıyla yayınlanabilir grafik üret", 60),
    T("py", "study", "Seaborn: dağılım, korelasyon, ısı haritası; korelasyon ≠ nedensellik", 40),
    T("chem", "study", "Kimyanın temel kanunları, mol, stokiyometri", 50),
    T("chem", "study", "Atomun yapısı, kuantum sayıları, elektron dizilimi", 50),
    JP("Genki Bl.11"),
    UWC("Korelasyon nedensellik değildir. Bu hatayı yapan bir haber/iddia hatırlıyor musun?"),
    ANKI(),
    CLOSE("hata yayılımının neden önemli olduğu"),
])

U("10. Sınıf: Kimya ve Biyoloji", "Hafta 4 · Okul Müfredatı", [
    T("chem", "study", "Periyodik sistem ve periyodik özelliklerin değişimi", 45),
    T("chem", "study", "İyonik, kovalent, metalik bağ; moleküller arası kuvvetler", 50),
    T("chem", "study", "Bileşik adlandırma, tepkime türleri, denkleştirme", 45),
    T("chem", "study", "Karışımlar, çözünürlük, derişim hesapları (molarite, %, ppm)", 45),
    T("bio", "study", "Mitoz ve mayoz evreleri, karşılaştırma, krossing-over", 50),
    T("bio", "study", "Eşeyli/eşeysiz üreme", 25),
    T("bio", "study", "Kalıtım: Mendel yasaları, monohibrit, dihibrit, kontrol çaprazlaması", 55),
    T("bio", "study", "Eşeye bağlı kalıtım, kan grupları, soy ağacı analizi", 40),
    T("bio", "study", "Ekosistem ekolojisi, madde döngüleri, enerji akışı, besin piramidi", 40),
    DE("A1 genel tekrar + A2 gramer taraması"),
    UWC("'Çevreye saygı' değeri: doğayı kaynak değil ilişki olarak görmek ne demek?"),
    ANKI(),
    CLOSE("mayozun genetik çeşitliliği nasıl ürettiği"),
])

U("10. Sınıf: Tarih, Coğrafya, Edebiyat", "Hafta 4 · Okul Müfredatı", [
    T("hist", "study", "Beylikten devlete: Osmanlı'nın kuruluşu (1302–1453)", 45),
    T("hist", "study", "Dünya gücü Osmanlı (1453–1595): fetihler, klasik dönem kurumları", 45),
    T("hist", "study", "Sultan ve merkez teşkilatı; millet sistemi, tımar, vakıf", 45),
    T("hist", "study", "Değişen dünya dengeleri (1595–1774)", 35),
    T("geo", "study", "Tektonik oluşum, iç ve dış kuvvetler, Türkiye'nin yer şekilleri", 50),
    T("geo", "study", "Su kaynakları, toprak tipleri, bitki örtüsü", 35),
    T("geo", "study", "Nüfus politikaları, göç, yerleşme", 35),
    T("lit", "study", "Tiyatro: Batı etkisinde Türk tiyatrosu, Karagöz, orta oyunu", 45),
    T("lit", "study", "Biyografi, otobiyografi, mektup, günlük türleri", 30),
    T("lit", "write", "Bir edebî metin analizi denemesi yaz (P5 portfolyo)", 60),
    JP("Genki Bl.12"),
    UWC("Millet sistemi: farklılıkla bir arada yaşamanın tarihsel bir modeli. Güçlü ve zayıf yanları?"),
    ANKI(),
], bridges=["Edebiyat ↔ Akademik Yazım"])

U("İnteraktif Tasarım + Akademik İngilizce", "Hafta 4 · Üretim", [
    T("py", "study", "Plotly: interaktif grafikler, 3B görselleştirme", 45),
    T("py", "study", "Streamlit: veri uygulaması, widget'lar, düzen", 45),
    T("py", "code", "ipywidgets ve matplotlib.animation ile parametre kontrolü", 40),
    T("prod", "produce", "P2 İnteraktif Fizik Atlası — ilk sayfa: eğik atış (React + Plotly)", 75),
    T("eng", "study", "Bilimsel makale anatomisi: Abstract/Intro/Methods/Results/Discussion", 40),
    T("eng", "study", "Akademik kalıplar, hedge language, pasif yapı, APA/AIP referans", 40),
    T("eng", "read", "arXiv'den 2 fizik makalesi oku", 60),
    T("eng", "write", "Her makaleye 250 kelimelik İngilizce özet yaz", 60),
    T("eng", "review", "Akademik kelime listesinden (AWL) 100 kelime Anki'ye", 30),
    JP("Japonca tekrar"),
    UWC("İngilizce akademik yazmak: kendi sesini kaybetmeden yazmak mümkün mü?"),
    CLOSE("bir bilimsel makalenin yapısı"),
])

U("Proje Günü + Felsefe", "Hafta 4 · Üretim", [
    T("prod", "produce", "fizik-py v1.0: dokümantasyon, README, testler, GitHub yayını", 90),
    T("prod", "produce", "P2 Fizik Atlası: dairesel hareket ve salınım sayfaları", 90),
    T("prod", "produce", "P4 kotoba başlangıç: SM-2 algoritması ve veri modeli tasarımı", 75),
    T("phil", "study", "Siyaset felsefesi: toplum sözleşmesi (Hobbes, Locke, Rousseau), Rawls", 50),
    T("phil", "study", "Din felsefesi: Tanrı kanıtları, teodise problemi", 35),
    T("phil", "write", "Felsefi deneme: 'Bilimsel bilgi nesnel midir?' (P5 portfolyo)", 70),
    DE("A2 giriş: Passiv, Relativsätze"),
    UWC("Rawls'un 'cehalet perdesi': hangi ailede doğacağını bilmesen nasıl bir dünya tasarlardın?"),
    ANKI(),
])

# ── Hafta 5: Konsolidasyon ──
U("Fizik Tam Tekrar", "Hafta 5 · Konsolidasyon", [
    T("phys", "review", "Bl.1–20 kavram haritasını kapalı kitap yeniden çiz", 70),
    T("phys", "problem", "Her bölümden en zor 3 problem — toplam 60 problem", 180),
    T("phys", "review", "Hata Defteri'ndeki TÜM soruları yeniden çöz", 90),
    T("phys", "review", "Zayıf çıkan 3 bölümü belirle ve hedefli tekrar yap", 90),
    T("phys", "explain", "En zayıf bölümü İngilizce anlat ve kaydet", 25),
    UWC("Dört haftalık yansımalarını oku. Bir örüntü görüyor musun?"),
])

U("Matematik + Python Tam Tekrar", "Hafta 5 · Konsolidasyon", [
    T("math", "test", "Kalkülüs BC full deneme — süreli, 3 saat", 180),
    T("math", "review", "Taylor açılımları ve integral teknikleri ezber kontrolü", 45),
    T("py", "code", "fizik-py'yi kütüphaneye bakmadan sıfırdan yeniden yazmayı dene", 90),
    T("math", "review", "10. sınıf matematik hızlı tarama", 60),
    T("math", "review", "Hata Defteri matematik bölümünü yeniden çöz", 60),
    CLOSE("kalkülüsün tek bir cümlelik özeti"),
])

U("Diller + Faz 2 Hazırlığı", "Hafta 5 · Konsolidasyon", [
    JP("N5 birinci yarı tam tekrar: hiragana, katakana, kanji 1–50, Genki Bl.1–12", 90),
    DE("A1 tam tekrar + A2 gramer haritası çıkar", 75),
    T("eng", "review", "Tüm Anki destesinin bir kez tam geçişi", 60),
    T("prod", "produce", "Okul programına göre Faz 2 takvimini kişiselleştir", 45),
    T("prod", "read", "10. sınıf ders kitaplarını önceden tara — neyin geleceğini bil", 60),
    UWC("Faz 1 bitti. UWC başvurusu için hangi hikâyeyi anlatabileceğini bir paragrafta yaz."),
    T("uwc", "study", "UWC başvuru takvimini kontrol et, açılış tarihini takvime gir", 15),
])

P("p1", "Temeller", "Faz 1 · ~325 saat",
  "Matematik ve programlama temelini kur, mekaniği bitir.", 325, "u001", "u031")

# ══════════════════════════════════════════════════════════════════
# FAZ 2 — DERİNLEŞME (12 birim)
# ══════════════════════════════════════════════════════════════════

U("Çok Değişkenli I: Vektörler ve Kısmi Türev", "Faz 2 · Hafta 1", [
    T("math", "study", "3B uzayda vektörler, doğru ve düzlem denklemleri", 50),
    T("math", "study", "Vektör değerli fonksiyonlar, uzay eğrileri, eğrilik", 50),
    T("math", "study", "Çok değişkenli fonksiyonlar, seviye eğrileri ve yüzeyleri", 40),
    T("math", "study", "Kısmi türev, üst mertebeden kısmi türev, Clairaut teoremi", 50),
    T("math", "problem", "30 soru", 70),
    JP("Genki Bl.13, kanji 55–62"),
    UWC("Okul başladı. Ritmi korumak için neyi değiştirmen gerekiyor?"),
    ANKI(),
])

U("Çok Değişkenli I: Gradyan ve Teğet Düzlem", "Faz 2 · Hafta 1", [
    T("math", "study", "Çok değişkenli zincir kuralı", 40),
    T("math", "study", "Gradyan vektörü, yönlü türev, geometrik anlamı", 55),
    T("math", "study", "Teğet düzlem, doğrusal yaklaşım, toplam diferansiyel", 40),
    T("phys", "study", "KÖPRÜ: F = −∇U, eşpotansiyel yüzeyler", 40),
    T("math", "problem", "30 soru", 70),
    T("py", "code", "Gradyan alanı görselleştirici (quiver plot) yaz", 45),
    DE("A2: Nebensätze tam, Präteritum tekrar"),
    ANKI(),
    CLOSE("gradyanın neden en dik yükseliş yönünü verdiği"),
], bridges=["Vektör Analizi ↔ Elektrik Alan"],
    feynman="Explain what the gradient vector tells you geometrically, in English.")

U("Halliday Tekrar + Okul Uyumu", "Faz 2 · Hafta 1", [
    T("phys", "review", "Halliday Bl.1–5 yeniden — bu sefer Irodov seviyesi problemlerle", 90),
    T("phys", "problem", "Irodov'dan 8 zor mekanik problemi", 80),
    T("prod", "produce", "Okul ödevleri ve derse hazırlık", 60),
    T("lit", "read", "Okul edebiyat müfredatı takibi", 45),
    JP("Genki Bl.13 devamı, kelime 420"),
    UWC("Perspektif alıştırması: okuldaki bir arkadaşınla yaşadığın bir anı iki gözden yaz"),
    ANKI(),
])

U("Çok Değişkenli II: Ekstremum ve Lagrange", "Faz 2 · Hafta 2", [
    T("math", "study", "Çok değişkenli ekstremum, kritik noktalar, ikinci türev testi", 55),
    T("math", "study", "Lagrange çarpanları — kısıtlı optimizasyon", 55),
    T("math", "problem", "25 soru", 70),
    T("py", "code", "scipy.optimize ile kısıtlı optimizasyon çöz, elle çözümle karşılaştır", 45),
    JP("Genki Bl.14, kanji 63–70"),
    UWC("Kısıt altında optimizasyon: sınırlı zamanla en iyi sonucu almak nasıl bir beceri?"),
    ANKI(),
])

U("Çok Değişkenli II: Çoklu İntegral", "Faz 2 · Hafta 2", [
    T("math", "study", "İki katlı integral, genel bölgelerde integral", 50),
    T("math", "study", "Kutupsal koordinatlarda integral", 40),
    T("math", "study", "Üç katlı integral, silindirik ve küresel koordinatlar", 55),
    T("math", "study", "Jakobiyen, değişken dönüşümü", 40),
    T("math", "problem", "30 soru", 75),
    DE("A2: Passiv, Relativsätze uygulama"),
    ANKI(),
    CLOSE("koordinat dönüşümünde Jakobiyenin ne yaptığı"),
])

U("Fizik Uygulaması + Python", "Faz 2 · Hafta 2", [
    T("phys", "study", "Sürekli cisimlerde kütle merkezi — çoklu integralle", 55),
    T("phys", "study", "Eylemsizlik momenti hesapları — üç katlı integralle", 55),
    T("py", "code", "Çok katlı integralin sayısal çözümü + 3B görselleştirme", 60),
    T("prod", "produce", "fizik-py: RK4 ODE çözücü ekle", 60),
    T("prod", "produce", "fizik-py: sarkaç ve sönümlü salınım modülleri", 60),
    JP("Genki Bl.14 devamı"),
    UWC("Bir projeyi 'bitirmek' ile 'yayınlamak' arasındaki fark seni nasıl etkiliyor?"),
    ANKI(),
])

U("Vektör Analizi: Çizgi İntegrali ve Green", "Faz 2 · Hafta 3", [
    T("math", "study", "Vektör alanları, çizgi integrali", 50),
    T("math", "study", "Korunumlu vektör alanı, potansiyel fonksiyon bulma", 50),
    T("math", "study", "Kalkülüsün Temel Teoremi — çizgi integrali versiyonu", 35),
    T("math", "study", "Green teoremi", 45),
    T("math", "study", "Rotasyonel (curl) ve diverjans — fiziksel anlamları", 50),
    T("math", "problem", "25 soru", 70),
    JP("Genki Bl.15, kanji 71–78"),
    ANKI(),
    CLOSE("diverjans ve rotasyonelin fiziksel anlamı"),
], feynman="Explain what divergence and curl mean physically, in English.")

U("Lineer Cebir I: Matrisler ve Sistemler", "Faz 2 · Hafta 3", [
    T("math", "study", "3Blue1Brown — Essence of Linear Algebra Bl.1–5 izle", 60),
    T("math", "study", "Matrisler, determinant, ters matris, Cramer kuralı", 55),
    T("math", "study", "Lineer denklem sistemleri, Gauss eliminasyonu, rank", 55),
    T("math", "problem", "25 soru", 65),
    T("py", "code", "NumPy linalg ile sistem çöz, elle Gauss eliminasyonu da kodla", 50),
    DE("A2: Konjunktiv II, dolaylı anlatım"),
    ANKI(),
])

U("Halliday Tekrar + P4 Başlangıç", "Faz 2 · Hafta 3", [
    T("phys", "review", "Halliday Bl.6–13 yeniden, zor problemlerle", 90),
    T("phys", "problem", "Irodov'dan 8 problem (enerji, momentum, dönme)", 85),
    T("prod", "produce", "P4 kotoba: SM-2 algoritmasını Python'da uygula", 75),
    T("prod", "produce", "P4 kotoba: kanji/kelime veritabanı şeması", 60),
    JP("Genki Bl.15 devamı, kelime 500"),
    UWC("kotoba: kendi öğrenme aracını yapmak — bu senin hakkında ne söylüyor?"),
    ANKI(),
], bridges=["Japonca ↔ Programlama"])

U("Lineer Cebir II: Özdeğer ve Özvektör", "Faz 2 · Hafta 4", [
    T("math", "study", "Vektör uzayı, alt uzay, taban, boyut", 50),
    T("math", "study", "Lineer dönüşümler ve matris temsili", 45),
    T("math", "study", "Özdeğer, özvektör, köşegenleştirme", 65),
    T("math", "problem", "25 soru", 70),
    T("py", "code", "Özdeğer hesaplayıcı yaz, NumPy sonucuyla karşılaştır", 45),
    JP("Genki Bl.16, kanji 79–85"),
    ANKI(),
    CLOSE("özvektörün geometrik anlamı"),
])

U("Fizik ve Kuantum Köprüsü", "Faz 2 · Hafta 4", [
    T("phys", "study", "Eylemsizlik tensörü, temel eksenler", 50),
    T("phys", "study", "Bağlı salınımların normal modları — özdeğer problemi olarak", 60),
    T("phys", "study", "KÖPRÜ: Hilbert uzayı, operatörler, özdurum kavramı", 55),
    T("py", "code", "İki bağlı sarkaç simülasyonu — normal modları göster", 60),
    T("prod", "produce", "fizik-py: dalga süperpozisyon modülü", 50),
    DE("A2 tekrar ve deneme"),
    UWC("Kuantum mekaniğinin dili lineer cebir. Bir alanın 'dili' olması ne demek?"),
    ANKI(),
], bridges=["Lineer Cebir ↔ Kuantum"],
    feynman="Explain why quantum mechanics uses linear algebra, in English.")

U("P4 Tamamlama + Faz 2 Sınavı", "Faz 2 · Hafta 4", [
    T("prod", "produce", "P4 kotoba: arayüz ve ilerleme grafikleri, çalışır sürüm", 90),
    T("prod", "produce", "P5: bu ayın iki denemesini yaz (1 Türkçe, 1 İngilizce)", 90),
    T("math", "test", "SINAV: Çok değişkenli + lineer cebir + Halliday 1–20 — 4 saat", 240),
    T("math", "review", "Sınav yanlışlarını Hata Defteri'ne geçir", 45),
    T("prod", "review", "Haftalık değerlendirme: ne ürettim, neyi kitapsız anlatabiliyorum?", 30),
    UWC("Faz 2 bitti. Başvuru dosyan için somut bir eylem başlat."),
])

P("p2", "Derinleşme", "Faz 2 · ~175 saat",
  "Çok değişkenli matematiğe geç, Halliday Cilt 1'i kapat, okul ritmine otur.", 175, "u032", "u043")

# ══════════════════════════════════════════════════════════════════
# FAZ 3 — ELEKTROMANYETİZMA (12 birim)
# ══════════════════════════════════════════════════════════════════

U("Bl.21–22: Coulomb Yasası ve Elektrik Alan", "Faz 3 · Hafta 1", [
    T("phys", "study", "Elektrik yükü, korunumu, kuantumlanması, Coulomb yasası", 45),
    T("phys", "study", "Süperpozisyon ilkesi, çok yüklü sistemler", 40),
    T("phys", "study", "Elektrik alan, alan çizgileri, nokta yük ve dipol alanı", 50),
    T("phys", "study", "Sürekli yük dağılımlarının alanı — integral hesabı", 55),
    T("phys", "study", "Dipolün elektrik alandaki torku ve potansiyel enerjisi", 35),
    T("phys", "problem", "40 soru", 90),
    JP("Genki Bl.17, kanji 86–92"),
    ANKI(),
    CLOSE("elektrik alanın neden bir alan olarak tanımlandığı"),
])

U("Bl.23: Gauss Yasası", "Faz 3 · Hafta 1", [
    T("phys", "study", "Elektrik akı kavramı", 35),
    T("phys", "study", "Gauss yasası — ifadesi ve Coulomb'dan türetimi", 50),
    T("phys", "study", "Küresel, silindirik, düzlemsel simetriye uygulama", 60),
    T("phys", "study", "İletkenlerde yük dağılımı, kafes etkisi", 40),
    T("math", "study", "KÖPRÜ: Gauss yasası ↔ diverjans teoremi (Faz 2'deki vektör analizi)", 45),
    T("phys", "problem", "35 soru", 85),
    DE("A2 pekiştirme"),
    ANKI(),
    CLOSE("Gauss yasasının ne zaman işe yaradığı"),
], bridges=["Vektör Analizi ↔ Elektrik Alan"],
    feynman="Explain when Gauss's law is useful and when it isn't, in English.")

U("Python: Alan Haritalama", "Faz 3 · Hafta 1", [
    T("py", "code", "fizik-py: elektrik alan haritalayıcı (nokta yükler, dipol)", 75),
    T("py", "code", "Alan çizgisi çizim algoritması", 60),
    T("prod", "produce", "P2 Fizik Atlası: elektrik alan çizgileri sayfası", 75),
    T("phys", "review", "Bl.21–23 tekrar, kavram haritası", 45),
    JP("Genki Bl.17 devamı"),
    UWC("Görselleştirmek anlamayı değiştiriyor mu, yoksa sadece gösteriyor mu?"),
    ANKI(),
])

U("Bl.24: Elektrik Potansiyel", "Faz 3 · Hafta 2", [
    T("phys", "study", "Potansiyel enerji, elektrik potansiyeli, eşpotansiyel yüzeyler", 50),
    T("phys", "study", "Potansiyelden alan hesabı (gradyan) — Faz 2 matematiği burada", 50),
    T("phys", "study", "Alandan potansiyel hesabı (çizgi integrali)", 45),
    T("phys", "study", "Yük dağılımlarının potansiyeli, iletkenlerde potansiyel", 45),
    T("phys", "problem", "40 soru", 90),
    T("py", "code", "fizik-py: potansiyel hesabı ve eşpotansiyel eğri çizimi", 55),
    JP("Genki Bl.18, kanji 93–100"),
    ANKI(),
    CLOSE("potansiyel ile alan arasındaki gradyan ilişkisi"),
])

U("Bl.25–26: Kapasitans, Akım ve Direnç", "Faz 3 · Hafta 2", [
    T("phys", "study", "Kapasitans, seri/paralel bağlama, depolanan enerji", 45),
    T("phys", "study", "Enerji yoğunluğu, dielektrikler, dielektrikli Gauss yasası", 45),
    T("phys", "study", "Akım, akım yoğunluğu, sürüklenme hızı", 40),
    T("phys", "study", "Direnç, özdirenç, Ohm yasası, güç", 40),
    T("phys", "problem", "35 soru", 85),
    DE("A2 tekrar"),
    ANKI(),
])

U("Köprü + Tekrar + P3 Başlangıç", "Faz 3 · Hafta 2", [
    T("phys", "review", "Bl.21–26 kavram haritası ve zor problem seti", 90),
    T("phys", "problem", "Irodov elektrostatik: 8 problem", 85),
    T("prod", "produce", "P3: 4006 verilerinin Pandas ile yeniden analizi", 75),
    T("prod", "write", "P3: Methods bölümünün İngilizce taslağı", 70),
    JP("Genki Bl.18 devamı, kelime 600"),
    UWC("Kendi araştırmanı yazmak: en zor kısmı ne olacak sence?"),
    ANKI(),
])

U("Bl.27: Devreler", "Faz 3 · Hafta 3", [
    T("phys", "study", "Emk, iç direnç, tek ilmekli devreler", 40),
    T("phys", "study", "Kirchhoff yasaları, çok ilmekli devreler", 55),
    T("phys", "study", "RC devreleri — diferansiyel denklem çözümü, zaman sabiti", 55),
    T("phys", "problem", "40 soru", 90),
    T("py", "code", "fizik-py: matris tabanlı devre çözücü (Kirchhoff)", 70),
    JP("Genki Bl.19, N4 kanji girişi"),
    ANKI(),
    CLOSE("RC devresindeki üstel davranışın nereden geldiği"),
], bridges=["Diferansiyel Denklem ↔ Devreler"])

U("Bl.28: Manyetik Alan", "Faz 3 · Hafta 3", [
    T("phys", "study", "Manyetik alan, Lorentz kuvveti", 40),
    T("phys", "study", "Yüklü parçacığın manyetik alandaki hareketi, siklotron", 50),
    T("phys", "study", "Hız seçici, Hall etkisi", 35),
    T("phys", "study", "Akım taşıyan telde kuvvet, manyetik dipol momenti, tork", 45),
    T("phys", "problem", "40 soru", 90),
    T("py", "code", "Siklotron ve yüklü parçacık hareketi simülasyonu", 60),
    DE("A2 sınav hazırlığı"),
    ANKI(),
])

U("Bl.29: Akımların Manyetik Alanı", "Faz 3 · Hafta 3", [
    T("phys", "study", "Biot-Savart yasası ve uygulamaları", 55),
    T("phys", "study", "Ampère yasası, simetrik durumlara uygulama", 55),
    T("phys", "study", "Solenoid ve toroid, paralel tellerin etkileşimi", 45),
    T("phys", "problem", "35 soru", 85),
    T("py", "code", "fizik-py: manyetik alan görselleştirme (tel, halka, solenoid)", 65),
    JP("Genki Bl.19 devamı"),
    ANKI(),
    CLOSE("Ampère yasası ile Gauss yasasının yapısal benzerliği"),
])

U("Bl.30: İndüksiyon ve İndüktans", "Faz 3 · Hafta 4", [
    T("phys", "study", "Manyetik akı, Faraday yasası, Lenz yasası", 50),
    T("phys", "study", "Hareket emk'sı, indüklenen elektrik alan", 45),
    T("phys", "study", "İndüktans, öz indüksiyon, RL devreleri (DD çözümü)", 55),
    T("phys", "study", "Manyetik alanda depolanan enerji, karşılıklı indüksiyon", 40),
    T("phys", "problem", "40 soru", 90),
    JP("Genki Bl.20"),
    ANKI(),
    CLOSE("Lenz yasasının enerji korunumuyla ilişkisi"),
])

U("Bl.31: EM Salınımlar ve AC", "Faz 3 · Hafta 4", [
    T("phys", "study", "LC salınımı — SHM'nin elektriksel karşılığı", 50),
    T("phys", "study", "RLC devresi, sönümlü salınım analojisi", 50),
    T("phys", "study", "Alternatif akım, fazör, empedans, rezonans", 55),
    T("phys", "study", "Transformatör, güç iletimi", 30),
    T("phys", "problem", "35 soru", 85),
    T("py", "code", "RLC devresi simülasyonu, rezonans eğrisi", 60),
    DE("A2 tamamlama"),
    ANKI(),
], bridges=["Salınım ↔ Devre: aynı diferansiyel denklem"])

U("Bl.32: Maxwell Denklemleri + Faz 3 Sınavı", "Faz 3 · Hafta 4", [
    T("phys", "study", "Gauss (elektrik ve manyetik), Faraday, Ampère-Maxwell", 60),
    T("phys", "study", "Yer değiştirme akımı ve neden gerekli olduğu", 45),
    T("phys", "study", "Maxwell denklemlerinin bütünü — ışığın EM dalga olduğu öngörüsü", 55),
    T("phys", "study", "Maddenin manyetizması: dia-, para-, ferromanyetizma", 40),
    T("phys", "test", "SINAV: Halliday Bl.21–32 + olimpiyat seviyesi 5 problem — 4 saat", 240),
    T("phys", "review", "Hata Defteri güncelle", 40),
    T("prod", "write", "P3: Results bölümünün İngilizce taslağı", 70),
    T("prod", "produce", "P5: bu ayın iki denemesi", 90),
    UWC("Maxwell dört denklemi birleştirdi ve ışığı buldu. Birleştirme neden bu kadar güçlü?"),
    CLOSE("Maxwell denklemlerinin birlikte ne söylediği"),
], feynman="Explain how Maxwell's equations predict the existence of light, in English.")

P("p3", "Elektromanyetizma", "Faz 3 · ~175 saat",
  "Halliday Cilt 2 elektromanyetizmayı tamamla, araştırma makalesine başla.", 175, "u044", "u055")

# ══════════════════════════════════════════════════════════════════
# FAZ 4 — SENTEZ VE ÜRETİM (12 birim)
# ══════════════════════════════════════════════════════════════════

U("Bl.33: Elektromanyetik Dalgalar", "Faz 4 · Hafta 1", [
    T("phys", "study", "EM dalga denklemi, ışık hızının Maxwell'den çıkışı", 50),
    T("phys", "study", "Poynting vektörü, radyasyon basıncı", 40),
    T("phys", "study", "Polarizasyon, Malus yasası", 40),
    T("phys", "study", "Yansıma ve kırılma, Snell yasası, tam yansıma, Brewster açısı", 50),
    T("phys", "problem", "35 soru", 85),
    JP("N4 gramer girişi, kanji devam"),
    ANKI(),
    CLOSE("ışığın neden elektromanyetik bir dalga olduğu"),
])

U("Bl.34: Görüntüler ve Mercekler", "Faz 4 · Hafta 1", [
    T("phys", "study", "Düzlem ve küresel aynalar, ayna denklemi", 45),
    T("phys", "study", "İnce mercekler, mercek yapımcısı denklemi", 50),
    T("phys", "study", "Mercek sistemleri, büyütme", 40),
    T("phys", "study", "Optik aletler: büyüteç, mikroskop, teleskop", 40),
    T("phys", "problem", "35 soru", 85),
    T("prod", "produce", "P2 Fizik Atlası: mercek sistemleri sayfası", 75),
    DE("A2 tamamlama sınavı ve değerlendirme"),
    ANKI(),
])

U("Python: Işın İzleme", "Faz 4 · Hafta 1", [
    T("py", "code", "fizik-py: ışın izleme (ray tracing) modülü", 90),
    T("py", "code", "Mercek ve ayna sistemleri için otomatik ışın diyagramı üretici", 75),
    T("prod", "produce", "P1 fizik-py: optik modülü dokümantasyonu", 50),
    T("phys", "review", "Bl.33–34 tekrar ve zor problemler", 60),
    JP("N4 kelime çalışması"),
    UWC("Kendi kütüphaneni yazmak: dört ay önce bunu yapabilir miydin?"),
    ANKI(),
])

U("Bl.35: Girişim", "Faz 4 · Hafta 2", [
    T("phys", "study", "Young çift yarık deneyi, girişim koşulları", 55),
    T("phys", "study", "Faz farkı, yol farkı, girişim deseni analizi", 45),
    T("phys", "study", "İnce film girişimi, Michelson interferometresi", 50),
    T("phys", "problem", "35 soru", 85),
    T("prod", "produce", "P2 Fizik Atlası: dalga girişimi sayfası", 70),
    JP("N4 gramer"),
    ANKI(),
    CLOSE("çift yarık deneyinin neden bu kadar önemli olduğu"),
])

U("Bl.36: Kırınım", "Faz 4 · Hafta 2", [
    T("phys", "study", "Tek yarık kırınımı, şiddet dağılımı", 50),
    T("phys", "study", "Çözünürlük ve Rayleigh ölçütü", 40),
    T("phys", "study", "Kırınım ağı, spektroskopi", 45),
    T("phys", "study", "X-ışını kırınımı ve Bragg yasası", 40),
    T("phys", "problem", "35 soru", 85),
    T("py", "code", "Kırınım deseni hesaplayıcı ve görselleştirici", 60),
    DE("Almanca pekiştirme"),
    ANKI(),
])

U("Bl.37: Özel Görelilik", "Faz 4 · Hafta 2", [
    T("phys", "study", "Göreliliğin iki postülatı ve tarihsel arka planı", 45),
    T("phys", "study", "Zaman genleşmesi, uzunluk kısalması, eşzamanlılığın göreliliği", 60),
    T("phys", "study", "Lorentz dönüşümleri, göreli hız toplama", 55),
    T("phys", "study", "Görelilikte momentum ve enerji, E = mc²", 55),
    T("phys", "study", "KÖPRÜ: Maxwell denklemleri neden Galile dönüşümü altında değişmez değil", 45),
    T("phys", "problem", "30 soru", 80),
    T("phil", "read", "Zaman felsefesi: eşzamanlılık göreliyse 'şimdi' nedir?", 40),
    JP("N4 devam"),
    ANKI(),
    CLOSE("eşzamanlılığın neden göreli olduğu"),
], bridges=["Görelilik ↔ Felsefe: zamanın doğası"],
    feynman="Explain why simultaneity is relative, in English.")

U("Bl.38: Fotonlar ve Madde Dalgaları", "Faz 4 · Hafta 3", [
    T("phys", "study", "Siyah cisim ışıması, Planck hipotezi, ultraviyole felaketi", 55),
    T("phys", "study", "Fotoelektrik olay, Einstein'ın açıklaması", 45),
    T("phys", "study", "Compton saçılması", 40),
    T("phys", "study", "de Broglie dalga boyu, elektron kırınımı", 45),
    T("phys", "study", "Heisenberg belirsizlik ilkesi", 45),
    T("phys", "study", "Dalga fonksiyonu, olasılık yorumu, kutudaki parçacık", 55),
    T("phys", "problem", "30 soru", 80),
    JP("N4 devam"),
    ANKI(),
    CLOSE("belirsizlik ilkesinin ne olduğu ve ne olmadığı"),
], bridges=["Olasılık ↔ Kuantum"],
    feynman="Explain the uncertainty principle without saying 'the observer disturbs the system', in English.")

U("Kuantum Köprüsü + P3 Makale", "Faz 4 · Hafta 3", [
    T("phys", "study", "4006 projenin teorik temeli: kuantum simülasyonunun dayandığı formalizm", 60),
    T("math", "study", "Lineer cebir ↔ kuantum: durum vektörü, operatör, özdeğer ölçüm", 55),
    T("prod", "write", "P3: Introduction bölümünü yaz (İngilizce, 800 kelime)", 90),
    T("prod", "write", "P3: Abstract taslağı (250 kelime)", 50),
    T("prod", "produce", "P3: tüm şekilleri yayın kalitesine getir", 60),
    UWC("Bir makale yazmak: kime yazıyorsun ve neden okumalılar?"),
    ANKI(),
], bridges=["Lineer Cebir ↔ Kuantum", "Olasılık ↔ Kuantum"])

U("Olimpiyat Kampı I", "Faz 4 · Hafta 3", [
    T("phys", "problem", "Irodov mekanik: 8 zor problem", 120),
    T("phys", "problem", "Irodov elektromanyetizma: 8 zor problem", 120),
    T("phys", "review", "Çözemediklerini Hata Defteri'ne detaylı yaz", 45),
    T("phys", "study", "Olimpiyat teknikleri: boyut analizi, limit durumları, simetri argümanı", 50),
    JP("N4 devam"),
    ANKI(),
])

U("Olimpiyat Kampı II", "Faz 4 · Hafta 4", [
    T("phys", "problem", "IPhO geçmiş yıl: tam sınav simülasyonu 1 (5 saat)", 300),
    T("phys", "review", "Sınav analizi: hangi konuda kaç puan kaybettin?", 45),
    T("phys", "problem", "APhO geçmiş yıl: 3 problem", 120),
    T("phys", "review", "Hata Defteri'nde tekrar eden kalıpları çıkar", 40),
    UWC("Zorlandığın bir problemi çözemeden bırakmak — bunu nasıl karşılıyorsun?"),
])

U("P3 Makale Tamamlama", "Faz 4 · Hafta 4", [
    T("prod", "write", "P3: Discussion bölümü (İngilizce)", 90),
    T("prod", "write", "P3: Conclusion ve Limitations", 60),
    T("prod", "write", "P3: referanslar, alıntı biçimi (AIP), kaynakça", 50),
    T("prod", "review", "P3: baştan sona oku, dil ve tutarlılık düzeltmesi", 75),
    T("prod", "produce", "P3: tam taslağı PDF olarak dışa aktar", 40),
    T("eng", "explain", "Makaleni 3 dakikada İngilizce anlat ve kaydet", 30),
    UWC("Makale bitti. Bunu UWC başvurunda nasıl anlatırsın? Tek paragraf yaz."),
])

U("Yayın + Genel Deneme", "Faz 4 · Hafta 4", [
    T("prod", "produce", "P1 fizik-py v2.0: E&M + optik modülleri, tam dokümantasyon, GitHub yayını", 120),
    T("prod", "produce", "P2 Fizik Atlası: kalan sayfaları tamamla ve yayına al", 120),
    T("prod", "produce", "P5: son iki deneme — portfolyo 16 yazıya tamamlansın", 90),
    T("phys", "test", "GENEL DENEME: fizik + matematik tam kapsam — 5 saat", 300),
    T("prod", "review", "Dört ayın çıktı envanterini çıkar", 45),
    T("prod", "produce", "Ocak–Mart planını yaz: olimpiyat, SAT, AP, TÜBİTAK 2204-A", 60),
    UWC("Dört ay bitti. Başladığın yerden nereye geldin? Uzun yaz — bu essay'inin çekirdeği."),
    CLOSE("dört ayda öğrendiğin en önemli şey"),
])

P("p4", "Sentez ve Üretim", "Faz 4 · ~180 saat",
  "Optik, modern fizik, olimpiyat kampı, makale ve projelerin tamamlanması.", 180, "u056", "u067")

# ══════════════════════════════════════════════════════════════════
# PROJELER
# ══════════════════════════════════════════════════════════════════

projects = [
    {"id": "P1", "name": "fizik.py", "emoji": "🐍",
     "goal": "Kendi sayısal fizik kütüphaneni sıfırdan yaz ve GitHub'da yayınla.",
     "phases": {
         "p1": "1D/2D kinematik, vektör sınıfı, sabit ivme çözücü, hava dirençli eğik atış",
         "p2": "RK4 ODE çözücü, sarkaç, sönümlü/zorlanmış salınım, dalga süperpozisyonu",
         "p3": "Elektrik alan haritalama, potansiyel hesabı, yük dağılımları, devre çözücü",
         "p4": "Işın izleme, Fourier analizi, tam dokümantasyon, GitHub yayını"},
     "topics": ["Kalkülüs", "Diferansiyel Denklem", "Mekanik", "Elektromanyetizma", "Optik"],
     "next": "GitHub'da fizik-py reposunu aç ve README yaz"},
    {"id": "P2", "name": "İnteraktif Fizik Atlası", "emoji": "🗺️",
     "goal": "React + Plotly ile parametre kaydırıcılı interaktif fizik sayfaları.",
     "phases": {
         "p1": "Eğik atış sayfası — ilk prototip",
         "p2": "Dairesel hareket ve salınım sayfaları",
         "p3": "Elektrik alan çizgileri sayfası",
         "p4": "Dalga girişimi ve mercek sistemleri, yayına alma"},
     "topics": ["Mekanik", "Salınım", "Dalgalar", "Elektrik Alan", "Optik", "Python"],
     "next": "Eğik atış sayfasının React iskeletini kur"},
    {"id": "P3", "name": "Kuantum Simülasyon Makalesi", "emoji": "📄",
     "goal": "4006 projeni İngilizce akademik makaleye dönüştür.",
     "phases": {
         "p1": "Danışman iletişimi, literatür taraması",
         "p2": "Veri yeniden analizi planı",
         "p3": "Methods ve Results bölümleri",
         "p4": "Introduction, Discussion, Conclusion, referanslar, PDF"},
     "topics": ["Kuantum", "Lineer Cebir", "Olasılık", "Akademik İngilizce", "Pandas"],
     "next": "Danışman hocana literatür taraması için e-posta yaz"},
    {"id": "P4", "name": "kotoba", "emoji": "🇯🇵",
     "goal": "Kendi Japonca SRS uygulamanı yap — SM-2, kanji veritabanı, ilerleme grafikleri.",
     "phases": {
         "p1": "Veri modeli tasarımı (kağıt üzerinde)",
         "p2": "SM-2 algoritması, veritabanı, arayüz, çalışır sürüm",
         "p3": "Günlük kullanım + iyileştirmeler",
         "p4": "İstatistik paneli ve dışa aktarma"},
     "topics": ["Python", "OOP", "Veritabanı", "Japonca"],
     "next": "SM-2 algoritmasının nasıl çalıştığını okuyup not al"},
    {"id": "P5", "name": "Deneme Portfolyosu", "emoji": "✍️",
     "goal": "Ayda 2 deneme — biri Türkçe (edebiyat/felsefe), biri İngilizce (bilim/felsefe). Hedef 16 yazı.",
     "phases": {
         "p1": "Zenon denemesi, bilim felsefesi denemesi, metin analizi, 'Bilimsel bilgi nesnel midir?'",
         "p2": "İki deneme",
         "p3": "İki deneme",
         "p4": "Son iki deneme, portfolyo derlemesi"},
     "topics": ["Edebiyat", "Felsefe", "Akademik İngilizce"],
     "next": "Zenon paradoksu üzerine 150 kelimelik ilk yazıyı yaz"},
]

# ══════════════════════════════════════════════════════════════════
# SINAVLAR
# ══════════════════════════════════════════════════════════════════

assessments = [
    {"id": "a1", "name": "Kalkülüs BC", "scope": "Limit, türev, integral, seri, diferansiyel denklem",
     "hours": 3, "phase": "p1", "unit": "u007"},
    {"id": "a2", "name": "Halliday Bl.1–10", "scope": "Ölçme, kinematik, Newton yasaları, enerji, momentum, dönme",
     "hours": 3, "phase": "p1", "unit": "u014"},
    {"id": "a3", "name": "Halliday Bl.11–20", "scope": "Yuvarlanma, denge, kütleçekim, akışkanlar, salınım, dalga, termodinamik",
     "hours": 3, "phase": "p1", "unit": "u021"},
    {"id": "a4", "name": "Faz 1 Genel", "scope": "Kalkülüs BC + Halliday Bl.1–20 karma",
     "hours": 4, "phase": "p1", "unit": "u030"},
    {"id": "a5", "name": "Faz 2 Genel", "scope": "Çok değişkenli + lineer cebir + Halliday Bl.1–20",
     "hours": 4, "phase": "p2", "unit": "u043"},
    {"id": "a6", "name": "Faz 3 Genel", "scope": "Halliday Bl.21–32 + olimpiyat seviyesi problemler",
     "hours": 4, "phase": "p3", "unit": "u055"},
    {"id": "a7", "name": "Genel Deneme", "scope": "Fizik ve matematik tam kapsam",
     "hours": 5, "phase": "p4", "unit": "u067"},
]

# ══════════════════════════════════════════════════════════════════
# KÖPRÜLER
# ══════════════════════════════════════════════════════════════════

bridges = [
    {"n": "Entropi ↔ Enformasyon", "e": "🔥",
     "d": "Boltzmann entropisi ile Shannon entropisi aynı matematiği paylaşır. Termodinamik + bilgisayar bilimi + felsefe (determinizm, zamanın oku).",
     "t": ["Termodinamik", "Python", "Felsefe"]},
    {"n": "Bilim Felsefesi ↔ Halliday", "e": "🔭",
     "d": "Popper'ın yanlışlanabilirliği ve Kuhn'un paradigma kayması. Newton→Einstein geçişi tam bir Kuhn örneği.",
     "t": ["Felsefe", "Fizik", "Görelilik"]},
    {"n": "Kalkülüs ↔ Felsefe", "e": "♾️",
     "d": "Zenon paradoksları ve limit kavramı. Sonsuz küçük tartışması, Berkeley'in eleştirisi, gerçek sayıların inşası.",
     "t": ["Limit", "Felsefe", "Seriler"]},
    {"n": "Edebiyat ↔ Akademik Yazım", "e": "✒️",
     "d": "Anlatı yapısı, argüman kurma, üslup kontrolü. Edebiyat analizi akademik makale yazmanın antrenmanıdır.",
     "t": ["Edebiyat", "Akademik İngilizce"]},
    {"n": "Japonca ↔ Programlama", "e": "🈁",
     "d": "kotoba projesi: Unicode, karakter kodlama, veritabanı tasarımı, aralıklı tekrar algoritması.",
     "t": ["Japonca", "Python", "Veritabanı"]},
    {"n": "Olasılık ↔ Kuantum", "e": "🎲",
     "d": "10. sınıf olasılık konusu ve kuantum mekaniğinin olasılıksal yorumu. 4006 projenle doğrudan bağlantılı.",
     "t": ["Olasılık", "Kuantum", "P3"]},
    {"n": "Lineer Cebir ↔ Kuantum", "e": "🧮",
     "d": "Özdeğer/özvektör, Hilbert uzayı, operatörler. Kuantum mekaniğinin dili lineer cebirdir.",
     "t": ["Lineer Cebir", "Kuantum"]},
    {"n": "Fourier ↔ Müzik ↔ Dalga", "e": "🎵",
     "d": "Ses dalgaları, harmonikler, spektrum analizi. Python'la gerçek ses dosyası analizi.",
     "t": ["Dalgalar", "Python", "Fourier"]},
    {"n": "Vektör Analizi ↔ Elektrik Alan", "e": "⚡",
     "d": "Gradyan, diverjans, rotasyonel. Gauss yasası diverjans teoreminin fiziksel hali.",
     "t": ["Çok Değişkenli", "Elektromanyetizma"]},
    {"n": "Diferansiyel Denklem ↔ Salınım", "e": "〰️",
     "d": "Yay-kütle, sarkaç, RC, RLC, radyoaktif bozunma — hepsi aynı denklemin farklı kılıkları.",
     "t": ["DD", "Salınım", "Devreler"]},
    {"n": "Kalkülüs ↔ Fizik", "e": "📐",
     "d": "Sabit ivme denklemleri integralden, hız türevden gelir. Halliday'i türevsiz okumak kitabı yarım okumaktır.",
     "t": ["Kalkülüs", "Kinematik"]},
    {"n": "Fizik ↔ Felsefe: referans çerçevesi", "e": "🎞️",
     "d": "Aynı olay farklı çerçevelerden farklı görünür ama fizik yasaları değişmez. Bakış açısı ile gerçeklik arasındaki fark.",
     "t": ["Göreli Hareket", "Felsefe", "Görelilik"]},
    {"n": "Fizik ↔ Felsefe: bilim etiği", "e": "⚖️",
     "d": "Aristoteles, Kant ve faydacılık üçgeninde araştırmacı sorumluluğu: veri seçimi, atıf, sonuçların kullanımı.",
     "t": ["Etik", "Araştırma", "P3"]},
    {"n": "Termodinamik ↔ Felsefe: zamanın oku", "e": "⏳",
     "d": "Mekanik yasaları zamanda simetrik, entropi değil. Geçmiş ile gelecek arasındaki farkı entropi yaratıyor olabilir mi?",
     "t": ["Entropi", "Felsefe", "Determinizm"]},
    {"n": "Görelilik ↔ Felsefe: zamanın doğası", "e": "🕰️",
     "d": "Eşzamanlılık göreliyse 'şimdi' evrensel bir an değildir. Blok evren tartışması buradan doğar.",
     "t": ["Görelilik", "Felsefe"]},
    {"n": "Diferansiyel Denklem ↔ Devreler", "e": "🔌",
     "d": "RC ve RL devreleri birinci mertebe, RLC ikinci mertebe DD'dir. Matematik dersinde çözdüğün denklem burada devreye gelir.",
     "t": ["DD", "Devreler"]},
    {"n": "Salınım ↔ Devre: aynı diferansiyel denklem", "e": "🔁",
     "d": "LC salınımı yay-kütle sisteminin elektriksel ikizidir: yük↔konum, akım↔hız, indüktans↔kütle.",
     "t": ["Salınım", "Devreler", "Analoji"]},
]

# ══════════════════════════════════════════════════════════════════
# KAYNAKLAR
# ══════════════════════════════════════════════════════════════════

resources = [
    {"s": "math", "n": "Stewart — Calculus: Early Transcendentals", "u": "Ana kitap, Faz 1–2"},
    {"s": "math", "n": "MIT OCW 18.01 / 18.02", "u": "Video ders"},
    {"s": "math", "n": "3Blue1Brown — Essence of Calculus / Linear Algebra", "u": "Sezgi, önce izle"},
    {"s": "math", "n": "Strang — Introduction to Linear Algebra", "u": "Faz 2"},
    {"s": "math", "n": "Spivak — Calculus", "u": "İspat isteyen bölümler"},
    {"s": "math", "n": "Boyce & DiPrima — Elementary Differential Equations", "u": "Faz 3"},
    {"s": "phys", "n": "Halliday, Resnick & Walker — Fundamentals of Physics", "u": "ANA KAYNAK, İngilizce"},
    {"s": "phys", "n": "MIT 8.01 / 8.02 — Walter Lewin", "u": "Video ders"},
    {"s": "phys", "n": "Irodov — Problems in General Physics", "u": "Olimpiyat problemleri"},
    {"s": "phys", "n": "Morin — Introduction to Classical Mechanics", "u": "Bl.10–11 için ek"},
    {"s": "phys", "n": "Purcell — Electricity and Magnetism", "u": "Faz 3 derinleşme"},
    {"s": "phys", "n": "Feynman Lectures Vol I–II", "u": "Kavramsal derinlik"},
    {"s": "py", "n": "Automate the Boring Stuff", "u": "Faz 1 temel"},
    {"s": "py", "n": "Python Crash Course — Matthes", "u": "Faz 1 OOP"},
    {"s": "py", "n": "Computational Physics — Mark Newman", "u": "Sayısal fizik"},
    {"s": "py", "n": "Python for Data Analysis — McKinney", "u": "Pandas"},
    {"s": "py", "n": "SciPy Lecture Notes", "u": "Referans"},
    {"s": "jp", "n": "Genki I + II", "u": "Ana kitap"},
    {"s": "jp", "n": "WaniKani / Anki Core 2k", "u": "Kanji ve kelime"},
    {"s": "jp", "n": "Tae Kim's Guide", "u": "Gramer referansı"},
    {"s": "jp", "n": "NHK Easy News", "u": "Okuma pratiği"},
    {"s": "de", "n": "Menschen A1/A2", "u": "Ana kitap"},
    {"s": "de", "n": "Nicos Weg (DW)", "u": "Ücretsiz video kurs"},
    {"s": "lit", "n": "MEB ders kitapları", "u": "10. sınıf"},
    {"s": "phil", "n": "Nigel Warburton — Felsefeye Giriş", "u": "10. sınıf felsefe"},
]

subjects = [
    {"c": "math", "n": "Matematik", "e": "📐", "col": "#7BB661"},
    {"c": "phys", "n": "Fizik", "e": "🍎", "col": "#C8402F"},
    {"c": "py", "n": "Python", "e": "🐍", "col": "#3F7A57"},
    {"c": "jp", "n": "Japonca", "e": "🌸", "col": "#E58FA6"},
    {"c": "de", "n": "Almanca", "e": "🥨", "col": "#B98A3E"},
    {"c": "lit", "n": "Edebiyat", "e": "📖", "col": "#9A6B4F"},
    {"c": "phil", "n": "Felsefe", "e": "🦉", "col": "#6E6BA8"},
    {"c": "chem", "n": "Kimya", "e": "⚗️", "col": "#4E9E9E"},
    {"c": "bio", "n": "Biyoloji", "e": "🌿", "col": "#5E9C4F"},
    {"c": "hist", "n": "Tarih", "e": "🏛️", "col": "#A8763E"},
    {"c": "geo", "n": "Coğrafya", "e": "🗺️", "col": "#4F86A0"},
    {"c": "eng", "n": "İngilizce", "e": "🫐", "col": "#5B7BB8"},
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
    "version": 1,
    "title": "Beril · 4 Aylık Müfredat",
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
