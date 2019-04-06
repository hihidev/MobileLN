package com.mobileln.lightningd.lnd;

import android.content.Context;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;
import android.util.Log;

import com.mobileln.MyApplication;
import com.mobileln.lightningd.ChannelInfo;
import com.mobileln.lightningd.LightningClientInterface;
import com.mobileln.lightningd.PaymentInfo;
import com.mobileln.utils.FileUtils;
import com.mobileln.utils.ProcessHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

// TODO: Verify all commands and expected fields, as some fields may not exist in some cases
// which might case JSONException while it should be handled gracefully
// TODO: Should have some automate test to sure commands are still working correctly even
// lightning-cli is updated
public class LndClient extends ProcessHelper implements LightningClientInterface {
    private static final String TAG = "Lightning-cli";
    private static volatile long sCachedInboundCapacity = -1;
    private static volatile long sCachedOutboundCapacity = -1;
    private static volatile long sCachedInChannelBalance = -1;
    private static volatile long sCachedOnChainBalance = -1;
    private static volatile long sCachedUnconfirmedOnChainBalance = -1;
    private static volatile String sCachedMyAddress;

    public LndClient(boolean redirectError) {
        super(redirectError);
    }

    @WorkerThread
    private JSONObject getJSONResponseInternal(Context context, String[] args)
            throws IOException, JSONException {
        final String executable = FileUtils.getLncliExecutable(context);
        final String lndDir = FileUtils.getLndDataFolderPath(context);

        final String[] tmp = new String[args.length + 3];
        tmp[0] = executable;
        tmp[1] = "--lnddir=" + lndDir;
        tmp[2] = "--network=testnet";
        System.arraycopy(args, 0, tmp, 3, args.length);
        setExecutableAndArgs(tmp);
        startProcess(context, TAG);
        String output = "";
        try {
            waitFor();
            output = getOutput();
            JSONObject json = new JSONObject(output);
            if (json.has("code")) {
                String message = json.getString("message");
                Log.i(TAG, "ERROR! cmd: " + args[0] + ", message: " + message);
                throw new IOException(message);
            }
            return json;
        } catch (JSONException e) {
            Log.e(TAG, "Exception output: " + output, e);
            throw e;
        } catch (Exception e) {
            Log.e(TAG, "Exception output: " + output, e);
            throw new IOException(e.getMessage());
        } finally {
            clearOutput();
            stopProcess();
        }
    }

    @WorkerThread
    public String rawQuery(Context context, String[] args) {
        try {
            final String executable = FileUtils.getLncliExecutable(context);
            final String lndDir = FileUtils.getLndDataFolderPath(context);

            final String[] tmp = new String[args.length + 3];
            tmp[0] = executable;
            tmp[1] = "--lnddir=" + lndDir;
            tmp[2] = "--network=testnet";
            System.arraycopy(args, 0, tmp, 3, args.length);
            setExecutableAndArgs(tmp);
            startProcess(context, TAG);
            String output = "";
            waitFor();
            return getOutput();
        } catch (Exception e) {
            return e.getMessage();
        } finally {
            clearOutput();
            stopProcess();
        }
    }

    @WorkerThread
    public synchronized JSONObject getJSONResponse(Context context, String[] args)
            throws IOException, JSONException {
        // TODO: Remove this retry loop when it's stable ?
//        for (int i = 0; i < 0; i++) {
//            try {
//                return getJSONResponseInternal(context, args);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
        return getJSONResponseInternal(context, args);
    }

    @WorkerThread
    public int getBlockHeight(Context context) throws IOException, JSONException {
        JSONObject json = getJSONResponse(context, new String[]{"getinfo"});
        return json.getInt("block_height");
    }

    @WorkerThread
    public String[] getMyBech32Addresses() throws IOException, JSONException {
        JSONObject json = getJSONResponse(MyApplication.getContext(), new String[]{"listunspent"});
        JSONArray utxos = json.getJSONArray("utxos");
        int len = utxos.length();
        String[] result = new String[len];
        for (int i = 0; i < len; i++) {
            JSONObject obj = utxos.getJSONObject(i);
            result[i] = obj.getString("address");
        }
        return result;
    }

    @WorkerThread
    public String getMyBech32Address() throws IOException, JSONException {
        if (sCachedMyAddress == null) {
            JSONObject json = getJSONResponse(MyApplication.getContext(),
                    new String[]{"newaddress", "p2wkh"});
            sCachedMyAddress = json.getString("address");
        }
        return sCachedMyAddress;
    }

    @Override
    public long getUnconfirmedOnChainBalance(int minConfirmation) throws JSONException, IOException {
        JSONObject json = getJSONResponse(MyApplication.getContext(), new String[]{"walletbalance"});
        long result = json.getLong("unconfirmed_balance");
        sCachedUnconfirmedOnChainBalance = result;
        return result;
    }

    @WorkerThread
    public long getConfirmedOnChainBalance() throws JSONException, IOException {
        JSONObject json = getJSONResponse(MyApplication.getContext(), new String[]{"walletbalance"});
        long result = json.getLong("confirmed_balance");
        sCachedOnChainBalance = result;
        return result;
    }

