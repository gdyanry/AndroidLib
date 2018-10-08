package com.yanry.android.test;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioGroup;

import lib.android.model.AndroidLogHandler;
import lib.android.view.pop.handler.ToastHandler;
import lib.android.view.pop.DataViewHandler;
import lib.android.view.pop.PopDataManager;
import lib.android.view.pop.ShowTask;
import lib.common.model.log.LogLevel;
import lib.common.model.log.Logger;
import lib.common.model.log.SimpleFormatterBuilder;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private int counter;
    private RadioGroup radioGroup;
    private CheckBox cbRejectExpelled;
    private CheckBox cbRejectDismissed;
    private CheckBox cbExpelExistingTask;
    private PopDataManager manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Logger.getDefault().addHandler(new AndroidLogHandler(new SimpleFormatterBuilder().method().sequenceNumber().thread().build(), LogLevel.Verbose));

        radioGroup = findViewById(R.id.rg_strategy);
        cbRejectExpelled = findViewById(R.id.reject_expelled);
        cbRejectDismissed = findViewById(R.id.reject_dismissed);
        cbExpelExistingTask = findViewById(R.id.expel_existing_task);
        DataViewHandler handler = new ToastHandler<>();
        manager = new PopDataManager();
        manager.registerHandler(handler);
        findViewById(R.id.btn_show).setOnClickListener(this);
        findViewById(R.id.btn_builder).setOnClickListener(this);
    }

    @Override
    protected void onDestroy() {
        manager.cancelTasksByContext(this);
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_show:
                ShowTask task = new ShowTask(ToastHandler.class, this, counter++, 3500) {
                    @Override
                    protected int getStrategy() {
                        switch (radioGroup.getCheckedRadioButtonId()) {
                            case R.id.show_immediately:
                                return STRATEGY_SHOW_IMMEDIATELY;
                            case R.id.insert_head:
                                return STRATEGY_INSERT_HEAD;
                        }
                        return 0;
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
                    protected boolean expelWaitingTask(ShowTask task) {
                        return cbExpelExistingTask.isChecked();
                    }
                };
                manager.show(task);
                break;
            case R.id.btn_builder:
                ShowTask.Builder builder = ShowTask.builder();
                switch (radioGroup.getCheckedRadioButtonId()) {
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
                manager.show(builder.duration(3500).type(ToastHandler.class).build(this, counter++));
                break;
        }
    }
}
