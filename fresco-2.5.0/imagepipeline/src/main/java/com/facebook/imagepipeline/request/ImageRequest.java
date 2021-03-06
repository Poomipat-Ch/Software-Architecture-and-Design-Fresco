/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.request;

import android.net.Uri;
import com.facebook.cache.common.CacheKey;
import com.facebook.common.internal.Fn;
import com.facebook.common.internal.Objects;
import com.facebook.common.media.MediaUtils;
import com.facebook.common.util.UriUtil;
import com.facebook.imagepipeline.common.BytesRange;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.common.Priority;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.common.RotationOptions;
import com.facebook.imagepipeline.common.SourceUriType;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.imageutils.BitmapUtil;
import java.io.File;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_DATA;
import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_LOCAL_ASSET;
import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_LOCAL_CONTENT;
import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_LOCAL_IMAGE_FILE;
import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_LOCAL_RESOURCE;
import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_LOCAL_VIDEO_FILE;
import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_NETWORK;
import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_QUALIFIED_RESOURCE;
import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_UNKNOWN;
/**
 * Immutable object encapsulating everything pipeline has to know about requested image to proceed.
 */
@Immutable
public class ImageRequest {
  public enum RequestLevel {
    FULL_FETCH(1),/**
     *  Fetch (from the network or local storage) 
     */

    DISK_CACHE(2),/**
     *  Disk caching 
     */

    ENCODED_MEMORY_CACHE(3),/**
     *  Encoded memory caching 
     */

    BITMAP_MEMORY_CACHE(4),/**
     *  Bitmap caching 
     */
;
    private int mValue;
    private RequestLevel(int value) {
      mValue = value;
    }

    public int getValue() {
      return mValue;
    }

    public static ImageRequest.RequestLevel getMax(ImageRequest.RequestLevel requestLevel1, ImageRequest.RequestLevel requestLevel2)
    {
      return requestLevel1.getValue() > requestLevel2.getValue() ? requestLevel1 : requestLevel2;
    }

  }

  public enum CacheChoice {
    SMALL,/**
     *  Indicates that this image should go in the small disk cache, if one is being used 
     */

    DEFAULT,/**
     *  Default 
     */
;
  }

  private static boolean sUseCachedHashcodeInEquals;

  private static boolean sCacheHashcode;

  private int mHashcode;

  /**
   *  Cache choice 
   */
  private final ImageRequest.CacheChoice mCacheChoice;

  /**
   *  Source Uri 
   */
  private final Uri mSourceUri;

  @SourceUriType
  private final int mSourceUriType;

  /**
   *  Source File - for local fetches only, lazily initialized 
   */
  @Nullable
  private File mSourceFile;

  /**
   *  If set - the client will receive intermediate results 
   */
  private final boolean mProgressiveRenderingEnabled;

  /**
   *  If set the client will receive thumbnail previews for local images, before the whole image 
   */
  private final boolean mLocalThumbnailPreviewsEnabled;

  private final com.facebook.imagepipeline.common.ImageDecodeOptions mImageDecodeOptions;

  /**
   *  resize options 
   */
  @Nullable
  private final com.facebook.imagepipeline.common.ResizeOptions mResizeOptions;

  /**
   *  rotation options 
   */
  private final com.facebook.imagepipeline.common.RotationOptions mRotationOptions;

  /**
   *  Range of bytes to request from the network 
   */
  @Nullable
  private final com.facebook.imagepipeline.common.BytesRange mBytesRange;

  /**
   *  Priority levels of this request. 
   */
  private final com.facebook.imagepipeline.common.Priority mRequestPriority;

  /**
   *  Lowest level that is permitted to fetch an image from 
   */
  private final ImageRequest.RequestLevel mLowestPermittedRequestLevel;

  /**
   *  Whether the disk cache should be used for this request 
   */
  private final boolean mIsDiskCacheEnabled;

