package com.hhh.liveeventbus;

import java.util.HashMap;
import java.util.Map;

import android.os.Message;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

public class LiveEventBus {

  private static volatile LiveEventBus DEFAULT_INSTANCE;

  private final Map<String, LiveEvent<Object>> mEventMap = new HashMap<>();

  @NonNull
  public static LiveEventBus getDefault() {
    if (DEFAULT_INSTANCE == null) {
      synchronized (LiveEventBus.class) {
        if (DEFAULT_INSTANCE == null) {
          DEFAULT_INSTANCE = new LiveEventBus();
        }
      }
    }
    return DEFAULT_INSTANCE;
  }

  /**
   * @param type 默认key为 type.getName()
   */
  @NonNull
  @MainThread
  public <T> Observable<T> with(@NonNull Class<T> type) {
    return with(type.getName(), type);
  }

  /**
   * 默认事件为Message，多用于进程间通信
   */
  @NonNull
  @MainThread
  public Observable<Message> with(@NonNull String key) {
    return with(key, Message.class);
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
