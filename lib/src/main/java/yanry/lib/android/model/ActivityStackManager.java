package yanry.lib.android.model;

import android.app.Activity;
import android.app.Application;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.lifecycle.Lifecycle;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import yanry.lib.java.interfaces.OnValueChangeListener;
import yanry.lib.java.model.log.LogLevel;
import yanry.lib.java.model.log.Logger;

/**
 * Created by yanry on 2020/3/1.
 */
public class ActivityStackManager implements Application.ActivityLifecycleCallbacks {
    private Activity topActivity;
    private LinkedList<OnValueChangeListener<Activity>> topActivityChangeListeners;
    private LinkedHashMap<Activity, ActivityState> activityStates;

    public ActivityStackManager() {
        topActivityChangeListeners = new LinkedList<>();
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

    public Activity getTopActivity() {
        return topActivity;
    }

    public void addOnTopActivityChangeListener(OnValueChangeListener<Activity> listener) {
        topActivityChangeListeners.add(listener);
    }

    public void removeOnTopActivityChangeListener(OnValueChangeListener<Activity> listener) {
        topActivityChangeListeners.remove(listener);
    }

    public boolean addOnActivityStateChangeListener(Activity activity, OnValueChangeListener<Lifecycle.State> listener) {
        ActivityState activityState = activityStates.get(activity);
        if (activityState != null) {
            activityState.add(listener);
            return true;
        }
        return false;
    }

    public boolean removeOnActivityStateChangeListener(Activity activity, OnValueChangeListener<Lifecycle.State> listener) {
        ActivityState activityState = activityStates.get(activity);
        if (activityState != null) {
            activityState.remove(listener);
            return true;
        }
        return false;
    }

    private void refreshTopActivity(int encapsulationLayerCount) {
        Activity currentTop = null;
        Lifecycle.State topState = null;
        for (Map.Entry<Activity, ActivityState> entry : activityStates.entrySet()) {
            ActivityState state = entry.getValue();
            if (topState == null || state.currentState.isAtLeast(topState)) {
                topState = state.currentState;
                currentTop = entry.getKey();
            }
        }
        dispatchTopActivityChangeEvent(++encapsulationLayerCount, currentTop);
    }

    private void dispatchTopActivityChangeEvent(int encapsulationLayerCount, Activity currentTop) {
        if (topActivity != currentTop) {
            Activity old = topActivity;
            topActivity = currentTop;
            Logger.getDefault().concat(++encapsulationLayerCount, LogLevel.Debug, "top activity: ", currentTop);
            for (OnValueChangeListener<Activity> listener : topActivityChangeListeners) {
                listener.onValueChange(currentTop, old);
            }
        }
    }

    private void handleActivityState(Activity activity, Lifecycle.State state) {
        if (activityStates != null) {
            ActivityState activityState = activityStates.get(activity);
            if (activityState == null) {
                activityState = new ActivityState();
                activityStates.put(activity, activityState);
            }
            Lifecycle.State oldState = activityState.currentState;
            if (oldState != state) {
                activityState.currentState = state;
                refreshTopActivity(1);
                for (OnValueChangeListener<Lifecycle.State> listener : activityState) {
                    listener.onValueChange(state, oldState);
                }
                if (topActivity == activity && state == Lifecycle.State.DESTROYED) {
                    activityStates.remove(activity);
                    dispatchTopActivityChangeEvent(1, null);
                }
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

    private class ActivityState extends LinkedList<OnValueChangeListener<Lifecycle.State>> {
        private Lifecycle.State currentState = Lifecycle.State.INITIALIZED;
    }
}
