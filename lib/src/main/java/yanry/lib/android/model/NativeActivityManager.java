package yanry.lib.android.model;

import android.app.Activity;
import android.app.Application;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;

import java.util.LinkedHashMap;
import java.util.Map;

import yanry.lib.java.model.log.Logger;
import yanry.lib.java.model.watch.ValueHolder;
import yanry.lib.java.model.watch.ValueHolderImpl;

/**
 * 当前应用的activity管理器。
 * <p>
 * Created by yanry on 2020/3/1.
 */
public class NativeActivityManager implements Application.ActivityLifecycleCallbacks {
    private ValueHolderImpl<Activity> topActivity;
    private LinkedHashMap<Activity, ValueHolderImpl<Lifecycle.State>> activityStates;

    public NativeActivityManager() {
        topActivity = new ValueHolderImpl<>();
    }

    public void init(Application application) {
        try {
            ActivityInfo[] activities = application.getPackageManager().getPackageInfo(application.getPackageName(), PackageManager.GET_ACTIVITIES).activities;
            int activityCount = activities == null ? 0 : activities.length;
            Logger.getDefault().dd("activity count: ", activityCount);
            activityStates = new LinkedHashMap<>(activityCount, 0.75f, true);
            application.registerActivityLifecycleCallbacks(this);
        } catch (PackageManager.NameNotFoundException e) {
            Logger.getDefault().catches(e);
        }
    }

    @NonNull
    public ValueHolder<Activity> getTopActivity() {
        return topActivity;
    }

    /**
     * 查询监听指定activity状态。
     *
     * @param activity
     * @return 当activity已经销毁时返回null。
     */
    @Nullable
    public ValueHolder<Lifecycle.State> getActivityState(Activity activity) {
        return activityStates.get(activity);
    }

    /**
     * @return 顶部activity是否发生变化
     */
    private boolean refreshTopActivity() {
        Activity currentTop = null;
        Lifecycle.State topState = null;
        for (Map.Entry<Activity, ValueHolderImpl<Lifecycle.State>> entry : activityStates.entrySet()) {
            ValueHolder<Lifecycle.State> state = entry.getValue();
            if (topState == null || state.getValue().isAtLeast(topState)) {
                topState = state.getValue();
                currentTop = entry.getKey();
            }
        }
        return topActivity.setValue(currentTop) != currentTop;
    }

    private void handleActivityState(Activity activity, Lifecycle.State state) {
        if (activityStates != null) {
            ValueHolderImpl<Lifecycle.State> activityState = activityStates.get(activity);
            if (activityState == null) {
                activityState = new ValueHolderImpl<>(Lifecycle.State.INITIALIZED);
                activityStates.put(activity, activityState);
            }
            if (activityState.setValue(state) != state && refreshTopActivity() && topActivity.getValue() == activity && state == Lifecycle.State.DESTROYED) {
                // topActivity状态为DESTROYED时topActivity设为null
                topActivity.setValue(null);
            }
        }
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        handleActivityState(activity, Lifecycle.State.CREATED);
    }

    @Override
    public void onActivityStarted(Activity activity) {
        handleActivityState(activity, Lifecycle.State.STARTED);
    }

    @Override
    public void onActivityResumed(Activity activity) {
        handleActivityState(activity, Lifecycle.State.RESUMED);
    }

    @Override
    public void onActivityPaused(Activity activity) {
        handleActivityState(activity, Lifecycle.State.STARTED);
    }

    @Override
    public void onActivityStopped(Activity activity) {
        handleActivityState(activity, Lifecycle.State.CREATED);
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        handleActivityState(activity, Lifecycle.State.DESTROYED);
        activityStates.remove(activity);
    }
}
