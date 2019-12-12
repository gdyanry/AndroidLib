package com.yanry.android.test;

import android.content.Intent;

import yanry.lib.android.view.pop.ActivityDisplay;
import yanry.lib.android.view.pop.ContextShowData;

/**
 * rongyu.yan
 * 2018/11/13
 **/
public class DemoActivityDisplay extends ActivityDisplay<ContextShowData> {

    @Override
    protected void showView(ContextShowData data) {
        data.getContext().startActivity(new Intent(data.getContext(), DisplayActivity.class));
    }
}
