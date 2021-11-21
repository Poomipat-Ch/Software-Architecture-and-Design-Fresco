/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.animated.factory;

import com.facebook.imagepipeline.animated.base.AnimatedImage;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.infer.annotation.Nullsafe;
import java.nio.ByteBuffer;
@Nullsafe(Nullsafe.Mode.LOCAL)
public interface AnimatedImageDecoder {
  /**
   * Factory method to create the AnimatedImage from a native pointer
   * 
   * @param nativePtr The native pointer
   * @param sizeInBytes The size in byte to allocate
   * @param options The options for decoding
   * @return The AnimatedImage allocation
   */
  com.facebook.imagepipeline.animated.base.AnimatedImage decodeFromNativeMemory(long nativePtr, int sizeInBytes, com.facebook.imagepipeline.common.ImageDecodeOptions options) ;

  /**
   * Factory method to create the AnimatedImage from a ByteBuffer
   * 
   * @param byteBuffer The ByteBuffer containing the image
   * @param options The options for decoding
   * @return The AnimatedImage allocation
   */
  com.facebook.imagepipeline.animated.base.AnimatedImage decodeFromByteBuffer(ByteBuffer byteBuffer, com.facebook.imagepipeline.common.ImageDecodeOptions options) ;

}
