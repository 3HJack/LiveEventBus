package com.hhh.liveeventbus;

import java.util.HashMap;
import java.util.Map;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

class LiveEvent<T> implements Observable<T> {

  private static final Handler HANDLER = new Handler(Looper.getMainLooper());
  private final LiveEventData<T> mLiveData = new LiveEventData<>();
  private final Map<Observer, ObserverWrapper<T>> mObserverMap = new HashMap<>();

  private static boolean isMainThread() {
    return Looper.getMainLooper().getThread() == Thread.currentThread();
  }

  @Override
  public void post(@Nullable T value) {
    if (isMainThread()) {
      postInternal(value);
    } else {
      HANDLER.post(() -> postInternal(value));
    }
  }

  @Override
  public void postDelay(@Nullable T value, long delay) {
    HANDLER.postDelayed(() -> postInternal(value), delay);
  }

  @Override
  public void postOrderly(@Nullable T value) {
    HANDLER.post(() -> postInternal(value));
  }

  @Override
  public void observe(@NonNull final LifecycleOwner owner, @NonNull final Observer<T> observer) {
    if (isMainThread()) {
      observeInternal(owner, observer);
    } else {
      HANDLER.post(() -> observeInternal(owner, observer));
    }
  }

  @Override
  public void observeSticky(@NonNull final LifecycleOwner owner,
      @NonNull final Observer<T> observer) {
    if (isMainThread()) {
      observeStickyInternal(owner, observer);
    } else {
      HANDLER.post(() -> observeStickyInternal(owner, observer));
    }
  }

  @Override
  public void observeForever(@NonNull final Observer<T> observer) {
    if (isMainThread()) {
      observeForeverInternal(observer);
    } else {
      HANDLER.post(() -> observeForeverInternal(observer));
    }
  }

  @Override
  public void observeStickyForever(@NonNull final Observer<T> observer) {
    if (isMainThread()) {
      observeStickyForeverInternal(observer);
    } else {
      HANDLER.post(() -> observeStickyForeverInternal(observer));
    }
  }

  @Override
  public void removeObserver(@NonNull final Observer<T> observer) {
    if (isMainThread()) {
      removeObserverInternal(observer);
    } else {
      HANDLER.post(() -> removeObserverInternal(observer));
    }
  }

  @MainThread
  private void postInternal(T value) {
    mLiveData.setValue(value);
  }

  @MainThread
  private void observeInternal(@NonNull LifecycleOwner owner, @NonNull Observer<T> observer) {
    ObserverWrapper<T> observerWrapper = new ObserverWrapper<>(observer);
    observerWrapper.mPreventNextEvent = mLiveData.getVersion() > LiveEventData.START_VERSION;
    mLiveData.observe(owner, observerWrapper);
  }

  @MainThread
  private void observeStickyInternal(@NonNull LifecycleOwner owner, @NonNull Observer<T> observer) {
    mLiveData.observe(owner, new ObserverWrapper<>(observer));
  }

  @MainThread
  private void observeForeverInternal(@NonNull Observer<T> observer) {
    ObserverWrapper<T> observerWrapper = new ObserverWrapper<>(observer);
    observerWrapper.mPreventNextEvent = mLiveData.getVersion() > LiveEventData.START_VERSION;
    mObserverMap.put(observer, observerWrapper);
    mLiveData.observeForever(observerWrapper);
  }

  @MainThread
  private void observeStickyForeverInternal(@NonNull Observer<T> observer) {
    ObserverWrapper<T> observerWrapper = new ObserverWrapper<>(observer);
    mObserverMap.put(observer, observerWrapper);
    mLiveData.observeForever(observerWrapper);
  }

  @MainThread
  private void removeObserverInternal(@NonNull Observer<T> observer) {
    if (mObserverMap.containsKey(observer)) {
      observer = mObserverMap.remove(observer);
    }
    mLiveData.removeObserver(observer);
  }
}
