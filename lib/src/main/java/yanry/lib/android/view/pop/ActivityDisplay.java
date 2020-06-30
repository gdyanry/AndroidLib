package yanry.lib.android.view.pop;

import android.app.Activity;
import android.content.Intent;

import androidx.lifecycle.Lifecycle;

import java.util.concurrent.atomic.AtomicInteger;

import yanry.lib.android.model.NativeActivityManager;
import yanry.lib.java.interfaces.BiConsumer;
import yanry.lib.java.model.Singletons;
import yanry.lib.java.model.schedule.ShowData;
import yanry.lib.java.model.schedule.ViewDisplay;
import yanry.lib.java.model.watch.ValueHolder;
import yanry.lib.java.model.watch.ValueWatcher;

/**
 * 以Activity作为Display中view的载体。用法如下：
 * 1、作为view的Activity实现{@link AsyncDisplayView}接口。
 * 2、添加相应泛型类型的ActivityDisplay子类。
 * 3、调用{@link yanry.lib.java.model.schedule.Scheduler#show(ShowData, Class)}显示数据。
 * <p>
 * rongyu.yan
 * 2018/11/13
 **/
public abstract class ActivityDisplay<D extends ContextShowData, A extends Activity & AsyncDisplayView<D>> extends ViewDisplay<D, A> implements BiConsumer<Activity, Lifecycle.Event>, ValueWatcher<Lifecycle.State> {
    private static final String EXTRA_ID = "yanry.lib.scheduler.display.id";
    private final int id;
    private StartActivityMark startActivityMark;

    public ActivityDisplay() {
        id = Singletons.get(AtomicInteger.class).getAndIncrement();
    }

    /**
     * @param data
     * @return 用于启动activity的Intent对象。
     */
    protected abstract Intent getIntent(D data);

    /**
     * 使用给定Intent启动Activity。
     *
     * @param intent
     * @param data
     */
    protected abstract void startActivity(Intent intent, D data);

    @Override
    protected void show(D data) {
        A activity = getShowingView().getValue();
        if (activity == null) {
            if (startActivityMark != null) {
                startActivityMark.release();
            }
            Singletons.get(NativeActivityManager.class).registerActivityEventListener(Lifecycle.Event.ON_CREATE, this);
            startActivityMark = new StartActivityMark(data);
            Intent intent = getIntent(data);
            intent.putExtra(EXTRA_ID, id);
            startActivity(intent, data);
        } else {
            activity.bindData(data);
        }
    }

    @Override
    protected void dismiss(A view) {
        view.finish();
    }

    @Override
    protected boolean isShowing(A view) {
        ValueHolder<Lifecycle.State> activityState = Singletons.get(NativeActivityManager.class).getActivityState(view);
        return activityState != null && activityState.getValue() != Lifecycle.State.DESTROYED;
    }

    @Override
    public void accept(Activity activity, Lifecycle.Event event) {
        switch (event) {
            case ON_CREATE:
                // 确保activity是由当前display启动的
                if (activity instanceof AsyncDisplayView && getShowingView().getValue() == null && activity.getIntent().getIntExtra(EXTRA_ID, -1) == id) {
                    // 确保ShowData状态正常
                    if (startActivityMark != null) {
                        if (startActivityMark.data == getShowingData() && startActivityMark.data.getState().getValue() == ShowData.STATE_SHOWING) {
                            NativeActivityManager nativeActivityManager = Singletons.get(NativeActivityManager.class);
                            nativeActivityManager.unregisterActivityEventListener(Lifecycle.Event.ON_CREATE, this);
                            nativeActivityManager.registerActivityEventListener(Lifecycle.Event.ON_DESTROY, this);
                            setView((A) activity);
                            // 此时activity.onCreate()还未回调，故添加activity状态监听，在状态变为STARTED时调用activity.bindData()
                            nativeActivityManager.getActivityState(activity).addWatcher(this);
                            break;
                        }
                        startActivityMark.release();
                    }
                    activity.finish();
                }
                break;
            case ON_DESTROY:
                if (activity == getShowingView().getValue()) {
                    Singletons.get(NativeActivityManager.class).unregisterActivityEventListener(Lifecycle.Event.ON_DESTROY, this);
                    notifyDismiss((A) activity);
                }
                break;
        }
    }

    @Override
    public void onValueChange(Lifecycle.State to, Lifecycle.State from) {
        if (to == Lifecycle.State.STARTED && from == Lifecycle.State.CREATED) {
            D showingData = getShowingData();
            A activity = getShowingView().getValue();
            if (showingData != null && activity != null) {
                Integer dataState = showingData.getState().getValue();
                if (dataState == ShowData.STATE_SHOWING) {
                    activity.bindData(showingData);
                } else if (dataState == ShowData.STATE_DISMISS) {
                    dismiss(activity);
                }
            }
            Singletons.get(NativeActivityManager.class).getActivityState(activity).removeWatcher(this);
        }
    }

    private class StartActivityMark implements ValueWatcher<Integer> {
        private D data;

        private StartActivityMark(D data) {
            this.data = data;
            data.getState().addWatcher(this);
        }

        void release() {
            if (startActivityMark == this) {
                startActivityMark = null;
            }
            data.getState().removeWatcher(this);
        }

        @Override
        public void onValueChange(Integer to, Integer from) {
            if (to == ShowData.STATE_DISMISS) {
                release();
            }
        }
    }
}
