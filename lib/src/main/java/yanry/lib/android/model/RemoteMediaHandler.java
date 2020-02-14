package yanry.lib.android.model;

import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;

import androidx.annotation.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import yanry.lib.android.entity.MainHandler;
import yanry.lib.java.interfaces.OnValueChangeListener;
import yanry.lib.java.model.Singletons;
import yanry.lib.java.model.log.Logger;

/**
 * Created by yanry on 2020/2/10.
 */
public class RemoteMediaHandler extends MediaController.Callback implements MediaSessionManager.OnActiveSessionsChangedListener {
    private String pkgName;
    private MediaController controller;
    private LinkedList<OnValueChangeListener<MediaController>> sessionChangeListeners;

    public RemoteMediaHandler(Context context, String pkgName) {
        this.pkgName = pkgName;
        sessionChangeListeners = new LinkedList<>();
        MediaSessionManager mediaSessionManager = (MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE);
        mediaSessionManager.addOnActiveSessionsChangedListener(this, null);
        onActiveSessionsChanged(mediaSessionManager.getActiveSessions(null));
    }

    public MediaController getController() {
        return controller;
    }

    /**
     * 按给定的key序列依次获取对应的元数据值，直到得到不为null的值为止。
     *
     * @param keys {@link MediaMetadata}中以METADATA_KEY_开头的常量。
     * @return
     */
    public String getMetadata(String... keys) {
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

    public void addOnSessionChangedListener(OnValueChangeListener<MediaController> listener) {
        sessionChangeListeners.add(listener);
    }

    @Override
    public final void onActiveSessionsChanged(@Nullable List<MediaController> controllers) {
        MediaController old = this.controller;
        if (controllers != null) {
            for (MediaController controller : controllers) {
                if (Objects.equals(pkgName, controller.getPackageName())) {
                    controller.registerCallback(this, Singletons.get(MainHandler.class));
                    this.controller = controller;
                    break;
                }
            }
        }
        if (!Objects.equals(old, controller) && sessionChangeListeners != null) {
            for (OnValueChangeListener listener : sessionChangeListeners) {
                listener.onValueChange(controller, old);
            }
        }
    }

    @Override
    public void onSessionDestroyed() {
        Logger.getDefault().dd("media session destroyed: ", pkgName);
        MediaController old = controller;
        controller = null;
        for (OnValueChangeListener<MediaController> listener : sessionChangeListeners) {
            listener.onValueChange(null, old);
        }
    }
}
