package com.emekalites.react.alarm.notification;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;

public class AlarmBootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.i(Constants.TAG, "Rescheduling after boot!");
            AlarmDatabase alarmDB = null;
            try {
                alarmDB = new AlarmDatabase(context);
                ArrayList<AlarmModel> alarms = alarmDB.getAlarmList(1);
                AlarmUtil alarmUtil = new AlarmUtil((Application) context.getApplicationContext());

                for (AlarmModel alarm : alarms) {
                    alarmUtil.setAlarm(alarm);
                }
            } catch (Exception e) {
                Log.e(Constants.TAG, "Could not reschedule alarms on boot", e);
            } finally {
                if (alarmDB != null) {
                    alarmDB.close();
                }
            }
        }
    }
}
