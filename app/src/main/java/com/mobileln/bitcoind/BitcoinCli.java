package com.mobileln.bitcoind;

import java.net.MalformedURLException;
import java.net.URL;

import wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient;
import wf.bitcoin.javabitcoindrpcclient.BitcoinRPCException;

public class BitcoinCli {
    private static BitcoinJSONRPCClient getClient() {
        String username = Bitcoind.getInstance().getRpcUserName();
        String password = Bitcoind.getInstance().getRpcPassowrd();
        int port = Bitcoind.RPC_PORT;
        try {
            return new BitcoinJSONRPCClient(new URL("http://" + username + ':' + password + "@" + "localhost" + ":" + port + "/"));
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
}
