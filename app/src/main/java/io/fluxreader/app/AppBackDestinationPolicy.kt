package io.fluxreader.app

internal object AppBackDestinationPolicy {
    fun destination(
        current: AppRoute,
        routeBeforeSpeed: AppRoute,
        routeBeforeSettings: AppRoute,
    ): AppRoute = when (current) {
        AppRoute.LIBRARY -> AppRoute.LIBRARY
        AppRoute.READER, AppRoute.PDF, AppRoute.AUDIOBOOKS -> AppRoute.LIBRARY
        AppRoute.SPEED -> if (routeBeforeSpeed == AppRoute.SPEED) AppRoute.LIBRARY else routeBeforeSpeed
        AppRoute.SETTINGS -> if (routeBeforeSettings == AppRoute.SETTINGS) AppRoute.LIBRARY else routeBeforeSettings
    }
}
