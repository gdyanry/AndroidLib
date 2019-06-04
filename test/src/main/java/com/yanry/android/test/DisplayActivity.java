package com.yanry.android.test;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import yanry.lib.android.interfaces.Function;
import yanry.lib.android.view.pop.PopScheduler;
import yanry.lib.android.view.pop.ShowData;

public class DisplayActivity extends AppCompatActivity implements Function<ShowData, Activity> {

    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);
        textView = findViewById(R.id.tv);
        for (String tag : new String[]{"A", "B", "C"}) {
            PopScheduler.get(tag).getDisplay(DemoActivityDisplay.class).notifyCreate(this);
        }
    }

    @Override
    protected void onDestroy() {
        for (String tag : new String[]{"A", "B", "C"}) {
            PopScheduler.get(tag).getDisplay(DemoActivityDisplay.class).notifyDismiss(this);
        }
        super.onDestroy();
    }

    @Override
    public Activity apply(ShowData integer) {
        textView.setText(String.valueOf(integer));
        return this;
    }
}
