package yanry.lib.android.model;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

/**
 * rongyu.yan
 * 2019/5/10
 **/
public class ActionSequence<T> {
    public static final int IRRELEVANT = 0;
    public static final int PENDING = 1;
    public static final int CATCH = 2;

    private boolean resetOnIrrelevantAction;
    private T[] sequence;
    private ArrayList<T> temp;

    public ActionSequence(boolean resetOnIrrelevantAction, T... sequence) {
        this.resetOnIrrelevantAction = resetOnIrrelevantAction;
        this.sequence = sequence;
        temp = new ArrayList<>(sequence.length);
    }

    @TestResult
    public int test(T action) {
        int size = temp.size();
        if (action.equals(sequence[size])) {
            if (size == sequence.length - 1) {
                temp.clear();
                return CATCH;
            }
            temp.add(action);
            return PENDING;
        }
        if (resetOnIrrelevantAction) {
            temp.clear();
        }
        return IRRELEVANT;
    }

    public List<T> getCurrentSequence() {
        return temp;
    }

    public void reset() {
        temp.clear();
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({IRRELEVANT, PENDING, CATCH})
    public @interface TestResult {
    }
}