  /**
   *  Whether the memory cache should be used for this request 
   */
  private final boolean mIsMemoryCacheEnabled;

  /**
   * Whether to decode prefetched images. true -> Cache both encoded image and bitmap. false ->
   * Cache only encoded image and do not decode until image is needed to be shown. null -> Use
   * pipeline's default
   * 
   */
  @Nullable
  private final Boolean mDecodePrefetches;

  /**
   *  Postprocessor to run on the output bitmap. 
   */
  @Nullable
  private final Postprocessor mPostprocessor;

  /**
   *  Request listener to use for this image request 
   */
  @Nullable
  private final com.facebook.imagepipeline.listener.RequestListener mRequestListener;

  /**
   * Controls whether resizing is allowed for this request. true -> allow for this request. false ->
   * disallow for this request. null -> use default pipeline's setting.
   * 
   */
  @Nullable
  private final Boolean mResizingAllowedOverride;

  private final int mDelayMs;

  @Nullable
  public static ImageRequest fromFile(@Nullable File file)
  {
    return (file == null) ? null : ImageRequest.fromUri(UriUtil.getUriForFile(file));
  }

  @Nullable
  public static ImageRequest fromUri(@Nullable Uri uri)
  {
    return (uri == null) ? null : ImageRequestBuilder.newBuilderWithSource(uri).build();
  }

  @Nullable
  public static ImageRequest fromUri(@Nullable String uriString)
  {
    return (uriString == null || uriString.length() == 0) ? null : fromUri(Uri.parse(uriString));
  }

  protected ImageRequest(ImageRequestBuilder builder) {
    mCacheChoice = builder.getCacheChoice();
    mSourceUri = builder.getSourceUri();
    mSourceUriType = getSourceUriType(mSourceUri);

    mProgressiveRenderingEnabled = builder.isProgressiveRenderingEnabled();
    mLocalThumbnailPreviewsEnabled = builder.isLocalThumbnailPreviewsEnabled();

    mImageDecodeOptions = builder.getImageDecodeOptions();

    mResizeOptions = builder.getResizeOptions();
    mRotationOptions =
        builder.getRotationOptions() == null
            ? RotationOptions.autoRotate()
            : builder.getRotationOptions();
    mBytesRange = builder.getBytesRange();

    mRequestPriority = builder.getRequestPriority();
    mLowestPermittedRequestLevel = builder.getLowestPermittedRequestLevel();
    mIsDiskCacheEnabled = builder.isDiskCacheEnabled();
    mIsMemoryCacheEnabled = builder.isMemoryCacheEnabled();
    mDecodePrefetches = builder.shouldDecodePrefetches();

    mPostprocessor = builder.getPostprocessor();

    mRequestListener = builder.getRequestListener();

    mResizingAllowedOverride = builder.getResizingAllowedOverride();

    mDelayMs = builder.getDelayMs();
  }

  public ImageRequest.CacheChoice getCacheChoice() {
    return mCacheChoice;
  }

  public Uri getSourceUri() {
    return mSourceUri;
  }

  @SourceUriType
  public int getSourceUriType() {
    return mSourceUriType;
  }

  public int getPreferredWidth() {
    return (mResizeOptions != null) ? mResizeOptions.width : (int) BitmapUtil.MAX_BITMAP_SIZE;
  }

  public int getPreferredHeight() {
    return (mResizeOptions != null) ? mResizeOptions.height : (int) BitmapUtil.MAX_BITMAP_SIZE;
  }

  @Nullable
  public com.facebook.imagepipeline.common.ResizeOptions getResizeOptions() {
    return mResizeOptions;
  }

  public com.facebook.imagepipeline.common.RotationOptions getRotationOptions() {
    return mRotationOptions;
  }

  /**
   *  @deprecated Use {@link #getRotationOptions()} 
   */
  @Deprecated
  public boolean getAutoRotateEnabled() {
    return mRotationOptions.useImageMetadata();
  }

