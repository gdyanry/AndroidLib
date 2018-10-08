package lib.android.interfaces;

public interface Filter<T> {
    boolean accept(T target);
}
