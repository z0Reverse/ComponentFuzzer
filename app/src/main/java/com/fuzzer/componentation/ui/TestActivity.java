package com.fuzzer.componentation.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.fuzzer.componentation.R;
import com.fuzzer.componentation.excute.ActivityComponentTest;
import com.fuzzer.componentation.excute.BroadcastComponentTest;
import com.fuzzer.componentation.excute.ServiceComponentTest;

public class TestActivity extends AppCompatActivity {
    private Button autoButton;
    private Button handButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    //
    protected void init(){
        setContentView(R.layout.activity_test);
        autoButton=findViewById(R.id.autoFuzzer);
        handButton=findViewById(R.id.ByHand);
        excute();

    }

    protected void excute(){
        String packageName= getIntent().getStringExtra("packageName");
        autoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("Auto Fuzzer", "begin to excute activity: ");
                Intent intent=new Intent(TestActivity.this, ActivityComponentTest.class);
                intent.putExtra("packageName",packageName);
                intent.putExtra("isExcute",true);
                startActivity(intent);

                Log.d("Auto Fuzzer", "begin to excute broadcast: ");
                Intent intent2=new Intent(TestActivity.this, BroadcastComponentTest.class);
                intent2.putExtra("packageName",packageName);
                intent2.putExtra("isExcute",true);
                startActivity(intent2);

                Log.d("Auto Fuzzer", "begin to excute activity: ");
                Intent intent3=new Intent(TestActivity.this, ServiceComponentTest.class);
                intent3.putExtra("packageName",packageName);
                intent3.putExtra("isExcute",true);
                startActivity(intent3);



            }
        });

        handButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


            }
        });
    }

}