package com.emekalites.react.alarm.notification;

import android.app.AlarmManager;
import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;

import static com.emekalites.react.alarm.notification.Constants.ADD_INTENT;

class AlarmUtil {

    private final Context context;
    private final AlarmDatabase alarmDB;

    AlarmUtil(Application context) {
        this.context = context;
        alarmDB =  new AlarmDatabase(context);
    }

    private AlarmManager getAlarmManager() {
        return (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    private NotificationManager getNotificationManager() {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    boolean checkAlarm(ArrayList<AlarmModel> alarms, AlarmModel alarm) {
        for (AlarmModel aAlarm : alarms) {
            if (aAlarm.isSameTime(alarm) && aAlarm.getActive() == 1) {
                Toast.makeText(context, "You have already set this Alarm", Toast.LENGTH_SHORT).show();
                return true;
            }
        }
        return false;
    }

    void setBootReceiver() {
        ArrayList<AlarmModel> alarms = alarmDB.getActiveAlarmList();
        if (alarms.size() > 0) {
            enableBootReceiver(context);
        } else {
            disableBootReceiver(context);
        }
    }

    void setAlarm(AlarmModel alarm) {
        Log.i(Constants.TAG, "Set alarm " + alarm);

        Calendar calendar = alarm.getAlarmDateTime();
        int notificationId = alarm.getNotificationId();

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("intentType", ADD_INTENT);
        intent.putExtra("PendingId", alarm.getId());

        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, notificationId, intent, 0);
        AlarmManager alarmManager = this.getAlarmManager();

        String scheduleType = alarm.getScheduleType();
        switch(scheduleType) {
            case "once":
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), alarmIntent);
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), alarmIntent);
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), alarmIntent);
                }
                break;
            
            case "repeat":
                long interval = this.getInterval(alarm.getInterval(), alarm.getIntervalValue());
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), interval, alarmIntent);
                break;

            default:
                Log.w(Constants.TAG, "Schedule type should either be once or repeat");
                return;
        }

        this.setBootReceiver();
    }

    void snoozeAlarm(AlarmModel alarm) {
        Log.i(Constants.TAG, "Snooze alarm: " + alarm.toString());

        Calendar calendar = alarm.snooze();

        long time = System.currentTimeMillis() / 1000;

        int notificationId = (int) time;
        alarm.setNotificationId(notificationId);
        // TODO looks like this sets a new id and then tries to update the row in DB
        // how's that supposed to work?
        alarmDB.update(alarm);

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("intentType", ADD_INTENT);
        intent.putExtra("PendingId", alarm.getId());

        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, notificationId, intent, 0);
        AlarmManager alarmManager = this.getAlarmManager();

        String scheduleType = alarm.getScheduleType();
        switch(scheduleType) {
            case "once":
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), alarmIntent);
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), alarmIntent);
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), alarmIntent);
                }
                break;

            case "repeat":
                long interval = this.getInterval(alarm.getInterval(), alarm.getIntervalValue());
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), interval, alarmIntent);
                break;

            default:
                Log.w(Constants.TAG, "Schedule type should either be once or repeat");
                break;
        }
    }

    long getInterval(String interval, int value) {
        long duration = 1;

        switch (interval) {
            case "minutely":
                duration = value;
                break;
            case "hourly":
                duration = 60 * value;
                break;
            case "daily":
                duration = 60 * 24;
                break;
            case "weekly":
                duration = 60 * 24 * 7;
                break;
        }

        return duration * 60 * 1000;
    }

    void cancelOnceAlarm(int id) {
        try {
            this.deleteOnceAlarm(id);
        } catch (Exception e) {
            Log.e(Constants.TAG, "Could not cancel alarm with id " + id, e);
        }
    }

    void deleteAlarm(int id) {
        try {
            AlarmModel alarm = alarmDB.getAlarm(id);
            if (alarm == null) {
                Log.w(Constants.TAG, "Cannot delete alarm as alarm id " + id + " doesn't exist");
                return;
            }
            this.stopAlarm(alarm);
        } catch (Exception e) {
            Log.e(Constants.TAG, "Could not delete alarm with id " + id, e);
        }
    }

    void deleteRepeatingAlarm(int id) {
        try {
            AlarmModel alarm = alarmDB.getAlarm(id);
            if (alarm == null) {
                Log.w(Constants.TAG, "Cannot delete repeating alarm as alarm id " + id + " doesn't exist");
                return;
            }

            String scheduleType = alarm.getScheduleType();
            if (scheduleType.equals("repeat")) {
                this.stopAlarm(alarm);
            }
        } catch (Exception e) {
            Log.e(Constants.TAG, "Could not delete repeating alarm with id " + id, e);
        }
    }

    void deleteOnceAlarm(int id) {
        try {
            AlarmModel alarm = alarmDB.getAlarm(id);
            if (alarm == null) {
                Log.w(Constants.TAG, "Cannot delete once alarm as alarm id " + id + " doesn't exist");
                return;
            }

            String scheduleType = alarm.getScheduleType();
            if (scheduleType.equals("once")) {
                this.stopAlarm(alarm);
            }
        } catch (Exception e) {
            Log.e(Constants.TAG, "Could not delete once alarm with id " + id, e);
        }
    }

    void stopAlarm(AlarmModel alarm) {
        AlarmManager alarmManager = this.getAlarmManager();

        int noticationId = alarm.getNotificationId();

        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, noticationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.cancel(alarmIntent);

        alarmDB.delete(alarm.getId());

        this.setBootReceiver();
    }

    private void enableBootReceiver(Context context) {
        ComponentName receiver = new ComponentName(context, AlarmBootReceiver.class);
        PackageManager pm = context.getPackageManager();

        int setting = pm.getComponentEnabledSetting(receiver);
        if (setting == PackageManager.COMPONENT_ENABLED_STATE_DISABLED ||
            setting == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {
            Log.i(Constants.TAG, "Enable boot receiver");
            pm.setComponentEnabledSetting(receiver,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
        } else {
            Log.i(Constants.TAG, "Boot receiver already enabled");
        }
    }

    private void disableBootReceiver(Context context) {
        Log.i(Constants.TAG, "Disable boot receiver");

        ComponentName receiver = new ComponentName(context, AlarmBootReceiver.class);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    void removeFiredNotification(int id) {
        try {
            AlarmModel alarm = alarmDB.getAlarm(id);
            if (alarm == null) {
                Log.w(Constants.TAG, "Cannot remove notification as alarm id " + id + " doesn't exist");
                return;
            }
            int notificationId = alarm.getNotificationId();
            getNotificationManager().cancel(notificationId);
            Log.i(Constants.TAG, "Removed fired alarm " + id + " with notificationId: " + notificationId);
        } catch (Exception e) {
            Log.e(Constants.TAG, "Could not remove fired notification with id " + id, e);
        }
    }

    void removeAllFiredNotifications() {
        getNotificationManager().cancelAll();
    }

    WritableMap convertJsonToMap(JSONObject jsonObject) throws JSONException {
        WritableMap map = new WritableNativeMap();

        Iterator<String> iterator = jsonObject.keys();
        while (iterator.hasNext()) {
            String key = iterator.next();
            Object value = jsonObject.get(key);
            if (value instanceof JSONObject) {
                map.putMap(key, convertJsonToMap((JSONObject) value));
            } else if (value instanceof Boolean) {
                map.putBoolean(key, (Boolean) value);
            } else if (value instanceof Integer) {
                map.putInt(key, (Integer) value);
            } else if (value instanceof Double) {
                map.putDouble(key, (Double) value);
            } else if (value instanceof String) {
                map.putString(key, (String) value);
            } else {
                map.putString(key, value.toString());
            }
        }
        return map;
    }
}
