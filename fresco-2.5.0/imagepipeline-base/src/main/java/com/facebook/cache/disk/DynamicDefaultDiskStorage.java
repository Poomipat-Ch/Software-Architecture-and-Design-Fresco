/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 * 
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.cache.disk;

import androidx.annotation.VisibleForTesting;
import com.facebook.binaryresource.BinaryResource;
import com.facebook.cache.common.CacheErrorLogger;
import com.facebook.common.file.FileTree;
import com.facebook.common.file.FileUtils;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Supplier;
import com.facebook.common.logging.FLog;
import com.facebook.infer.annotation.Nullsafe;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import javax.annotation.Nullable;
/**
 *  A supplier of a DiskStorage concrete implementation. 
 */
@Nullsafe(Nullsafe.Mode.STRICT)
public class DynamicDefaultDiskStorage implements DiskStorage {
  private static final Class<?> TAG =  DynamicDefaultDiskStorage.class;

  private final int mVersion;

  private final com.facebook.common.internal.Supplier<File> mBaseDirectoryPathSupplier;

  private final String mBaseDirectoryName;

  private final com.facebook.cache.common.CacheErrorLogger mCacheErrorLogger;

  @VisibleForTesting
  static class State {
    @Nullable
    public final DiskStorage delegate;

    @Nullable
    public final File rootDirectory;

    @VisibleForTesting
    State(@Nullable File rootDirectory, @Nullable DiskStorage delegate) {
      this.delegate = delegate;
      this.rootDirectory = rootDirectory;
    }

  }

  @VisibleForTesting
  volatile DynamicDefaultDiskStorage.State mCurrentState;

  public DynamicDefaultDiskStorage(int version, com.facebook.common.internal.Supplier<File> baseDirectoryPathSupplier, String baseDirectoryName, com.facebook.cache.common.CacheErrorLogger cacheErrorLogger) {
    mVersion = version;
    mCacheErrorLogger = cacheErrorLogger;
    mBaseDirectoryPathSupplier = baseDirectoryPathSupplier;
    mBaseDirectoryName = baseDirectoryName;
    mCurrentState = new State(null, null);
  }

  @Override
  public boolean isEnabled() {
    try {
      return get().isEnabled();
    } catch (IOException ioe) {
      return false;
    }
  }

  @Override
  public boolean isExternal() {
    try {
      return get().isExternal();
    } catch (IOException ioe) {
      return false;
    }
  }

  @Override
  public String getStorageName() {
    try {
      return get().getStorageName();
    } catch (IOException ioe) {
      return "";
    }
  }

  @Override
  @Nullable
  public com.facebook.binaryresource.BinaryResource getResource(String resourceId, Object debugInfo) throws IOException {
    return get().getResource(resourceId, debugInfo);
  }

  @Override
  public boolean contains(String resourceId, Object debugInfo) throws IOException {
    return get().contains(resourceId, debugInfo);
  }

  @Override
  public boolean touch(String resourceId, Object debugInfo) throws IOException {
    return get().touch(resourceId, debugInfo);
  }

  @Override
  public void purgeUnexpectedResources() {
    try {
      get().purgeUnexpectedResources();
    } catch (IOException ioe) {
      // this method in fact should throu IOException
      // for now we will swallow the exception as it's done in DefaultDiskStorage
      FLog.e(TAG, "purgeUnexpectedResources", ioe);
    }
  }

  @Override
  public DiskStorage.Inserter insert(String resourceId, Object debugInfo) throws IOException {
    return get().insert(resourceId, debugInfo);
  }

  @Override
  public Collection<DiskStorage.Entry> getEntries() throws IOException {
    return get().getEntries();
  }

  @Override
  public long remove(DiskStorage.Entry entry) throws IOException {
    return get().remove(entry);
  }

  @Override
  public long remove(String resourceId) throws IOException {
    return get().remove(resourceId);
  }

  @Override
  public void clearAll() throws IOException {
    get().clearAll();
  }

  @Override
  public DiskStorage.DiskDumpInfo getDumpInfo() throws IOException {
    return get().getDumpInfo();
  }

  /**
   * Gets a concrete disk-storage instance. If nothing has changed since the last call, then the
   * last state is returned
   * 
   * @return an instance of the appropriate DiskStorage class
   * @throws IOException
   * 
   *  package protected 
   */
  @VisibleForTesting
  synchronized DiskStorage get() throws IOException {
    if (shouldCreateNewStorage()) {
      // discard anything we created
      deleteOldStorageIfNecessary();
      createStorage();
    }
    return Preconditions.checkNotNull(mCurrentState.delegate);
  }

  private boolean shouldCreateNewStorage() {
    State currentState = mCurrentState;
    return (currentState.delegate == null
        || currentState.rootDirectory == null
        || !currentState.rootDirectory.exists());
  }

  @VisibleForTesting
  void deleteOldStorageIfNecessary() {
    if (mCurrentState.delegate != null && mCurrentState.rootDirectory != null) {
      // LATER: Actually delegate this call to the storage. We shouldn't be
      // making an end-run around it
      FileTree.deleteRecursively(mCurrentState.rootDirectory);
    }
  }

  private void createStorage() throws IOException {
    File rootDirectory = new File(mBaseDirectoryPathSupplier.get(), mBaseDirectoryName);
    createRootDirectoryIfNecessary(rootDirectory);
    DiskStorage storage = new DefaultDiskStorage(rootDirectory, mVersion, mCacheErrorLogger);
    mCurrentState = new State(rootDirectory, storage);
  }

  @VisibleForTesting
  void createRootDirectoryIfNecessary(File rootDirectory) throws IOException {
    try {
      FileUtils.mkdirs(rootDirectory);
    } catch (FileUtils.CreateDirectoryException cde) {
      mCacheErrorLogger.logError(
          CacheErrorLogger.CacheErrorCategory.WRITE_CREATE_DIR,
          TAG,
          "createRootDirectoryIfNecessary",
          cde);
      throw cde;
    }
    FLog.d(TAG, "Created cache directory %s", rootDirectory.getAbsolutePath());
  }

}