package com.elias.kamasona.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Pronunciation: prefer the bundled community recordings (assets/audio/<word>.mp3),
 * fall back to on-device TTS (eSpeak NG is installed on the S6) when a clip is missing.
 */
object Speaker {

    private var player: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    fun init(ctx: Context) {
        if (tts != null) return
        tts = TextToSpeech(ctx.applicationContext) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) {
                // toki pona has no TTS locale; a vowel-pure locale beats US English here.
                val t = tts ?: return@TextToSpeech
                t.setLanguage(Locale.ITALIAN)
                t.setSpeechRate(0.85f)
                t.setPitch(1.0f)
            }
        }
    }

    fun release() {
        player?.release(); player = null
        tts?.shutdown(); tts = null
        ttsReady = false
    }

    /** Play the word: bundled clip if present, else TTS. */
    fun say(ctx: Context, word: String) {
        val path = "audio/$word.mp3"
        val ok = try {
            ctx.assets.openFd(path).use { fd ->
                player?.release()
                val mp = MediaPlayer()
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                mp.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                mp.prepare()
                mp.start()
                player = mp
                true
            }
        } catch (e: Exception) {
            false
        }
        if (!ok) sayTts(word)
    }

    private fun sayTts(word: String) {
        val t = tts ?: return
        if (!ttsReady) return
        t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) {}
            @Deprecated("old API") override fun onError(id: String?) {}
        })
        t.speak(word.replace("-", " "), TextToSpeech.QUEUE_FLUSH, null, "tp_$word")
    }

    fun hasClip(ctx: Context, word: String): Boolean = try {
        ctx.assets.openFd("audio/$word.mp3").use { true }
    } catch (e: Exception) {
        false
    }
}
