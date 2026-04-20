package com.base.reportservice.service

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import org.jsoup.Jsoup
import org.jsoup.helper.W3CDom
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.io.File

@Service
class PdfGenerationService {
    private val log = LoggerFactory.getLogger(javaClass)

    private val cyrillicFontCandidates = listOf(
        // Linux — Debian/Ubuntu
        "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
        "/usr/share/fonts/dejavu-sans/DejaVuSans.ttf",
        // Linux — RHEL/CentOS/Fedora
        "/usr/share/fonts/dejavu-sans-fonts/DejaVuSans.ttf",
        // Linux — Liberation (тоже Cyrillic)
        "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
        "/usr/share/fonts/liberation-sans/LiberationSans-Regular.ttf",
        // Windows
        "C:/Windows/Fonts/arial.ttf",
        "C:/Windows/Fonts/calibri.ttf",
        "C:/Windows/Fonts/times.ttf",
    )

    fun generatePdf(html: String): ByteArray {
        val xhtml = toXhtml(html)
        val out = ByteArrayOutputStream()
        try {
            val builder = PdfRendererBuilder()
            registerCyrillicFont(builder)
            builder
                .withW3cDocument(xhtml, null)
                .toStream(out)
                .run()
        } catch (ex: Exception) {
            log.error("PDF generation failed", ex)
            throw ex
        }
        return out.toByteArray()
    }

    private fun registerCyrillicFont(builder: PdfRendererBuilder) {
        for (path in cyrillicFontCandidates) {
            val file = File(path)
            if (file.exists()) {
                builder.useFont(file, FONT_FAMILY)
                log.debug("PDF Cyrillic font registered from: {}", path)
                return
            }
        }
        log.warn(
            "No Cyrillic font found. PDF text may render as '#'. " +
                "On Linux install: apt-get install -y fonts-dejavu-core"
        )
    }

    private fun toXhtml(html: String): org.w3c.dom.Document {
        val doc = Jsoup.parse(html)
        doc.outputSettings()
            .syntax(org.jsoup.nodes.Document.OutputSettings.Syntax.xml)
            .charset(Charsets.UTF_8)

        doc.head().append(
            """<style>
               @page { size: A4; margin: 15mm 12mm; }
               * { font-family: '$FONT_FAMILY', Arial, sans-serif !important; }
               body { -webkit-print-color-adjust: exact; print-color-adjust: exact; }
               table { page-break-inside: avoid; }
            </style>"""
        )

        return W3CDom().fromJsoup(doc)
    }

    companion object {
        private const val FONT_FAMILY = "CyrillicFont"
    }
}
