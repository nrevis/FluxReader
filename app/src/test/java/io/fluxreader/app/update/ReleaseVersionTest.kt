package io.fluxreader.app.update

import org.junit.Assert.*
import org.junit.Test

class ReleaseVersionTest {
    @Test fun numericAlphaOrdering() {
        assertTrue(ReleaseVersion.parse("v0.2.0-alpha.100")!! > ReleaseVersion.parse("0.2.0-alpha.93")!!)
        assertTrue(ReleaseVersion.parse("v0.2.0-alpha.94")!! > ReleaseVersion.parse("0.2.0-alpha.93")!!)
        assertFalse(UpdateVersionPolicy.newerThanInstalled("v0.2.0-alpha.92", "0.2.0-alpha.93", true))
    }
    @Test fun stableBeatsAlpha() {
        assertTrue(UpdateVersionPolicy.newerThanInstalled("v0.2.0", "0.2.0-alpha.93", false))
        assertTrue(UpdateVersionPolicy.newerThanInstalled("v0.3.0-alpha.1", "0.2.0", true))
        assertFalse(UpdateVersionPolicy.newerThanInstalled("v0.3.0-alpha.1", "0.2.0", false))
    }
    @Test fun malformedTagsCannotTriggerUpdates() {
        assertFalse(UpdateVersionPolicy.newerThanInstalled("nightly", "0.2.0-alpha.93", true))
        assertFalse(UpdateVersionPolicy.newerThanInstalled("v0.2.0-alpha.93-unsigned", "0.2.0-alpha.92", true))
        assertNull(ReleaseVersion.parse("0.2.0-alpha.-1"))
    }
}
