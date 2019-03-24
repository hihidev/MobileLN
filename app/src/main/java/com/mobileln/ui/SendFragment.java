package com.mobileln.ui;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONException;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import com.mobileln.R;
import com.mobileln.lightningd.LightningCli;
import com.mobileln.lightningd.PaymentInfo;
import com.mobileln.utils.BtcSatUtils;

public class SendFragment extends Fragment {

    private static final String TAG = "SendFragment";

    private TextView mScanQRTextView;
    private EditText mInvoiceTextView;
    private TextView mAmountTextView;
    private Button mPayNowBtn;
    private Button mClearBtn;
    private TextView mDescriptionTextView;
    private LinearLayout mPaymentSentListLayout;

    private long mPayAmount;
    private String mPayDescription;
    private boolean mValidInvoice = false;
    private AlertDialog mPayingDialog = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.ln_send_payment_layout, container, false);
        mScanQRTextView = view.findViewById(R.id.scan_qr_payment);
        mInvoiceTextView = view.findViewById(R.id.payment_invoice_textview);
        mPayNowBtn = view.findViewById(R.id.payment_pay_now_button);
        mClearBtn = view.findViewById(R.id.payment_clear_button);
        mPaymentSentListLayout = view.findViewById(R.id.payment_sent_list);
        mPayNowBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mValidInvoice) {
                    showConfirmPaymentDialog(mInvoiceTextView.getText().toString(), mPayDescription,
                            mPayAmount);
                }
            }
        });
        mClearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mInvoiceTextView.setText("");
            }
        });
        mInvoiceTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                mValidInvoice = false;
                mPayDescription = null;
                mPayAmount = 0;
                mPayNowBtn.setEnabled(false);
                updateAmountDescriptionAsync(editable.toString());
            }
        });
        mAmountTextView = view.findViewById(R.id.payment_amount_textivew);
        mDescriptionTextView = view.findViewById(R.id.payment_description_textivew);
        mScanQRTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IntentIntegrator integrator = IntentIntegrator.forSupportFragment(
                        SendFragment.this);
                integrator.setOrientationLocked(false);
                integrator.setPrompt("Scan QR invoice");
                integrator.setCameraId(0);  // Use a specific camera of the device
                integrator.setBeepEnabled(false);
                integrator.setBarcodeImageEnabled(true);
                integrator.initiateScan();
            }
        });
        updatePaymentSentAsync();
        mPayingDialog = new AlertDialog.Builder(getContext())
                .setTitle("Payment")
                .setMessage("Sending payment, please wait...")
                .setNegativeButton(android.R.string.no, null).create();
        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(getContext(), "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                mInvoiceTextView.setText(result.getContents());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void updateAmountDescriptionAsync(final String invoice) {
        new AsyncTask<Void, Void, PaymentInfo>() {

            @Override
            protected PaymentInfo doInBackground(Void... voids) {
                try {
                    return LightningCli.newInstance().getDecodedInvoice(invoice);
                } catch (IOException | JSONException e) {
                    return null;
                }
            }

            @Override
            public void onPostExecute(PaymentInfo paymentInfo) {
                if (paymentInfo == null) {
                    mAmountTextView.setText("------");
                    mDescriptionTextView.setText("------");
                    return;
                }
                mAmountTextView.setText(BtcSatUtils.sat2String(paymentInfo.satAmount));
                String description = paymentInfo.description;
                if (description == null) {
                    description = paymentInfo.paymentHash;
                }
                mDescriptionTextView.setText(paymentInfo.description);
                mValidInvoice = true;
                mPayDescription = description;
                mPayAmount = paymentInfo.satAmount;
                mPayNowBtn.setEnabled(true);
            }
        }.execute();
    }

    private void showConfirmPaymentDialog(final String invoice, final String label, long amount) {
        new AlertDialog.Builder(getContext())
                .setTitle("Payment")
                .setMessage("Description:\t" + label + "\n" + "Amount:\t" + BtcSatUtils.sat2String(
                        amount) + "\n" + "Do you CONFIRM to pay this invoice?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Toast.makeText(getContext(), "Yaay", Toast.LENGTH_SHORT).show();
                        mInvoiceTextView.setText("");
                        mPayingDialog.show();
                        new Thread() {
                            public void run() {
                                try {
                                    showPaymentSuccessResult(
                                            LightningCli.newInstance().payInvoice(invoice, label));
                                } catch (IOException | JSONException e) {
                                    showPaymentFailedResult(e.getMessage());
                                }
                                updatePaymentSentAsync();
                            }
                        }.start();
                    }
                })
                .setNegativeButton(android.R.string.no, null).show();
    }

    private void updatePaymentSentAsync() {
        new AsyncTask<Void, Void, PaymentInfo[]>() {

            @Override
            protected PaymentInfo[] doInBackground(Void... voids) {
                try {
                    return LightningCli.newInstance().getPaymentSent();
                } catch (IOException | JSONException e) {
                    return null;
                }
            }

            @Override
            public void onPostExecute(PaymentInfo[] paymentInfos) {
                if (paymentInfos == null) {
                    Toast.makeText(getContext(), "cannot update payment",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                int paymentCount = paymentInfos.length;
                int childCount = mPaymentSentListLayout.getChildCount();

                // TOOD: Better timezone handling
                Calendar cal = Calendar.getInstance();
                TimeZone tz = cal.getTimeZone();
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm");
                sdf.setTimeZone(tz);

                // TODO: Convert it into listview, no hardcode
                for (int i = 0; i < childCount; i++) {
                    View view = mPaymentSentListLayout.getChildAt(i);
                    if (i < paymentCount) {
                        view.setVisibility(View.VISIBLE);
                        TextView fromTextView = view.findViewById(
                                R.id.receive_description_textview);
                        TextView amountTextView = view.findViewById(R.id.receive_amount_textview);
                        TextView referenceTextView = view.findViewById(R.id.receive_date_textview);
                        ImageView aymentStatusImage = view.findViewById(
                                R.id.receive_status_imageview);
                        PaymentInfo paymentInfo = paymentInfos[paymentCount - i - 1];
                        String description = paymentInfo.description;
                        if (TextUtils.isEmpty(description)) {
                            description = paymentInfo.paymentHash;
                        }
                        aymentStatusImage.setImageResource(
                                paymentInfo.completed ? R.drawable.tick : R.drawable.clock);
                        fromTextView.setText(description);
                        amountTextView.setText(BtcSatUtils.sat2String(paymentInfo.satAmount));
                        referenceTextView.setText(paymentInfo.completed ? "Paid: " + sdf.format(
                                new Date(paymentInfo.dateTime * 1000)) : "Paying invoice...");
                    } else {
                        view.setVisibility(View.GONE);
                    }
                }
            }
        }.execute();
    }

    private void showPaymentSuccessResult(final PaymentInfo paymentInfo) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPayingDialog.dismiss();
                new AlertDialog.Builder(getContext())
                        .setTitle("Payment success")
                        .setMessage("Paid:\t" + BtcSatUtils.sat2String(paymentInfo.satAmount))
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setNeutralButton(android.R.string.ok, null)
                        .show();
                clearPaymentDetails();
            }
        });
    }

    private void clearPaymentDetails() {
        mAmountTextView.setText("------");
        mDescriptionTextView.setText("------");
        mValidInvoice = false;
        mPayNowBtn.setEnabled(false);
    }

    private void showPaymentFailedResult(final String errorMsg) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPayingDialog.dismiss();
                new AlertDialog.Builder(getContext())
                        .setTitle("Payment failed")
                        .setMessage("Error: " + errorMsg)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setNeutralButton(android.R.string.ok, null)
                        .show();
            }
        });
    }
}
