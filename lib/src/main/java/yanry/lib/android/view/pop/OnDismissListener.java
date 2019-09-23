package yanry.lib.android.view.pop;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface OnDismissListener {
    int DISMISS_TYPE_MANUAL = 1;
    int DISMISS_TYPE_TIMEOUT = 2;
    int DISMISS_TYPE_EXPELLED = 3;
    int DISMISS_TYPE_CANCELLED = 4;
    int DISMISS_TYPE_NOTIFIED = 5;

    void onDismiss(@DismissType int type);

    @IntDef({DISMISS_TYPE_CANCELLED, DISMISS_TYPE_MANUAL, DISMISS_TYPE_NOTIFIED, DISMISS_TYPE_TIMEOUT, DISMISS_TYPE_EXPELLED})
    @Retention(RetentionPolicy.SOURCE)
    @interface DismissType {
    }
}
