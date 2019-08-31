package com.hhh.liveeventbus;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

public class LiveEventBus {

  private static final LiveEventBus LIVE_EVENT_BUS = new LiveEventBus();

  private final Map<String, LiveEvent<Object>> mEventMap = new HashMap<>();

  private LiveEventBus() {}

  @NonNull
  public static LiveEventBus getInstance() {
    return LIVE_EVENT_BUS;
  }

  @NonNull
  @MainThread
  public <T> Observable<T> with(@NonNull Class<T> type) {
    return with(type.getName(), type);
  }

  /**
   * 之所以要求在主线程，是不想加锁，只要不是高并发，在子线程也不会有问题
   */
  @NonNull
  @MainThread
  public <T> Observable<T> with(@NonNull String key, @NonNull Class<T> type) {
    if (!mEventMap.containsKey(key)) {
      mEventMap.put(key, new LiveEvent<>());
    }
    return (Observable<T>) mEventMap.get(key);
  }
}
