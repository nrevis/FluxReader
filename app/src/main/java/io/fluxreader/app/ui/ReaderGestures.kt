package io.fluxreader.app.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs
import kotlin.math.max

/**
 * Observes reader gestures without consuming vertical scroll events.
 * Vertical scrolling stays owned by LazyColumn/ScrollState; this layer only reacts to taps
 * and clearly-horizontal swipes after the gesture finishes.
 */
@Composable
fun Modifier.readerGestures(
    edgeFraction: Float,
    edgeTapsEnabled: Boolean,
    centerTapEnabled: Boolean = true,
    swipeEnabled: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onCenterTap: () -> Unit,
): Modifier {
    val previous by rememberUpdatedState(onPrevious)
    val next by rememberUpdatedState(onNext)
    val center by rememberUpdatedState(onCenterTap)

    return pointerInput(edgeFraction, edgeTapsEnabled, centerTapEnabled, swipeEnabled) {
        val safeEdgeFraction = edgeFraction.coerceIn(0.10f, 0.40f)
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
            val start = down.position
            val pointerId = down.id
            var end = start
            var maxDistance = 0f
            var consumedByChild = down.isConsumed

            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Final)
                val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                end = change.position
                consumedByChild = consumedByChild || change.isConsumed
                maxDistance = max(maxDistance, (end - start).getDistance())
                if (!change.pressed) break
            }

            val dx = end.x - start.x
            val dy = end.y - start.y
            val horizontalSwipe = swipeEnabled &&
                abs(dx) >= viewConfiguration.touchSlop * 2.6f &&
                abs(dx) > abs(dy) * 1.25f

            when {
                horizontalSwipe -> if (dx < 0f) next() else previous()
                !consumedByChild && maxDistance <= viewConfiguration.touchSlop * 1.35f -> {
                    val width = size.width.toFloat().coerceAtLeast(1f)
                    val edge = width * safeEdgeFraction
                    when {
                        edgeTapsEnabled && start.x <= edge -> previous()
                        edgeTapsEnabled && start.x >= width - edge -> next()
                        centerTapEnabled -> center()
                    }
                }
            }
        }
    }
}


/**
 * Observes only a real long press and consumes that gesture once the timeout has elapsed.
 * Short taps stay unconsumed so the parent readerGestures layer keeps edge/center navigation.
 */
@Composable
fun Modifier.readerTextLongPress(
    enabled: Boolean = true,
    onLongPress: (Offset) -> Unit,
): Modifier {
    val longPressAction by rememberUpdatedState(onLongPress)
    return pointerInput(enabled) {
        if (!enabled) return@pointerInput
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Main)
            val longPress = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
            longPress.consume()
            longPressAction(longPress.position)
            do {
                val event = awaitPointerEvent(PointerEventPass.Main)
                event.changes.forEach { it.consume() }
            } while (event.changes.any { it.pressed })
        }
    }
}

/**
 * Handles only child-owned taps (for example EPUB links). Returning false leaves the tap
 * untouched so the parent reader layer can still perform edge/center navigation.
 */
@Composable
fun Modifier.readerTextTapAction(
    enabled: Boolean = true,
    onTap: (Offset) -> Boolean,
): Modifier {
    val tapAction by rememberUpdatedState(onTap)
    return pointerInput(enabled) {
        if (!enabled) return@pointerInput
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Main)
            val pointerId = down.id
            val start = down.position
            var maxDistance = 0f
            var upChange: androidx.compose.ui.input.pointer.PointerInputChange? = null
            var cancelled = down.isConsumed
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                val change = event.changes.firstOrNull { it.id == pointerId }
                if (change == null) {
                    cancelled = true
                    break
                }
                maxDistance = max(maxDistance, (change.position - start).getDistance())
                if (change.isConsumed) cancelled = true
                if (!change.pressed) {
                    upChange = change
                    break
                }
            }
            if (!cancelled && maxDistance <= viewConfiguration.touchSlop * 1.35f && tapAction(start)) {
                upChange?.consume()
            }
        }
    }
}

/**
 * Starts a text selection after a real long press, then keeps consuming the pointer while it is
 * dragged so parent paging/scroll gestures cannot fire. Coordinates are local to the text block.
 */
@Composable
fun Modifier.readerTextRangeSelection(
    enabled: Boolean = true,
    onStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onEnd: (Offset) -> Unit,
): Modifier {
    val startAction by rememberUpdatedState(onStart)
    val dragAction by rememberUpdatedState(onDrag)
    val endAction by rememberUpdatedState(onEnd)
    return pointerInput(enabled) {
        if (!enabled) return@pointerInput
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Main)
            val longPress = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
            longPress.consume()
            var last = longPress.position
            startAction(last)
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                last = change.position
                change.consume()
                if (!change.pressed) {
                    endAction(last)
                    break
                }
                dragAction(last)
            }
        }
    }
}
