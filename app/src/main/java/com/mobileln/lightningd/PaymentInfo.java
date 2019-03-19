package com.mobileln.lightningd;

public class PaymentInfo {
    public String description;
    public String bolt11;
    public String paymentHash;
    public long satAmount;
    public boolean completed;
    public long dateTime;

    public PaymentInfo(String description, String bolt11, String paymentHash, long satAmount,
            boolean completed, long dateTime) {
        this.description = description;
        this.bolt11 = bolt11;
        this.paymentHash = paymentHash;
        this.satAmount = satAmount;
        this.completed = completed;
        this.dateTime = dateTime;
    }
}
