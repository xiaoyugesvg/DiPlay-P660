package com.shilapi.xcertplay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import com.shilapi.xcertplay.airplay.CarPlayMediaButton
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * CarLetter head units (the P660/P760 family) deliver the steering-wheel media keys as a
 * broadcast instead of a global KeyEvent, so nothing reaches the accessibility input filter and
 * [WheelKeyService] never sees them. TbhCarPlay subscribes to the same action on these units; do
 * the same and forward the press to the active CarPlay session.
 *
 * The head unit's extra names are undocumented, so the command is looked up three ways: an integer
 * that is a media key code, a string naming the command, or the action suffix. Every received
 * broadcast is appended to `carletter-keys.log` on the external files directory, so an
 * unrecognised format can be read back over adb instead of guessed at.
 */
class CarLetterMediaReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val index = mediaIndex(intent)
        note(context, intent, index)
        if (index != null) {
            CarPlayBackgroundSession.snapshot()?.controller?.sendMediaButton(index)
        }
    }

    private fun mediaIndex(intent: Intent): Int? {
        intent.extras?.let { extras ->
            for (key in extras.keySet()) {
                when (val value = extras.get(key)) {
                    is Int -> indexForKeyCode(value)?.let { return it }
                    is Long -> indexForKeyCode(value.toInt())?.let { return it }
                    is String -> indexForText(value)?.let { return it }
                }
            }
        }
        return indexForText(intent.action.orEmpty())
    }

    private fun indexForKeyCode(code: Int): Int? = when (code) {
        KeyEvent.KEYCODE_MEDIA_NEXT -> CarPlayMediaButton.NEXT
        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> CarPlayMediaButton.PREVIOUS
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> CarPlayMediaButton.PLAY_PAUSE
        KeyEvent.KEYCODE_MEDIA_PLAY -> CarPlayMediaButton.PLAY
        KeyEvent.KEYCODE_MEDIA_PAUSE -> CarPlayMediaButton.PAUSE
        KeyEvent.KEYCODE_MEDIA_STOP -> CarPlayMediaButton.PLAY_PAUSE
        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> CarPlayMediaButton.NEXT
        KeyEvent.KEYCODE_MEDIA_REWIND -> CarPlayMediaButton.PREVIOUS
        else -> null
    }

    private fun indexForText(raw: String): Int? {
        val text = raw.lowercase(Locale.ROOT)
        return when {
            text.contains("next") || text.contains("forward") -> CarPlayMediaButton.NEXT
            text.contains("prev") || text.contains("rewind") -> CarPlayMediaButton.PREVIOUS
            text.contains("play") || text.contains("pause") || text.contains("toggle") ->
                CarPlayMediaButton.PLAY_PAUSE
            else -> null
        }
    }

    private fun note(context: Context, intent: Intent, index: Int?) {
        runCatching {
            val dir = context.getExternalFilesDir(null) ?: context.filesDir
            val stamp = SimpleDateFormat("MM-dd HH:mm:ss", Locale.ROOT).format(Date())
            File(dir, "carletter-keys.log")
                .appendText("$stamp action=${intent.action} extras=${describe(intent.extras)} -> ${index ?: "none"}\n")
        }
    }

    private fun describe(extras: Bundle?): String {
        if (extras == null) return "{}"
        return extras.keySet().joinToString(",", "{", "}") { "$it=${extras.get(it)}" }
    }
}
