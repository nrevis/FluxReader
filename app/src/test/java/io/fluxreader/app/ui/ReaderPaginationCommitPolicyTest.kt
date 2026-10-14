package io.fluxreader.app.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderPaginationCommitPolicyTest {
    @Test
    fun acceptsOnlyCurrentLayoutKey() {
        assertTrue(ReaderPaginationCommitPolicy.shouldCommit("book:1:1080x2200", "book:1:1080x2200"))
        assertFalse(ReaderPaginationCommitPolicy.shouldCommit("book:1:1080x2200", "book:1:2200x1080"))
        assertFalse(ReaderPaginationCommitPolicy.shouldCommit("book:1:normal", "book:1:fullscreen"))
    }
}
