package com.yanry.android.test;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import yanry.lib.android.view.pop.ContextShowData;
import yanry.lib.java.model.Singletons;
import yanry.lib.java.model.schedule.AsyncBridge;

public class DisplayActivity extends AppCompatActivity implements AsyncBridge<ContextShowData, Activity> {

    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);
        textView = findViewById(R.id.tv);
        for (String tag : new String[]{"Left", "Center", "Right"}) {
            Singletons.get(PopManager.class).get(tag).getDisplay(DemoActivityDisplay.class).notifyCreate(this);
        }
    }

    @Override
    protected void onDestroy() {
        for (String tag : new String[]{"Left", "Center", "Right"}) {
            Singletons.get(PopManager.class).get(tag).getDisplay(DemoActivityDisplay.class).notifyDismiss(this);
        }
        super.onDestroy();
    }

    @Override
    public Activity show(ContextShowData data) {
        textView.setText(String.valueOf(data));
        return this;
    }
}
