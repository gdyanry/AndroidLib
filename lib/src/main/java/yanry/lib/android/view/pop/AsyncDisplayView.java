package yanry.lib.android.view.pop;

/**
 * Created by yanry on 2020/6/29.
 */
public interface AsyncDisplayView<D extends ContextShowData> {
    void bindData(D data);
}
