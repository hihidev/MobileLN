package com.mobileln;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.mobileln.lightningd.LightningClient;
import com.mobileln.utils.CircularArray;

public class DebugConsoleActivity extends AppCompatActivity {

    private static final String TAG = "DebugConsoleActivity";
    private TextView mDebugConsoleResult;
    private TextView mDebugConsoleArguments;
    private CircularArray mCircularArray = new CircularArray(30);
    private String mType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug_console);
        mDebugConsoleResult = findViewById(R.id.debug_console_result);
        mDebugConsoleArguments = findViewById(R.id.debug_console_arguments);

        Intent intent = getIntent();
        String type = intent.getStringExtra("type");
        if ("lightning-cli".equals(type)) {
            mType = type;
        } else {
            Log.e(TAG, "unknown type: " + type);
            finish();
            return;
        }
        ((TextView) findViewById(R.id.debug_console_name)).setText(type);
        findViewById(R.id.debug_console_ok_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String argumentsStr = mDebugConsoleArguments.getText().toString();
                String[] arguments = argumentsStr.split(" ");
                runQuery(arguments);
            }
        });
    }

    private void runQuery(final String[] arguments) {
        new AsyncTask<Void, Void, String>() {

            @Override
            protected String doInBackground(Void... voids) {
                return LightningClient.newInstance(true).rawQuery(DebugConsoleActivity.this, arguments);
            }
            @Override
            public void onPostExecute(String str) {
                updateResult(str);
            }
        }.execute();
    }

    private void updateResult(String result) {
        Log.i(TAG, "update");
        mCircularArray.add(result);
        mDebugConsoleResult.setText(mCircularArray.get());
    }
}
