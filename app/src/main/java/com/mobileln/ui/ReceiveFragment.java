package com.mobileln.ui;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.WriterException;

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
import com.mobileln.utils.QRUtils;
import com.mobileln.utils.UIUtils;

public class ReceiveFragment extends Fragment {

    private static final String TAG = "ReceiveFragment";
    private static final long PAYMENT_LISTENER_INTERVAL_MS = 500;

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ImageView mQRImageView;
    private Button mMyInvoiceBtn;
    private LinearLayout mPaymentReceivedListLayout;
    private LinearLayout mQRLinearLayout;
    private EditText mAmountEditText;
    private EditText mDescriptionEditText;
    private Button mGenerateInvoiceBtn;
    private TextView mNoInboundPaymentsTextview;
    private String mLabelPendingPayment = null;
    private PaymentInfo[] mInboundPaymentList = null;
    private volatile Thread mUpdateUiThread = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.ln_receive_payment_layout, container, false);
        mSwipeRefreshLayout = view.findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updateReceivedPaymentAsync();
            }
        });
        mQRImageView = view.findViewById(R.id.receive_payment_qr_imageview);
        mMyInvoiceBtn = view.findViewById(R.id.receive_my_invoice_button);
        mPaymentReceivedListLayout = view.findViewById(R.id.payment_received_list);
        mQRLinearLayout = view.findViewById(R.id.receive_payment_qr_linear_layout);
        mAmountEditText = view.findViewById(R.id.receive_payment_amount_textview);
        mDescriptionEditText = view.findViewById(R.id.receive_payment_description_textview);
        mNoInboundPaymentsTextview = view.findViewById(R.id.receive_no_inbound_payment_textview);
        mGenerateInvoiceBtn = view.findViewById(R.id.generate_new_invoice_btn);
        mGenerateInvoiceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showGenerateInvoiceDialog();
            }
        });
        mMyInvoiceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(
                        Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("addr", mMyInvoiceBtn.getText());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getContext(), "Copied address", Toast.LENGTH_SHORT).show();
            }
        });
        try {
            mQRImageView.setImageBitmap(QRUtils.encodeAsBitmap("fuck", getResources()));
        } catch (WriterException e) {
            e.printStackTrace();
        }
        updateReceivedPaymentAsync();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mLabelPendingPayment != null) {
            registerPaymentReceivedListener(mLabelPendingPayment);
        }
        updatePaymentList(mInboundPaymentList);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterPaymentReceivedListener();
        clearPendingPayment();
    }

    private void showGenerateInvoiceDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.generate_invoice, null);
        final EditText amountEditText = dialogView.findViewById(
                R.id.gen_invoice_payment_amount_textivew);
        final EditText descriptionEditText = dialogView.findViewById(
                R.id.gen_invoice_payment_description_textivew);
        new AlertDialog.Builder(getActivity(),
                R.style.Theme_MaterialComponents_Light_Dialog_Alert).setTitle(
                "New Invoice").setView(
                dialogView).setPositiveButton(
                android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        final long amount;
                        try {
                            amount = Long.parseLong(amountEditText.getText().toString());
                        } catch (Exception e) {
                            UIUtils.showErrorToast(getActivity(), "Amount cannot be empty");
                            return;
                        }
                        final String description = descriptionEditText.getText().toString();
                        if (TextUtils.isEmpty(description)) {
                            UIUtils.showErrorToast(getActivity(), "Description cannot be empty");
                            return;
                        }
                        updateReceiveInfoAsync(amount, description);
                    }
                }).setNeutralButton(android.R.string.cancel, null).show();
    }

    private void updateReceiveInfoAsync(final long amount, final String description) {
        new AsyncTask<Void, Void, String>() {

            @Override
            protected String doInBackground(Void... voids) {
                try {
                    return LightningCli.newInstance().generateInvoice(amount,
                            description);
                } catch (IOException | JSONException e) {
                    UIUtils.showErrorToast(getActivity(), e.getMessage());
                    return null;
                }
            }

            @Override
            public void onPostExecute(String bolt11) {
                if (bolt11 == null) {
                    return;
                }
                updateQRImage(bolt11, amount, description);
                registerPaymentReceivedListener(description);
            }
        }.execute();
    }

    private void updateQRImage(String bolt11, long amount, String description) {
        try {
            Bitmap qrCode = QRUtils.encodeAsBitmap(bolt11, getResources());
            mQRImageView.setImageBitmap(qrCode);
            mMyInvoiceBtn.setText(bolt11);
            mQRLinearLayout.setVisibility(View.VISIBLE);
            mGenerateInvoiceBtn.setVisibility(View.GONE);
            mAmountEditText.setText(String.valueOf(amount));
            mDescriptionEditText.setText(description);
            mLabelPendingPayment = description;
            updateReceivedPaymentAsync();
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    private void clearPendingPayment() {
        mQRLinearLayout.setVisibility(View.GONE);
        mGenerateInvoiceBtn.setVisibility(View.VISIBLE);
        mLabelPendingPayment = null;
        updateReceivedPaymentAsync();
    }

    private void updateReceivedPaymentAsync() {
        new AsyncTask<Void, Void, PaymentInfo[]>() {

            @Override
            protected PaymentInfo[] doInBackground(Void... voids) {
                try {
                    return LightningCli.newInstance().getPaymentReceived();
                } catch (IOException | JSONException e) {
                    UIUtils.showErrorToast(getActivity(), e.getMessage());
                }
                return null;
            }

            @Override
            public void onPostExecute(PaymentInfo[] paymentInfos) {
                mSwipeRefreshLayout.setRefreshing(false);
                if (paymentInfos == null) {
                    return;
                }
                mInboundPaymentList = paymentInfos;
                updatePaymentList(mInboundPaymentList);
            }
        }.execute();
    }

    private void updatePaymentList(PaymentInfo[] inboundPaymentList) {
        if (inboundPaymentList == null) {
            return;
        }
        int paymentCount = inboundPaymentList.length;
        int childCount = mPaymentReceivedListLayout.getChildCount();

        // TOOD: Better timezone handling
        Calendar cal = Calendar.getInstance();
        TimeZone tz = cal.getTimeZone();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm");
        sdf.setTimeZone(tz);

        if (paymentCount > 0) {
            mNoInboundPaymentsTextview.setVisibility(View.GONE);
        } else {
            mNoInboundPaymentsTextview.setVisibility(View.VISIBLE);
        }

        // TODO: Convert it into listview, no hardcode
        for (int i = 0; i < childCount; i++) {
            View view = mPaymentReceivedListLayout.getChildAt(i);
            if (i < paymentCount) {
                view.setVisibility(View.VISIBLE);
                ImageView receiveStatusImage = view.findViewById(
                        R.id.receive_status_imageview);
                TextView descriptionTextView = view.findViewById(
                        R.id.receive_description_textview);
                TextView amountTextView = view.findViewById(R.id.receive_amount_textview);
                TextView dateTextView = view.findViewById(
                        R.id.receive_date_textview);
                final PaymentInfo paymentInfo = inboundPaymentList[paymentCount - i - 1];
                String description = paymentInfo.description;
                if (TextUtils.isEmpty(description)) {
                    description = paymentInfo.paymentHash;
                }
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (!paymentInfo.completed) {
                            updateQRImage(paymentInfo.bolt11, paymentInfo.satAmount,
                                    paymentInfo.description);
                            registerPaymentReceivedListener(paymentInfo.description);
                        }
                    }
                });
                receiveStatusImage.setImageResource(
                        paymentInfo.completed ? R.drawable.tick : R.drawable.clock);
                descriptionTextView.setText(description);
                amountTextView.setText(BtcSatUtils.sat2String(paymentInfo.satAmount));
                dateTextView.setText(paymentInfo.dateTime > 0 ?
                        "Received: " + sdf.format(new Date(paymentInfo.dateTime * 1000))
                        : (paymentInfo.completed ? "completed?????"
                                : "Pending payment..."));
            } else {
                view.setVisibility(View.GONE);
            }
        }
    }

    private void registerPaymentReceivedListener(final String label) {
        final Thread thread = new Thread() {
            public void run() {
                while (mUpdateUiThread == this) {
                    try {
                        final PaymentInfo paymentInfo = LightningCli.newInstance().getInvoiceInfo(
                                label);
                        if (paymentInfo != null && paymentInfo.completed) {
                            unregisterPaymentReceivedListener();
                            final Activity activity = getActivity();
                            if (activity == null) {
                                return;
                            }
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    new AlertDialog.Builder(activity,
                                            R.style.Theme_MaterialComponents_Light_Dialog_Alert)
                                            .setTitle(
                                                    "Payment received").setMessage(
                                            "\nReceived:      \t" + BtcSatUtils.sat2String(
                                                    paymentInfo.satAmount) + "\n\nDescription:   \t"
                                                    + paymentInfo.description).setPositiveButton(
                                            android.R.string.ok, null).show();
                                    clearPendingPayment();
                                }
                            });
                        }
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }
                    SystemClock.sleep(PAYMENT_LISTENER_INTERVAL_MS);
                }
            }
        };
        mUpdateUiThread = thread;
        thread.start();
    }

    private void unregisterPaymentReceivedListener() {
        mUpdateUiThread = null;
    }
}
