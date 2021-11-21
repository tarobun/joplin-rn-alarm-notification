package com.emekalites.react.alarm.notification;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.ArrayList;

import com.facebook.react.modules.core.DeviceEventManagerModule;

import static com.emekalites.react.alarm.notification.Constants.NOTIFICATION_ACTION_DISMISS;
import static com.emekalites.react.alarm.notification.Constants.NOTIFICATION_ACTION_SNOOZE;

public class AlarmReceiver extends BroadcastReceiver {

    private static final long[] DEFAULT_VIBRATE_PATTERN = {0, 250, 250, 250};

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }

        Log.d(Constants.TAG, "Received intent URI: " + intent.toUri(0));
        
        final AlarmDatabase alarmDB = new AlarmDatabase(context);
        AlarmUtil alarmUtil = new AlarmUtil((Application) context.getApplicationContext());
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        String intentType = intent.getExtras().getString("intentType");
        Log.i(Constants.TAG, "INTENT_TYPE: " + intentType);

        if(Constants.ADD_INTENT.equals(intentType)) {
            int id = intent.getExtras().getInt("PendingId");
            AlarmModel alarm = alarmDB.getAlarm(id);
            sendNotification(context, notificationManager, alarm);
            return;
        }

        String action = intent.getAction();
        Log.i(Constants.TAG, "ACTION: " + action);

        int id = intent.getExtras().getInt("AlarmId");
        AlarmModel alarm = alarmDB.getAlarm(id);
        notificationManager.cancel(alarm.getNotificationId());

        switch (action) {
            case Constants.NOTIFICATION_ACTION_SNOOZE:
                try {
                    alarmUtil.snoozeAlarm(alarm);
                    Log.i(Constants.TAG, "alarm snoozed: " + id);
                } catch (Exception e) {
                    Log.e(Constants.TAG, "Failed to snooze alarm", e);
                }
                break;

            case Constants.NOTIFICATION_ACTION_DISMISS:
                try {
                    Log.i(Constants.TAG, "Cancel alarm: " + id);
                    alarmUtil.cancelOnceAlarm(id);

                    // emit notification dismissed
                    // TODO also send all user-provided args back
                    ANModule.getReactAppContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                        .emit("OnNotificationDismissed", "{\"id\": \"" + id + "\"}");
                } catch (Exception e) {
                    Log.e(Constants.TAG, "Failed to dismiss alarm", e);
                }
                break;

            default:
                Log.e(Constants.TAG, "Received unknown action: " + action);
                break;
        }
    }

    private Class<?> getMainActivityClass(Context context) {
        try {
            String packageName = context.getPackageName();
            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
            String className = launchIntent.getComponent().getClassName();
            Log.d(Constants.TAG, "main activity classname: " + className);
            return Class.forName(className);
        } catch (Exception e) {
            Log.e(Constants.TAG, "Could not load main activity class", e);
            return null;
        }
    }

    private PendingIntent createOnDismissedIntent(Context context, int alarmId) {
        Intent intent = new Intent(context, AlarmDismissReceiver.class);
        intent.putExtra(Constants.NOTIFICATION_ALARM_ID, alarmId);
        return PendingIntent.getBroadcast(context.getApplicationContext(), alarmId, intent, 0);
    }

    private PendingIntent createPendingIntent(Context context, String action, int alarmId, int notificationId) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(action);
        intent.putExtra("AlarmId", alarmId);
        return PendingIntent.getBroadcast(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void sendNotification(Context context, NotificationManager notificationManager, AlarmModel alarm) {
        try {
            Class<?> intentClass = getMainActivityClass(context);

            if (intentClass == null) {
                Log.e(Constants.TAG, "No activity class found for the notification");
                return;
            }

            // title
            String title = alarm.getTitle();
            if (title == null || title.equals("")) {
                ApplicationInfo appInfo = context.getApplicationInfo();
                title = context.getPackageManager().getApplicationLabel(appInfo).toString();
            }

            // message
            // TODO move to AlarmModel constructor?
            String message = alarm.getMessage();
            if (message == null || message.equals("")) {
                Log.e(Constants.TAG, "Cannot send to notification centre because there is no 'message' found");
                return;
            }

            // channel
            // TODO move to AlarmModel constructor?
            String channelID = alarm.getChannel();
            if (channelID == null || channelID.equals("")) {
                Log.e(Constants.TAG, "Cannot send to notification centre because there is no 'channel' found");
                return;
            }

            Resources res = context.getResources();
            String packageName = context.getPackageName();

            //icon
            // TODO move to AlarmModel constructor?
            int smallIconResId;
            String smallIcon = alarm.getSmallIcon();
            if (smallIcon != null && !smallIcon.equals("")) {
                smallIconResId = res.getIdentifier(smallIcon, "mipmap", packageName);
            } else {
                smallIconResId = res.getIdentifier("ic_launcher", "mipmap", packageName);
            }

            Intent intent = new Intent(context, intentClass);
            intent.setAction(Constants.NOTIFICATION_ACTION_CLICK);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

            int alarmId = alarm.getId();
            intent.putExtra(Constants.NOTIFICATION_ALARM_ID, alarmId);
            intent.putExtra("data", alarm.getData());

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, channelID)
                    .setSmallIcon(smallIconResId)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setTicker(alarm.getTicker())
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setAutoCancel(alarm.isAutoCancel())
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setSound(null)
                    .setDeleteIntent(createOnDismissedIntent(context, alarmId));

            if (alarm.isPlaySound()) {
                // TODO use user-supplied sound if available
                mBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), AudioManager.STREAM_NOTIFICATION);
            }

            long vibration = alarm.getVibration();
            long[] vibrationPattern = vibration == 0 ? DEFAULT_VIBRATE_PATTERN : new long[]{0, vibration, 1000, vibration};

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel mChannel = new NotificationChannel(channelID, "Alarm Notify", NotificationManager.IMPORTANCE_HIGH);
                mChannel.enableLights(true);

                String color = alarm.getColor();
                if (color != null && !color.equals("")) {
                    mChannel.setLightColor(Color.parseColor(color));
                }

                if (mChannel.canBypassDnd()) {
                    mChannel.setBypassDnd(alarm.isBypassDnd());
                }

                if (alarm.isVibrate()) {
                    mChannel.setVibrationPattern(vibrationPattern);
                    mChannel.enableVibration(true);
                }

                notificationManager.createNotificationChannel(mChannel);
                mBuilder.setChannelId(channelID);
            } else {
                // set vibration
                mBuilder.setVibrate(alarm.isVibrate() ? vibrationPattern : null);
            }

            //color
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                String color = alarm.getColor();
                if (color != null && !color.equals("")) {
                    mBuilder.setColor(Color.parseColor(color));
                }
            }

            int notificationId = alarm.getNotificationId();
            PendingIntent pendingIntent = PendingIntent.getActivity(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setContentIntent(pendingIntent);

            if (alarm.isHasButton()) {
                PendingIntent pendingDismiss = createPendingIntent(context, NOTIFICATION_ACTION_DISMISS, notificationId, alarmId);
                NotificationCompat.Action dismissAction = new NotificationCompat.Action(android.R.drawable.ic_lock_idle_alarm, "DISMISS", pendingDismiss);
                mBuilder.addAction(dismissAction);

                PendingIntent pendingSnooze = createPendingIntent(context, NOTIFICATION_ACTION_SNOOZE, notificationId, alarmId);
                NotificationCompat.Action snoozeAction = new NotificationCompat.Action(R.drawable.ic_snooze, "SNOOZE", pendingSnooze);
                mBuilder.addAction(snoozeAction);
            }

            //use big text
            if (alarm.isUseBigText()) {
                mBuilder = mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(message));
            }

            //large icon
            String largeIcon = alarm.getLargeIcon();
            if (largeIcon != null && !largeIcon.equals("") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                int largeIconResId = res.getIdentifier(largeIcon, "mipmap", packageName);
                Bitmap largeIconBitmap = BitmapFactory.decodeResource(res, largeIconResId);
                if (largeIconResId != 0) {
                    mBuilder.setLargeIcon(largeIconBitmap);
                }
            }

            // set tag and push notification
            Notification notification = mBuilder.build();

            String tag = alarm.getTag();
            if (tag != null && !tag.equals("")) {
                notificationManager.notify(tag, notificationId, notification);
            } else {
                notificationManager.notify(notificationId, notification);
            }
            Log.i(Constants.TAG, "Sent notification with notification id: " + notificationId);
        } catch (Exception e) {
            Log.e(Constants.TAG, "Failed to send notification", e);
        }
    }
}
