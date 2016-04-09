package com.futurice.cascade.util;

import android.support.annotation.NonNull;

import com.futurice.cascade.i.nonnull;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * A {@link java.lang.ref.WeakReference} which also returns <code>true</code> as normal, and also if
 * equality tests positive for two AltWeakReference objects pointed to.  just as if it were the object to which it points
 * <p>
 * Created by Paul Houghton on 23-03-2015.
 */
public class AltWeakReference<T> extends WeakReference<T> {

    /**
     * Create a new reference
     *
     * @param r
     */
    public AltWeakReference(@NonNull @nonnull final T r) {
        super(r);
    }

    /**
     * Create a new reference
     *
     * @param r
     * @param q
     */
    public AltWeakReference(
            @NonNull @nonnull final T r,
            @NonNull @nonnull final ReferenceQueue<? super T> q) {
        super(r, q);
    }

    /**
     * Test equality. The definition of equality is different from {@link WeakReference}. Two items
     * are also equal if the <em>items referenced</em> are equal or if one of them is the item referenced.
     *
     * This helps to simplify some logic associated with {@link WeakReference}
     *
     * @param other
     * @return
     */
    @Override // Object
    public boolean equals(Object other) {
        if (super.equals(other)) { //FIXME .equals() and .hashCode() should match responses in all cases
            return true;
        }

        final T t = this.get();
        if (t != null && other != null) {
            if (t.equals(other)) {
                return true;
            }
            if (other instanceof AltWeakReference) {
                return other.equals(t);
            }
        }

        return false;
    }

    @Override // Object
    public int hashCode() {
        final T t = this.get();
        if (t != null) {
            return t.hashCode();
        }

        return super.hashCode();
    }
}
