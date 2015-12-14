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
import android.widget.TextView;

import java.util.Random;

public class AppActivity extends AppCompatActivity implements SensorEventListener {
  // class constants
  private static final float X_THRESHOLD = 0.02f;
  private static final float Y_THRESHOLD = 0.02f;
  private static final long HOLD_TIME = 400;
  private static final long VIBRATE_TIME = 100;
  private static final int TOTAL_TRIALS = 10;
  private static final int TOTAL_BUTTONS = 4;
  private static final int LIGHT_THRESHOLD = 80;

  private float prevLight;
  private long start, trialStart;
  private long lightPrevChange;
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

  @Override
  protected void onPause() {
    // Be sure to register the sensor when the activity pauses
    // Prevents the sensor from draining the battery when not in use
    super.onPause();
    sm.unregisterListener(this);
  }

  private void onLightChanged(SensorEvent event) {
    float light = event.values[0];

    TextView temp = (TextView) findViewById(R.id.light);
    temp.setText(String.format("Light Value: %f", light));

    if (light < LIGHT_THRESHOLD && !locked) {
      locked = true;
      lightPrevChange = System.currentTimeMillis();
    } else if (light < LIGHT_THRESHOLD) {
      if (light != prevLight) {
        if (System.currentTimeMillis() - lightPrevChange >= HOLD_TIME) {
          currAction = 2;
        }
      }
    } else if (light >= LIGHT_THRESHOLD && locked) {
      locked = false;
      // if user held the light down
      if (System.currentTimeMillis() - lightPrevChange >= HOLD_TIME) {
        currAction = 2;
      } else {
        currAction = 1;
      }
      if (currAction == targetAction && currButton == targetButton) {
        nextTrial();
      }
    }

    prevLight = event.values[0];
    updateScreen();
  }

  private void onRotationChanged(SensorEvent event) {
    if (locked) return;

    float x = event.values[0];
    float y = event.values[1];
    // might want to remove this later
    float z = event.values[2];

    TextView temp = (TextView) findViewById(R.id.rotation);
    temp.setText(String.format("Rotation Values - x : %f, y : %f, z : %f", x, y, z));

    if (x < -X_THRESHOLD && (currButton  == 1 || currButton == 3)) {
      currButton--;
      if (currButton == targetButton) {
        v.vibrate(VIBRATE_TIME);
      }
    } else if (x >= X_THRESHOLD && (currButton == 0 || currButton == 2)) {
      currButton++;
      if (currButton == targetButton) {
        v.vibrate(VIBRATE_TIME);
      }
    }

    if (y < -Y_THRESHOLD && (currButton == 0 || currButton == 1)) {
      currButton+=2;
      if (currButton == targetButton) {
        v.vibrate(VIBRATE_TIME);
      }
    } else if (y >= Y_THRESHOLD && (currButton == 2 || currButton == 3)) {
      currButton-=2;
      if (currButton == targetButton) {
        v.vibrate(VIBRATE_TIME);
      }
    }

    updateScreen();
  }

  private void updateScreen() {
    if (currAction == targetAction && currButton == targetButton && locked) {
      trialTimes[currTrial] = System.currentTimeMillis() - trialStart;
      if (currTrial >= TOTAL_TRIALS) {
        Intent intent = new Intent(AppActivity.this, ResultsActivity.class);
        intent.putExtra("time", System.currentTimeMillis() - start);
        intent.putExtra("trials", TOTAL_TRIALS);
        intent.putExtra("results", trialTimes);
        AppActivity.this.startActivity(intent);
      } else {
        nextTrial();
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

  private void nextTrial() {
    trialTimes[currTrial] = System.currentTimeMillis() - trialStart;
    v.vibrate(100);
    trialStart = System.currentTimeMillis();
    targetButton = random.nextInt(TOTAL_BUTTONS);
    targetAction = random.nextBoolean() ? 1 : 2;
    currButton = currAction = 0;
    locked = false;
    currTrial++;
    if (currTrial >= TOTAL_TRIALS) {
      Intent intent = new Intent(AppActivity.this, ResultsActivity.class);
      intent.putExtra("time", System.currentTimeMillis() - start);
      intent.putExtra("trials", TOTAL_TRIALS);
      intent.putExtra("results", trialTimes);
      AppActivity.this.startActivity(intent);
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
