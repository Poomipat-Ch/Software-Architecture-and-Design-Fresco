/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.producers;

import androidx.annotation.VisibleForTesting;
import com.facebook.cache.common.CacheKey;
import com.facebook.common.internal.ImmutableMap;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.cache.MemoryCache;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.Postprocessor;
import com.facebook.imagepipeline.request.RepeatedPostprocessor;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;
/**
 *  Memory cache producer for the bitmap memory cache. 
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class PostprocessedBitmapMemoryCacheProducer implements Producer<> {
  public static final String PRODUCER_NAME =  "PostprocessedBitmapMemoryCacheProducer";

  @VisibleForTesting
  static final String VALUE_FOUND =  "cached_value_found";

  private final com.facebook.imagepipeline.cache.MemoryCache<CacheKey, CloseableImage> mMemoryCache;

  private final com.facebook.imagepipeline.cache.CacheKeyFactory mCacheKeyFactory;

  private final Producer<CloseableReference<CloseableImage>> mInputProducer;

  public PostprocessedBitmapMemoryCacheProducer(com.facebook.imagepipeline.cache.MemoryCache<CacheKey, CloseableImage> memoryCache, com.facebook.imagepipeline.cache.CacheKeyFactory cacheKeyFactory, Producer<CloseableReference<CloseableImage>> inputProducer) {
    mMemoryCache = memoryCache;
    mCacheKeyFactory = cacheKeyFactory;
    mInputProducer = inputProducer;
  }

  @Override
  public void produceResults(final Consumer<CloseableReference<CloseableImage>> consumer, final ProducerContext producerContext) {

    final ProducerListener2 listener = producerContext.getProducerListener();
    final ImageRequest imageRequest = producerContext.getImageRequest();
    final Object callerContext = producerContext.getCallerContext();

    // If there's no postprocessor or the postprocessor doesn't require caching, forward results.
    final Postprocessor postprocessor = imageRequest.getPostprocessor();
    if (postprocessor == null || postprocessor.getPostprocessorCacheKey() == null) {
      mInputProducer.produceResults(consumer, producerContext);
      return;
    }
    listener.onProducerStart(producerContext, getProducerName());
    final CacheKey cacheKey =
        mCacheKeyFactory.getPostprocessedBitmapCacheKey(imageRequest, callerContext);
    CloseableReference<CloseableImage> cachedReference = mMemoryCache.get(cacheKey);
    if (cachedReference != null) {
      listener.onProducerFinishWithSuccess(
          producerContext,
          getProducerName(),
          listener.requiresExtraMap(producerContext, getProducerName())
              ? ImmutableMap.of(VALUE_FOUND, "true")
              : null);
      listener.onUltimateProducerReached(producerContext, PRODUCER_NAME, true);
      producerContext.putOriginExtra("memory_bitmap", "postprocessed");
      consumer.onProgressUpdate(1.0f);
      consumer.onNewResult(cachedReference, Consumer.IS_LAST);
      cachedReference.close();
    } else {
      final boolean isRepeatedProcessor = postprocessor instanceof RepeatedPostprocessor;
      final boolean isMemoryCachedEnabled =
          producerContext.getImageRequest().isMemoryCacheEnabled();
      Consumer<CloseableReference<CloseableImage>> cachedConsumer =
          new CachedPostprocessorConsumer(
              consumer, cacheKey, isRepeatedProcessor, mMemoryCache, isMemoryCachedEnabled);
      listener.onProducerFinishWithSuccess(
          producerContext,
          getProducerName(),
          listener.requiresExtraMap(producerContext, getProducerName())
              ? ImmutableMap.of(VALUE_FOUND, "false")
              : null);
      mInputProducer.produceResults(cachedConsumer, producerContext);
    }
  }

  public static class CachedPostprocessorConsumer extends DelegatingConsumer<, > {
    private final com.facebook.cache.common.CacheKey mCacheKey;

    private final boolean mIsRepeatedProcessor;

    private final com.facebook.imagepipeline.cache.MemoryCache<CacheKey, CloseableImage> mMemoryCache;

    private final boolean mIsMemoryCachedEnabled;

    public CachedPostprocessorConsumer(final Consumer<CloseableReference<CloseableImage>> consumer, final com.facebook.cache.common.CacheKey cacheKey, final boolean isRepeatedProcessor, final com.facebook.imagepipeline.cache.MemoryCache<CacheKey, CloseableImage> memoryCache, boolean isMemoryCachedEnabled) {
      super(consumer);
      this.mCacheKey = cacheKey;
      this.mIsRepeatedProcessor = isRepeatedProcessor;
      this.mMemoryCache = memoryCache;
      mIsMemoryCachedEnabled = isMemoryCachedEnabled;
    }

    @Override
    protected void onNewResultImpl(@Nullable com.facebook.common.references.CloseableReference<CloseableImage> newResult, @Status int status) {
      // ignore invalid intermediate results and forward the null result if last
      if (newResult == null) {
        if (isLast(status)) {
          getConsumer().onNewResult(null, status);
        }
        return;
      }
      // ignore intermediate results for non-repeated postprocessors
      if (isNotLast(status) && !mIsRepeatedProcessor) {
        return;
      }
      // cache, if needed, and forward the new result
      CloseableReference<CloseableImage> newCachedResult = null;
      if (mIsMemoryCachedEnabled) {
        newCachedResult = mMemoryCache.cache(mCacheKey, newResult);
      }
      try {
        getConsumer().onProgressUpdate(1f);
        getConsumer().onNewResult((newCachedResult != null) ? newCachedResult : newResult, status);
      } finally {
        CloseableReference.closeSafely(newCachedResult);
      }
    }

  }

  protected String getProducerName() {
    return PRODUCER_NAME;
  }

}
