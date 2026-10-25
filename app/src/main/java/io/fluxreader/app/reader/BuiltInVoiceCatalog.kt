package io.fluxreader.app.reader

/** IDs are persisted with the existing voice preference, shared by both readers. */
object BuiltInVoiceCatalog {
    const val PREFIX = "builtin:supertonic3:"
    const val DEFAULT_VOICE = PREFIX + "0"
    const val PACK_ID = "supertonic3-int8-20260511"
    const val ARCHIVE_ROOT = "sherpa-onnx-supertonic-3-tts-int8-2026-05-11"
    const val URL = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/$ARCHIVE_ROOT.tar.bz2"
    const val DOWNLOAD_BYTES = 128774318L
    const val SHA256 = "82fa96f91c4ef8abaae3a14a3f4153facf88bed821d1f7331cec2700f432c427"
    const val LICENSE_ASSET = "licenses/supertonic3-OpenRAIL-M.txt"
    const val PREVIEW = "Ветер стихал. Наступал вечер, и на горизонте зажигались первые огни."
    data class Voice(val id: String, val label: String, val speaker: Int)
    // Conversion concatenates preset styles in sorted filename order: F1..F5, M1..M5.
    val voices = (0..9).map { sid -> Voice(PREFIX + sid,
        if (sid < 5) "Женский ${sid + 1}" else "Мужской ${sid - 4}", sid) }
    fun isBuiltIn(id: String) = id.startsWith("builtin:")
    fun voice(id: String) = voices.firstOrNull { it.id == id }
    val files = mapOf(
        "text_encoder.int8.onnx" to 36416150L,
        "duration_predictor.int8.onnx" to 3700147L,
        "vector_estimator.int8.onnx" to 78400833L,
        "vocoder.int8.onnx" to 25991073L,
        "unicode_indexer.bin" to 262144L,
        "voice.bin" to 517168L,
        "tts.json" to 8253L,
        "LICENSE" to 1070L,
    )
}
