package com.mobileln.lightningd;

public class ChannelInfo {

    public static class State {
        public static final String CHANNELD_NORMAL = "CHANNELD_NORMAL";
        public static final String CHANNELD_AWAITING_LOCKIN = "CHANNELD_AWAITING_LOCKIN";
        public static final String CHANNELD_SHUTTING_DOWN = "CHANNELD_SHUTTING_DOWN";
        public static final String OPENINGD = "OPENINGD";
        public static final String CLOSINGD_SIGEXCHANGE = "CLOSINGD_SIGEXCHANGE";
        public static final String CLOSINGD_COMPLETE = "CLOSINGD_COMPLETE";
        public static final String FUNDING_SPEND_SEEN = "FUNDING_SPEND_SEEN";
        public static final String ONCHAIN = "ONCHAIN";
        public static final String AWAITING_UNILATERAL = "AWAITING_UNILATERAL";
    }

    public String channelId;
    public boolean closed;
    public boolean active;
    public String name;
    // msatoshi_total
    public long channelTotalSat;
    // msatoshi_total - msatoshi_to_us
    public long myBal;
    // msatoshi_to_us
    public long oppBal;

    public ChannelInfo(String channelId, boolean closed, boolean active, String name, long channelTotalSat,
            long oppBal, long myBal) {
        this.channelId = channelId;
        this.closed = closed;
        this.active = active;
        this.name = name;
        this.channelTotalSat = channelTotalSat;
        this.oppBal = oppBal;
        this.myBal = myBal;
    }
}
