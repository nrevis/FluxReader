package io.fluxreader.app.reader

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest

class VoicePackageInstallerTest {
    private fun archive(entries: List<Pair<TarArchiveEntry, ByteArray>>): ByteArray {
        val bytes = ByteArrayOutputStream()
        TarArchiveOutputStream(BZip2CompressorOutputStream(bytes)).use { tar ->
            entries.forEach { (entry, data) ->
                entry.size = data.size.toLong(); tar.putArchiveEntry(entry); tar.write(data); tar.closeArchiveEntry()
            }
        }
        return bytes.toByteArray()
    }
    private fun file(name: String, text: String) = TarArchiveEntry(name) to text.toByteArray()
    private fun reject(block: () -> Unit) {
        try { block(); fail("Unsafe package accepted") } catch (_: IllegalArgumentException) { }
    }
    @Test fun completePackageInstallsOnlyAllowlistedFiles() {
        val root = Files.createTempDirectory("voice-test").toFile()
        try {
            val destination = File(root, "model")
            val bytes = archive(listOf(file("pack/model.onnx", "model"), file("pack/README.md", "notice")))
            VoicePackageInstaller.extract(ByteArrayInputStream(bytes), destination, "pack", mapOf("model.onnx" to 5L))
            assertEquals("model", File(destination, "model.onnx").readText())
            assertFalse(File(destination, "README.md").exists())
        } finally { root.deleteRecursively() }
    }
    @Test fun traversalAndSymlinkAreRejectedAndStagingRemoved() {
        val root = Files.createTempDirectory("voice-test").toFile()
        try {
            val link = TarArchiveEntry("pack/model.onnx", '2'.code.toByte()).apply { linkName = "../../outside" }
            for (entries in listOf(listOf(file("pack/../../outside", "bad")), listOf(link to byteArrayOf()))) {
                val destination = File(root, "model")
                reject { VoicePackageInstaller.extract(ByteArrayInputStream(archive(entries)), destination, "pack", mapOf("model.onnx" to 5L)) }
                assertFalse(destination.exists())
                assertFalse(File(root.parentFile, "outside").exists())
            }
        } finally { root.deleteRecursively() }
    }
    @Test fun duplicatesWrongSizesAndMissingModelAreRejected() {
        val root = Files.createTempDirectory("voice-test").toFile()
        try {
            for (entries in listOf(listOf(file("pack/model.onnx", "wrong-size")),
                listOf(file("pack/README.md", "none")),
                listOf(file("pack/model.onnx", "model"), file("pack/model.onnx", "model")))) {
                val destination = File(root, "model")
                reject { VoicePackageInstaller.extract(ByteArrayInputStream(archive(entries)), destination, "pack", mapOf("model.onnx" to 5L)) }
                assertFalse(destination.exists())
            }
        } finally { root.deleteRecursively() }
    }
    @Test fun partialAndTamperedDownloadsFailChecksumBeforeInstallation() {
        val file = Files.createTempFile("voice-test", ".part").toFile()
        try {
            file.writeText("voice")
            val hash = MessageDigest.getInstance("SHA-256").digest("voice".toByteArray()).joinToString("") { "%02x".format(it) }
            VoicePackageInstaller.verify(file, 5, hash)
            reject { VoicePackageInstaller.verify(file, 6, hash) }
            file.writeText("other")
            reject { VoicePackageInstaller.verify(file, 5, hash) }
        } finally { file.delete() }
    }
    @Test fun interruptedExtractionRemovesPartialModel() {
        val root = Files.createTempDirectory("voice-test").toFile()
        try {
            var calls = 0
            val destination = File(root, "model")
            try {
                VoicePackageInstaller.extract(ByteArrayInputStream(archive(listOf(file("pack/model.onnx", "model")))),
                    destination, "pack", mapOf("model.onnx" to 5L)) { if (++calls == 3) throw java.util.concurrent.CancellationException() }
                fail("Cancellation ignored")
            } catch (_: java.util.concurrent.CancellationException) { }
            assertFalse(destination.exists())
        } finally { root.deleteRecursively() }
    }
    @Test fun voiceIdsHaveNoFallbackToUnrelatedSpeaker() {
        assertEquals(10, BuiltInVoiceCatalog.voices.map { it.id }.distinct().size)
        assertNull(BuiltInVoiceCatalog.voice("builtin:supertonic3:999"))
        assertNull(BuiltInVoiceCatalog.voice("android-voice"))
        assertTrue(BuiltInVoiceCatalog.isBuiltIn(BuiltInVoiceCatalog.DEFAULT_VOICE))
    }
}
