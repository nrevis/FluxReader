package io.fluxreader.app.data

/** A new key prevents interpreting the old percentage as milliseconds on later launches. */
internal fun migratedComplexWordPauseMs(legacyPercent: Int?): Int =
    legacyPercent?.coerceIn(0, 60)?.times(5) ?: 150
