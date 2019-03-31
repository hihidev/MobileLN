package com.mobileln;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.mobileln.bitcoind.BitcoindConfig;
import com.mobileln.lightningd.clightning.CLightningdConfig;

public class CustomConfigActivity extends AppCompatActivity {

    private static final String TAG = "CustomConfigActivity";
    private EditText mCustomConfigContentEditText;
    private Button mOkBtn;
    private String mType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_config);
        mCustomConfigContentEditText = findViewById(R.id.debug_custom_config_content);
        mOkBtn = findViewById(R.id.debug_custom_config_ok_btn);
        mOkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveConfig();
                Toast.makeText(CustomConfigActivity.this, "Saved", Toast.LENGTH_SHORT).show();
            }
        });

        Intent intent = getIntent();
        String type = intent.getStringExtra("type");
        if ("lightningd".equals(type)) {
            mType = type;
        } else if ("bitcoind".equals(type)) {
            mType = type;
        } else {
            Log.e(TAG, "unknown type: " + type);
            finish();
            return;
        }
        loadConfig();
    }

    private void loadConfig() {
        try {
            final Map<String, String> map;
            if (mType.equals("lightningd")) {
                map = CLightningdConfig.readConfig(this);
            } else if (mType.equals("bitcoind")) {
                map = BitcoindConfig.readConfig(this);
            } else {
                Log.i(TAG, "WTF? unknown type: " + mType);
                return;
            }
            Properties properties = new Properties();
            properties.putAll(map);
            StringWriter writer = new StringWriter();
            properties.store(new PrintWriter(writer), null);
            String content = writer.getBuffer().toString();
            mCustomConfigContentEditText.setText(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveConfig() {
        try {
            String content = mCustomConfigContentEditText.getText().toString();
            Properties properties = new Properties();
            properties.load(new StringReader(content));
            HashMap<String, String> map = new HashMap<>();
            for (final String name : properties.stringPropertyNames()) {
                map.put(name, properties.getProperty(name));
            }

            if (mType.equals("lightningd")) {
                CLightningdConfig.saveConfig(this, map);
            } else if (mType.equals("bitcoind")) {
                BitcoindConfig.saveConfig(this, map);
            } else {
                Log.i(TAG, "WTF? unknown type: " + mType);
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
