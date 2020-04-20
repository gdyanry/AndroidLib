package yanry.lib.android.model;

import android.app.Activity;
import android.app.Application;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.lifecycle.Lifecycle;

import java.util.LinkedHashMap;
import java.util.Map;

import yanry.lib.java.model.log.Logger;
import yanry.lib.java.model.watch.ValueHolder;
import yanry.lib.java.model.watch.ValueHolderImpl;
import yanry.lib.java.model.watch.ValueWatcher;

/**
 * Created by yanry on 2020/3/1.
 */
public class TopActivityHolder extends ValueHolderImpl<Activity> implements Application.ActivityLifecycleCallbacks {
    private LinkedHashMap<Activity, ValueHolderImpl<Lifecycle.State>> activityStates;

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

    public boolean addActivityStateWatcher(Activity activity, ValueWatcher<Lifecycle.State> watcher) {
        ValueHolder<Lifecycle.State> activityState = activityStates.get(activity);
        return activityState != null && activityState.addWatcher(watcher);
    }

    public boolean removeActivityStateWatcher(Activity activity, ValueWatcher<Lifecycle.State> watcher) {
        ValueHolder<Lifecycle.State> activityState = activityStates.get(activity);
        return activityState != null && activityState.removeWatcher(watcher);
    }

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
        return setValue(currentTop);
    }

    private void handleActivityState(Activity activity, Lifecycle.State state) {
        if (activityStates != null) {
            ValueHolderImpl<Lifecycle.State> activityState = activityStates.get(activity);
            if (activityState == null) {
                activityState = new ValueHolderImpl<>(Lifecycle.State.INITIALIZED);
                activityStates.put(activity, activityState);
            }
            if (activityState.setValue(state) && refreshTopActivity() && getValue() == activity && state == Lifecycle.State.DESTROYED) {
                setValue(null);
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
