/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.fresco.vito.core.impl;

import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.datasource.DataSubscriber;
import com.facebook.drawee.backends.pipeline.info.ImageOrigin;
import com.facebook.drawee.components.DeferredReleaser;
import com.facebook.drawee.drawable.ScaleTypeDrawable;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.fresco.ui.common.ControllerListener2;
import com.facebook.fresco.vito.core.CombinedImageListener;
import com.facebook.fresco.vito.core.FrescoDrawable2;
import com.facebook.fresco.vito.core.NopDrawable;
import com.facebook.fresco.vito.core.VitoImagePerfListener;
import com.facebook.fresco.vito.core.VitoImageRequest;
import com.facebook.fresco.vito.core.VitoImageRequestListener;
import com.facebook.fresco.vito.listener.ImageListener;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.listener.BaseRequestListener;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.infer.annotation.Nullsafe;
@Nullsafe(Nullsafe.Mode.LOCAL)
public class FrescoDrawable2Impl extends com.facebook.fresco.vito.core.FrescoDrawable2 implements com.facebook.datasource.DataSubscriber<> {
  /**
   *  Roughly 5 frames.
   */
  private static final long RELEASE_DELAY =  16 * 5;

  private static final Handler sHandler =  new Handler(Looper.getMainLooper());

  private static final com.facebook.drawee.components.DeferredReleaser sDeferredReleaser =  DeferredReleaser.getInstance();

  private final boolean mUseNewReleaseCallbacks;

  @Nullable
  private com.facebook.fresco.vito.core.VitoImageRequest mImageRequest;

  @Nullable
  private Object mCallerContext;

  @Nullable
  private DrawableDataSubscriber mDrawableDataSubscriber;

  private long mImageId;

  @Nullable
  private Object mExtras;

  @Nullable
  private com.facebook.datasource.DataSource<CloseableReference<CloseableImage>> mDataSource;

  private boolean mFetchSubmitted;

  private final CombinedImageListenerImpl mImageListener =  new CombinedImageListenerImpl();

  private final com.facebook.fresco.vito.core.VitoImagePerfListener mImagePerfListener;

  private final Runnable mReleaseRunnable = 
      new Runnable() {
        @Override
        public void run() {
          scheduleReleaseNextFrame();
        }
      };

  private boolean mDelayedReleasePending;

  private final com.facebook.drawee.drawable.ScaleTypeDrawable mActualImageWrapper = 
      new ScaleTypeDrawable(NopDrawable.INSTANCE, ScalingUtils.ScaleType.CENTER_CROP);

  /**
   *  Image perf data fields
   */
  private final com.facebook.imagepipeline.listener.RequestListener mImageOriginListener = 
      new BaseRequestListener() {
        @Override
        public void onUltimateProducerReached(
            String requestId, String producerName, boolean successful) {
          mImageOrigin = mapProducerNameToImageOrigin(producerName);
        }

        private @ImageOrigin int mapProducerNameToImageOrigin(final String producerName) {
          switch (producerName) {
            case "BitmapMemoryCacheGetProducer":
            case "BitmapMemoryCacheProducer":
            case "PostprocessedBitmapMemoryCacheProducer":
              return ImageOrigin.MEMORY_BITMAP;

            case "EncodedMemoryCacheProducer":
              return ImageOrigin.MEMORY_ENCODED;

            case "DiskCacheProducer":
            case "PartialDiskCacheProducer":
              return ImageOrigin.DISK;

            case "NetworkFetchProducer":
              return ImageOrigin.NETWORK;

            case "DataFetchProducer":
            case "LocalAssetFetchProducer":
            case "LocalContentUriFetchProducer":
            case "LocalContentUriThumbnailFetchProducer":
            case "LocalFileFetchProducer":
            case "LocalResourceFetchProducer":
            case "VideoThumbnailProducer":
            case "QualifiedResourceFetchProducer":
              return ImageOrigin.LOCAL;

            default:
              return ImageOrigin.UNKNOWN;
          }
        }
      };

  @ImageOrigin
  private int mImageOrigin =  ImageOrigin.UNKNOWN;

  @VisibleForTesting
  @Nullable
  com.facebook.common.references.CloseableReference<CloseableImage> mImageReference;

