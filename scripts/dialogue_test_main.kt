import io.fluxreader.app.reader.SpeedDialogueAnalyzer as A
import io.fluxreader.app.reader.SpeedReaderCursor

private fun nameAt(text: String, token: String, expected: String?) {
    val idx = text.indexOf(token)
    require(idx >= 0) { "Token not found: $token" }
    val speaker = A.at(text, idx)?.speaker
    check(speaker == expected) { "Expected '$expected', got '$speaker' (offset=$idx)" }
}
private fun assertProse(text: String, token: String) {
    val idx = text.indexOf(token)
    check(idx >= 0)
    check(A.at(text, idx) == null) { "Prose misidentified: $token" }
}

fun main() {
    val two = "— Привет! — сказала Анна.\n— Здравствуй! — ответил Павел."
    nameAt(two, "— Привет", "Анна")
    nameAt(two, "— Здравствуй", "Павел")
    assertProse(two, "сказала")
    assertProse(two, "ответил")
    val q = "Анна сказала: «Пойдём домой!» Он согласился."
    nameAt(q, "«Пойдём", "Анна")
    assertProse(q, "Он согласился")
    assertProse("Он читал «Войну и мир» на диване.", "«Войну")
    nameAt("— Нет!\nДождь стих.", "— Нет", null)
    assertProse("— Нет!\nДождь стих.", "Дождь")
    check(A.isReplyStart(two, 0))
    check(A.isReplyStart(two, two.indexOf("— Здравствуй")))
    check(!A.isReplyStart(two, two.indexOf("ответил")))
    check(A.shortReplyWordCount("— Нет. — ответил Павел.", 0) == 1)
    check(A.shortReplyWordCount("— Ну как дела? — спросил Павел.", 0) == 3)
    check(A.shortReplyWordCount("— Сегодня мы совершенно не планируем идти за грибами!", 0) == null)
    check(A.shortReplyWordCount("— Текст без знака", 0) == null)
    check(A.shortReplyWordCount("— Привет! — ответил Павел.", 4) == null)
    check(A.at("", 0) == null)
    check(A.at("— Привет!", -1) == null)
    check(A.at("— Привет!", 100) == null)
    val sample = "— Да! — сказала Анна.\n— А ты уверен, что всё получится? — спросил Павел.\nПосле разговора они вышли."
    val chunks = mutableListOf<String>()
    val starts = mutableListOf<Int>()
    var offset = 0
    val adjusted = sample.indexOfFirst { !it.isWhitespace() }
    while (true) {
        var local = offset
        while (local < sample.length && sample[local].isWhitespace()) local++
        val short = if (local < sample.length) A.shortReplyWordCount(sample, local) else null
        val next = SpeedReaderCursor.next(sample, offset, short ?: 1, smartChunking = short != null) ?: break
        check(next.endOffset > offset)
        starts += next.startOffset
        chunks.addAll(next.tokens)
        offset = next.endOffset
        check(chunks.size < 200) { "Cursor stopped progressing" }
    }
    check(chunks == Regex("\\S+").findAll(sample).map { it.value }.toList()) { "Cursor missed or duplicated words" }
    check(starts == starts.sorted())
    check(starts.first() == adjusted)
    println("PASS: dialogue/prose/offset and RSVP traversal tests")
}
