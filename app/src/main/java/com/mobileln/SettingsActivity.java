package com.mobileln;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        findViewById(R.id.settings_bitcoind_log).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SettingsActivity.this, DebugLogActivity.class);
                intent.putExtra("type", "bitcoind");
                startActivity(intent);
            }
        });
        findViewById(R.id.settings_lightningd_log).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SettingsActivity.this, DebugLogActivity.class);
                intent.putExtra("type", "lightningd");
                startActivity(intent);
            }
        });
        findViewById(R.id.settings_lightning_cli_console).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SettingsActivity.this, DebugConsoleActivity.class);
                intent.putExtra("type", "lightning-cli");
                startActivity(intent);
            }
        });
        findViewById(R.id.settings_bitcoind_config).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SettingsActivity.this, CustomConfigActivity.class);
                intent.putExtra("type", "bitcoind");
                startActivity(intent);
            }
        });
        findViewById(R.id.settings_lightningd_config).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SettingsActivity.this, CustomConfigActivity.class);
                intent.putExtra("type", "lightningd");
                startActivity(intent);
            }
        });
    }
}
