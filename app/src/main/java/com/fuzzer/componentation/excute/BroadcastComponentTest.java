package com.fuzzer.componentation.excute;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;

import com.fuzzer.componentation.module.SafeSerializableData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class BroadcastComponentTest extends AppCompatActivity {
    private static final String TAG = "BroadcastTest";
    private ArrayList<String> receiverNames = new ArrayList<>();
    private String packageName;
    private PackageManager packageManager;
    private AtomicReference<String> currentReceiver = new AtomicReference<>();
    private volatile boolean monitoring = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        packageName = getIntent().getStringExtra("packageName");
        packageManager = getPackageManager();

        startLogMonitor(); // 启动日志监控
        loadReceivers();   // 加载广播接收器
        autoTestReceivers(); // 自动化测试
    }

    private void startLogMonitor() {
        new Thread(() -> {
            Process process = null;
            try {
                process = Runtime.getRuntime().exec("logcat -v brief");
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));

                String line;
                while (monitoring && (line = reader.readLine()) != null) {
                    if (line.contains(packageName) && isCrashLog(line)) {
                        String receiver = currentReceiver.get();
                        Log.w("Auto Fuzzer", "[CRASH DETECTED] Receiver: " + receiver
                                + " | Log: " + line);
                    }
                }
            } catch (IOException e) {
                Log.e("Auto Fuzzer", "Log monitoring failed: " + e.getMessage());
            } finally {
                if (process != null) process.destroy();
            }
        }).start();
    }

    private boolean isCrashLog(String log) {
        return log.contains("FATAL EXCEPTION")
                || log.contains("AndroidRuntime: CRASH")
                || log.contains("AndroidRuntime: FATAL");
    }

    private void loadReceivers() {
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(
                    packageName, PackageManager.GET_RECEIVERS);

            if (packageInfo.receivers != null) {
                for (ActivityInfo receiverInfo : packageInfo.receivers) {
                    if (receiverInfo.exported) {
                        receiverNames.add(receiverInfo.name);
                        Log.d(TAG, "Found exported receiver: " + receiverInfo.name);
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("Auto Fuzzer", "Package not found: " + e.getMessage());
            finish();
        }
    }

    private void autoTestReceivers() {
        new Thread(() -> {
            for (String receiverName : receiverNames) {
                currentReceiver.set(receiverName);

                // 普通广播测试
                testReceiver(receiverName, false);

                // 带序列化数据的测试
                testReceiver(receiverName, true);

                // 测试间隔防止过快
                safeSleep(300);
            }

            monitoring = false;
            finish();
        }).start();
    }

    private void testReceiver(String receiverName, boolean withData) {
        try {
            ComponentName component = new ComponentName(packageName, receiverName);
            Intent intent = new Intent().setComponent(component);

            if (withData) {
                intent.putExtra("test_data", new SafeSerializableData());
            }

            sendBroadcast(intent);
            Log.i("Auto Fuzzer", "Broadcast sent to: " + receiverName);
        } catch (SecurityException e) {
            Log.w("Auto Fuzzer", "Permission denied for: " + receiverName);
        } catch (Exception e) {
            Log.e("Auto Fuzzer", "Error sending to " + receiverName
                    + ": " + e.getClass().getSimpleName());
        }

        safeSleep(200); // 等待广播处理
    }

    private void safeSleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    protected void onDestroy() {
        monitoring = false;
        super.onDestroy();
    }
}

// 安全可序列化测试数据
