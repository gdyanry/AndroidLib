package yanry.lib.android.model;

import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;

import androidx.annotation.Nullable;

import java.util.List;
import java.util.Objects;

import yanry.lib.android.model.runner.UiScheduleRunner;
import yanry.lib.java.model.Singletons;
import yanry.lib.java.model.log.Logger;
import yanry.lib.java.model.watch.ValueHolder;
import yanry.lib.java.model.watch.ValueHolderImpl;

/**
 * Created by yanry on 2020/2/10.
 */
public class RemoteMediaHandler extends MediaController.Callback implements MediaSessionManager.OnActiveSessionsChangedListener {
    private String pkgName;
    private ValueHolderImpl<MediaController> controllerHolder;

    public RemoteMediaHandler(Context context, String pkgName) {
        this.pkgName = pkgName;
        controllerHolder = new ValueHolderImpl<>();
        MediaSessionManager mediaSessionManager = (MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE);
        mediaSessionManager.addOnActiveSessionsChangedListener(this, null);
        onActiveSessionsChanged(mediaSessionManager.getActiveSessions(null));
    }

    public ValueHolder<MediaController> getControllerHolder() {
        return controllerHolder;
    }

    /**
     * 按给定的key序列依次获取对应的元数据值，直到得到不为null的值为止。
     *
     * @param keys {@link MediaMetadata}中以METADATA_KEY_开头的常量。
     * @return
     */
    public String getMetadata(String... keys) {
        MediaController controller = controllerHolder.getValue();
        if (controller != null) {
            MediaMetadata metadata = controller.getMetadata();
            if (metadata != null) {
                for (String key : keys) {
                    String value = metadata.getString(key);
                    if (value != null) {
                        return value;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public final void onActiveSessionsChanged(@Nullable List<MediaController> controllers) {
        MediaController newController = controllerHolder.getValue();
        if (controllers != null) {
            for (MediaController controller : controllers) {
                if (Objects.equals(pkgName, controller.getPackageName())) {
                    controller.registerCallback(this, Singletons.get(UiScheduleRunner.class));
                    newController = controller;
                    break;
                }
            }
        }
        controllerHolder.setValue(newController);
    }

    @Override
    public void onSessionDestroyed() {
        Logger.getDefault().dd("media session destroyed: ", pkgName);
        controllerHolder.setValue(null);
    }
}
