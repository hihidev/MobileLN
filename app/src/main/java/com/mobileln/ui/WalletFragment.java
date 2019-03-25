package com.mobileln.ui;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;

import java.io.IOException;
import java.util.Map;

import com.mobileln.BitcoinWalletActivity;
import com.mobileln.ChannelSetupActivity;
import com.mobileln.MainActivity;
import com.mobileln.MyApplication;
import com.mobileln.NodeService;
import com.mobileln.R;
import com.mobileln.SettingsActivity;
import com.mobileln.bitcoind.BitcoindConfig;
import com.mobileln.bitcoind.BitcoindState;
import com.mobileln.lightningd.LightningCli;
import com.mobileln.lightningd.LightningdConfig;
import com.mobileln.utils.BtcSatUtils;
import com.mobileln.utils.FastSyncUtils;
import com.mobileln.utils.UIUtils;

public class WalletFragment extends Fragment {

    private static final String TAG = "WalletFragment";

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private TextView mTotalBalanceTextView;
    private TextView mTotalBalanceUnitTextView;
    private TextView mChannelBalanceTextView;
    private TextView mChannelBalanceUnitTextView;
    private TextView mChainBalanceTextView;
    private TextView mChainBalanceUnitTextView;

    private CardView mServiceStatusCardView;
    private View mSettingsImageView;

    private View mManageChannelBtn;
    private View mManageChainWalletBtn;

    private AlertDialog mServiceStatusDialog = null;
    private View mDialogView = null;
    private Button mDialogOkBtn = null;
    private Button mDialogCancelBtn = null;
    private TextView mDialogMessageTextView = null;

    private int mPreviousState = NodeService.NodeState.UNKNOWN;

