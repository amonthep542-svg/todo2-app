package com.todoapp;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        String taskId   = intent.getStringExtra("taskId");
        String taskText = intent.getStringExtra("taskText");
        if (taskId == null) return;

        // Intent เปิดแอป
        Intent openIntent = new Intent(ctx, MainActivity.class);
        openIntent.putExtra("action", "open");
        openIntent.putExtra("taskId", taskId);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPI = PendingIntent.getActivity(
            ctx, taskId.hashCode(), openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Intent กด "ทำแล้ว"
        Intent doneIntent = new Intent(ctx, NotifActionReceiver.class);
        doneIntent.putExtra("taskId", taskId);
        doneIntent.setAction("DONE_" + taskId);
        PendingIntent donePI = PendingIntent.getBroadcast(
            ctx, taskId.hashCode() + 1, doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, MainActivity.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("🔔 " + taskText)
            .setContentText("แตะเพื่อเปิดแอป")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)           // ไม่หายเมื่อแตะ (ต้องกดปุ่มเอง)
            .setOngoing(false)
            .setSound(sound)
            .setVibrate(new long[]{0, 300, 100, 300, 100, 500})
            .setContentIntent(openPI)
            // ปุ่ม "ทำแล้ว" ใน notification shade
            .addAction(android.R.drawable.checkbox_on_background, "✓ ทำแล้ว", donePI)
            // ค้างอยู่ใน notification shade จนกว่าจะกดเอง
            .setTimeoutAfter(-1);

        NotificationManagerCompat nm = NotificationManagerCompat.from(ctx);
        try {
            nm.notify(taskId.hashCode(), builder.build());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }
}
