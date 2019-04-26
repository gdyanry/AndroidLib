package com.yanry.android.test;

import android.content.Context;
import android.content.Intent;

import yanry.lib.android.view.pop.display.ActivityDisplay;

/**
 * rongyu.yan
 * 2018/11/13
 **/
public class DemoActivityDisplay extends ActivityDisplay<Integer> {
    @Override
    protected void show(Context context) {
        context.startActivity(new Intent(context, DisplayActivity.class));
    }
}
