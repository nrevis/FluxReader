package io.fluxreader.app.reader

import io.fluxreader.app.data.BookOutline
import io.fluxreader.app.data.ChapterContent
import io.fluxreader.app.data.ChapterRef
import io.fluxreader.app.data.ReaderBlock
import io.fluxreader.app.data.TextRole
import java.io.File
import java.nio.charset.Charset

object TxtParser {
    fun outline(file: File, title: String = file.nameWithoutExtension): BookOutline = BookOutline(
        title = title,
        author = "",
        chapters = listOf(ChapterRef(0, "Текст", file.absolutePath)),
    )

    fun loadChapter(file: File, title: String = file.nameWithoutExtension): ChapterContent {
        val text = decode(file.readBytes())
        val blocks = text
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .split(Regex("\\n\\s*\\n+"))
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { paragraph -> ReaderBlock.Text(paragraph, TextRole.PARAGRAPH) }
            .toList()
        return ChapterContent(0, title, blocks)
    }

    private fun decode(bytes: ByteArray): String {
        val utf = bytes.toString(Charsets.UTF_8)
        val utfBad = utf.count { it == '\uFFFD' }
        if (utfBad == 0) return utf
        val cp = bytes.toString(Charset.forName("windows-1251"))
        return if (cp.count { it == '\uFFFD' } < utfBad) cp else utf
    }
}
