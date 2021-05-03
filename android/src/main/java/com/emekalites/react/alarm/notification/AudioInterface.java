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
    private static final String TAG = AudioInterface.class.getSimpleName();

    private static MediaPlayer player;
    private static AudioInterface ourInstance = new AudioInterface();
    private Context mContext;
    private Uri uri;

    private AudioInterface() {
    }

    private static Context get() {
        return getInstance().getContext();
    }

    static synchronized AudioInterface getInstance() {
        return ourInstance;
    }

    void init(Context context) {
        uri = Settings.System.DEFAULT_ALARM_ALERT_URI;

        if (mContext == null) {
            this.mContext = context;
        }
    }

    private Context getContext() {
        return mContext;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    MediaPlayer getSingletonMedia(String soundName, String soundNames, AudioAttributes audioAttributes) {
        Log.e(TAG, "player: " + soundName + ", names: " + soundNames);
        if (player == null) {
            List<Integer> resIds = new ArrayList<Integer>();
            if (soundNames != null && !soundNames.equals("")){
                String[] names = soundNames.split(",");
                for (String item : names) {
                    int _resId;
                    if (mContext.getResources().getIdentifier(item, "raw", mContext.getPackageName()) != 0) {
                        _resId = mContext.getResources().getIdentifier(item, "raw", mContext.getPackageName());
                    } else {
                        String _item = item.substring(0, item.lastIndexOf('.'));
                        _resId = mContext.getResources().getIdentifier(_item, "raw", mContext.getPackageName());
                    }

                    resIds.add(_resId);
                }
            }

            int audioSessionId;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                audioSessionId = mContext.getSystemService(AudioManager.class).generateAudioSessionId();
            } else {
                audioSessionId = ((AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE)).generateAudioSessionId();
            }

            if (resIds.size() > 0) {
                Random rand = new Random();
                int n = rand.nextInt(resIds.size());

                int resId = resIds.get(n);

                player = MediaPlayer.create(get(), resId, audioAttributes, audioSessionId);
            } else if (soundName != null && !soundName.equals("")) {
                int resId;
                if (mContext.getResources().getIdentifier(soundName, "raw", mContext.getPackageName()) != 0) {
                    resId = mContext.getResources().getIdentifier(soundName, "raw", mContext.getPackageName());
                } else {
                    soundName = soundName.substring(0, soundName.lastIndexOf('.'));
                    resId = mContext.getResources().getIdentifier(soundName, "raw", mContext.getPackageName());
                }

                player = MediaPlayer.create(get(), resId, audioAttributes, audioSessionId);
            } else {
                player = MediaPlayer.create(get(), this.uri, null, audioAttributes, audioSessionId);
            }
        }

        return player;
    }

    void stopPlayer() {
        try {
            player.stop();
            player.reset();
            player.release();

            player = null;
        } catch (Exception e) {
            Log.e(TAG, "player not found", e);
        }
    }
}