  public FrescoDrawable2Impl(boolean useNewReleaseCallbacks, @Nullable com.facebook.fresco.ui.common.ControllerListener2<ImageInfo> imagePerfControllerListener, com.facebook.fresco.vito.core.VitoImagePerfListener imagePerfListener) {
    mUseNewReleaseCallbacks = useNewReleaseCallbacks;
    mImageListener.setImagePerfControllerListener(imagePerfControllerListener);
    mImagePerfListener = imagePerfListener;
  }

  @Nullable
  public Drawable setImageDrawable(@Nullable Drawable newDrawable) {
    return setImage(newDrawable, null);
  }

  @Nullable
  public Drawable setImage(@Nullable Drawable imageDrawable, @Nullable com.facebook.common.references.CloseableReference<CloseableImage> imageReference) {
    cancelReleaseNextFrame();
    cancelReleaseDelayed();
    if (imageDrawable != mActualImageWrapper) {
      mActualImageWrapper.setCurrent(NopDrawable.INSTANCE);
    }
    CloseableReference.closeSafely(mImageReference);
    mImageReference = CloseableReference.cloneOrNull(imageReference);
    return setDrawable(IMAGE_DRAWABLE_INDEX, imageDrawable);
  }

  @Override
  public com.facebook.drawee.drawable.ScaleTypeDrawable getActualImageWrapper() {
    return mActualImageWrapper;
  }

  @Nullable
  @Override
  public Drawable getActualImageDrawable() {
    Drawable actual = getDrawable(IMAGE_DRAWABLE_INDEX);
    if (actual == mActualImageWrapper) {
      return mActualImageWrapper.getDrawable();
    }
    return actual;
  }

  public synchronized void setDataSource(long imageId, @Nullable com.facebook.datasource.DataSource<CloseableReference<CloseableImage>> dataSource) {
    if (imageId != mImageId) {
      return;
    }
    if (mDataSource != null && mDataSource != dataSource) {
      mDataSource.close();
    }
    mDataSource = dataSource;
  }

  public void setFetchSubmitted(boolean fetchSubmitted) {
    mFetchSubmitted = fetchSubmitted;
  }

  @Override
  public boolean isFetchSubmitted() {
    return mFetchSubmitted;
  }

  public void setDrawableDataSubscriber(@Nullable DrawableDataSubscriber drawableDataSubscriber) {
    mDrawableDataSubscriber = drawableDataSubscriber;
  }

  @Nullable
  public DrawableDataSubscriber getDrawableDataSubscriber() {
    return mDrawableDataSubscriber;
  }

  @Override
  public void setImageRequest(@Nullable com.facebook.fresco.vito.core.VitoImageRequest imageRequest) {
    mImageRequest = imageRequest;
  }

  @Override
  public void setCallerContext(@Nullable Object callerContext) {
    mCallerContext = callerContext;
  }

  @Override
  @Nullable
  public Object getCallerContext() {
    return mCallerContext;
  }

  @Override
  public void setImageListener(@Nullable com.facebook.fresco.vito.listener.ImageListener imageListener) {
    mImageListener.setImageListener(imageListener);
  }

  @Override
  @Nullable
  public com.facebook.fresco.vito.listener.ImageListener getImageListener() {
    return mImageListener.getImageListener();
  }

  public void setVitoImageRequestListener(@Nullable com.facebook.fresco.vito.core.VitoImageRequestListener listener) {
    mImageListener.setVitoImageRequestListener(listener);
  }

  public com.facebook.fresco.vito.core.CombinedImageListener getInternalListener() {
    return mImageListener;
  }

  public com.facebook.imagepipeline.listener.RequestListener getImageOriginListener() {
    return mImageOriginListener;
  }

  @Override
  @Nullable
  public com.facebook.fresco.vito.core.VitoImageRequest getImageRequest() {
    return mImageRequest;
  }

  public synchronized void setImageId(long imageId) {
    mImageId = imageId;
  }

  @Override
  public synchronized long getImageId() {
    return mImageId;
  }

  public void setImageOrigin(@ImageOrigin int imageOrigin) {
    mImageOrigin = imageOrigin;
  }

  @ImageOrigin
  public int getImageOrigin() {
    return mImageOrigin;
  }

