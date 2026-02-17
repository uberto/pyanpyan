package com.pyanpyan.android.sound

import android.content.Context
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.util.Log
import com.pyanpyan.domain.model.AppSettings
import com.pyanpyan.domain.model.CompletionSound
import com.pyanpyan.domain.model.SwipeSound
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class SoundManager(
    private val context: Context,
    settingsFlow: Flow<AppSettings>,
    scope: CoroutineScope
) {
    private var toneGenerator: ToneGenerator? = null
    private var currentSettings: AppSettings = AppSettings()

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        } catch (e: Exception) {
            Log.e("SoundManager", "Failed to initialize ToneGenerator", e)
        }

        // Observe settings changes
        scope.launch {
            settingsFlow.collect { settings ->
                currentSettings = settings
            }
        }
    }

    fun playSwipeSound() {
        try {
            when (currentSettings.swipeSound) {
                SwipeSound.NONE -> { /* No sound */ }
                SwipeSound.SOFT_CLICK -> {
                    toneGenerator?.startTone(ToneGenerator.TONE_DTMF_1, 50)
                }
                SwipeSound.BEEP -> {
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                }
                SwipeSound.POP -> {
                    toneGenerator?.startTone(ToneGenerator.TONE_DTMF_9, 80)
                }
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "Error playing swipe sound", e)
        }
    }

    fun playCompletionSound() {
        try {
            when (currentSettings.completionSound) {
                CompletionSound.NONE -> { /* No sound */ }
                CompletionSound.NOTIFICATION -> {
                    playSystemNotification(RingtoneManager.TYPE_NOTIFICATION)
                }
                CompletionSound.SUCCESS_CHIME -> {
                    // Use default notification but could be customized
                    playSystemNotification(RingtoneManager.TYPE_NOTIFICATION)
                }
                CompletionSound.TADA -> {
                    // Use ringtone for longer sound
                    playSystemNotification(RingtoneManager.TYPE_RINGTONE)
                }
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "Error playing completion sound", e)
        }
    }

    private fun playSystemNotification(type: Int) {
        try {
            val notificationUri: Uri? = RingtoneManager.getDefaultUri(type)
            if (notificationUri != null) {
                val ringtone = RingtoneManager.getRingtone(context, notificationUri)
                ringtone?.play()
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "Error playing system notification", e)
        }
    }

    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (e: Exception) {
            Log.e("SoundManager", "Error releasing ToneGenerator", e)
        }
    }
}
