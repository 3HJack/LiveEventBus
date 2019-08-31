package com.hhh.liveeventbus;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

public interface Observable<T> {

  /**
   * 发送一个消息，支持前台线程、后台线程发送
   */
  void post(@Nullable T value);

  /**
   * 延迟发送一个消息，支持前台线程、后台线程发送
   */
  void postDelay(@Nullable T value, long delay);

  /**
   * 发送一个消息，支持前台线程、后台线程发送
   * 接收到消息的顺序和发送顺序一致
   */
  void postOrderly(@Nullable T value);

  /**
   * 注册一个Observer，生命周期感知，自动取消订阅
   */
  void observe(@NonNull LifecycleOwner owner, @NonNull Observer<T> observer);

  /**
   * 注册一个Observer，生命周期感知，自动取消订阅
   * 如果之前有消息发送，可以在注册时收到消息（消息同步）
   */
  void observeSticky(@NonNull LifecycleOwner owner, @NonNull Observer<T> observer);

  /**
   * 注册一个Observer
   */
  void observeForever(@NonNull Observer<T> observer);

  /**
   * 注册一个Observer
   * 如果之前有消息发送，可以在注册时收到消息（消息同步）
   */
  void observeStickyForever(@NonNull Observer<T> observer);

  /**
   * 通过observeForever或observeStickyForever注册的，需要调用该方法取消订阅
   */
  void removeObserver(@NonNull Observer<T> observer);
}
