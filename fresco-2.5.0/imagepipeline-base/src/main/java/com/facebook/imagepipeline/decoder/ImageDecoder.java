/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.decoder;

import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.image.QualityInfo;
import javax.annotation.Nonnull;
/**
 *  Image decoder interface. Takes an {@link EncodedImage} and creates a {@link CloseableImage}. 
 */
public interface ImageDecoder {
  com.facebook.imagepipeline.image.CloseableImage decode(@Nonnull com.facebook.imagepipeline.image.EncodedImage encodedImage, int length, @Nonnull com.facebook.imagepipeline.image.QualityInfo qualityInfo, @Nonnull com.facebook.imagepipeline.common.ImageDecodeOptions options) ;

}
