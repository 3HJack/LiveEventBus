/*
 * Copyright (C) 2017 The Android Open Source Project
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hhh.liveeventbus;

import static androidx.lifecycle.Lifecycle.State.DESTROYED;
import static androidx.lifecycle.Lifecycle.State.STARTED;

import java.util.Iterator;
import java.util.Map;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.internal.SafeIterableMap;
import androidx.lifecycle.GenericLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

/**
 * LiveEventData is a data holder class that can be observed within a given lifecycle.
 * This means that an {@link Observer} can be added in a pair with a {@link LifecycleOwner}, and
 * this observer will be notified about modifications of the wrapped data only if the paired
 * LifecycleOwner is in active state. LifecycleOwner is considered as active, if its state is
 * {@link Lifecycle.State#STARTED} or {@link Lifecycle.State#RESUMED}. An observer added via
 * {@link #observeForever(Observer)} is considered as always active and thus will be always notified
 * about modifications. For those observers, you should manually call
 * {@link #removeObserver(Observer)}.
 *
 * <p>
 * An observer added with a Lifecycle will be automatically removed if the corresponding
 * Lifecycle moves to {@link Lifecycle.State#DESTROYED} state. This is especially useful for
 * activities and fragments where they can safely observe LiveEventData and not worry about leaks:
 * they will be instantly unsubscribed when they are destroyed.
 *
 * <p>
 * In addition, LiveEventData has {@link LiveEventData#onActive()} and
 * {@link LiveEventData#onInactive()} methods
 * to get notified when number of active {@link Observer}s change between 0 and 1.
 * This allows LiveEventData to release any heavy resources when it does not have any Observers that
 * are actively observing.
 * <p>
 * This class is designed for sharing data between different modules in your application
 * in a decoupled fashion.
 *
 * @param <T> The type of data held by this instance
 */
class LiveEventData<T> {
  static final int START_VERSION = -1;
  private static final Object NOT_SET = new Object();

  private final Object mDataLock = new Object();
  private final SafeIterableMap<Observer<T>, ObserverWrapper> mObservers = new SafeIterableMap<>();

  private int mVersion = START_VERSION;
  // how many observers are in active state
  private int mActiveCount = 0;
  private volatile Object mData = NOT_SET;
  // when setData is called, we set the pending data and actual data swap happens on the main thread
  private volatile Object mPendingData = NOT_SET;

  private boolean mDispatchingValue;
  @SuppressWarnings("FieldCanBeLocal")
  private boolean mDispatchInvalidated;
  private final Runnable mPostValueRunnable = () -> {
    Object newValue;
    synchronized (mDataLock) {
      newValue = mPendingData;
      mPendingData = NOT_SET;
    }
    // noinspection unchecked
    setValue((T) newValue);
  };

  private static void assertMainThread(String methodName) {
    if (!ArchTaskExecutor.getInstance().isMainThread()) {
      throw new IllegalStateException(
          "Cannot invoke " + methodName + " on a background" + " thread");
    }
  }

  private void considerNotify(ObserverWrapper observer) {
    if (!observer.mActive) {
      return;
    }
    // Check latest state b4 dispatch. Maybe it changed state but we didn't get the event yet.
    //
    // we still first check observer.active to keep it as the entrance for events. So even if
    // the observer moved to an active state, if we've not received that event, we better not
    // notify for a more predictable notification order.
    if (!observer.shouldBeActive()) {
      observer.activeStateChanged(false);
      return;
    }
    if (observer.mLastVersion >= mVersion) {
      return;
    }
    observer.mLastVersion = mVersion;
    // noinspection unchecked
    observer.mObserver.onChanged((T) mData);
  }

  private void dispatchingValue(@Nullable ObserverWrapper initiator) {
    if (mDispatchingValue) {
      mDispatchInvalidated = true;
      return;
    }
    mDispatchingValue = true;
    do {
      mDispatchInvalidated = false;
      if (initiator != null) {
        considerNotify(initiator);
        initiator = null;
      } else {
        for (Iterator<Map.Entry<Observer<T>, ObserverWrapper>> iterator =
            mObservers.iteratorWithAdditions(); iterator.hasNext();) {
          considerNotify(iterator.next().getValue());
          if (mDispatchInvalidated) {
            break;
          }
        }
      }
    } while (mDispatchInvalidated);
    mDispatchingValue = false;
  }

