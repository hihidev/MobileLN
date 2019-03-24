package com.mobileln;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ContextThemeWrapper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.util.Map;

import com.mobileln.bitcoind.BitcoindConfig;
import com.mobileln.bitcoind.BitcoindState;
import com.mobileln.lightningd.LightningdConfig;
import com.mobileln.ui.ReceiveFragment;
import com.mobileln.ui.SendFragment;
import com.mobileln.ui.WalletFragment;
import com.mobileln.utils.ExtractResourceUtils;
import com.mobileln.utils.FastSyncUtils;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final boolean DEBUG = false;

    private final Fragment mWalletFragment = new WalletFragment();
    private final Fragment mSendFragment = new SendFragment();
    private final Fragment mReceiveFragment = new ReceiveFragment();

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            final FragmentManager fm = getSupportFragmentManager();
            switch (item.getItemId()) {
                case R.id.navigation_receive:
                    if (!DEBUG && !isNodeReady()) {
                        return false;
                    }
                    fm.beginTransaction().replace(R.id.main_container, mReceiveFragment,
                            "receive_fragment").commit();
                    return true;
                case R.id.navigation_wallet:
                    fm.beginTransaction().replace(R.id.main_container, mWalletFragment,
                            "wallet_fragment").commit();
                    return true;
                case R.id.navigation_send:
                    if (!DEBUG && !isNodeReady()) {
                        return false;
                    }
                    fm.beginTransaction().replace(R.id.main_container, mSendFragment,
                            "send_fragment").commit();
                    return true;
            }
            return false;
        }
    };

    private boolean isNodeReady() {
        return NodeService.getCurrentState() == NodeService.NodeState.ALL_READY;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        navigation.setSelectedItemId(R.id.navigation_wallet);
        checkUpdate();
    }

    private void checkUpdate() {
        final ProgressDialog dialog;
        if (ExtractResourceUtils.requiresUpdate(this)) {
            dialog = ProgressDialog.show(this, "",
                    "Extracting libraries. Please wait...", true);
            dialog.show();
            new Thread() {
                public void run() {
                    try {
                        ExtractResourceUtils.extractExecutablesIfNecessary(MainActivity.this,
                                false);
                        dialog.dismiss();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    }
}
