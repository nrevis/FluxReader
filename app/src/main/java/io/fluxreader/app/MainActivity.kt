package io.fluxreader.app

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import io.fluxreader.app.ui.FluxReaderApp

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { FluxReaderApp(viewModel) }
    }


    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        viewModel.handleTrimMemory(level)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val isVolumeKey = event.keyCode == KeyEvent.KEYCODE_VOLUME_UP || event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        if (isVolumeKey && viewModel.route.value == AppRoute.SPEED && viewModel.settings.value.speedVolumeKeysEnabled) {
            if (event.action == KeyEvent.ACTION_UP) {
                val held = event.eventTime - event.downTime >= 500L
                if (held) {
                    if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP) viewModel.speedNextSentence()
                    else viewModel.speedPreviousSentence()
                } else {
                    viewModel.speedToggle()
                }
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}
