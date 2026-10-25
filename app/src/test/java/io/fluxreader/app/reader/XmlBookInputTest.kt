package io.fluxreader.app.reader

import java.io.ByteArrayInputStream
import java.nio.charset.Charset
import javax.xml.parsers.SAXParserFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.xml.sax.helpers.DefaultHandler

class XmlBookInputTest {
    @Test fun declaredWindows1251IsDecodedBeforeAndroidSax() {
        val xml = """<?xml version="1.0" encoding="windows-1251"?><p>— Вернусь, — ответил Пётр.</p>"""
        val source = XmlBookInput.source(ByteArrayInputStream(xml.toByteArray(Charset.forName("windows-1251"))))
        assertNotNull(source.characterStream)
        val text = StringBuilder()
        SAXParserFactory.newInstance().newSAXParser().parse(source, object : DefaultHandler() {
            override fun characters(ch: CharArray, start: Int, length: Int) { text.append(ch, start, length) }
        })
        assertEquals("— Вернусь, — ответил Пётр.", text.toString())
    }

    @Test fun unicodeBomAndUtf8RemainOnNativeInputPath() {
        for (encoding in listOf(Charsets.UTF_8, Charsets.UTF_16)) {
            val xml = """<?xml version="1.0" encoding="${encoding.name()}"?><p>Анна</p>"""
            val source = XmlBookInput.source(ByteArrayInputStream(xml.toByteArray(encoding)))
            assertNotNull(source.byteStream)
            val text = StringBuilder()
            SAXParserFactory.newInstance().newSAXParser().parse(source, object : DefaultHandler() {
                override fun characters(ch: CharArray, start: Int, length: Int) { text.append(ch, start, length) }
            })
            assertEquals("Анна", text.toString())
        }
    }
}
