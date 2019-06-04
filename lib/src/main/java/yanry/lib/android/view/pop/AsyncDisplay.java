package yanry.lib.android.view.pop;

import android.content.Context;

import yanry.lib.android.interfaces.Function;

/**
 * 对应的View需要在创建时调用{@link #notifyCreate(Function)}，并在销毁时调用{@link #notifyDismiss(V)}。
 * rongyu.yan
 * 2018/11/13
 **/
public abstract class AsyncDisplay<D extends ShowData, V> extends Display<D, V> {
    private D data;
    private Function<D, V> async;

    public void notifyCreate(Function<D, V> function) {
        this.async = function;
        if (data != null) {
            setPopInstance(function.apply(data));
            data = null;
        }
    }

    @Override
    protected void show(Context context, D data) {
        this.data = data;
        if (getPopInstance() == null) {
            show(context);
        } else {
            async.apply(data);
        }
    }

    @Override
    public boolean notifyDismiss(V popInstance) {
        if (super.notifyDismiss(popInstance)) {
            async = null;
            return true;
        }
        return false;
    }

    @Override
    protected void internalDismiss() {
        super.internalDismiss();
        async = null;
    }

    protected abstract void show(Context context);
}
