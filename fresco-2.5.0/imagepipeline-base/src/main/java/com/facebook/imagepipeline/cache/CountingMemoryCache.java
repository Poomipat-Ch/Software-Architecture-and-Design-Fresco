/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.cache;

import android.graphics.Bitmap;
import androidx.annotation.VisibleForTesting;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.memory.MemoryTrimmable;
import com.facebook.common.references.CloseableReference;
import com.facebook.infer.annotation.Nullsafe;
import java.util.Map;
import javax.annotation.Nullable;
@Nullsafe(Nullsafe.Mode.STRICT)
public interface CountingMemoryCache<K, V> extends MemoryCache<, >, com.facebook.common.memory.MemoryTrimmable {
  public interface EntryStateObserver<K> {
    /**
     * Called when the exclusivity status of the entry changes.
     * 
     * <p>The item can be reused if it is exclusively owned by the cache.
     */
    void onExclusivityChanged(K key, boolean isExclusive) ;

  }

  @Nullable
  com.facebook.common.references.CloseableReference<V> cache(K key, com.facebook.common.references.CloseableReference<V> valueRef, CountingMemoryCache.EntryStateObserver<K> observer) ;

  @Nullable
  com.facebook.common.references.CloseableReference<V> reuse(K key) ;

  /**
   * Removes the exclusively owned items until the cache constraints are met.
   * 
   * <p>This method invokes the external {@link CloseableReference#close} method, so it must not be
   * called while holding the <code>this</code> lock.
   */
  void maybeEvictEntries() ;

  /**
   *  Gets the total size in bytes of the cached items that are used by at least one client. 
   */
  int getInUseSizeInBytes() ;

  /**
   *  Gets the total size in bytes of the cached items that are used by at least one client. 
   */
  int getEvictionQueueCount() ;

  /**
   *  Gets the total size in bytes of the exclusively owned items. 
   */
  int getEvictionQueueSizeInBytes() ;

  /**
   *  Removes all the items from the cache. 
   */
  void clear() ;

  MemoryCacheParams getMemoryCacheParams() ;

  @VisibleForTesting
  public class Entry<K, V> {
    public final K key;

    public final com.facebook.common.references.CloseableReference<V> valueRef;

    /**
     *  The number of clients that reference the value.
     */
    public int clientCount;

    /**
     *  Whether or not this entry is tracked by this cache. Orphans are not tracked by the cache and
     *  as soon as the last client of an orphaned entry closes their reference, the entry's copy is
     *  closed too.
     */
    public boolean isOrphan;

    @Nullable
    public final CountingMemoryCache.EntryStateObserver<K> observer;

    public int accessCount;

    private Entry(K key, com.facebook.common.references.CloseableReference<V> valueRef, @Nullable CountingMemoryCache.EntryStateObserver<K> observer) {
      this.key = Preconditions.checkNotNull(key);
      this.valueRef = Preconditions.checkNotNull(CloseableReference.cloneOrNull(valueRef));
      this.clientCount = 0;
      this.isOrphan = false;
      this.observer = observer;
      this.accessCount = 0;
    }

    /**
     *  Creates a new entry with the usage count of 0. 
     */
    @VisibleForTesting
    public static <K, V> CountingMemoryCache.Entry<K, V> of(final K key, final com.facebook.common.references.CloseableReference<V> valueRef, @Nullable final CountingMemoryCache.EntryStateObserver<K> observer)
    {
      return new Entry<>(key, valueRef, observer);
    }

  }

  CountingLruMap<K, Entry<K, V>> getCachedEntries() ;

  Map<Bitmap, Object> getOtherEntries() ;

}
