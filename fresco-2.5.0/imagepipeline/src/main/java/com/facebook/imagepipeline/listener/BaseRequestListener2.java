/**
 *  (c) Facebook, Inc. and its affiliates. Confidential and proprietary.
 */
package com.facebook.imagepipeline.listener;

import androidx.annotation.NonNull;
import com.facebook.imagepipeline.producers.ProducerContext;
import com.facebook.infer.annotation.Nullsafe;
import java.util.Map;
import javax.annotation.Nullable;
@Nullsafe(Nullsafe.Mode.STRICT)
public class BaseRequestListener2 implements RequestListener2 {
  @Override
  public void onRequestStart(@NonNull com.facebook.imagepipeline.producers.ProducerContext producerContext) {
  }

  @Override
  public void onRequestSuccess(@NonNull com.facebook.imagepipeline.producers.ProducerContext producerContext) {
  }

  @Override
  public void onRequestFailure(@NonNull com.facebook.imagepipeline.producers.ProducerContext producerContext, Throwable throwable) {
  }

  @Override
  public void onRequestCancellation(@NonNull com.facebook.imagepipeline.producers.ProducerContext producerContext) {
  }

  @Override
  public void onProducerStart(@NonNull com.facebook.imagepipeline.producers.ProducerContext producerContext, @NonNull String producerName) {
  }

  @Override
  public void onProducerEvent(@NonNull com.facebook.imagepipeline.producers.ProducerContext producerContext, @NonNull String producerName, @NonNull String eventName) {
  }

  @Override
  public void onProducerFinishWithSuccess(@NonNull com.facebook.imagepipeline.producers.ProducerContext producerContext, @NonNull String producerName, @Nullable Map<String, String> extraMap) {
  }

  @Override
  public void onProducerFinishWithFailure(@NonNull com.facebook.imagepipeline.producers.ProducerContext producerContext, String producerName, Throwable t, @Nullable Map<String, String> extraMap) {
  }

  @Override
  public void onProducerFinishWithCancellation(@NonNull com.facebook.imagepipeline.producers.ProducerContext producerContext, @NonNull String producerName, @Nullable Map<String, String> extraMap) {
  }

  @Override
  public void onUltimateProducerReached(@NonNull com.facebook.imagepipeline.producers.ProducerContext producerContext, @NonNull String producerName, boolean successful) {
  }

  @Override
  public boolean requiresExtraMap(@NonNull com.facebook.imagepipeline.producers.ProducerContext producerContext, @NonNull String producerName) {
    return false;
  }

}
