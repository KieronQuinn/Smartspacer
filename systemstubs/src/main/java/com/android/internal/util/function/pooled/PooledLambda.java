package com.android.internal.util.function.pooled;

import android.os.Message;

import com.android.internal.util.function.TriConsumer;

import java.util.function.BiConsumer;

public interface PooledLambda {

    /**
     * Factory of {@link Message}s that contain an
     * ({@link PooledLambda#recycleOnUse auto-recycling}) {@link PooledRunnable} as its
     * {@link Message#getCallback internal callback}.
     *
     * The callback is equivalent to one obtainable via
     * {@link #obtainRunnable(TriConsumer, Object, Object, Object)}
     *
     * Note that using this method with {@link android.os.Handler#handleMessage}
     * is more efficient than the alternative of {@link android.os.Handler#post}
     * with a {@link PooledRunnable} due to the lack of 2 separate synchronization points
     * when obtaining {@link Message} and {@link PooledRunnable} from pools separately
     *
     * You may optionally set a {@link Message#what} for the message if you want to be
     * able to cancel it via {@link android.os.Handler#removeMessages}, but otherwise
     * there's no need to do so
     *
     * @param function non-capturing lambda(typically an unbounded method reference)
     *                 to be invoked on call
     * @param arg1 parameter supplied to {@code function} on call
     * @param arg2 parameter supplied to {@code function} on call
     * @param arg3 parameter supplied to {@code function} on call
     * @return a {@link Message} invoking {@code function(arg1, arg2, arg3) } when handled
     */
    static <A, B, C> Message obtainMessage(
            TriConsumer<? super A, ? super B, ? super C> function,
            A arg1, B arg2, C arg3) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Factory of {@link Message}s that contain an
     * ({@link PooledLambda#recycleOnUse auto-recycling}) {@link PooledRunnable} as its
     * {@link Message#getCallback internal callback}.
     *
     * The callback is equivalent to one obtainable via
     * {@link #obtainRunnable(BiConsumer, Object, Object)}
     *
     * Note that using this method with {@link android.os.Handler#handleMessage}
     * is more efficient than the alternative of {@link android.os.Handler#post}
     * with a {@link PooledRunnable} due to the lack of 2 separate synchronization points
     * when obtaining {@link Message} and {@link PooledRunnable} from pools separately
     *
     * You may optionally set a {@link Message#what} for the message if you want to be
     * able to cancel it via {@link android.os.Handler#removeMessages}, but otherwise
     * there's no need to do so
     *
     * @param function non-capturing lambda(typically an unbounded method reference)
     *                 to be invoked on call
     * @param arg1 parameter supplied to {@code function} on call
     * @param arg2 parameter supplied to {@code function} on call
     * @return a {@link Message} invoking {@code function(arg1, arg2) } when handled
     */
    static <A, B> Message obtainMessage(
            BiConsumer<? super A, ? super B> function,
            A arg1, B arg2) {
        throw new RuntimeException("Stub!");
    }

}
