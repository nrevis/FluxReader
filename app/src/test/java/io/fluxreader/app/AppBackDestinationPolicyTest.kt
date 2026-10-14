package io.fluxreader.app

import org.junit.Assert.assertEquals
import org.junit.Test

class AppBackDestinationPolicyTest {
    @Test fun readerAndPdfReturnToLibrary() {
        assertEquals(AppRoute.LIBRARY, AppBackDestinationPolicy.destination(AppRoute.READER, AppRoute.LIBRARY, AppRoute.LIBRARY))
        assertEquals(AppRoute.LIBRARY, AppBackDestinationPolicy.destination(AppRoute.PDF, AppRoute.LIBRARY, AppRoute.LIBRARY))
    }

    @Test fun audiobooksReturnToLibrary() {
        assertEquals(AppRoute.LIBRARY, AppBackDestinationPolicy.destination(AppRoute.AUDIOBOOKS, AppRoute.LIBRARY, AppRoute.LIBRARY))
    }

    @Test fun speedReturnsToItsSourceRoute() {
        assertEquals(AppRoute.READER, AppBackDestinationPolicy.destination(AppRoute.SPEED, AppRoute.READER, AppRoute.LIBRARY))
        assertEquals(AppRoute.PDF, AppBackDestinationPolicy.destination(AppRoute.SPEED, AppRoute.PDF, AppRoute.LIBRARY))
        assertEquals(AppRoute.LIBRARY, AppBackDestinationPolicy.destination(AppRoute.SPEED, AppRoute.LIBRARY, AppRoute.LIBRARY))
    }

    @Test fun settingsReturnsToPreviousRouteAndGuardsSelfReference() {
        assertEquals(AppRoute.READER, AppBackDestinationPolicy.destination(AppRoute.SETTINGS, AppRoute.LIBRARY, AppRoute.READER))
        assertEquals(AppRoute.LIBRARY, AppBackDestinationPolicy.destination(AppRoute.SETTINGS, AppRoute.LIBRARY, AppRoute.SETTINGS))
        assertEquals(AppRoute.LIBRARY, AppBackDestinationPolicy.destination(AppRoute.SPEED, AppRoute.SPEED, AppRoute.LIBRARY))
    }

    @Test fun readerSpeedReaderLibrarySequenceIsStable() {
        val backFromSpeed = AppBackDestinationPolicy.destination(
            current = AppRoute.SPEED,
            routeBeforeSpeed = AppRoute.READER,
            routeBeforeSettings = AppRoute.LIBRARY,
        )
        assertEquals(AppRoute.READER, backFromSpeed)
        assertEquals(
            AppRoute.LIBRARY,
            AppBackDestinationPolicy.destination(
                current = backFromSpeed,
                routeBeforeSpeed = AppRoute.READER,
                routeBeforeSettings = AppRoute.LIBRARY,
            ),
        )
    }
}
