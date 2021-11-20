package com.emekalites.react.alarm.notification;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.facebook.react.modules.core.DeviceEventManagerModule;

public class AlarmDismissReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            int id = intent.getExtras().getInt(Constants.NOTIFICATION_ID);
            if (ANModule.getReactAppContext() != null) {
                // TODO also send all user-provided args back
                ANModule.getReactAppContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                        .emit("OnNotificationDismissed", "{\"id\": \"" + id + "\"}");
            }

            AlarmUtil alarmUtil = new AlarmUtil((Application) context.getApplicationContext());
            alarmUtil.removeFiredNotification(id);
            alarmUtil.doCancelAlarm(id);
        } catch (Exception e) {
            Log.e(Constants.TAG, "Exception when handling notification dismiss. " + e);
        }
    }
}
