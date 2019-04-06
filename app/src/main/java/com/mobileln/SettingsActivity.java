package com.mobileln;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.customtabs.CustomTabsIntent;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.mobileln.lightningd.LightningClient;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        findViewById(R.id.settings_about_me).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                CustomTabsIntent customTabsIntent = builder.build();
                customTabsIntent.launchUrl(SettingsActivity.this,
                        Uri.parse("https://github.com/hihidev/MobileLN"));
            }
        });
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
                String type = LightningClient.useLnd() ? "lnd" : "clightningd";
                intent.putExtra("type", type);
                startActivity(intent);
            }
        });
        findViewById(R.id.settings_lightning_cli_console).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SettingsActivity.this, DebugConsoleActivity.class);
                String type = LightningClient.useLnd() ? "lncli" : "lightning-cli";
                intent.putExtra("type", type);
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
                String type = LightningClient.useLnd() ? "lnd" : "clightningd";
                intent.putExtra("type", type);
                startActivity(intent);
            }
        });
    }
}
