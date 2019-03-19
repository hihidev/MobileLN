package com.mobileln.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
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

public class ReceiveFragment extends Fragment {

    private static final String TAG = "ReceiveFragment";

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ImageView mQRImageView;
    private Button mMyAddressBtn;
    private LinearLayout mPaymentReceivedListLayout;
    private LinearLayout mQRLinearLayout;
    private EditText mAmountEditText;
    private EditText mDescriptionEditText;
    private Button mGenerateInvoiceBtn;

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
        mMyAddressBtn = view.findViewById(R.id.receive_my_address_button);
        mPaymentReceivedListLayout = view.findViewById(R.id.payment_received_list);
        mQRLinearLayout = view.findViewById(R.id.receive_payment_qr_linear_layout);
        mAmountEditText = view.findViewById(R.id.receive_payment_amount_textview);
        mDescriptionEditText = view.findViewById(R.id.receive_payment_description_textview);
        mGenerateInvoiceBtn = view.findViewById(R.id.generate_new_invoice_btn);
        mGenerateInvoiceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showGenerateInvoiceDialog();
            }
        });
        mMyAddressBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(
                        Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("addr", mMyAddressBtn.getText());
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

    private void showGenerateInvoiceDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.generate_invoice, null);
        final EditText amountEditText = dialogView.findViewById(R.id.payment_amount_textivew);
        final EditText descriptionEditText = dialogView.findViewById(
                R.id.payment_description_textivew);
        new AlertDialog.Builder(getContext()).setTitle("Invoice").setView(
                dialogView).setPositiveButton(

                android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        updateReceiveInfoAsync(Long.parseLong(amountEditText.getText().toString()),
                                descriptionEditText.getText().toString());
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
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            public void onPostExecute(String bolt11) {
                if (bolt11 == null) {
                    Toast.makeText(getContext(), "cannot get bolt11", Toast.LENGTH_SHORT).show();
                    return;
                }
                updateQRImage(bolt11, amount, description);
            }
        }.execute();
    }

    private void updateQRImage(String bolt11, long amount, String description) {
        try {
            Bitmap qrCode = QRUtils.encodeAsBitmap(bolt11, getResources());
            mQRImageView.setImageBitmap(qrCode);
            mMyAddressBtn.setText("lightning:" + bolt11);
            mQRLinearLayout.setVisibility(View.VISIBLE);
            mGenerateInvoiceBtn.setVisibility(View.GONE);
            mAmountEditText.setText(String.valueOf(amount));
            mDescriptionEditText.setText(description);
            updateReceivedPaymentAsync();
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    private void updateReceivedPaymentAsync() {
        new AsyncTask<Void, Void, PaymentInfo[]>() {

            @Override
            protected PaymentInfo[] doInBackground(Void... voids) {
                try {
                    return LightningCli.newInstance().getPaymentReceived();
                } catch (IOException | JSONException e) {
                    Log.i(TAG, "Error", e);
                }
                return null;
            }

            @Override
            public void onPostExecute(PaymentInfo[] paymentInfos) {
                mSwipeRefreshLayout.setRefreshing(false);
                if (paymentInfos == null) {
                    Toast.makeText(getContext(), "cannot getPaymentReceived",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                int paymentCount = paymentInfos.length;
                int childCount = mPaymentReceivedListLayout.getChildCount();

                // TOOD: Better timezone handling
                Calendar cal = Calendar.getInstance();
                TimeZone tz = cal.getTimeZone();
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                sdf.setTimeZone(tz);

                // TODO: Convert it into listview, no hardcode
                for (int i = 0; i < childCount; i++) {
                    View view = mPaymentReceivedListLayout.getChildAt(i);
                    if (i < paymentCount) {
                        view.setVisibility(View.VISIBLE);
                        ImageView receiveStatusImage = view.findViewById(
                                R.id.receive_status_imageview);
                        TextView fromTextView = view.findViewById(R.id.from_textview);
                        TextView amountTextView = view.findViewById(R.id.receive_amount_textview);
                        TextView referenceTextView = view.findViewById(
                                R.id.receive_reference_textview);
                        final PaymentInfo paymentInfo = paymentInfos[paymentCount - i - 1];
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
                                }
                            }
                        });
                        receiveStatusImage.setImageResource(
                                paymentInfo.completed ? R.drawable.tick : R.drawable.clock);
                        fromTextView.setText(description);
                        amountTextView.setText(BtcSatUtils.sat2String(paymentInfo.satAmount));
                        referenceTextView.setText(paymentInfo.dateTime > 0 ?
                                sdf.format(new Date(paymentInfo.dateTime * 1000)) : "");
                    } else {
                        view.setVisibility(View.GONE);
                    }
                }
            }
        }.execute();
    }
}
