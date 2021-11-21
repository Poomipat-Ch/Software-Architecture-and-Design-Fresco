/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.fresco.vito.core;

import android.net.Uri;
import com.facebook.fresco.vito.options.DecodedImageOptions;
import com.facebook.fresco.vito.options.EncodedImageOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;
@Nullsafe(Nullsafe.Mode.STRICT)
public interface ImagePipelineUtils {
  @Nullable
  com.facebook.imagepipeline.request.ImageRequest buildImageRequest(@Nullable Uri uri, com.facebook.fresco.vito.options.DecodedImageOptions imageOptions) ;

  @Nullable
  com.facebook.imagepipeline.request.ImageRequest wrapDecodedImageRequest(com.facebook.imagepipeline.request.ImageRequest originalRequest, com.facebook.fresco.vito.options.DecodedImageOptions imageOptions) ;

  @Nullable
  com.facebook.imagepipeline.request.ImageRequest buildEncodedImageRequest(@Nullable Uri uri, com.facebook.fresco.vito.options.EncodedImageOptions imageOptions) ;

}
