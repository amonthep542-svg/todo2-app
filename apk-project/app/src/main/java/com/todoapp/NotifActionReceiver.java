package com.todoapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationManagerCompat;

public class NotifActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        String taskId = intent.getStringExtra("taskId");
        if (taskId == null) return;

        // ลบ notification ออก
        NotificationManagerCompat.from(ctx).cancel(taskId.hashCode());

        // เปิดแอปแล้วส่ง action=done
        Intent open = new Intent(ctx, MainActivity.class);
        open.putExtra("action", "done");
        open.putExtra("taskId", taskId);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        ctx.startActivity(open);
    }
}
