/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.backends.okhttp3;

import android.net.Uri;
import android.os.Looper;
import android.os.SystemClock;
import com.facebook.imagepipeline.common.BytesRange;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.producers.BaseNetworkFetcher;
import com.facebook.imagepipeline.producers.BaseProducerContextCallbacks;
import com.facebook.imagepipeline.producers.Consumer;
import com.facebook.imagepipeline.producers.FetchState;
import com.facebook.imagepipeline.producers.NetworkFetcher;
import com.facebook.imagepipeline.producers.ProducerContext;
import com.facebook.infer.annotation.Nullsafe;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
/**
 *  Network fetcher that uses OkHttp 3 as a backend. 
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class OkHttpNetworkFetcher extends com.facebook.imagepipeline.producers.BaseNetworkFetcher<> {
  public static class OkHttpNetworkFetchState extends com.facebook.imagepipeline.producers.FetchState {
    public long submitTime;

    public long responseTime;

    public long fetchCompleteTime;

    public OkHttpNetworkFetchState(com.facebook.imagepipeline.producers.Consumer<EncodedImage> consumer, com.facebook.imagepipeline.producers.ProducerContext producerContext) {
      super(consumer, producerContext);
    }

  }

  private static final String QUEUE_TIME =  "queue_time";

  private static final String FETCH_TIME =  "fetch_time";

  private static final String TOTAL_TIME =  "total_time";

  private static final String IMAGE_SIZE =  "image_size";

  private final Call.Factory mCallFactory;

  @Nullable
  private final CacheControl mCacheControl;

  private Executor mCancellationExecutor;

  /**
   *  @param okHttpClient client to use 
   */
  public OkHttpNetworkFetcher(OkHttpClient okHttpClient) {
    this(okHttpClient, okHttpClient.dispatcher().executorService());
  }

  /**
   * @param callFactory custom {@link Call.Factory} for fetching image from the network
   * @param cancellationExecutor executor on which fetching cancellation is performed if
   *     cancellation is requested from the UI Thread
   */
  public OkHttpNetworkFetcher(Call.Factory callFactory, Executor cancellationExecutor) {
    this(callFactory, cancellationExecutor, true);
  }

  /**
   * @param callFactory custom {@link Call.Factory} for fetching image from the network
   * @param cancellationExecutor executor on which fetching cancellation is performed if
   *     cancellation is requested from the UI Thread
   * @param disableOkHttpCache true if network requests should not be cached by OkHttp
   */
  public OkHttpNetworkFetcher(Call.Factory callFactory, Executor cancellationExecutor, boolean disableOkHttpCache) {
    mCallFactory = callFactory;
    mCancellationExecutor = cancellationExecutor;
    mCacheControl = disableOkHttpCache ? new CacheControl.Builder().noStore().build() : null;
  }

  @Override
  public OkHttpNetworkFetcher.OkHttpNetworkFetchState createFetchState(com.facebook.imagepipeline.producers.Consumer<EncodedImage> consumer, com.facebook.imagepipeline.producers.ProducerContext context) {
    return new OkHttpNetworkFetchState(consumer, context);
  }

  @Override
  public void fetch(final OkHttpNetworkFetcher.OkHttpNetworkFetchState fetchState, final com.facebook.imagepipeline.producers.NetworkFetcher.Callback callback) {
    fetchState.submitTime = SystemClock.elapsedRealtime();
    final Uri uri = fetchState.getUri();

    try {
      final Request.Builder requestBuilder = new Request.Builder().url(uri.toString()).get();

      if (mCacheControl != null) {
        requestBuilder.cacheControl(mCacheControl);
      }

      final BytesRange bytesRange = fetchState.getContext().getImageRequest().getBytesRange();
      if (bytesRange != null) {
        requestBuilder.addHeader("Range", bytesRange.toHttpRangeHeaderValue());
      }

      fetchWithRequest(fetchState, callback, requestBuilder.build());
    } catch (Exception e) {
      // handle error while creating the request
      callback.onFailure(e);
    }
  }

  @Override
  public void onFetchCompletion(OkHttpNetworkFetcher.OkHttpNetworkFetchState fetchState, int byteSize) {
    fetchState.fetchCompleteTime = SystemClock.elapsedRealtime();
  }

  @Override
  public Map<String, String> getExtraMap(OkHttpNetworkFetcher.OkHttpNetworkFetchState fetchState, int byteSize) {
    Map<String, String> extraMap = new HashMap<>(4);
    extraMap.put(QUEUE_TIME, Long.toString(fetchState.responseTime - fetchState.submitTime));
    extraMap.put(FETCH_TIME, Long.toString(fetchState.fetchCompleteTime - fetchState.responseTime));
    extraMap.put(TOTAL_TIME, Long.toString(fetchState.fetchCompleteTime - fetchState.submitTime));
    extraMap.put(IMAGE_SIZE, Integer.toString(byteSize));
    return extraMap;
  }

  protected void fetchWithRequest(final OkHttpNetworkFetcher.OkHttpNetworkFetchState fetchState, final com.facebook.imagepipeline.producers.NetworkFetcher.Callback callback, final Request request) {
    final Call call = mCallFactory.newCall(request);

    fetchState
        .getContext()
        .addCallbacks(
            new BaseProducerContextCallbacks() {
              @Override
              public void onCancellationRequested() {
                if (Looper.myLooper() != Looper.getMainLooper()) {
                  call.cancel();
                } else {
                  mCancellationExecutor.execute(
                      new Runnable() {
                        @Override
                        public void run() {
                          call.cancel();
                        }
                      });
                }
              }
            });

    call.enqueue(
        new okhttp3.Callback() {
          @Override
          public void onResponse(Call call, Response response) throws IOException {
            fetchState.responseTime = SystemClock.elapsedRealtime();
            final ResponseBody body = response.body();
            if (body == null) {
              handleException(call, new IOException("Response body null: " + response), callback);
              return;
            }
            try {
              if (!response.isSuccessful()) {
                handleException(
                    call, new IOException("Unexpected HTTP code " + response), callback);
                return;
              }

              BytesRange responseRange =
                  BytesRange.fromContentRangeHeader(response.header("Content-Range"));
              if (responseRange != null
                  && !(responseRange.from == 0
                      && responseRange.to == BytesRange.TO_END_OF_CONTENT)) {
                // Only treat as a partial image if the range is not all of the content
                fetchState.setResponseBytesRange(responseRange);
                fetchState.setOnNewResultStatusFlags(Consumer.IS_PARTIAL_RESULT);
              }

              long contentLength = body.contentLength();
              if (contentLength < 0) {
                contentLength = 0;
              }
              callback.onResponse(body.byteStream(), (int) contentLength);
            } catch (Exception e) {
              handleException(call, e, callback);
            } finally {
              body.close();
            }
          }

          @Override
          public void onFailure(Call call, IOException e) {
            handleException(call, e, callback);
          }
        });
  }

  /**
   * Handles exceptions.
   * 
   * <p>OkHttp notifies callers of cancellations via an IOException. If IOException is caught after
   * request cancellation, then the exception is interpreted as successful cancellation and
   * onCancellation is called. Otherwise onFailure is called.
   */
  private void handleException(final Call call, final Exception e, final com.facebook.imagepipeline.producers.NetworkFetcher.Callback callback) {
    if (call.isCanceled()) {
      callback.onCancellation();
    } else {
      callback.onFailure(e);
    }
  }

}
