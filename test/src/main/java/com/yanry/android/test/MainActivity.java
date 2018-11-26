package com.yanry.android.test;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import lib.android.model.AndroidLogHandler;
import lib.android.view.pop.PopScheduler;
import lib.android.view.pop.ShowTask;
import lib.android.view.pop.display.ToastDisplay;
import lib.common.model.log.Logger;
import lib.common.model.log.SimpleFormatter;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private int counter;
    private RadioGroup rgStrategy;
    private CheckBox cbRejectExpelled;
    private CheckBox cbRejectDismissed;
    private CheckBox cbExpelExistingTask;
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
        findViewById(R.id.btn_show).setOnClickListener(this);
        findViewById(R.id.btn_builder).setOnClickListener(this);
        rgTag = findViewById(R.id.rg_tag);
        registerDisplay("A");
        registerDisplay("B");
        PopScheduler.get("B").registerDisplay(DemoActivityDisplay.getInstance());
        registerDisplay("C");
        PopScheduler.get("B").addLink(PopScheduler.get("A"), PopScheduler.get("C"));
        PopScheduler.get("C").addLink(PopScheduler.get("B"));
    }

    private void registerDisplay(String tag) {
        PopScheduler.get(tag).registerDisplay(new DemoFloatDisplay(tag));
    }

    @Override
    protected void onDestroy() {
        PopScheduler.cancelAll(true);
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        Object indicator = null;
        switch (rgDisplay.getCheckedRadioButtonId()) {
            case R.id.rb_activity:
                indicator = DemoActivityDisplay.class;
                break;
            case R.id.rb_float:
                indicator = DemoFloatDisplay.class;
                break;
            case R.id.rb_toast:
                indicator = ToastDisplay.class;
                break;
        }
        RadioButton rbTag = findViewById(rgTag.getCheckedRadioButtonId());
        PopScheduler manager = PopScheduler.get(rbTag.getText().toString());
        final int data = counter++;
        switch (v.getId()) {
            case R.id.btn_show:
                ShowTask task = new ShowTask(indicator, this, data) {
                    @Override
                    protected int getStrategy() {
                        switch (rgStrategy.getCheckedRadioButtonId()) {
                            case R.id.show_immediately:
                                return STRATEGY_SHOW_IMMEDIATELY;
                            case R.id.insert_head:
                                return STRATEGY_INSERT_HEAD;
                        }
                        return STRATEGY_APPEND_TAIL;
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
                    protected boolean expelWaitingTask(ShowTask request) {
                        return cbExpelExistingTask.isChecked();
                    }

                    @Override
                    protected void onShow() {
                        Logger.getDefault().vv(data);
                    }

                    @Override
                    protected void onDismiss(boolean isFromInternal) {
                        Logger.getDefault().vv(data, ' ', isFromInternal);
                    }
                };
                manager.show(task.setTag(this).setDuration(400000));
                break;
            case R.id.btn_builder:
                ShowTask.Builder builder = ShowTask.getBuilder().displayIndicator(indicator)
                        .onShow(request -> Logger.getDefault().vv("onShow: ", data))
                        .onDismiss(isInternal -> Logger.getDefault().vv("onDismiss: ", data, ", is internal: ", isInternal));
                switch (rgStrategy.getCheckedRadioButtonId()) {
                    case R.id.show_immediately:
                        builder.showImmediately();
                        break;
                    case R.id.insert_head:
                        builder.insertHead();
                        break;
                }
                if (cbRejectExpelled.isChecked()) {
                    builder.rejectExpelled();
                }
                if (cbRejectDismissed.isChecked()) {
                    builder.rejectDismissed();
                }
                if (cbExpelExistingTask.isChecked()) {
                    builder.expelWaitingTasks();
                }
                manager.show(builder.build(this, data).setDuration(4000));
                break;
        }
    }
}
