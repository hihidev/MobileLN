package com.mobileln.lightningd;

import android.content.Context;

import com.mobileln.MyApplication;
import com.mobileln.lightningd.clightning.CLightningClient;
import com.mobileln.utils.SettingsSharedPrefs;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class LightningClient implements LightningClientInterface {

    private LightningClientInterface mImpl;

    private LightningClient(boolean redirectError) {
        boolean useLnd = new SettingsSharedPrefs(MyApplication.getContext()).isBackendLnd();
        if (useLnd) {

        } else {
            mImpl = new CLightningClient(redirectError);
        }
    }

    public static LightningClient newInstance() {
        return newInstance(false);
    }

    public static LightningClient newInstance(boolean redirectError) {
        return new LightningClient(redirectError);
    }

    @Override
    public String rawQuery(Context context, String[] args) {
        return mImpl.rawQuery(context, args);
    }

    @Override
    public JSONObject getJSONResponse(Context context, String[] args)
            throws IOException, JSONException {
        return mImpl.getJSONResponse(context, args);
    }

    @Override
    public int getBlockHeight(Context context) throws IOException, JSONException {
        return mImpl.getBlockHeight(context);
    }

    @Override
    public String[] getMyBech32Addresses() throws IOException, JSONException {
        return mImpl.getMyBech32Addresses();
    }

    @Override
    public long getConfirmedBtcBalanceInWallet() throws IOException, JSONException {
        return mImpl.getConfirmedBtcBalanceInWallet();
    }

    @Override
    public long getConfirmedBalanceInChannels() throws IOException, JSONException {
        return mImpl.getConfirmedBalanceInChannels();
    }

    @Override
    public long getCachedOnChainBalance() {
        return mImpl.getCachedOnChainBalance();
    }

    @Override
    public long getCachedInboundCapacity() {
        return mImpl.getCachedInboundCapacity();
    }

    @Override
    public long getCachedOutboundCapacity() {
        return mImpl.getCachedOutboundCapacity();
    }

    @Override
    public long getCachedInChannelBalance() {
        return mImpl.getCachedInChannelBalance();
    }

    @Override
    public PaymentInfo[] getPaymentSent() throws IOException, JSONException {
        return mImpl.getPaymentSent();
    }

    @Override
    public PaymentInfo[] getPaymentReceived() throws IOException, JSONException {
        return mImpl.getPaymentReceived();
    }

    @Override
    public PaymentInfo getInvoiceInfo(String label) throws IOException, JSONException {
        return mImpl.getInvoiceInfo(label);
    }

    @Override
    public PaymentInfo getDecodedInvoice(String invoice) throws IOException, JSONException {
        return mImpl.getInvoiceInfo(invoice);
    }

    @Override
    public PaymentInfo payInvoice(String invoice, String label) throws IOException, JSONException {
        return mImpl.payInvoice(invoice, label);
    }

    @Override
    public String withdrawBtc(String withdrawalAddr, long amount, boolean withdrawAll)
            throws IOException, JSONException {
        return mImpl.withdrawBtc(withdrawalAddr, amount, withdrawAll);
    }

    @Override
    public String generateInvoice(long sat, String description) throws IOException, JSONException {
        return mImpl.generateInvoice(sat, description);
    }

    @Override
    public void updateCachedInOutboundCapacity() throws IOException, JSONException {
        mImpl.updateCachedInOutboundCapacity();
    }

    @Override
    public ChannelInfo[] getChannelList() throws IOException, JSONException {
        return mImpl.getChannelList();
    }

    @Override
    public boolean closeChannel(String channelId, boolean force) throws IOException, JSONException {
        return mImpl.closeChannel(channelId, force);
    }

    @Override
    public String connectPeer(String nodeAddress) throws IOException, JSONException {
        return mImpl.connectPeer(nodeAddress);
    }

    @Override
    public boolean fundChannel(String peerId, long amount) throws IOException, JSONException {
        return mImpl.fundChannel(peerId, amount);
    }

    @Override
    public String[] getListAddrs(int maxIndex) throws IOException, JSONException {
        return mImpl.getListAddrs(maxIndex);
    }
}
