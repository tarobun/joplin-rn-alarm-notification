package com.emekalites.react.alarm.notification;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
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

import java.util.ArrayList;

@SuppressWarnings("unused")
public class ANModule extends ReactContextBaseJavaModule implements ActivityEventListener {
    private final AlarmUtil alarmUtil;
    private final AlarmDatabase alarmDB;
    private static ReactApplicationContext mReactContext;

    private static final String E_SCHEDULE_ALARM_FAILED = "E_SCHEDULE_ALARM_FAILED";

    ANModule(ReactApplicationContext reactContext) {
        super(reactContext);
        
        mReactContext = reactContext;
        alarmUtil = new AlarmUtil((Application) reactContext.getApplicationContext());
        alarmDB = new AlarmDatabase(mReactContext);

        mReactContext.addActivityEventListener(this);
    }

    static ReactApplicationContext getReactAppContext() {
        return mReactContext;
    }

    @NonNull
    @Override
    public String getName() {
        return "RNAlarmNotification";
    }

    @ReactMethod
    public void scheduleAlarm(ReadableMap details, Promise promise) {
        try {
            Bundle bundle = Arguments.toBundle(details);
            AlarmModel alarm = AlarmModel.fromBundle(bundle);

            // check if alarm has been set at this time
            boolean containAlarm = alarmUtil.checkAlarm(alarmDB.getActiveAlarmList(), alarm);
            if (containAlarm) {
                promise.reject(E_SCHEDULE_ALARM_FAILED, "duplicate alarm set at date");
                return;
            }

            int id = alarmDB.insert(alarm);
            alarm.setId(id);

            alarmUtil.setAlarm(alarm);

            WritableMap map = Arguments.createMap();
            map.putInt("id", id);
            promise.resolve(map);

        } catch (Exception e) {
            Log.e(Constants.TAG, "Could not schedule alarm", e);
            promise.reject(E_SCHEDULE_ALARM_FAILED, e);
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
    public void removeFiredNotification(int id) {
        alarmUtil.removeFiredNotification(id);
    }

    @ReactMethod
    public void removeAllFiredNotifications() {
        alarmUtil.removeAllFiredNotifications();
    }

    @ReactMethod
    public void getScheduledAlarms(Promise promise) throws JSONException {
        ArrayList<AlarmModel> alarms = alarmDB.getActiveAlarmList();
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
            if (bundle == null) {
                return;
            }

            try {
                int id = bundle.getInt(Constants.NOTIFICATION_ALARM_ID);
                alarmUtil.removeFiredNotification(id);
                alarmUtil.cancelOnceAlarm(id);

                mReactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                        .emit("OnNotificationOpened", "{\"id\": \"" + id + "\"}");
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
        if (intent == null || intent.getExtras() == null) {
            promise.resolve(null);
            return;
        }

        if (Constants.NOTIFICATION_ACTION_CLICK.equals(intent.getAction())) {
            Bundle bundle = intent.getExtras();
            WritableMap data = Arguments.fromBundle(bundle.getBundle("data"));
            promise.resolve(data);

            // cleanup

            // other libs may not expect the intent to be null so set an empty intent here
            getCurrentActivity().setIntent(new Intent());

            int id = bundle.getInt(Constants.NOTIFICATION_ALARM_ID);
            alarmUtil.removeFiredNotification(id);
            alarmUtil.cancelOnceAlarm(id);
        }
    }
}
