package com.emekalites.react.alarm.notification;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

public class ANModule extends ReactContextBaseJavaModule implements ActivityEventListener {
    private final AlarmUtil alarmUtil;
    private static ReactApplicationContext mReactContext;

    private static final String E_SCHEDULE_ALARM_FAILED = "E_SCHEDULE_ALARM_FAILED";

    ANModule(ReactApplicationContext reactContext) {
        super(reactContext);
        mReactContext = reactContext;
        alarmUtil = new AlarmUtil((Application) reactContext.getApplicationContext());
        reactContext.addActivityEventListener(this);
    }

    static ReactApplicationContext getReactAppContext() {
        return mReactContext;
    }

    @NonNull
    @Override
    public String getName() {
        return "RNAlarmNotification";
    }

    private AlarmDatabase getAlarmDB() {
        return new AlarmDatabase(mReactContext);
    }

    @ReactMethod
    public void scheduleAlarm(ReadableMap details, Promise promise) throws ParseException {
        Bundle bundle = Arguments.toBundle(details);

        AlarmModel alarm = new AlarmModel();

        long time = System.currentTimeMillis() / 1000;

        alarm.setAlarmId((int) time);

        alarm.setActive(1);
        alarm.setAutoCancel(bundle.getBoolean("auto_cancel", true));
        alarm.setChannel(bundle.getString("channel", "my_channel_id"));
        alarm.setColor(bundle.getString("color", "red"));

        Bundle data = bundle.getBundle("data");
        alarm.setData(data);

        alarm.setInterval(bundle.getString("repeat_interval", "hourly"));
        alarm.setLargeIcon(bundle.getString("large_icon", ""));
        alarm.setLoopSound(bundle.getBoolean("loop_sound", false));
        alarm.setMessage(bundle.getString("message", "My Notification Message"));
        alarm.setPlaySound(bundle.getBoolean("play_sound", true));
        alarm.setScheduleType(bundle.getString("schedule_type", "once"));
        alarm.setSmallIcon(bundle.getString("small_icon", "ic_launcher"));
        alarm.setSnoozeInterval((int) bundle.getDouble("snooze_interval", 1.0));
        alarm.setSoundName(bundle.getString("sound_name", null));
        alarm.setSoundNames(bundle.getString("sound_names", null));
        alarm.setTag(bundle.getString("tag", ""));
        alarm.setTicker(bundle.getString("ticker", ""));
        alarm.setTitle(bundle.getString("title", "My Notification Title"));
        alarm.setVibrate(bundle.getBoolean("vibrate", true));
        alarm.setHasButton(bundle.getBoolean("has_button", false));
        alarm.setVibration((int) bundle.getDouble("vibration", 100.0));
        alarm.setUseBigText(bundle.getBoolean("use_big_text", false));
        alarm.setVolume(bundle.getDouble("volume", 0.5));
        alarm.setIntervalValue((int) bundle.getDouble("interval_value", 1));
        alarm.setBypassDnd(bundle.getBoolean("bypass_dnd", false));

        String datetime = bundle.getString("fire_date");
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.ENGLISH);
        Calendar calendar = new GregorianCalendar();

        calendar.setTime(sdf.parse(datetime));

        alarmUtil.setAlarmFromCalendar(alarm, calendar);

