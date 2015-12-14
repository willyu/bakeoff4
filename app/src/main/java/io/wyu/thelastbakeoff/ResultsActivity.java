package io.wyu.thelastbakeoff;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ScrollView;
import android.widget.TextView;

public class ResultsActivity extends AppCompatActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Log.i("ResultsActivity", "Made it to the results page!");
    super.onCreate(savedInstanceState);
    setContentView(R.layout.results_main);
    Intent intent = getIntent();

    long time = intent.getLongExtra("time", 0);
    int trials = intent.getIntExtra("trials", 3);
    long[] results = intent.getLongArrayExtra("results");

    String result = String.format("Total time taken: %d milliseconds\n", time);
    result += String.format("Time per trial: %d milliseconds\n", time/trials);

    for (int i = 1; i <= results.length; i++) {
      result += String.format("Trial %d: %d milliseconds\n", i, results[i-1]);
    }

    TextView temp = (TextView) findViewById(R.id.results);
    temp.setText(result);
  }
}