  @Nullable
  public com.facebook.imagepipeline.common.BytesRange getBytesRange() {
    return mBytesRange;
  }

  public com.facebook.imagepipeline.common.ImageDecodeOptions getImageDecodeOptions() {
    return mImageDecodeOptions;
  }

  public boolean getProgressiveRenderingEnabled() {
    return mProgressiveRenderingEnabled;
  }

  public boolean getLocalThumbnailPreviewsEnabled() {
    return mLocalThumbnailPreviewsEnabled;
  }

  public com.facebook.imagepipeline.common.Priority getPriority() {
    return mRequestPriority;
  }

  public ImageRequest.RequestLevel getLowestPermittedRequestLevel() {
    return mLowestPermittedRequestLevel;
  }

  public boolean isDiskCacheEnabled() {
    return mIsDiskCacheEnabled;
  }

  public boolean isMemoryCacheEnabled() {
    return mIsMemoryCacheEnabled;
  }

  @Nullable
  public Boolean shouldDecodePrefetches() {
    return mDecodePrefetches;
  }

  @Nullable
  public Boolean getResizingAllowedOverride() {
    return mResizingAllowedOverride;
  }

  public int getDelayMs() {
    return mDelayMs;
  }

  public synchronized File getSourceFile() {
    if (mSourceFile == null) {
      mSourceFile = new File(mSourceUri.getPath());
    }
    return mSourceFile;
  }

  @Nullable
  public Postprocessor getPostprocessor() {
    return mPostprocessor;
  }

  @Nullable
  public com.facebook.imagepipeline.listener.RequestListener getRequestListener() {
    return mRequestListener;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (!(o instanceof ImageRequest)) {
      return false;
    }
    ImageRequest request = (ImageRequest) o;
    if (sUseCachedHashcodeInEquals) {
      int a = mHashcode;
      int b = request.mHashcode;
      if (a != 0 && b != 0 && a != b) {
        return false;
      }
    }
    if (mLocalThumbnailPreviewsEnabled != request.mLocalThumbnailPreviewsEnabled) return false;
    if (mIsDiskCacheEnabled != request.mIsDiskCacheEnabled) return false;
    if (mIsMemoryCacheEnabled != request.mIsMemoryCacheEnabled) return false;
    if (!Objects.equal(mSourceUri, request.mSourceUri)
        || !Objects.equal(mCacheChoice, request.mCacheChoice)
        || !Objects.equal(mSourceFile, request.mSourceFile)
        || !Objects.equal(mBytesRange, request.mBytesRange)
        || !Objects.equal(mImageDecodeOptions, request.mImageDecodeOptions)
        || !Objects.equal(mResizeOptions, request.mResizeOptions)
        || !Objects.equal(mRequestPriority, request.mRequestPriority)
        || !Objects.equal(mLowestPermittedRequestLevel, request.mLowestPermittedRequestLevel)
        || !Objects.equal(mDecodePrefetches, request.mDecodePrefetches)
        || !Objects.equal(mResizingAllowedOverride, request.mResizingAllowedOverride)
        || !Objects.equal(mRotationOptions, request.mRotationOptions)) {
      return false;
    }
    final CacheKey thisPostprocessorKey =
        mPostprocessor != null ? mPostprocessor.getPostprocessorCacheKey() : null;
    final CacheKey thatPostprocessorKey =
        request.mPostprocessor != null ? request.mPostprocessor.getPostprocessorCacheKey() : null;
    if (!Objects.equal(thisPostprocessorKey, thatPostprocessorKey)) return false;
    return mDelayMs == request.mDelayMs;
  }

