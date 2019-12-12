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
        Class<? extends Display> indicator = null;
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
                ContextShowData showData = new ContextShowData(this) {
                    @Override
                    protected int getStrategy() {
                        switch (rgStrategy.getCheckedRadioButtonId()) {
                            case R.id.append_tail:
                                return STRATEGY_APPEND_TAIL;
                            case R.id.insert_head:
                                return STRATEGY_INSERT_HEAD;
                            default:
                                return STRATEGY_SHOW_IMMEDIATELY;
                        }
                    }

                    @Override
                    protected boolean rejectExpelled() {
                        return cbRejectExpelled.isChecked();
                    }

                    @Override
                    protected boolean rejectDismissed() {
                        return cbRejectDismissed.isChecked();
                    }

                    @Override
                    protected boolean expelWaitingTask(ShowData request) {
                        return cbExpelExistingTask.isChecked();
                    }

                    @Override
                    protected boolean isValidOnDequeue() {
                        return cbIsValid.isChecked();
                    }
                };
                showData.setExtra(data)
                        .setDuration(5000);
                scheduler.show(showData, indicator);
                break;
        }
    }
}
