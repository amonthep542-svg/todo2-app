package com.todoapp;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    WebView webView;
    public static final String CHANNEL_ID = "todo_alarms";
    public static final String PREFS = "todo_prefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createNotificationChannel();

        webView = new WebView(this);
        setContentView(webView);

        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setAllowFileAccessFromFileURLs(true);
        ws.setAllowUniversalAccessFromFileURLs(true);
        ws.setDatabaseEnabled(true);
        ws.setMediaPlaybackRequiresUserGesture(false);

        webView.addJavascriptInterface(new AndroidBridge(), "Android");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                return false;
            }
        });

        webView.loadUrl("file:///android_asset/www/index.html");

        // ถ้าเปิดจาก notification action
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    void handleIntent(Intent intent) {
        if (intent == null) return;
        String action = intent.getStringExtra("action");
        String taskId = intent.getStringExtra("taskId");
        if ("done".equals(action) && taskId != null) {
            // mark done via JS
            final String id = taskId;
            webView.post(() -> webView.evaluateJavascript(
                "if(typeof markDoneById==='function')markDoneById('" + id + "');", null));
        }
    }

    void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, "แจ้งเตือนสิ่งที่ต้องทำ",
                NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("แจ้งเตือนรายการที่ถึงเวลา");
            ch.enableVibration(true);
            ch.setVibrationPattern(new long[]{0, 300, 100, 300, 100, 500});
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(ch);
        }
    }

    // JS Bridge
    class AndroidBridge {

        // เรียกจาก JS เพื่อตั้ง alarm
        @JavascriptInterface
        public void scheduleAlarm(String taskId, String taskText, long triggerMs) {
            Intent i = new Intent(MainActivity.this, AlarmReceiver.class);
            i.putExtra("taskId", taskId);
            i.putExtra("taskText", taskText);
            int reqCode = taskId.hashCode();
            PendingIntent pi = PendingIntent.getBroadcast(
                MainActivity.this, reqCode, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi);
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi);
            }
        }

        // ยกเลิก alarm
        @JavascriptInterface
        public void cancelAlarm(String taskId) {
            Intent i = new Intent(MainActivity.this, AlarmReceiver.class);
            int reqCode = taskId.hashCode();
            PendingIntent pi = PendingIntent.getBroadcast(
                MainActivity.this, reqCode, i,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (pi != null) {
                AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
                am.cancel(pi);
            }
        }

        // บันทึก tasks ลง SharedPreferences (สำหรับ restore หลัง reboot)
        @JavascriptInterface
        public void saveTasks(String json) {
            SharedPreferences.Editor e = getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
            e.putString("tasks", json);
            e.apply();
        }

        @JavascriptInterface
        public String loadTasks() {
            return getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString("tasks", "{}");
        }

        @JavascriptInterface
        public void log(String msg) {
            android.util.Log.d("TodoApp", msg);
        }
    }
}
