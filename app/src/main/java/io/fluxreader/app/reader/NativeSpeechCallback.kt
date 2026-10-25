package io.fluxreader.app.reader

import androidx.annotation.Keep
import kotlinx.coroutines.isActive
import kotlin.coroutines.CoroutineContext

/** sherpa JNI looks up invoke([F)Ljava/lang/Integer; directly. An indy lambda
 * only has invoke(Object), so use a real typed class and retain its JNI method. */
@Keep
internal class NativeSpeechCallback(private val owner: CoroutineContext) : (FloatArray) -> Int {
    override fun invoke(samples: FloatArray): Int = if (owner.isActive) 1 else 0
}
