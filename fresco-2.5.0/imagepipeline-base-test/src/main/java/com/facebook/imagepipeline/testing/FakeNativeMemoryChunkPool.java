/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.testing;

import android.util.SparseIntArray;
import com.facebook.common.memory.MemoryTrimmableRegistry;
import com.facebook.imagepipeline.memory.NativeMemoryChunk;
import com.facebook.imagepipeline.memory.NativeMemoryChunkPool;
import com.facebook.imagepipeline.memory.PoolParams;
import com.facebook.imagepipeline.memory.PoolStatsTracker;
import static org.mockito.Mockito.mock;
/**
 *  A 'fake' {@link NativeMemoryChunkPool} instance as a test helper 
 */
public class FakeNativeMemoryChunkPool extends com.facebook.imagepipeline.memory.NativeMemoryChunkPool {
  public FakeNativeMemoryChunkPool() {
    this(new PoolParams(128, getBucketSizes()));
  }

  public FakeNativeMemoryChunkPool(com.facebook.imagepipeline.memory.PoolParams poolParams) {
    super(mock(MemoryTrimmableRegistry.class), poolParams, mock(PoolStatsTracker.class));
  }

  @Override
  protected com.facebook.imagepipeline.memory.NativeMemoryChunk alloc(int bucketedSize) {
    return new FakeNativeMemoryChunk(bucketedSize);
  }

  private static SparseIntArray getBucketSizes()
  {
    final SparseIntArray bucketSizes = new SparseIntArray();
    bucketSizes.put(4, 10);
    bucketSizes.put(8, 10);
    bucketSizes.put(16, 10);
    bucketSizes.put(32, 10);
    return bucketSizes;
  }

}
