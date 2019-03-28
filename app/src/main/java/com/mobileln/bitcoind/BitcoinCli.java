package com.mobileln.bitcoind;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient;
import wf.bitcoin.javabitcoindrpcclient.BitcoinRPCException;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;

public class BitcoinCli {

    private volatile static long sCachedUnconfirmedBalance = -1;

    private static BitcoinJSONRPCClient getClient() {
        String username = Bitcoind.getInstance().getRpcUserName();
        String password = Bitcoind.getInstance().getRpcPassowrd();
        int port = Bitcoind.RPC_PORT;
        try {
            return new BitcoinJSONRPCClient(
                    new URL("http://" + username + ':' + password + "@" + "localhost" + ":" + port
                            + "/"));
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static double getVerificationProgress() throws BitcoinRPCException {
        try {
            return getClient().getBlockChainInfo().verificationProgress().doubleValue();
        } catch (BitcoinRPCException | UnsupportedOperationException e) {
            throw new BitcoinRPCException(e.getMessage());
        }
    }

    public static long getUnconfirmedBalance(int minConfirmationCount) throws BitcoinRPCException {
        try {
            long result = 0;
            List<BitcoindRpcClient.Transaction> list = getClient().listTransactions("*", 10, 0,
                    true);
            for (BitcoindRpcClient.Transaction transaction : list) {
                if (transaction.confirmations() < minConfirmationCount) {
                    long amount = transaction.amount().multiply(
                            new BigDecimal(100000000)).longValue();
                    // We don't do amount < 0 as lightning-cli listfunds won't list spent funds,
                    // it's deduced in confirmed balance already
                    if (amount > 0) {
                        result += transaction.amount().multiply(
                                new BigDecimal(100000000)).longValue();
                    }
                }
            }
            sCachedUnconfirmedBalance = result;
            return result;
        } catch (BitcoinRPCException | UnsupportedOperationException e) {
            throw new BitcoinRPCException(e.getMessage());
        }
    }

    public static long getCachedUnconfirmedBalance() {
        return sCachedUnconfirmedBalance;
    }

    public static void addWatchOnlyAddress(String address) {
        getClient().importAddress(address, address, false);
    }
}
