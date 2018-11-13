package com.yanry.android.test;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import lib.android.view.pop.display.FloatDisplay;

/**
 * rongyu.yan
 * 2018/11/7
 **/
public class DemoFloatDisplay extends FloatDisplay<Integer> {
    private int gravity;

    public DemoFloatDisplay(String tag) {
        switch (tag) {
            case "A":
                gravity = Gravity.BOTTOM | Gravity.LEFT;
                break;
            case "B":
                gravity = Gravity.BOTTOM | Gravity.CENTER;
                break;
            case "C":
                gravity = Gravity.BOTTOM | Gravity.RIGHT;
        }
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
    protected void setData(View instance, Integer data) {
        TextView textView = instance.findViewById(R.id.tv);
        textView.setText(data.toString());
    }

    @Override
    protected boolean accept(Object handlerIndicator) {
        return handlerIndicator.equals(getClass());
    }
}
