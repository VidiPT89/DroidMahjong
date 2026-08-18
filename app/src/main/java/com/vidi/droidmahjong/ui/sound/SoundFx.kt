package com.vidi.droidmahjong.ui.sound

import android.media.AudioManager
import android.media.ToneGenerator

/**
 * PLACEHOLDER SOUND FEEDBACK.
 *
 * This repository ships no custom audio assets under `res/raw/` (there is no sound designer
 * output to draw from yet), so tile-pick / match / mismatch / win feedback is synthesized at
 * runtime with [android.media.ToneGenerator] — simple DTMF-style system tones, not real SFX.
 *
 * To swap in real audio later:
 *   1. Drop `.ogg`/`.mp3` files into `app/src/main/res/raw/` (e.g. `tile_pick.ogg`,
 *      `tile_match.ogg`, `tile_mismatch.ogg`, `game_win.ogg`).
 *   2. Replace the `ToneGenerator.startTone(...)` calls below with a small `android.media.SoundPool`
 *      (load each `R.raw.*` resource once in [init]/a lazy `load()`, then `pool.play(...)` per event).
 *   3. Everything else — the [SoundFx] call sites in the UI — stays the same, since callers only
 *      ever call [tilePick]/[tileMatch]/[tileMismatch]/[win]/[release].
 */
class SoundFx {
    private var toneGenerator: ToneGenerator? = null
    private var enabled = true

    private fun generator(): ToneGenerator? {
        if (!enabled) return null
        var tg = toneGenerator
        if (tg == null) {
            tg = try {
                ToneGenerator(AudioManager.STREAM_MUSIC, VOLUME)
            } catch (e: RuntimeException) {
                // No audio output available (e.g. some test/emulator configurations) — fail
                // silently rather than crash the game over sound feedback.
                null
            }
            toneGenerator = tg
        }
        return tg
    }

    /** Short, quiet blip when a tile is picked/selected. */
    fun tilePick() {
        generator()?.startTone(ToneGenerator.TONE_PROP_BEEP, TONE_DURATION_SHORT_MS)
    }

    /** A brighter double-tone confirming a successful pair match. */
    fun tileMatch() {
        generator()?.startTone(ToneGenerator.TONE_PROP_ACK, TONE_DURATION_SHORT_MS)
    }

    /** A low "buzz" tone for an invalid/blocked selection or a mismatch. */
    fun tileMismatch() {
        generator()?.startTone(ToneGenerator.TONE_PROP_NACK, TONE_DURATION_SHORT_MS)
    }

    /** Longer celebratory tone on a full board clear. */
    fun win() {
        generator()?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, TONE_DURATION_LONG_MS)
    }

    fun setEnabled(value: Boolean) {
        enabled = value
    }

    /** Releases the underlying ToneGenerator; call from a DisposableEffect/onDispose. */
    fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }

    companion object {
        private const val VOLUME = 60 // 0-100, kept modest since these are placeholder tones
        private const val TONE_DURATION_SHORT_MS = 80
        private const val TONE_DURATION_LONG_MS = 400
    }
}