  /**
   * Adds the given observer to the observers list within the lifespan of the given
   * owner. The events are dispatched on the main thread. If LiveEventData already has data
   * set, it will be delivered to the observer.
   * <p>
   * The observer will only receive events if the owner is in {@link Lifecycle.State#STARTED}
   * or {@link Lifecycle.State#RESUMED} state (active).
   * <p>
   * If the owner moves to the {@link Lifecycle.State#DESTROYED} state, the observer will
   * automatically be removed.
   * <p>
   * When data changes while the {@code owner} is not active, it will not receive any updates.
   * If it becomes active again, it will receive the last available data automatically.
   * <p>
   * LiveEventData keeps a strong reference to the observer and the owner as long as the
   * given LifecycleOwner is not destroyed. When it is destroyed, LiveEventData removes references
   * to
   * the observer &amp; the owner.
   * <p>
   * If the given owner is already in {@link Lifecycle.State#DESTROYED} state, LiveEventData
   * ignores the call.
   * <p>
   * If the given owner, observer tuple is already in the list, the call is ignored.
   * If the observer is already in the list with another owner, LiveEventData throws an
   * {@link IllegalArgumentException}.
   *
   * @param owner The LifecycleOwner which controls the observer
   * @param observer The observer that will receive the events
   */
  @MainThread
  public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<T> observer) {
    if (owner.getLifecycle().getCurrentState() == DESTROYED) {
      // ignore
      return;
    }
    LifecycleBoundObserver wrapper = new LifecycleBoundObserver(owner, observer);
    ObserverWrapper existing = mObservers.putIfAbsent(observer, wrapper);
    if (existing != null && !existing.isAttachedTo(owner)) {
      throw new IllegalArgumentException(
          "Cannot add the same observer" + " with different lifecycles");
    }
    if (existing != null) {
      return;
    }
    owner.getLifecycle().addObserver(wrapper);
  }

  /**
   * Adds the given observer to the observers list. This call is similar to
   * {@link LiveEventData#observe(LifecycleOwner, Observer)} with a LifecycleOwner, which
   * is always active. This means that the given observer will receive all events and will never
   * be automatically removed. You should manually call {@link #removeObserver(Observer)} to stop
   * observing this LiveEventData.
   * While LiveEventData has one of such observers, it will be considered
   * as active.
   * <p>
   * If the observer was already added with an owner to this LiveEventData, LiveEventData throws an
   * {@link IllegalArgumentException}.
   *
   * @param observer The observer that will receive the events
   */
  @MainThread
  public void observeForever(@NonNull Observer<T> observer) {
    AlwaysActiveObserver wrapper = new AlwaysActiveObserver(observer);
    ObserverWrapper existing = mObservers.putIfAbsent(observer, wrapper);
    if (existing instanceof LiveEventData.LifecycleBoundObserver) {
      throw new IllegalArgumentException(
          "Cannot add the same observer" + " with different lifecycles");
    }
    if (existing != null) {
      return;
    }
    wrapper.activeStateChanged(true);
  }

  /**
   * Removes the given observer from the observers list.
   *
   * @param observer The Observer to receive events.
   */
  @MainThread
  public void removeObserver(@NonNull final Observer<T> observer) {
    assertMainThread("removeObserver");
    ObserverWrapper removed = mObservers.remove(observer);
    if (removed == null) {
      return;
    }
    removed.detachObserver();
    removed.activeStateChanged(false);
  }

  /**
   * Removes all observers that are tied to the given {@link LifecycleOwner}.
   *
   * @param owner The {@code LifecycleOwner} scope for the observers to be removed.
   */
  @SuppressWarnings("WeakerAccess")
  @MainThread
  public void removeObservers(@NonNull final LifecycleOwner owner) {
    assertMainThread("removeObservers");
    for (Map.Entry<Observer<T>, ObserverWrapper> entry : mObservers) {
      if (entry.getValue().isAttachedTo(owner)) {
        removeObserver(entry.getKey());
      }
    }
  }

  /**
   * Posts a task to a main thread to set the given value. So if you have a following code
   * executed in the main thread:
   * 
   * <pre class="prettyprint">
   * liveData.postValue("a");
   * liveData.setValue("b");
   * </pre>
   * 
   * The value "b" would be set at first and later the main thread would override it with
   * the value "a".
   * <p>
   * If you called this method multiple times before a main thread executed a posted task, only
   * the last value would be dispatched.
   *
   * @param value The new value
   */
  protected void postValue(T value) {
    boolean postTask;
    synchronized (mDataLock) {
      postTask = mPendingData == NOT_SET;
      mPendingData = value;
    }
    if (!postTask) {
      return;
    }
    ArchTaskExecutor.getInstance().postToMainThread(mPostValueRunnable);
  }

  /**
   * Returns the current value.
   * Note that calling this method on a background thread does not guarantee that the latest
   * value set will be received.
   *
   * @return the current value
   */
  @Nullable
  public T getValue() {
    Object data = mData;
    if (data != NOT_SET) {
      // noinspection unchecked
      return (T) data;
    }
    return null;
  }

  /**
   * Sets the value. If there are active observers, the value will be dispatched to them.
   * <p>
   * This method must be called from the main thread. If you need set a value from a background
   * thread, you can use {@link #postValue(Object)}
   *
   * @param value The new value
   */
  @MainThread
  protected void setValue(T value) {
    assertMainThread("setValue");
    mVersion++;
    mData = value;
    dispatchingValue(null);
  }

  int getVersion() {
    return mVersion;
  }

  /**
   * Called when the number of active observers change to 1 from 0.
   * <p>
   * This callback can be used to know that this LiveEventData is being used thus should be kept
   * up to date.
   */
  protected void onActive() {

  }

  /**
   * Called when the number of active observers change from 1 to 0.
   * <p>
   * This does not mean that there are no observers left, there may still be observers but their
   * lifecycle states aren't {@link Lifecycle.State#STARTED} or {@link Lifecycle.State#RESUMED}
   * (like an Activity in the back stack).
   * <p>
   * You can check if there are observers via {@link #hasObservers()}.
   */
  protected void onInactive() {

  }

  /**
   * Returns true if this LiveEventData has observers.
   *
   * @return true if this LiveEventData has observers
   */
  @SuppressWarnings("WeakerAccess")
  public boolean hasObservers() {
    return mObservers.size() > 0;
  }

  /**
   * Returns true if this LiveEventData has active observers.
   *
   * @return true if this LiveEventData has active observers
   */
  @SuppressWarnings("WeakerAccess")
  public boolean hasActiveObservers() {
    return mActiveCount > 0;
  }

  class LifecycleBoundObserver extends ObserverWrapper implements GenericLifecycleObserver {
    @NonNull
    final LifecycleOwner mOwner;

    LifecycleBoundObserver(@NonNull LifecycleOwner owner, Observer<T> observer) {
      super(observer);
      mOwner = owner;
    }

    @Override
    boolean shouldBeActive() {
      return mOwner.getLifecycle().getCurrentState().isAtLeast(STARTED);
    }

    @Override
    public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {
      if (mOwner.getLifecycle().getCurrentState() == DESTROYED) {
        removeObserver(mObserver);
        return;
      }
      activeStateChanged(shouldBeActive());
    }

    @Override
    boolean isAttachedTo(LifecycleOwner owner) {
      return mOwner == owner;
    }

    @Override
    void detachObserver() {
      mOwner.getLifecycle().removeObserver(this);
    }
  }

  private abstract class ObserverWrapper {
    final Observer<T> mObserver;
    boolean mActive;
    int mLastVersion = START_VERSION;

    ObserverWrapper(Observer<T> observer) {
      mObserver = observer;
    }

    abstract boolean shouldBeActive();

    boolean isAttachedTo(LifecycleOwner owner) {
      return false;
    }

    void detachObserver() {}

    void activeStateChanged(boolean newActive) {
      if (newActive == mActive) {
        return;
      }
      // immediately set active state, so we'd never dispatch anything to inactive
      // owner
      mActive = newActive;
      boolean wasInactive = LiveEventData.this.mActiveCount == 0;
      LiveEventData.this.mActiveCount += mActive ? 1 : -1;
      if (wasInactive && mActive) {
        onActive();
      }
      if (LiveEventData.this.mActiveCount == 0 && !mActive) {
        onInactive();
      }
      if (mActive) {
        dispatchingValue(this);
      }
    }
  }

  private class AlwaysActiveObserver extends ObserverWrapper {

    AlwaysActiveObserver(Observer<T> observer) {
      super(observer);
    }

    @Override
    boolean shouldBeActive() {
      return true;
    }
  }
}
