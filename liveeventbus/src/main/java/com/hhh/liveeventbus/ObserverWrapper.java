package com.hhh.liveeventbus;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;

class ObserverWrapper<T> implements Observer<T> {

  private final Observer<T> mObserver;
  boolean mPreventNextEvent;

  ObserverWrapper(@NonNull Observer<T> observer) {
    mObserver = observer;
  }

  @Override
  public void onChanged(@Nullable T t) {
    if (mPreventNextEvent) {
      mPreventNextEvent = false;
      return;
    }
    mObserver.onChanged(t);
  }
}
