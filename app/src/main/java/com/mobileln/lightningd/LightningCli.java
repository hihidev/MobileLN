package com.mobileln.lightningd;

import android.content.Context;
import android.support.annotation.WorkerThread;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import com.mobileln.MyApplication;
import com.mobileln.utils.FileUtils;
import com.mobileln.utils.ProcessHelper;

// TODO: Verify all commands and expected fields, as some fields may not exist in some cases
// which might case JSONException while it should be handled gracefully
// TODO: Should have some automate test to sure commands are still working correctly even
// lightning-cli is updated
public class LightningCli extends ProcessHelper {
    private static final String TAG = "Lightning-cli";

    private LightningCli(boolean redirectError) {
        super(redirectError);
    }

    public static LightningCli newInstance() {
        return newInstance(false);
    }

    public static LightningCli newInstance(boolean redirectError) {
        return new LightningCli(redirectError);
    }

    @WorkerThread
    private JSONObject getJSONResponseInternal(Context context, String[] args)
            throws IOException, JSONException {
        final String executable = FileUtils.getLightningCliExecutable(context);
        final String lightningDataDir = FileUtils.getLightningdDataFolderPath(context);
        final String lightningRPCFile = FileUtils.getLightningdRPCPath(context);
        final String[] tmp = new String[args.length + 3];
        tmp[0] = executable;
        tmp[1] = "--lightning-dir=" + lightningDataDir;
        tmp[2] = "--rpc-file=" + lightningRPCFile;
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
            final String executable = FileUtils.getLightningCliExecutable(context);
            final String lightningDataDir = FileUtils.getLightningdDataFolderPath(context);
            final String lightningRPCFile = FileUtils.getLightningdRPCPath(context);
            final String[] tmp = new String[args.length + 3];
            tmp[0] = executable;
            tmp[1] = "--lightning-dir=" + lightningDataDir;
            tmp[2] = "--rpc-file=" + lightningRPCFile;
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
        return json.getInt("blockheight");
    }

    @WorkerThread
    public String[] getMyBech32Addresses() throws IOException, JSONException {
        JSONObject json = getJSONResponse(MyApplication.getContext(),
                new String[]{"dev-listaddrs"});
        JSONArray jsonArray = json.getJSONArray("addresses");
        String[] result = new String[jsonArray.length()];
        for (int i = 0; i < result.length; i++) {
            result[i] = jsonArray.getJSONObject(i).getString("bech32");
        }
        return result;
    }

    @WorkerThread
    public long getConfirmedBtcBalanceInWallet() throws JSONException, IOException {
        JSONObject json = getJSONResponse(MyApplication.getContext(), new String[]{"listfunds"});
        JSONArray jsonArray = json.getJSONArray("outputs");
        int len = jsonArray.length();
        long result = 0;
        for (int i = 0; i < len; i++) {
            JSONObject obj = jsonArray.getJSONObject(i);
            if ("confirmed".equals(obj.getString("status"))) {
                result += obj.getLong("value");
            }
        }
        return result;
    }