  @Override
  public int hashCode() {
    final boolean cacheHashcode = sCacheHashcode;
    int result = 0;
    if (cacheHashcode) {
      result = mHashcode;
    }
    if (result == 0) {
      final CacheKey postprocessorCacheKey =
          mPostprocessor != null ? mPostprocessor.getPostprocessorCacheKey() : null;
      result =
          Objects.hashCode(
              mCacheChoice,
              mSourceUri,
              mLocalThumbnailPreviewsEnabled,
              mBytesRange,
              mRequestPriority,
              mLowestPermittedRequestLevel,
              mIsDiskCacheEnabled,
              mIsMemoryCacheEnabled,
              mImageDecodeOptions,
              mDecodePrefetches,
              mResizeOptions,
              mRotationOptions,
              postprocessorCacheKey,
              mResizingAllowedOverride,
              mDelayMs);
      // ^ I *think* this is safe despite autoboxing...?
      if (cacheHashcode) {
        mHashcode = result;
      }
    }
    return result;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("uri", mSourceUri)
        .add("cacheChoice", mCacheChoice)
        .add("decodeOptions", mImageDecodeOptions)
        .add("postprocessor", mPostprocessor)
        .add("priority", mRequestPriority)
        .add("resizeOptions", mResizeOptions)
        .add("rotationOptions", mRotationOptions)
        .add("bytesRange", mBytesRange)
        .add("resizingAllowedOverride", mResizingAllowedOverride)
        .add("progressiveRenderingEnabled", mProgressiveRenderingEnabled)
        .add("localThumbnailPreviewsEnabled", mLocalThumbnailPreviewsEnabled)
        .add("lowestPermittedRequestLevel", mLowestPermittedRequestLevel)
        .add("isDiskCacheEnabled", mIsDiskCacheEnabled)
        .add("isMemoryCacheEnabled", mIsMemoryCacheEnabled)
        .add("decodePrefetches", mDecodePrefetches)
        .add("delayMs", mDelayMs)
        .toString();
  }

  /**
   * This is a utility method which returns the type of Uri
   * 
   * @param uri The Uri to test
   * @return The type of the given Uri if available or SOURCE_TYPE_UNKNOWN if not
   */
  @SourceUriType
  private static int getSourceUriType(final Uri uri)
  {
    if (uri == null) {
      return SOURCE_TYPE_UNKNOWN;
    }
    if (UriUtil.isNetworkUri(uri)) {
      return SOURCE_TYPE_NETWORK;
    } else if (UriUtil.isLocalFileUri(uri)) {
      if (MediaUtils.isVideo(MediaUtils.extractMime(uri.getPath()))) {
        return SOURCE_TYPE_LOCAL_VIDEO_FILE;
      } else {
        return SOURCE_TYPE_LOCAL_IMAGE_FILE;
      }
    } else if (UriUtil.isLocalContentUri(uri)) {
      return SOURCE_TYPE_LOCAL_CONTENT;
    } else if (UriUtil.isLocalAssetUri(uri)) {
      return SOURCE_TYPE_LOCAL_ASSET;
    } else if (UriUtil.isLocalResourceUri(uri)) {
      return SOURCE_TYPE_LOCAL_RESOURCE;
    } else if (UriUtil.isDataUri(uri)) {
      return SOURCE_TYPE_DATA;
    } else if (UriUtil.isQualifiedResourceUri(uri)) {
      return SOURCE_TYPE_QUALIFIED_RESOURCE;
    } else {
      return SOURCE_TYPE_UNKNOWN;
    }
  }

  public static final com.facebook.common.internal.Fn<ImageRequest, Uri> REQUEST_TO_URI_FN = 
      new Fn<ImageRequest, Uri>() {
        @Override
        public @Nullable Uri apply(@Nullable ImageRequest arg) {
          return arg != null ? arg.getSourceUri() : null;
        }
      };

  public static void setUseCachedHashcodeInEquals(boolean useCachedHashcodeInEquals)
  {
    sUseCachedHashcodeInEquals = useCachedHashcodeInEquals;
  }

  public static void setCacheHashcode(boolean cacheHashcode)
  {
    sCacheHashcode = cacheHashcode;
  }

}
