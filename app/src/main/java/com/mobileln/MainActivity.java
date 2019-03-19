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
    private final Fragment mWalletFragment = new WalletFragment();
    private final Fragment mSendFragment = new SendFragment();
    private final Fragment mReceiveFragment = new ReceiveFragment();

    private ImageView mServiceStatusImageView;
    private ImageView mSettingsImageView;

    private AlertDialog mServiceStatusDialog = null;
    private View mDialogView = null;
    private Button mDialogOkBtn = null;
    private Button mDialogCancelBtn = null;
    private TextView mDialogMessageTextView = null;

    private int mPreviousState = NodeService.NodeState.UNKNOWN;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            final FragmentManager fm = getSupportFragmentManager();
            switch (item.getItemId()) {
                case R.id.navigation_receive:
                    if (!isNodeReady()) {
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
                    if (!isNodeReady()) {
                        return false;
                    }
                    fm.beginTransaction().replace(R.id.main_container, mSendFragment,
                            "send_fragment").commit();
                    return true;
            }
            return false;
        }
    };

    private Runnable mCurrentStatusUpdated = new Runnable() {

        @Override
        public void run() {
            Log.i(TAG, "mCurrentStatusUpdated");
            int status = NodeService.getCurrentState();
            if (mPreviousState == status && (status != NodeService.NodeState.SYNCING_BITCOIND
                    && status != NodeService.NodeState.DOWNLOAD_FASTSYNC)) {
                return;
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateCurrentStatusUI();
                }
            });
        }
    };

    @MainThread
    private void updateCurrentStatusUI() {
        int status = NodeService.getCurrentState();
        mPreviousState = status;
        final int id;
        if (status == NodeService.NodeState.UNKNOWN
                || status == NodeService.NodeState.ALL_DISCONNECTED) {
            id = R.drawable.red_circle;
        } else if (status == NodeService.NodeState.ALL_READY) {
            id = R.drawable.green_circle;
        } else {
            id = R.drawable.yellow_circle;
        }
        mServiceStatusImageView.setImageResource(id);
        updateServiceStatusDialog();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        navigation.setSelectedItemId(R.id.navigation_wallet);

        Context context = new ContextThemeWrapper(this,
                R.style.Theme_MaterialComponents_Light_Dialog_Alert);
        mDialogView = LayoutInflater.from(context).inflate(R.layout.service_status_layout, null,
                true);
        mServiceStatusDialog = new AlertDialog.Builder(this,
                R.style.Theme_MaterialComponents_Light_Dialog_Alert).setView(mDialogView).create();
        mDialogOkBtn = mDialogView.findViewById(R.id.ok);
        mDialogCancelBtn = mDialogView.findViewById(R.id.cancel);
        mDialogCancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mServiceStatusDialog.dismiss();
            }
        });
        mDialogMessageTextView = mDialogView.findViewById(R.id.message);

        mServiceStatusImageView = findViewById(R.id.service_status_light_imageview);
        mServiceStatusImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isNodeConfigValid()) {
                    showSetupConfigDialog();
                } else {
                    updateServiceStatusDialog();
                    mServiceStatusDialog.show();
                }
            }
        });
        mSettingsImageView = findViewById(R.id.wallet_settings_imageview);
        mSettingsImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });
        checkUpdate();
    }

    @Override
    public void onResume() {
        super.onResume();
        NodeService.addCurrentStateUpdatedCallback(mCurrentStatusUpdated);
        updateCurrentStatusUI();
    }

    @Override
    public void onPause() {
        super.onPause();
        NodeService.removeCurrentStateUpdatedCallback(mCurrentStatusUpdated);
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

    private boolean isNodeReady() {
        return mPreviousState == NodeService.NodeState.ALL_READY;
    }

    private String getDialogMessage(int status) {
        switch (status) {
            case NodeService.NodeState.UNKNOWN:
                return getString(R.string.text_node_status_unkonwn);
            case NodeService.NodeState.ALL_DISCONNECTED:
                return getString(R.string.text_node_status_disconnected);
            case NodeService.NodeState.DOWNLOAD_FASTSYNC:
                final double progress = FastSyncUtils.downloadProgress(MainActivity.this) * 100;
                return String.format("%s%.4f%%",
                        getString(R.string.text_node_status_fast_sync_bitcoin), progress);
            case NodeService.NodeState.STARTING_BITCOIND:
                return getString(R.string.text_node_status_connecting_bitcoin);
            case NodeService.NodeState.SYNCING_BITCOIND:
                final double progress2 =
                        BitcoindState.getInstance().getVerificationProgress() * 100;
                return String.format("%s%.4f%%",
                        getString(R.string.text_node_status_syncing_bitcoin), progress2);
            case NodeService.NodeState.STARTING_LIGHTNINGD:
                return getString(R.string.text_node_status_starting_lightning);
            case NodeService.NodeState.ALL_READY:
                return getString(R.string.text_node_status_online);
            case NodeService.NodeState.DISCONNECTING:
                return getString(R.string.text_node_status_disconnecting);
            default:
                return "???";
        }
    }

    private void updateServiceStatusDialog() {
        final boolean isRunning = NodeService.isRunning();
        mDialogMessageTextView.setText(getDialogMessage(mPreviousState));
        mDialogOkBtn.setText(isRunning ? "Disconnect" : "Connect");
        mDialogOkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isRunning) {
                    NodeService.stopNodeService(MainActivity.this);
                } else {
                    if (isNodeConfigValid()) {
                        NodeService.startNodeService(MainActivity.this);
                    } else {
                        Log.w(TAG, "Should not happen");
                    }
                }
            }
        });
    }

    private boolean isNodeConfigValid() {
        try {
            // TODO: Validate config inside!
            if (BitcoindConfig.readConfig(this).size() > 0 ||
                    LightningdConfig.readConfig(this).size() > 0) {
                return true;
            }
        } catch (IOException e) {
        }
        return false;
    }

    @MainThread
    private void showSetupConfigDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_question_ln_service_title).setMessage(
                R.string.dialog_question_setup_config).setPositiveButton(
                "Yes(Fast sync)", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        try {
                            Map<String, String> map = BitcoindConfig.readDefaultConfig();
                            BitcoindConfig.saveConfig(MainActivity.this, map);
                            LightningdConfig.saveConfig(MainActivity.this,
                                    LightningdConfig.readDefaultConfig());
                            FastSyncUtils.savePendingFastSyncWork(MainActivity.this, true);
                            if (NodeService.isRunning()) {
                                Log.w(TAG, "Node shouldn't be running?");
                            } else {
                                updateServiceStatusDialog();
                                mServiceStatusDialog.show();
                                NodeService.startNodeService(MainActivity.this);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                })
                .setNegativeButton("Yes(Full slow sync)",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                try {
                                    Map<String, String> map = BitcoindConfig.readDefaultConfig();
                                    map.remove("assumevalid");
                                    BitcoindConfig.saveConfig(MainActivity.this, map);
                                    LightningdConfig.saveConfig(MainActivity.this,
                                            LightningdConfig.readDefaultConfig());
                                    if (NodeService.isRunning()) {
                                        Log.w(TAG, "Node shouldn't be running?");
                                    } else {
                                        updateServiceStatusDialog();
                                        mServiceStatusDialog.show();
                                        NodeService.startNodeService(MainActivity.this);
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        })
                .setNeutralButton(R.string.dialog_question_no,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        }).show();
    }
}
