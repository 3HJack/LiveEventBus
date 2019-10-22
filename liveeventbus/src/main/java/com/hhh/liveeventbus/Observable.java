package com.hhh.liveeventbus;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

public interface Observable<T> {

  /**
   * 发送一个消息，支持前、后台线程发送
   */
  void post(@Nullable T value);

  /**
   * 延迟发送一个消息，支持前、后台线程发送
   */
  void postDelay(@Nullable T value, long delay);

  /**
   * 发送一个消息，支持前、后台线程发送
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
   * 注册一个Observer，不会自动反注册，如果不是进程级的监听，需要手动调用 removeObserver 反注册，否则会有内存泄漏
   * 请优先使用 {@link #observe(LifecycleOwner, Observer)}
   */
  void observeForever(@NonNull Observer<T> observer);

  /**
   * 注册一个Observer，不会自动反注册，如果不是进程级的监听，需要手动调用 removeObserver 反注册，否则会有内存泄漏
   * 请优先使用 {@link #observeSticky(LifecycleOwner, Observer)}
   *
   * 如果之前有消息发送，可以在注册时收到消息（消息同步）
   */
  void observeStickyForever(@NonNull Observer<T> observer);

  /**
   * 通过 observeForever 或 observeStickyForever 注册的，可以调用该方法取消订阅
   */
  void removeObserver(@NonNull Observer<T> observer);
}
