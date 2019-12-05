package yanry.lib.android.view.pop;

import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface OnDismissListener {
    String DISMISS_TYPE_MANUAL = "MANUAL";
    String DISMISS_TYPE_TIMEOUT = "TIMEOUT";
    String DISMISS_TYPE_EXPELLED = "EXPELLED";
    String DISMISS_TYPE_CANCELLED = "CANCELLED";
    String DISMISS_TYPE_NOTIFIED = "NOTIFIED";

    void onDismiss(@DismissType String type);

    @StringDef({DISMISS_TYPE_CANCELLED, DISMISS_TYPE_MANUAL, DISMISS_TYPE_NOTIFIED, DISMISS_TYPE_TIMEOUT, DISMISS_TYPE_EXPELLED})
    @Retention(RetentionPolicy.SOURCE)
    @interface DismissType {
    }
}