    private Runnable mCurrentStateUpdated = new Runnable() {

        @Override
        public void run() {
            Log.i(TAG, "mCurrentStateUpdated");
            final int currentState = NodeService.getCurrentState();
            if (currentState == NodeService.NodeState.ALL_READY) {
                updateWalletBalanceAsync();
            }
            int status = NodeService.getCurrentState();
            if (mPreviousState == status && (status != NodeService.NodeState.SYNCING_BITCOIND
                    && status != NodeService.NodeState.DOWNLOAD_FASTSYNC)) {
                return;
            }
            Activity activity = getActivity();
            if (activity == null) {
                return;
            }
            activity.runOnUiThread(new Runnable() {
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
        final int color;
        if (status == NodeService.NodeState.UNKNOWN
                || status == NodeService.NodeState.ALL_DISCONNECTED) {
            color = Color.RED;
        } else if (status == NodeService.NodeState.ALL_READY) {
            color = Color.GREEN;
        } else {
            color = Color.YELLOW;
        }
        mServiceStatusCardView.setCardBackgroundColor(color);
        updateServiceStatusDialog();
    }

    private String getDialogMessage(int status) {
        switch (status) {
            case NodeService.NodeState.UNKNOWN:
                return getString(R.string.text_node_status_unkonwn);
            case NodeService.NodeState.ALL_DISCONNECTED:
                return getString(R.string.text_node_status_disconnected);
            case NodeService.NodeState.DOWNLOAD_FASTSYNC:
                final double progress = FastSyncUtils.downloadProgress(MyApplication.getContext()) * 100;
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
                    NodeService.stopNodeService(MyApplication.getContext());
                } else {
                    if (isNodeConfigValid()) {
                        NodeService.startNodeService(MyApplication.getContext());
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
            if (BitcoindConfig.readConfig(MyApplication.getContext()).size() > 0 ||
                    LightningdConfig.readConfig(MyApplication.getContext()).size() > 0) {
                return true;
            }
        } catch (IOException e) {
        }
        return false;
    }

    @MainThread
    private void showSetupConfigDialog() {
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.Theme_MaterialComponents_Light_Dialog_Alert);
        builder.setTitle(R.string.dialog_question_ln_service_title).setMessage(
                R.string.dialog_question_setup_config).setPositiveButton(
                "Yes(Fast sync)", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        try {
                            Map<String, String> map = BitcoindConfig.readDefaultConfig();
                            BitcoindConfig.saveConfig(MyApplication.getContext(), map);
                            LightningdConfig.saveConfig(MyApplication.getContext(),
                                    LightningdConfig.readDefaultConfig());
                            FastSyncUtils.savePendingFastSyncWork(MyApplication.getContext(), true);
                            if (NodeService.isRunning()) {
                                Log.w(TAG, "Node shouldn't be running?");
                            } else {
                                updateServiceStatusDialog();
                                mServiceStatusDialog.show();
                                NodeService.startNodeService(MyApplication.getContext());
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
                                    BitcoindConfig.saveConfig(MyApplication.getContext(), map);
                                    LightningdConfig.saveConfig(MyApplication.getContext(),
                                            LightningdConfig.readDefaultConfig());
                                    if (NodeService.isRunning()) {
                                        Log.w(TAG, "Node shouldn't be running?");
                                    } else {
                                        updateServiceStatusDialog();
                                        mServiceStatusDialog.show();
                                        NodeService.startNodeService(MyApplication.getContext());
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.ln_wallet_layout, container, false);
        mSwipeRefreshLayout = view.findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (!isNodeReady()) {
                    mSwipeRefreshLayout.setRefreshing(false);
                    return;
                }
                if (NodeService.getCurrentState() == NodeService.NodeState.ALL_DISCONNECTED) {
                    // showSetupOrSwitchOnOff();
                    mSwipeRefreshLayout.setRefreshing(false);
                    return;
                }
                updateWalletBalanceAsync();
            }
        });
        mTotalBalanceTextView = view.findViewById(R.id.wallet_total_balance_amount);
        mTotalBalanceUnitTextView = view.findViewById(R.id.wallet_total_balance_unit);
        mChannelBalanceTextView = view.findViewById(R.id.wallet_channel_balance_amount);
        mChannelBalanceUnitTextView = view.findViewById(R.id.wallet_channel_balance_unit);
        mChainBalanceTextView = view.findViewById(R.id.wallet_chain_balance_amount);
        mChainBalanceUnitTextView = view.findViewById(R.id.wallet_chain_balance_unit);
        mManageChannelBtn = view.findViewById(R.id.wallet_manage_channel);
        mManageChannelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isNodeReady()) {
                    return;
                }
                if (mTotalBalanceTextView.getText().toString().equals("0")) {
                    new AlertDialog.Builder(getActivity(),
                            R.style.Theme_MaterialComponents_Light_Dialog_Alert)
                            .setTitle("Welcome")
                            .setMessage(
                                    "Please get some funds into wallet before creating a channel")
                            .setPositiveButton(android.R.string.ok,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface,
                                                int i) {
                                            startActivity(new Intent(getActivity(),
                                                    BitcoinWalletActivity.class));
                                        }
                                    })
                            .show();
                    return;
                }
                startActivity(new Intent(getActivity(), ChannelSetupActivity.class));
            }
        });
        mManageChainWalletBtn = view.findViewById(R.id.wallet_manage_chain);
        mManageChainWalletBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isNodeReady()) {
                    return;
                }
                startActivity(new Intent(getActivity(), BitcoinWalletActivity.class));
            }
        });

        Context context = new ContextThemeWrapper(getActivity(),
                R.style.Theme_MaterialComponents_Light_Dialog_Alert);
        mDialogView = LayoutInflater.from(context).inflate(R.layout.service_status_layout, null,
                true);
        mServiceStatusDialog = new AlertDialog.Builder(getActivity(),
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

        mServiceStatusCardView = view.findViewById(R.id.wallet_status_circle);
        mServiceStatusCardView.setOnClickListener(new View.OnClickListener() {
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
        mSettingsImageView = view.findViewById(R.id.wallet_settings_icon);
        mSettingsImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getContext(), SettingsActivity.class);
                startActivity(intent);
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        NodeService.addCurrentStateUpdatedCallback(mCurrentStateUpdated);
        mCurrentStateUpdated.run();
        updateCurrentStatusUI();
    }

    @Override
    public void onPause() {
        super.onPause();
        NodeService.removeCurrentStateUpdatedCallback(mCurrentStateUpdated);
    }

    private boolean isNodeReady() {
        return NodeService.getCurrentState() == NodeService.NodeState.ALL_READY;
    }

    @MainThread
    private void updateWalletBalanceAsync() {
        new AsyncTask<Void, Void, Pair<Long, Long>>() {

            @Override
            protected Pair<Long, Long> doInBackground(Void... voids) {
                try {
                    long channelBalance = LightningCli.newInstance().getConfirmedBalanceInChannels();
                    long btcBalance = LightningCli.newInstance().getConfirmedBtcBalanceInWallet();
                    return Pair.create(channelBalance, btcBalance);
                } catch (IOException | JSONException e) {
                    UIUtils.showErrorToast(getActivity(), e.getMessage());
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Pair<Long, Long> result) {
                mSwipeRefreshLayout.setRefreshing(false);
                if (result == null) {
                    return;
                }
                Pair<String, String> channelBal = BtcSatUtils.sat2StringPair(result.first);
                Pair<String, String> chainBal = BtcSatUtils.sat2StringPair(result.second);
                Pair<String, String> totalBal = BtcSatUtils.sat2StringPair(result.first + result.second);
                mChannelBalanceTextView.setText(channelBal.first);
                mChannelBalanceUnitTextView.setText(channelBal.second);
                mChainBalanceTextView.setText(chainBal.first);
                mChainBalanceUnitTextView.setText(chainBal.second);
                mTotalBalanceTextView.setText(totalBal.first);
                mTotalBalanceUnitTextView.setText(totalBal.second);
            }
        }.execute();
    }
}
