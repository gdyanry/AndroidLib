package com.yanry.android.test;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import yanry.lib.android.model.AndroidLogHandler;
import yanry.lib.android.view.pop.Display;
import yanry.lib.android.view.pop.PopScheduler;
import yanry.lib.android.view.pop.ShowData;
import yanry.lib.android.view.pop.display.ToastDisplay;
import yanry.lib.java.model.log.Logger;
import yanry.lib.java.model.log.SimpleFormatter;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private int counter;
    private RadioGroup rgStrategy;
    private CheckBox cbRejectExpelled;
    private CheckBox cbRejectDismissed;
    private CheckBox cbExpelExistingTask;
    private CheckBox cbIsValid;
    private RadioGroup rgTag;
    private RadioGroup rgDisplay;

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
        PopScheduler.get("B").addLink(PopScheduler.get("A"), PopScheduler.get("C"));
        PopScheduler.get("C").addLink(PopScheduler.get("B"));
    }

    @Override
    protected void onDestroy() {
        PopScheduler.cancelAll(true);
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
                    case "A":
                        indicator = LeftFloatDisplay.class;
                        break;
                    case "B":
                        indicator = CenterFloatDisplay.class;
                        break;
                    case "C":
                        indicator = RightFloatDisplay.class;
                        break;
                }
                break;
            case R.id.rb_toast:
                indicator = ToastDisplay.class;
                break;
        }
        PopScheduler scheduler = PopScheduler.get(tag);
        final int data = counter++;
        switch (v.getId()) {
            case R.id.btn_show:
                ShowData showData = new ShowData(this) {
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
                    protected boolean isValid() {
                        return cbIsValid.isChecked();
                    }
                };
                showData.setExtra(data)
                        .setDuration(20000);
                scheduler.show(showData, indicator);
                break;
        }
    }
}