        // check if alarm has been set at this time
        boolean containAlarm = alarmUtil.checkAlarm(getAlarmDB().getAlarmList(1), alarm);
        if (!containAlarm) {
            try {
                int id = getAlarmDB().insert(alarm);
                alarm.setId(id);

                alarmUtil.setAlarm(alarm);

                WritableMap map = Arguments.createMap();
                map.putInt("id", id);

                promise.resolve(map);
            } catch (Exception e) {
                Log.e(Constants.TAG, "Could not schedule alarm", e);
                promise.reject(E_SCHEDULE_ALARM_FAILED, e);
            }
        } else {
            promise.reject(E_SCHEDULE_ALARM_FAILED, "duplicate alarm set at date");
        }
    }

    @ReactMethod
    public void deleteAlarm(int alarmID) {
        alarmUtil.deleteAlarm(alarmID);
    }

    @ReactMethod
    public void deleteRepeatingAlarm(int alarmID) {
        alarmUtil.deleteRepeatingAlarm(alarmID);
    }

    @ReactMethod
    public void stopAlarmSound() {
        alarmUtil.stopAlarmSound();
    }

    @ReactMethod
    public void sendNotification(ReadableMap details) {
        Bundle bundle = Arguments.toBundle(details);

        AlarmModel alarm = new AlarmModel();

        long time = System.currentTimeMillis() / 1000;

        alarm.setAlarmId((int) time);

        alarm.setActive(1);
        alarm.setAutoCancel(bundle.getBoolean("auto_cancel", true));
        alarm.setChannel(bundle.getString("channel", "my_channel_id"));
        alarm.setColor(bundle.getString("color", "red"));

        Bundle data = bundle.getBundle("data");
        alarm.setData(data);

        alarm.setLargeIcon(bundle.getString("large_icon"));
        alarm.setLoopSound(bundle.getBoolean("loop_sound", false));
        alarm.setMessage(bundle.getString("message", "My Notification Message"));
        alarm.setPlaySound(bundle.getBoolean("play_sound", true));
        alarm.setSmallIcon(bundle.getString("small_icon", "ic_launcher"));
        alarm.setSnoozeInterval((int) bundle.getDouble("snooze_interval", 1));
        alarm.setSoundName(bundle.getString("sound_name"));
        alarm.setSoundNames(bundle.getString("sound_names"));
        alarm.setTag(bundle.getString("tag"));
        alarm.setTicker(bundle.getString("ticker"));
        alarm.setTitle(bundle.getString("title", "My Notification Title"));
        alarm.setVibrate(bundle.getBoolean("loop_sound", true));
        alarm.setHasButton(bundle.getBoolean("has_button", false));
        alarm.setVibration((int) bundle.getDouble("vibration", 100));
        alarm.setUseBigText(bundle.getBoolean("use_big_text", false));
        alarm.setVolume(bundle.getDouble("volume", 0.5));
        alarm.setBypassDnd(bundle.getBoolean("bypass_dnd", false));

        Calendar calendar = new GregorianCalendar();

        alarmUtil.setAlarmFromCalendar(alarm, calendar);

        try {
            int id = getAlarmDB().insert(alarm);
            alarm.setId(id);

            alarmUtil.sendNotification(alarm);
        } catch (Exception e) {
            Log.e(Constants.TAG, "Could not send notification", e);
        }
    }

    @ReactMethod
    public void removeFiredNotification(int id) {
        alarmUtil.removeFiredNotification(id);
    }

    @ReactMethod
    public void removeAllFiredNotifications() {
        alarmUtil.removeAllFiredNotifications();
    }

    @ReactMethod
    public void getScheduledAlarms(Promise promise) throws JSONException {
        ArrayList<AlarmModel> alarms = alarmUtil.getAlarms();
        WritableArray array = Arguments.createArray();
        Gson gson = new Gson();
        for (AlarmModel alarm : alarms) {
            WritableMap alarmMap = alarmUtil.convertJsonToMap(new JSONObject(gson.toJson(alarm)));
            array.pushMap(alarmMap);
        }
        promise.resolve(array);
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {

    }

    @Override
    public void onNewIntent(Intent intent) {
        if (Constants.NOTIFICATION_ACTION_CLICK.equals(intent.getAction())) {
            Bundle bundle = intent.getExtras();
            try {
                if (bundle != null) {
                    int alarmId = bundle.getInt(Constants.NOTIFICATION_ID);
                    alarmUtil.removeFiredNotification(alarmId);
                    alarmUtil.doCancelAlarm(alarmId);

                    WritableMap response = Arguments.fromBundle(bundle.getBundle("data"));
                    mReactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                            .emit("OnNotificationOpened", response);
                }
            } catch (Exception e) {
                Log.e(Constants.TAG, "Couldn't convert bundle to JSON", e);
            }
        }
    }

    @ReactMethod
    public void getAlarmInfo(Promise promise) {
        if (getCurrentActivity() == null) {
            promise.resolve(null);
            return;
        }

        Intent intent = getCurrentActivity().getIntent();
        if (intent != null) {
            if (Constants.NOTIFICATION_ACTION_CLICK.equals(intent.getAction()) &&
                    intent.getExtras() != null) {
                Bundle bundle = intent.getExtras();
                WritableMap response = Arguments.fromBundle(bundle.getBundle("data"));
                promise.resolve(response);

                // cleanup

                // react-native-quick-actions does not expect the intent to be null so set an empty intent here
                getCurrentActivity().setIntent(new Intent());

                int alarmId = bundle.getInt(Constants.NOTIFICATION_ID);
                alarmUtil.removeFiredNotification(alarmId);
                alarmUtil.doCancelAlarm(alarmId);

                return;
            }
        }
        promise.resolve(null);
    }
}
