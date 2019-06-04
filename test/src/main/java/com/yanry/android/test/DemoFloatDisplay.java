package com.yanry.android.test;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import yanry.lib.android.view.pop.ShowData;
import yanry.lib.android.view.pop.display.FloatDisplay;

/**
 * rongyu.yan
 * 2018/11/7
 **/
public class DemoFloatDisplay extends FloatDisplay<ShowData> {
    private int gravity;

    public DemoFloatDisplay(int gravity) {
        this.gravity = gravity;
    }

    @Override
    protected WindowManager.LayoutParams getLayoutParams() {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        params.gravity = gravity;
        params.width = 300;
        params.height = 300;
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        params.type = WindowManager.LayoutParams.TYPE_TOAST;
        params.format = PixelFormat.RGBA_8888;
        return params;
    }

    @Override
    protected View getContentView(Context context) {
        return LayoutInflater.from(context).inflate(R.layout.window_float_demo, null);
    }

    @Override
    protected void setData(View instance, ShowData data) {
        TextView textView = instance.findViewById(R.id.tv);
        textView.setText(data.toString());
    }
}
