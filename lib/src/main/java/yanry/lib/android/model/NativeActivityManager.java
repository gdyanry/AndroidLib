package yanry.lib.android.model;

import android.app.Activity;
import android.app.Application;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;

import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import yanry.lib.java.interfaces.BiConsumer;
import yanry.lib.java.model.Registry;
import yanry.lib.java.model.log.Logger;
import yanry.lib.java.model.watch.ValueHolder;
import yanry.lib.java.model.watch.ValueHolderImpl;

/**
 * 当前应用的activity管理器。
 * <p>
 * Created by yanry on 2020/3/1.
 */
public class NativeActivityManager {
    private TopActivityHolder topActivity;
    private CopyOnWriteArrayList<ActivityStateHolder> activityStates;
    private HashMap<Lifecycle.Event, Registry<BiConsumer<Activity, Lifecycle.Event>>> eventDispatcher;

    public NativeActivityManager() {
        topActivity = new TopActivityHolder();
        eventDispatcher = new HashMap<>();
    }

    public void init(Application application) {
        try {
            ActivityInfo[] activities = application.getPackageManager().getPackageInfo(application.getPackageName(), PackageManager.GET_ACTIVITIES).activities;
            int activityCount = activities == null ? 0 : activities.length;
            Logger.getDefault().dd("activity count: ", activityCount);
            activityStates = new CopyOnWriteArrayList<>();
            application.registerActivityLifecycleCallbacks(topActivity);
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
        return getActivityStateHolder(activity);
    }

    private ActivityStateHolder getActivityStateHolder(Activity activity) {
        // 反向遍历，因为后添加的activity状态更容易改变
        for (int i = activityStates.size() - 1; i >= 0; i--) {
            ActivityStateHolder activityStateHolder = activityStates.get(i);
            if (activityStateHolder.activity == activity) {
                return activityStateHolder;
            }
        }
        return null;
    }

    public boolean registerActivityEventListener(Lifecycle.Event event, BiConsumer<Activity, Lifecycle.Event> listener) {
        Registry<BiConsumer<Activity, Lifecycle.Event>> registry = eventDispatcher.get(event);
        if (registry == null) {
            registry = new Registry<>();
            eventDispatcher.put(event, registry);
        }
        return registry.register(listener);
    }

    public boolean unregisterActivityEventListener(Lifecycle.Event event, BiConsumer<Activity, Lifecycle.Event> listener) {
        Registry<BiConsumer<Activity, Lifecycle.Event>> registry = eventDispatcher.get(event);
        return registry != null && registry.unregister(listener);
    }

    private class TopActivityHolder extends ValueHolderImpl<Activity> implements Application.ActivityLifecycleCallbacks {

        private synchronized void handleActivityEvent(Activity activity, Lifecycle.Event event, Lifecycle.State state) {
            ActivityStateHolder activityState = getActivityStateHolder(activity);
            if (activityState == null) {
                activityState = new ActivityStateHolder(activity);
            }
            doDispatchEvent(activity, event);
            doDispatchEvent(activity, Lifecycle.Event.ON_ANY);
            if (activityState.setValue(state) != state) {
                // 刷新topActivity
                Activity currentTop = null;
                Lifecycle.State topState = null;
                for (ActivityStateHolder stateHolder : activityStates) {
                    if (topState == null || stateHolder.getValue().isAtLeast(topState)) {
                        topState = stateHolder.getValue();
                        currentTop = stateHolder.activity;
                    }
                }
                // topActivity状态为DESTROYED时topActivity设为null
                topActivity.setValue(currentTop == activity && state == Lifecycle.State.DESTROYED ? null : currentTop);
            }
        }

        private void doDispatchEvent(Activity activity, Lifecycle.Event event) {
            Registry<BiConsumer<Activity, Lifecycle.Event>> registry = eventDispatcher.get(event);
            if (registry != null) {
                for (BiConsumer<Activity, Lifecycle.Event> listener : registry.getList()) {
                    listener.accept(activity, event);
                }
            }
        }

        @Override
        public final void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            handleActivityEvent(activity, Lifecycle.Event.ON_CREATE, Lifecycle.State.CREATED);
        }

        @Override
        public final void onActivityStarted(Activity activity) {
            handleActivityEvent(activity, Lifecycle.Event.ON_START, Lifecycle.State.STARTED);
        }

        @Override
        public final void onActivityResumed(Activity activity) {
            handleActivityEvent(activity, Lifecycle.Event.ON_RESUME, Lifecycle.State.RESUMED);
        }

        @Override
        public final void onActivityPaused(Activity activity) {
            handleActivityEvent(activity, Lifecycle.Event.ON_PAUSE, Lifecycle.State.STARTED);
        }

        @Override
        public final void onActivityStopped(Activity activity) {
            handleActivityEvent(activity, Lifecycle.Event.ON_STOP, Lifecycle.State.CREATED);
        }

        @Override
        public final void onActivitySaveInstanceState(Activity activity, Bundle outState) {

        }

        @Override
        public final synchronized void onActivityDestroyed(Activity activity) {
            handleActivityEvent(activity, Lifecycle.Event.ON_DESTROY, Lifecycle.State.DESTROYED);
            activityStates.remove(activity);
        }
    }

    private class ActivityStateHolder extends ValueHolderImpl<Lifecycle.State> {
        private Activity activity;

        public ActivityStateHolder(Activity activity) {
            super(Lifecycle.State.INITIALIZED);
            this.activity = activity;
            activityStates.add(this);
        }
    }
}
