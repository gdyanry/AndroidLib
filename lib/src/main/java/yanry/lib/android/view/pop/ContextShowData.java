package yanry.lib.android.view.pop;

import android.content.Context;

import yanry.lib.java.model.schedule.ShowData;

/**
 * Created by yanry on 2019/12/11.
 */
public class ContextShowData extends ShowData {
    private Context context;

    public ContextShowData(Context context) {
        this.context = context;
    }

    public Context getContext() {
        return context;
    }
}