    @Override
    public long getCachedUnconfirmedOnChainBalance() {
        return sCachedUnconfirmedOnChainBalance;
    }

    public long getCachedOnChainBalance() {
        return sCachedOnChainBalance;
    }

    public long getCachedInboundCapacity() {
        return sCachedInboundCapacity;
    }

    public long getCachedOutboundCapacity() {
        return sCachedOutboundCapacity;
    }

    @WorkerThread
    public long getBalanceInChannels() throws IOException, JSONException {
        JSONObject json = getJSONResponse(MyApplication.getContext(), new String[]{"channelbalance"});
        long result = json.getLong("balance") + json.getLong("pending_open_balance");
        sCachedInChannelBalance = result;
        return result;
    }

    public long getCachedInChannelBalance() {
        return sCachedInChannelBalance;
    }

    @WorkerThread
    public PaymentInfo[] getPaymentSent() throws IOException, JSONException {
        // TODO: Use new API listpays ! Can get label !
        JSONObject json = getJSONResponse(MyApplication.getContext(), new String[]{"listpayments"});
        JSONArray payArray = json.getJSONArray("payments");
        int payArrayLen = payArray.length();
        PaymentInfo[] result = new PaymentInfo[payArrayLen];
        for (int i = 0; i < payArrayLen; i++) {
            JSONObject obj = payArray.getJSONObject(i);
            String description = obj.optString("payment_hash");
            String bolt11 = null;
            String paymentHash = obj.getString("payment_hash");
            long sat = obj.getLong("value_msat") / 1000;
            long createdAt = obj.getLong("creation_date");
            boolean completed = true;
            result[i] = new PaymentInfo(description, bolt11, paymentHash, sat, completed,
                    createdAt);
        }
        return result;
    }

    @WorkerThread
    public PaymentInfo[] getPaymentReceived() throws IOException, JSONException {
        JSONObject json = getJSONResponse(MyApplication.getContext(), new String[]{"listinvoices"});
        JSONArray invoices = json.getJSONArray("invoices");
        int invoicesLen = invoices.length();
        ArrayList<PaymentInfo> result = new ArrayList<>();
        for (int i = 0; i < invoicesLen; i++) {
            JSONObject obj = invoices.getJSONObject(i);
            String state = obj.getString("state");
            if (state == "CANCELED") {
                continue;
            }
            boolean completed = "SETTLED".equals(state);
            long sat = obj.getLong("value");
            long paidAt = obj.optLong("settle_date", 0);
            String description = obj.getString("memo");
            String bolt11 = obj.optString("payment_request");
            String paymentHash = obj.getString("r_hash");
            result.add(new PaymentInfo(description, bolt11, paymentHash, sat, completed, paidAt));
        }
        return result.toArray(new PaymentInfo[0]);
    }

    @WorkerThread
    public PaymentInfo getInvoiceInfo(String label) throws IOException, JSONException {
        JSONObject json = getJSONResponse(MyApplication.getContext(),
                new String[]{"listinvoices", label});
        return null;
    }

    @WorkerThread
    public PaymentInfo getDecodedInvoice(String invoice) throws IOException, JSONException {
        JSONObject json = getJSONResponse(MyApplication.getContext(),
                new String[]{"decodepayreq", "--pay_req=" + invoice});
        long amount = json.getLong("num_satoshis");
        String description = json.getString("description");
        String paymentHash = json.getString("payment_hash");
        return new PaymentInfo(description, invoice, paymentHash, amount, false, 0);
    }

    // TODO: Support custom amount
    @WorkerThread
    public PaymentInfo payInvoice(String invoice, String label) throws IOException, JSONException {
        JSONObject json = getJSONResponse(MyApplication.getContext(),
                new String[]{"sendpayment", "--pay_req=" + invoice, "-f"});
        long amount = 0;
        long dateTime = 0;
        String description = null;
        String paymentPreimage = json.getString("payment_preimage");
        return new PaymentInfo(description, invoice, paymentPreimage, amount, false, dateTime);
    }

    @WorkerThread
    public String withdrawBtc(String withdrawalAddr, long amount, boolean withdrawAll)
            throws IOException, JSONException {
        JSONObject json = getJSONResponse(MyApplication.getContext(),
                new String[]{"sendcoins", "--addr=" + withdrawalAddr,
                        withdrawAll ? "--sweepall" : "--amt=" + String.valueOf(amount)});
        return json.getString("txid");
    }

    @WorkerThread
    public String generateInvoice(long sat, String description) throws IOException, JSONException {
        JSONObject json = getJSONResponse(MyApplication.getContext(),
                new String[]{"addinvoice", "--amt=" + String.valueOf(sat), "--memo=" + description});

        return json.getString("pay_req");
    }

    @WorkerThread
    public void updateCachedInOutboundCapacity() throws IOException, JSONException {
        getChannelList();
    }

