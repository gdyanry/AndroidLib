package com.yanry.android.test;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MyLog.get().d("on create.");
        runOnUiThread(() -> System.out.println("hehe"));
    }
}
