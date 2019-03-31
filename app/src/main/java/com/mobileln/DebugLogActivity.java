package com.mobileln;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.TextView;

import com.mobileln.bitcoind.Bitcoind;
import com.mobileln.lightningd.clightning.CLightningd;
import com.mobileln.utils.ProcessHelper;

public class DebugLogActivity extends AppCompatActivity {

    private static final String TAG = "DebugLogActivity";
    private ProcessHelper mProcessHelper;
    private TextView mTextView;
    private boolean mDone = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug_log);
        mTextView = findViewById(R.id.debug_log_textview);
        mTextView.setMovementMethod(new ScrollingMovementMethod());

        Intent intent = getIntent();
        String type = intent.getStringExtra("type");
        if ("bitcoind".equals(type)) {
            mProcessHelper = Bitcoind.getInstance();
        } else if ("lightningd".equals(type)) {
            mProcessHelper = CLightningd.getInstance();
        } else {
            Log.e(TAG, "unknown type: " + type);
            finish();
            return;
        }
        new Thread() {
            @Override
            public void run() {
                try {
                    while (!isInterrupted() && !mDone) {
                        Thread.sleep(200);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateTextView();
                            }
                        });
                    }
                } catch (InterruptedException e) {
                }
            }
        }.start();
    }

    private void updateTextView() {
        Log.i(TAG, "update");
        mTextView.setText(mProcessHelper.getOutput());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDone = true;
    }
}
