package com.beril.kaomoji.storage

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

class DocumentExtractException(message: String) : Exception(message)

/** Kullanıcının seçtiği bir .md/.txt/.pdf dosyasından düz metin çıkarır — özel
 *  müfredat üretimi için (bkz. ui/CurriculumGenScreen.kt, ai/AiClient.generateCurriculum). */
object DocumentTextExtractor {
    @Volatile private var pdfBoxReady = false

    fun displayName(ctx: Context, uri: Uri): String {
        var name: String? = null
        try {
            ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) name = c.getString(idx)
            }
        } catch (_: Exception) {
        }
        return name ?: uri.lastPathSegment ?: "belge"
    }

    fun extract(ctx: Context, uri: Uri): String {
        val name = displayName(ctx, uri).lowercase()
        return if (name.endsWith(".pdf")) extractPdf(ctx, uri) else extractText(ctx, uri)
    }

    private fun extractText(ctx: Context, uri: Uri): String {
        val text = try {
            ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        } catch (e: Exception) {
            throw DocumentExtractException("Dosya okunamadı: ${e.message}")
        } ?: throw DocumentExtractException("Dosya okunamadı.")
        if (text.isBlank()) throw DocumentExtractException("Dosya boş görünüyor.")
        return text
    }

    private fun extractPdf(ctx: Context, uri: Uri): String {
        if (!pdfBoxReady) {
            synchronized(this) {
                if (!pdfBoxReady) {
                    PDFBoxResourceLoader.init(ctx.applicationContext)
                    pdfBoxReady = true
                }
            }
        }
        val input = ctx.contentResolver.openInputStream(uri)
            ?: throw DocumentExtractException("PDF dosyası açılamadı.")
        return try {
            input.use { ins ->
                PDDocument.load(ins).use { doc ->
                    if (doc.isEncrypted) throw DocumentExtractException("Şifreli PDF'ler desteklenmiyor.")
                    val text = PDFTextStripper().getText(doc)
                    if (text.isBlank()) {
                        throw DocumentExtractException(
                            "PDF'den metin çıkarılamadı — taranmış/görsel bir PDF olabilir (OCR gerekir)."
                        )
                    }
                    text
                }
            }
        } catch (e: DocumentExtractException) {
            throw e
        } catch (e: Exception) {
            throw DocumentExtractException("PDF okunamadı: ${e.message}")
        }
    }
}
