package com.yanry.android.test;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import lib.android.interfaces.Function;

public class DisplayActivity extends AppCompatActivity implements Function<Integer, Activity> {

    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);
        textView = findViewById(R.id.tv);
        DemoActivityDisplay.getInstance().async(this);
    }

    @Override
    protected void onDestroy() {
        DemoActivityDisplay.getInstance().notifyDismiss();
        super.onDestroy();
    }

    @Override
    public Activity apply(Integer integer) {
        textView.setText(String.valueOf(integer));
        return this;
    }
}
