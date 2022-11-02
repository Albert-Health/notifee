package app.notifee.core;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Looper;
import android.util.Log;

public class VoiceNotificationPlayer {

  private long activatedAt = Long.MAX_VALUE;
  private static final int DURATION = 8000;
  AudioManager audio;
  double initialVolumeLevel;

  public VoiceNotificationPlayer(Context context) {
    this.audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    this.initialVolumeLevel = getMusicStreamLevel();
  }

  public boolean isActive() {
    long activeFor = System.currentTimeMillis() - activatedAt;
    return activeFor >= 0 && activeFor <= DURATION;
  }

  private double getMusicStreamLevel() {
    int maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    int currentVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);

    if (maxVolume == 0 || currentVolume == 0)
      return 0.0;

    return currentVolume * 1.0 / maxVolume;
  }

  private double getRingerSteamLevel() {
    int maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_RING);
    int currentVolume = audio.getStreamVolume(AudioManager.STREAM_RING);

    if (maxVolume == 0 || currentVolume == 0)
      return 0.0;

    return currentVolume * 1.0 / maxVolume;
  }

  public void playNotification(final String soundName) {
    if (isActive() || audio.getRingerMode() == AudioManager.RINGER_MODE_SILENT)
      return;

    if (initialVolumeLevel < getRingerSteamLevel()) {
      int maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
      double targetAudioLevel = maxVolume * getRingerSteamLevel();
      audio.setStreamVolume(AudioManager.STREAM_MUSIC, (int) targetAudioLevel, 0);
    }

    activatedAt = System.currentTimeMillis();
    new Thread(new Runnable() {
      @Override
      public void run() {
        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
          mediaPlayer.setDataSource(soundName);
          mediaPlayer.prepare();
          mediaPlayer.start();
        } catch (Exception e) {
          Log.i("VoiceNotificationPlayer", "exception catch: " + e.getLocalizedMessage());
          e.printStackTrace();
        } finally {
          new android.os.Handler(Looper.getMainLooper()).postDelayed(
            new Runnable() {
              public void run() {
                int maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                double targetAudioLevel = maxVolume * initialVolumeLevel;

                audio.setStreamVolume(AudioManager.STREAM_MUSIC,
                  (int)targetAudioLevel,
                  0);
              }
            },
            8000);
        }
      }
    }).start();
  }
}

