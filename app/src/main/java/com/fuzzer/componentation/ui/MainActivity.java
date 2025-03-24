package com.fuzzer.componentation.ui;


import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.fuzzer.componentation.R;

import java.util.ArrayList;

public class MainActivity extends Activity {


    private static final int REQUEST_PERMISSIONS = 1;
    private Button submitButton;
    private String inputStream;
    private static final String TAG = "MainActivity";
    private TextView displayText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }
    protected void init(){
        //加载页面
        setContentView(R.layout.activity_main);
        displayText = findViewById(R.id.display_text);
        submitButton = findViewById(R.id.submit_button);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //直接采用string类来接受数据
                EditText editText = findViewById(R.id.package_name_input);
                inputStream = editText.getText().toString().trim();
                if(inputStream.isEmpty()){
                    Toast.makeText(MainActivity.this, "请输入包名", Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.d(TAG, "onClick: Package name is: " + inputStream);
                //如果包名非空，申请权限检测内存空间哎
                checkAndRequestPermissions(MainActivity.this);

                //获取权限之后进行检测是否存在包名
                boolean isInstalled = isPackageInstalled(inputStream);
                Log.d(TAG, "onClick: isPackageInstalled result: " + isInstalled);
                if (isInstalled) {
                    Intent intent = new Intent(MainActivity.this, TestActivity.class);
                    intent.putExtra("packageName", inputStream);
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "Package not found: " + inputStream, Toast.LENGTH_LONG).show();
                    Log.d(TAG, "onClick: Package name not found: " + inputStream);
                }
            }
        });
    }

    //检测应用是否存在
    protected boolean isPackageInstalled(String inputStream){
        //boolean result;
        PackageManager packageManager=getPackageManager();
        try {
            PackageInfo packageInfo=packageManager.getPackageInfo(inputStream,0);
            Log.d(TAG, "isPackageInstalled: ture");
            return true;
        }catch (PackageManager.NameNotFoundException e){
            Log.d(TAG, "isPackageInstalled: Package Not Found");
        }
        return false;
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            if (!allPermissionsGranted) {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            } else {
                Log.d(TAG, "All requested permissions granted.");
            }
        }
    }

    public static void checkAndRequestPermissions(Activity activity) {
        String[] permissions = {
                Manifest.permission.MANAGE_EXTERNAL_STORAGE, // Use for managing all files
                Manifest.permission.QUERY_ALL_PACKAGES     // Use if querying all packages is necessary
        };

        // Find out which permissions need to be requested
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        // Request permissions
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(activity,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS);
        }
    }


}