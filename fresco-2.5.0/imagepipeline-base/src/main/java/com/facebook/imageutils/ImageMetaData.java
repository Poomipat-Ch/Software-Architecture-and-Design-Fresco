/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imageutils;

import android.graphics.ColorSpace;
import android.util.Pair;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;
/**
 *  Wrapper class representing the recovered meta data of an image when decoding. 
 */
@Nullsafe(Nullsafe.Mode.STRICT)
public class ImageMetaData {
  @Nullable
  private final Pair<Integer, Integer> mDimensions;

  @Nullable
  private final ColorSpace mColorSpace;

  public ImageMetaData(int width, int height, @Nullable ColorSpace colorSpace) {
    mDimensions = (width == -1 || height == -1) ? null : new Pair<>(width, height);
    mColorSpace = colorSpace;
  }

  @Nullable
  public Pair<Integer, Integer> getDimensions() {
    return mDimensions;
  }

  @Nullable
  public ColorSpace getColorSpace() {
    return mColorSpace;
  }

}
