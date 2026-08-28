package com.beril.kaomoji.ai

/** Groq/Gemini üzerinden özel müfredat üretimi için ortak sistem talimatı ve yardımcılar. */
object CurriculumPrompt {

    const val SYSTEM = """Sen bir müfredat/program mimarısın. Kullanıcı sana bir belge (ders programı, çalışma planı, kişisel gelişim programı, kitap içindekiler tablosu vb.) verecek. Görevin bu belgeyi SADECE aşağıdaki JSON şemasına birebir uyan, doğrudan bir uygulamada kullanılabilir bir müfredat JSON'una dönüştürmek.

ŞEMA (alan adları TAM OLARAK bu şekilde, kısaltılmış anahtarlar zorunlu):
{
  "version": 1,
  "title": "kısa program başlığı",
  "phases": [
    {
      "id": "p1", "name": "faz adı", "sub": "alt başlık/süre", "goal": "bu fazın amacı", "hours": 100,
      "units": [
        {
          "id": "u01", "title": "birim başlığı", "kicker": "kısa alt başlık",
          "note": "opsiyonel not",
          "fey": "opsiyonel — kitaba bakmadan anlatıp kaydetme (Feynman) istemi",
          "br": ["opsiyonel köprü adları"],
          "tasks": [
            {"i":"u01-t01","t":"görev metni (net, uygulanabilir)","s":"ders_kodu","k":"kind_kodu","m":45}
          ]
        }
      ]
    }
  ],
  "projects": [{"id":"P1","name":"proje adı","emoji":"🌱","goal":"...","phases":{"p1":"bu fazda proje ne durumda"},"topics":["..."],"next":"sıradaki somut eylem","ms":["kilometre taşı", "..."]}],
  "assessments": [{"id":"a1","name":"...","scope":"kapsam","hours":2,"phase":"p1","unit":"u05"}],
  "bridges": [{"n":"köprü adı","e":"🔗","d":"iki alanı birbirine bağlayan açıklama","t":["konu1","konu2"]}],
  "resources": [{"s":"ders_kodu","n":"kaynak adı","u":"kullanım notu"}],
  "subjects": [{"c":"ders_kodu","n":"Ders Adı","e":"emoji","col":"#RRGGBB"}],
  "kinds": [{"c":"kind_kodu","n":"Görev Türü Adı","e":"emoji"}]
}

KURALLAR:
- SADECE geçerli JSON döndür. Markdown code fence (```), açıklama, giriş/kapanış cümlesi YOK — cevabın ilk karakteri "{" olsun.
- "subjects" ve "kinds" dizileri boş OLAMAZ; her task'ın "s" alanı subjects'teki bir "c" ile, "k" alanı kinds'teki bir "c" ile eşleşmeli.
- Her unit en az 1 task içermeli, her phase en az 1 unit içermeli, en az 1 phase olmalı.
- Tüm id'ler (phase/unit/task/project/assessment) kendi türü içinde benzersiz olmalı.
- Belgede haftalık/günlük yapı varsa bunu units olarak birebir yansıt (örn. 1 unit = 1 hafta).
- Belgede sabit tarih/saat yoksa (esnek/sıra bazlı bir programsa) bunu ilgili "note" alanlarında belirt — ama şemaya saat/tarih alanı EKLEME, şema sabit.
- Program çok uzunsa (aylarca/yıllarca sürüyorsa), makul ve gerçekten kullanılabilir bir ilk dilim üret: toplam en fazla ~14 unit, unit başına en fazla ~10 task. Kapsamı daraltman gerekirse belgenin en kritik/ilk bölümünü önceliklendir, bunu "note" alanında belirt.
- "projects", "assessments", "bridges", "resources" belgede karşılığı yoksa boş dizi [] olarak bırakılabilir — ama "phases", "subjects", "kinds" hiçbir zaman boş olamaz.
- Türkçe yaz (belge başka dildeyse görev metinlerini Türkçeye çevirerek yaz), belgenin kendi terminolojisini/isimlerini koru."""

    fun userMessage(docTitle: String, docText: String): String {
        val cap = 20000
        val trimmed = if (docText.length > cap) docText.take(cap) + "\n\n[...belge bu noktada kırpıldı, kalan kısmı görmedin...]" else docText
        return "Belge başlığı: $docTitle\n\nBelge içeriği:\n$trimmed\n\nBu belgeye göre yukarıdaki şemaya uygun müfredat JSON'unu üret."
    }

    /** Model bazen ```json ... ``` ile sarmalıyor ya da öncesine/sonrasına metin ekliyor — temizle. */
    fun extractJson(raw: String): String {
        val cleaned = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val start = cleaned.indexOf('{')
        val end = cleaned.lastIndexOf('}')
        if (start == -1 || end == -1 || end < start) return cleaned
        return cleaned.substring(start, end + 1)
    }
}
