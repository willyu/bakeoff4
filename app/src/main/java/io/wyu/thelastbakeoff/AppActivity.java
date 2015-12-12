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
    if (prevZ - event.values[2] < -.07) {
      if (currButton == targetButton && locked) {
        currAction = 2;
      }
    // and downwards
    } else if (prevZ - event.values[2] > .05) {
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
      //@TODO: Fix terrible style
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

        for (int i = 0; i < TOTAL_BUTTONS; i++) {
          Button temp = (Button) findViewById(getButton(i));
          //@TODO: This is mega messy, clean up logic later
          if (i == targetButton && currButton != targetButton) {
            temp.setBackgroundColor(Color.parseColor("#00E5FF"));
            temp.setText("");
          } else if (i == currButton) {
            if (currButton == targetButton && locked) {
              temp.setBackgroundColor(Color.parseColor("#F50057"));
              if (targetAction == 1) {
                temp.setText("up");
              } else {
                temp.setText("down");
              }
            } else if (currButton == targetButton && !locked) {
              temp.setBackgroundColor(Color.parseColor("#00E676"));
              if (targetAction == 1) {
                temp.setText("up");
              } else {
                temp.setText("down");
              }
            } else {
              temp.setBackgroundColor(Color.parseColor("#BBBBBB"));
              temp.setText("");
            }
          } else {
            temp.setBackgroundColor(Color.parseColor("#888888"));
            temp.setText("");
          }
        }
      }
    }

    for (int i = 0; i < TOTAL_BUTTONS; i++) {
      Button temp = (Button) findViewById(getButton(i));
      //@TODO: This is mega messy, clean up logic later
      if (i == targetButton && currButton != targetButton) {
        temp.setBackgroundColor(Color.parseColor("#00E5FF"));
        temp.setText("");
      } else if (i == currButton) {
        if (currButton == targetButton && locked) {
          temp.setBackgroundColor(Color.parseColor("#F50057"));
          if (targetAction == 1) {
            temp.setText("up");
          } else {
            temp.setText("down");
          }
        } else if (currButton == targetButton && !locked) {
          temp.setBackgroundColor(Color.parseColor("#00E676"));
          if (targetAction == 1) {
            temp.setText("up");
          } else {
            temp.setText("down");
          }
        } else {
          temp.setBackgroundColor(Color.parseColor("#BBBBBB"));
          temp.setText("");
        }
      } else {
        temp.setBackgroundColor(Color.parseColor("#666666"));
        temp.setText("");
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

}
