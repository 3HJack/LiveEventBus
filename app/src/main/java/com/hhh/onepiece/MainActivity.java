package com.hhh.onepiece;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import com.hhh.liveeventbus.LiveEventBus;

public class MainActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    testSendEvent();
    testAcceptEvent(this);
  }

  private void testSendEvent() {
    findViewById(R.id.post).setOnClickListener(view -> LiveEventBus.getDefault()
        .with(TestEvent.class).post(new TestEvent("post")));
    findViewById(R.id.post_delay).setOnClickListener(view -> LiveEventBus.getDefault()
        .with(TestEvent.class).postDelay(new TestEvent("postDelay"), 1000L));
    findViewById(R.id.post_orderly).setOnClickListener(view -> LiveEventBus.getDefault()
        .with(TestEvent.class).postOrderly(new TestEvent("postOrderly")));
  }

  private void testAcceptEvent(FragmentActivity activity) {
    findViewById(R.id.observe).setOnClickListener(
        view -> LiveEventBus.getDefault().with(TestEvent.class).observe(activity,
            testEvent -> Toast.makeText(activity, testEvent.mEvent, Toast.LENGTH_SHORT).show()));
    findViewById(R.id.observe_sticky).setOnClickListener(view -> LiveEventBus.getDefault()
        .with(TestEvent.class).observeSticky(activity, testEvent -> Toast
            .makeText(activity, "stick: " + testEvent.mEvent, Toast.LENGTH_SHORT).show()));
    findViewById(R.id.observe_forever).setOnClickListener(
        view -> LiveEventBus.getDefault().with(TestEvent.class).observeForever(testEvent -> Toast
            .makeText(activity, "forever: " + testEvent.mEvent, Toast.LENGTH_SHORT).show()));
    findViewById(R.id.observe_sticky_forever).setOnClickListener(view -> LiveEventBus.getDefault()
        .with(TestEvent.class).observeStickyForever(testEvent -> Toast
            .makeText(activity, "sticky_forever: " + testEvent.mEvent, Toast.LENGTH_SHORT).show()));
  }
}
