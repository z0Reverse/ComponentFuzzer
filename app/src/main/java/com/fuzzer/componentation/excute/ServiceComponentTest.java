package com.fuzzer.componentation.excute;



import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;

import com.fuzzer.componentation.module.SafeSerializableData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class ServiceComponentTest extends AppCompatActivity {
    private static final String TAG = "ServiceComponentTest";
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

        startLogMonitor();
        loadComponents();
        autoTestComponents();
    }

    // 日志监控（保持与Activity检测相同）
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
                        Log.w("Auto Fuzzer", "[SERVICE CRASH] Component: " + component
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

    // 添加Service特有的崩溃关键字
    private boolean isCrashLog(String log) {
        return log.contains("FATAL EXCEPTION")
                || log.contains("Service crashed")
                || log.contains("AndroidRuntime: FATAL")
                || log.contains("ServiceConnectionLeaked");
    }

    // 加载Service组件
    private void loadComponents() {
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(
                    packageName, PackageManager.GET_SERVICES);

            if (packageInfo.services != null) {
                for (ServiceInfo serviceInfo : packageInfo.services) {
                    if (serviceInfo.exported) {
                        componentNames.add(serviceInfo.name);
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("Auto Fuzzer", "Package not found: " + e.getMessage());
            finish();
        }
    }

    // 自动化测试逻辑
    private void autoTestComponents() {
        new Thread(() -> {
            for (String serviceName : componentNames) {
                currentComponent.set(serviceName);

                // 测试两种启动方式
                testComponent(serviceName, false); // startService
                testComponent(serviceName, true);  // bindService

                safeSleep(500);
            }

            monitoring = false;
            finish();
        }).start();
    }

    private void testComponent(String serviceName, boolean bindMode) {
        runOnUiThread(() -> {
            try {
                ComponentName component = new ComponentName(packageName, serviceName);
                Intent intent = new Intent().setComponent(component);

                if (bindMode) {
                    // 绑定服务测试
                    boolean result = bindService(intent, new DummyConnection(),
                            BIND_AUTO_CREATE);
                    Log.i("Auto Fuzzer", "BindService result: " + result
                            + " for " + serviceName);
                } else {
                    // 启动服务测试
                    if (android.os.Build.VERSION.SDK_INT >= 26) {
                        startForegroundService(intent);
                    } else {
                        startService(intent);
                    }
                    Log.i("Auto Fuzzer", "Started service: " + serviceName);
                }

                // 添加序列化数据测试
                testWithSerializedData(serviceName, bindMode);

            } catch (SecurityException e) {
                Log.w("Auto Fuzzer", "Permission denied for: " + serviceName);
            } catch (Exception e) {
                Log.e("Auto Fuzzer", "Error in " + (bindMode ? "bind" : "start")
                        + " service " + serviceName + ": " + e.getClass().getSimpleName());
            }
        });

        safeSleep(300);
    }

    // 带序列化数据的测试
    private void testWithSerializedData(String serviceName, boolean bindMode) {
        try {
            ComponentName component = new ComponentName(packageName, serviceName);
            Intent intent = new Intent().setComponent(component);
            intent.putExtra("serial_data", new SafeSerializableData());

            if (bindMode) {
                bindService(intent, new DummyConnection(), BIND_AUTO_CREATE);
            } else {
                startService(intent);
            }
        } catch (Exception e) {
            Log.e("Auto Fuzzer", "SerialData test failed for " + serviceName
                    + ": " + e.getClass().getSimpleName());
        }
    }

    // 空连接实现
    private static class DummyConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {}

        @Override
        public void onServiceDisconnected(ComponentName name) {}
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