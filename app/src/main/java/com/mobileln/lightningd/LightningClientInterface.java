package com.mobileln.lightningd;

import android.content.Context;
import android.support.annotation.WorkerThread;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public interface LightningClientInterface {

    @WorkerThread
    String rawQuery(Context context, String[] args);

    @WorkerThread
    JSONObject getJSONResponse(Context context, String[] args) throws IOException, JSONException;

    @WorkerThread
    int getBlockHeight(Context context) throws IOException, JSONException;

    @WorkerThread
    String[] getMyBech32Addresses() throws IOException, JSONException;

    @WorkerThread
    String newBech32Address() throws IOException, JSONException;

    @WorkerThread
    String getGeneratedBech32Address() throws IOException, JSONException;

    @WorkerThread
    long getConfirmedOnChainBalance() throws IOException, JSONException;

    @WorkerThread
    long getBalanceInChannels() throws IOException, JSONException;

    long getCachedOnChainBalance();

    long getCachedInboundCapacity();

    long getCachedOutboundCapacity();

    long getCachedInChannelBalance();

    @WorkerThread
    PaymentInfo[] getPaymentSent() throws IOException, JSONException;

    @WorkerThread
    PaymentInfo[] getPaymentReceived() throws IOException, JSONException;

    @WorkerThread
    PaymentInfo getInvoiceInfo(String label) throws IOException, JSONException;

    @WorkerThread
    PaymentInfo getDecodedInvoice(String invoice) throws IOException, JSONException;

    @WorkerThread
    PaymentInfo payInvoice(String invoice, String label) throws IOException, JSONException;

    @WorkerThread
    String withdrawBtc(String withdrawalAddr, long amount, boolean withdrawAll)
            throws IOException, JSONException;

    @WorkerThread
    String generateInvoice(long sat, String description) throws IOException, JSONException;

    @WorkerThread
    void updateCachedInOutboundCapacity() throws IOException, JSONException;

    @WorkerThread
    ChannelInfo[] getChannelList() throws IOException, JSONException;

    @WorkerThread
    boolean closeChannel(String channelId, boolean force) throws IOException, JSONException;

    @WorkerThread
    String connectPeer(String nodeAddress) throws IOException, JSONException;

    @WorkerThread
    boolean fundChannel(String peerId, long amount) throws IOException, JSONException;

    @WorkerThread
    String[] getListAddrs(int maxIndex) throws IOException, JSONException;

    long getCachedUnconfirmedOnChainBalance();

    long getUnconfirmedOnChainBalance(int minConfirmation) throws JSONException, IOException;
}