    @WorkerThread
    public long getConfirmedBalanceInChannels() throws IOException, JSONException {
        JSONObject json = getJSONResponse(MyApplication.getContext(), new String[]{"listpeers"});
        long result = 0;
        JSONArray peers = json.getJSONArray("peers");
        int peersLen = peers.length();
        for (int i = 0; i < peersLen; i++) {
            JSONObject peer = peers.getJSONObject(i);
            JSONArray channels = peer.getJSONArray("channels");
            int channelsLen = channels.length();
            for (int j = 0; j < channelsLen; j++) {
                JSONObject channel = channels.getJSONObject(j);
                String state = channel.getString("state");
                if (!"ONCHAIN".equals(state)) {
                    result += channel.getLong("msatoshi_to_us") / 1000;
                }

            }
        }
        return result;

        // Seems it also list closed channel...=0=...so we use listpeers instead
//        JSONObject json = getJSONResponse(MyApplication.getContext(), new String[]{"listfunds"});
//        JSONArray jsonArray = json.getJSONArray("channels");
//        int len = jsonArray.length();
//        long result = 0;
//        for (int i = 0; i < len; i++) {
//            JSONObject obj = jsonArray.getJSONObject(i);
//            result += obj.getLong("channel_sat");
//        }
//        return result;
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
            String description = obj.optString("description");
            String bolt11 = obj.optString("bolt11");
            String paymentHash = obj.getString("payment_hash");
            long sat = obj.getLong("msatoshi") / 1000;
            long createdAt = obj.getLong("created_at");
            boolean completed = "complete".equals(obj.getString("status"));
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
            String status = obj.getString("status");
            if (status.equals("expired")) {
                continue;
            }
            boolean completed = "paid".equals(status);
            long sat = obj.getLong("msatoshi") / 1000;
            long paidAt = obj.optLong("paid_at", 0);
            String description = obj.getString("description");
            String bolt11 = obj.getString("bolt11");
            String paymentHash = obj.getString("payment_hash");
            result.add(new PaymentInfo(description, bolt11, paymentHash, sat, completed, paidAt));
        }
        return result.toArray(new PaymentInfo[0]);
    }

    @WorkerThread
    public PaymentInfo getInvoiceInfo(String label) throws IOException, JSONException {
        JSONObject json = getJSONResponse(MyApplication.getContext(),
                new String[]{"listinvoices", label});
        JSONArray invoices = json.getJSONArray("invoices");
        int invoicesLen = invoices.length();
        for (int i = 0; i < invoicesLen; i++) {
            JSONObject obj = invoices.getJSONObject(i);
            String status = obj.getString("status");
            if (status.equals("expired")) {
                continue;
            }
            boolean completed = "paid".equals(status);
            long sat = obj.getLong("msatoshi") / 1000;
            long paidAt = obj.optLong("paid_at", 0);
            String description = obj.getString("description");
            String bolt11 = obj.getString("bolt11");
            String paymentHash = obj.getString("payment_hash");
            return new PaymentInfo(description, bolt11, paymentHash, sat, completed, paidAt);
        }
        return null;
    }

    @WorkerThread
    public PaymentInfo getDecodedInvoice(String invoice) throws IOException, JSONException {
        JSONObject json = getJSONResponse(MyApplication.getContext(),
                new String[]{"decodepay", invoice});
        long amount = json.getLong("msatoshi") / 1000;
        String description = json.getString("description");
        String paymentHash = json.getString("payment_hash");
        return new PaymentInfo(description, invoice, paymentHash, amount, false, 0);
    }

    // TODO: Support custom amount
    @WorkerThread
    public PaymentInfo payInvoice(String invoice, String label) throws IOException, JSONException {
        JSONObject json = getJSONResponse(MyApplication.getContext(),
                new String[]{"pay", invoice, "null", label});
        long amount = json.getLong("msatoshi") / 1000;
        long dateTime = json.getLong("created_at");
        String description = label;
        String paymentHash = json.getString("payment_hash");
        return new PaymentInfo(description, invoice, paymentHash, amount, false, dateTime);
    }

    @WorkerThread
    public String withdrawBtc(String withdrawalAddr, long amount, boolean withdrawAll)
            throws IOException, JSONException {
        JSONObject json = getJSONResponse(MyApplication.getContext(),
                new String[]{"withdraw", withdrawalAddr,
                        withdrawAll ? "all" : String.valueOf(amount)});
        return json.getString("txid");
    }

    @WorkerThread
    public String generateInvoice(long sat, String description) throws IOException, JSONException {
        JSONObject json = getJSONResponse(MyApplication.getContext(),
                new String[]{"invoice", String.valueOf(sat * 1000), description, description});
        if (json.has("warning_capacity")) {
            getJSONResponse(MyApplication.getContext(),
                    new String[]{"delinvoice", description, "unpaid"});
            throw new IOException(json.getString("warning_capacity"));
        }
        return json.getString("bolt11");
    }

    @WorkerThread
    public ChannelInfo[] getChannelList() throws IOException, JSONException {
        JSONObject json = getJSONResponse(MyApplication.getContext(), new String[]{"listpeers"});
        ArrayList<ChannelInfo> result = new ArrayList<>();
        JSONArray peers = json.getJSONArray("peers");
        int peersLen = peers.length();
        for (int i = 0; i < peersLen; i++) {
            JSONObject peer = peers.getJSONObject(i);
            JSONArray channels = peer.getJSONArray("channels");
            int channelsLen = channels.length();
            for (int j = 0; j < channelsLen; j++) {
                JSONObject channel = channels.getJSONObject(j);
                String channelId = channel.getString("channel_id");
                String state = channel.getString("state");
                String name = channel.optString("owner");
                long channelTotalSat = channel.getLong("msatoshi_total") / 1000;
                long outCapacitySat = channel.getLong("msatoshi_to_us") / 1000;
                long inCapacitySat = channelTotalSat - outCapacitySat;
                result.add(new ChannelInfo(channelId, state, name, channelTotalSat, inCapacitySat,
                        outCapacitySat));
            }
        }
        return result.toArray(new ChannelInfo[0]);
    }

    @WorkerThread
    public boolean closeChannel(String channelId, boolean force) throws IOException, JSONException {
        getJSONResponse(MyApplication.getContext(),
                new String[]{"close", channelId, force ? "true" : "false"});
        return true;
    }

    @WorkerThread
    public String connectPeer(String nodeAddress) throws IOException, JSONException {
        JSONObject json = getJSONResponse(MyApplication.getContext(),
                new String[]{"connect", nodeAddress});
        return json.getString("id");
    }

    @WorkerThread
    public boolean fundChannel(String peerId, long amount) throws IOException, JSONException {
        getJSONResponse(MyApplication.getContext(),
                new String[]{"fundchannel", peerId, String.valueOf(amount)});
        return true;
    }

    @WorkerThread
    public String[] getListAddrs(int maxIndex) throws IOException, JSONException {
        JSONObject json = getJSONResponse(MyApplication.getContext(),
                new String[]{"dev-listaddrs", String.valueOf(maxIndex)});
        JSONArray jsonArray = json.getJSONArray("addresses");
        int len = jsonArray.length();
        ArrayList<String> result = new ArrayList<>();
        for (int i = 0; i < len; i++) {
            result.add(jsonArray.getJSONObject(i).getString("bech32"));
        }
        return result.toArray(new String[0]);
    }
}
