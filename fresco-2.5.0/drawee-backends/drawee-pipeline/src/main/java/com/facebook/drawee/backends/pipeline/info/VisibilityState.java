/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.drawee.backends.pipeline.info;

import androidx.annotation.IntDef;
import java.lang.annotation.Retention;
import static com.facebook.drawee.backends.pipeline.info.VisibilityState.INVISIBLE;
import static com.facebook.drawee.backends.pipeline.info.VisibilityState.UNKNOWN;
import static com.facebook.drawee.backends.pipeline.info.VisibilityState.VISIBLE;
import static java.lang.annotation.RetentionPolicy.SOURCE;
@Retention(SOURCE)
@IntDef({
  UNKNOWN, VISIBLE, INVISIBLE,
})
public @interface VisibilityState {
  int UNKNOWN =  -1;

  int VISIBLE =  1;

  int INVISIBLE =  2;

}
