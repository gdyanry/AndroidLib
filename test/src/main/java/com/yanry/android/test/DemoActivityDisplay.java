package com.yanry.android.test;

import android.content.Context;
import android.content.Intent;

import yanry.lib.android.view.pop.display.ActivityDisplay;

/**
 * rongyu.yan
 * 2018/11/13
 **/
public class DemoActivityDisplay extends ActivityDisplay<Integer> {
    private static DemoActivityDisplay instance = new DemoActivityDisplay();

    public static DemoActivityDisplay getInstance() {
        return instance;
    }

    @Override
    protected void show(Context context) {
        context.startActivity(new Intent(context, DisplayActivity.class));
    }

    @Override
    protected boolean accept(Object handlerIndicator) {
        return handlerIndicator.equals(getClass());
    }
}
