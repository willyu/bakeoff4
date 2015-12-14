package io.wyu.thelastbakeoff;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.util.Random;

public class AppActivity extends AppCompatActivity implements SensorEventListener {
  // class constants
  private static final int TOTAL_TRIALS = 10;
  private static final int TOTAL_BUTTONS = 4;

  private float prevZ = 1.0f;
  private long start, trialStart;
  private long[] trialTimes;
  private int currTrial, currAction;
  private int targetButton, targetAction;
  private int currButton;
  private boolean locked;

  private SensorManager sm;
  private Sensor sl;
  private Sensor sr;

  private Vibrator v;

  private Random random = new Random();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.app_main);

    start = trialStart = System.currentTimeMillis();
    trialTimes = new long[TOTAL_TRIALS];
    currTrial = 0;
    targetButton = random.nextInt(TOTAL_BUTTONS);
    targetAction = random.nextBoolean() ? 1 : 2;
    currButton = currAction = 0;
    locked = false;

    sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    sl = sm.getDefaultSensor(Sensor.TYPE_LIGHT);
    sr = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

    v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
  }

  @Override
  public final void onAccuracyChanged(Sensor sensor, int accuracy) {}

  @Override
  public final void onSensorChanged(SensorEvent event) {
    Sensor source = event.sensor;

    if (source.getType() == Sensor.TYPE_LIGHT) {
      onLightChanged(event);
    } else if (source.getType() == Sensor.TYPE_ROTATION_VECTOR) {
      onRotationChanged(event);
    }
  }

  @Override
  protected void onResume() {
    // Register a listener for our sensors
    super.onResume();
    sm.registerListener(this, sl, SensorManager.SENSOR_DELAY_NORMAL);
    sm.registerListener(this, sr, SensorManager.SENSOR_DELAY_NORMAL);
  }

  protected void onPause() {
    // Be sure to register the sensor when the activity pauses
    // Prevents the sensor from draining the battery when not in use
    super.onPause();
    sm.unregisterListener(this);
  }

  private void onLightChanged(SensorEvent event) {
    float light = event.values[0];

    // unlock and go to next button
    if (locked && light > 20) {
      locked = false;
      currButton = (currButton + 1) % TOTAL_BUTTONS;
      if (currButton == targetButton) {
        v.vibrate(200);
      }
    // lock the current button
    } else if (!locked && light <= 20) {
      locked = true;
      if (currButton == targetButton) {
        v.vibrate(500);
      }
    }

    updateScreen();
  }

  private void onRotationChanged(SensorEvent event) {
    // have to fix this initialization case somehow
    if (prevZ == 1.0) {
      prevZ = event.values[2];
    }

    // trying this out: if the user flips the phone "upwards"
    if (prevZ - event.values[2] < -.03) {
      if (currButton == targetButton && locked) {
        currAction = 2;
      }
    // and downwards
    } else if (prevZ - event.values[2] > .025) {
      if (currButton == targetButton && locked) {
        currAction = 1;
      }
    }

    prevZ = event.values[2];
    updateScreen();
  }

  private void updateScreen() {
    if (currAction == targetAction && currButton == targetButton && locked) {
      trialTimes[currTrial] = System.currentTimeMillis() - trialStart;
      currTrial++;
      if (currTrial >= TOTAL_TRIALS) {
        Intent intent = new Intent(AppActivity.this, ResultsActivity.class);
        intent.putExtra("time", System.currentTimeMillis() - start);
        intent.putExtra("trials", TOTAL_TRIALS);
        intent.putExtra("results", trialTimes);
        AppActivity.this.startActivity(intent);
      } else {
        trialStart = System.currentTimeMillis();
        targetButton = random.nextInt(TOTAL_BUTTONS);
        targetAction = random.nextBoolean() ? 1 : 2;
        currButton = currAction = 0;
        locked = false;
      }
    }
    updateButtons();

  }

  private void updateButtons() {

    // getTargetColor() and getTargetString()
    for (int i = 0; i < TOTAL_BUTTONS; i++) {
      Button temp = (Button) findViewById(getButton(i));
      if (i == targetButton) {
        temp.setBackgroundColor(getTargetColor());
        temp.setText(getTargetString());
      // should only reach this point if currButton != targetButton
      } else if (i == currButton) {
        temp.setBackgroundColor(Color.parseColor("#BBBBBB"));
        temp.setText(String.valueOf(i + 1));
      } else {
        temp.setBackgroundColor(Color.parseColor("#666666"));
        temp.setText(String.valueOf(i + 1));
      }
    }
  }

  // wrapper/helper functions
  private int getButton(int b) {
    if (b == 0) {
      return R.id.button_1;
    } else if (b == 1) {
      return R.id.button_2;
    } else if (b == 2) {
      return R.id.button_3;
    } else if (b == 3) {
      return R.id.button_4;
    } else {
      return -1;
    }
  }

  private int getTargetColor() {
    if (currButton == targetButton) {
      return Color.parseColor("#F50057");
    }
    return Color.parseColor("#00E5FF");
  }

  private String getTargetString() {
    if (targetAction == 1) {
      return "tap";
    }
    return "hold";
  }

}
