package com.emekalites.react.alarm.notification;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

class AudioInterface {
    private final Uri DEFAULT_SOUND = Settings.System.DEFAULT_NOTIFICATION_URI;

    private static MediaPlayer player;
    private static final AudioInterface ourInstance = new AudioInterface();

    private AudioInterface() {
    }

    static AudioInterface getInstance() {
        return ourInstance;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    MediaPlayer getSingletonMedia(Context context, String soundName, String soundNames, AudioAttributes audioAttributes) {
        if (player == null) {
            List<Integer> resIds = new ArrayList<>();
            if (soundNames != null && !soundNames.equals("")){
                String[] names = soundNames.split(",");
                for (String item : names) {
                    int _resId;
                    if (context.getResources().getIdentifier(item, "raw", context.getPackageName()) != 0) {
                        _resId = context.getResources().getIdentifier(item, "raw", context.getPackageName());
                    } else {
                        String _item = item.substring(0, item.lastIndexOf('.'));
                        _resId = context.getResources().getIdentifier(_item, "raw", context.getPackageName());
                    }

                    resIds.add(_resId);
                }
            }

            int audioSessionId;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                audioSessionId = context.getSystemService(AudioManager.class).generateAudioSessionId();
            } else {
                audioSessionId = ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE)).generateAudioSessionId();
            }

            if (resIds.size() > 0) {
                Random rand = new Random();
                int n = rand.nextInt(resIds.size());

                int resId = resIds.get(n);

                player = MediaPlayer.create(context, resId, audioAttributes, audioSessionId);
            } else if (soundName != null && !soundName.equals("")) {
                int resId;
                if (context.getResources().getIdentifier(soundName, "raw", context.getPackageName()) == 0) {
                    soundName = soundName.substring(0, soundName.lastIndexOf('.'));
                }
                resId = context.getResources().getIdentifier(soundName, "raw", context.getPackageName());

                player = MediaPlayer.create(context, resId, audioAttributes, audioSessionId);
            } else {
                player = MediaPlayer.create(context, this.DEFAULT_SOUND, null, audioAttributes, audioSessionId);
            }
        }

        return player;
    }

    void stopPlayer() {
        try {
            if (player.isPlaying()) {
                player.stop();
            }
            player.reset();
            player.release();

            player = null;
        } catch (Exception e) {
            Log.e(Constants.TAG, "player not found", e);
        }
    }
}
