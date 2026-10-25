package io.fluxreader.app.reader

import java.io.BufferedInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import org.xml.sax.InputSource

/** Android's Expat accepts fewer byte encodings than Charset; decode legacy FB2 explicitly. */
internal object XmlBookInput {
    private val declaration = Regex("""^\s*<\?xml\s[^?]*encoding\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)

    fun source(input: InputStream): InputSource {
        val buffered = BufferedInputStream(input)
        buffered.mark(1024)
        val head = ByteArray(1024)
        var size = 0
        while (size < head.size) {
            val read = buffered.read(head, size, head.size - size)
            if (read <= 0) break
            size += read
        }
        buffered.reset()
        val declared = declaration.find(String(head, 0, size, Charsets.ISO_8859_1))?.groupValues?.get(1)
        val charset = declared?.let { Charset.forName(it) }
        // Keep BOM/XML auto-detection for native Unicode encodings. All other encodings are
        // handed to SAX as characters so Android does not reinterpret CP1251 bytes as UTF-8.
        return if (charset != null && !charset.name().startsWith("UTF-", ignoreCase = true)) {
            InputSource(InputStreamReader(buffered, charset))
        } else InputSource(buffered)
    }
}
