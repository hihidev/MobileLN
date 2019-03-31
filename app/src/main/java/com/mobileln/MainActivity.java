package com.mobileln;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import java.io.IOException;

import com.mobileln.lightningd.LightningClient;
import com.mobileln.ui.ReceiveFragment;
import com.mobileln.ui.SendFragment;
import com.mobileln.ui.WalletFragment;
import com.mobileln.utils.ExtractResourceUtils;
import com.mobileln.utils.UIUtils;

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
            final long inCap = LightningClient.newInstance().getCachedInboundCapacity();
            final long outCap = LightningClient.newInstance().getCachedOutboundCapacity();
            switch (item.getItemId()) {
                case R.id.navigation_receive:
                    if (!DEBUG && !isNodeReady()) {
                        UIUtils.showToast(MainActivity.this, "Please start Mobile LN service first");
                        return false;
                    }
                    if (inCap < 0) {
                        return false;
                    }
                    if (inCap == 0) {
                        if (outCap == 0) {
                            UIUtils.showToast(MainActivity.this, "Please create a channel first");
                        } else {
                            UIUtils.showToast(MainActivity.this, "Please fund your inbound channel first");
                        }
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
                        UIUtils.showToast(MainActivity.this, "Please start Mobile LN service first");
                        return false;
                    }
                    if (outCap < 0) {
                        return false;
                    }
                    if (outCap == 0) {
                        if (inCap == 0) {
                            UIUtils.showToast(MainActivity.this, "Please create a channel first");
                        } else {
                            UIUtils.showToast(MainActivity.this, "Please fund your inbound channel first");
                        }
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
