package com.emekalites.react.alarm.notification;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.ArrayList;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }

        Log.d(Constants.TAG, "Received intent URI: " + intent.toUri(0));
        
        final AlarmDatabase alarmDB = new AlarmDatabase(context);
        AlarmUtil alarmUtil = new AlarmUtil((Application) context.getApplicationContext());

        String intentType = intent.getExtras().getString("intentType");
        Log.i(Constants.TAG, "INTENT_TYPE: " + intentType);

        if(Constants.ADD_INTENT.equals(intentType))
            int id = intent.getExtras().getInt("PendingId");

            try {
                AlarmModel alarm = alarmDB.getAlarm(id);
                alarmUtil.sendNotification(alarm);
                alarmUtil.setBootReceiver();
                Log.i(Constants.TAG, "alarm started: " + id);
            } catch (Exception e) {
                Log.e(Constants.TAG, "Failed to add alarm", e);
            }
            return;
        }

        String action = intent.getAction();
        Log.i(Constants.TAG, "ACTION: " + action);

        switch (action) {
            case Constants.NOTIFICATION_ACTION_SNOOZE:
                int id = intent.getExtras().getInt("SnoozeAlarmId");

                try {
                    AlarmModel alarm = alarmDB.getAlarm(id);
                    alarmUtil.snoozeAlarm(alarm);
                    Log.i(Constants.TAG, "alarm snoozed: " + id);

                    alarmUtil.removeFiredNotification(id);
                } catch (Exception e) {
                    Log.e(Constants.TAG, "Failed to snooze alarm", e);
                }
                break;

            case Constants.NOTIFICATION_ACTION_DISMISS:
                id = intent.getExtras().getInt("AlarmId");

                try {
                    Log.i(Constants.TAG, "Cancel alarm: " + id);

                    // emit notification dismissed
                    // TODO also send all user-provided args back
                    ANModule.getReactAppContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                        .emit("OnNotificationDismissed", "{\"id\": \"" + id + "\"}");

                    alarmUtil.removeFiredNotification(id);
                    alarmUtil.cancelOnceAlarm(id);
                } catch (Exception e) {
                    Log.e(Constants.TAG, "Failed to dismiss alarm", e);
                }
                break;

            default:
                Log.e(Constants.TAG, "Received unknown action: " + action);
                break;
        }
    }
}
