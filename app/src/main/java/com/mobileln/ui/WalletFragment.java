package com.mobileln.ui;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;

import java.io.IOException;

import com.mobileln.BitcoinWalletActivity;
import com.mobileln.ChannelSetupActivity;
import com.mobileln.NodeService;
import com.mobileln.R;
import com.mobileln.lightningd.LightningCli;
import com.mobileln.utils.BtcSatUtils;

public class WalletFragment extends Fragment {

    private static final String TAG = "WalletFragment";

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private TextView mTotalBalanceTextView;
    private TextView mChannelBalanceTextView;
    private TextView mChainBalanceTextView;
    private Button mManageChannelBtn;
    private Button mManageChainWalletBtn;
    private Runnable mCurrentStateUpdated = new Runnable() {

        @Override
        public void run() {
            Log.i(TAG, "mCurrentStateUpdated");
            final int currentState = NodeService.getCurrentState();
            if (currentState == NodeService.NodeState.ALL_READY) {
                updateWalletBalanceAsync();
            }
        }
    };

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
        mTotalBalanceTextView = view.findViewById(R.id.total_balance_content);
        mChannelBalanceTextView = view.findViewById(R.id.channel_balance_content);
        mChainBalanceTextView = view.findViewById(R.id.chain_balance_content);
        mManageChannelBtn = view.findViewById(R.id.manage_channel_btn);
        mManageChannelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isNodeReady()) {
                    return;
                }
                startActivity(new Intent(getActivity(), ChannelSetupActivity.class));
            }
        });
        mManageChainWalletBtn = view.findViewById(R.id.manage_on_chain_btn);
        mManageChainWalletBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isNodeReady()) {
                    return;
                }
                startActivity(new Intent(getActivity(), BitcoinWalletActivity.class));
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        NodeService.addCurrentStateUpdatedCallback(mCurrentStateUpdated);
        mCurrentStateUpdated.run();
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
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Pair<Long, Long> result) {
                mSwipeRefreshLayout.setRefreshing(false);
                if (result == null) {
                    Toast.makeText(getContext(), "Error", Toast.LENGTH_LONG).show();
                    return;
                }
                mChannelBalanceTextView.setText(BtcSatUtils.sat2String(result.first));
                mChainBalanceTextView.setText(BtcSatUtils.sat2String(result.second));
                mTotalBalanceTextView.setText(BtcSatUtils.sat2String(result.first + result.second));
            }
        }.execute();
    }
}
