/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.platform;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import androidx.core.util.Pools.SynchronizedPool;
import androidx.core.util.Preconditions;
import com.facebook.imagepipeline.memory.BitmapPool;
import com.facebook.imageutils.BitmapUtil;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.concurrent.ThreadSafe;
/**
 *  Bitmap decoder for ART VM (Lollipop and up). 
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
@ThreadSafe
@Nullsafe(Nullsafe.Mode.LOCAL)
public class ArtDecoder extends DefaultDecoder {
  public ArtDecoder(com.facebook.imagepipeline.memory.BitmapPool bitmapPool, int maxNumThreads, SynchronizedPool decodeBuffers) {
    super(bitmapPool, maxNumThreads, decodeBuffers);
  }

  @Override
  public int getBitmapSize(final int width, final int height, final BitmapFactory.Options options) {
    Bitmap.Config c = Preconditions.checkNotNull(options.inPreferredConfig);
    return BitmapUtil.getSizeInByteForBitmap(width, height, c);
  }

}
