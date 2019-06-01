package yanry.lib.android.model.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;

import yanry.lib.java.model.cache.ResizableLruCache;

/**
 * rongyu.yan
 * 2019/5/17
 **/
public abstract class ComplexItemRecyclerAdapter<T> extends RecyclerAdapter {
    private ResizableLruCache<T, ComplexItem<T, RecyclerViewHolder>> cache;

    public ComplexItemRecyclerAdapter() {
        registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                getCache().resize(getItemCount() + 1);
            }
        });
    }

    private ResizableLruCache<T, ComplexItem<T, RecyclerViewHolder>> getCache() {
        if (cache == null) {
            cache = new ResizableLruCache<T, ComplexItem<T, RecyclerViewHolder>>(getItemCount() + 1) {
                @Override
                protected ComplexItem<T, RecyclerViewHolder> create(T key) {
                    return createComplexItem(key);
                }
            };
        }
        return cache;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewHolder holder, int position) {
        getCache().get(getItem(position)).display(holder);
    }

    protected abstract T getItem(int position);

    protected abstract ComplexItem<T, RecyclerViewHolder> createComplexItem(T itemData);
}