  @Override
  public void release() {
    close();
  }

  @Override
  public void reset() {
    // Close calls super.reset()
    close();
  }

  @Override
  public synchronized void close() {
    cancelReleaseNextFrame();
    cancelReleaseDelayed();
    if (mUseNewReleaseCallbacks && mFetchSubmitted && mDrawableDataSubscriber != null) {
      mDrawableDataSubscriber.onRelease(this);
    }
    setImageId(0);
    super.close();
    super.reset();
    mActualImageWrapper.setCurrent(NopDrawable.INSTANCE);
    CloseableReference.closeSafely(mImageReference);
    mImageReference = null;
    mDrawableDataSubscriber = null;
    if (mDataSource != null) {
      mDataSource.close();
    }
    mDataSource = null;
    mFetchSubmitted = false;

    mImageOrigin = ImageOrigin.UNKNOWN;
    mExtras = null;
    setOnFadeListener(null);
    mImageListener.onReset();
  }

  public void scheduleReleaseDelayed() {
    if (mDelayedReleasePending) {
      return;
    }
    sHandler.postDelayed(mReleaseRunnable, RELEASE_DELAY);
    mDelayedReleasePending = true;
  }

  @Override
  public void cancelReleaseDelayed() {
    if (mDelayedReleasePending) {
      sHandler.removeCallbacks(mReleaseRunnable);
      mDelayedReleasePending = false;
    }
  }

  public void scheduleReleaseNextFrame() {
    cancelReleaseDelayed();
    sDeferredReleaser.scheduleDeferredRelease(this);
    if (!mUseNewReleaseCallbacks && mDrawableDataSubscriber != null) {
      mDrawableDataSubscriber.onRelease(this);
    }
  }

  public void releaseImmediately() {
    if (!mUseNewReleaseCallbacks && mDrawableDataSubscriber != null) {
      mDrawableDataSubscriber.onRelease(this);
    }
    close();
  }

  @Override
  public void cancelReleaseNextFrame() {
    sDeferredReleaser.cancelDeferredRelease(this);
  }

  @Override
  public void onNewResult(com.facebook.datasource.DataSource<CloseableReference<CloseableImage>> dataSource) {
    if (dataSource != mDataSource || mImageRequest == null || mDrawableDataSubscriber == null) {
      getImagePerfListener().onIgnoreResult(this);
      return; // We don't care
    }
    mDrawableDataSubscriber.onNewResult(this, mImageRequest, dataSource);
  }

  @Override
  public void onFailure(com.facebook.datasource.DataSource<CloseableReference<CloseableImage>> dataSource) {
    if (dataSource != mDataSource || mImageRequest == null || mDrawableDataSubscriber == null) {
      getImagePerfListener().onIgnoreFailure(this);
      return; // wrong image
    }
    mDrawableDataSubscriber.onFailure(this, mImageRequest, dataSource);
  }

  @Override
  public void onCancellation(com.facebook.datasource.DataSource<CloseableReference<CloseableImage>> dataSource) {
    // no-op
  }

  @Override
  public void onProgressUpdate(com.facebook.datasource.DataSource<CloseableReference<CloseableImage>> dataSource) {
    if (dataSource != mDataSource || mImageRequest == null || mDrawableDataSubscriber == null) {
      return; // wrong image
    }
    mDrawableDataSubscriber.onProgressUpdate(this, mImageRequest, dataSource);
  }

  @Nullable
  @Override
  public Object getExtras() {
    return mExtras;
  }

  @Override
  public void setExtras(@Nullable Object extras) {
    mExtras = extras;
  }

  @Override
  public com.facebook.fresco.vito.core.VitoImagePerfListener getImagePerfListener() {
    return mImagePerfListener;
  }

  /**
   *  @return the width of the underlying actual image or -1 if unset 
   */
  @Override
  public int getActualImageWidthPx() {
    if (CloseableReference.isValid(mImageReference)) {
      return mImageReference.get().getWidth();
    }
    return -1;
  }

  /**
   *  @return the width of the underlying actual image or -1 if unset 
   */
  @Override
  public int getActualImageHeightPx() {
    if (CloseableReference.isValid(mImageReference)) {
      return mImageReference.get().getHeight();
    }
    return -1;
  }

}
