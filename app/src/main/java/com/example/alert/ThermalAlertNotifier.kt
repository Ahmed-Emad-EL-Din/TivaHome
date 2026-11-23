package com.example.alert

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ThermalAlertNotifier(private val context: Context) {

  private val scope = CoroutineScope(Dispatchers.Default)

  fun triggerAlert(playSound: Boolean, vibrate: Boolean) {
    if (playSound) {
      scope.launch { playShortAlertTone() }
    }
    if (vibrate) {
      triggerVibrationPulse()
    }
  }

  private fun playShortAlertTone() {
    // Primary: lightweight non-blocking ToneGenerator beep; fallback to default notification ringtone
    val playedTone =
      runCatching {
        val tone = ToneGenerator(AudioManager.STREAM_ALARM, 85)
        tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 420)
        Thread.sleep(460)
        tone.release()
      }.isSuccess

    if (!playedTone) {
      runCatching {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val ringtone = RingtoneManager.getRingtone(context, uri)
        ringtone?.audioAttributes =
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        ringtone?.play()
      }
    }
  }

  private fun triggerVibrationPulse() {
    runCatching {
      val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          val manager =
            context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
          manager?.defaultVibrator
        } else {
          @Suppress("DEPRECATION")
          context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

      if (vibrator?.hasVibrator() == true) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          val pattern = longArrayOf(0, 180, 100, 240)
          vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
          @Suppress("DEPRECATION")
          vibrator.vibrate(longArrayOf(0, 180, 100, 240), -1)
        }
      }
    }
  }
}
