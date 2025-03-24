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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class ActivityComponentTest extends AppCompatActivity {
    private static final String TAG = "ActivityComponentTest";
    private ArrayList<String> componentNames = new ArrayList<>();
    private String packageName;
    private PackageManager packageManager;
    private AtomicReference<String> currentComponent = new AtomicReference<>();
    private volatile boolean monitoring = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        packageName = getIntent().getStringExtra("packageName");
        packageManager = getPackageManager();

        startLogMonitor(); // 启动日志监控
        loadComponents();//加载组件
        autoTestComponents();//自动化测试
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
                        String component = currentComponent.get();
                        Log.w("Auto Fuzzer", "[CRASH DETECTED] Component: " + component
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

    private void loadComponents() {
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(
                    packageName, PackageManager.GET_ACTIVITIES);

            if (packageInfo.activities != null) {
                for (ActivityInfo activityInfo : packageInfo.activities) {
                    if (activityInfo.exported) {
                        componentNames.add(activityInfo.name);
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("Auto Fuzzer", "Package not found: " + e.getMessage());
            finish();
        }
    }

    private void autoTestComponents() {
        new Thread(() -> {
            for (String activityName : componentNames) {
                currentComponent.set(activityName);

                // 普通Intent测试
                testComponent(activityName, false);

                // 带序列化数据的测试
                testComponent(activityName, true);

                // 组件测试间隔
                safeSleep(500);
            }

            monitoring = false;
            finish();
        }).start();
    }

    private void testComponent(String activityName, boolean withData) {
        runOnUiThread(() -> {
            try {
                ComponentName component = new ComponentName(packageName, activityName);
                Intent intent = new Intent().setComponent(component);

                if (withData) {
                    intent.putExtra("test_data", new SafeSerializableData());
                }

                startActivity(intent);
                Log.i("Auto Fuzzer", "Successfully launched: " + activityName);
            } catch (SecurityException e) {
                Log.w("Auto Fuzzer", "Permission denied for: " + activityName);
            } catch (Exception e) {
                Log.e("Auto Fuzzer", "Error launching " + activityName
                        + ": " + e.getClass().getSimpleName());
            }
        });

        // 等待Activity启动
        safeSleep(300);
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

