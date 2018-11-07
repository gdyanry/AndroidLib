package com.yanry.android.test;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import lib.android.model.AndroidLogHandler;
import lib.android.view.pop.PopScheduler;
import lib.android.view.pop.ShowRequest;
import lib.common.model.log.Logger;
import lib.common.model.log.SimpleFormatter;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private int counter;
    private RadioGroup rgStrategy;
    private CheckBox cbRejectExpelled;
    private CheckBox cbRejectDismissed;
    private CheckBox cbExpelExistingTask;
    private RadioGroup rgTag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Logger.getDefault().addHandler(new AndroidLogHandler(new SimpleFormatter().method(3), null, true));

        rgStrategy = findViewById(R.id.rg_strategy);
        cbRejectExpelled = findViewById(R.id.reject_expelled);
        cbRejectDismissed = findViewById(R.id.reject_dismissed);
        cbExpelExistingTask = findViewById(R.id.expel_existing_task);
        findViewById(R.id.btn_show).setOnClickListener(this);
        findViewById(R.id.btn_builder).setOnClickListener(this);
        rgTag = findViewById(R.id.rg_tag);
        registerDisplay("A");
        registerDisplay("B");
        registerDisplay("C");
        PopScheduler.link(PopScheduler.get("A"), PopScheduler.get("B"));
        PopScheduler.link(PopScheduler.get("C"), PopScheduler.get("B"));
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
        RadioButton rbTag = findViewById(rgTag.getCheckedRadioButtonId());
        PopScheduler manager = PopScheduler.get(rbTag.getText().toString());
        final int data = counter++;
        switch (v.getId()) {
            case R.id.btn_show:
                ShowRequest task = new ShowRequest(data, this, data) {
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
                    protected boolean expelWaitingTask(ShowRequest request) {
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
                manager.show(task.setTag(this).setDuration(4000));
                break;
            case R.id.btn_builder:
                ShowRequest.Builder builder = ShowRequest.getBuilder().onShow(request -> Logger.getDefault().vv("onShow: ", data))
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
