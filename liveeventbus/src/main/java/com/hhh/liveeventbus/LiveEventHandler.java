package com.hhh.liveeventbus;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import androidx.annotation.NonNull;

public class LiveEventHandler extends Handler {

  private final LiveEventBus mLiveEventBus;

  public LiveEventHandler(@NonNull LiveEventBus liveEventBus) {
    mLiveEventBus = liveEventBus;
  }

  @Override
  public void handleMessage(Message msg) {
    String key;
    if (msg.obj != null) {
      key = msg.obj.getClass().getName();
    } else {
      Bundle data = msg.getData();
      data.setClassLoader(getClass().getClassLoader());
      key = data.getString(LiveEventBus.KEY_IPC_EVENT_KEY);
    }
    if (TextUtils.isEmpty(key)) {
      throw new RuntimeException("ipc event key cannot be empty!!!");
    }
    mLiveEventBus.with(key).post(msg);
  }
}
