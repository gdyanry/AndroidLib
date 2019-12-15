package com.yanry.android.test;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import yanry.lib.android.model.AndroidLogHandler;
import yanry.lib.android.view.pop.ContextShowData;
import yanry.lib.android.view.pop.ToastDisplay;
import yanry.lib.java.model.Singletons;
import yanry.lib.java.model.log.Logger;
import yanry.lib.java.model.log.SimpleFormatter;
import yanry.lib.java.model.schedule.Display;
import yanry.lib.java.model.schedule.Scheduler;
import yanry.lib.java.model.schedule.SchedulerManager;
import yanry.lib.java.model.schedule.ShowData;

import static yanry.lib.java.model.schedule.ShowData.STRATEGY_APPEND_TAIL;
import static yanry.lib.java.model.schedule.ShowData.STRATEGY_INSERT_HEAD;
import static yanry.lib.java.model.schedule.ShowData.STRATEGY_SHOW_IMMEDIATELY;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private int counter;
    private RadioGroup rgStrategy;
    private CheckBox cbRejectExpelled;
    private CheckBox cbRejectDismissed;
    private CheckBox cbExpelExistingTask;
    private CheckBox cbIsValid;
    private RadioGroup rgTag;
    private RadioGroup rgDisplay;

    private SchedulerManager manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Logger.getDefault().addHandler(new AndroidLogHandler(new SimpleFormatter().sequenceNumber().method(3), null, true));
        rgDisplay = findViewById(R.id.rg_display);
        rgStrategy = findViewById(R.id.rg_strategy);
        cbRejectExpelled = findViewById(R.id.reject_expelled);
        cbRejectDismissed = findViewById(R.id.reject_dismissed);
        cbExpelExistingTask = findViewById(R.id.expel_existing_task);
        cbIsValid = findViewById(R.id.is_valid);
        findViewById(R.id.btn_show).setOnClickListener(this);
        rgTag = findViewById(R.id.rg_tag);

        manager = Singletons.get(PopManager.class);
        manager.get("Center").addLink(manager.get("Left"), manager.get("Right"));
        manager.get("Right").addLink(manager.get("Center"));
    }

    @Override
    protected void onDestroy() {
        manager.cancelAll(true);
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        RadioButton rbTag = findViewById(rgTag.getCheckedRadioButtonId());
        String tag = rbTag.getText().toString();
        Class<? extends Display<ContextShowData, ?>> indicator = null;
        switch (rgDisplay.getCheckedRadioButtonId()) {
            case R.id.rb_activity:
                indicator = DemoActivityDisplay.class;
                break;
            case R.id.rb_float:
                switch (tag) {
                    case "Left":
                        indicator = LeftFloatDisplay.class;
                        break;
                    case "Center":
                        indicator = CenterFloatDisplay.class;
                        break;
                    case "Right":
                        indicator = RightFloatDisplay.class;
                        break;
                }
                break;
            case R.id.rb_toast:
                indicator = ToastDisplay.class;
                break;
        }
        Scheduler scheduler = manager.get(tag);
        final int data = counter++;
        switch (v.getId()) {
            case R.id.btn_show:
                ContextShowData showData = new ContextShowData(this);
                switch (rgStrategy.getCheckedRadioButtonId()) {
                    case R.id.append_tail:
                        showData.setStrategy(STRATEGY_APPEND_TAIL);
                        break;
                    case R.id.insert_head:
                        showData.setStrategy(STRATEGY_INSERT_HEAD);
                        break;
                    default:
                        showData.setStrategy(STRATEGY_SHOW_IMMEDIATELY);
                        break;
                }
                if (cbRejectExpelled.isChecked()) {
                    showData.addFlag(ShowData.FLAG_REJECT_EXPELLED);
                }
                if (cbRejectDismissed.isChecked()) {
                    showData.addFlag(ShowData.FLAG_REJECT_DISMISSED);
                }
                if (cbExpelExistingTask.isChecked()) {
                    showData.addFlag(ShowData.FLAG_EXPEL_WAITING_DATA);
                }
                if (cbIsValid.isChecked()) {
                    showData.addFlag(ShowData.FLAG_INVALID_ON_DEQUEUE);
                }
                showData.setExtra(data)
                        .setDuration(5000);
                scheduler.show(showData, indicator);
                break;
        }
    }
}