    private ArrayList<ChannelInfo> getOpenedChannels() throws IOException, JSONException {
        JSONObject json = getJSONResponse(MyApplication.getContext(), new String[]{"listchannels"});
        JSONArray channels = json.getJSONArray("channels");
        return getChannelInfo(channels, false, true, "opened_channels");
    }

    private ArrayList<ChannelInfo> getClosedChannels() throws IOException, JSONException {
        JSONObject json = getJSONResponse(MyApplication.getContext(), new String[]{"closedchannels"});
        JSONArray channels = json.getJSONArray("channels");
        return getChannelInfo(channels, true, false, "closedchannels");
    }

    private ArrayList<ChannelInfo> getPendingChannels() throws IOException, JSONException {
        JSONObject json = getJSONResponse(MyApplication.getContext(), new String[]{"pendingchannels"});
        ArrayList<ChannelInfo> result = new ArrayList<>();
        result.addAll(getPendingChannelInfo(json.getJSONArray("pending_open_channels"), false, false, "pending_open_channels"));
        result.addAll(getPendingChannelInfo(json.getJSONArray("pending_closing_channels"), false, false, "pending_closing_channels"));
        result.addAll(getPendingChannelInfo(json.getJSONArray("pending_force_closing_channels"), false, false, "pending_force_closing_channels"));
        result.addAll(getPendingChannelInfo(json.getJSONArray("waiting_close_channels"), false, false, "waiting_close_channels"));
        return result;
    }

    private ArrayList<ChannelInfo> getPendingChannelInfo(JSONArray channels, boolean closed, boolean opened, String stateName) throws JSONException {
        ArrayList<ChannelInfo> result = new ArrayList<>();
        int channelsLen = channels.length();
        for (int j = 0; j < channelsLen; j++) {
            JSONObject channel = channels.getJSONObject(j).getJSONObject("channel");
            result.add(getChannelInfo(channel, closed, opened, stateName));
        }
        return result;
    }

    private ArrayList<ChannelInfo> getChannelInfo(JSONArray channels, boolean closed, boolean opened, String stateName) throws JSONException {
        ArrayList<ChannelInfo> result = new ArrayList<>();
        int channelsLen = channels.length();
        for (int j = 0; j < channelsLen; j++) {
            JSONObject channel = channels.getJSONObject(j);
            result.add(getChannelInfo(channel, closed, opened, stateName));
        }
        return result;
    }

    private ChannelInfo getChannelInfo(JSONObject channel, boolean closed, boolean opened, String stateName) throws JSONException {
        String channelId = channel.getString("channel_point");
        boolean active = false;
        if (opened && !closed) {
            // active = channel.getBoolean("active");
            active = true;
        }
        String pubkey = channel.optString("remote_pubkey");
        if (TextUtils.isEmpty(pubkey)) {
            pubkey = channel.optString("remote_node_pub");
        }
        String name = "[" + stateName + "]" + pubkey;
        long channelTotalSat = channel.getLong("capacity");
        long outCapacitySat = channel.optLong("local_balance", channel.optLong("settled_balance", 0));
        long inCapacitySat = channelTotalSat - outCapacitySat;
        return new ChannelInfo(channelId, closed, active, name, channelTotalSat, inCapacitySat, outCapacitySat);
    }

    @WorkerThread
    public ChannelInfo[] getChannelList() throws IOException, JSONException {
        ArrayList<ChannelInfo> result = new ArrayList<>();
        result.addAll(getPendingChannels());
        result.addAll(getOpenedChannels());
        // result.addAll(getClosedChannels());
        long inCap = 0;
        long outCap = 0;
        for (ChannelInfo channelInfo : result) {
            if (channelInfo.active) {
                inCap += channelInfo.oppBal;
                outCap += channelInfo.myBal;
            }
        }
        sCachedInboundCapacity = inCap;
        sCachedOutboundCapacity = outCap;
        return result.toArray(new ChannelInfo[0]);
    }

    @WorkerThread
    public boolean closeChannel(String addr, boolean force) throws IOException, JSONException {
        String channelPoint = addr.split(":")[0];
        String index = addr.split(":")[1];
        ArrayList<String> list = new ArrayList<>();
        list.add("closechannel");
        if (force) {
            list.add("--force");
        }
        list.add(channelPoint);
        list.add(index);
        getJSONResponse(MyApplication.getContext(), list.toArray(new String[0]));
        return true;
    }

    @WorkerThread
    public String connectPeer(String nodeAddress) throws IOException, JSONException {
        rawQuery(MyApplication.getContext(), new String[]{"connect", nodeAddress});
        return nodeAddress.split("@")[0];
    }

    @WorkerThread
    public boolean fundChannel(String peerId, long amount) throws IOException, JSONException {
        getJSONResponse(MyApplication.getContext(),
                new String[]{"openchannel", "--node_key=" + peerId, "--local_amt=" + String.valueOf(amount)});
        return true;
    }

    @WorkerThread
    public String[] getListAddrs(int maxIndex) throws IOException, JSONException {
        throw new IllegalStateException("Should not call getListAddrs() under LND");
    }
}
