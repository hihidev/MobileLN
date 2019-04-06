package com.mobileln;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.design.card.MaterialCardView;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.mobileln.lightningd.ChannelInfo;
import com.mobileln.lightningd.LightningClient;
import com.mobileln.utils.UIUtils;

public class ChannelSetupActivity extends AppCompatActivity {

    private static final String TAG = "ChannelSetupActivity";
    private static final String TEST_NODE_ADDR =
            "0260d9119979caedc570ada883ff614c6efb93f7f7382e25d73ecbeba0b62df2d7@lnd.fun:9735";
    private static final String TEST_NODE_DEFAULT_DEPOSIT = "50000";

    private ListView mListView;
    private TextView mCreateChannelPeerAddrTextView;
    private TextView mCreateChannelAmountTextView;
    private Button mCreateChannelBtn;
    private TextView mNoConnectedChannels;
    private Button mTryTestChannelBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel_setup);
        setupListView();
        mCreateChannelPeerAddrTextView = findViewById(R.id.create_channel_peer_address);
        mCreateChannelAmountTextView = findViewById(R.id.create_channel_amount_sat);
        mCreateChannelBtn = findViewById(R.id.create_channel_btn);
        mCreateChannelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String addr = mCreateChannelPeerAddrTextView.getText().toString();
                String amountStr = mCreateChannelAmountTextView.getText().toString();
                if (TextUtils.isEmpty(addr)) {
                    UIUtils.showErrorToast(ChannelSetupActivity.this, "Address cannot be empty");
                    return;
                }
                long amount;
                try {
                    amount = Long.parseLong(amountStr);
                } catch (Exception e) {
                    UIUtils.showErrorToast(ChannelSetupActivity.this,
                            "Amount needs to be a number");
                    return;
                }
                createChannelAsync(addr, amount);
            }
        });
        mNoConnectedChannels = findViewById(R.id.create_channel_no_connected_channel);
        mTryTestChannelBtn = findViewById(R.id.connect_to_test_channel);
        mTryTestChannelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCreateChannelPeerAddrTextView.setText(TEST_NODE_ADDR);
                mCreateChannelAmountTextView.setText(TEST_NODE_DEFAULT_DEPOSIT);
                Toast.makeText(ChannelSetupActivity.this,
                        "Filled in test channel details.\nClick \"Create Channel\" to the next "
                                + "step!",
                        Toast.LENGTH_SHORT).show();
            }
        });

        updateChannelsAsync();
    }

    private void setupListView() {
        mListView = (ListView) findViewById(R.id.channel_listview);
        View header = getLayoutInflater().inflate(R.layout.new_channel_layout, mListView, false);
        mListView.addHeaderView(header);
        ViewCompat.setNestedScrollingEnabled(mListView, true);
    }

    private static class MyAdapter extends ArrayAdapter<ChannelInfo> implements
            View.OnClickListener {

        private ArrayList<ChannelInfo> mChannelInfos;
        private WeakReference<ChannelSetupActivity> mActivity;

        private static class ViewHolder {
            String channelId;
            TextView channelName;
            MaterialCardView channelColor;
            TextView myBal;
            TextView oppBal;
        }

        public MyAdapter(ArrayList<ChannelInfo> channelInfos, ChannelSetupActivity mActivity) {
            super(mActivity, R.layout.channel_item, channelInfos);
            this.mChannelInfos = channelInfos;
            this.mActivity = new WeakReference<>(mActivity);
        }

        @Override
        public void onClick(View v) {
            ViewHolder viewHolder = (ViewHolder) v.getTag();
            ChannelSetupActivity activity = mActivity.get();
            if (activity != null) {
                activity.showCloseChannelDialog(viewHolder.channelId);
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ChannelInfo channelInfo = getItem(position);
            final ViewHolder viewHolder;

            if (convertView == null) {
                viewHolder = new ViewHolder();
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(R.layout.channel_item, parent, false);
                viewHolder.channelName = convertView.findViewById(R.id.channel_name_textview);
                viewHolder.channelColor = convertView.findViewById(
                        R.id.channel_state_color_cardview);
                viewHolder.myBal = convertView.findViewById(R.id.channel_my_bal);
                viewHolder.oppBal = convertView.findViewById(R.id.channel_opp_bal);
                convertView.setTag(viewHolder);
                convertView.setOnClickListener(this);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            viewHolder.channelName.setText(channelInfo.name);
            viewHolder.channelColor.setCardBackgroundColor(channelInfo.active ? 0xFF00FF00
                            : 0xFFFFFF00);
            viewHolder.myBal.setText("Outbound cap:" + channelInfo.myBal);
            viewHolder.oppBal.setText("Inbound cap:" + channelInfo.oppBal);
            viewHolder.channelId = channelInfo.channelId;
            return convertView;
        }
    }

    @MainThread
    private void showCloseChannelDialog(final String channelId) {
        final CharSequence[] charSequence =
                new CharSequence[]{"Close channel", "FORCE Close channel (Funds may locked)"};
        final List<Integer> clickedOption = new ArrayList<>();
        clickedOption.add(0);
        new AlertDialog.Builder(this)
                .setTitle("Close channel")
                .setSingleChoiceItems(charSequence, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        clickedOption.clear();
                        clickedOption.add(i);
                    }
                })
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (clickedOption.size() > 0) {
                            final int option = clickedOption.get(0);
                            if (option == 0) {
                                closeChannelAsync(channelId, false);
                            } else if (option == 1) {
                                closeChannelAsync(channelId, true);
                            } else {
                                Log.e(TAG, "Should not happen, option: " + option);
                            }
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @MainThread
    private void closeChannelAsync(final String channelId, final boolean force) {
        final AlertDialog closingDialog = new AlertDialog.Builder(this,
                R.style.Theme_MaterialComponents_Light_Dialog_Alert)
                .setTitle("Closing channel")
                .setMessage("Please wait...").setCancelable(false).create();
        closingDialog.show();
        new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    return LightningClient.newInstance().closeChannel(channelId, force);
                } catch (IOException | JSONException e) {
                    UIUtils.showErrorToast(ChannelSetupActivity.this, e.getMessage());
                    return null;
                }
            }

            @Override
            public void onPostExecute(Boolean result) {
                closingDialog.dismiss();
                if (result == null) {
                    return;
                }
                updateChannelsAsync();
                new AlertDialog.Builder(ChannelSetupActivity.this)
                        .setTitle("Channel closed")
                        .setMessage("Channel successfully closed")
                        .setNeutralButton(android.R.string.ok, null)
                        .show();
            }
        }.execute();
    }

    @MainThread
    private void updateListView(ChannelInfo[] channelInfos) {
        mListView.setAdapter(
                new MyAdapter(new ArrayList<ChannelInfo>(Arrays.asList(channelInfos)), this));
    }

    @MainThread
    private void createChannelAsync(final String peerAddr, final long amount) {
        new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    String peerId = LightningClient.newInstance().connectPeer(peerAddr);
                    return LightningClient.newInstance().fundChannel(peerId, amount);
                } catch (IOException | JSONException e) {
                    UIUtils.showErrorToast(ChannelSetupActivity.this, e.getMessage());
                    return null;
                }
            }

            @Override
            public void onPostExecute(Boolean result) {
                if (result == null) {
                    return;
                }
                new AlertDialog.Builder(ChannelSetupActivity.this)
                        .setTitle("Channel created")
                        .setMessage("Channel successfully created")
                        .setNeutralButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        updateChannelsAsync();
                                    }
                                })
                        .show();
            }
        }.execute();
    }

    @MainThread
    private void updateChannelsAsync() {
        new AsyncTask<Void, Void, ChannelInfo[]>() {

            @Override
            protected ChannelInfo[] doInBackground(Void... voids) {
                try {
                    return LightningClient.newInstance().getChannelList();
                } catch (IOException | JSONException e) {
                    UIUtils.showErrorToast(ChannelSetupActivity.this, e.getMessage());
                    return null;
                }
            }

            @Override
            public void onPostExecute(ChannelInfo[] channelInfos) {
                if (channelInfos == null) {
                    return;
                }
                if (channelInfos.length == 0) {
                    mNoConnectedChannels.setVisibility(View.VISIBLE);
                    mTryTestChannelBtn.setVisibility(
                            NodeService.isTestnet() ? View.VISIBLE : View.GONE);
                } else {
                    mNoConnectedChannels.setVisibility(View.GONE);
                    mTryTestChannelBtn.setVisibility(View.GONE);
                }
                updateListView(channelInfos);
            }
        }.execute();
    }
}
